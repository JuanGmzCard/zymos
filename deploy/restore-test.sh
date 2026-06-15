#!/usr/bin/env bash
# =============================================================================
# restore-test.sh — Prueba mensual de restauración de backups Zymos
# =============================================================================
# Restaura el backup más reciente generado por backup.sh en una base de datos
# temporal, corre sanity checks (conteo de filas en tablas clave) y borra la
# base temporal. Sale con error si algo falla — pensado para alertar via cron.
#
# Uso:
#   chmod +x deploy/restore-test.sh
#   ./deploy/restore-test.sh
#
# Cron mensual (día 1 a las 3 AM):
#   0 3 1 * * /ruta/al/proyecto/deploy/restore-test.sh >> /var/log/zymos-restore-test.log 2>&1
#
# Variables de entorno necesarias (leer de .env o definir en el sistema):
#   DB_HOST     — host PostgreSQL (default: localhost)
#   DB_PORT     — puerto (default: 5432)
#   DB_NAME     — nombre de la base original (default: trazabilidad_cervezas)
#   RESTORE_TEST_DB_USERNAME / RESTORE_TEST_DB_PASSWORD — usuario con privilegio
#       CREATEDB (necesario para crear/borrar la base temporal). NO usar
#       zymos_app/zymos_flyway salvo que se les otorgue CREATEDB explícitamente.
#       Si no se definen, se usa DB_USERNAME/DB_PASSWORD.
#   BACKUP_DIR  — directorio de backups (default: ./backups), igual que backup.sh
#   RESTORE_TEST_DB_NAME — nombre de la base temporal (default: ${DB_NAME}_restore_test)
# =============================================================================

set -euo pipefail

# ── Cargar .env si existe ─────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="$SCRIPT_DIR/../.env"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a && source "$ENV_FILE" && set +a
fi

# ── Configuración ─────────────────────────────────────────────────────────────
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-trazabilidad_cervezas}"
DB_USER="${RESTORE_TEST_DB_USERNAME:-${DB_USERNAME:?DB_USERNAME no está definida}}"
PGPASSWORD="${RESTORE_TEST_DB_PASSWORD:-${DB_PASSWORD:?DB_PASSWORD no está definida}}"
export PGPASSWORD

BACKUP_DIR="${BACKUP_DIR:-$SCRIPT_DIR/../backups}"
TEST_DB="${RESTORE_TEST_DB_NAME:-${DB_NAME}_restore_test}"

PSQL=(psql --host="$DB_HOST" --port="$DB_PORT" --username="$DB_USER" --no-password)

log() { echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*"; }

# ── Limpieza garantizada de la base temporal ────────────────────────────────────
cleanup() {
  "${PSQL[@]}" -d postgres -v ON_ERROR_STOP=1 -c "DROP DATABASE IF EXISTS \"$TEST_DB\";" >/dev/null
}
trap cleanup EXIT

# ── Ubicar el backup más reciente (diario, o semanal si no hay diarios) ─────────
LATEST="$(ls -t "$BACKUP_DIR"/zymos_*.sql.gz 2>/dev/null | head -1 || true)"
if [[ -z "$LATEST" ]]; then
  LATEST="$(ls -t "$BACKUP_DIR/weekly"/zymos_*.sql.gz 2>/dev/null | head -1 || true)"
fi
if [[ -z "$LATEST" ]]; then
  echo "ERROR: no se encontró ningún backup en $BACKUP_DIR" >&2
  exit 1
fi
log "Usando backup: $LATEST"

# ── Verificar integridad del archivo antes de restaurar ─────────────────────────
if ! gzip -t "$LATEST" 2>/dev/null; then
  echo "ERROR: backup corrupto: $LATEST" >&2
  exit 1
fi

# ── Crear base temporal y restaurar ──────────────────────────────────────────────
log "Creando base temporal $TEST_DB"
cleanup
"${PSQL[@]}" -d postgres -v ON_ERROR_STOP=1 -c "CREATE DATABASE \"$TEST_DB\";" >/dev/null

log "Restaurando backup en $TEST_DB"
gunzip -c "$LATEST" | "${PSQL[@]}" -d "$TEST_DB" -v ON_ERROR_STOP=1 >/dev/null

# ── Sanity checks: conteo de filas en tablas clave ───────────────────────────────
log "Ejecutando sanity checks"
FAILED=0
for TABLE in usuarios tenants lotes_cerveza inventario; do
  COUNT="$("${PSQL[@]}" -d "$TEST_DB" -t -A -c "SELECT count(*) FROM $TABLE;" 2>/dev/null || echo "ERROR")"
  if [[ "$COUNT" == "ERROR" ]]; then
    echo "ERROR: no se pudo consultar la tabla $TABLE" >&2
    FAILED=1
  else
    log "  $TABLE: $COUNT filas"
  fi
done

if [[ "$FAILED" -ne 0 ]]; then
  echo "ERROR: sanity check falló — ver detalles arriba" >&2
  exit 1
fi

unset PGPASSWORD
log "Prueba de restauración finalizada correctamente (base temporal $TEST_DB será eliminada)."
