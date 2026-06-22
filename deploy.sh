#!/usr/bin/env bash
set -euo pipefail

# ── Configuración ────────────────────────────────────────────────────────────
BACKUP_DIR="${BACKUP_DIR:-./backups}"
HEALTH_URL="http://localhost:8080/actuator/health"
REQUIRED_VARS=(DB_USERNAME DB_PASSWORD JWT_SECRET ADMIN_USERNAME ADMIN_PASSWORD)

# ── 1. Validar variables de entorno requeridas ───────────────────────────────
echo "▶ Validando variables de entorno..."
MISSING=()
for var in "${REQUIRED_VARS[@]}"; do
  [[ -z "${!var:-}" ]] && MISSING+=("$var")
done
if [[ ${#MISSING[@]} -gt 0 ]]; then
  echo "❌ Variables obligatorias no definidas: ${MISSING[*]}"
  echo "   Definirlas en el archivo .env o como variables de entorno del sistema."
  exit 1
fi
echo "✅ Variables OK"

# ── 2. Backup de base de datos ───────────────────────────────────────────────
mkdir -p "$BACKUP_DIR"
BACKUP_FILE="$BACKUP_DIR/pre_deploy_$(date +%Y%m%d_%H%M%S).sql.gz"
echo "▶ Creando backup → $BACKUP_FILE"
if docker compose ps db --status running | grep -q "running"; then
  docker compose exec -T db pg_dump -U "$DB_USERNAME" trazabilidad_cervezas \
    | gzip > "$BACKUP_FILE"
  echo "✅ Backup completado"
else
  echo "⚠️  Contenedor db no está corriendo — omitiendo backup"
fi

# ── 3. Marcar imagen anterior para rollback manual ───────────────────────────
PREV_IMAGE_ID=$(docker compose images -q app 2>/dev/null | head -1 || echo "")
if [[ -n "$PREV_IMAGE_ID" ]]; then
  docker tag "$PREV_IMAGE_ID" zymos:previous 2>/dev/null || true
  echo "▶ Imagen anterior marcada como zymos:previous"
fi

# ── 4. Build ─────────────────────────────────────────────────────────────────
echo "▶ Construyendo nueva imagen..."
docker compose build --no-cache app
echo "✅ Build completado"

# ── 5. Deploy ────────────────────────────────────────────────────────────────
echo "▶ Desplegando..."
docker compose up -d --no-deps app

# ── 6. Health check (máx 120s) ───────────────────────────────────────────────
echo "▶ Esperando health check..."
HEALTHY=false
for i in $(seq 1 24); do
  sleep 5
  HTTP_STATUS=$(curl -sfo /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null || echo "000")
  if [[ "$HTTP_STATUS" == "200" ]]; then
    HEALTH_BODY=$(curl -sf "$HEALTH_URL" 2>/dev/null || echo "")
    if echo "$HEALTH_BODY" | grep -q '"status":"UP"'; then
      HEALTHY=true
      echo "✅ App UP después de $((i * 5))s"
      break
    fi
  fi
  echo "   Intento $i/24 — HTTP $HTTP_STATUS..."
done

# ── 7. Resultado ─────────────────────────────────────────────────────────────
if [[ "$HEALTHY" == "true" ]]; then
  # Limpiar backups viejos (conservar los últimos 5)
  ls -t "$BACKUP_DIR"/*.sql.gz 2>/dev/null | tail -n +6 | xargs rm -f 2>/dev/null || true
  echo ""
  echo "╔══════════════════════════════════════╗"
  echo "║   ✅ Deploy completado exitosamente  ║"
  echo "╚══════════════════════════════════════╝"
else
  echo ""
  echo "╔══════════════════════════════════════════════════════════╗"
  echo "║   ❌ Health check falló — el deploy puede estar roto    ║"
  echo "╚══════════════════════════════════════════════════════════╝"
  echo ""
  echo "Logs recientes:"
  docker compose logs --tail=50 app
  echo ""
  echo "Para rollback manual:"
  echo "  docker compose stop app"
  echo "  docker tag zymos:previous zymos-app:latest"
  echo "  docker compose up -d --no-deps app"
  echo ""
  echo "Backup disponible en: $BACKUP_FILE"
  exit 1
fi
