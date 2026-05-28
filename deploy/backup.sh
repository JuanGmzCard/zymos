#!/usr/bin/env bash
# =============================================================================
# backup.sh — Backup diario de PostgreSQL para Zymos
# =============================================================================
# Uso:
#   chmod +x deploy/backup.sh
#   ./deploy/backup.sh
#
# Cron diario a las 2 AM:
#   0 2 * * * /ruta/al/proyecto/deploy/backup.sh >> /var/log/zymos-backup.log 2>&1
#
# Variables de entorno necesarias (leer de .env o definir en el sistema):
#   DB_HOST     — host PostgreSQL (default: localhost)
#   DB_PORT     — puerto (default: 5432)
#   DB_NAME     — nombre de la base (default: trazabilidad_cervezas)
#   DB_USERNAME — usuario con acceso de lectura (zymos_flyway recomendado)
#   DB_PASSWORD — contraseña del usuario
#   BACKUP_DIR  — directorio donde guardar los backups (default: ./backups)
#   BACKUP_KEEP_DAYS   — días a retener backups diarios (default: 7)
#   BACKUP_KEEP_WEEKS  — semanas a retener backups semanales (default: 4)
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
DB_USER="${DB_USERNAME:?DB_USERNAME no está definida}"
PGPASSWORD="${DB_PASSWORD:?DB_PASSWORD no está definida}"
export PGPASSWORD

BACKUP_DIR="${BACKUP_DIR:-$SCRIPT_DIR/../backups}"
KEEP_DAYS="${BACKUP_KEEP_DAYS:-7}"
KEEP_WEEKS="${BACKUP_KEEP_WEEKS:-4}"

TIMESTAMP="$(date +%Y%m%d_%H%M%S)"
DOW="$(date +%u)"   # 1=lunes … 7=domingo
FILENAME="zymos_${DB_NAME}_${TIMESTAMP}.sql.gz"
FILEPATH="$BACKUP_DIR/$FILENAME"

mkdir -p "$BACKUP_DIR"

# ── Ejecutar pg_dump ──────────────────────────────────────────────────────────
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Iniciando backup → $FILENAME"

pg_dump \
  --host="$DB_HOST" \
  --port="$DB_PORT" \
  --username="$DB_USER" \
  --no-password \
  --format=plain \
  --no-owner \
  --no-acl \
  "$DB_NAME" | gzip -9 > "$FILEPATH"

SIZE="$(du -sh "$FILEPATH" | cut -f1)"
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup completado: $FILENAME ($SIZE)"

# ── Backup semanal (domingo) — copia adicional ────────────────────────────────
if [[ "$DOW" -eq 7 ]]; then
  WEEKLY_DIR="$BACKUP_DIR/weekly"
  mkdir -p "$WEEKLY_DIR"
  cp "$FILEPATH" "$WEEKLY_DIR/$FILENAME"
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup semanal guardado en weekly/"

  # Limpiar backups semanales viejos
  find "$WEEKLY_DIR" -name "zymos_*.sql.gz" -mtime +"$((KEEP_WEEKS * 7))" -delete
fi

# ── Limpiar backups diarios viejos ────────────────────────────────────────────
find "$BACKUP_DIR" -maxdepth 1 -name "zymos_*.sql.gz" -mtime +"$KEEP_DAYS" -delete
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Limpieza completada (retención: ${KEEP_DAYS}d diarios, ${KEEP_WEEKS}w semanales)"

# ── Verificar integridad del backup ──────────────────────────────────────────
if gzip -t "$FILEPATH" 2>/dev/null; then
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] Integridad OK ✓"
else
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: backup corrupto — verificar manualmente" >&2
  exit 1
fi

unset PGPASSWORD
echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup finalizado correctamente."
