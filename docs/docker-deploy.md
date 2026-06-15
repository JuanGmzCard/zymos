## ASISTENTE CLI (`assistant/`)

Herramienta TypeScript independiente que usa `@anthropic-ai/sdk` como asistente de desarrollo para el proyecto Alera.

- **Ubicación**: `assistant/` — proyecto Node.js separado, **no forma parte del build Maven**
- **Uso**: `cd assistant && npm install && npm run dev`
- **Funcionamiento**: Lee `../CLAUDE.md` al arrancar → lo envía como system prompt con `cache_control: ephemeral` (prompt caching de Anthropic) → chat interactivo en terminal con streaming
- **Comandos internos**: `salir`, `limpiar` (nueva conversación), `recargar` (recarga CLAUDE.md en caliente sin reiniciar)
- **Modelo**: `claude-opus-4-7` con prompt caching — el system prompt solo se procesa una vez por sesión
- El `node_modules/` y `dist/` están en `.gitignore` — regenerar con `npm install` dentro de `assistant/`

---

## DOCKER

```bash
# Levantar con Docker Compose
# Copiar plantilla de variables: cp .env.Zymos .env  (luego completar contraseñas)
docker compose up --build

# Variables de entorno en .env (ver .env.Zymos como plantilla)

# ── Base de datos — roles con mínimo privilegio (ver db_security.sql) ──
DB_URL=jdbc:postgresql://localhost:5432/trazabilidad_cervezas
DB_USERNAME=zymos_app           # rol solo DML — creado por db_security.sql
DB_PASSWORD=<contraseña_fuerte>

FLYWAY_USERNAME=zymos_flyway    # rol DDL para migraciones — creado por db_security.sql
FLYWAY_PASSWORD=<contraseña_flyway>

# ── Usuario administrador inicial de la app ──
ADMIN_USERNAME=admin
ADMIN_PASSWORD=<contraseña_admin>

# ── Usuarios adicionales por rol (opcionales) ──
PRODUCCION_USERNAME=produccion
PRODUCCION_PASSWORD=<pwd>
INVENTARIO_USERNAME=inventario
INVENTARIO_PASSWORD=<pwd>
FACTURACION_USERNAME=facturacion
FACTURACION_PASSWORD=<pwd>
EQUIPOS_USERNAME=equipos
EQUIPOS_PASSWORD=<pwd>

# JWT — obligatorio en producción (docker-compose falla si no está definido)
JWT_SECRET=<string_aleatorio_minimo_32_chars>
JWT_TTL_HOURS=24

# Multi-tenant
DEFAULT_SUBDOMAIN=default

# Branding (ver .env.Zymos para valores por defecto de Zymos)
APP_BRAND_NAME=Zymos
APP_BRAND_TAGLINE=Sistema de Gestión y Trazabilidad Integral
APP_BRAND_LOGO_URL=
APP_BRAND_COLOR_NAVBAR=#1e293b
APP_BRAND_COLOR_PRIMARY=#2563eb
APP_BRAND_COLOR_ACCENT=#0ea5e9
APP_BRAND_COLOR_ACCENT_HOVER=#38bdf8
APP_BRAND_COLOR_CREAM=#f8fafc
APP_BRAND_COLOR_BODY_BG=#f1f5f9
APP_BRAND_FONT_HEADINGS=Inter
APP_BRAND_FONT_BODY=Roboto
```

- Build multi-etapa: `maven:3.9-eclipse-temurin-21` + `eclipse-temurin:21-jre-alpine`
- Healthcheck: `GET /actuator/health` cada 30s (requiere `spring-boot-starter-actuator`)
- Docker activa automáticamente `SPRING_PROFILES_ACTIVE=prod` → usa `application-prod.properties` (sin fallbacks de credenciales BD ni JWT — la app falla al iniciar si `DB_PASSWORD` o `JWT_SECRET` no están definidos)
- Desarrollo local: `application.properties` mantiene fallbacks (ej: `DB_PASSWORD:12345`, `JWT_SECRET:zymos-dev-secret-key-change-in-production-2024`) para arrancar sin variables de entorno
- **Usuario no-root**: la imagen de producción crea usuario/grupo `zymos` y corre el proceso como ese usuario (principio de mínimo privilegio)
- **Logging**: `logback-spring.xml` — perfil `!prod` con consola colorizada DEBUG; perfil `prod` con stdout estructurado, raíz en WARN, `com.alera` en INFO
- **Seguridad BD — roles PostgreSQL** (`db_security.sql` en raíz del proyecto): ejecutar UNA VEZ en el servidor PostgreSQL de producción (`psql -U postgres -d trazabilidad_cervezas -f db_security.sql`). Crea dos roles con mínimo privilegio: `zymos_app` (solo DML — usado por HikariCP) y `zymos_flyway` (DDL completo — usado solo por Flyway en cada deploy). `ALTER DEFAULT PRIVILEGES FOR ROLE zymos_flyway` garantiza que `zymos_app` reciba DML automáticamente en tablas creadas por migraciones futuras. Cambiar contraseñas placeholder con `\password zymos_app` y `\password zymos_flyway` tras ejecutar el script.
- **Plantilla de entorno**: `.env.Zymos` en raíz del proyecto — copiar a `.env` y completar contraseñas antes del primer deploy.

---

## DEPLOY (`deploy/`)

Archivos de infraestructura en el directorio `deploy/` (no forman parte del build Maven):

### `deploy/nginx.conf` — Reverse proxy HTTPS
- Reverse proxy nginx para producción. Soporta wildcard `*.tudominio.com` → multi-tenant por subdominio.
- Pasa `Host` intacto a Spring Boot → `TenantFilter` resuelve el tenant por subdominio.
- Redirige HTTP → HTTPS (301). TLS 1.2/1.3 con OCSP stapling.
- Cache agresiva (30d) para assets estáticos (css/js/img/fonts).
- `/actuator/` solo accesible desde red interna (127.0.0.1, 10.x, 172.16.x, 192.168.x).
- `client_max_body_size 20M` — necesario para importación de Excel en `/admin/migracion`.
- **Configurar**: reemplazar `tudominio.com` por el dominio real, ajustar rutas de certificados Let's Encrypt.
- **Uso**: `cp deploy/nginx.conf /etc/nginx/sites-available/zymos && ln -s ... /sites-enabled/zymos && nginx -t && systemctl reload nginx`

### `deploy/backup.sh` — Backup diario de PostgreSQL
- Genera dump comprimido (`pg_dump | gzip -9`) con timestamp: `zymos_{DB_NAME}_{YYYYMMDD_HHMMSS}.sql.gz`.
- **Backups diarios**: retención configurable via `BACKUP_KEEP_DAYS` (def: 7). Limpieza automática con `find -mtime`.
- **Backups semanales**: cada domingo copia el dump a `backups/weekly/`, retención `BACKUP_KEEP_WEEKS` (def: 4 semanas).
- **Verificación de integridad**: `gzip -t` tras cada backup — sale con error si el archivo está corrupto.
- Carga `.env` automáticamente si existe en la raíz del proyecto (`set -a && source .env && set +a`).
- Variables: `DB_HOST` (def: localhost), `DB_PORT` (def: 5432), `DB_NAME` (def: trazabilidad_cervezas), `DB_USERNAME` (obligatorio), `DB_PASSWORD` (obligatorio), `BACKUP_DIR` (def: `./backups`), `BACKUP_KEEP_DAYS`, `BACKUP_KEEP_WEEKS`.
- **Cron diario a las 2 AM**: `0 2 * * * /ruta/al/proyecto/deploy/backup.sh >> /var/log/zymos-backup.log 2>&1`
- **Uso**: `chmod +x deploy/backup.sh && ./deploy/backup.sh`
- Usa `zymos_flyway` (acceso completo a la BD) como usuario recomendado para el dump.

