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
- Micrometer + Prometheus — métricas de producción (`/actuator/prometheus`)
- OpenPDF 1.3.43 (`com.github.librepdf`) — generación de PDF (licencia LGPL/Apache). Clases en `com.lowagie.text.*`
- Spring Boot Starter Mail — envío de emails HTML vía SMTP. `JavaMailSender` solo se auto-configura si `spring.mail.host` está definido (no vacío). `EmailService` usa `@Autowired(required = false)` para soportar entornos sin SMTP.
- Apache POI 5.2.5 (`poi-ooxml`) — generación de Excel .xlsx. Clases en `org.apache.poi.xssf.usermodel.*`
- JJWT 0.12.6 (`jjwt-api` + `jjwt-impl` + `jjwt-jackson`) — generación y validación de tokens JWT HS256 para la API REST
- JUnit 5 + Mockito (unitarios) + Testcontainers (integración con PostgreSQL real)
- Tipografías: Cinzel (headings), Raleway (body)
- `spring.thymeleaf.cache=false` | `spring.jpa.hibernate.ddl-auto=validate`

## CONFIGURACIÓN

- Puerto: 8080
- BD: PostgreSQL localhost:5432/trazabilidad_cervezas
- Credenciales via variables de entorno: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`
- Usuarios adicionales por rol (opcionales): `PRODUCCION_USERNAME/PASSWORD`, `INVENTARIO_USERNAME/PASSWORD`, `FACTURACION_USERNAME/PASSWORD`, `EQUIPOS_USERNAME/PASSWORD`
- Flyway: `baseline-on-migrate=true`, migraciones en `db/migration/` (V1–V65). En producción usa credenciales separadas: `FLYWAY_USERNAME=zymos_flyway` / `FLYWAY_PASSWORD` (rol con DDL); si no se definen, usa `DB_USERNAME`/`DB_PASSWORD` como fallback. Ver `db_security.sql` para crear los roles.
- Sesión: timeout 30 minutos de inactividad (`server.servlet.session.timeout=30m`)
- Docker: `Dockerfile` + `docker-compose.yml` disponibles en raíz del proyecto
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

---

## PALETA DE COLORES (style.css)

- Verde oscuro: `#242E0D` (navbar) — CSS var `--verde-oscuro`
- Verde Alera: `#364318` (hero, section-headers) — CSS var `--verde-alera`
- Dorado: `#C9A028` (acento principal) — CSS var `--dorado`
- Dorado claro: `#E0B840` (hover) — CSS var `--dorado-claro`
- Crema: `#F5EDD0` (texto sobre fondos oscuros) — CSS var `--crema`
- Fondo body: `#F0EDE2` — CSS var `--fondo`
- Dark mode: fondo `#111606`, cards `#1c2410`, texto crema — activado con clase `html.dark-mode`. Variables centralizadas `--dm-*` en `style.css` (bloque `:root`): `--dm-bg`, `--dm-card`, `--dm-input`, `--dm-text`, `--dm-text-muted`, `--dm-text-dim`, `--dm-text-dimmer`, `--dm-border-faint`, `--dm-border-light`, `--dm-border-med`, `--dm-border-heavy`, `--dm-hover`, `--dm-verde-bg`, `--dm-verde-border`, `--dm-verde-faint`. Los templates con `<style>` inline propio incluyen también un bloque `html.dark-mode` local al final de ese `<style>`, usando las vars `--dm-*`.
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
2. **Generación código lote**: 3 primeras letras del estilo → "IPA" → IPA-001, IPA-002...
3. **Descuento automático inventario**: al crear/actualizar/eliminar lote. Retorna advertencias si stock insuficiente (no bloquea). Al crear/editar facturas, el inventario se actualiza automáticamente (suma en guardar, revierte + suma en actualizar, revierte en eliminar).
4. **Normalización unidades** (via UnidadUtils): kg→gr (×1000), L→mL (×1000), gal→mL (×3785.41).
5. **IVA por ítem**: facturas no tienen IVA global. Cada ítem tiene su propio `porcentajeIvaItem`.
6. **Sincronización facturas**: al actualizar/eliminar, revierte inventario anterior antes de aplicar cambios.
7. **Vencimientos**: alerta ≤30 días en dashboard.
8. **Thymeleaf — CRÍTICO**: `eq`, `ne`, `lt`, `gt`, `le`, `ge` son palabras reservadas. NO usar como variables de iteración en `th:each`. Síntoma: `Iteration variable cannot be null` en `Each.<init>` al renderizar el template. Caso real: `th:each="eq, stat : ${...}"` en `emails/alertas.html` → renombrado a `equipo` (2026-06-07).
8b. **Thymeleaf — colecciones en SpEL**: para filtrar una colección en `th:if` usar el operador de selección SpEL `?[]` en lugar de lambdas Java: `${!#lists.isEmpty(lista.?[campo != null and !campo.isEmpty()])}`. Evita dependencia de la versión exacta de SpEL y es más legible. Alternativa en `th:each` con `th:if` anidado.
8c. **Thymeleaf — `th:onclick` con Strings bloqueado en 3.1+**: Thymeleaf 3.1 rechaza expresiones que devuelven strings en atributos de event handlers DOM (`th:onclick`, `th:onchange`, etc.) por seguridad XSS. Solo se permiten números y booleanos. Solución: usar atributos `data-*` con `th:attr` y leer desde un handler JS con `this.dataset.*`. Ejemplo — en lugar de `th:onclick="'abrirModal(' + ${p.nombre} + ')'"` usar `th:attr="data-nombre=${p.nombre}" onclick="abrirModalDesdeBtn(this)"` con `function abrirModalDesdeBtn(btn) { abrirModal(btn.dataset.nombre); }`.
8d. **Thymeleaf — `th:with` y `th:if` en el mismo elemento**: `th:if` (precedencia 40) se procesa ANTES que `th:with` (precedencia 600). Una variable definida con `th:with` en el mismo elemento es `null` cuando `th:if` la evalúa → `SpelEvaluationException: cannot convert from null to boolean`. Solución: nunca usar en `th:if` una variable del `th:with` del mismo elemento; en su lugar, repetir la expresión inline en `th:if`, o poner `th:with` en un elemento padre contenedor.
8e. **Thymeleaf — `#{}` NUNCA dentro de `${}`**: `#{}` es una expresión Thymeleaf de mensajes; `${}` es SpEL. SpEL no reconoce `#{}` y lanza `SpelParseException: Unexpected token 'lcurly({)'`. Error típico: `${valor != null ? valor : #{clave}}`. Solución: mover el ternario al nivel Thymeleaf: `th:text="${valor != null} ? ${valor} : #{clave}"`. Con helpers: `${cond} ? ${#temporals.format(campo,'...')} : #{clave}`.
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
18. **FacturaProveedor**: tiene dos campos de proveedor: `proveedor` (String original) y `proveedorRef` (FK LAZY nullable). Coexisten para compat. histórica. El campo de fecha es `fechaFactura` — **NO** `fecha`. En JPQL: `f.fechaFactura`; en Java: `getFechaFactura()`. Escribir `f.fecha` en un `@Query` provoca `UnknownPathException` al arrancar. El flag `ivaIncluido` (boolean, default false) indica si los valores unitarios de los ítems ya incluyen IVA — `FacturaItem.getValorUnitarioSinIva()` hace la extracción automáticamente consultando `factura.isIvaIncluido()`.
19. **Fechas en filtros JPQL**: para `LocalDate` nullable usar `CAST(:param AS LocalDate) IS NULL OR campo >= :param` — el `CAST` fuerza el tipo del parámetro y evita el error de PostgreSQL `"no se pudo determinar el tipo del parámetro $N"` (SQLState 42P18) que ocurre cuando el parámetro es `null` sin contexto de tipo. Patrón sin `CAST` (`(:param IS NULL OR ...)`) no funciona con PostgreSQL + Hibernate 6.
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
33. **Calculadora de escala (recetas/detalle)**: escala ingredientes, agua macerado y agua sparge. Variables JS: `AGUA_MACERADO`, `UNIDAD_MACERADO`, `AGUA_SPARGE`, `UNIDAD_SPARGE` (inline Thymeleaf). Al resetear llama `resetAgua()`.
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

45. **Multi-cocción — arquitectura**: `LoteCerveza` soporta 1, 2 o 3 cocciones controladas por `numeroCocciones` (Integer). Campos por sesión (todos nullable, activados según n):
    - Sesión 1: `ogPrimeraCoccion` (Integer SG), `volumenFinalPrimeraCoccion` (BigDecimal L)
    - Sesión 2: `fechaSegundaCoccion`, `aguaSegundaCoccion`, `ogSegundaCoccion` (SG) / `ogBrixSegundaCoccion` (BigDecimal Brix), `volumenFinalSegundaCoccion`
    - Sesión 3: ídem con `Tercera`. En formulario: `#segunda-coccion-section` / `#tercera-coccion-section` (ocultos por `applyCocciones()`). OG combinado = media ponderada por volumen en SG. `sincronizarVolumenFinalTotal()` en `trazabilidad-ingredientes.js` auto-suma los volúmenes de sesión en `litrosFinales`. `densidadInicial` siempre visible; readonly en Brix (calculado desde °Brix via `calcularSgDesdeBrix()`). `densidadFinalFecha` es campo común fuera de los bloques `#modo-sg` / `#modo-brix`.

46. **Formulario — mover nodos DOM entre secciones**: cuando un campo debe aparecer en distinto lugar según estado del formulario (sin duplicar `name`), capturar `parentNode` y `nextSibling` antes del primer `applyCocciones()` y usar `anchor.after(col)` / `origParent.insertBefore(col, origNextSib)` para moverlo físicamente. Patrón implementado en Agua Utilizada (`#agua-general-col`) que se mueve a Primera Sesión de Cocción cuando n≥2.

47. **TrazabilidadController — redirect POST /guardar**: al crear un lote nuevo, el controller redirige a `redirect:/editar/{id}` (igual que `POST /actualizar/{id}`), NO a la lista `redirect:/`. La variable `loteId` es `Long` (objeto, no primitivo) — si fuera `long` y el ID fuera null, el autoboxing lanzaría NPE y el catch devolvería `"danger"` silenciosamente. Fallback a `redirect:/` solo si `loteId == null` (no ocurre en producción; el repo siempre asigna ID tras `save()`). **Tests**: los mocks de `service.guardar()` deben llamar `lote.setId(N)` antes de construir `LoteGuardadoResult`; de lo contrario `loteId` es null y la URL de redirección esperada es `"/"` en vez de `"/editar/N"`.

42. **Plan de tenant — alertas y bloqueo**: `AlertaScheduler` llama diariamente a `NotificacionService.crearAlertaPlan(tenant, totalLotes, totalUsuarios)` para generar notificaciones in-app (`PLAN_VENCIMIENTO`, `PLAN_LIMITE`) cuando el plan está vencido/por vencer (`Tenant.isPlanVencido()`/`isPlanPorVencer()`, ≤7 días) o cerca/sobre los límites `maxLotes`/`maxUsuarios` (≥90%/100%). Si `planFin + app.plan.dias-gracia (def: 7)` ya pasó, `TenantFilter` redirige todas las rutas (excepto `/plan-vencido`, `/logout`, `/login*`) a `/plan-vencido` (`PlanController` + `templates/plan/vencido.html`).

---

## CONVENCIONES DEL PROYECTO

- Flash attributes: `"mensaje"` y `"tipoMensaje"` (success, warning, danger) — Bootstrap Toast top-right, auto-dismiss 5s, en navbar.html
- Templates heredan navbar: `th:replace="~{fragments/navbar :: navbar}"`
- Paginación: tamaño configurable via `app.page-size=15` (default). Fragmento `paginacion.html` recibe `baseUrl`, `extraParams`, `paginaActual`, `totalPaginas`
- Logging: INFO operaciones exitosas, WARN stock insuficiente/logins fallidos, ERROR excepciones
- Todos los servicios `@Transactional`. DashboardService `@Transactional(readOnly = true)`
- Bean Validation en DTOs + `@DateTimeFormat(iso=DATE)` en todos los campos `LocalDate`
- **Visualización de cantidades en templates — regla obligatoria**: para valores `BigDecimal` con unidad (litros, °C, %, gramos, kg, etc.), usar siempre `valor.stripTrailingZeros().toPlainString()` en lugar de `#numbers.formatDecimal`. Esto elimina ceros decimales superfluos: `5.000` → `5`, `2.500` → `2.5`, `2.501` → `2.501`. Para valores nullable: `${valor != null ? valor.stripTrailingZeros().toPlainString() + ' L' : '—'}`. Para expresiones aritméticas Java (Integer − Integer) * double que producen `Double` en SpEL y no soportan `stripTrailingZeros()` directamente, envolver con `T(java.math.BigDecimal).valueOf(expr).stripTrailingZeros().toPlainString()`. En JavaScript inline (cálculos en tiempo real), usar el helper `fmtNum`: `function fmtNum(n, maxDec) { return parseFloat((+n).toFixed(maxDec)).toString(); }` — reemplaza `.toFixed()` en displays de usuario. **No aplicar** a: valores de `<input th:value>` (binding de formulario), arrays de datos para Chart.js, valores monetarios con formato fijo `#numbers.formatDecimal(v,1,'COMMA',2,'POINT')`.
- Dark mode: toggle luna/sol en navbar, clase `html.dark-mode` en `<html>`, persiste en `localStorage`. Variables centralizadas `--dm-*` en `style.css` (usarlas siempre en reglas `html.dark-mode`, no hardcodear hex oscuros). Los `<style>` inline de templates con dark mode propio incluyen el bloque `html.dark-mode {...}` al final del mismo `<style>`. `!important` requerido para sobreescribir inline styles de elementos (ej: `kanban-dias`, `.subtipo-placeholder`).
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

## DOCUMENTACIÓN DETALLADA (`docs/`)

La documentación técnica detallada del proyecto está dividida por tema en `docs/`:

- **[docs/estructura.md](docs/estructura.md)** — Estructura de paquetes (`config/`, `controller/`, `service/`, `model/`, etc.)
- **[docs/entidades.md](docs/entidades.md)** — Entidades y modelos JPA
- **[docs/repositorios.md](docs/repositorios.md)** — Repositorios y queries clave
- **[docs/servicios.md](docs/servicios.md)** — Servicios y lógica de negocio
- **[docs/controladores.md](docs/controladores.md)** — Controladores y endpoints
- **[docs/seguridad.md](docs/seguridad.md)** — Seguridad (Spring Security, JWT, multi-tenant, rate limiting)
- **[docs/docker-deploy.md](docs/docker-deploy.md)** — Asistente CLI, Docker, deploy
- **[docs/tests.md](docs/tests.md)** — Tests y archivos de prueba de migración
