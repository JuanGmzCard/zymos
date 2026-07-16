# Zymos — Sistema de Gestión de Trazabilidad de Cerveza Artesanal

Zymos es una plataforma SaaS multi-tenant desarrollada con Spring Boot 3.4.4 + Java 21 (runtime Java 26) + Thymeleaf + PostgreSQL.
Sistema de gestión integral para cervecerías artesanales. **Nota**: "Alera" es el nombre de uno de los tenants; la infraestructura del proyecto se llama Zymos.
**Arquitectura multi-tenant SaaS**: una misma instancia sirve a múltiples clientes aislados por subdominio (`cliente.app.com`). Cada tenant tiene sus propios datos y branding.

---

## STACK TECNOLÓGICO

- Spring Boot 3.4.4, Java 21 (ejecutado en OpenJDK 26.0.1), Maven
- Spring Data JPA (Hibernate 6.x), Spring Security (BCrypt), @EnableMethodSecurity
- Spring Boot Actuator (health, metrics, flyway, loggers, prometheus)
- Spring Cache + Caffeine (cache en memoria con TTL configurable)
- Thymeleaf 3.1.x, Bootstrap 5.3.3, Bootstrap Icons 1.11.3, Chart.js 4.4.3, FullCalendar 6.1.10
- PostgreSQL (driver 42.x), Flyway 10.x para migraciones
- Lombok 1.18.46 (override — Spring Boot 3.4.x gestiona 1.18.36, se sobreescribe para Java 26)
- MapStruct 1.5.5.Final — generación de mapeos entidad↔DTO en tiempo de compilación
- SpringDoc OpenAPI 2.8.3 — documentación automática de la API REST (`/swagger-ui.html`)
- Micrometer + Prometheus — métricas de producción (`/actuator/prometheus`). **Stack de monitoreo en Docker**: Prometheus v2.53.0 + Grafana v11.1.0 (`monitoring/`). Dashboard "Zymos — Overview" auto-provisionado con 12 paneles (HTTP traffic, latencia p50/p95/p99, JVM heap, HikariCP pool, CPU, logback events).
- OpenPDF 1.3.43 (`com.github.librepdf`) — generación de PDF (licencia LGPL/Apache). Clases en `com.lowagie.text.*`
- Spring Boot Starter Mail — envío de emails HTML vía SMTP. `JavaMailSender` solo se auto-configura si `spring.mail.host` está definido (no vacío). `EmailService` usa `@Autowired(required = false)` para soportar entornos sin SMTP.
- Apache POI 5.2.5 (`poi-ooxml`) — generación de Excel .xlsx. Clases en `org.apache.poi.xssf.usermodel.*`
- JJWT 0.12.6 (`jjwt-api` + `jjwt-impl` + `jjwt-jackson`) — generación y validación de tokens JWT HS256 para la API REST
- Sentry 7.14.0 (`sentry-spring-boot-starter-jakarta`) — error tracking multi-tenant. Deshabilitado si `SENTRY_DSN` está vacío. Cada evento incluye tag `tenant` (subdomain activo) y `user.username`. Ver configuración en `SentryConfig.java`.
- SignaturePad 4.1.7 (CDN: `cdn.jsdelivr.net/npm/signature_pad@4.1.7/dist/signature_pad.umd.min.js`) — firma digital canvas en módulo BPM. Integrado vía `static/js/bpm-firma.js`
- JUnit 5 + Mockito (unitarios) + Testcontainers (integración con PostgreSQL real)
- SpotBugs 4.8.6.4 (`spotbugs-maven-plugin`) — análisis estático de bytecode. Exclusiones en `spotbugs-exclude.xml` (EI_EXPOSE_REP de Lombok + clases `*Impl` de MapStruct). **Solo corre en CI (Java 21)** — ASM no soporta class file v70 (JDK 26 local).
- Tipografías: Cinzel (headings), Raleway (body)
- `spring.thymeleaf.cache=false` | `spring.jpa.hibernate.ddl-auto=validate`

## CONFIGURACIÓN

- Puerto: 8080
- BD: PostgreSQL localhost:5432/trazabilidad_cervezas
- Credenciales via variables de entorno: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`
- Usuarios adicionales por rol (opcionales): `PRODUCCION_USERNAME/PASSWORD`, `INVENTARIO_USERNAME/PASSWORD`, `FACTURACION_USERNAME/PASSWORD`, `EQUIPOS_USERNAME/PASSWORD`
- Flyway: `baseline-on-migrate=true`, migraciones en `db/migration/` (V1–V71). En producción usa credenciales separadas: `FLYWAY_USERNAME=zymos_flyway` / `FLYWAY_PASSWORD` (rol con DDL); si no se definen, usa `DB_USERNAME`/`DB_PASSWORD` como fallback. Ver `db_security.sql` para crear los roles.
- Sesión: timeout 30 minutos de inactividad (`server.servlet.session.timeout=30m`)
- Docker: `Dockerfile` + `docker-compose.yml` disponibles en raíz del proyecto
- **Monitoreo** (solo en Docker — opcional): Prometheus `localhost:9090`, Grafana `localhost:3000`. Vars: `GRAFANA_ADMIN_USER` (def: admin), `GRAFANA_ADMIN_PASSWORD` (def: admin — cambiar en prod). Prometheus usa `ADMIN_USERNAME/PASSWORD` para autenticarse en `/actuator/prometheus`. Archivos en `monitoring/`: template `prometheus/prometheus.yml`, script `prometheus/docker-entrypoint.sh` (sustituye credenciales via `sed`), `grafana/provisioning/` (datasource + dashboard auto-provisionado).
- **CI/CD**: `.github/workflows/ci.yml` — dos jobs paralelos en cada push: `Build & Test` (Java 21 + Postgres para Testcontainers) y `SpotBugs` (Java 21, sin Postgres, `mvn compile spotbugs:check`). **Dependabot**: `.github/dependabot.yml` — revisa Maven y GitHub Actions semanalmente con grupos `spring-boot`, `testcontainers`, `security`.
- Actuator: `GET /actuator/health` (público), `/actuator/**` solo ADMIN
- Swagger UI: `GET /swagger-ui.html` (requiere autenticación)
- Paginación configurable: `app.page-size=15` (servicios), `app.log-page-size=25` (LogAccesoService)
- Perfil prod: `application-prod.properties` — elimina fallbacks de credenciales BD; agrega cookies seguras (`secure=true`, `http-only=true`, `same-site=Strict`), `thymeleaf.cache=true`, HikariCP pool mayor (`maximum-pool-size=${DB_POOL_SIZE:20}`, `minimum-idle=5`). Docker activa `SPRING_PROFILES_ACTIVE=prod`.
- **HikariCP base** (en `application.properties`, sobreescrito por prod): pool `ZymosPool`, `maximum-pool-size=10`, `minimum-idle=2`, `connection-timeout=20000`, `idle-timeout=300000`, `max-lifetime=1200000`.
- **Multi-tenant**: `app.default-subdomain=${DEFAULT_SUBDOMAIN:default}` — subdomain usado en localhost y como tenant inicial
- **Branding por tenant** (env vars con fallback): `APP_BRAND_NAME` (def: Zymos), `APP_BRAND_TAGLINE`, `APP_BRAND_LOGO_URL` (def: vacío — muestra ícono de gota), `APP_BRAND_COLOR_NAVBAR` (def: `#1e293b`), `APP_BRAND_COLOR_PRIMARY` (def: `#2563eb`), `APP_BRAND_COLOR_ACCENT` (def: `#0ea5e9`), `APP_BRAND_COLOR_ACCENT_HOVER` (def: `#38bdf8`), `APP_BRAND_COLOR_CREAM` (def: `#f8fafc`), `APP_BRAND_COLOR_BODY_BG` (def: `#f1f5f9`), `APP_BRAND_FONT_HEADINGS` (def: Inter), `APP_BRAND_FONT_BODY` (def: Roboto). Los defaults se aplican al tenant `default` al arrancar (via `DataInitializer`); para cambiarlos en BD sin reiniciar usar `/admin/tenants/editar/default` + "Limpiar cache".
- **Email/Alertas** (opcionales — si no se definen, las notificaciones quedan deshabilitadas): `SMTP_HOST`, `SMTP_PORT` (def: 587), `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH` (def: true), `SMTP_STARTTLS` (def: true), `SMTP_FROM` (def: noreply@zymos.app), `APP_BASE_URL` (def: http://localhost:8080), `ALERT_CRON` (def: `0 0 8 * * MON-FRI`), `ALERT_VENCIMIENTO_DIAS` (def: 30), `FACTURAS_ALERTA_DIAS` → `app.facturas.alerta-dias` (def: 30) — días sin procesar para disparar alerta de facturas RECIBIDA/VERIFICADA
- **Cotizaciones**: `app.cotizacion.expiracion-dias=${COTIZACION_EXPIRACION_DIAS:15}` — días de validez por defecto al crear una cotización (si no se especifica fecha explícita)
- **Protección contra fuerza bruta**: `LOGIN_MAX_INTENTOS` (def: 5), `LOGIN_BLOQUEO_MINUTOS` (def: 15)
- **Rate limiting API**: `app.api.rate-limit=${API_RATE_LIMIT:100}` — máximo de peticiones a `/api/**` por IP en ventana fija de 1 minuto. Implementado en `ApiRateLimitFilter` con Caffeine (`expireAfterWrite`). Devuelve HTTP 429 con `{error:"Rate limit exceeded"}` al excederse.
- **JWT API**: `JWT_SECRET` (obligatorio en prod — sin fallback en `application-prod.properties`; en dev usa `zymos-dev-secret-key-change-in-production-2024`), `JWT_TTL_HOURS` (def: 24). Configurado en `app.jwt.secret` y `app.jwt.ttl-hours`.
- **Sentry — error tracking** (opcional): `SENTRY_DSN` (vacío → Sentry deshabilitado; en prod definir con el DSN del proyecto en sentry.io), `SENTRY_RELEASE` (def: `1.0.0` — pasar git SHA en CI para rastrear qué versión causó el error). Configurado en `SentryConfig.java` con dos beans: `EventProcessor` (agrega tag `tenant` desde `TenantContext` a cada evento) y `SentryUserProvider` (adjunta `username` del usuario autenticado vía `SecurityContextHolder`). `GlobalExceptionHandler` llama `Sentry.captureException(ex)` tanto en el handler de 500 (`Exception`) como en el de 400 (`RuntimeException`). `sentry.environment` es `dev` por defecto y `prod` en `application-prod.properties`.

---

## PALETA DE COLORES (style.css)

- Verde oscuro: `#242E0D` (navbar) — CSS var `--verde-oscuro`
- Verde Alera: `#364318` (hero, section-headers) — CSS var `--verde-alera`
- Dorado: `#C9A028` (acento principal) — CSS var `--dorado`
- Dorado claro: `#E0B840` (hover) — CSS var `--dorado-claro`
- Crema: `#F5EDD0` (texto sobre fondos oscuros) — CSS var `--crema`
- Fondo body: `#F0EDE2` — CSS var `--fondo`
- Dark mode: fondo `#0f172a` (slate oscuro), cards `#1e293b`, inputs `#334155`, texto `#e2e8f0` — activado con clase `html.dark-mode`. **Paleta slate/azul-Zymos** (migrada de verde-Alera el 2026-07-06). Variables centralizadas `--dm-*` en `style.css` (bloque `:root`): `--dm-bg` (#0f172a), `--dm-bg-deep` (#080d1a), `--dm-card` (#1e293b), `--dm-input` (#334155), `--dm-input-focus` (#3d4f67), `--dm-text` (#e2e8f0), `--dm-text-strong`, `--dm-text-faint`, `--dm-text-muted`, `--dm-text-soft`, `--dm-text-dim`, `--dm-text-dimmer`, `--dm-border` (rgba slate 20%), `--dm-border-heavy`, `--dm-border-med`, `--dm-border-light`, `--dm-border-faint`, `--dm-border-focus` (blue focus), `--dm-hover`, `--dm-verde-bg` (blue accent rgba 12%), `--dm-verde-faint`, `--dm-verde-border`. Los templates con `<style>` inline propio incluyen también un bloque `html.dark-mode` local al final de ese `<style>`, usando las vars `--dm-*`.
- Componentes clave en `style.css` (globales, no redefinir en `<style>` inline): `.phase-badge`, `.detail-label`, `.detail-value`, `.ingrediente-chip`, `.densidad-box`, `.fase-col`, `.comparativa-box`, `.kanban-card`, `.kanban-col`, `.badge-role` (pill dorado para rol de usuario en navbar), `.fase-pill` (6 variantes en `trazabilidad/index.html` con dark mode), `.kanban-col-header` (dark mode por columna con colores de fase usando `!important` sobre inline styles), `.wz-tab.done` (tab wizard completado — círculo verde con ✓ via CSS `::after { content:'✓' }`)
- **Componentes UI modernos slate/azul** — definidos localmente en el `<style>` de cada template (NO en `style.css`). Todos incluyen bloque `html.dark-mode { ... }` al final usando vars `--dm-*`:
  - `.page-header` — cabecera de página: `padding:1.75rem 0 1rem`; h1 `font-size:1.5rem; font-weight:700; color:#1e293b`; `.subtitle` en `#94a3b8; font-size:0.875rem`. Siempre en `<div class="container-fluid px-4">` propio, antes del `container-fluid px-4 pb-4` del contenido.
  - `.stat-card-simple` — tarjeta de métrica compacta: `background:#fff; border:1px solid #e2e8f0; border-radius:8px; padding:1rem 1.5rem; min-width:130px`. Sub-elementos: `.stat-label` (`font-size:0.7rem; font-weight:600; text-transform:uppercase; color:#94a3b8`) y `.stat-value` (`font-size:1.75rem; font-weight:700; color:#1e293b`) con variantes de color `.success` (#16a34a), `.info` (#0284c7), `.warning` (#f97316), `.muted` (#64748b). Agrupadas en `<div class="d-flex flex-wrap gap-3 mb-4">`.
  - `.filter-card` — panel de filtros: `background:#fff; border:1px solid #e2e8f0; border-radius:8px; padding:1rem 1.25rem`. Contiene forms y filter-tabs.
  - `.filter-tab` — píldora de filtro activo/inactivo: `border-radius:20px; font-size:0.8rem`. `.active` usa `background:var(--verde-zymos)` o azul fijo; `.inactive` es transparente con hover `#f1f5f9`.
  - `.table-card` — contenedor de tabla con header: `background:#fff; border:1px solid #e2e8f0; border-radius:8px; overflow:hidden`. Sub-elemento `.table-count` o `.table-title` con `border-bottom:1px solid #e2e8f0`. `thead th`: `background:#f8fafc !important; color:#64748b !important; font-size:0.7rem`. `tbody td`: `padding:0.75rem 1rem; border-bottom:1px solid #f1f5f9; color:#334155`. Hover: `background:#f8fafc`.
  - `.section-box` — variante de `.table-card` para secciones con contenido mixto (ej: `trazabilidad/detalle.html`): mismo borde/fondo/radius. Sub-elementos `.sh` (header con padding, `border-bottom`) y `.sb` (body con padding `1.25rem`). Útil cuando una sección combina datos con padding y tablas edge-to-edge.
  - `.chart-card` — tarjeta de gráfico: igual que `.table-card`. Sub-elemento `.chart-card-header` con `font-size:0.8rem; font-weight:600; color:#475569; border-bottom`.
  - **Regla de botones en templates modernos**: usar `btn-primary btn-sm` (acción principal), `btn-outline-secondary btn-sm` (acciones secundarias). NUNCA `btn-zymos`, `btn-outline-crema` ni `btn-zymos-outline` en templates rediseñados.
- **Botones en card-header oscuro** (solo en `.card-zymos` legacy): `.card-zymos .card-header` usa gradiente oscuro. Botones dentro DEBEN usar `btn-outline-crema`. NUNCA `btn-zymos-outline` ahí. **Excepción — `dashboard.html`**: la única sección que aún usa `.card-zymos` es Planificación (card-header oscuro con botón "Ver calendario" → `btn-outline-secondary btn-sm`). Las secciones Finanzas y Últimos Lotes fueron migradas a `.table-card` (estilo moderno slate/azul).
- **Multi-tenant**: el navbar fragment inyecta `<style th:inline="text">:root { --verde-oscuro: [[${branding.colorNavbar}]]; --verde-zymos: [[${branding.colorPrimary}]]; --verde-alera: [[${branding.colorPrimary}]]; ... }</style>` que sobrescribe las variables CSS del archivo. `login.html` hace lo mismo en su `<head>`. Los templates NO cambian — siguen usando `${branding.*}` y las CSS vars son transparentes. **CRÍTICO**: `--verde-alera` y `--verde-zymos` apuntan ambas a `branding.colorPrimary` — los encabezados de tabla (`<thead style="background:var(--verde-alera)">`) toman automáticamente el color primario del tenant.
- **Colores hardcodeados — regla**: NUNCA usar hex fijos (`#364318`, `#F5EDD0`, `#C9A028`, etc.) en templates HTML. Usar siempre las CSS vars: `var(--verde-alera)`, `var(--crema)`, `var(--dorado)`, `var(--dorado-claro)`, `var(--verde-oscuro)`. Excepción: `emails/alertas.html` (clientes de email no soportan CSS vars) y fallbacks de JS del patrón `getComputedStyle(...) || '#hex'`. Las `rgba(...)` tampoco pueden usar CSS vars directamente — usar `color-mix(in srgb, var(--dorado) XX%, transparent)` como alternativa moderna que sí acepta CSS vars y genera el equivalente a `rgba(C9A028, XX%)`. Soportado en Chrome 111+, Firefox 113+, Safari 16.2+. Ejemplo en `navbar.html`: `border: 1px solid color-mix(in srgb, var(--dorado) 35%, transparent)`.

---

## MANEJO DE EXCEPCIONES

`GlobalExceptionHandler` (`@ControllerAdvice`) — orden de prioridad:

| Excepción | HTTP | Comportamiento |
|---|---|---|
| `NoHandlerFoundException`, `NoResourceFoundException` | 404 | Vista `error/error` genérica |
| `EntityNotFoundException` | 404 | Vista `error/error` — "Registro no encontrado" |
| `EquipoEnUsoException` | — | Vista `error/error` con link "Volver a Equipos" |
| `LoteNoEncontradoException` | 404 | Vista `error/error` con link "Volver a Lotes" |
| `RuntimeException` | 400 | Vista `error/error` — último recurso, muestra mensaje |
| `Exception` | 500 | Vista `error/error` — error interno |

- `LoteNoEncontradoException` en `com.alera.exception` — lanzada por `TrazabilidadService` en `buscarPorId`, `actualizar` y `eliminar`
- **IMPORTANTE**: `LoteNoEncontradoException` debe declararse ANTES de `RuntimeException` en el handler (Spring usa el tipo más específico; el orden es por precedencia de clase)

---

## REGLAS DE NEGOCIO IMPORTANTES

1. **Disponibilidad fermentadores**: disponible cuando no hay lote activo con `carbFechaInicial = NULL` asignado.
1b. **Prerequisito de recetas para crear lote**: `GET /` (TrazabilidadController) redirige a `redirect:/` con flash `sinRecetas=true` si `recetaService.listarActivas().isEmpty()`. `trazabilidad/index.html` muestra un banner de alerta con link a `/recetas/nueva` cuando `${sinRecetas}` es true. Ruta correcta del formulario de recetas: `/recetas/nueva` (femenino) — NO `/recetas/nuevo`.
2. **Generación código lote**: 3 primeras letras del estilo → "IPA" → IPA-001, IPA-002...
3. **Descuento automático inventario**: al crear/actualizar/eliminar lote. Retorna advertencias si stock insuficiente (no bloquea). Al crear/editar facturas, el inventario se actualiza automáticamente (suma en guardar, revierte + suma en actualizar, revierte en eliminar).
4. **Normalización unidades** (via UnidadUtils): kg→gr (×1000), L→mL (×1000), gal→mL (×3785.41).
5. **IVA por ítem**: facturas no tienen IVA global. Cada ítem tiene su propio `porcentajeIvaItem`.
6. **Sincronización facturas**: al actualizar/eliminar, revierte inventario anterior antes de aplicar cambios.
7. **Vencimientos**: alerta ≤30 días en dashboard.
8. **Thymeleaf — CRÍTICO**: `eq`, `ne`, `lt`, `gt`, `le`, `ge` son palabras reservadas. NO usar como variables de iteración en `th:each`. Síntoma: `Iteration variable cannot be null` en `Each.<init>` al renderizar el template. Casos reales: `th:each="eq, stat : ${...}"` en `emails/alertas.html` → renombrado a `equipo` (2026-06-07); `th:each="eq : ${equipos}"` en `busqueda.html` → renombrado a `equipo` (2026-07-15). La variable sin `, stat` también falla.
8b. **Thymeleaf — colecciones en SpEL**: para filtrar una colección en `th:if` usar el operador de selección SpEL `?[]` en lugar de lambdas Java: `${!#lists.isEmpty(lista.?[campo != null and !campo.isEmpty()])}`. Evita dependencia de la versión exacta de SpEL y es más legible. Alternativa en `th:each` con `th:if` anidado.
8c. **Thymeleaf — `th:onclick` con Strings bloqueado en 3.1+**: Thymeleaf 3.1 rechaza expresiones que devuelven strings en atributos de event handlers DOM (`th:onclick`, `th:onchange`, etc.) por seguridad XSS. Solo se permiten números y booleanos. Solución: usar atributos `data-*` con `th:attr` y leer desde un handler JS con `this.dataset.*`. Ejemplo — en lugar de `th:onclick="'abrirModal(' + ${p.nombre} + ')'"` usar `th:attr="data-nombre=${p.nombre}" onclick="abrirModalDesdeBtn(this)"` con `function abrirModalDesdeBtn(btn) { abrirModal(btn.dataset.nombre); }`.
8d. **Thymeleaf — `th:with` y `th:if` en el mismo elemento**: `th:if` (precedencia 40) se procesa ANTES que `th:with` (precedencia 600). Una variable definida con `th:with` en el mismo elemento es `null` cuando `th:if` la evalúa → `SpelEvaluationException: cannot convert from null to boolean`. Solución: nunca usar en `th:if` una variable del `th:with` del mismo elemento; en su lugar, repetir la expresión inline en `th:if`, o poner `th:with` en un elemento padre contenedor.
8e. **Thymeleaf — `#{}` NUNCA dentro de `${}`**: `#{}` es una expresión Thymeleaf de mensajes; `${}` es SpEL. SpEL no reconoce `#{}` y lanza `SpelParseException: Unexpected token 'lcurly({)'`. Error típico: `${valor != null ? valor : #{clave}}`. Solución: mover el ternario al nivel Thymeleaf: `th:text="${valor != null} ? ${valor} : #{clave}"`. Con helpers: `${cond} ? ${#temporals.format(campo,'...')} : #{clave}`. Con keys parametrizadas: `${cond} ? #{admin.inact.dias(${r.diasInactivo})} : #{admin.inact.sin.reg}` — el `#{}` puede recibir `${}` como argumento siempre que el ternario esté al nivel Thymeleaf, no dentro de `${}`. Caso real: `tenants-inactivos.html` (2026-07-15) — página completamente inaccesible por SpelParseException.
9. **PostgreSQL + Hibernate**: pasar `""` en lugar de `null` en parámetros JPQL String para evitar error `lower(bytea)`.
10. **Native queries + Spring Data**: NO usar `::int` — interpreta `:int` como parámetro. Usar `CAST(... AS integer)`.
11. **Naming strategy**: campos con sola mayúscula final (ej: `temperaturaC`) requieren `@Column(name="temperatura_c")` explícito.
12. **Densidades OG/FG — formato XXXX**: `densidadInicial`, `densidadFinal` (LoteCerveza) y `ogObjetivo`, `fgObjetivo` (Receta) son `Integer`. El valor 1.056 se guarda como 1056. NO usar `step="any"` ni `BigDecimal` para densidades. Rangos de validación:
    - OG (`densidadInicial`): `@Min(1000)` / `@Max(1150)` — cubre desde mosto base hasta barleywines/imperial stouts extremos. HTML: `min="1000" max="1150"`.
    - FG (`densidadFinal`): `@Min(990)` / `@Max(1060)` — permite densidades bajo 1.000 (alta atenuación) hasta cervezas muy dulces. HTML: `min="990" max="1060"`.
    - Inputs usan `step="1"` en ambos campos.
13. **Fórmula ABV con densidades XXXX**:
    - Java: `(OG - FG) * 131.25 / 1000.0`
    - Thymeleaf: `${(OG - FG) * 0.13125}`
    - JavaScript: `(og - fg) * 0.13125`
    - **NUNCA** usar `* 131.25` directamente con densidades en formato XXXX.
14. **Comparativa exactitud densidad**: threshold cambiado de `<= 0.005` (BigDecimal) a `<= 5` (Integer — puntos de gravedad). En Thymeleaf: `(a - b <= 5) and (b - a <= 5)` para abs(a-b) ≤ 5.
15. **Historial lotes**: `HistorialLote` sin FK intencionalmente — preserva historia tras borrar el lote.
16. **Log accesos**: `LogAccesoService.registrar()` usa `REQUIRES_NEW` — se guarda aunque la tx principal haga rollback.
17. **Proveedores**: campo `activo` (no `activa`) — Spring Data derivado debe ser `findAllByActivoTrue*`.
17c. **InsumoInventario — `cantidad` nullable**: el campo `cantidad` de `InsumoInventario` no tiene `@NotNull` — puede ser null en BD para insumos creados sin stock inicial. Antes de llamar métodos sobre él (`stripTrailingZeros()`, `compareTo()`, etc.) siempre hacer null-check: `i.getCantidad() != null ? i.getCantidad().stripTrailingZeros().toPlainString() : "0"`. Afecta `suggest()`, cálculos de stock y cualquier método que itere insumos sin garantizar cantidad > 0.

17b. **InsumoInventario — campo `tipo` almacena display name**: `InsumoInventario.tipo` guarda el nombre visible del tipo (ej: `"Químico"`, `"Malta"`, `"Lúpulo"`), NO el nombre del enum Java. Fue migrado por V45/V47 desde enum names a los nombres de la tabla `tipos_insumo`. Al filtrar por tipo, usar siempre el display name con tilde y capitalización correcta. Ejemplo: `repo.findAllByTipoOrderByNombreAsc("Químico")` — nunca `"QUIMICO"`.
18. **FacturaProveedor**: tiene dos campos de proveedor: `proveedor` (String original) y `proveedorRef` (FK LAZY nullable). Coexisten para compat. histórica. El campo de fecha es `fechaFactura` — **NO** `fecha`. En JPQL: `f.fechaFactura`; en Java: `getFechaFactura()`. Escribir `f.fecha` en un `@Query` provoca `UnknownPathException` al arrancar. El flag `ivaIncluido` (boolean, default false) indica si los valores unitarios de los ítems ya incluyen IVA — `FacturaItem.getValorUnitarioSinIva()` hace la extracción automáticamente consultando `factura.isIvaIncluido()`.
19. **Fechas en filtros JPQL**: para `LocalDate` nullable usar `CAST(:param AS LocalDate) IS NULL OR campo >= :param` — el `CAST` fuerza el tipo del parámetro y evita el error de PostgreSQL `"no se pudo determinar el tipo del parámetro $N"` (SQLState 42P18) que ocurre cuando el parámetro es `null` sin contexto de tipo. Patrón sin `CAST` (`(:param IS NULL OR ...)`) no funciona con PostgreSQL + Hibernate 6. **Caso real (2026-07-15)**: `FacturaProveedorRepository` tenía 5 queries con el patrón roto — causaban fallo total en el módulo de facturas cuando se accedía sin filtros de fecha (los parámetros son null por defecto). Corregido aplicando `CAST` en los 5 métodos.
20. **AuditableEntity — error de compilación**: si una subclase declara `getCreatedAt()` / `setCreatedAt()` o cualquier getter/setter de los 4 campos auditados, el compilador lanza `createdAt has private access in AuditableEntity`. Solución: eliminar esos métodos de la subclase.
21. **AuditableEntity — @PrePersist incompatible**: no usar `@PrePersist` para setear `createdAt` en entidades que extienden `AuditableEntity`; el campo ya lo maneja `@CreatedDate`. Si coexisten, el valor queda `null` porque Spring Data Auditing no sobreescribe un valor ya seteado.
22. **Cache y @Transactional**: `@Cacheable` en métodos `@Transactional(readOnly=true)` es correcto — en cache hit no se abre transacción; en cache miss la transacción se abre normalmente. Con `spring.jpa.open-in-view=true` (default), lazy loading funciona desde objetos cacheados.
23. **MapStruct + Lombok**: el `mapstruct-processor` debe declararse DESPUÉS de `lombok` en `annotationProcessorPaths` del `maven-compiler-plugin` para garantizar el orden correcto de procesamiento.
24. **Usuarios — roles como enum**: `Usuario.rol` es `RolUsuario` (`@Enumerated(EnumType.STRING)`). No pasar Strings libres; usar `RolUsuario.ADMIN`, `RolUsuario.INVENTARIO`, etc. `DataInitializer` y `UsuarioService.guardar()` ya usan el enum.
25. **Usuarios — auto-protección**: `UsuarioController` comprueba `service.esElMismoUsuario(id, auth.getName())` antes de eliminar, desactivar o cambiar el rol. En el template, esos botones están deshabilitados para la fila del usuario en sesión (verificado con `${usuario.username == #authentication.name}`).
26. **Usuarios — contraseña mínima**: validada en backend (`MIN_PASSWORD_LENGTH = 6`) en `guardar` y `cambiarPassword`. No depender solo del atributo HTML `minlength`.
27. **`RolUsuario.getDisplayName()`**: usar este método en templates (`${rol.displayName}`) para mostrar nombres legibles ("Administrador", "Inventario"...). No hardcodear strings de roles en HTML.
28. **Receta — Hervor y Lúpulo**: `AdicionHervor` almacena adiciones de lúpulos/clarificantes durante el hervor. `minutosRestantes = 0` significa flameout/apagado. Ordenadas descendente por `minutosRestantes` (primeras adiciones al inicio del hervor). El formulario muestra datalist de lúpulos + clarificantes del inventario.
29. **Inventario — unidad como select**: el campo `unidad` en `inventario/formulario.html` es un `<select>` con opciones fijas (gr, kg, mL, L, gal, und). No es input libre.
29b. **Clarificantes — unidad "und"**: el `<select>` de unidad para clarificantes incluye "und" (unidades) además de gr/kg/mL/L/gal, tanto en `recetas/formulario.html` como en `trazabilidad/formulario.html`. En filas estáticas Thymeleaf: `<option th:selected="${clar.unidad == 'und'}" value="und">und</option>`. En filas dinámicas JS: `UNIT_OPTIONS_CLAR` en lugar de `UNIT_OPTIONS`; en `poblarDesdeReceta` se pasa `includePcs=true` cuando `tipo === 'clarificantes'`.
30. **Receta — datalists en formulario**: el campo `estilo` usa datalist de `TipoCerveza` activos. Cada grupo de ingredientes usa datalist del inventario filtrado por tipo. Si el ítem no existe, el botón `⊞` abre un modal de creación rápida vía AJAX.
31. **Factura — datalist dinámico por categoría**: el campo `nombre` de cada ítem usa un datalist compartido (`#datalist-item-nombres`) que se actualiza según tipo+categoría seleccionados. Los mapas `insumosPorTipo` y `equiposPorTipo` (claves = enum name: MALTA, FERMENTADOR…) se serializan como JSON en la página. `actualizarDatalist(tipo, categoria)` busca `INSUMOS_POR_TIPO[categoria]` donde `categoria` es el `value` del select de categoría (enum name). **CRÍTICO**: el mapa del servidor debe usar enum names como claves; si se usan display names ("Malta") el lookup retorna `undefined` y el datalist queda vacío. El botón `⊞` abre modal según el tipo del ítem; `agregarAlDatalist` también usa enum name como clave.
32. **Trazabilidad — Costo de Producción** (activo): asignación a nivel de ítem con cantidad parcial. La sección en `formulario.html` muestra un buscador de ítems de factura (filtrable por nombre/proveedor/tipo) vía AJAX — `GET /suggest-items?q=&tipo=` (`TrazabilidadController.suggestItems`, paginado a 30 resultados, `FacturaItemRepository.search`). Los ítems seleccionados van a `lote.itemsFactura` (OneToMany `LoteItemFactura`). `detalle.html` muestra tabla con factura, proveedor, cantidad asignada y valor proporcional. Cantidad 0 = costo completo del ítem sin ingrediente. **Auto-población al cargar receta**: al hacer click en "Cargar Receta", `verificarStockReceta()` busca el ítem de factura que coincide por nombre con cada ingrediente y pasa `cantidadReceta`/`unidadReceta` en el objeto sugerido; `autoAgregarCostosReceta()` usa esa cantidad convertida a la unidad del ítem de factura como `cantidadAsignada` inicial.
33. **Calculadora de escala (recetas/detalle)**: escala ingredientes (maltas, lúpulos, levaduras, clarificantes), adiciones de hervor, agua macerado y agua sparge. Variables JS: `AGUA_MACERADO`, `UNIDAD_MACERADO`, `AGUA_SPARGE`, `UNIDAD_SPARGE`, `RECETA_ID` (inline Thymeleaf). Al resetear llama `resetAgua()`. Botón "Crear receta escalada" habilitado solo cuando `volObj > 0`; enlaza a `GET /recetas/escalar/{id}?volumen={volObj}` y se construye dinámicamente en JS. `RecetaService.escalarComoFormDto(id, volumenObjetivo)` clona la receta, aplica el factor `volumenObjetivo / volumenBase`, actualiza cantidades y nombre inteligente (reemplaza patrón `XX litros/L` en el nombre, o agrega `" XX L"` al final). **CRÍTICO — BigDecimal en campos de formulario**: NUNCA usar `stripTrailingZeros()` en BigDecimal que se va a renderizar via `th:field` o `th:value`. `new BigDecimal("50").stripTrailingZeros()` produce escala -1 y `toString()` = `"5E+1"`, que numeros.js parsea incorrectamente al hacer submit (ej: `"5E+1"` → `51`). Usar `setScale(2, RoundingMode.HALF_UP)` para campos de formulario; reservar `stripTrailingZeros().toPlainString()` solo para display de solo lectura en templates. Aplica también a cálculos intermedios en servicios que escriben en campos de DTO: `RecetaService.escalarIngredientes()` usaba `stripTrailingZeros()` (2026-07-15) — corregido eliminando esa llamada.
34. **Multi-tenant — @TenantId en todas las entidades**: todas las 17 entidades de datos tienen `@TenantId private String tenantId`. Las 6 que extienden `AuditableEntity` lo heredan; las 11 restantes lo declaran directamente. Hibernate agrega `WHERE tenant_id = :current` automáticamente a todos los SELECT. NO setear `tenantId` manualmente — Hibernate lo gestiona.
35. **Multi-tenant — DataInitializer**: al arrancar, crea el tenant `defaultSubdomain` si no existe (copia todos los campos de `BrandingProperties` incluyendo `fontHeadings` y `fontBody`), luego itera **todos los tenants** en BD. Para cada uno con cero usuarios, crea los usuarios de las env vars (`ADMIN_USERNAME/PASSWORD`, etc.) usando `insertarConTenant` (native SQL) y tipos de cerveza. Los tenants con usuarios ya configurados no se tocan. Esto garantiza que cualquier tenant creado vía UI reciba su admin al reiniciar la app. El método `run()` tiene `@Transactional` porque `insertarConTenant` es `@Modifying` y requiere una transacción activa. **CRÍTICO**: los métodos `@Modifying` en repositorios (`insertarConTenant`, `toggleActivoByIdAndTenantId`, etc.) deben tener `@Transactional` propio si se llaman fuera de un contexto transaccional externo — de lo contrario lanzan `TransactionRequiredException`.
36. **Multi-tenant — agregar cliente nuevo**: (1) crear tenant en `/admin/tenants/nuevo` (UI) o insertar fila en `tenants` directamente en BD; (2) DNS `cliente.tuapp.com` → servidor; (3) crear usuario admin del tenant en `/usuarios` (el contexto de tenant ya estará activo vía subdominio) o con `INSERT INTO usuarios (username, password, rol, activo, tenant_id) VALUES (...)`.
37. **Branding — orden de prioridad**: `Tenant.color*` / `Tenant.font*` > `BrandingProperties` (env vars/properties). `Tenant` se crea con valores de `BrandingProperties` pero puede actualizarse via `/admin/tenants/editar/{subdomain}` o directamente en BD. Tras edición directa en BD, usar "Limpiar cache" en `/admin/tenants` para reflejar cambios sin reiniciar.
37b. **Login page — logo**: sin círculo decorativo. Si `branding.logoUrl` no está vacío, muestra la imagen (`max-height:90px; max-width:240px`). Si está vacío, muestra ícono `bi-droplet-fill`. El campo `logoUrl` acepta URL externa o ruta relativa (`/img/logo.png`) — archivos locales van en `src/main/resources/static/img/`.
38. **@WebMvcTest — seguridad URL-based no se enforce con handler mock**: `ZymosAccessDeniedHandler` mockeado es un no-op (void). Cuando Spring Security lanza `AccessDeniedException` y el handler no comite la respuesta, el request puede llegar al controller. Las pruebas de seguridad URL-based (como "rol no-admin no puede acceder a /nuevo") NO funcionan correctamente en `@WebMvcTest` — deben testearse en integración. Las pruebas `@PreAuthorize` (method-level) SÍ funcionan porque `@EnableMethodSecurity` está activo en `SecurityConfig`.
41. **Tests de aislamiento multi-tenant — NO usar `@Transactional` en el test**: Con `@Transactional` en el test, Spring abre UN EntityManager al inicio del método (cuando TenantContext está vacío). Todos los cambios de TenantContext dentro del test no afectan ese EntityManager — el filtro `@TenantId` usa el tenant capturado al abrir la sesión (null/vacío), lo que hace que las queries no filtren correctamente. Solución: sin `@Transactional` en el test → cada repo call crea su propio EntityManager que captura el TenantContext activo en ese momento. Usar `JdbcTemplate` con SQL explícito para cleanup en `@AfterEach`. Agregar `@Transactional` a los métodos `@Modifying` en el repositorio para que tengan su propia transacción cuando se llaman sin contexto transaccional externo.

40. **Operaciones cross-tenant (admin) — usar SIEMPRE native SQL**: Hibernate añade automáticamente `AND tenant_id = :currentTenant` a TODAS las queries sobre entidades con `@TenantId`, incluso queries JPQL custom con `WHERE u.tenantId = :tenantId` explícito. El `open-in-view` fija el tenant del EntityManager al inicio del request (antes de cualquier swap en el controller). Para operar sobre un tenant distinto al del request activo (ej: admin super-tenant gestionando usuarios de otro tenant), usar `nativeQuery = true` con `tenant_id` como parámetro explícito. Ver `UsuarioRepository`: `findAllByTenantId`, `insertarConTenant`, `toggleActivoByIdAndTenantId`, etc. Intentos fallidos: JPQL custom, `REQUIRES_NEW`, swap de `TenantContext` en controller — ninguno bypasea el filtro Hibernate con open-in-view activo.

39. **@WebMvcTest — httpBasic y status de autenticación**: con `httpBasic()` configurado en `SecurityConfig`, peticiones sin credenciales y sin `Accept: text/html` devuelven `401 Unauthorized` (no `302 redirect`). Las aserciones de tests deben usar `status().isUnauthorized()` para requests no autenticados en endpoints REST.

43. **MapStruct — código generado desactualizado**: `LoteMapperImpl.java` en `target/generated-sources/annotations/` se genera en tiempo de compilación. Si se agregan campos nuevos a la entidad/DTO después de la última compilación, el archivo generado NO los incluirá — los campos simplemente no se mapearán (null silencioso). Síntoma: el formulario de edición muestra vacíos campos que sí se guardaron. Solución: IntelliJ "Build > Rebuild Project" o `mvn compile`. **CRÍTICO**: editar manualmente el archivo generado es solo un workaround temporal — se sobreescribe en la siguiente compilación.

44. **TrazabilidadService — dos rutas de mapeo separadas**: el lote tiene DOS paths de mapeo que deben mantenerse sincronizados al agregar campos:
    - **Carga para editar** (entity→DTO): `LoteMapper.toLoteFormDto()` (MapStruct, auto-generado). Campos simples se mapean por nombre automáticamente. Requiere recompilar cuando se agregan campos.
    - **Guardado** (DTO→entity): `TrazabilidadService.mapearDto()` — mapeo MANUAL con `lote.setXxx(dto.getXxx())`. MapStruct NO se usa aquí. Al agregar campos nuevos, SIEMPRE añadir los setters en `mapearDto()` dentro del bloque `if (numCoc >= N)` / `else` correspondiente; de lo contrario los valores se pierden silenciosamente.

45. **Multi-elaboración — arquitectura**: `LoteCerveza` soporta 1, 2, 3 o 4 elaboraciones controladas por `numeroElaboraciones` (Integer, default 1). Campos por sesión (todos nullable, activados según n):
    - Sesión 1: `ogPrimeraElaboracion` (Integer SG), `ogBrix` (BigDecimal Brix de S1), `volumenFinalPrimeraElaboracion`, `horaInicioPrimeraElaboracion`, `horaFinPrimeraElaboracion`
    - Sesión 2: `fechaSegundaElaboracion`, `aguaSegundaElaboracion`, `ogSegundaElaboracion` (SG) / `ogBrixSegundaElaboracion` (Brix), `volumenFinalSegundaElaboracion`, `horaInicioSegundaElaboracion`, `horaFinSegundaElaboracion`, `receta2` (FK)
    - Sesión 3: ídem con `Tercera` + `receta3` (FK)
    - Sesión 4: ídem con `Cuarta` + `receta4` (FK). En formulario: secciones `#segunda-elaboracion-section` / `#tercera-elaboracion-section` / `#cuarta-elaboracion-section` (ocultos por `applyElaboraciones()`). OG combinado = media ponderada por volumen en SG (`calcularOgCombinado()` → escribe en `densidadInicial` automáticamente; **no hay botón manual** — el campo se actualiza en cada `input` de OG/volumen de cualquier sesión). `sincronizarVolumenFinalTotal()` auto-suma volúmenes S1–S4 en `litrosFinales`. `densidadInicial` siempre visible; readonly en modo Brix. `densidadFinalFecha` campo común. **`getTotalAguaElaboraciones()`** — computed method en `LoteCerveza` que suma `aguaUtilizada + aguaSegunda + aguaTercera + aguaCuarta` (usado en detalle.html para mostrar Agua Total). **Display OG en detalle.html**: box "OG" (principal, combinado) muestra badge "Refractómetro" y °Brix SOLO en modo sesión única (n==1); en multi-sesión el OG es el combinado y no se le asocia el Brix de S1. Box "OG S1" muestra badge Refractómetro + °Brix desde `ogBrix`. Boxes S2/S3/S4 leen sus propios campos `ogBrixX`. **Fermentador en Panel 0**: el `<select equipoFermentadorId>` vive en el Panel 0 (Datos Generales); Panel 2 (Fermentación) solo muestra un display readonly + link "Asignar en Datos Generales" (`goTab(0)`). La validación en `trazabilidad-costos.js` redirige a `goTab(0)` si falta fermentador.

45b. **Trazabilidad — selector "Destino y Empaque" (`carbDestino`)**: el campo `carbDestino` (campo `LoteCerveza.carbDestino`, guardado como `"48 × Botella 330ml | 2 × Barril 20L"`) se construye en formulario con filas dinámicas (`#destinoList`). Cada fila tiene un `<select class="destino-formato">` poblado con `DESTINO_OPTS`. **Lógica de construcción de `DESTINO_OPTS`** (en `DOMContentLoaded` de `formulario.html`): si el inventario tiene insumos de tipo ENVASE (`ENVASES_INVENTARIO = ${envasesInventario.![nombre]}`), se **combinan** — grupo `"Del inventario"` primero, luego los grupos estáticos completos (Botellas 330/500/750/1000ml, Latas 330/355/473ml, Barriles/Kegs 10/20/30/50L, Growlers 1/2/3.8L, A granel, Otro). Si no hay envases en inventario, se usa solo el fallback estático con los mismos grupos. **NUNCA reemplazar** la lista estática completamente con solo los items de inventario — los usuarios perderían acceso a los grupos Latas/Barriles/Growlers aunque no los tengan en inventario. **En ventas**: el select de unidad por ítem sigue la misma lógica de grupos pero cuando el lote seleccionado tiene `carbDestino` definido, se reemplaza con SOLO los formatos del lote (`parseDestinoJS(cdFull)` → `entries`); si el lote no tiene destino, se muestra el `UNIT_OPTIONS` completo.

46. **Formulario — mover nodos DOM entre secciones**: cuando un campo debe aparecer en distinto lugar según estado del formulario (sin duplicar `name`), capturar `parentNode` y `nextSibling` antes del primer `applyCocciones()` y usar `anchor.after(col)` / `origParent.insertBefore(col, origNextSib)` para moverlo físicamente. Patrón implementado en Agua Utilizada (`#agua-general-col`) que se mueve a Primera Sesión de Cocción cuando n≥2.

47. **TrazabilidadController — redirect POST /guardar**: al crear un lote nuevo, el controller redirige a `redirect:/editar/{id}` (igual que `POST /actualizar/{id}`), NO a la lista `redirect:/`. La variable `loteId` es `Long` (objeto, no primitivo) — si fuera `long` y el ID fuera null, el autoboxing lanzaría NPE y el catch devolvería `"danger"` silenciosamente. Fallback a `redirect:/` solo si `loteId == null` (no ocurre en producción; el repo siempre asigna ID tras `save()`). **Tests**: los mocks de `service.guardar()` deben llamar `lote.setId(N)` antes de construir `LoteGuardadoResult`; de lo contrario `loteId` es null y la URL de redirección esperada es `"/"` en vez de `"/editar/N"`.

42. **Plan de tenant — alertas y bloqueo**: `AlertaScheduler` llama diariamente a `NotificacionService.crearAlertaPlan(tenant, totalLotes, totalUsuarios)` para generar notificaciones in-app (`PLAN_VENCIMIENTO`, `PLAN_LIMITE`) cuando el plan está vencido/por vencer (`Tenant.isPlanVencido()`/`isPlanPorVencer()`, ≤7 días) o cerca/sobre los límites `maxLotes`/`maxUsuarios` (≥90%/100%). Si `planFin + app.plan.dias-gracia (def: 7)` ya pasó, `TenantFilter` redirige todas las rutas (excepto `/plan-vencido`, `/logout`, `/login*`) a `/plan-vencido` (`PlanController` + `templates/plan/vencido.html`).

48. **`ZymosAccessDeniedHandler` — usar `sendError`, nunca `sendRedirect`**: el handler debe llamar `resp.sendError(HttpServletResponse.SC_FORBIDDEN)`. Un `sendRedirect("/error?status=403")` crea una nueva petición GET sin atributos `RequestDispatcher.ERROR_STATUS_CODE`, por lo que `CustomErrorController` lee `statusAttr == null` y defaultea a 500. Con `sendError(403)` el Servlet container hace un forward interno a `/error` con todos los atributos de error correctamente seteados.

49. **`@WebMvcTest` — `TrazabilidadController.GET /nuevo` necesita receta activa**: el controller redirige a `redirect:/` si `recetaService.listarActivas().isEmpty()`. En tests de `@WebMvcTest`, stubear siempre en `@BeforeEach`: `when(recetaService.listarActivas()).thenReturn(List.of(new Receta()))`. Sin este stub, Mockito devuelve una lista vacía por defecto y el test obtiene 302 en vez de 200. NO usar `List.of()` en el stub de `nuevo()`-focused tests.

50. **`display:contents` en `<form>` dentro de flex containers**: para que botones dentro de `<form>` participen directamente como flex items (sin que la caja del form rompa el layout), usar `style="display:contents"` en el `<form>`. El form sigue funcionando (submit, CSRF hidden input). Aplica cuando hay múltiples `<form>` inline dentro de `<div class="d-flex">` — el `class="d-inline"` en el form no funciona bien con flex y causa que elementos adicionales se caigan a una nueva línea.

52. **Índices únicos y soft delete — usar índice parcial**: cuando una entidad usa soft delete (`deleted_at IS NULL` via `@SQLRestriction`) y tiene un unique constraint, el índice debe ser **parcial** en PostgreSQL: `CREATE UNIQUE INDEX ... ON tabla(col) WHERE deleted_at IS NULL`. Sin la cláusula `WHERE`, filas archivadas siguen ocupando el nombre y bloquean crear registros nuevos con el mismo valor. Caso real: `ux_recetas_nombre_tenant` (V23) era global — V80 lo reemplaza por índice parcial. Al agregar nuevas entidades con soft delete + unicidad, crear siempre el índice con `WHERE deleted_at IS NULL`. Quitar también `unique = true` del `@Column` JPA: no refleja el constraint real (compuesto + parcial) y puede confundir la validación de Hibernate.

53. **Excel de Trazabilidad de Lotes — `GET /excel`**: `TrazabilidadController.excel()` acepta los mismos 4 filtros del listado (`estilo`, `fase`, `desde`, `hasta`) y llama a `TrazabilidadService.listarFiltrado()` (versión sin paginar de `listarPaginado()` que llama a `LoteCervezaRepository.findByFiltrosSinPaginar()`). `ExcelExportService.generarExcelLotes()` produce 6 hojas que replican los 6 tabs de la UI: `General e Insumos` (código, estilo, fecha, # sesiones, receta, fermentador, fase, ingredientes como string CSV, agua, pH, litros, costo), `Fermentación`, `Acondicionamiento`, `Maduración` (las 3 con estado, inicio, fin ideal, temperatura, fin real), `Carbonatación` (igual + método, CO₂, azúcar, presión, técnica, validación, destino) y `Densidad-Observaciones` (OG, FG, ABV, atenuación, eficiencia, observaciones, notas de cata). Ingredientes se formatean con `joinIngredientes()` que usa `UnidadUtils.displayTexto()` para mostrar unidades amigables (ej: "5000000 gr" → "5 kg"). `faseEstado(inicio, fin)` retorna "Pendiente"/"En proceso"/"Completado" según las fechas. `@MockBean ExcelExportService` requerido en `TrazabilidadControllerTest`.

54. **Apache POI — caracteres prohibidos en nombres de hojas**: los nombres de hojas Excel no pueden contener `/`, `\`, `*`, `?`, `:`, `[`, `]`. POI lanza `IllegalArgumentException` al llamar `wb.createSheet(name)` si el nombre incluye alguno de esos caracteres. El `GlobalExceptionHandler` captura esa excepción como `RuntimeException` genérica — sin el stacktrace en el log (ahora corregido: se pasa `ex` como tercer arg a `log.error`), el error era invisible. Al definir keys `xls.sheet.*` en `messages.properties`, verificar que ningún valor contenga los caracteres prohibidos.

55. **Spring MVC — repoblar modelo en paths de error de validación POST**: cuando un `@PostMapping` devuelve la vista directamente (en lugar de redirigir) porque `result.hasErrors()`, el modelo solo contiene el DTO con `@ModelAttribute`. TODOS los atributos que el template necesita para renderizarse deben añadirse al modelo explícitamente en ese path — el formulario no los hereda del GET equivalente. Caso real: `RecetaController.guardar()` y `actualizar()` no añadían `insumosInventario` ni `tiposCerveza` → `TemplateProcessingException: Cannot iterate over null` en cada submit con validación fallida. Patrón correcto: `if (result.hasErrors()) { model.addAttribute("listaNecesaria", service.listar()); return "vista/formulario"; }`.

56. **`JdbcTemplate.queryForObject()` — lanza excepción si no hay resultado**: `queryForObject(sql, tipo, args)` lanza `EmptyResultDataAccessException` (mensaje Spring críptico) cuando la query retorna 0 filas. En contextos donde el registro puede no existir (ej: búsqueda por nombre en importación), usar `queryForList()` + check `if (lista.isEmpty()) throw new IllegalArgumentException("mensaje claro: " + valor)`. La excepción `IllegalArgumentException` se propaga hasta el catch del bucle de importación y aparece en el log de errores con contexto útil. Caso real: `MigracionService.importarComercial()` buscaba proveedor por nombre — si el XLSX referenciaba un proveedor inexistente, fallaba con traza Spring ininteligible.

57. **`JwtFilter` — usuario eliminado post-emisión de token**: `loadUserByUsername()` lanza `UsernameNotFoundException` si el usuario fue eliminado en BD después de que se emitió su JWT (el token sigue siendo criptográficamente válido). Sin captura, la excepción se propaga como 500. Fix: `try { ... } catch (UsernameNotFoundException e) { response.sendError(SC_UNAUTHORIZED, "Token user not found"); return; }`. El `return` es crítico para no continuar el `chain.doFilter()` después de `sendError`.

58. **Naming de atributos de modelo — consistencia controller↔template**: el nombre del atributo en `model.addAttribute("nombre", ...)` debe coincidir exactamente con `${nombre}` en el template. Una discrepancia (ej: controller pone `"estadoFiltro"` pero template usa `${estado}`) resulta en null silencioso en expresiones Thymeleaf — sin excepción, el valor simplemente queda vacío. Afecta especialmente los parámetros de filtro en URLs de export (PDF/Excel) donde el null genera una querystring vacía ignorando el filtro del usuario. Caso real: `reportes/ventas.html` usaba `${estado}` pero `ReporteController` ponía `"estadoFiltro"` → exports ignoraban el filtro de estado.

51. **Notificaciones — filtrado por rol**: `NotificacionService` filtra las notificaciones según las authorities del usuario. `TIPO_AUTHORITY` (Map estático) define qué authority requiere cada `TipoNotificacion`: `BAJO_STOCK`/`VENCIMIENTO` → `MODULO_INVENTARIO_VER`, `MANTENIMIENTO` → `MODULO_EQUIPOS_VER`, `SISTEMA` → `MODULO_FACTURACION_VER`, `BPM_SALUD` → `MODULO_BPM_VER`. Tipos sin entrada en el mapa (`PLAN_VENCIMIENTO`, `PLAN_LIMITE`) solo los ven ADMIN/SUPERADMIN. `tiposVisibles(Collection<String> authorities)` calcula la lista filtrada. Los métodos `listarRecientes`, `contarNoLeidas` y `listarTodas` reciben `Collection<String> authorities` como parámetro — obtenerlas en el controller con `auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toSet())`. **Al agregar un nuevo `TipoNotificacion`**: añadir entrada en `TIPO_AUTHORITY` si no es admin-only; si es admin-only, simplemente no agregar entrada. `TipoNotificacion.BPM_SALUD` se genera en `BpmService.guardarSintoma()` cuando `r.tieneSintomas()` es true, con deduplicación de 1 por día via `existeEnPeriodo()`.

---

## CONVENCIONES DEL PROYECTO

- Flash attributes: `"mensaje"` y `"tipoMensaje"` (success, warning, danger) — Bootstrap Toast top-right, auto-dismiss 5s, en navbar.html
- Templates heredan navbar: `th:replace="~{fragments/navbar :: navbar}"`
- Paginación: tamaño configurable via `app.page-size=15` (default). Fragmento `paginacion.html` recibe `baseUrl`, `extraParams`, `paginaActual`, `totalPaginas`
- Logging: INFO operaciones exitosas, WARN stock insuficiente/logins fallidos, ERROR excepciones
- Todos los servicios `@Transactional`. DashboardService `@Transactional(readOnly = true)`
- Bean Validation en DTOs + `@DateTimeFormat(iso=DATE)` en todos los campos `LocalDate`
- **Visualización de cantidades en templates — regla obligatoria**: para valores `BigDecimal` con unidad (litros, °C, %, gramos, kg, etc.), usar siempre `valor.stripTrailingZeros().toPlainString()` en lugar de `#numbers.formatDecimal`. Esto elimina ceros decimales superfluos: `5.000` → `5`, `2.500` → `2.5`, `2.501` → `2.501`. Para valores nullable: `${valor != null ? valor.stripTrailingZeros().toPlainString() + ' L' : '—'}`. Para expresiones aritméticas Java (Integer − Integer) * double que producen `Double` en SpEL y no soportan `stripTrailingZeros()` directamente, envolver con `T(java.math.BigDecimal).valueOf(expr).stripTrailingZeros().toPlainString()`. En JavaScript inline (cálculos en tiempo real), usar el helper `fmtNum`: `function fmtNum(n, maxDec) { return parseFloat((+n).toFixed(maxDec)).toString(); }` — reemplaza `.toFixed()` en displays de usuario. **No aplicar** a: valores de `<input th:value>` (binding de formulario), arrays de datos para Chart.js, valores monetarios con formato fijo `#numbers.formatDecimal(v,1,'COMMA',2,'POINT')`.
- Dark mode: toggle luna/sol en navbar, clase `html.dark-mode` en `<html>`, persiste en `localStorage`. Variables centralizadas `--dm-*` en `style.css` (usarlas siempre en reglas `html.dark-mode`, no hardcodear hex oscuros). Los `<style>` inline de templates con dark mode propio incluyen el bloque `html.dark-mode {...}` al final del mismo `<style>`. `!important` requerido para sobreescribir inline styles de elementos (ej: `kanban-dias`, `.subtipo-placeholder`). **CRÍTICO — cascada CSS `!important`**: `style="background:#X !important"` en un elemento NO puede sobreescribirse desde ninguna regla stylesheet (ni siquiera con `!important` autor). La única solución es eliminar el `style` inline y mover el valor a una clase CSS con su override `html.dark-mode`. **Bootstrap Toast**: `.toast-body` usa `--bs-body-bg` (blanco) por defecto — override en `navbar.html`: `html.dark-mode .toast { background: var(--dm-card); border-color: var(--dm-border-light) !important; }` y `html.dark-mode .toast-body { color: var(--dm-text); }`. **Auditoría dark mode completada 2026-07-06** — todos los templates cubiertos.
- Dashboard personalizable (todo localStorage, sin backend): visibilidad (`zymos-dashboard-secciones`) y orden drag & drop (`zymos-dashboard-orden`) via SortableJS 1.15.2 (`handle:'.dash-handle'`, `ghostClass:'dash-ghost'`, `dragClass:'dash-drag'`). Secciones (`id="dash-{nombre}"`, clase `dash-section`): `stats-lotes`, `stats-inventario`, `alertas`, `elaboraciones`, `charts`, `finanzas`. `restaurarOrden()` antes de `restaurarVisibilidad()`. `alertas` usa `th:if` → puede no existir en DOM; null-check en `restaurarOrden()`. Stat-cards en `<a class="stat-card-link">` con `translateY(-2px)` en hover. Links: totalLotes→`/`, enProceso→`/kanban`, completados→`/?fase=completados`, estilosDistintos→`/reportes/produccion`, totalInsumos→`/inventario`, bajoStock→`/inventario?filtroBajoStock=true`, proximosAVencer→`/inventario?filtroPorVencer=true`, mantenimientoPendiente→`/equipos?estado=MANTENIMIENTO`. Stats Lotes: 4 cards (`totalLotes`, `enProceso`, `completados`, `estilosDistintos`). Chart.js colors via `getComputedStyle(…).getPropertyValue('--verde-alera'/'--dorado')` en `DOMContentLoaded`, fallback hex. Próximas Elaboraciones (`dash-elaboraciones`): hasta 5 futuras, PLANIFICADA→`/nuevo?planId={id}`, otros→`/planificacion`. "Restablecer" borra ambas claves localStorage.
- Búsqueda global: `GET /buscar?q=` (página completa) + `GET /buscar/suggest?q=` (JSON para typeahead del navbar)
- **Patrón typeahead/suggest**: cada módulo con lista expone `GET /{modulo}/suggest?q=` (`@ResponseBody`, `produces=JSON`). El servicio filtra en memoria (listas pequeñas) o via query DB con LIKE (facturas). El JS del template usa debounce 260ms, flechas ↑↓ para navegar, Escape para cerrar. CSS del dropdown definido en `navbar.html` (clases `.search-suggest`, `.ss-item`, `.ss-title`, `.ss-sub`, `.ss-section`, `.ss-footer`, `.ss-empty`) — disponibles globalmente por ser parte del fragment. El `data-*` attribute en el input inyecta filtros activos (tipo, estado, activa) sin inline JS. Usuarios: sin página de detalle — el click usa `href="#usuario-{id}"` + CSS `tr:target { animation: rowFlash }` para scroll+flash sin navegación.
- Mapeos entidad→DTO: usar `LoteMapper` en `com.alera.mapper` (MapStruct). No escribir asignaciones manuales campo a campo en servicios.
- Templates con AJAX: incluir siempre `<meta name="_csrf">` y `<meta name="_csrf_header">` en el `<head>`. Leer en JS con `document.querySelector('meta[name="_csrf"]')?.content`.
- **Branding en templates**: `${branding.name}`, `${branding.tagline}`, `${branding.logoUrl}`, `${branding.colorAccent}`, etc. — disponible en todos los templates via `GlobalControllerAdvice`. NO hardcodear "Alera" en HTML. Patrón obligatorio para el `<title>`: `<title th:text="${branding.name} + ' - Sección'">Alera - Sección</title>`. En contextos donde `branding` puede ser null (página de error, dispatches de error de Servlet), usar la expresión null-safe: `<title th:text="${branding != null ? branding.name : 'Alera'} + ' - Error'">Alera - Error</title>`. Los 25 templates de la app ya siguen este patrón.
- **Costos en formulario**: `INIT_ITEMS_DATA` (solo ítems ya asignados al lote, vía `itemsFacturaAsignados`/`FacturaItemRepository.findByIdIn`), `INIT_IDS`, `INIT_CANTIDADES` se inyectan como JS via `<script th:inline="javascript">`. La búsqueda del resto de ítems es AJAX (`/suggest-items`, ver regla 32) — no se serializa el catálogo completo. El submit handler construye `itemsIds[]` + `itemsCantidades[]` como hidden inputs desde el array `asignados`. El botón "Aplicar a Receta e Insumos" llama `sincronizarIngredientesDesdeItems()` que rellena los containers de ingredientes y navega al tab 1 (`goTab(1)`).
- **JS estático de trazabilidad** (`static/js/`): `<script th:inline="javascript">` inyecta variables globales; el `.js` externo las lee. Archivos:
  - `trazabilidad-ingredientes.js` — wizard de tabs, filas dinámicas, carga de receta (`_recetaPendiente`), stock via `/suggest-items-por-nombre`, `UNIT_OPTIONS_CLAR` (incluye "und" para clarificantes), `goTab()` gestiona clase `done` en `.wz-tab`. `sincronizarVolumenFinalTotal()` auto-suma `litros1-value + litros2-value + litros3-value` (en litros) en `litros-value` y `litros-display` cuando n≥2; se llama al final de `volUpdate` para litros1/2/3 y en DOMContentLoaded. Usado por `formulario.html`.
  - `trazabilidad-costos.js` — buscador AJAX `/suggest-items` (caché `ultimosResultados`), `sincronizarIngredientesDesdeItems()`, `convertirCantidadUnidades()`, validación de fermentador en submit (revierte a tab 2 si `fermFechaInicial` no vacío y `equipoFermentadorId` vacío). Depende de `trazabilidad-ingredientes.js`. Usado por `formulario.html`.
  - `trazabilidad-detalle.js` — Chart.js dual-eje densidad+temperatura. Lee `CHART_FECHAS`, `CHART_DENSIDAD`, `CHART_TEMP`. Usado por `detalle.html`.
  - `trazabilidad-kanban.js` — SortableJS drag & drop, AJAX cambio de fase, validación de fermentador (revierte DOM si `card.dataset.tieneFermentador !== 'true'`, toast 5s con link a `/editar/{id}`). Usado por `kanban.html`.
  - **Orden en `formulario.html`**: (1) `th:inline` data block, (2) `trazabilidad-ingredientes.js`, (3) `trazabilidad-costos.js`.
- **`numeros.js`** (`static/js/numeros.js`) — motor global de formateo numérico europeo (`.` miles, `,` decimal). Cargado vía `navbar.html` en todos los templates. Funciones clave: `window.numVal(v)` (parsea string europeo a float), `fromDisplay(s)` (elimina `.` y sustituye `,` → `.`), `toDisplay(raw)` (convierte decimal del servidor a europeo al cargar la página). Listener `submit` en fase de captura que desnormaliza antes de enviar al servidor. MutationObserver para inputs añadidos dinámicamente. **Exclusión obligatoria**: inputs cuyo `name` o `id` coincida con `/densidad|objetivo|brix|phagua|porcentaje|descuento|pct|\.iva|iva\b|co2|presion|psi|temperatura|temp\b|minutos|duracion|horas\b|tiempo\b|orden\b|aroma\b|apariencia|sabor|sensacion|impresion/i` NO se formatean (valores enteros XXXX, porcentajes, parámetros de carbonatación, sliders BJCP). **NO aplicar** a: inputs de densidad (formato XXXX), arrays de Chart.js, valores de `<input type="hidden">` de binding. Usado globalmente — no reimplementar formateo en templates individuales.

---

## MÓDULO BPM (Buenas Prácticas de Manufactura)

Módulo de trazabilidad de inocuidad alimentaria. Ruta base: `/bpm`. Posición en navbar: entre Comercial y Admin.

### Submodulos

| Submodulo | Ruta | Entidad | Tabla BD |
|---|---|---|---|
| Estado de Salud (diario) | `/bpm/salud/diario` | `RegistroSintomas` | `bpm_registros_sintomas` |
| Autorizaciones de Salud | `/bpm/salud/autorizaciones` | `RegistroSintomas` | `bpm_registros_sintomas` |
| Soluciones Desinfectantes | `/bpm/soluciones` | `SolucionDesinfectante` | `bpm_soluciones_desinfectantes` |
| Control de Plagas | `/bpm/plagas` | `AvistamientoPlagas` | `bpm_avistamiento_plagas` |
| Evacuación de Residuos | `/bpm/residuos` | `EvacuacionResiduos` | `bpm_evacuacion_residuos` |
| Limpieza y Desinfección | `/bpm/limpieza` | `LimpiezaDesinfeccion` | `bpm_limpieza_desinfeccion` |

### Arquitectura BPM

- **`BpmController`** — controller único para todos los submodulos. Inyecta `BpmService`, `BpmPdfService`, `InsumoInventarioService` y `MessageSource`. Flash messages y títulos de PDF resueltos con `messageSource.getMessage(key, args, key, locale)` via helpers privados `msg(key, locale)` / `msgf(key, locale, args...)`. Parámetro `Locale locale` en todos los POST y endpoints PDF.
- **`BpmService`** — lógica de negocio para los 5 módulos. Método especial: `autorizarAcceso(Long id, String adminUsername, String firmaResponsable)` — guarda la firma del responsable al autorizar.
- **`BpmPdfService`** — genera PDFs (OpenPDF) para cada submodulo. Inyecta `MessageSource`. Usa `ThreadLocal<Locale> LOCALE_HOLDER` + helper `t(key)` (mismo patrón que `PdfExportService`). Todos los `generar*()` reciben `Locale locale` como último parámetro y llaman `LOCALE_HOLDER.set(locale)` al inicio. Método `addTdFirma(tabla, firmaData, bg)` decodifica base64 PNG y renderiza la imagen (60×20px) en la celda PDF. Cada registro tiene endpoint individual: `GET /bpm/{modulo}/{id}/pdf`.
- **`BpmDashboardController`** — dashboard BPM en `/bpm` con métricas del mes actual.
- **`BpmSaludFilter`** (`OncePerRequestFilter`, registrado en `SecurityConfig`) — intercepta todos los requests de usuarios no-admin y verifica el registro diario de salud. Flujo: (1) sin registro hoy → redirige a `/bpm/salud/diario`; (2) registro con síntomas (`tieneSintomas()=true`) y sin autorizar → redirige a `/bpm/salud/bloqueado`; (3) registro OK o autorizado → deja pasar. ADMIN/SUPERADMIN saltean el filtro. `shouldSkip()` excluye: `/bpm/salud/**`, assets estáticos, `/login`, `/logout`, `/error`, `/api/**`, `/actuator/**`, `/plan-vencido`. Al guardar síntomas (`POST /salud/diario`): si `tieneSintomas()=true` → redirige a `/bpm/salud/bloqueado` + dispara `NotificacionService.crearAlertaBpmSalud()`. El desbloqueo lo hace el ADMIN desde `/bpm/salud/autorizaciones` (requiere `ROLE_ADMIN/SUPERADMIN`) firmando con `SignaturePad` → `autorizadoPorAdmin=true`.

### Firma Digital

- **`bpm-firma.js`** (`static/js/`) — script global para todos los formularios BPM con firma. Inicializa `SignaturePad` en todos los `.firma-canvas`, restaura firma existente al editar, sincroniza al `<input type="hidden">` correspondiente vía `dataset.input` en el submit del form.
- **HTML del canvas**: `<canvas class="firma-canvas" data-input="id-del-input-hidden">`. El input hidden tiene el `name` que recibe el controller (`@RequestParam String firma`).
- **Base64 PNG**: las firmas se almacenan como `data:image/png;base64,...` en columnas `TEXT` en BD (V71: `ALTER COLUMN firma TYPE TEXT`).
- **Autorizaciones**: botón "Firmar y autorizar" abre un modal con canvas dedicado. La firma es **obligatoria** — el submit está bloqueado si el pad está vacío (`pad.isEmpty()`). El controller recibe `@RequestParam(required = false) String firmaResponsable`.
- **Inicializar con ratio**: `canvas.width = canvas.offsetWidth * devicePixelRatio; canvas.height = canvas.offsetHeight * ratio; canvas.getContext('2d').scale(ratio, ratio)` — necesario para evitar firmas borrosas en pantallas HiDPI.

### Inventario Químico en Soluciones

- El formulario "Nueva Solución Desinfectante" muestra un datalist con productos del inventario de tipo "Químico".
- `InsumoInventarioService.listarPorTipo(String tipo)` — filtra por el campo `tipo` de `InsumoInventario`, que almacena el **nombre display** del tipo (ej: `"Químico"`), NO el nombre del enum Java (`"QUIMICO"`). Este campo fue migrado por V45/V47 de enum names a display names de `tipos_insumo`.
- **CRÍTICO**: siempre pasar el display name: `listarPorTipo("Químico")`, nunca `"QUIMICO"`.
- `InsumoInventarioRepository.findAllByTipoOrderByNombreAsc(String tipo)` — query derivada de Spring Data.

### Migraciones BPM

- V69: tablas BPM base (5 tablas con `tenant_id`)
- V70: columnas de firma iniciales
- V71: `firma` columns widened to `TEXT` en todas las tablas BPM para soportar base64 PNG completo

---

## MÓDULO BPM — REGLAS CSP

El CSP enforced (activo desde 2026-07-03, Fase D) bloquea **todos** los event handlers inline y **todos** los `<script>` sin nonce. Reglas para templates BPM (y cualquier template nuevo):

1. **NUNCA usar `onclick=`, `onsubmit=`, `onchange=`, `oninput=` inline** en ningún elemento HTML.
   - Reemplazar con `addEventListener` en un `<script th:attr="nonce=${cspNonce}">`.
   - Para confirmaciones de borrado: usar `data-confirm="¿mensaje?"` — navbar.html lo maneja globalmente en fase de captura.
   - Para abrir modales desde botones: agregar clase CSS descriptiva + `data-*` attrs, escuchar con `document.addEventListener('click', fn)` buscando `e.target.closest('.clase')`.

2. **TODO `<script>` inline DEBE llevar `th:attr="nonce=${cspNonce}"`** — si falta, el navegador bloquea el script silenciosamente y los botones no responden.
   ```html
   <script th:attr="nonce=${cspNonce}">
   document.addEventListener('DOMContentLoaded', function () { ... });
   </script>
   ```

3. **Scripts CDN** (Bootstrap, SignaturePad, etc.) no necesitan nonce — están en la allowlist CSP por hash o por ser `cdn.jsdelivr.net`. Sí verificar que el CDN esté en la allowlist de `CspFilter`.

4. **`bpm-firma.js`** ya sigue CSP: es un archivo estático servido por `/js/bpm-firma.js`, no inline. Cargarlo con `<script th:src="@{/js/bpm-firma.js}">` — no necesita nonce.

---

## INTERNACIONALIZACIÓN (i18n)

**Estado (2026-07-07): cobertura completa** — todos los templates, servicios PDF y controllers están internacionalizados.

### Archivos de mensajes
- `src/main/resources/messages.properties` — idioma por defecto (español)
- `src/main/resources/messages_en.properties` — inglés

### Convención de keys
| Módulo | Prefijo |
|---|---|
| Acciones genéricas | `action.*` |
| Labels genéricos | `label.*` |
| Navbar | `nav.*` |
| BPM módulo | `bpm.*` |
| BPM PDFs (columnas, secciones) | `bpm.pdf.*` |
| PDFs de lotes/recetas | `pdf.*` |

### Patrón en templates Thymeleaf
```html
<!-- Texto simple -->
<span th:text="#{bpm.limp.title}">Limpieza y Desinfección</span>

<!-- Parametrizado -->
<p th:text="#{bpm.aut.subtitle(${#temporals.format(hoy,'dd/MM/yyyy')})}"></p>

<!-- HTML en el mensaje (usar th:utext, NO th:text) -->
<p th:utext="#{bpm.bloqueado.accion.texto}"></p>

<!-- data-confirm (global handler en navbar.html) -->
<form th:attr="data-confirm=#{action.delete} + ' ?'">
```
**CRÍTICO — `#{}` NUNCA dentro de comillas simples**: `th:text="'#{key} ' + ${var}"` → `#{}` no se resuelve. Forma correcta: `th:text="#{key} + ' ' + ${var}"`.

### Patrón en servicios PDF (PdfExportService / BpmPdfService)
```java
private final MessageSource messageSource;
private static final ThreadLocal<Locale> LOCALE_HOLDER = new ThreadLocal<>();

private String t(String key) {
    Locale loc = LOCALE_HOLDER.get();
    return messageSource.getMessage(key, null, key, loc != null ? loc : Locale.forLanguageTag("es"));
}

// Al inicio de cada método público:
public byte[] generarXxx(..., Locale locale) {
    LOCALE_HOLDER.set(locale);
    // usar t("bpm.pdf.col.xxx") internamente
}
```

### Patrón en controllers (flash messages y PDF títulos)
```java
private final MessageSource messageSource;

private String msg(String key, Locale locale) {
    return messageSource.getMessage(key, null, key, locale);
}
private String msgf(String key, Locale locale, Object... args) {
    return messageSource.getMessage(key, args, key, locale);
}

// Flash message:
flash.addFlashAttribute("mensaje", msg("bpm.sint.guardado", locale));

// PDF período parametrizado:
String sub = msgf("bpm.pdf.periodo", locale, desde.format(fmt), hasta.format(fmt));
```
Spring MVC inyecta `Locale locale` automáticamente como parámetro de método — no requiere configuración adicional.

### Cambio de idioma — selector en navbar

`I18nConfig`: `TenantAwareCookieLocaleResolver` (cookie `zymos-lang`) + `LocaleChangeInterceptor` (param `lang`).

El selector en `navbar.html` usa `th:href="${currentUri + '?lang=es'}"` — `currentUri` es un `@ModelAttribute` de `GlobalControllerAdvice` que expone `request.getRequestURI()`. El navegador navega via GET normal, `LocaleChangeInterceptor` procesa el param `lang`, guarda la cookie y la página se recarga en el nuevo idioma de forma inmediata. No requiere JS.

**CRÍTICO — Thymeleaf 3.1**: `#request` ya NO está disponible en expresiones `${}` (`IllegalArgumentException: The 'request'... expression utility objects are no longer available by default`). Para acceder a datos del request en templates, siempre exponer via `@ModelAttribute` en `GlobalControllerAdvice`.

**NO usar** `th:href="@{''(lang='es')}"`: genera solo `?lang=es` sin ruta (puede no recargar). **NO usar** `href="#" + listener JS delegado en document`: Bootstrap puede interceptar el click dentro del dropdown antes de que el evento burbujee al listener.

---

## MÓDULO RBAC DINÁMICO (roles por tenant)

Implementado en V72+V73 (2026-07-09). Permite a cada tenant crear y gestionar sus propios roles con permisos granulares por módulo. Ruta de administración: `/admin/roles` (solo ADMIN/SUPERADMIN).

### Tablas
- **`roles_tenant`** — roles por tenant: `id`, `tenant_id`, `nombre`, `descripcion`, `activo`, `es_sistema` (bool), `es_admin` (bool), `created_at`. Unique constraint `(tenant_id, nombre)`.
- **`roles_modulos_permisos`** — permisos por rol: `id`, `rol_id`, `modulo` (enum name de `ModuloApp`), `puede_ver`, `puede_crear`, `puede_editar`, `puede_eliminar`. Unique constraint `(rol_id, modulo)`.
- **`usuarios.rol_custom_id`** — FK nullable → `roles_tenant.id`. Fuente primaria de autorización (post-V73).

### Flujo de autenticación (`UsuarioService.loadUserByUsername`)
1. Si `rolCustomId != null` → `buildCustomAuthorities(rolCustomId)`.
2. Si `rolCustomId == null` → buscar en `roles_tenant` por `ENUM_TO_SISTEMA.get(u.getRol())` (map enum→nombre sistema).
3. Fallback: `ROLE_{enum.name()}` legacy (sin roles en BD).

`buildCustomAuthorities(rolCustomId)` otorga: `ROLE_CUSTOM` siempre + `ROLE_ADMIN` si `es_admin=true` + `MODULO_X_VER/CREAR/EDITAR/ELIMINAR` por cada permiso activo.

### Roles de sistema vs custom
- **`es_sistema = true`**: los 5 roles migrados de enum (Administrador, Producción, Inventario, Facturación, Equipos). No se pueden eliminar desde la UI. Tienen la badge "Sistema".
- **`es_admin = true`**: solo el rol "Administrador". Otorga `ROLE_ADMIN` para rutas `/admin/**`, `/usuarios/**`.
- **`es_sistema = false`**: roles custom creados por el tenant (ej: "Recursos Humanos"). Pueden eliminarse. No tienen badge "Sistema".

### Roles por defecto (creados por `RolTenantService.crearRolesPorDefectoSiNoTiene`)
Solo actúa si `countByTenantId == 0`. Crea 6 roles: Administrador (es_admin=true, es_sistema=true), Producción, Inventario, Facturación, Equipos (todos es_sistema=true), Recursos Humanos (es_sistema=false, solo BPM). Llamado desde `DataInitializer` al arrancar para cada tenant.

### SecurityConfig — HTTP method en requestMatchers
Al usar `.requestMatchers(HttpMethod.X, "/path/**")`, verificar que el `HttpMethod` coincide exactamente con el `@XMapping` del controller. Error típico: proteger `/duplicar/**` con `HttpMethod.POST` cuando el endpoint es `@GetMapping` — la regla nunca matchea y el acceso queda abierto para cualquier usuario autenticado. **Caso real (2026-07-15)**: `TrazabilidadController.duplicar()` es `@GetMapping` pero estaba protegido solo en `POST`, permitiendo duplicar lotes sin `MODULO_TRAZABILIDAD_CREAR`. Corregido agregando regla `HttpMethod.GET` separada.

### SecurityConfig — helpers lambda
**NUNCA** usar `WebExpressionAuthorizationManager` instanciado manualmente en helpers estáticos — `setExpressionHandler()` no se llama → `this.expression == null` → NPE → 302 en `@WebMvcTest`. Usar lambdas:
```java
private static AuthorizationManager<RequestAuthorizationContext> adminOnly() {
    return (auth, ctx) -> new AuthorizationDecision(
            auth.get().getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN")));
}
private static AuthorizationManager<RequestAuthorizationContext> modulo(String m) { return moduloOp(m, "VER"); }
private static AuthorizationManager<RequestAuthorizationContext> moduloOp(String m, String op) {
    String authority = "MODULO_" + m + "_" + op;
    return (auth, ctx) -> new AuthorizationDecision(
            auth.get().getAuthorities().stream().anyMatch(a ->
                    a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN") ||
                    a.getAuthority().equals(authority)));
}
```

### Sincronización enum↔custom
`sincronizarRolCustom(u)` — llamado en `guardar()` y `cambiarRol()`: busca el rol de sistema por `ENUM_TO_SISTEMA.get(u.getRol())` y setea `u.setRolCustomId(sid)`. Si no hay roles en BD (entorno de tests), deja `rolCustomId = null` (cae al fallback enum). `asignarRolCustom(id, rolCustomId)` — usada por `POST /usuarios/{id}/rol-custom` para asignar roles custom directamente.

### Queries cross-tenant
`RolTenantRepository` usa `nativeQuery = true` para todos los métodos de DataInitializer: `insertarRolNativo`, `insertarPermisoNativo`, `countByTenantId`, `findIdByTenantIdAndNombre`, `findEsAdminById` (bypasean el filtro `@TenantId` de Hibernate).

### @WebMvcTest con RBAC
`UsuarioService` es `@MockBean` en tests de controller → `RolTenantRepository` no necesita ser mocked (Spring no instancia el servicio real). Todos los controller tests que tengan `@MockBean UsuarioService` son compatibles con el RBAC sin cambios adicionales. `BpmControllerTest` requiere `@MockBean InsumoInventarioService` (inyectado en `BpmController`).

### Nombre de rol visible en UI (`NOMBRE_ROL_*`)
`buildCustomAuthorities(rolCustomId)` también emite la authority `NOMBRE_ROL_{nombre}` (ej: `NOMBRE_ROL_Producción`) para transportar el display name del rol sin consultar BD en cada request. `GlobalControllerAdvice.rolNombreCustom(Authentication)` es un `@ModelAttribute("rolNombreCustom")` que extrae ese nombre filtrando por `startsWith("NOMBRE_ROL_")` — disponible en todos los templates. En `navbar.html` se usa: `th:if="${rolNombreCustom != null}" sec:authorize="!hasRole('ADMIN')" th:text="${rolNombreCustom}"`. **CRÍTICO**: usuarios con `ROLE_ADMIN` no emiten este authority (se construyen con el fallback enum), por eso el `sec:authorize="!hasRole('ADMIN')"` evita mostrar null para admins.

### `sec:authorize` en templates con RBAC
Los templates usan `sec:authorize` para mostrar/ocultar botones. Con RBAC los usuarios no tienen `ROLE_PRODUCCION` etc. — tienen `ROLE_CUSTOM` + `MODULO_X_VER/CREAR/EDITAR/ELIMINAR`. **Patrón obligatorio**: extender las expresiones existentes con `or hasAuthority('MODULO_X_OPERACION')`:
```html
<!-- Ver sección -->
sec:authorize="hasAnyRole('ADMIN','PRODUCCION','SUPERADMIN') or hasAuthority('MODULO_TRAZABILIDAD_VER')"
<!-- Botón crear -->
sec:authorize="hasAnyRole('ADMIN','PRODUCCION','SUPERADMIN') or hasAuthority('MODULO_TRAZABILIDAD_CREAR')"
<!-- Botón editar -->
sec:authorize="hasAnyRole('ADMIN','PRODUCCION','SUPERADMIN') or hasAuthority('MODULO_TRAZABILIDAD_EDITAR')"
<!-- Botón eliminar -->
sec:authorize="hasAnyRole('ADMIN','SUPERADMIN') or hasAuthority('MODULO_TRAZABILIDAD_ELIMINAR')"
```
Templates auditados (17): barriles, clientes, equipos, facturas, inventario, ordenes-compra, planificacion, recetas, stock, trazabilidad/detalle, trazabilidad/kanban, ventas. **NUNCA** usar solo los roles legacy en templates nuevos — los roles RBAC custom nunca tendrán `ROLE_X`.

---

## MÓDULO DE MIGRACIÓN DE DATOS (`/admin/migracion`)

Herramienta de importación masiva vía XLSX para onboarding de tenants. Accesible solo para ADMIN/SUPERADMIN (restringido por SecurityConfig en `/admin/**`). Ruta: `/admin/migracion/{subdomain}`.

### Arquitectura
- **`MigracionController`** — 3 endpoints: `GET /{subdomain}` (página), `GET /{subdomain}/plantilla/{modulo}` (descarga XLSX), `POST /{subdomain}/importar/{modulo}` (importación). Inyecta `MessageSource`; recibe `Locale locale` en `detalle()` e `importar()` para flash messages i18n. Pasa 3 stats al modelo: `totalImportaciones`, `importacionesExitosas`, `importacionesParciales` (calculados con `stream().filter()`).
- **`MigracionService`** — 11 métodos `importar*()`. Usa `JdbcTemplate` (SQL nativo) y Apache POI para leer XLSX. Devuelve `Resultado(procesadas, exitosas, errores, mensajes, estado)` donde `estado` es `"EXITOSO"` / `"PARCIAL"` / `"ERROR"`. Guarda `MigracionLog` por cada importación. Helpers privados: `hora(row, col)` → `LocalTime` desde HH:mm; `resolverRecetaOpcional(nombre, recetaIds, tenantId)` → `Long` id buscando primero en mapa luego en BD.
- **Multi-elaboración en `importarProduccion`** — hoja Lotes acepta 71 columnas (0-70). Cols 42-70 opcionales: `[42]numero_elaboraciones` (default 1), `[43]og_s1`, `[44]vol_final_s1`, `[45]hora_inicio_s1`, `[46]hora_fin_s1`; S2: `[47]fecha_s2` … `[54]nombre_receta_s2`; S3: `[55-62]`; S4: `[63-70]`. Los campos `receta2_id/receta3_id/receta4_id` se resuelven por nombre. Horas en formato HH:mm.
- **`MigracionTemplateService`** — 11 métodos `plantilla*()` que generan XLSX con estilos, dropdowns de validación, filas de ejemplo y hoja de instrucciones.
- **`MigracionLog`** — entidad (`migracion_log`) con: `tenantId`, `modulo`, `archivo`, `procesadas`, `exitosas`, `conErrores`, `estado`, `detalles` (TEXT), `usuario`, `fecha` (`@PrePersist`).
- **`MigracionLogRepository`** — `findByTenantIdOrderByFechaDesc(String tenantId)` y `countByTenantId(String tenantId)`.

### 11 módulos soportados
| Módulo | Key i18n | Hojas XLSX |
|---|---|---|
| `almacen` | `admin.mig.mod.almacen` | 1 |
| `equipos` | `admin.mig.mod.equipos` | 1 |
| `comercial` | `admin.mig.mod.comercial` | 3 (Proveedores→Facturas→Factura_Items) |
| `produccion` | `admin.mig.mod.produccion` | 6 (Recetas→Ingredientes→Escalones→Adiciones→Lotes→Lote_Ingredientes). Hoja Lotes: cols 0-41 base + cols 42-70 multi-elaboración (ver abajo) |
| `clientes` | `admin.mig.mod.clientes` | 1 |
| `ventas` | `admin.mig.mod.ventas` | 2 (Ventas→Venta_Items, misma referencia_venta) |
| `barriles` | `admin.mig.mod.barriles` | 1 |
| `ordenes` | `admin.mig.mod.ordenes` | 2 (OC→OC_Items, mismo numero_oc) |
| `seguimiento` | `admin.mig.mod.seguimiento` | 3 (independientes, referencia codigo_lote) |
| `catalogos` | `admin.mig.mod.catalogos` | 2 (Tipos_Cerveza y Categorias independientes) |
| `mantenimientos` | `admin.mig.mod.mantenimientos` | 1 (referencia equipo por nombre exacto) |

### Flash messages (i18n)
Prefijo `admin.mig.flash.*` — resueltos con `messageSource.getMessage(key, args, key, locale)`. Keys:
- `admin.mig.flash.tenant.no.encontrado` — tenant no encontrado (redirect a `/admin/tenants`)
- `admin.mig.flash.archivo.vacio` — sin archivo seleccionado
- `admin.mig.flash.resultado` — resumen `{0}: {1} filas, {2} exitosas, {3} con errores`
- `admin.mig.flash.primeros.errores` — primeras 3 líneas de error concatenadas
- `admin.mig.flash.error.procesando` — excepción no controlada

### Template (`admin/migracion/detalle.html`)
- **Stat cards** (`.stat-card-simple`): total, exitosas, parciales, último módulo — visibles solo cuando `totalImportaciones > 0`.
- **Flash message**: `class="alert alert-dismissible"` + `th:classappend="'alert-' + ..."` (no `th:classappend="'alert alert-' + ..."`).
- **Modal errores**: `#erroresTexto` sin `bg-light`; dark mode via CSS en `<style th:attr="nonce=${cspNonce}">`.
- **Badge módulo en historial**: ternario de 11 entradas — `catalogos` → `bg-secondary`, `mantenimientos` → `bg-danger`.
- **Al agregar un módulo nuevo**: (1) añadir case en el switch del controller; (2) añadir card en el grid del template; (3) añadir instrucción en la lista; (4) añadir entrada al badge ternario; (5) añadir keys i18n `admin.mig.mod.*` en ambos `messages*.properties`.

### Tests del módulo de migración

- **`MigracionControllerTest`** (`@WebMvcTest`) — 46 tests organizados en 3 `@Nested`:
  - `Detalle` (5 tests): autenticación, modelo con stats (totalImportaciones, exitosas, parciales), tenant inexistente → redirect danger.
  - `DescargaPlantilla` (20 tests): 11 módulos vía `@ParameterizedTest` → 200 + content-type XLSX; `Content-Disposition` incluye tenant y módulo; módulo desconocido → 400 (GlobalExceptionHandler captura `RuntimeException`).
  - `Importar` (21 tests): archivo vacío → warning sin llamar servicio; EXITOSO → success; PARCIAL → warning; FALLIDO → danger; excepción → danger; módulo desconocido → danger; 11 módulos `@ParameterizedTest` → todos enrutan al método correcto del servicio; usuario autenticado se pasa como parámetro.
- **`MigracionServiceIntegrationTest`** — 9 tests con Testcontainers (PostgreSQL real): almacén happy path, tipo inválido, nombre vacío, equipos estado default, comercial con subtotal, proveedor duplicado, producción con receta/escalón/lote, código de lote duplicado, MigracionLog persistido.
- **`MigracionTemplateServiceTest`** — 7 tests: 6 plantillas generan XLSX válido (magic bytes `PK\x03\x04`), archivos distintos entre módulos.
- **`MigracionTestDataGenerator`** — clase utilitaria en `com.alera.util`. Ejecutar como test JUnit (`generarTodos()`) para generar los 11 archivos Excel de prueba en `src/test/resources/migracion-test/` (01-catalogos.xlsx … 11-mantenimientos.xlsx). Datos consistentes: ventas/barriles referencian lote `IPA-2024-001` de producción; mantenimientos referencian equipos del archivo 03. Orden recomendado: 01→02→03→04→05→06→07→08→09→10→11.

---

## MÓDULO REPORTES (`/reportes`)

Tres reportes: Producción (`/reportes/produccion`), Ventas (`/reportes/ventas`), Rentabilidad (`/reportes/rentabilidad`). Navegación via fragmento `fragments/reportes-nav.html`. `GET /reportes/` redirige a `/reportes/produccion`.

### Exports disponibles por reporte

| Reporte | Excel | PDF |
|---|---|---|
| Producción | `GET /reportes/produccion/excel` — 3 sheets (Lotes, Por Estilo, Tendencia Mensual) | `GET /reportes/produccion/pdf` |
| Ventas | `GET /reportes/ventas/excel` — 5 sheets (Ventas, Ítems, Por Cliente, Por Estado, Tendencia) | `GET /reportes/ventas/pdf` |
| Rentabilidad | `GET /reportes/rentabilidad/excel` — 3 sheets (Rentabilidad, Por Estilo, Tendencia Mensual) | `GET /reportes/rentabilidad/pdf` |

### ExcelExportService — notas críticas

- **`generarExcelReporteProduccion(lotes, resumen, ...)`**: el parámetro `List<Object[]> resumen` es **ignorado**. `construirSheetEstilos` calcula los datos directamente desde `lotes`. Tests que llamen a este método deben pasar `LoteCerveza` reales; pasar solo `resumen` sin lotes produce una hoja vacía.
- **`generarExcelReporteRentabilidad(filas, estilo, desde, hasta, ...)`**: recibe `List<RentabilidadLoteDto>` ya calculados por `ReporteController.buildRentabilidadFilas()`. Los 3 sheets usan los campos del DTO directamente.
- **Paleta multi-tenant**: todas las sheets usan `Pal.of(ExportBranding)` — los colores se calculan desde el branding del tenant. No hardcodear hex en `ExcelExportService`.

### PdfExportService — receta en multi-elaboración

- `addTablaInfoLote()`: cuando `numCoc >= 2`, el label de la receta principal (S1) usa `pdf.label.receta_s1` ("Receta Sesión 1") en lugar del genérico `pdf.label.receta` ("Receta"). S2/S3/S4 usan `pdf.label.receta_s2/s3/s4` respectivamente.

### detalle.html — receta S1 en multi-elaboración

- Bloque "Receta Sesión 1" visible solo con `lote.numeroElaboraciones >= 2 and lote.receta != null`, posicionado después del bloque Fermentador. En sesión única, la receta aparece en el encabezado del lote y no se repite aquí.

---

## MÓDULO TAREAS (`/tareas`)

Asignación y seguimiento de tareas operativas para usuarios del tenant. Ruta base: `/tareas`. Posición en navbar: entre BPM y Admin (link simple, sin dropdown). V75–V79.

### Entidades

- **`Tarea`** (tabla `tareas`): `titulo`, `descripcion`, `fechaVencimiento`, `prioridad` (enum `PrioridadTarea`), `estado` (enum `EstadoTarea`), `asignadoA` (String username — no FK), `creadoPor` (String username), `@TenantId`, `@CreatedDate` / `@LastModifiedDate`, `items` OneToMany→`TareaItem`, `referencias` OneToMany→`TareaReferencia`.
  - **11 FK de referencia legacy** (nullable, mantenidas por compat con datos pre-V79): `lote`, `equipo`, `insumo`, `elaboracion`, `ordenCompra`, `venta`, `cliente`, `factura`, `proveedor`, `receta`, `barril`. Agregadas en V76 (alta), V77 (media), V78 (baja).
  - `referencias` — `@OneToMany(cascade=ALL, orphanRemoval=true) @BatchSize(size=30)` — colección principal desde V79. `getRefEntries()` usa `referencias` si no está vacía; si está vacía cae al fallback de 11 FK (datos pre-V79).
  - `isVencida()`: `fechaVencimiento != null && now().isAfter(fechaVencimiento) && estado != COMPLETADA`
  - `getPorcentajeCompletado()`: `completados * 100 / items.size()`
  - **Métodos de referencia backward-compat**: `getRefTipo()`, `getRefId()`, `getRefLabel()`, `getRefUrl()` — evalúan `referencias` primero (primer entry), luego 11 FK.
- **`TareaItem`** (tabla `tarea_items`): `descripcion`, `completado` (Boolean), `ordenItem` (Integer), `@TenantId`, `tarea` ManyToOne. No extiende AuditableEntity.
- **`TareaReferencia`** (tabla `tarea_referencias`, V79): `tipo` (VARCHAR 30), `entidadId` (BIGINT), `label` (TEXT — denormalizado, guardado al crear), `url` (VARCHAR 500 — calculada server-side), `orden` (Integer), `@TenantId`, `tarea` ManyToOne. FK cascade ON DELETE CASCADE. No extiende AuditableEntity.

### Sistema de referencias múltiples (V79)

Desde V79 una tarea puede tener **N referencias de cualquier tipo**, incluyendo múltiples del mismo tipo (ej: dos Equipos). `TareaService.guardar()` y `actualizar()` reciben `List<String> refTipos, List<Long> refIds, List<String> refLabels` (tres listas paralelas). `resolverMultiplesReferencias` crea un `TareaReferencia` por cada entrada — el label se recibe del frontend (ya lo conoce el UI al seleccionar del suggest), la URL se calcula con `computeRefUrl(tipo, id)`. `limpiarReferencias(tarea)` llama `tarea.getReferencias().clear()` (orphanRemoval borra en BD) y además nulifica las 11 FK legacy.

V79 también migra los datos existentes de las 11 FK a `tarea_referencias` vía INSERT…SELECT con JOINs para calcular el label.

| Tipo | Entidad | Prioridad | Migración |
|---|---|---|---|
| `LOTE` | LoteCerveza | Alta | V76 |
| `EQUIPO` | Equipo | Alta | V75 |
| `INSUMO` | InsumoInventario | Alta | V76 |
| `ELABORACION` | ElaboracionPlanificada | Alta | V76 |
| `ORDEN_COMPRA` | OrdenCompra | Alta | V76 |
| `VENTA` | Venta | Alta | V76 |
| `CLIENTE` | Cliente | Media | V77 |
| `FACTURA` | FacturaProveedor | Media | V77 |
| `PROVEEDOR` | Proveedor | Media | V77 |
| `RECETA` | Receta | Baja | V78 |
| `BARRIL` | Barril | Baja | V78 |

### Endpoint suggest-ref

`GET /tareas/suggest-ref?tipo=XX&q=YY` — `@ResponseBody`, normaliza la respuesta de cada módulo a `{id, label, sub}`. Mínimo 2 caracteres en `q`. El formulario lo llama con debounce 260ms al escribir en el campo de búsqueda de referencia.

### Auto-recálculo de estado

`TareaService.recalcularEstado(tareaId)` — llamado en cada `toggleItem()`:
- Todos completados → `COMPLETADA`
- ≥1 completado → `EN_PROGRESO`
- 0 completados → `PENDIENTE`
- Si `items.isEmpty()` → no cambia el estado

### Notificaciones

- `TipoNotificacion.TAREA_ASIGNADA` — disparada en `guardar()` si `asignadoA != null && !asignadoA.equals(creadoPor)` (no se notifica auto-asignación), y en `actualizar()` si el usuario asignado cambió. Vinculada a `MODULO_TAREAS_VER` en `TIPO_AUTHORITY`.
- `TipoNotificacion.TAREA_VENCIMIENTO` — disparada por `AlertaScheduler` diariamente para tareas vencidas o que vencen hasta `LocalDate.now().plusDays(1)` (usa `LessThanEqual`, no coincidencia exacta). Deduplicación por día via `existeEnPeriodo`.
- `NotificacionService.TIPO_AUTHORITY` usa `Map.ofEntries()` (no `Map.of()`) para soportar los 7 tipos actuales.
- `NotificacionService.crearAlertaPlan` — lotes y usuarios se evalúan en bloques `if` **independientes** (no `else-if`) para que ambos límites disparen en la misma ejecución. Si ambos alcanzan su límite, se genera una sola `PLAN_LIMITE` notification con título y mensaje combinados (`"/"`). El `existeEnPeriodo` se chequea una sola vez antes del bloque.

### RBAC

- `ModuloApp.TAREAS` — añadido al enum
- SecurityConfig: `.requestMatchers("/tareas/**").access(modulo("TAREAS"))` (antes del bloque BPM)
- Roles por defecto en `RolTenantService`: Administrador (full), Producción (full), Recursos Humanos (full). Inventario/Facturación/Equipos no tienen acceso por defecto.

### Ítems vs Referencias — distinción conceptual

- **Ítems** (`TareaItem`): checklist de acciones a ejecutar *dentro* de la tarea (ej: "Lavar fermentador", "Verificar válvulas"). Se marcan completados uno a uno y determinan el progreso (0–100%). Son trabajo pendiente.
- **Referencias** (`TareaReferencia`): vínculos a registros del sistema que dan *contexto* a la tarea (ej: sobre qué Lote, Equipo u Orden es esta tarea). No se "completan" — son links de navegación. El label de cada referencia se genera automáticamente del nombre del registro seleccionado; el usuario no escribe ninguna descripción para la referencia.

### Toggle ítem AJAX

`POST /tareas/{tareaId}/items/{itemId}/toggle` — `@ResponseBody`, retorna JSON: `{completado: bool, estado: "PENDIENTE"|"EN_PROGRESO"|"COMPLETADA", pct: int}`. El JS en `detalle.html` usa CSRF via `<meta name="_csrf">` / `<meta name="_csrf_header">`.
- **Ownership check**: usa `itemRepo.findByIdAndTareaId(itemId, tareaId)` — lanza `EntityNotFoundException` si el ítem no pertenece a esa tarea. El controller devuelve **404** (no 400) para `EntityNotFoundException`; cualquier otra excepción devuelve 400.
- **JS**: verifica `response.ok` antes de llamar `r.json()` — si el server devuelve 404/5xx, no actualiza la UI.

### Formulario — referencia con dropdown + typeahead (multi-chip)

El formulario tiene `<select id="selectRefTipo">` + campo de búsqueda + dropdown AJAX. Se pueden agregar múltiples chips, incluso varios del mismo tipo. El JS mantiene `chips = [{key:"TIPO:id", tipo, id, label}]` (array, no map). La clave compuesta `tipo:id` impide duplicados exactos pero permite `EQUIPO:1` y `EQUIPO:2` en simultáneo. Al hacer submit se generan inputs hidden paralelos `refTipos[]`, `refIds[]`, `refLabels[]`. Al editar, `INIT_REFS` (inyectado por Thymeleaf desde `tarea.refEntries`) inicializa los chips. **No usar** selects separados por tipo — el sistema es data-driven vía `tiposReferencia()` en el controller.

### Performance — TareaRepository

- `@EntityGraph(attributePaths = "items")` en las 4 consultas `findAllBy*` usadas por `listar()` — evita N+1 al renderizar `t.items.size()` en `index.html`.
- `referencias` usa `@BatchSize(size=30)` en lugar de agregarse al EntityGraph (evita `MultipleBagFetchException` por dos colecciones List en el mismo JOIN FETCH). Con open-in-view activo, el batch-load se activa al renderizar la vista.
- `contarPorEstado()` usa `countGroupByEstado()` (JPQL GROUP BY, 1 query) en lugar de 4 `COUNT` separadas. El servicio llena el Map iterando `List<Object[]>` con cast a `EstadoTarea` + `Number`.
- `computeRefUrl(tipo, id)` en `TareaService` — switch expression que calcula la URL de detalle según el tipo sin cargar la entidad.

### Tests

- `TareaServiceTest` — requiere `@Mock` para los 13 repos que inyecta el constructor (incluye todos los repos de referencia — ya no se usan en `resolverMultiplesReferencias` pero siguen inyectados). Los stubs de toggle usan `itemRepo.findByIdAndTareaId(itemId, tareaId)` (no `findById`). El stub de `contarPorEstado` usa `repo.countGroupByEstado()` retornando `List<Object[]>`. El stub de `listarProximasAVencer` usa `findByFechaVencimientoLessThanEqualAndEstadoNot`. Las llamadas a `guardar` y `actualizar` reciben un `null` extra para `refLabels` (posición tras `refIds`).
- `TareaControllerTest` — stubs de `tareaService.guardar(...)` y `tareaService.actualizar(...)` necesitan 10 y 10 `any()` respectivamente (incluye el nuevo param `refLabels`). `MessageSource` es auto-configurado por Spring Boot — no necesita `@MockBean`.
- `AlertaSchedulerTest` — requiere `@Mock TareaService tareaService` + stub `lenient().when(tareaService.listarProximasAVencer(any())).thenReturn(List.of())` en `@BeforeEach`.

---

## CONVENCIONES DEL PROYECTO

La documentación técnica detallada del proyecto está dividida por tema en `docs/`:

- **[docs/estructura.md](docs/estructura.md)** — Estructura de paquetes (`config/`, `controller/`, `service/`, `model/`, etc.)
- **[docs/entidades.md](docs/entidades.md)** — Entidades y modelos JPA
- **[docs/repositorios.md](docs/repositorios.md)** — Repositorios y queries clave
- **[docs/servicios.md](docs/servicios.md)** — Servicios y lógica de negocio
- **[docs/controladores.md](docs/controladores.md)** — Controladores y endpoints
- **[docs/seguridad.md](docs/seguridad.md)** — Seguridad (Spring Security, JWT, multi-tenant, rate limiting)
- **[docs/docker-deploy.md](docs/docker-deploy.md)** — Asistente CLI, Docker, deploy
- **[docs/tests.md](docs/tests.md)** — Tests y archivos de prueba de migración
