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
- Flyway: `baseline-on-migrate=true`, migraciones en `db/migration/` (V1–V52). En producción usa credenciales separadas: `FLYWAY_USERNAME=zymos_flyway` / `FLYWAY_PASSWORD` (rol con DDL); si no se definen, usa `DB_USERNAME`/`DB_PASSWORD` como fallback. Ver `db_security.sql` para crear los roles.
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

## ESTRUCTURA DE PAQUETES

```
com.alera/
├── config/     SecurityConfig, JpaConfig (@EnableJpaAuditing), AuditorAwareImpl,
│               DataInitializer, GlobalExceptionHandler, GlobalControllerAdvice, UnidadUtils,
│               CacheConfig (@EnableCaching + Caffeine), SchedulingConfig (@EnableScheduling),
│               OpenApiConfig (Swagger),
│               ZymosAuthSuccessHandler, ZymosAuthFailureHandler, ZymosAccessDeniedHandler,
│               LoginAttemptService (protección fuerza bruta — cache Caffeine por IP),
│               LoginAttemptFilter (OncePerRequestFilter — creado como @Bean en SecurityConfig, no @Component),
│               PasswordPolicy (utilidad estática — MIN_LENGTH=8, requiere letra + número; `validar(pwd)` retorna null si OK o mensaje de error),
│               BrandingProperties (@ConfigurationProperties prefix=app.brand),
│               ExportBranding (record — pre-parsea colores del tenant en java.awt.Color para PDF/Excel; ExportBranding.from(Tenant) y ExportBranding.defaults(name); helper lighten(Color,float)),
│               TenantContext (ThreadLocal), TenantFilter (OncePerRequestFilter),
│               TenantIdentifierResolver (CurrentTenantIdentifierResolver<String>),
│               HibernateMultiTenancyConfig (HibernatePropertiesCustomizer),
│               JwtFilter (OncePerRequestFilter — valida Bearer tokens para /api/**; creado como @Bean en SecurityConfig, no @Component),
│               ApiRateLimitFilter (OncePerRequestFilter — rate limiting /api/** por IP; ventana fija 1 min via Caffeine; creado como @Bean en SecurityConfig, no @Component)
├── exception/  EquipoEnUsoException, LoteNoEncontradoException
├── controller/ 27 controladores:
│               TrazabilidadController, DashboardController, EquipoController,
│               FacturaProveedorController, InsumoInventarioController,
│               RecetaController, ProveedorController, CalendarioController,
│               ReporteController, BusquedaController, AdminController, ApiController,
│               TipoCervezaController, UsuarioController, MantenimientoController,
│               LoginController, TenantAdminController, ComparativaController, AlertaController,
│               PlanificacionController, PerfilController, NotificacionController,
│               MigracionController (/admin/migracion — plantillas + importación por módulo),
│               AuthController (POST /api/auth/login — obtención de token JWT),
│               CustomErrorController (GET /error — intercepta el endpoint de error de Spring Boot),
│               ClienteController (/clientes — CRUD + suggest; ADMIN/FACTURACION/SUPERADMIN)
│               CategoriaController (/admin/categorias — CRUD categorías de insumo y equipo; ADMIN/SUPERADMIN)
│               BarrilController (/barriles — CRUD + cambiar estado; ADMIN/INVENTARIO/PRODUCCION/SUPERADMIN)
│               OrdenCompraController (/ordenes-compra — CRUD + cambiar estado + convertir a factura + PDF; ADMIN/FACTURACION/SUPERADMIN)
├── service/    TrazabilidadService, RecetaService, EquipoService, FacturaProveedorService,
│               InsumoInventarioService, ProveedorService, LogAccesoService,
│               DashboardService, MantenimientoEquipoService, TipoCervezaService,
│               UsuarioService (implements UserDetailsService — integración Spring Security),
│               TenantService, PdfExportService, ExcelExportService, LecturaFermentacionService, PlanificacionService,
│               EmailService, AlertaScheduler, NotificacionService, MigracionTemplateService, MigracionService,
│               EvaluacionSensorialService,
│               BarrilService,
│               OrdenCompraService,
│               TenantMetricsService (JdbcTemplate cross-tenant — 14 queries para producción/ventas/compras/inventario/equipos/usuarios/último acceso; inner record `TenantMetrics` con campos: `totalLotes`, `lotesEnProceso`, `lotesCompletados`, `litrosTotales` — producción; `totalVentas`, `ingresosVentas`, `totalClientes` — ventas; `totalFacturas`, `totalGastado` — compras; `totalInsumos`, `bajoStock`, `totalEquipos` — inventario; `totalUsuarios`, `ultimoAcceso` — sistema; método público `obtener(String tenantId) → TenantMetrics`),
│               ClienteService,
│               CategoriaInsumoService, CategoriaEquipoService,
│               JwtService (generación/validación tokens HS256 — secret via @Value, claims: subject=username, tenant, rol)
├── model/      30 entidades:
│               AuditableEntity (@MappedSuperclass — base de auditoría + @TenantId),
│               Tenant (tabla tenants — subdomain PK + branding),
│               LoteCerveza, Ingrediente, Receta, RecetaIngrediente, EscalonMacerado,
│               AdicionHervor, HistorialLote, LogAcceso, Equipo, MantenimientoEquipo,
│               InsumoInventario, FacturaProveedor, FacturaItem,
│               Proveedor, TipoCerveza, Usuario,
│               LoteItemFactura (tabla lote_items_factura — asignación parcial de ítems a lotes),
│               Notificacion (tabla notificaciones — notificaciones in-app persistentes por tenant),
│               FacturaHistorialEstado (tabla factura_historial_estado — auditoría de cambios de estado por factura),
│               MigracionLog (tabla migracion_log — historial de importaciones por tenant, sin @TenantId),
│               VentaItem (tabla venta_items — ítems de venta, multi-lote por venta),
│               Cliente (tabla clientes — datos fiscales y de contacto del cliente; extiende AuditableEntity)
│               CategoriaInsumo (tabla tipos_insumo — categorías de insumo por tenant; @TenantId),
│               CategoriaEquipo (tabla tipos_equipo — categorías de equipo por tenant; @TenantId),
│               EvaluacionSensorial (tabla evaluaciones_sensoriales — catas BJCP por lote; @TenantId)
│               Barril (tabla barriles — inventario de kegs/barriles; extiende AuditableEntity),
│               MovimientoBarril (tabla movimientos_barriles — historial de cambios de estado; @TenantId directo, sin FK)
│               OrdenCompra (tabla ordenes_compra — órdenes de compra a proveedores; @TenantId),
│               OrdenCompraItem (tabla orden_compra_items — ítems de la OC; @TenantId)
│               + 14 enums (incluye RolUsuario: ADMIN, PRODUCCION, INVENTARIO, FACTURACION, EQUIPOS;
│               EstadoPlanificacion: PLANIFICADA, EN_PROCESO, COMPLETADA, CANCELADA;
│               EstadoFactura: RECIBIDA, VERIFICADA, PAGADA;
│               EstadoVenta: COTIZACION, PENDIENTE, DESPACHADO, CANCELADO, EXPIRADO;
│               TipoNotificacion: BAJO_STOCK, VENCIMIENTO, MANTENIMIENTO, SISTEMA;
│               ListaPrecio: VENTA_DIRECTA, DISTRIBUIDOR, BAR, MAYORISTA, EXPORTACION, EMPLEADO;
│               RegimenTributario: SIMPLIFICADO, RESPONSABLE_IVA;
│               EstadoBarril: DISPONIBLE, LLENO, DESPACHADO, VACIO, LIMPIEZA, BAJA;
│               EstadoOrdenCompra: BORRADOR, ENVIADA, RECIBIDA_PARCIAL, RECIBIDA, CANCELADA)
├── repository/ 15 repositorios JPA (+ CategoriaInsumoRepository, CategoriaEquipoRepository, TenantRepository, FacturaItemRepository, LecturaFermentacionRepository, EvaluacionSensorialRepository,
│               ElaboracionPlanificadaRepository, NotificacionRepository, FacturaHistorialEstadoRepository,
│               MigracionLogRepository, VentaItemRepository, ClienteRepository,
│               BarrilRepository, MovimientoBarrilRepository,
│               OrdenCompraRepository, OrdenCompraItemRepository)
├── dto/        LoteFormDto, LoteGuardadoResult, InsumoDto, FacturaFormDto,
│               FacturaItemDto (campos numéricos `cantidad`, `valorUnitario`, `porcentajeDescuento`, `porcentajeIvaItem`, `impuestoConsumo` sin valor por defecto — `null`; el servicio tiene fallbacks null-safe; el formulario muestra placeholder en lugar de 0), MantenimientoDto, DashboardStats,
│               RecetaFormDto (incluye EscalonDto y AdicionHervorDto inner classes),
│               AlertaContadores (bajoStock, vencimientos, mantenimiento + getTotal() — devuelto por AlertaController),
│               AuthRequest (@NotBlank username + password — body de POST /api/auth/login),
│               AuthResponse (token, tipo="Bearer", expiresIn, username, rol — respuesta del login JWT),
│               VentaFormDto (@NotNull Long clienteId, String cliente (sin @NotBlank — retrocompat.), fechaDespacho, estado, cotizacionExpiraEn (@DateTimeFormat ISO DATE), notas, List<VentaItemFormDto> items),
│               VentaItemFormDto (loteId, descripcion, cantidad, unidad, precioUnitario, descuentoPct),
│               ClienteFormDto (@NotBlank nombre, razonSocial, nit, regimenTributario, @Email email, telefono, direccionDespacho, ciudad, departamento, listaPrecio, activo, notas)
└── mapper/     LoteMapper (MapStruct — LoteCerveza → LoteFormDto),
                MantenimientoMapper (MapStruct — MantenimientoDto → MantenimientoEquipo, ignora `id` y `equipo`)

templates/
├── fragments/  navbar.html (dropdowns Producción/Almacén/Comercial/Admin + botón `+` acciones rápidas + campana notificaciones in-app + búsqueda global con typeahead + dropdown usuario con rol badge + perfil), paginacion.html
├── error/      error.html
├── trazabilidad/ index.html (4 stat-cards: Total Lotes → `/`, En Proceso → `/kanban` (`.info`), Completados → `/?fase=COMPLETADO` (`.success`), Estilos Únicos (`.muted`) — datos inyectados por `TrazabilidadController` desde `loteRepo.count()`, `countEnProceso()`, `countCompletados()`, `countDistinctEstilos()`; filtros con typeahead en campo "Estilo / Código" busca por codigoLote o estilo, badge de fase; Tab "General e Insumos": columnas Maltas/Lúpulos/Levadura tienen `d-none d-md-table-cell` — se ocultan en mobile < 768px; Tab "Carbonatación": columnas Método, CO₂ Obj. y CO₂ Real con `d-none d-lg-table-cell` — solo visibles en ≥992px),
│               formulario.html (wizard de 6 tabs; tab completado muestra ✓ verde en el círculo numerado vía clase `.wz-tab.done` — `goTab()` en `trazabilidad-ingredientes.js` la añade/remueve en el loop; todos los `<label>` tienen `for` apuntando al `id` del campo correspondiente; **Tab 5 Carbonatación** rediseñado en 3 capas: [1] fecha inicio/fin + temperatura + CO₂ objetivo + selector de método, [2] sección condicional `#seccionNatural` (tipo de azúcar/agente — select dinámico: optgroup "📦 Del inventario" con insumos `TipoInsumo.AGENTE_CARBONATACION` si los hay, luego optgroup "Otro" con Dextrosa/Sacarosa/Extracto/Miel estáticos; gramos calculados; alert con fórmula priming; botón "Agregar al costo de producción" que auto-agrega el agente a la sección Costos con cantidad en gramos convertida a la unidad del ítem de factura) o `#seccionForzada` (presión PSI, tiempo horas, técnica), [3] validación organoléptica + **destino y empaque multi-formato**: campo hidden `carbDestino` compuesto por UI dinámica de filas (select tipo envase + input cantidad + botón eliminar) via JS `addDestinoRow(formato,cantidad)`, `removeDestinoRow(btn)`, `onDestinoRowChange(sel)`, `componerDestino()`, `initDestino()`; soporta múltiples formatos separados por `" | "` (ej. `"48 × Botella 330ml | 2 × Barril 20L"`); `DESTINO_OPTS` string con opciones predefinidas (Botella 330/500/750ml, Lata 330/473ml, Barril 20/30/50L, Growler, Keg, A granel, Otro personalizado); JS inline `toggleMetodoCarb()`, `sugerirCo2PorEstilo()`, `actualizarCalculadora()`, `factorParaAgente(nombre)`, `agregarAgenteCostoSugerido()`), detalle.html (detalle incluye card "Carbonatación — Detalle" entre fases y curva de fermentación — visible si `carbMetodo != null or carbCo2Objetivo != null or carbDestino != null`; muestra badge método, comparativa CO₂, badge validación, **destino como badges por entrada** (`#strings.split(lote.carbDestino,'|')` con `#strings.trim()`), y fila condicional de parámetros; sección "Curva de Fermentación" con Chart.js dual-eje + tabla + formulario inline; JS de formulario y detalle en `static/js/`),
│               kanban.html (SortableJS 1.15.2 — drag & drop entre 6 columnas; ADMIN y SUPERADMIN pueden arrastrar; `esAdmin = hasAnyRole('ADMIN','SUPERADMIN')`; cada card tiene `data-tiene-fermentador` para validación client-side; dark mode de headers (`.col-{fase} .kanban-col-header`) y badges de días (`.kanban-dias`) con colores por fase usando `!important` para vencer inline styles; JS en `static/js/trazabilidad-kanban.js`)
├── login.html, dashboard.html (personalizable — 6 secciones drag-sortable: Stats Lotes, Inventario, Alertas, Próximas Elaboraciones, Gráficas, Finanzas; stat-cards clickeables con `.stat-card-link`; colores de tabla y Chart.js leen CSS vars en runtime; sección "Finanzas" usa `.table-card` con sub-componente `.fin-metric`/`.fin-label`/`.fin-value` para métricas financieras; sección "Últimos Lotes" usa `.table-card` con `thead th` estilo moderno (`background:#f8fafc !important; color:#64748b !important`); sección "Planificación" aún usa `.card-zymos` con botón "Ver calendario" → `btn-outline-secondary btn-sm`), calendario.html, busqueda.html
├── usuarios.html  (tabla con modales: nuevo usuario, cambiar contraseña, cambiar rol; fila del usuario en sesión marcada y botones destructivos deshabilitados; typeahead en card-header, `th:id="'usuario-'+${u.id}"` en cada `<tr>`, click hace scroll+flash `:target` dorado)
├── perfil/     password.html (formulario autogestionado de cambio de contraseña — accesible todos los roles via `GET /perfil/password`)
├── equipos/    lista (4 stat-cards + typeahead en card-header respeta filtro estado + select de cambio rápido de estado por fila), formulario (todos los `<label>` tienen `for`), mantenimientos (muestra totalMantenimientos + costoTotal en el header del historial), detalle (nuevа — 4 stat-cards + datos del equipo + selector de estado + historial completo)
├── inventario/ lista (typeahead en campo nombre respeta filtro tipo), formulario (todos los `<label>` tienen `for`),
│               precios.html (buscador con datalist + 4 stat-cards + Chart.js barras + tabla de compras)
├── tipos-cerveza/ lista
├── facturas/   lista (typeahead en card-header busca por N° o proveedor; 4 stat-cards: total facturas, monto total, pendientes de pago, monto pendiente), formulario (toggle "El precio ya incluye IVA" — cambia la etiqueta de la columna V. Unitario y recalcula totales en tiempo real; columna **Imp. Consumo** (valor fijo, no porcentaje) incluida en `calcularTotalFila()` JS; `.subtipo-placeholder` con dark mode), detalle (historial de cambios de estado + botón Duplicar + badge "Precio con IVA incluido" + columna "V. sin IVA" condicional + columna "Imp. Consumo" en tabla de ítems; Resumen Financiero muestra fila "Imp. Consumo" condicional cuando total > 0; cuando `costoEnvio > 0` muestra dos totales: "Total sin envío" (semibold, antes de la fila Envío) y "TOTAL (con envío)" como total final — sin envío solo muestra "TOTAL")
├── recetas/    lista (tabla paginada con filtros activa/inactiva + typeahead a la derecha; respeta filtro estado), formulario (todos los `<label>` tienen `for`), detalle (+ calculadora escala)
├── proveedores/ lista (typeahead en card-header busca por nombre o NIT), formulario (todos los `<label>` tienen `for`)
├── reportes/   produccion.html (8 stat-cards, 3 gráficos Chart.js, tabla con paginación client-side, resumen por estilo con barras de progreso; colores leen CSS vars del tenant en `DOMContentLoaded`)
├── comparativa/ seleccion.html (tabla con checkboxes, filtro por código/estilo, máx. 6 lotes),
│               resultado.html (tabla transpuesta con métricas por columna + Chart.js grouped bar)
├── planificacion/ index.html (4 stat-cards calculadas con SpEL `?[]` sobre lista `todas` inyectada por el controller: Total (`#lists.size(todas)`), Planificadas (`?[estado.name() == 'PLANIFICADA'].size()`, `.warning`), En Proceso (`?[estado.name() == 'EN_PROCESO'].size()`, `.info`), Completadas (`?[estado.name() == 'COMPLETADA'].size()`, `.success`) — sin cambios en controller ni repositorio; FullCalendar + panel próximas + tabla completa + modal crear/editar)
│               — dateClick → modal nuevo con fecha pre-llenada; eventClick → modal editar con extendedProps
│               — botón Editar en tabla usa `data-*` attrs (`th:attr`) + `onclick="abrirModalEditarDesdeBtn(this)"` para pasar strings sin violar restricción Thymeleaf 3.1 (regla 8c)
├── notificaciones/ index.html (historial paginado con badges por tipo, marcar leída por fila, marcar todas, paginación)
├── clientes/   lista.html (stat-card totalClientes, filtros nombre+activo con tabs Activos/Inactivos, typeahead `/clientes/suggest`, tabla: nombre/razón social, NIT, ciudad/dpto, listaPrecio badge, activo badge, botones ver/editar/toggle), formulario.html (secciones: Identificación — nombre*, razón social, NIT, régimen tributario select, lista de precio select; Contacto — email, teléfono; Dirección de despacho — dirección, ciudad, departamento; Notas), detalle.html (hero con nombre+badges activo+listaPrecio+NIT, cards: Identificación fiscal, Contacto y Ubicación, Notas Internas; panel lateral: Acciones — Registrar Venta / Editar / Toggle activo; Registro — auditoría AuditableEntity)
│               ventas/  formulario.html — cliente seleccionado via typeahead (`/clientes/suggest?q=`) que llena hidden `clienteId` y chip de nombre+NIT; campo "Válida hasta" (`cotizacionExpiraEn`) visible/oculto con JS `toggleCotizacionField()` según estado COTIZACION; estado select filtra EXPIRADO (`th:if="${est.name() != 'EXPIRADO'}"`); `estadoInicial` inyectado como Thymeleaf inline JS para mostrar/ocultar campo en carga inicial.
│               ventas/  detalle.html — hero muestra badge remisionNumero si `venta.remisionNumero != null`; card "Info General" muestra NIT, lista de precio y link "Ver ficha" desde `venta.clienteRef` (lazy, open-in-view); muestra `cotizacionExpiraEn` cuando estado es COTIZACION o EXPIRADO.
└── admin/      logs.html, tenants.html (lista de tenants con cards + franja de colores + botón "Limpiar cache" → `POST /admin/tenants/cache/evict` + botones por card: "Usuarios" → `/admin/tenants/{subdomain}/usuarios`, "Métricas" → `/admin/tenants/{subdomain}/metricas`, "Migración" → `/admin/migracion/{subdomain}`; badges de vigencia del plan en cada card: `bg-danger` "Vencido", `bg-warning text-dark` "Por vencer" (≤7 días), `bg-info text-dark` con fecha cuando vigente — condicionales via `t.planVencido` / `t.planPorVencer` / `t.planFin`),
                tenant-formulario.html (crear/editar tenant con color pickers y preview en vivo del navbar + selectores de tipografía con preview en vivo — `fontHeadings` y `fontBody`; campo `logoUrl` es `type="text"` para aceptar rutas relativas `/img/` además de URLs externas; sección "Límites del plan" con: select `planTipo` (MENSUAL/TRIMESTRAL/SEMESTRAL/ANUAL/BIANUAL), datepicker `planInicio`, display readonly de `planFin` calculado con JS + indicador de estado en color (verde=vigente, amarillo=≤7 días, rojo=vencido), campos numéricos `maxLotes` y `maxUsuarios`; JS inline `onPlanTipoChange()`, `actualizarPlanFin()` — calcula la fecha de vencimiento en el cliente antes de guardar),
                tenant-usuarios.html (gestión de usuarios por tenant: tabla con toggle activo/inactivo, cambiar contraseña, cambiar rol, eliminar + modal "Nuevo Usuario"; todas las queries usan SQL nativo explícito — ver regla 40),
                tenant-historial.html (auditoría de cambios del tenant: tabla fecha/acción/usuario/detalles; badges de color por tipo de acción),
                tenant-formulario.html (edición) incluye sección "Importar / Exportar": botón Exportar JSON, form upload Importar JSON, select "Copiar de..." + botón AJAX que llama `/config` y rellena el form con previews en vivo,
                migracion/detalle.html (página de migración por tenant: instrucciones generales, 6 cards de módulo cada una con descarga de plantilla + formulario de carga, historial de importaciones con badge de estado y modal de errores; módulos: almacen, equipos, comercial, produccion, clientes, ventas)
│               categorias.html (gestión de categorías de insumo y equipo: dos tabs con tabla CRUD + formulario de creación por tipo)
├── barriles/   lista.html (4 stat-cards: Total/Disponibles/Llenos/Despachados + filtros codigo+estado + tabla con badges de estado), formulario.html (CRUD con campos: codigo, tipo, capacidadLitros, estado, codigoLote, clienteNombre, fechaDespacho, observaciones), detalle.html (hero con código+estado, historial de movimientos, panel cambiar estado, registro de auditoría, zona de peligro)
├── ordenes-compra/ lista.html (4 stat-cards: Total OC/Borrador/Enviadas/Recibidas + filtro por estado + tabla con N° OC/proveedor/fechas/estado/ítems/total estimado + botones ver/editar/PDF), formulario.html (datos generales: proveedor typeahead → `/proveedores/suggest`, fechaEmision, fechaRequerida, notas; tabla de ítems dinámica con Tipo select/Categoría select/**Nombre datalist sugiere insumos/equipos del inventario filtrados por categoría**/descripción/cantidad/unidad/precio/IVA%; total estimado en tiempo real; JS: `INSUMOS_POR_TIPO` y `EQUIPOS_POR_TIPO` serializados como JSON, `onTipoChange()`/`onCategoriaChange()`/`addItemRow()`/`removeRow()`/`recalcular()`), detalle.html (info general + estado + ítems + historial de estado + botones cambiar estado + convertir a factura + PDF)
```

### Migraciones Flyway
- `V1__initial_schema.sql` — esquema base completo
- `V2__create_recetas.sql` — tablas recetas, receta_ingredientes, escalones_macerado
- `V3__lote_improvements.sql` — columnas notas_cata y receta_id en lotes_cerveza
- `V4__historial_lotes.sql` — tabla historial_lotes (sin FK, preserva historia tras borrado)
- `V5__costo_lote_escala_receta.sql` — tabla lote_facturas (N:M), columna volumen_base en recetas
- `V6__proveedores_calendario.sql` — tabla proveedores + proveedor_id FK en facturas_proveedor
- `V7__auditing.sql` — columnas last_modified_at y last_modified_by en lotes_cerveza
- `V8__log_accesos.sql` — tabla log_accesos con índices en fecha, usuario, tipo
- `V9__extend_auditing.sql` — extiende auditoría a recetas, equipos, insumos_inventario, facturas_proveedor, proveedores; agrega created_by a lotes_cerveza
- `V10__backfill_proveedor_ref.sql` — vincula facturas históricas a entidad Proveedor por coincidencia de nombre (UPDATE seguro, solo donde proveedor_id IS NULL)
- `V11__adiciones_hervor.sql` — tabla adiciones_hervor (adiciones de lúpulos/clarificantes durante el hervor en recetas)
- `V12__densidades_enteras.sql` — convierte densidad_inicial/densidad_final (lotes) y og_objetivo/fg_objetivo (recetas) de DECIMAL a INTEGER formato XXXX (ej: 1.056 → 1056)
- `V13__lote_items_factura.sql` — drop `lote_facturas` (nunca expuesta en UI); crea `lote_items_factura(id, lote_id, factura_item_id, cantidad_asignada)` para asignación parcial de ítems de factura a lotes
- `V14__lote_items_cantidad_cero.sql` — relaja constraint a `cantidad_asignada >= 0` (0 = costo total del ítem, sin ingrediente)
- `V15__tenants.sql` — tabla `tenants(subdomain PK, name, tagline, logo_url, colores, active)` para multi-tenancy
- `V16__tenant_id_columns.sql` — agrega `tenant_id VARCHAR(100) NOT NULL DEFAULT 'default'` a las 17 tablas de datos + índices + reemplaza unique constraints simples por compuestas `(campo, tenant_id)`
- `V17__lecturas_fermentacion.sql` — tabla `lecturas_fermentacion(id, lote_id FK CASCADE, fecha, densidad INTEGER formato XXXX, temperatura DECIMAL(5,2), notas, tenant_id)` + índices en `lote_id` y `tenant_id`
- `V18__tenant_email.sql` — `ALTER TABLE tenants ADD COLUMN email_admin VARCHAR(200)` — dirección de email para alertas diarias por tenant
- `V19__planificacion_produccion.sql` — tabla `elaboraciones_planificadas(id, tenant_id, fecha_planeada, receta_id FK nullable, nombre_elaboracion, volumen_estimado, estado VARCHAR(20), notas, creado_at)` + índices en tenant_id y (fecha_planeada, tenant_id)
- `V20__alertas_reintentos.sql` — `ALTER TABLE tenants ADD COLUMN alertas_intentos_fallidos INTEGER NOT NULL DEFAULT 0`, `alertas_ultimo_intento TIMESTAMP`, `alertas_ultimo_exito TIMESTAMP` — tracking de fallos SMTP consecutivos por tenant
- `V21__tenant_fonts.sql` — `ALTER TABLE tenants ADD COLUMN font_headings VARCHAR(100) NOT NULL DEFAULT 'Cinzel'`, `font_body VARCHAR(100) NOT NULL DEFAULT 'Raleway'` — tipografías personalizables por tenant
- `V22__fix_usuarios_unique_constraint.sql` — elimina constraint única simple de `username` en `usuarios` (nombre generado por JPA/Hibernate) y garantiza índice compuesto `ux_usuarios_username_tenant (username, tenant_id)` — corrige lo que V16 intentó hacer pero con nombre de constraint distinto
- `V23__fix_jpa_unique_constraints.sql` — DO block dinámico que elimina constraints únicas simples de columna (nombre generado por JPA) en `tipos_cerveza`, `recetas`, `proveedores`, `lotes_cerveza`; garantiza índices compuestos `ux_*_nombre_tenant` y `ux_lotes_codigo_tenant`
- `V24__historial_tenants.sql` — tabla `historial_tenants(id BIGSERIAL, subdomain VARCHAR(100), accion VARCHAR(50), usuario VARCHAR(100), fecha TIMESTAMP DEFAULT NOW(), detalles VARCHAR(500))` + índices en `subdomain` y `fecha DESC`. Sin FK a `tenants` (preserva historial si se elimina el tenant). Sin `@TenantId` — es auditoría de super-admin, no filtrada por tenant.
- `V25__soft_delete_lotes_recetas.sql` — `ALTER TABLE lotes_cerveza ADD COLUMN deleted_at TIMESTAMP` y `ALTER TABLE recetas ADD COLUMN deleted_at TIMESTAMP` — soft delete: `@SQLRestriction("deleted_at IS NULL")` en ambas entidades. `eliminar()` en los servicios setea `deletedAt = LocalDateTime.now()` y guarda (no borra físicamente).
- `V26__notificaciones.sql` — tabla `notificaciones(id BIGSERIAL, tenant_id VARCHAR(100), tipo VARCHAR(50), titulo VARCHAR(200), mensaje VARCHAR(500), url_accion VARCHAR(300), leida BOOLEAN DEFAULT FALSE, created_at TIMESTAMP DEFAULT NOW())` + índices en `(tenant_id, leida)` y `(tenant_id, created_at DESC)`. Con `@TenantId` — filtrada por tenant. Sin FK externa.
- `V27__estado_factura.sql` — `ALTER TABLE facturas_proveedor ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'RECIBIDA'` — estado de la factura: RECIBIDA (gris), VERIFICADA (amarillo), PAGADA (verde). Todas las facturas existentes quedan en RECIBIDA.
- `V28__factura_historial_estado.sql` — tabla `factura_historial_estado(id BIGSERIAL, tenant_id VARCHAR(100), factura_id BIGINT NOT NULL, estado_anterior VARCHAR(20), estado_nuevo VARCHAR(20) NOT NULL, usuario VARCHAR(100), fecha TIMESTAMP DEFAULT NOW())` + índices en `factura_id` y `tenant_id`. Sin FK a `facturas_proveedor` — preserva historial si se elimina la factura. Con `@TenantId` — filtrada por tenant.
- `V29__superadmin_table.sql` — tabla `super_admins(id BIGSERIAL, username VARCHAR(100) UNIQUE, password VARCHAR(255), activo BOOLEAN DEFAULT TRUE)` — super-administradores globales sin `tenant_id`; tienen acceso a todos los tenants. Sin `@TenantId`.
- `V30__movimientos_inventario_receta_version.sql` — tabla `movimientos_inventario(id, tenant_id, insumo_id, insumo_nombre, tipo, cantidad, cantidad_anterior, cantidad_posterior, motivo, referencia, usuario, fecha)` para auditoría de stock + índices; y `ALTER TABLE recetas ADD COLUMN version INTEGER NOT NULL DEFAULT 1` — versionado de recetas.
- `V31__receta_ph_agua.sql` — `ALTER TABLE recetas ADD COLUMN IF NOT EXISTS ph_agua DECIMAL(4,2)` — pH objetivo del agua de macerado en receta.
- `V32__migracion_log.sql` — tabla `migracion_log(id BIGSERIAL PK, tenant_id VARCHAR(100), modulo VARCHAR(50), archivo VARCHAR(255), procesadas INT, exitosas INT, con_errores INT, estado VARCHAR(20), detalles TEXT, usuario VARCHAR(100), fecha TIMESTAMP DEFAULT NOW())` + índices en `tenant_id` y `fecha DESC`. Sin `@TenantId` — el admin puede consultar historial cross-tenant libremente.
- `V33__factura_iva_incluido.sql` — `ALTER TABLE facturas_proveedor ADD COLUMN IF NOT EXISTS iva_incluido BOOLEAN NOT NULL DEFAULT FALSE` — flag que indica si los precios unitarios de los ítems ya incluyen IVA. Facturas existentes quedan en `false` (modo estándar sin IVA incluido).
- `V34__indices_performance.sql` — índices adicionales de performance sobre columnas de filtrado frecuente.
- `V35__carbonatacion_avanzada.sql` — `ALTER TABLE lotes_cerveza ADD COLUMN IF NOT EXISTS` 10 columnas para carbonatación avanzada: `carb_metodo VARCHAR(20)`, `carb_co2_objetivo DECIMAL(4,2)`, `carb_co2_real DECIMAL(4,2)`, `carb_azucar_tipo VARCHAR(100)`, `carb_azucar_gramos DECIMAL(10,2)`, `carb_presion_psi DECIMAL(6,2)`, `carb_tiempo_horas INTEGER`, `carb_tecnica VARCHAR(50)`, `carb_validacion VARCHAR(50)`, `carb_destino VARCHAR(300)`. Todas nullable. **CRÍTICO**: `carb_co2_objetivo` y `carb_co2_real` usan underscore entre `co2` y el sufijo — la `SpringPhysicalNamingStrategy` NO inserta underscore entre dígito y mayúscula, por lo que los campos Java `carbCo2Objetivo`/`carbCo2Real` requieren `@Column(name="carb_co2_objetivo")`/`@Column(name="carb_co2_real")` explícitos.
- `V37__ventas.sql` — tabla `ventas(id BIGSERIAL PK, tenant_id VARCHAR(100), lote_id FK REFERENCES lotes_cerveza ON DELETE SET NULL, codigo_lote VARCHAR(50), cliente VARCHAR(200) NOT NULL, fecha_despacho DATE NOT NULL, cantidad DECIMAL(10,3) NOT NULL, unidad VARCHAR(50), precio_unitario DECIMAL(12,2) NOT NULL, descuento_pct DECIMAL(5,2) DEFAULT 0, notas VARCHAR(500), estado VARCHAR(20) DEFAULT 'PENDIENTE', created_at, created_by, last_modified_at, last_modified_by)` + CHECK constraints en `estado` (PENDIENTE/DESPACHADO/CANCELADO) y `descuento_pct` (0–100) + índices en tenant_id, fecha, lote_id, estado, LOWER(cliente).
- `V38__venta_historial_estado.sql` — tabla `venta_historial_estado(id BIGSERIAL PK, tenant_id VARCHAR(100), venta_id BIGINT NOT NULL, estado_anterior VARCHAR(20), estado_nuevo VARCHAR(20) NOT NULL, usuario VARCHAR(100), fecha TIMESTAMP NOT NULL DEFAULT NOW())` + índices en `venta_id` y `tenant_id`. Sin FK a `ventas` — preserva historial si se elimina la venta. Con `@TenantId` — filtrada por tenant.
- `V39__ventas_soft_delete.sql` — `ALTER TABLE ventas ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP` — soft delete: `@SQLRestriction("deleted_at IS NULL")` en `Venta`. `VentaService.eliminar()` setea `deletedAt = LocalDateTime.now()` y guarda (no borra físicamente). Índice parcial `idx_ventas_deleted ON ventas (deleted_at) WHERE deleted_at IS NULL`.
- `V40__venta_items.sql` — Convierte ventas al modelo multi-ítem (multi-lote por venta). (1) Crea tabla `venta_items(id BIGSERIAL PK, tenant_id, venta_id BIGINT NOT NULL FK→ventas ON DELETE CASCADE, lote_id FK→lotes_cerveza ON DELETE SET NULL, codigo_lote VARCHAR(50), descripcion VARCHAR(200), cantidad DECIMAL(10,3) NOT NULL CHECK >0, unidad VARCHAR(50), precio_unitario DECIMAL(12,2) NOT NULL, descuento_pct DECIMAL(5,2) DEFAULT 0 BETWEEN 0–100)` + índices en `venta_id`, `tenant_id`, `lote_id` (parcial WHERE NOT NULL). (2) Migra datos existentes: inserta un ítem por cada venta activa, copiando `lote_id`, `codigo_lote`, `cantidad`, `unidad`, `precio_unitario`, `descuento_pct`. (3) Elimina las 5 columnas de la tabla `ventas`: `lote_id`, `codigo_lote`, `cantidad`, `unidad`, `precio_unitario`, `descuento_pct`.
- `V41__clientes.sql` — tabla `clientes(id BIGSERIAL PK, tenant_id VARCHAR(100), nombre VARCHAR(200) NOT NULL, razon_social VARCHAR(200), nit VARCHAR(50), regimen_tributario VARCHAR(30), email VARCHAR(200), telefono VARCHAR(50), direccion_despacho VARCHAR(300), ciudad VARCHAR(100), departamento VARCHAR(100), lista_precio VARCHAR(30), activo BOOLEAN NOT NULL DEFAULT TRUE, notas VARCHAR(500), created_at, created_by, last_modified_at, last_modified_by)` + índices en `tenant_id` y `LOWER(nombre)` + índice único parcial `ux_clientes_nit_tenant ON clientes (tenant_id, nit) WHERE nit IS NOT NULL`. Con `@TenantId` — filtrada por tenant. Extiende `AuditableEntity`.
- `V42__venta_cliente_fk.sql` — `ALTER TABLE ventas ADD COLUMN IF NOT EXISTS cliente_id BIGINT REFERENCES clientes(id) ON DELETE SET NULL`, `ADD COLUMN IF NOT EXISTS remision_numero VARCHAR(20)`, `ADD COLUMN IF NOT EXISTS cotizacion_expira_en DATE`. FK nullable — retrocompatibilidad con ventas existentes sin cliente registrado. Índice parcial `idx_ventas_remision ON ventas (remision_numero) WHERE remision_numero IS NOT NULL`.
- `V43__venta_estado_cotizacion.sql` — Recrea CHECK constraint del campo `estado` en `ventas` para incluir los nuevos estados: `CHECK (estado IN ('COTIZACION','PENDIENTE','DESPACHADO','CANCELADO','EXPIRADO'))`. Se hace via ALTER TABLE DROP/ADD CONSTRAINT.
- `V44__cotizacion_scheduler_index.sql` — Índice parcial de performance `idx_ventas_cotizacion_vencida ON ventas (cotizacion_expira_en) WHERE estado = 'COTIZACION' AND deleted_at IS NULL` — optimiza la query del scheduler `expirarCotizaciones()` que busca cotizaciones vencidas diariamente.
- `V45__tipo_libre.sql` — DDL + DML migration: primero elimina el CHECK constraint `insumos_inventario_tipo_check` de V36 (que solo permitía enum names uppercase y bloqueaba los UPDATEs), luego convierte los valores de `tipo` de nombre de enum (ej: `MALTA`) a nombre de display (ej: `Malta`) en las tablas `insumos_inventario` y `equipos`. Necesario porque `TipoInsumo` y `TipoEquipo` pasaron de `@Enumerated(EnumType.STRING)` a `String` libre con valores display en lugar de enum names.
- `V46__fix_tipo_libre_constraint.sql` — DDL + DML fix: elimina el CHECK constraint `insumos_inventario_tipo_check` creado en V36 que solo permitía valores enum uppercase (MALTA, LUPULO...). Tras V45, `insumos_inventario.tipo` es texto libre; el constraint bloqueaba inserts de display names (Malta, Lúpulo...). Repite los UPDATEs de V45 de forma idempotente para cubrir DBs donde V45 no pudo ejecutarse por el constraint activo. El constraint `factura_items_tipo_insumo_check` NO se modifica — `FacturaItem.tipoInsumo` sigue siendo enum.
- `V47__categorias_tipo.sql` — Crea tablas `tipos_insumo` y `tipos_equipo` (id BIGSERIAL PK, tenant_id VARCHAR(100), nombre VARCHAR(100), activo BOOLEAN DEFAULT TRUE; UNIQUE (tenant_id, nombre)) con índices en tenant_id. Puebla con los 9 valores de insumo (Malta, Lúpulo, Levadura, Clarificante, Agente de Carbonatación, Agua, Químico, Envase, Otro) y 11 valores de equipo (Fermentador, Olla de Macerado, Olla de Hervor, Enfriador, Bomba, Filtro, Medidor de pH, Densímetro, Báscula, Compresor, Otro) para cada tenant existente via CROSS JOIN. Elimina CHECK constraints `factura_items_tipo_insumo_check` y `factura_items_tipo_equipo_check`. Convierte `factura_items.tipo_insumo` y `tipo_equipo` de enum-name a display name ("MALTA" → "Malta", etc.).
- `V49__indices_busqueda_texto.sql` — Índices compuestos `(tenant_id, LOWER(nombre))` para búsquedas de texto filtradas por tenant: `idx_recetas_tenant_nombre` (parcial `WHERE deleted_at IS NULL`), `idx_proveedores_tenant_nombre`, `idx_proveedores_tenant_nit` (parcial `WHERE nit IS NOT NULL`), `idx_insumos_tenant_nombre`. Complementa `idx_insumos_nombre` de V1 (solo LOWER(nombre), sin tenant) con una variante compuesta para queries multi-tenant.
- `V50__plan_limits.sql` — `ALTER TABLE tenants ADD COLUMN IF NOT EXISTS max_lotes INTEGER` y `ADD COLUMN IF NOT EXISTS max_usuarios INTEGER`. Ambas nullable — `NULL` = sin límite (ilimitado). Permite configurar cuotas de plan por tenant: `TrazabilidadService.guardar()` lanza `RuntimeException` si `loteRepo.count() >= maxLotes`; `TenantAdminController.guardarUsuario()` retorna flash danger si `usuariosExistentes.size() >= maxUsuarios`. Los límites se configuran en `/admin/tenants/editar/{subdomain}` y se visualizan con barras de progreso en `/admin/tenants/{subdomain}/metricas`.
- `V51__backfill_lecturas_og_inicial.sql` — DML backfill: inserta la primera `LecturaFermentacion` (OG + temperatura de fermentación) para lotes existentes que tengan `densidad_inicial` pero ninguna lectura registrada. `fecha` = `COALESCE(ferm_fecha_inicial, fecha_elaboracion)`; `densidad` = `densidad_inicial`; `temperatura` = `ferm_temperatura` (nullable). Condición `NOT EXISTS` garantiza idempotencia. Complementa el comportamiento nuevo de `POST /guardar` (desde commit `5bebf06`) que crea este registro automáticamente en lotes nuevos.
- `V52__plan_periodo.sql` — `ALTER TABLE tenants ADD COLUMN IF NOT EXISTS plan_tipo VARCHAR(20)`, `ADD COLUMN IF NOT EXISTS plan_inicio DATE`, `ADD COLUMN IF NOT EXISTS plan_fin DATE`. Permiten configurar un período de vigencia por tenant: `plan_tipo` ∈ {`MENSUAL`, `TRIMESTRAL`, `SEMESTRAL`, `ANUAL`, `BIANUAL`} (null = sin vencimiento). `plan_fin` se calcula automáticamente en `TenantAdminController.calcularPlanFin()` al guardar (1/3/6/12/24 meses desde `plan_inicio`). `Tenant` expone `isPlanVencido()` y `isPlanPorVencer()` (≤7 días). La lista `/admin/tenants` muestra badges de estado (Vencido/Por vencer/fecha vigente). El formulario de creación/edición incluye selector de período + fecha de inicio + display calculado de vencimiento con indicador de estado.
- `V53__evaluaciones_sensoriales.sql` — `CREATE TABLE evaluaciones_sensoriales(id BIGSERIAL PK, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', lote_id BIGINT NOT NULL REFERENCES lotes_cerveza(id) ON DELETE CASCADE, fecha DATE NOT NULL, catador VARCHAR(100), aroma INTEGER CHECK (0–12), apariencia INTEGER CHECK (0–3), sabor INTEGER CHECK (0–20), sensacion_boca INTEGER CHECK (0–5), impresion_general INTEGER CHECK (0–10), notas VARCHAR(1000), creado_at TIMESTAMP NOT NULL DEFAULT NOW())` + índices en `lote_id` y `tenant_id`. Con `@TenantId`. FK con CASCADE DELETE — si se elimina (soft-delete) el lote, las evaluaciones físicamente asociadas se borran en cascada.
- `V54__barriles.sql` — Crea tabla `barriles(id BIGSERIAL PK, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', codigo VARCHAR(50) NOT NULL, tipo VARCHAR(50), capacidad_litros DECIMAL(8,2), estado VARCHAR(20) NOT NULL DEFAULT 'DISPONIBLE' CHECK IN (DISPONIBLE/LLENO/DESPACHADO/VACIO/LIMPIEZA/BAJA), lote_id BIGINT FK→lotes_cerveza ON DELETE SET NULL, codigo_lote VARCHAR(50), cliente_nombre VARCHAR(200), fecha_despacho DATE, observaciones VARCHAR(500), audit columns)` + tabla `movimientos_barriles(id BIGSERIAL PK, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', barril_id BIGINT NOT NULL — sin FK para preservar historial tras eliminación, estado_anterior VARCHAR(20), estado_nuevo VARCHAR(20) NOT NULL, usuario VARCHAR(100), notas VARCHAR(500), fecha TIMESTAMP NOT NULL DEFAULT NOW())`. Índices: `ux_barriles_codigo_tenant` (UNIQUE), `idx_barriles_tenant`, `idx_barriles_estado`, `idx_barriles_lote` (parcial WHERE NOT NULL), `idx_mov_barriles_barril`, `idx_mov_barriles_tenant`.
- `V55__ordenes_compra.sql` — Crea tabla `ordenes_compra(id BIGSERIAL PK, tenant_id VARCHAR(100) NOT NULL DEFAULT 'default', numero_oc VARCHAR(20), proveedor VARCHAR(200), proveedor_id BIGINT FK→proveedores ON DELETE SET NULL, fecha_emision DATE NOT NULL, fecha_requerida DATE, estado VARCHAR(30) NOT NULL DEFAULT 'BORRADOR' CHECK IN (BORRADOR/ENVIADA/RECIBIDA_PARCIAL/RECIBIDA/CANCELADA), notas VARCHAR(500), factura_id BIGINT — sin FK, audit columns)` + tabla `orden_compra_items(id BIGSERIAL PK, tenant_id VARCHAR(100), orden_id BIGINT NOT NULL FK→ordenes_compra ON DELETE CASCADE, tipo_item VARCHAR(20), nombre VARCHAR(200) NOT NULL, descripcion VARCHAR(300), cantidad DECIMAL(10,3) NOT NULL CHECK >0, unidad VARCHAR(50), precio_unitario_estimado DECIMAL(12,2), porcentaje_iva_item DECIMAL(5,2) DEFAULT 0, tipo_insumo VARCHAR(100), tipo_equipo VARCHAR(100))`. Índices: `idx_oc_tenant`, `idx_oc_estado`, `idx_oc_numero`, `idx_oc_items_orden`, `idx_oc_items_tenant`.
- `V56__factura_item_impuesto_consumo.sql` — `ALTER TABLE factura_items ADD COLUMN IF NOT EXISTS impuesto_consumo DECIMAL(15,2) NOT NULL DEFAULT 0` — valor fijo de impuesto al consumo por ítem (no porcentaje). Se suma directamente al total de línea. Facturas existentes quedan en 0.

---

## ENTIDADES Y MODELOS

### AuditableEntity (@MappedSuperclass)
Base de auditoría JPA compartida por 6 entidades. Contiene los 4 campos auditados + tenant:
- `@TenantId tenantId` — discriminador multi-tenant; Hibernate lo setea automáticamente en INSERT y filtra en SELECT. Heredado por todas las subclases.
- `@CreatedDate createdAt` — inmutable, seteado al persistir por primera vez
- `@CreatedBy createdBy` — inmutable, usuario de Spring Security al crear
- `@LastModifiedDate lastModifiedAt` — actualizado en cada `save()`
- `@LastModifiedBy lastModifiedBy` — usuario de Spring Security en cada `save()`
- Anotada con `@EntityListeners(AuditingEntityListener.class)` — NO repetir en subclases
- **CRÍTICO**: al extender, NO declarar `createdAt`, `@PrePersist`, `tenantId` ni getters de auditoría — vienen del padre
- **CRÍTICO**: NO redefinir `getCreatedAt()` / `setCreatedAt()` en subclases — causa `private access` en compilación
- Entidades que extienden: `LoteCerveza`, `Receta`, `Equipo`, `InsumoInventario`, `FacturaProveedor`, `Proveedor`

### ElaboracionPlanificada
Entidad para planificación de producción. Tabla `elaboraciones_planificadas`. Tiene `@TenantId`. **No extiende `AuditableEntity`** — usa `@PrePersist` para `creadoAt`.
- `id`, `tenantId` (@TenantId), `fechaPlaneada` (DATE, NOT NULL)
- `@ManyToOne receta → Receta` (LAZY, nullable) — receta base opcional
- `nombreElaboracion` (VARCHAR 150, NOT NULL) — si no se ingresa y hay receta, el servicio usa el nombre de la receta como fallback
- `volumenEstimado` (DECIMAL 10,2, nullable) — litros esperados
- `@Enumerated(EnumType.STRING) estado → EstadoPlanificacion` — PLANIFICADA / EN_PROCESO / COMPLETADA / CANCELADA. Default: PLANIFICADA.
- `notas` (VARCHAR 500, nullable), `creadoAt` (TIMESTAMP, NOT NULL, immutable via `@PrePersist`)
- **EstadoPlanificacion** (`com.alera.model.enums`): cada valor tiene `getColor()` (hex para FullCalendar), `getColorTexto()` y `getDisplayName()`. Colores: dorado/azul/verde/gris.

### LecturaFermentacion
Entidad para el registro periódico de densidad y temperatura durante la fermentación. Tabla `lecturas_fermentacion`. Tiene `@TenantId`. **No extiende `AuditableEntity`.**
- `id`, `tenantId` (@TenantId), `@ManyToOne lote → LoteCerveza` (LAZY, FK con ON DELETE CASCADE)
- `fecha` (DATE, NOT NULL) — fecha de la medición
- `densidad` (INTEGER, nullable) — formato XXXX (ej: 1042). Puede ser null si solo se registra temperatura.
- `temperatura` (DECIMAL 5,2, nullable) — en °C. Puede ser null si solo se registra densidad.
- `notas` (VARCHAR 500, nullable)
- `getAbvParcial(Integer ogLote)` — ABV de progreso: `(ogLote - densidad) * 131.25 / 1000.0`. Retorna null si densidad es null, ogLote es null, o densidad >= ogLote.
- Ordenadas en queries por `fecha ASC, id ASC` (no por `createdAt` — no tiene auditoría).

### LoteCerveza
Entidad central. Extiende `AuditableEntity`. Campos propios:
- `id`, `codigoLote` (unique por tenant, ej: "IPA-001"), `estilo`, `fechaElaboracion`
- `aguaUtilizada`, `phAgua`, `litrosFinales`, `clarificante`
- `densidadInicial` (`Integer`) — formato XXXX, ej: 1056. **NO usar BigDecimal.**
- `densidadFinal` (`Integer`) — formato XXXX, ej: 1015. **NO usar BigDecimal.**
- `densidadFinalFecha`
- `notasCata` (TEXT) — evaluación sensorial
- `@ManyToOne receta → Receta` (LAZY) — receta de origen
- `@ManyToOne equipoFermentador → Equipo` (LAZY)
- `@OneToMany ingredientes → Ingrediente` (CASCADE ALL, orphanRemoval) — inicializado como `new ArrayList<>()`
- `@OneToMany itemsFactura → LoteItemFactura` (CASCADE ALL, orphanRemoval) — ítems de factura asignados con cantidad parcial
- 4 fases: `ferm*` | `acond*` | `madur*` | `carb*`
- **Carbonatación avanzada** (10 campos, todos nullable — V35):
  - `carbMetodo` (String) — `"NATURAL"` (priming con azúcar) / `"FORZADA"` (inyección CO₂)
  - `carbCo2Objetivo` (BigDecimal) — `@Column(name="carb_co2_objetivo")` — volúmenes CO₂ objetivo (ej: 2.5)
  - `carbCo2Real` (BigDecimal) — `@Column(name="carb_co2_real")` — medición real post-carbonatación
  - `carbAzucarTipo` (String) — tipo de azúcar para priming: `"dextrosa"`, `"sacarosa"`, `"extracto"`, `"miel"`
  - `carbAzucarGramos` (BigDecimal) — gramos de azúcar calculados por la calculadora de priming
  - `carbPresionPsi` (BigDecimal) — presión en PSI para método forzado
  - `carbTiempoHoras` (Integer) — horas de carbonatación forzada
  - `carbTecnica` (String) — técnica forzada: `"PIEDRA"` / `"PRESION_FIJA"`
  - `carbValidacion` (String) — resultado organoléptico: `"ADECUADA"`, `"RETENCION_CORRECTA"`, `"SOBRECARBONATADA"`, `"BAJA_CARBONATACION"`
  - `carbDestino` (String) — destino/empaque final del lote. Soporta **múltiples formatos** separados por `" | "` (espacio-pipe-espacio): ej. `"48 × Botella 330ml | 2 × Barril 20L"`. Cada entrada sigue el patrón `"N × Formato"` (parseado por `DESTINO_PATTERN` en `VentaService`). Entradas sin cantidad (ej. `"A granel"`) también son válidas. Retrocompatible con valores anteriores de una sola entrada.
  - **Calculadora de priming**: `gramos = (co2Objetivo - co2Residual) × litros × 4 × factorAzucar`. CO₂ residual ≈ `0.5 + (temp × 0.065)`. Factores: dextrosa=1.0, sacarosa=0.91, extracto=1.40, miel=1.25
- `observaciones`
- Métodos: `getMaltas()`, `getLupulos()`, `getLevaduras()`, `getClarificantes()`, `getFaseActual()`, `isCompletado()`
- **Cálculos de calidad** (adaptados al formato Integer XXXX):
  - `getAbv()` → `(OG - FG) * 131.25 / 1000.0` → BigDecimal con scale 2
  - `getAtenuacionAparente()` → `(OG - FG) * 100.0 / (OG - 1000)` → BigDecimal con scale 1
  - `getEficienciaMacerado()` → `ogPuntos = OG - 1000` (ya en puntos, NO multiplicar por 1000)
- **Costo**: `getCostoTotal()` — suma `LoteItemFactura.getValorAsignado()` de cada ítem asignado; `getCostoPorLitro()` divide por litrosFinales
- **Kanban**: `getDiasEnFaseActual()` — días desde el inicio de la fase actual
- **Soft delete**: `@SQLRestriction("deleted_at IS NULL")` — Hibernate filtra automáticamente lotes eliminados. Campo `deletedAt` (`LocalDateTime`, nullable). `TrazabilidadService.eliminar()` setea `deletedAt` y guarda (no borra físicamente). El historial registra "ARCHIVADO" (no "ELIMINADO").

### Tenant
Entidad de configuración por cliente. Tabla `tenants`. **Sin `@TenantId`** (es la tabla maestra, no filtrada).
- `subdomain` (VARCHAR 100, PK) — ej: "cerveceria1", "default"
- `name`, `tagline`, `logoUrl` — identidad del cliente. `logoUrl` acepta URL externa (`https://...`) o ruta relativa local (`/img/logo.png`). Imágenes locales van en `src/main/resources/static/img/`.
- `colorNavbar`, `colorPrimary`, `colorAccent`, `colorAccentHover`, `colorCream`, `colorBodyBg` — paleta personalizada
- `fontHeadings` (VARCHAR 100, default `'Cinzel'`) — fuente de títulos y navbar. Opciones disponibles: Cinzel, Playfair Display, Cormorant Garamond, EB Garamond, Oswald, Montserrat, Inter, Roboto, Bowlby One SC.
- `fontBody` (VARCHAR 100, default `'Raleway'`) — fuente de cuerpo. Opciones: Raleway, Inter, Roboto, Open Sans, Poppins, Nunito, DM Sans.
- `emailAdmin` (VARCHAR 200, nullable) — destinatario de alertas diarias. Si es null o vacío, el tenant no recibe emails.
- `active` (boolean) — tenants inactivos retornan 503
- `alertasIntentosFallidos` (INTEGER, NOT NULL, default 0) — contador de fallos SMTP consecutivos. Se incrementa en cada fallo, se resetea a 0 al enviar exitosamente. Visible en `/admin/tenants` como badge amarillo.
- `alertasUltimoIntento` (TIMESTAMP, nullable) — fecha/hora del último intento de envío (exitoso o fallido).
- `alertasUltimoExito` (TIMESTAMP, nullable) — fecha/hora del último envío exitoso.
- `maxLotes` (INTEGER, nullable) — límite de lotes por plan. `NULL` = sin límite. `TrazabilidadService.guardar()` lanza `RuntimeException` al alcanzarlo.
- `maxUsuarios` (INTEGER, nullable) — límite de usuarios por plan. `NULL` = sin límite. `TenantAdminController.guardarUsuario()` bloquea la creación con flash danger al alcanzarlo.
- `planTipo` (VARCHAR 20, nullable) — período de vigencia del plan: `"MENSUAL"` (1 mes), `"TRIMESTRAL"` (3 meses), `"SEMESTRAL"` (6 meses), `"ANUAL"` (12 meses), `"BIANUAL"` (24 meses). `null` = sin vencimiento (ilimitado).
- `planInicio` (DATE, nullable) — fecha de inicio del período activo. Se setea al guardar el tenant cuando `planTipo != null`.
- `planFin` (DATE, nullable) — fecha de vencimiento calculada automáticamente por `TenantAdminController.calcularPlanFin()` como `planInicio + meses(planTipo)`. Se limpia cuando `planTipo` es null.
- Helpers: `isPlanVencido()` → `planFin < hoy`; `isPlanPorVencer()` → `planFin` entre hoy y hoy+7 días; `getPlanFinTexto()` → `"Vencido"` / `"Por vencer"` / null. Usados por los badges de la lista `/admin/tenants`.
- Creado por `DataInitializer` al arrancar. Al inicio, itera **todos los tenants** existentes en BD y crea usuarios/tipos de cerveza/categorías de insumo y equipo para los que no tengan ninguno. Si un tenant ya tiene usuarios, no se modifica.
- `GlobalControllerAdvice` lo expone como `${branding}` — los templates usan `${branding.name}`, `${branding.colorAccent}`, `${branding.fontHeadings}`, `${branding.fontBody}`, etc. sin cambios

### LoteItemFactura
Asignación parcial de ítems de factura a lotes. Tabla `lote_items_factura`. Tiene `@TenantId`.
- `id`, `@ManyToOne lote → LoteCerveza` (LAZY), `@ManyToOne item → FacturaItem` (LAZY)
- `cantidadAsignada` (DECIMAL 10,3) — cantidad del ítem asignada a este lote (0 = costo total, sin ingrediente)
- `getValorAsignado()` → proporcional: `(cantidadAsignada / item.cantidad) × item.valorLinea`. Si `cantidadAsignada = 0` devuelve `item.valorLinea` completo (costo sin ingrediente, ej: envase, flete)
- UNIQUE `(lote_id, factura_item_id)` — un ítem no puede asignarse dos veces al mismo lote

### Receta
Extiende `AuditableEntity`. Campos propios:
- `id`, `nombre` (unique), `estilo`, `descripcion`, `activa` (boolean, default true)
- `aguaMacerado`, `unidadAguaMacerado`, `aguaSparge`, `unidadAguaSparge`
- `tiempoHervorMinutos`
- `ogObjetivo` (`Integer`) — formato XXXX, ej: 1060. **NO usar BigDecimal.**
- `fgObjetivo` (`Integer`) — formato XXXX, ej: 1014. **NO usar BigDecimal.**
- `volumenBase`, `phAgua` (DECIMAL 4,2, nullable — pH objetivo del agua; rango típico 5.0–5.5), `notas`
- `@OneToMany ingredientes → RecetaIngrediente` + `@OneToMany escalones → EscalonMacerado`
- `@OneToMany adicionesHervor → AdicionHervor` (CASCADE ALL, orphanRemoval) — ordenadas por `minutosRestantes DESC, orden ASC`
- **CRÍTICO**: el campo se llama `activa` (no `activo`) — los métodos derivados de Spring Data son `findAllByActivaTrue*`, `findByActiva*`
- **Soft delete**: `@SQLRestriction("deleted_at IS NULL")` — campo `deletedAt` (`LocalDateTime`, nullable). `RecetaService.eliminar()` setea `deletedAt` y guarda (no borra físicamente).

### EscalonMacerado
- `@Column(name="temperatura_c")` y `@Column(name="duracion_minutos")` — **obligatorios** por naming strategy

### AdicionHervor
Nueva entidad. Tabla `adiciones_hervor`. Representa una adición de lúpulo o clarificante durante el hervor:
- `id`, `@ManyToOne receta` (LAZY, NOT NULL)
- `nombre` (VARCHAR 150) — nombre del insumo a agregar
- `minutosRestantes` (INTEGER) — tiempo restante del hervor al momento de la adición (0 = flameout/apagado)
- `cantidad` (DECIMAL 10,3), `unidad` (VARCHAR 20)
- `orden` (INTEGER, default 0) — para desempate en ordenamiento
- Ordenadas en Receta por `minutosRestantes DESC` (adiciones más tempranas primero)

### Notificacion
Notificaciones in-app persistentes por tenant. Tabla `notificaciones`. Tiene `@TenantId`. **No extiende `AuditableEntity`.**
- `id`, `tenantId` (@TenantId), `@Enumerated(EnumType.STRING) tipo → TipoNotificacion`, `titulo` (VARCHAR 200, NOT NULL)
- `mensaje` (VARCHAR 500, nullable), `urlAccion` (VARCHAR 300, nullable)
- `leida` (boolean, default false), `createdAt` (TIMESTAMP, NOT NULL, seteado por `@PrePersist`)
- Factory: `Notificacion.of(tipo, titulo, mensaje, urlAccion)` — crea instancia sin id ni tenantId
- **TipoNotificacion** (`com.alera.model.enums`): `BAJO_STOCK("bi-box-seam", "text-warning")`, `VENCIMIENTO("bi-calendar-x", "text-warning")`, `MANTENIMIENTO("bi-tools", "text-info")`, `SISTEMA("bi-info-circle-fill", "text-primary")`. Cada valor tiene `getIcono()` y `getColorClase()` para uso directo en templates/JS.

### HistorialLote
- `id`, `tenantId` (@TenantId), `loteId` (sin FK), `codigoLote`, `accion` (CREADO/EDITADO/ELIMINADO), `usuario`, `fecha`, `notas`

### HistorialTenant
Auditoría de cambios de configuración de tenants. Tabla `historial_tenants`. **Sin `@TenantId`** — datos de super-admin, no filtrados por tenant.
- `id`, `subdomain` (sin FK — preserva historial si se elimina el tenant), `accion`, `usuario`, `fecha`, `detalles`
- Factory: `HistorialTenant.of(subdomain, accion, usuario, detalles)`
- Acciones registradas: `CREADO`, `EDITADO`, `ACTIVADO`, `DESACTIVADO`, `USUARIO_CREADO`, `USUARIO_ELIMINADO`, `CONFIG_IMPORTADA`
- Consultado via `TenantService.listarHistorial(subdomain)` → `findBySubdomainOrderByFechaDesc`
- Registrado via `TenantService.registrarAccion(subdomain, accion, detalles)` — obtiene usuario de `SecurityContextHolder`

### LogAcceso
- `id`, `tenantId` (@TenantId), `usuario`, `tipo` (LOGIN_OK/LOGIN_FALLIDO/ACCESO_DENEGADO), `ip`, `url`, `userAgent`, `fecha`, `detalles`
- Factory: `LogAcceso.of(usuario, tipo, ip, url, userAgent, detalles)`
- IP extraída de `X-Forwarded-For` (proxy) o `RemoteAddr`

### Proveedor
Extiende `AuditableEntity`. Campos propios:
- `id`, `nombre` (unique), `nit`, `telefono`, `email`, `direccion`, `activo`

### Equipo / InsumoInventario / FacturaProveedor
Todos extienden `AuditableEntity` — los 4 campos de auditoría vienen del padre.
- **Campo `tipo` en `Equipo`**: `String` (no enum). Valores cargados desde `CategoriaEquipo` (BD, por tenant). El formulario HTML usa `<select>` poblado desde `categoriaEquipoService.listarNombresActivos()`. `EquipoController` ya no tiene `TIPOS_EQUIPO` estático.
- **Campo `tipo` en `InsumoInventario`**: `String` (no enum). Valores cargados desde `CategoriaInsumo` (BD, por tenant). `InsumoInventarioController` ya no tiene `TIPOS_INSUMO` estático. `InsumoInventarioService.detectarTipo()` retorna los mismos strings display. `getColorTipo()` en la entidad usa switch sobre el String. **En Thymeleaf y en templates de email**: usar el `String` directamente — `th:if="${ins.tipo == 'Malta'}"` o `th:text="${ins.tipo}"`. **NUNCA** llamar `.name()` ni `.getDisplayName()` sobre `ins.tipo` o `eq.tipo` — son `String`, no enums, y lanzan `SpelEvaluationException`. Afecta a todos los templates que iteren `InsumoInventario` o `Equipo`: `recetas/formulario.html`, `emails/alertas.html`, y cualquier template futuro. Excepción: `n.tipo` en `Notificacion` SÍ es enum (`TipoNotificacion`) — `.name()` es válido ahí.
- **`FacturaItem.tipoInsumo` y `FacturaItem.tipoEquipo`**: `String` (antes enums `TipoInsumo`/`TipoEquipo`). Almacenan el display name directamente ("Malta", "Fermentador"). V47 convirtió los valores históricos de enum-name a display name. `FacturaProveedorController.agregarDatosFormulario()` construye `insumosPorTipo` y `equiposPorTipo` con el display name como clave (no el enum name) — coincide con el valor que envía el select del formulario JS.
- **Filtro en repositorios**: `InsumoInventarioRepository.findByFiltros()` recibe `String tipo` (no enum). `EquipoRepository.findFermentadoresDisponibles()` recibe `String tipo` — llamado con `"Fermentador"` en `EquipoService`.
- `FacturaProveedor`: `proveedor` (String original) + `@ManyToOne proveedorRef → Proveedor` (LAZY, nullable) — coexisten para compat. histórica. V10 backfill vincula automáticamente donde los nombres coincidan.
- **Campo `estado` en `FacturaProveedor`**: `@Enumerated(EnumType.STRING) EstadoFactura estado` — default `RECIBIDA`. Valores: `RECIBIDA` (badge gris), `VERIFICADA` (badge amarillo), `PAGADA` (badge verde). Cada valor tiene `getDisplayName()` y `getBadgeClass()` (clase Bootstrap). Se puede cambiar desde el detalle via `POST /facturas/{id}/estado` o desde el formulario de edición via select.
- **Campo `ivaIncluido` en `FacturaProveedor`**: `boolean ivaIncluido = false`. Cuando `true`, el `valorUnitario` ingresado en cada ítem ya incluye el IVA (precio bruto). `FacturaItem.getValorUnitarioSinIva()` extrae la base dividiendo por `(1 + iva%/100)`; cuando `false` devuelve `valorUnitario` directamente. `calcularTotales()` en el servicio delega a los métodos computados del ítem, que acceden a `factura.isIvaIncluido()` via la referencia `@ManyToOne factura` (ya seteada antes de llamar al método). Visible en el formulario como toggle switch; en el detalle muestra badge y columna adicional "V. sin IVA".
- **Campo `impuestoConsumo` en `FacturaItem`**: `@Column(name="impuesto_consumo") BigDecimal impuestoConsumo = BigDecimal.ZERO` — valor fijo en pesos (no porcentaje) que se suma directamente al total de la línea. `calcularValorLinea()` retorna `getValorBase() + getValorIvaItem() + impuestoConsumo`. `calcularTotales()` en el servicio acumula `totalImpConsumo` y lo incluye en `valorTotal`. El Resumen Financiero del detalle muestra la línea "Imp. Consumo" solo cuando el total es distinto de cero (derivado como `valorTotal − subtotal − valorIva − costoEnvio`). `@Column(name="impuesto_consumo")` explícito — `SpringPhysicalNamingStrategy` no inserta underscore entre "impuesto" y "consumo" correctamente sin él.
- **Campo de fecha en `FacturaProveedor`**: `fechaFactura` (`LocalDate`) — **NO** `fecha`. En JPQL usar `f.fechaFactura`; en Java `getFechaFactura()`. Error frecuente: escribir `f.fecha` en un `@Query` → `UnknownPathException` al arrancar.

### FacturaHistorialEstado
Auditoría de cambios de estado de facturas. Tabla `factura_historial_estado`. Tiene `@TenantId`. **No extiende `AuditableEntity`.**
- `id`, `tenantId` (@TenantId), `facturaId` (BIGINT, sin FK — preserva historial si se elimina la factura)
- `@Enumerated(EnumType.STRING) estadoAnterior → EstadoFactura` — nullable (null = creación inicial de la factura)
- `@Enumerated(EnumType.STRING) estadoNuevo → EstadoFactura` (NOT NULL)
- `usuario` (VARCHAR 100) — nombre del usuario autenticado al momento del cambio (via `SecurityContextHolder`); `"sistema"` si no hay sesión
- `fecha` (TIMESTAMP, NOT NULL, seteado por `@PrePersist`)
- Factory: `FacturaHistorialEstado.of(facturaId, estadoAnterior, estadoNuevo, usuario)` — crea instancia sin id ni tenantId
- Se crea en `FacturaProveedorService.guardar()` (estado inicial, `estadoAnterior=null`) y en `cambiarEstado()` (transición, con estado anterior)

### MigracionLog
Registro de importaciones de datos por tenant. Tabla `migracion_log`. **Sin `@TenantId`** — el admin puede consultar historial de cualquier tenant libremente.
- `id`, `tenantId` (VARCHAR 100, NOT NULL — columna regular, no discriminador), `modulo` (VARCHAR 50 — almacen/equipos/comercial/produccion)
- `archivo` (VARCHAR 255) — nombre original del archivo subido
- `procesadas`, `exitosas`, `conErrores` (int) — contadores de filas
- `estado` (VARCHAR 20) — `"EXITOSO"` / `"PARCIAL"` / `"FALLIDO"`
- `detalles` (TEXT, nullable) — mensajes de error de filas fallidas, separados por `\n`
- `usuario` (VARCHAR 100) — nombre del usuario autenticado que realizó la importación
- `fecha` (TIMESTAMP, NOT NULL, seteado por `@PrePersist`)
- Factory: `MigracionLog.of(tenantId, modulo, archivo, procesadas, exitosas, conErrores, estado, detalles, usuario)`
- Solo getters (sin setters) — inmutable tras creación

### Cliente
Datos fiscales y de contacto del cliente. Tabla `clientes`. Tiene `@TenantId`. **Extiende `AuditableEntity`** (auditoría JPA automática).
- `id`, `nombre` (VARCHAR 200, NOT NULL), `razonSocial` (VARCHAR 200, nullable)
- `nit` (VARCHAR 50, nullable) — unique por tenant (índice parcial `WHERE nit IS NOT NULL`)
- `@Enumerated(EnumType.STRING) regimenTributario → RegimenTributario` — nullable (SIMPLIFICADO, RESPONSABLE_IVA). Cada valor tiene `getDisplayName()`.
- `email` (VARCHAR 200, nullable), `telefono` (VARCHAR 50, nullable)
- `direccionDespacho` (VARCHAR 300, nullable), `ciudad` (VARCHAR 100, nullable), `departamento` (VARCHAR 100, nullable)
- `@Enumerated(EnumType.STRING) listaPrecio → ListaPrecio` — nullable. Valores: VENTA_DIRECTA, DISTRIBUIDOR, BAR, MAYORISTA, EXPORTACION, EMPLEADO. Cada valor tiene `getDisplayName()`.
- `activo` (boolean, default true), `notas` (VARCHAR 500, nullable)
- Los 4 campos de auditoría vienen de `AuditableEntity` (`createdAt`, `createdBy`, `lastModifiedAt`, `lastModifiedBy`)
- **ListaPrecio** (`com.alera.model.enums`): `VENTA_DIRECTA("Venta directa")`, `DISTRIBUIDOR("Distribuidor")`, `BAR("Bar / Restaurante")`, `MAYORISTA("Mayorista")`, `EXPORTACION("Exportación")`, `EMPLEADO("Empleado")`.
- **RegimenTributario** (`com.alera.model.enums`): `SIMPLIFICADO("Régimen simplificado")`, `RESPONSABLE_IVA("Responsable de IVA")`.

### Venta
Registro de ventas/despachos a clientes. Puede incluir múltiples lotes/ítems. Tabla `ventas`. Tiene `@TenantId`. **No extiende `AuditableEntity`** — gestiona su propia auditoría con `@PrePersist`/`@PreUpdate`.
- `id`, `tenantId` (@TenantId), `cliente` (VARCHAR 200, NOT NULL — desnormalizado de `clienteRef.nombre`), `fechaDespacho` (DATE, NOT NULL)
- `@ManyToOne(LAZY) Cliente clienteRef` — `@JoinColumn(name="cliente_id")`, nullable (retrocompatibilidad con ventas sin cliente registrado). `open-in-view=true` permite acceder a `clienteRef` en templates sin LazyInitializationException.
- `remisionNumero` (VARCHAR 20, nullable) — generado automáticamente al cambiar estado a DESPACHADO. Formato `REM-NNN` (ej: `REM-001`). Thread-safe: `generarRemisionNumero()` hace `em.flush()` antes de `MAX(CAST(SUBSTRING(remision_numero FROM 5) AS INTEGER))` nativa.
- `cotizacionExpiraEn` (DATE, nullable) — solo relevante cuando `estado = COTIZACION`. Si no se especifica al crear la cotización, se calcula como `LocalDate.now().plusDays(expiracionDias)`.
- `notas` (VARCHAR 500, nullable)
- `@Enumerated(EnumType.STRING) estado → EstadoVenta` — default PENDIENTE
- `@OneToMany(mappedBy="venta", cascade=ALL, orphanRemoval=true, fetch=LAZY) items → List<VentaItem>` — inicializado como `new ArrayList<>()`
- `@Formula valorTotal` — subquery SQL `COALESCE(SUM(vi.cantidad * vi.precio_unitario * (1 - vi.descuento_pct/100.0)), 0)` sobre `venta_items WHERE vi.venta_id = id`. Populado en cada SELECT — evita N+1 al listar ventas. `getValorTotal()` retorna ZERO si null.
- `@Formula primerCodigoLote` — subquery SQL `SELECT vi.codigo_lote FROM venta_items vi WHERE vi.venta_id = id AND vi.codigo_lote IS NOT NULL ORDER BY vi.id LIMIT 1`. Permite mostrar el primer lote en la lista sin cargar la colección lazy. `getPrimerCodigoLote()` puede retornar null.
- `createdAt`, `createdBy`, `lastModifiedAt`, `lastModifiedBy` — auditoría propia
- `deletedAt` (TIMESTAMP, nullable) — soft delete: `@SQLRestriction("deleted_at IS NULL")`. `VentaService.eliminar()` setea `deletedAt` y guarda (no borra físicamente).
- **CRÍTICO — N+1 en lista**: los campos `@Formula` se calculan en SQL inline en cada SELECT de `Venta`. No iterar `items` en métodos usados por la lista; usar siempre `getValorTotal()` y `getPrimerCodigoLote()`.
- **EstadoVenta** (`com.alera.model.enums`): `COTIZACION("Cotización", "bg-info text-dark")`, `PENDIENTE("Pendiente", "bg-warning text-dark")`, `DESPACHADO("Despachado", "bg-success")`, `CANCELADO("Cancelado", "bg-secondary")`, `EXPIRADO("Expirado", "bg-dark")`. Cada valor tiene `getDisplayName()` y `getBadgeClass()`.
- **Transiciones de estado válidas** (mapa `TRANSICIONES_VALIDAS` en `VentaService`): COTIZACION → {PENDIENTE, CANCELADO}; PENDIENTE → {DESPACHADO, CANCELADO}; DESPACHADO → {}; CANCELADO → {}; EXPIRADO → {}. Transición inválida lanza `RuntimeException` en `cambiarEstado()`.
- **Descuento automático de envases** (al despachar): `descontarEnvases()` busca ítems de la venta cuya `unidad` sea un tipo de empaque reconocido (Botella, Lata, Barril, Growler, etc.) via `VentaItemRepository.findItemsConEnvase()`, y descuenta del inventario de insumos usando `InsumoInventarioService.descontarIngrediente()`. Si hay stock insuficiente, solo registra WARN (no bloquea el despacho).

### VentaItem
Línea de ítem dentro de una venta (lote + cantidad + precio). Tabla `venta_items`. Tiene `@TenantId`. **No extiende `AuditableEntity`.**
- `id`, `tenantId` (@TenantId)
- `@ManyToOne venta → Venta` (LAZY, NOT NULL, FK ON DELETE CASCADE)
- `@ManyToOne lote → LoteCerveza` (LAZY, nullable — ON DELETE SET NULL)
- `codigoLote` (VARCHAR 50) — desnormalizado; se copia de `lote.codigoLote` al guardar para preservar referencia histórica si el lote se elimina
- `descripcion` (VARCHAR 200, nullable) — texto libre opcional
- `cantidad` (DECIMAL 10,3, NOT NULL, CHECK > 0), `unidad` (VARCHAR 50, nullable)
- `precioUnitario` (DECIMAL 12,2, NOT NULL), `descuentoPct` (DECIMAL 5,2, default ZERO, BETWEEN 0–100)
- `getValorLinea()` — `cantidad × precioUnitario × (1 - descuentoPct/100)`, escala 2. Retorna ZERO si campos null.

### VentaHistorialEstado
Auditoría de cambios de estado de ventas. Tabla `venta_historial_estado`. Tiene `@TenantId`. **No extiende `AuditableEntity`.**
- `id`, `tenantId` (@TenantId), `ventaId` (BIGINT, sin FK — preserva historial si se elimina la venta)
- `@Enumerated(EnumType.STRING) estadoAnterior → EstadoVenta` — nullable (null = creación inicial)
- `@Enumerated(EnumType.STRING) estadoNuevo → EstadoVenta` (NOT NULL)
- `usuario` (VARCHAR 100) — nombre del usuario autenticado; `"sistema"` si no hay sesión
- `fecha` (TIMESTAMP, NOT NULL, seteado por `@PrePersist`)
- Factory: `VentaHistorialEstado.of(ventaId, estadoAnterior, estadoNuevo, usuario)`
- Se crea en `VentaService.guardar()` (estado inicial, `estadoAnterior=null`), en `actualizar()` (solo si el estado cambió), y en `cambiarEstado()`

### EvaluacionSensorial
Registro de cata estructurada estilo BJCP por lote. Tabla `evaluaciones_sensoriales`. Tiene `@TenantId`. **No extiende `AuditableEntity`.**
- `id`, `tenantId` (@TenantId), `@ManyToOne(LAZY) lote → LoteCerveza` (`@JoinColumn(name="lote_id", nullable=false)`, FK ON DELETE CASCADE)
- `fecha` (DATE, NOT NULL) — fecha de la cata
- `catador` (VARCHAR 100, nullable) — nombre del evaluador; `null` si está en blanco
- `aroma` (INTEGER, nullable) — puntuación 0–12
- `apariencia` (INTEGER, nullable) — puntuación 0–3
- `sabor` (INTEGER, nullable) — puntuación 0–20
- `sensacionBoca` (INTEGER, nullable) — puntuación 0–5 → columna `sensacion_boca` (naming strategy automático)
- `impresionGeneral` (INTEGER, nullable) — puntuación 0–10 → columna `impresion_general` (naming strategy automático)
- `notas` (VARCHAR 1000, nullable) — observaciones libres; `null` si está en blanco
- `creadoAt` (TIMESTAMP, NOT NULL, seteado por `@PrePersist`)
- **Puntaje total máximo: 50 puntos** (distribución BJCP: sabor 40%, aroma 24%, impresión 20%, boca 10%, apariencia 6%)
- `getPuntajeTotal()` → suma los campos no-null; retorna `null` si todos son null (evaluación sin puntaje)
- `getClasificacion()` → texto según rango: 47+ "Excepcional", 38+ "Excelente", 30+ "Muy buena", 21+ "Buena", 14+ "Aceptable", 7+ "Deficiente", 0–6 "Inaceptable"
- `getBadgeClass()` → clase Bootstrap según rango: Excepcional `"bg-warning text-dark"`, Excelente `"bg-success"`, Muy buena `"bg-info text-dark"`, Buena `"bg-primary"`, Aceptable `"bg-secondary"`, Deficiente/Inaceptable `"bg-danger"`. Retorna `"bg-secondary"` si `getPuntajeTotal()` es null.
- **Mostrado en `detalle.html`**: card collapsible antes de Observaciones/Notas. Tabla con historial de evaluaciones (fecha, catador, columnas BJCP, total, badge de clasificación, botón eliminar). Formulario de nueva evaluación con sliders `form-range` y JS inline para total + badge en tiempo real.

### Barril
Inventario de kegs y barriles. Tabla `barriles`. Tiene `@TenantId` (heredado de `AuditableEntity`). **Extiende `AuditableEntity`.**
- `id`, `codigo` (VARCHAR 50, NOT NULL, UNIQUE por tenant via `ux_barriles_codigo_tenant`)
- `tipo` (VARCHAR 50, nullable) — tipo de barril; valores gestionados con lista estática en el controller ("Keg 20L", "Keg 30L", "Keg 50L", "Barril 30L", "Barril 60L", "Otro")
- `capacidadLitros` (DECIMAL 8,2, nullable) — capacidad en litros
- `@Enumerated(EnumType.STRING) estado → EstadoBarril` — default DISPONIBLE
- `loteId` (Long, nullable, `@Column(name="lote_id")`) — FK lógica a `lotes_cerveza`; se limpia al pasar a estados "vacíos"
- `codigoLote` (VARCHAR 50, nullable) — desnormalizado del lote asociado
- `clienteNombre` (VARCHAR 200, nullable) — cliente/bar al que fue despachado
- `fechaDespacho` (LocalDate, nullable) — fecha de despacho
- `observaciones` (VARCHAR 500, nullable)
- Helpers: `isDisponible()`, `isLleno()`, `isDespachado()`, `isEnBaja()`
- Los 4 campos de auditoría vienen de `AuditableEntity`
- **Delete físico** (no soft delete) — `BarrilService.eliminar()` llama `deleteById` directamente

### MovimientoBarril
Historial de cambios de estado de un barril. Tabla `movimientos_barriles`. Tiene `@TenantId` directo (no extiende `AuditableEntity`). **Sin FK a `barriles`** — preserva historial si el barril se elimina (mismo patrón que `VentaHistorialEstado`, `FacturaHistorialEstado`).
- `id`, `tenantId` (@TenantId), `barrilId` (Long, NOT NULL, sin FK)
- `@Enumerated(EnumType.STRING) estadoAnterior → EstadoBarril` — nullable (null = creación inicial del barril)
- `@Enumerated(EnumType.STRING) estadoNuevo → EstadoBarril` (NOT NULL)
- `usuario` (VARCHAR 100) — nombre del usuario autenticado (via `SecurityContextHolder`); `"sistema"` si no hay sesión
- `notas` (VARCHAR 500, nullable) — notas del cambio; blank normalizado a null en el factory
- `fecha` (TIMESTAMP, NOT NULL, seteado por `@PrePersist`)
- Factory: `MovimientoBarril.of(barrilId, estadoAnterior, estadoNuevo, usuario, notas)` — crea instancia lista para persistir
- **EstadoBarril** (`com.alera.model.enums`): `DISPONIBLE("Disponible","bg-success")`, `LLENO("Lleno","bg-primary")`, `DESPACHADO("Despachado","bg-warning text-dark")`, `VACIO("Vacío","bg-secondary")`, `LIMPIEZA("En limpieza","bg-info text-dark")`, `BAJA("Dado de baja","bg-danger")`. Cada valor tiene `getDisplayName()` y `getBadgeClass()`.

### Usuario
No extiende `AuditableEntity`. Gestiona su propia auditoría con `@PrePersist createdAt`. Campos:
- `id`, `tenantId` (@TenantId — usuarios aislados por tenant), `username` (unique por tenant)
- `password` — siempre BCrypt encodeado, nunca texto plano
- `@Enumerated(EnumType.STRING) RolUsuario rol` — enum type-safe. Valores válidos: `ADMIN`, `INVENTARIO`, `FACTURACION`, `EQUIPOS`. **No usar Strings libres.**
- `activo` (boolean, default true) — los usuarios inactivos no pueden autenticarse (`loadUserByUsername` lanza `UsernameNotFoundException` si `!activo`)
- `createdAt` (LocalDateTime) — seteado por `@PrePersist`
- **RolUsuario** (`com.alera.model.enums`): `ADMIN("Administrador")`, `PRODUCCION("Producción")`, `INVENTARIO("Inventario")`, `FACTURACION("Facturación")`, `EQUIPOS("Equipos")`. Cada valor tiene `getDisplayName()` para mostrar en UI.
- **Multi-tenant**: `loadUserByUsername` filtra automáticamente por tenant activo (Hibernate añade `WHERE tenant_id = :current`). El mismo `username` puede existir en distintos tenants.

---

## REPOSITORIOS (queries clave)

### LoteCervezaRepository
- `findByFiltros(estilo, fase, desde, hasta, Pageable)` — filtros + paginación + rango de fechas; `desde`/`hasta` nullable con `IS NULL` en JPQL
- `findTop5(Pageable)`, `findByIdWithIngredientes(id)`
- `countDistinctEstilos()`, `countEnProceso()`, `countCompletados()`
- `countLotesActivosByEquipo(equipoId)`
- `findLitrosPorMes(desde, tenantId)` — nativeQuery, usa `CAST(EXTRACT(...) AS integer)` (NO `::int`); filtra por `tenant_id` y `deleted_at IS NULL` explícitamente (Hibernate no filtra queries nativas)
- `findLotesPorEstilo(tenantId)`, `findParaKanban(limite)`, `findByPeriodo(desde, hasta)` — `:desde IS NULL OR l.fechaElaboracion >= :desde` y `:hasta IS NULL OR l.fechaElaboracion <= :hasta`. Ambos parámetros nullable — null = sin restricción (todo el historial). Usado por el reporte de producción y sus exports.
- `findResumenPorEstilo(desde, hasta, tenantId)` — nativeQuery para reporte; filtra por `tenant_id` y `deleted_at IS NULL` explícitamente
- `findByRecetaId(recetaId)` — lotes elaborados con una receta
- `findByIds(List<Long> ids)` — `SELECT DISTINCT ... LEFT JOIN FETCH ingredientes WHERE id IN :ids` — para comparativa; DISTINCT evita filas duplicadas del join con colección
- `findMaxConsecutivoPorPrefix(prefix, tenantId)` — `nativeQuery=true`, `MAX(CAST(SUBSTRING(...) AS integer)) WHERE codigo_lote LIKE :prefix||'-%' AND tenant_id = :tenantId`. Native para incluir filtro explícito de tenant y evitar colisiones cross-tenant en códigos de lote. **CRÍTICO**: Hibernate NO hace flush automático antes de native queries → `TrazabilidadService.generarCodigo()` llama `em.flush()` antes de invocar esta query para ver inserts previos de la misma transacción.
- `search(q, Pageable)` — búsqueda global por codigoLote o estilo

### RecetaRepository
- `findAllByActivaTrueOrderByNombreAsc()` ← campo `activa` (boolean) en entidad Receta
- `findAllByOrderByActivaDescNombreAsc()` — lista sin paginar (para selects)
- `findAllByOrderByActivaDescNombreAsc(Pageable)` — paginada, activas primero
- `findByActivaOrderByNombreAsc(boolean activa, Pageable)` — filtrada por estado + paginada
- `search(q, Pageable)` — búsqueda global por nombre o estilo

### TipoCervezaRepository
- `findByActivoTrueOrderByNombreAsc()` — tipos activos para selects
- `findByNombreIgnoreCase(nombre)` — búsqueda exacta case-insensitive
- `existsByNombreIgnoreCase(nombre)` — validación de unicidad antes de crear

### LogAccesoRepository
- `findAllByOrderByFechaDesc(Pageable)`, `findByTipoOrderByFechaDesc(tipo, Pageable)`
- `countFallidosDesde(desde)` — métrica de seguridad

### InsumoInventarioRepository
- `findByNombreExacto(nombre)` — `LOWER(TRIM(i.nombre)) = LOWER(TRIM(:nombre))` — case-insensitive exact match
- `findBajoStock()`, `countBajoStock()` — items donde cantidad ≤ stockMinimo
- `findProximosAVencer(fecha)`, `countProximosAVencer(fecha)`
- `findAllByOrderByNombreAsc()` — para datalists en formularios
- `findByFiltros(nombre, tipo, Pageable)` — filtros con paginación

### ProveedorRepository
- `findAllByActivoTrueOrderByNombreAsc()` ← **`activo`** (no `activa`) — Proveedor usa `activo`
- `countFacturas(proveedorId)`, `sumFacturas(proveedorId)`

### FacturaProveedorRepository
- `findAllWithItems()` — DISTINCT + JOIN FETCH (usado en `TrazabilidadController.agregarInventarioAlModelo()` para el buscador de costos)
- `findAllFiltered(estado, desde, hasta, Pageable)` — paginado con filtros opcionales: `:estado IS NULL OR f.estado = :estado`, `:desde IS NULL OR f.fechaFactura >= :desde`, `:hasta IS NULL OR f.fechaFactura <= :hasta`. Orden `fechaFactura DESC NULLS LAST, id DESC`. Único query paginado — reemplazó `findAllPaged` y `findAllPagedByEstado`.
- `findByIdWithItems(id)` — LEFT JOIN FETCH items por id
- `search(q, Pageable)` — LIKE en `COALESCE(numeroFactura,'')` y `COALESCE(proveedor,'')`, orden `fechaFactura DESC NULLS LAST` — para el typeahead de la lista de facturas
- `sumTotalFiltered(estado, desde, hasta)` — `COALESCE(SUM(valorTotal), 0)` con los mismos filtros opcionales de `findAllFiltered`; usado para stat-cards
- `sumPorEstados(estados, desde, hasta)` — `COALESCE(SUM(valorTotal), 0)` donde `f.estado IN :estados`; usado para monto pendiente (RECIBIDA + VERIFICADA)
- `countPorEstados(estados, desde, hasta)` — `COUNT(f)` donde `f.estado IN :estados`; usado para conteo pendiente
- `findSinProcesar(estados, umbral)` — facturas con `estado IN :estados` y `fechaFactura <= :umbral`; usado por `AlertaScheduler` para detectar facturas sin procesar

### FacturaHistorialEstadoRepository
- `findByFacturaIdOrderByFechaDesc(facturaId)` — historial de cambios de estado de una factura, orden cronológico inverso. Hibernate filtra automáticamente por tenant activo via `@TenantId`.

### MigracionLogRepository
- `findByTenantIdOrderByFechaDesc(tenantId)` — historial de importaciones del tenant, orden cronológico inverso. Query JPQL sin `@TenantId` — consulta por el campo `tenantId` directamente (Hibernate NO añade filtro automático porque la entidad no tiene `@TenantId`).
- `countByTenantId(tenantId)` — conteo de importaciones del tenant.

### ClienteRepository
- `findAllByActivoTrueOrderByNombreAsc()` — clientes activos para selects y dropdown
- `findAllByOrderByNombreAsc()` — todos los clientes sin filtro de estado
- `findByNit(String nit)` — `Optional<Cliente>` — usado por `ClienteService` para validar unicidad de NIT antes de guardar
- `findAllFiltered(nombre, activo, Pageable)` — paginado con filtros opcionales: `:nombre = '' OR LOWER(c.nombre) LIKE LOWER(CONCAT('%',:nombre,'%'))` y `:activo IS NULL OR c.activo = :activo`. Orden `c.nombre ASC`. **CRÍTICO**: usa `:nombre = ''` (no `:nombre IS NULL`) para evitar el error `lower(bytea)` de Hibernate 6 — `ClienteService.listarPaginado()` pasa `""` cuando no hay filtro de nombre (regla 9).
- `searchActivos(q, Pageable)` — busca solo entre clientes activos por nombre o NIT con LIKE. Usado por `ClienteService.suggest()` (limit 6) y por `VentaService.suggestClientes()` (retorna solo el nombre).

### VentaRepository
- `findAllFiltered(estado, desde, hasta, Pageable)` — paginado con filtros opcionales: `:estado IS NULL OR v.estado = :estado`, `:desde IS NULL OR v.fechaDespacho >= :desde`, `:hasta IS NULL OR v.fechaDespacho <= :hasta`. Orden `fechaDespacho DESC NULLS LAST, id DESC`. **Patrón IS NULL OR** — no usa valores centinela (1900/2100).
- `countByEstado(EstadoVenta)` — conteo por estado; usado en stat-cards.
- `countClientesUnicos()` — `COUNT(DISTINCT v.cliente)` — para stat-card de clientes únicos.
- `search(q, Pageable)` — LIKE en `LOWER(v.cliente)` y subquery `EXISTS (SELECT 1 FROM VentaItem i WHERE i.venta = v AND LOWER(COALESCE(i.codigoLote,'')) LIKE ...)`. Orden `fechaDespacho DESC NULLS LAST`.
- `findByPeriodo(desde, hasta)` — `List<Venta>` para export; `:desde IS NULL OR v.fechaDespacho >= :desde` y `:hasta IS NULL OR v.fechaDespacho <= :hasta`. Ambos parámetros nullable.
- `findTopClientes(tenantId)` — `nativeQuery=true`, JOIN con `venta_items`. Top 5 clientes por ingresos despachados (`estado='DESPACHADO'`), agrupados con `COUNT(DISTINCT v.id)` y `SUM(vi.cantidad × vi.precio_unitario × (1 - vi.descuento_pct/100))`. Requiere `tenantId` explícito y filtra `deleted_at IS NULL` manualmente (native query).
- `findClientesSuggestions(q, Pageable)` — `SELECT DISTINCT v.cliente` con `LOWER(v.cliente) LIKE LOWER(:q||'%')`, orden alfabético. Usado por `VentaService.suggestClientes()` → `GET /ventas/suggest-clientes`.
- `findCotizacionesVencidas(hoy)` — `List<Venta>` con `estado = COTIZACION AND cotizacion_expira_en < :hoy`. Sin paginación — el scheduler procesa todas de una vez. Hibernate añade filtro de tenant automáticamente via `@TenantId`.

### VentaItemRepository
- `findByVentaId(Long ventaId)` — `List<VentaItem>` para una venta concreta.
- `findVentasByLoteId(loteId)` — `SELECT DISTINCT i.venta FROM VentaItem i WHERE i.lote.id = :loteId ORDER BY i.venta.fechaDespacho DESC`. Reemplaza el anterior `VentaRepository.findByLoteIdOrderByFechaDespachoDesc`. Usado por `VentaService.listarPorLote()` y `TrazabilidadController.ver()`.
- `sumCantidadActivaByLote(loteId, excludeVentaId)` — `COALESCE(SUM(i.cantidad), 0)` de ítems cuya `venta.estado != CANCELADO`, excluyendo los de la venta `excludeVentaId` (para edición). Usado por `VentaService.validarItemCantidad()`.
- `sumCantidadActivaByLoteAndUnidad(loteId, unidad, excludeVentaId)` — igual que el anterior pero filtrando además por `LOWER(COALESCE(i.unidad,'')) = LOWER(:unidad)`. Permite calcular lo vendido de un formato específico (ej. solo Botella 330ml) sin mezclar con otros formatos del mismo lote. Usado por `VentaService.validarItemCantidad()` cuando `carbDestino` tiene múltiples entradas.
- `findUnidadesActivasByLote(loteId, excludeVentaId)` — `SELECT DISTINCT i.unidad` de ítems activos del lote, filtrando `i.unidad IS NOT NULL AND i.unidad <> ''`. Retorna `Set<String>`. Usado por `VentaService.validarItemCantidad()` para detectar mezcla de unidades.
- `sumIngresosDespachados()` — `COALESCE(SUM(i.cantidad * i.precioUnitario * (1 - i.descuentoPct/100.0)), 0)` donde `i.venta.estado = DESPACHADO`. Retorna null si no hay ítems (el servicio normaliza a ZERO). Usado por `VentaService.sumIngresosDespachados()`.
- `findItemsConEnvase(ventaId)` — `List<VentaItem>` donde `LOWER(i.unidad) LIKE '%botella%' OR LIKE '%lata%' OR LIKE '%barril%' OR LIKE '%growler%' OR LIKE '%und%'`. Usado por `VentaService.descontarEnvases()` al despachar para descontar stock de packaging del inventario.

### VentaHistorialEstadoRepository
- `findByVentaIdOrderByFechaDesc(ventaId)` — historial de cambios de estado de una venta. Hibernate filtra automáticamente por tenant activo via `@TenantId`.

### ElaboracionPlanificadaRepository
- `findProximas(desde)` — elaboraciones con `fechaPlaneada >= :desde`, `LEFT JOIN FETCH receta`, orden ASC
- `findAllOrdenadas()` — todas las elaboraciones con `LEFT JOIN FETCH receta`, orden ASC por fecha
- `findByEstado(estado)` — filtrado por `EstadoPlanificacion`, `LEFT JOIN FETCH receta`
- `findByRangoFecha(desde, hasta)` — para el feed de eventos de FullCalendar (`BETWEEN`)
- `findByIdWithRecetaEIngredientes(id)` — `SELECT DISTINCT … LEFT JOIN FETCH receta r LEFT JOIN FETCH r.ingredientes` — carga el plan con receta e ingredientes en una sola query; necesario para pre-llenar el formulario de lote sin LazyInitializationException

### LecturaFermentacionRepository
- `findByLoteIdOrdenadas(loteId)` — `ORDER BY l.fecha ASC, l.id ASC`. Hibernate agrega filtro de tenant automáticamente vía `@TenantId`.

### EvaluacionSensorialRepository
- `findByLoteIdOrdenadas(loteId)` — `@Query` JPQL `ORDER BY e.fecha DESC, e.id DESC` — evaluaciones más recientes primero. Hibernate filtra automáticamente por tenant activo vía `@TenantId`.

### BarrilRepository
- `findByFiltros(codigo, estado, Pageable)` — `@Query` JPQL con patrón `:codigo = '' OR LOWER(b.codigo) LIKE LOWER(CONCAT('%',:codigo,'%'))` (pasa `""` no `null` para evitar `lower(bytea)`) y `:estado IS NULL OR b.estado = :estado`. Orden `b.codigo ASC`.
- `countByEstado(EstadoBarril estado)` — conteo por estado; para stat-cards.
- `existsByCodigoIgnoreCase(String codigo)` — validación de unicidad en `guardar` (nuevo).
- `existsByCodigoIgnoreCaseAndIdNot(String codigo, Long excludeId)` — validación de unicidad en `actualizar` (excluye el propio registro).

### MovimientoBarrilRepository
- `findByBarrilIdOrderByFechaDesc(Long barrilId)` — `List<MovimientoBarril>` ordenada por fecha DESC para el historial del detalle. Hibernate filtra automáticamente por tenant activo vía `@TenantId`.

### NotificacionRepository
- `findTop5ByLeidaFalseOrderByCreatedAtDesc()` — últimas 5 no leídas para el dropdown del navbar
- `countByLeidaFalse()` — conteo para el badge de la campana
- `findAllOrdenadas(Pageable)` — todas ordenadas: no leídas primero, luego por fecha DESC — para la página historial
- `marcarTodasLeidas()` — `@Modifying UPDATE SET leida = true WHERE leida = false` — bulk update dentro del tenant activo
- `existeEnPeriodo(tipo, desde, hasta)` — deduplicación diaria: evita crear la misma notificación dos veces el mismo día. Usado por `NotificacionService.crearAlertas()` antes de persistir cada tipo.

### TenantRepository
- `findBySubdomainAndActiveTrue(String subdomain)` — usado por `TenantFilter`; la entidad `Tenant` NO tiene `@TenantId` (es la tabla maestra)

### FacturaItemRepository
- `JpaRepository<FacturaItem, Long>`
- `findHistorialPreciosPorNombre(nombre)` — `JOIN FETCH fi.factura`, filtra por `LOWER(TRIM(fi.nombre)) = LOWER(TRIM(:nombre))`, `cantidad > 0`, orden `f.fechaFactura DESC NULLS LAST`. **CRÍTICO**: el campo de fecha en `FacturaProveedor` es `fechaFactura` (no `fecha`) — usar `f.fechaFactura` en JPQL y `getFechaFactura()` en Java.
- `findNombresDistintos()` — `SELECT DISTINCT fi.nombre` para datalist de búsqueda
- `findUltimosPrecios(List<String> nombres)` — `JOIN FETCH fi.factura`, filtra por `LOWER(TRIM(fi.nombre)) IN :nombres` y `valorUnitario > 0`, orden `f.fechaFactura DESC NULLS LAST, fi.id DESC`. Devuelve todos los ítems que coincidan; el controller toma el primero por nombre (más reciente). Usado por `RecetaController.calcularCostosEstimados()` para estimación de costo por ingrediente.
- Usado también por `TrazabilidadService.mapearDto()` para resolver ítems por ID al guardar lotes

### MantenimientoEquipoRepository
- `JpaRepository<MantenimientoEquipo, Long>`
- `findByEquipoIdOrderByFechaDesc(equipoId)` — historial de mantenimientos de un equipo, orden cronológico inverso
- `findMantenimientoPendiente(fecha)` — equipos cuyo `proximoMantenimiento <= :fecha`; usado por `EquipoService.listarMantenimientoPendiente()`
- `countMantenimientoPendiente(fecha)` — `COUNT` de equipos con `proximoMantenimiento <= :fecha`; ventana por defecto 7 días
- `sumTotalCostos()` — `SUM(m.costo)` global; usado en el dashboard
- `sumCostoByEquipoId(equipoId)` — `COALESCE(SUM(m.costo), 0)` filtrado por equipo; retorna `BigDecimal` nunca null — para costoTotal en detalle y mantenimientos
- `countByEquipoId(equipoId)` — `COUNT(m)` filtrado por equipo; para totalMantenimientos en detalle y mantenimientos

---

## SERVICIOS (lógica de negocio)

### TrazabilidadService
- `listarPaginado(estilo, fase, page)` — sobrecarga sin fechas
- `listarPaginado(estilo, fase, desde, hasta, page)` — con rango de fechas
- `guardar/actualizar/eliminar` → registra historial + auditing JPA automático + `@CacheEvict(value="...", allEntries=true)` en las 3 caches del dashboard (`allEntries=true` requerido porque la clave es el tenant, no los parámetros del método)
- `actualizar()` compara ingredientes antes/después con `ingredientesModificados()` (por nombre|cantidad, lista ordenada). Solo llama `restaurarInventario(old) + descontarInventario(new)` si hubo cambio real — evita movimientos duplicados en `movimientos_inventario` cuando el usuario edita fechas, notas o carbonatación sin tocar ingredientes.
- `listarParaKanban()` — lotes activos + completados últimos 7 días
- `moverFase(id, fase)` — cambia las fechas de fase del lote. **Avanzar**: setea `*FechaInicial` solo si era null (preserva fecha real de inicio); setea `*FechaFinal` de la fase que se deja solo si era null. **Retroceder**: limpia `*FechaInicial` y `*FechaFinal` de todas las fases posteriores a la destino, y `*FechaFinal` de la fase destino misma (el lote vuelve a estar "en curso" en esa fase). Comportamiento por destino: `sinIniciar` → limpia todo; `fermentacion` → **lanza `IllegalStateException` si `lote.getEquipoFermentador() == null`** (el kanban JS lo bloquea client-side antes del POST, pero el servicio también valida); preserva `fermFechaInicial`, limpia `fermFechaFinal` y todo lo posterior; `acondicionamiento` → cierra ferm, abre acond; `maduracion` → cierra ferm+acond, abre madur; `carbonatacion` → cierra ferm+acond+madur, abre carb; `completados` → cierra todas las fases. `@CacheEvict(value="dashboard-stats", allEntries=true)` + registra `HistorialLote` con acción "EDITADO" y notas "Fase → {fase}". Valores válidos de `fase`: `sinIniciar`, `fermentacion`, `acondicionamiento`, `maduracion`, `carbonatacion`, `completados`.
- `obtenerHistorial(loteId)` → historial manual (complementa auditing JPA)
- `toLoteFormDto(lote)` — delega a `LoteMapper` (MapStruct). No hace mapeo manual.
- `suggest(q)` — busca por codigoLote o estilo via `loteRepo.search()`, retorna hasta 6 mapas con `{codigoLote, estilo, fase, completado, url}` — usado por `GET /suggest`
- Lanza `LoteNoEncontradoException` (HTTP 404) cuando no encuentra un lote — ya no usa `RuntimeException` genérica
- **CRÍTICO**: `@DateTimeFormat(iso=DATE)` en todos los `LocalDate` de `LoteFormDto`
- `pageSize` inyectado via `@Value("${app.page-size:15}")`
- Inyecta `FacturaItemRepository` (no `FacturaProveedorRepository`) — `mapearDto()` resuelve ítems por ID y construye `LoteItemFactura` con `cantidadAsignada`
- Inyecta `EntityManager em` — usado en dos lugares: (1) `em.flush()` en `generarCodigo()` antes de `findMaxConsecutivoPorPrefix` para que Hibernate sincronice inserts previos con la BD antes de ejecutar la native query; (2) `em.flush()` en `mapearDto()` antes de los INSERT de `LoteItemFactura` para forzar los DELETE de orphans previos y evitar conflictos de constraint.
- `LoteFormDto` usa `itemsIds` (List<Long>) + `itemsCantidades` (List<BigDecimal>) como listas paralelas para binding de ítems de costo

### LogAccesoService
- `registrar(usuario, tipo, ip, url, userAgent, detalles)` — `@Transactional(REQUIRES_NEW)` para garantizar persistencia independiente
- `listarPaginado(tipo, page)` — filtrado por tipo opcional
- `fallidosUltimaHora()` — alerta de intentos de fuerza bruta
- `pageSize` inyectado via `@Value("${app.log-page-size:25}")`

### RecetaService
- `listarActivas()` — para selects en formularios
- `listarTodas()` — lista completa sin paginar
- `listarPaginado(Boolean activa, int page)` — paginada con filtro opcional (null=todas, true=activas, false=inactivas)
- `toFormDto` parsea `cantidad` normalizada de vuelta a `{cantidad, unidad}`, mapea `adicionesHervor` y mapea `phAgua`
- `actualizar()` → limpia `ingredientes`, `escalones` **y `adicionesHervor`** antes de remapear; incrementa `version` automáticamente (`version = (version ?? 1) + 1`)
- `mapDtoToEntity()` → persiste `adicionesHervor` además de ingredientes y escalones; copia `phAgua` del DTO
- `duplicarComoFormDto(Long id)` — carga la receta, llama `toFormDto()`, limpia `id` (null) y agrega " (Copia)" al nombre. El submit va a `POST /recetas/guardar` — crea una nueva receta, no edita la original. Version siempre empieza en 1 en la copia.
- `suggest(q, Boolean activa)` — filtra via `repo.search()` (limit 10) + stream filter por `activa` si no es null, retorna hasta 6 mapas con `{nombre, estilo, activa, url}` — usado por `GET /recetas/suggest`
- `pageSize` inyectado via `@Value("${app.page-size:15}")`

### InsumoInventarioService
- `buscarPorId(id)` — `Optional<InsumoInventario>`
- `buscarPorNombreExacto(nombre)` — delega a `repo.findByNombreExacto()`, usado para validar duplicados en quick-create
- `descontarIngrediente(nombre, cantidadTexto)` / `descontarIngrediente(nombre, cantidadTexto, referencia)` — retorna nombre si stock insuficiente, null si OK. `referencia` = codigoLote (se registra en `movimientos_inventario`).
- `restaurarIngrediente(nombre, cantidadTexto)` / `restaurarIngrediente(nombre, cantidadTexto, referencia)` — suma cantidad de vuelta al inventario; ídem con `referencia`.
- `listarBajoStock()`, `listarProximosAVencer(dias)`
- `listarPaginado(nombre, tipo, page)` — paginado con filtros opcionales; usado también por `/inventario/suggest`
- `detectarTipo(nombre)` — infiere el tipo del nombre por palabras clave: malta/pilsner/malt → "Malta"; lupulo/lúpulo/hop → "Lúpulo"; levadura/yeast → "Levadura"; clarific/gelatin/irish → "Clarificante"; dextrosa/sacarosa/priming/carbonat/extracto de malta → "Agente de Carbonatación"; envase/botell/lata → "Envase"; resto → "Otro". Retorna String display name, no enum.
- `parsearCantidad(texto)` — toma SOLO el primer token numérico del texto, ignora el sufijo de unidad. `"150 gr"` → 150, `"3800 gr"` → 3800. Es seguro ignorar la unidad porque `normalizarParaAlmacenamiento` ya convirtió a base (gr/mL) al guardar el ingrediente — el string almacenado en `Ingrediente.cantidad` siempre está en unidad base.
- **CRÍTICO — `movimientos_inventario` duplicados**: `RESTAURACION_LOTE` + `DESCUENTO_LOTE` consecutivos con la misma `referencia` y el mismo `cantidad` son pares espurios. Ocurrían cuando el código anterior corría `restaurarInventario` + `descontarInventario` en cada edición de lote independientemente de si los ingredientes cambiaron. El fix es `ingredientesModificados()` en `actualizar()` — solo ajusta inventario cuando el conjunto de ingredientes (nombre|cantidad ordenado) difiere entre antes y después.

### ProveedorService / FacturaProveedorService
- `ProveedorService.contarFacturas/totalFacturas` — estadísticas para vista edición
- `ProveedorService.suggest(q)` — usa `repo.search(q, PageRequest.of(0,6))` (LIKE en nombre y NIT), retorna hasta 6 mapas con `{nombre, nit, activo, url}` — usado por `GET /proveedores/suggest`
- `FacturaProveedorService` inyecta `ProveedorRepository` y `FacturaHistorialEstadoRepository` para vincular proveedor y registrar historial al guardar/cambiar estado
- `FacturaProveedorService.guardar/actualizar/eliminar` → `@CacheEvict("dashboard-stats")` — invalida caché al modificar datos financieros. `guardar()` además registra el estado inicial en `factura_historial_estado`.
- `FacturaProveedorService.mapearDto()` copia `dto.isIvaIncluido()` → `factura.setIvaIncluido()` **antes** del loop de ítems, para que `calcularTotales()` pueda acceder a `factura.isIvaIncluido()` via la referencia `item.factura`. `toFormDto()` hace el camino inverso (`dto.setIvaIncluido(f.isIvaIncluido())`).
- `FacturaProveedorService.calcularTotales()` — acumula `subtotal` (`getValorBase()`), `totalIva` (`getValorIvaItem()`) y `totalImpConsumo` (`impuestoConsumo`) por ítem. `valorTotal = subtotal + totalIva + totalImpConsumo + costoEnvio`. Los métodos computados de `FacturaItem` ya respetan `ivaIncluido` internamente — el servicio no duplica lógica (DRY).
- `FacturaProveedorService.listarPaginado(EstadoFactura estado, LocalDate desde, LocalDate hasta, int page)` — delega a `findAllFiltered`; los tres filtros son opcionales (null = sin filtro)
- `FacturaProveedorService.listarParaExport(EstadoFactura estado, LocalDate desde, LocalDate hasta)` — `@Transactional(readOnly=true)`, llama `findAllWithItems()` y filtra en memoria; los tres parámetros son opcionales (null = sin filtro)
- `FacturaProveedorService.cambiarEstado(id, EstadoFactura)` — actualiza estado y persiste `FacturaHistorialEstado` con estado anterior, nuevo y usuario actual (via `SecurityContextHolder`)
- `FacturaProveedorService.listarHistorial(facturaId)` — `@Transactional(readOnly=true)`, delega a `historialRepo.findByFacturaIdOrderByFechaDesc`
- `FacturaProveedorService.sumTotal(estado, desde, hasta)` — `@Transactional(readOnly=true)`, delega a `sumTotalFiltered`; para stat-cards de la lista
- `FacturaProveedorService.sumPendiente(desde, hasta)` — `@Transactional(readOnly=true)`, suma RECIBIDA + VERIFICADA; para stat-card de monto pendiente
- `FacturaProveedorService.countPendiente(desde, hasta)` — `@Transactional(readOnly=true)`, cuenta RECIBIDA + VERIFICADA; para stat-card de facturas pendientes
- `FacturaProveedorService.listarSinProcesar(dias)` — `@Transactional(readOnly=true)`, facturas RECIBIDA/VERIFICADA con `fechaFactura <= today - dias`; usado por `AlertaScheduler`
- `FacturaProveedorService.duplicarComoFormDto(id)` — llama `toFormDto()` y limpia `numeroFactura`, `fechaFactura`; setea `estado = RECIBIDA`; devuelve DTO listo para pre-llenar el formulario
- `FacturaProveedorService.suggest(q)` — usa `repo.search()`, retorna hasta 6 mapas con `{titulo, proveedor, fecha, total, url}` — usado por `GET /facturas/suggest`
- `pageSize` inyectado via `@Value("${app.page-size:15}")`

### DashboardService
- `getLitrosPorMes()` — datos para Chart.js — `@Cacheable(value="dashboard-litros-mes", key="T(com.alera.config.TenantContext).getCurrentTenant()")` TTL 10 min; pasa `tenantId` a `findLitrosPorMes`
- `getLotesPorEstilo()` — datos para Chart.js — `@Cacheable(value="dashboard-estilos", key="T(com.alera.config.TenantContext).getCurrentTenant()")` TTL 10 min; pasa `tenantId` a `findLotesPorEstilo`
- `obtenerEstadisticas()` — 13 COUNT queries a nivel BD — `@Cacheable(value="dashboard-stats", key="T(com.alera.config.TenantContext).getCurrentTenant()")` TTL 5 min; queries JPQL ya filtradas por Hibernate via `@TenantId`
- **CRÍTICO multi-tenant**: las 3 queries nativas del dashboard (`findLitrosPorMes`, `findLotesPorEstilo`, `findResumenPorEstilo`) requieren `tenantId` explícito — Hibernate NO filtra `nativeQuery=true`
- Caché Caffeine configurada en `CacheConfig`: `dashboard-stats` (50 entradas, 5 min), `dashboard-litros-mes` y `dashboard-estilos` (50 entradas c/u, 10 min). `maximumSize(50)` soporta múltiples tenants con claves distintas
- Las 3 caches se invalidan automáticamente al crear/editar/eliminar lotes (`allEntries = true`); `dashboard-stats` también al modificar facturas

### EquipoService
- `suggest(q, EstadoEquipo estado)` — filtra en memoria sobre `listarPorEstado(estado)` o `listarTodos()`, retorna hasta 6 mapas con `{nombre, tipo, estado, colorEstado, pendiente, url}` — usado por `GET /equipos/suggest`
- `cambiarEstado(id, EstadoEquipo)` — busca el equipo por id, actualiza `estado` y guarda. Lanza `RuntimeException` si no existe.
- `countByEstado(EstadoEquipo)` — delega a `repo.countByEstado()` — para stat-cards
- `countMantenimientoPendiente()` — delega a `repo.countMantenimientoPendiente(LocalDate.now().plusDays(7))` — para stat-cards
- `countTotal()` — delega a `repo.count()` — para stat-cards

### MantenimientoEquipoService
- `listarPorEquipo(equipoId)` — historial de mantenimientos ordenado por fecha desc
- `registrar(equipoId, dto)` — crea `MantenimientoEquipo` Y actualiza `equipo.fechaUltimoMantenimiento` y `equipo.proximoMantenimiento` en la misma transacción
- `eliminar(id)` — elimina registro de mantenimiento
- `sumCostoPorEquipo(equipoId)` — `@Transactional(readOnly=true)`, delega a `repo.sumCostoByEquipoId()` con `COALESCE(..., 0)` — retorna BigDecimal nunca null
- `countPorEquipo(equipoId)` — `@Transactional(readOnly=true)`, delega a `repo.countByEquipoId()` — retorna long

### TipoCervezaService
- `listarActivos()` — `findByActivoTrueOrderByNombreAsc()` — para selects y datalists en formularios
- `listarTodos()` — todos incluyendo inactivos
- `existePorNombre(nombre)` — delega a `repo.existsByNombreIgnoreCase()`, usado en quick-create
- `guardar(tipo)` / `eliminar(id)` — CRUD básico
- `toggleActivo(id)` — invierte el flag `activo` sin borrar el tipo

### CategoriaInsumoService
- `listarNombresActivos()` — `List<String>` de nombres activos ordenados por nombre ASC. Usado por `InsumoInventarioController`, `EquipoController` (no — ver Equipo), `FacturaProveedorController` para poblar selects y construir `insumosPorTipo`.
- `listarTodos()` — `List<CategoriaInsumo>` incluyendo inactivas. Usado por `CategoriaController`.
- `guardar(nombre)` — valida unicidad por tenant (`existsByNombreIgnoreCase`), crea y persiste. Lanza `RuntimeException` si ya existe.
- `toggleActivo(id)` — invierte `activo` y guarda.
- `eliminar(id)` — `repo.deleteById(id)`.

### CategoriaEquipoService
- Misma estructura que `CategoriaInsumoService`, opera sobre `CategoriaEquipo` / `tipos_equipo`.
- `listarNombresActivos()` — usado por `EquipoController` y `FacturaProveedorController`.

### UsuarioService
- Implementa `UserDetailsService` — usado por `SecurityConfig` via `DaoAuthenticationProvider`
- `loadUserByUsername(username)` — busca usuario activo y construye `UserDetails` con `ROLE_{rol.name()}`. Lanza `UsernameNotFoundException` si el usuario no existe o está inactivo.
- `listarTodos()` — ordenados por `createdAt` desc
- `buscarPorId(id)` — retorna `Optional<Usuario>`
- `buscarPorUsername(username)` — retorna `Optional<Usuario>` via `repo.findByUsername()` — usado por `PerfilController` para obtener el id del usuario en sesión
- `existeUsername(username)` — validación de unicidad
- `esElMismoUsuario(id, username)` — verifica si el id corresponde al username dado. Usado para evitar auto-eliminación/desactivación/cambio de rol.
- `guardar(username, password, RolUsuario rol)` — crea usuario con contraseña BCrypt; rol por defecto `RolUsuario.ADMIN`. Usa `repo.save()` — depende del `TenantContext` activo para `@TenantId`. **No usar en contexto cross-tenant** (ver regla 40).
- `guardarEnTenant(username, password, rol)` — `@Transactional(REQUIRES_NEW)`, mismo comportamiento que `guardar` pero en transacción nueva. Presente en el código pero el problema de `open-in-view` persiste; prefer `UsuarioRepository.insertarConTenant` para operaciones cross-tenant.
- `toggleActivo(id)` — habilita/deshabilita usuario
- `cambiarPassword(id, newPassword)` — re-encripta con BCrypt
- `cambiarRol(id, RolUsuario nuevoRol)` — actualiza el rol del usuario
- `eliminar(id)` — elimina usuario
- `suggest(q)` — filtra en memoria sobre `findAllByOrderByCreatedAtDesc()` por username, retorna hasta 6 mapas con `{username, rol, activo, anchor}` donde `anchor = "usuario-{id}"` — usado por `GET /usuarios/suggest`
- **CRÍTICO**: `Usuario.rol` es `@Enumerated(EnumType.STRING)` tipo `RolUsuario`. No usar Strings libres. Los valores válidos son `ADMIN`, `INVENTARIO`, `FACTURACION`, `EQUIPOS`.
- **Queries cross-tenant en `UsuarioRepository`** (todas `nativeQuery = true`): `findAllByTenantId(tenantId)`, `countByUsernameAndTenantId(username, tenantId)`, `insertarConTenant(username, password, rol, tenantId)`, `toggleActivoByIdAndTenantId(id, tenantId)`, `updatePasswordByIdAndTenantId(id, tenantId, password)`, `updateRolByIdAndTenantId(id, tenantId, rol)`, `deleteByIdAndTenantId(id, tenantId)`. Usan SQL nativo con `tenant_id` explícito — ver regla 40.

### PdfExportService
- `generarPdfLote(LoteCerveza, ExportBranding, List<LecturaFermentacion>)` → `byte[]` — genera PDF A4 con OpenPDF usando la paleta de colores del tenant. Secciones: encabezado, info del lote, parámetros/métricas, ingredientes, fases, **carbonatación — detalle** (si `carbMetodo`, `carbCo2Objetivo` o `carbDestino` no es null — via `addDetalleCarbonacion()`), **curva de fermentación** (si hay lecturas), **comparativa receta vs lote** (si tiene receta con OG/FG objetivo), costos, observaciones/notas de cata, pie de página. `addDetalleCarbonacion()`: tabla 4 cols (método, CO₂ objetivo→real, validación, destino) + fila condicional con parámetros del método (Natural: azúcar tipo + gramos; Forzada: presión PSI + tiempo + técnica). El destino se muestra con entradas separadas por `\n` (`carbDestino.replace(" | ", "\n")`) para que cada formato quede en su propia línea dentro de la celda PDF. La curva usa **Java2D** (BufferedImage 2x → PNG → bytes → `Image.getInstance(bytes)`), evitando los problemas de tipo de `PdfTemplate` con OpenPDF. El gráfico muestra: eje Y izquierdo dorado (densidad) + eje Y derecho azul (temperatura °C, aparece solo si hay lecturas con temperatura), línea dorada sólida de densidad, línea azul sólida de temperatura, puntos de colores en cada lectura, línea verde punteada de FG real, etiquetas X de fecha (dd/MM), leyenda con ambas series. El margen derecho se expande automáticamente (8pt → 40pt) cuando hay temperatura. El X axis usa el rango de TODAS las lecturas (no solo las de densidad). Bajo el gráfico: tabla con columnas adaptativas (temperatura y notas solo aparecen si alguna lectura las tiene). La sección "COMPARATIVA RECETA VS LOTE" muestra tabla OG/FG/ABV objetivo vs real con diferencia en verde (positivo) o rojo (negativo). Métrica FG muestra `densidadFinalFecha` como subtítulo cuando está presente.
- `generarPdfReceta(Receta receta, ExportBranding)` → `byte[]` — genera PDF A4 con OpenPDF usando paleta del tenant. Secciones: cabecera (nombre de receta + estilo), información general (nombre, estilo, estado, versión, hervor, vol. base, agua macerado/sparge, **pH agua si no es null**), **descripción** (si no está en blanco — párrafo texto libre), parámetros objetivo (OG/FG/ABV estimado si ambos están presentes), ingredientes agrupados por tipo (maltas/lúpulos/levaduras/clarificantes), escalones de macerado, adiciones de hervor, notas técnicas, pie de página. Reutiliza helpers `addTituloPdf`, `par`, `metricaCell`, `tableCell`.
- `generarPdfReporteProduccion(lotes, desde, hasta, estiloFiltro, ExportBranding)` → `byte[]` — genera PDF **landscape A4** con OpenPDF. Secciones: cabecera con período y filtro de estilo activo, resumen estadístico (8 métricas en tabla 8 cols), tabla de lotes (9 cols: Código, Estilo, Receta, Fecha, Litros, OG, ABV, Eficiencia, Estado) con filas alternas y código en color del tenant, y resumen por estilo (solo si hay >1 estilo). Helper privado `tablaCelda(t, text, font, bg)` para celdas con color de fila alterno.
- `generarPdfVenta(Venta, ExportBranding)` → `byte[]` — genera PDF A4 de remisión/nota de despacho. Secciones: cabecera (nombre del tenant + "REMISIÓN / NOTA DE DESPACHO" + ref. venta), datos del despacho (cliente, fecha, primer lote via `getPrimerCodigoLote()`, estado), detalle de ítems (tabla 6 cols: Lote, Descripción, Cantidad, Precio Unit., Desc.%, Total — una fila por `VentaItem`; si lista vacía muestra "Sin ítems registrados"), total general, pie de página. Usa helpers `addTituloPdf`, `par`. `open-in-view=true` garantiza acceso lazy a `venta.getItems()`. Descargado por `GET /ventas/{id}/pdf` como `remision-venta-{id}.pdf`.
- Colores neutros fijos (no cambian con branding): `C_GRIS`, `C_BORDE`. El resto usa `Pal` record interno calculado desde `ExportBranding`.
- Solo importa `com.lowagie.text.*` — sin colisión con POI.
- Inyectado en `TrazabilidadController`, `RecetaController`, `ReporteController` y `VentaController`.

### ExcelExportService
- `generarExcelReporteProduccion(lotes, resumen, desde, hasta, ExportBranding)` → `byte[]` — genera `.xlsx` con Apache POI. Dos hojas: hoja 1 con título, período, **2 filas de resumen estadístico** (fila 1: total lotes, litros, estilos, completados+%; fila 2: prom/lote, ABV promedio, eficiencia promedio, costo total), datos de lotes con autofilter — **18 columnas** incluyendo al final: `Método Carb.` (Natural/Forzada), `CO₂ Obj. (vol)`, `CO₂ Real (vol)`, `Destino / Empaque`; hoja 2 con producción agrupada por estilo. Filas alternas con fondo crema.
- `generarExcelFacturas(facturas, estadoFiltro, desde, hasta, ExportBranding)` → `byte[]` — genera `.xlsx` de facturas. **3 hojas**: Hoja 1 "Facturas": título, fila de filtros activos (estado + período), fila de resumen (count, subtotal, IVA, total general), **12 columnas** con autofilter (N° factura, proveedor, fecha, estado, ítems, subtotal, IVA, envío, total, **IVA incluido**, descripción, creado por). Hoja 2 "Por Proveedor": resumen agrupado por nombre de proveedor (count de facturas + total comprado). Hoja 3 **"Ítems"**: detalle de todas las líneas de factura exportadas — **13 columnas** (N° Factura, Proveedor, Fecha, Tipo, Nombre, Cantidad, Unidad, V. Unitario, Desc.%, IVA%, Valor IVA, **Imp. Consumo**, Total Línea) con autofilter. Filas alternas con fondo crema. Inyectado también en `FacturaProveedorController`.
- `generarExcelInventario(insumos, ExportBranding)` → `byte[]` — genera `.xlsx` de inventario. Hoja 1 "Inventario": 8 columnas (Nombre, Tipo, Cantidad, Unidad, Stock Mínimo, Estado, Vencimiento, Proveedor), autofilter, filas alternas crema. Hoja 2 "Por Tipo": resumen agrupado por tipo de insumo String (count, bajo stock, % bajo stock). Inyectado en `InsumoInventarioController`.
- `generarExcelVentas(ventas, estadoFiltro, desde, hasta, ExportBranding)` → `byte[]` — genera `.xlsx` de ventas. **4 hojas**: Hoja 1 "Ventas": 7 columnas (Cliente, Primer Lote, Fecha Despacho, Estado, Valor Total, Notas, Creado por), usa `v.getPrimerCodigoLote()` y `v.getValorTotal()` (@Formula — no N+1). Hoja 2 "Ítems": 11 columnas por fila (Venta ID, Cliente, Fecha Despacho, Estado Venta, Lote, Descripción, Cantidad, Unidad, Precio Unit., Desc.%, Total Línea) — itera `v.getItems()` por cada venta (`open-in-view=true`). Hoja 3 "Por Cliente": agrupado por cliente (count ventas + total ingresos). Hoja 4 "Por Estado": agrupado por estado (count + total). Descargado por `GET /ventas/export`. Inyectado en `VentaController`.
- Solo importa `org.apache.poi.*` — sin colisión con OpenPDF. Usa `XSSFFont` con cast `(XSSFFont) wb.createFont()`.
- Inyectado en `ReporteController` y `VentaController`.

### MigracionTemplateService
- `plantillaAlmacen()` → `byte[]` — genera `plantilla-almacen.xlsx` (1 hoja: Insumos + hoja Instrucciones). Columnas: nombre*, tipo* (dropdown de enum-names uppercase: MALTA/LUPULO/LEVADURA/CLARIFICANTE/AGENTE_CARBONATACION/AGUA/QUIMICO/ENVASE/OTRO — `MigracionService` convierte al display name antes de insertar), cantidad, unidad (dropdown: gr/kg/mL/L/gal/und), stockMinimo, descripcion, proveedor.
- `plantillaEquipos()` → `byte[]` — genera `plantilla-equipos.xlsx` (1 hoja: Equipos + hoja Instrucciones). Columnas: nombre*, tipo* (dropdown de enum-names uppercase: FERMENTADOR/OLLA_MACERADO/... — `MigracionService` convierte al display name antes de insertar), descripcion, ubicacion, fechaAdquisicion, proximoMantenimiento, estado (dropdown EstadoEquipo, default OPERATIVO).
- `plantillaComercial()` → `byte[]` — genera `plantilla-comercial.xlsx` (3 hojas en orden: Proveedores, Facturas, Factura_Items + hoja Instrucciones). Relación: Facturas.proveedor → Proveedores.nombre; Factura_Items.numeroFactura → Facturas.numeroFactura.
- `plantillaProduccion()` → `byte[]` — genera `plantilla-produccion.xlsx` (6 hojas en orden: Recetas, Receta_Ingredientes, Receta_Escalones, Receta_Adiciones, Lotes, Lote_Ingredientes + hoja Instrucciones). Relaciones: Ingredientes/Escalones/Adiciones.receta → Recetas.nombre; Lotes.receta → Recetas.nombre (opcional); Lote_Ingredientes.codigoLote → Lotes.codigoLote.
- `plantillaClientes()` → `byte[]` — genera `plantilla-clientes.xlsx` (1 hoja: Clientes + hoja Instrucciones). Columnas: nombre*, razon_social, nit (idempotencia: skip si ya existe para el tenant), regimen_tributario (dropdown: SIMPLIFICADO/RESPONSABLE_IVA), email, telefono, direccion_despacho, ciudad, departamento, lista_precio (dropdown: VENTA_DIRECTA/DISTRIBUIDOR/BAR/MAYORISTA/EXPORTACION/EMPLEADO), activo (dropdown: TRUE/FALSE, default TRUE), notas.
- `plantillaVentas()` → `byte[]` — genera `plantilla-ventas.xlsx` (2 hojas: Ventas + Venta_Items + hoja Instrucciones). Hoja "Ventas": referencia_venta* (clave de cruce), cliente_nombre*, cliente_nit, fecha_despacho*, estado (dropdown: COTIZACION/PENDIENTE/DESPACHADO/CANCELADO; default DESPACHADO), notas, remision_numero. Hoja "Venta_Items": referencia_venta* (debe coincidir con Ventas), codigo_lote, descripcion, cantidad*, unidad (dropdown: und/L/mL/Botella 330ml/etc.), precio_unitario*, descuento_pct. La `referencia_venta` es la clave de cruce definida por el usuario (no autogenerada).
- **Estructura de cada hoja**: row 0 = cabeceras (verde oscuro=obligatorio, gris=opcional) con sufijo " *" en requeridas; row 1 = leyenda " * = obligatorio"; row 2 = fila de ejemplo en gris/italic; row 3+ = datos del usuario. El parser en `MigracionService` salta filas `rowNum < 3`.
- **Helpers privados**: `estilos(wb)` — record `Estilos(req, opt, example, data, instrTitle, instrBody)` con los 6 `XSSFCellStyle`; `cabecera(sh, estilos, cols[][])` — row 0 + row 1 legend; `ejemplo(sh, estilos, valores[])` — row 2; `fila(Row, estilo, valores[])` — rellena fila (`Cell` no `XSSFCell` porque `Row` es interfaz); `dropdown(sh, firstRow, lastRow, col, opciones...)` — `XSSFDataValidationHelper` lista explícita; `anchos(sh, chars...)` — anchos de columna; `hojaInstrucciones(wb, estilos, modulo, reglas[][])` — hoja primera con tabla de reglas.
- **CRÍTICO**: `Row.createCell()` devuelve `Cell` (interfaz), NO `XSSFCell` — declarar como `Cell` en todos los helpers que reciban `Row` como parámetro.

### MigracionService
- `importarAlmacen(archivo, tenantId, usuario)` → `Resultado` — lee hoja "Insumos", valida tipo (uppercase enum name via mapa estático `TIPO_INSUMO_DISPLAY` — incluye AGENTE_CARBONATACION; convierte al display name antes de insertar), inserta en `insumos_inventario` via `JdbcTemplate` con `tenant_id` explícito. Idempotente: salta duplicados si `LOWER(nombre) + tenant_id` ya existe.
- `importarEquipos(archivo, tenantId, usuario)` → `Resultado` — lee hoja "Equipos", defaults `estado` a `"OPERATIVO"`, inserta en `equipos`.
- `importarComercial(archivo, tenantId, usuario)` → `Resultado` — 3 hojas en orden:
  1. "Proveedores" → inserta en `proveedores`, salta duplicados por nombre
  2. "Facturas" → inserta en `facturas_proveedor`, resuelve `proveedor_id` por nombre, construye `Map<String, Long> facturaIds`
  3. "Factura_Items" → inserta en `factura_items` usando `facturaIds`, recalcula `subtotal`/`valor_total` de la factura; **`tipo_insumo`/`tipo_equipo` se convierten de enum-name a display name** vía `TIPO_INSUMO_DISPLAY`/`TIPO_EQUIPO_DISPLAY` antes de insertar (V47 — almacenan display names)
- `importarClientes(archivo, tenantId, usuario)` → `Resultado` — lee hoja "Clientes". Valida enum `regimen_tributario` (SIMPLIFICADO/RESPONSABLE_IVA) y `lista_precio` (VENTA_DIRECTA/DISTRIBUIDOR/BAR/MAYORISTA/EXPORTACION/EMPLEADO). **Idempotencia por NIT**: cuando el campo `nit` no está en blanco, salta el insert si ya existe `(nit, tenant_id)`. Permite nombres duplicados cuando NIT es null (sin constraint de unicidad en `nombre`). Almacena enum names directamente en BD (no convierte a display name — `clientes.regimen_tributario` y `lista_precio` son `@Enumerated(EnumType.STRING)`). Helper `textoODefault(row, col, default)` para campo `activo` (default "TRUE"). Campos de auditoría `created_at/created_by/last_modified_at/last_modified_by` se poblan con `NOW()`/`usuario`.
- `importarVentas(archivo, tenantId, usuario)` → `Resultado` — 2 hojas en orden:
  1. "Ventas" → inserta en `ventas`, resuelve `cliente_id` por NIT primero, nombre como fallback; construye `Map<String, Long> ventaIds` keyed por `referencia_venta`.
  2. "Venta_Items" → inserta en `venta_items` usando `ventaIds`; resuelve `lote_id` por `codigo_lote` (tolerante: deja null si no existe — útil para datos históricos donde el lote fue eliminado). La columna `deleted_at IS NULL` se respeta en la búsqueda de lotes.
  - **Sin idempotencia**: ventas no tienen clave de negocio natural única; diseñado para importación única de histórico.
  - `referencia_venta` es la clave de cruce definida por el usuario — debe ser única dentro del archivo.
  - `estado` default: DESPACHADO. Valores válidos: COTIZACION, PENDIENTE, DESPACHADO, CANCELADO (no EXPIRADO — es auto-generado por el scheduler).
- `importarProduccion(archivo, tenantId, usuario)` → `Resultado` — 6 hojas en orden:
  1. "Recetas" → inserta en `recetas`, construye `Map<String, Long> recetaIds`
  2. "Receta_Ingredientes" → inserta en `receta_ingredientes` (cantidad como String "5000 gr")
  3. "Receta_Escalones" → inserta en `escalones_macerado` (columnas `temperatura_c`, `duracion_minutos`)
  4. "Receta_Adiciones" → inserta en `adiciones_hervor`
  5. "Lotes" → inserta en `lotes_cerveza`, resuelve receta por nombre, construye `Map<String, Long> loteIds`
  6. "Lote_Ingredientes" → inserta en `ingredientes`
- `historial(tenantId)` → `List<MigracionLog>` — delega a `logRepo.findByTenantIdOrderByFechaDesc`
- `Resultado` record: `(int procesadas, int exitosas, int errores, List<String> mensajes, String estado)`. `estado` = "EXITOSO" / "PARCIAL" / "FALLIDO" según si hubo 0, algunos o todos los errores.
- **JdbcTemplate cross-tenant**: usa `JdbcTemplate` (no JPA) con `tenant_id` como parámetro explícito — igual que `TenantAdminController`. Esto bypasea el filtro `@TenantId` de Hibernate que aplicaría el tenant del request activo (super-admin), no el del destinatario.
- **`insertarYRetornarId`**: helper con `KeyHolder + GeneratedKeyHolder`, usa `conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)` para capturar el ID generado en inserts con hijos.
- **Parsing helpers**: `texto(row, col)`, `decimal(row, col)`, `entero(row, col)`, `fecha(row, col)`, `textoODefault(row, col, default)` — manejan tanto celdas NUMERIC como STRING. `fecha()` detecta `DateUtil.isCellDateFormatted()` para celdas de fecha nativas Excel.
- **`TIPO_EQUIPO_DISPLAY`**: usa `Map.ofEntries()` con 11 entradas (FERMENTADOR…COMPRESOR + OTRO). **Importante**: `Map.of()` solo acepta 10 pares; el mapa de equipos tiene 11 entradas y requiere `Map.ofEntries()` para incluir "OTRO".
- **Tolerancia a errores**: errores por fila se capturan y acumulan; el procesamiento continúa con la siguiente fila. Al final se guarda `MigracionLog` con el resumen.
- Inyecta `JdbcTemplate jdbc` y `MigracionLogRepository logRepo`.

### EmailService
- `mailConfigurado()` → boolean — true si `JavaMailSender` fue auto-configurado (requiere `spring.mail.host` no vacío)
- `enviarAlertasDiarias(Tenant, bajoStock, proximosAVencer, mantenimientoPendiente)` → boolean — usa `SpringTemplateEngine` para renderizar `emails/alertas.html`, envía con `JavaMailSender`. Retorna false si: SMTP no configurado, `tenant.emailAdmin` vacío, o no hay alertas. Loggea error sin propagar excepción.
- `enviarBienvenida(Tenant, username, password)` → boolean — envía email de bienvenida al crear el primer usuario de un tenant. Renderiza `emails/bienvenida.html` con variables `tenant`, `username`, `password`, `appUrl`. Retorna false si: SMTP no configurado o `tenant.emailAdmin` vacío.
- `enviarEmailPrueba(String destinatario, String tenantName)` → String — envía un email de prueba al destinatario indicado. Retorna null si se envió correctamente, o el mensaje de error si falló.
- `diasHasta(LocalDate)` → long — método estático auxiliar usado en el template Thymeleaf vía `T(com.alera.service.EmailService).diasHasta(...)`
- Usa `@Autowired(required = false)` para `JavaMailSender` — la app arranca sin SMTP configurado
- Variables de entorno: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`, `APP_BASE_URL`

### AlertaScheduler (`@Component`)
- `@EventListener(ApplicationReadyEvent.class)` → `inicializarNotificaciones()` — ejecuta `enviarAlertasDiarias()` una vez al arrancar la app. La deduplicación diaria (`existeEnPeriodo`) evita duplicados si el cron ya corrió hoy. Garantiza que las notificaciones in-app existan desde el primer request, sin esperar las 8 AM.
- `@Scheduled(cron = "${app.alert.cron:0 0 8 * * MON-FRI}")` — lunes a viernes a las 8 AM por defecto. Configurable con `ALERT_CRON` env var.
- Itera **todos** los tenants activos (ya no filtra por `emailAdmin` — notificaciones in-app funcionan sin SMTP). Para cada uno: establece `TenantContext`, carga alertas, llama `NotificacionService.crearAlertas()` siempre, luego envía email solo si SMTP configurado y tenant tiene email. Limpia contexto en `finally`.
- **Notificaciones in-app**: se crean independientemente de SMTP — la app no necesita email configurado para generar notificaciones en la UI.
- **Facturas sin procesar**: tras las alertas de inventario/equipos, llama `facturaService.listarSinProcesar(facturaAlertaDias)` y pasa la lista a `notificacionService.crearAlertaFacturas()`. Configurable via `app.facturas.alerta-dias` (def: 30). Si hay facturas RECIBIDA/VERIFICADA con más de ese número de días, se crea una notificación SISTEMA deduplicada por día.
- **Tracking de fallos**: solo aplica al canal email. Si `enviarAlertasDiarias()` lanza excepción, llama `TenantService.registrarEnvioFallido()`. Si exitoso, `registrarEnvioExitoso()`. Las notifs in-app no afectan el tracking.
- **WARN escalado**: si `alertasIntentosFallidos >= UMBRAL_WARN (3)`, loggea WARN antes de cada intento de email.
- **EmailService**: `enviarAlertasDiarias()` relanza excepción SMTP como `RuntimeException` para que el scheduler pueda trackearla.
- **Expiración de cotizaciones**: por cada tenant activo, después de procesar alertas de inventario/equipos/facturas, llama `ventaService.expirarCotizaciones()`. Las cotizaciones con `cotizacion_expira_en < today` y `estado = COTIZACION` pasan automáticamente a EXPIRADO.
- Loggea resumen: "N notificación(es) in-app creada(s), M email(s) enviado(s) de K tenant(s)"
- Inyecta `NotificacionService`, `FacturaProveedorService` y `VentaService`.

### NotificacionService
- `crear(tipo, titulo, mensaje, urlAccion)` — persiste una `Notificacion` para el tenant activo
- `crearAlertas(bajoStock, proximosAVencer, mantenimiento)` — crea una notificación por cada tipo de alerta que tenga elementos, con deduplicación diaria via `existeEnPeriodo()`. Retorna cantidad de notificaciones creadas. Mensajes: resume los primeros 3 elementos + "y N más." si hay más.
  - `BAJO_STOCK` → `urlAccion="/inventario"`, `VENCIMIENTO` → `"/inventario"`, `MANTENIMIENTO` → `"/equipos"`
- `listarRecientes()` — top 5 no leídas, orden `createdAt DESC`
- `contarNoLeidas()` — `countByLeidaFalse()`, usado por el badge del navbar
- `listarTodas(page)` — `findAllOrdenadas(PageRequest)` — paginado, no leídas primero
- `marcarLeida(id)` — busca por id y setea `leida = true`
- `marcarTodasLeidas()` — bulk update via `repo.marcarTodasLeidas()`
- `crearAlertaFacturas(sinProcesar, dias)` — crea notificación `TipoNotificacion.SISTEMA` con deduplicación diaria (`existeEnPeriodo(SISTEMA, hoy, maniana)`). Mensaje resume los primeros 3 proveedores. URL de acción: `/facturas`. Solo crea si `!sinProcesar.isEmpty()` y no existe notificación SISTEMA del día.
- `pageSize` inyectado via `@Value("${app.page-size:15}")`

### PlanificacionService
- `listarProximas()` — `findProximas(LocalDate.now().minusDays(1))` — incluye elaboraciones de ayer en adelante (para no cortar las del día actual)
- `listarTodas()` — todas ordenadas por fecha ASC
- `buscarPorId(id)` — `Optional<ElaboracionPlanificada>` (lazy — no carga ingredientes de receta)
- `buscarConRecetaEIngredientes(id)` — `Optional<ElaboracionPlanificada>` con receta e ingredientes en eager — usar este método cuando se necesite acceder a los ingredientes de la receta fuera de transacción (ej: pre-llenar formulario de lote)
- `listarPorRango(desde, hasta)` — delega a `findByRangoFecha`, usado por el feed de eventos FullCalendar
- `guardar(plan, recetaId)` — vincula la receta si `recetaId != null`; si `nombreElaboracion` está vacío y hay receta, usa el nombre de la receta como fallback automático
- `cambiarEstado(id, nuevoEstado)` — busca el plan por ID y actualiza el estado
- `eliminar(id)` — `repo.deleteById(id)`

### ClienteService
- `listarActivos()` — `findAllByActivoTrueOrderByNombreAsc()` — para selects y dropdown en el formulario de venta
- `listarTodos()` — todos incluyendo inactivos
- `listarPaginado(nombre, activo, page)` — paginado con filtros opcionales (null=todos)
- `buscarPorId(id)` — `Optional<Cliente>`
- `guardar(dto)` — valida unicidad de NIT antes de crear; lanza `RuntimeException` si ya existe otro cliente con el mismo NIT
- `actualizar(id, dto)` — valida unicidad de NIT excluyendo el propio registro
- `toggleActivo(id)` — invierte `activo` y guarda
- `suggest(q)` — mínimo 1 char; delega a `repo.searchActivos(q, PageRequest.of(0, 8))`, retorna hasta 6 mapas `{id, nombre, nit, listaPrecio, ciudad}`. Usado por `GET /clientes/suggest` para el typeahead del formulario de venta.
- `mapearDto(dto, cliente)` — helper privado: normaliza blancos a null con `blank(s)`, copia todos los campos incluyendo enums nullable

### VentaService
- `listarPaginado(estado, desde, hasta, page)` — paginado con filtros opcionales; todos los parámetros son nullable. Delega a `ventaRepo.findAllFiltered` con patrón `:param IS NULL OR` — sin valores centinela de fecha.
- `buscarPorId(id)` — `Optional<Venta>`
- `listarPorLote(loteId)` — `List<Venta>` ordenadas por `fechaDespacho DESC`; delega a `ventaItemRepo.findVentasByLoteId()`. Usado por `TrazabilidadController.ver()`.
- `listarHistorial(ventaId)` — `@Transactional(readOnly=true)`, delega a `historialRepo.findByVentaIdOrderByFechaDesc`. Usado por `VentaController.ver()`.
- `guardar(dto)` — `mapearDto()` crea los `VentaItem` y los añade a `venta.getItems()`. Registra `VentaHistorialEstado` con `estadoAnterior=null` (creación inicial).
- `actualizar(id, dto)` — llama `v.getItems().clear()` + `mapearDto()` para reemplazar ítems. Registra historial y crea notificación in-app SOLO si el estado cambió.
- `eliminar(id)` — **soft delete**: setea `deletedAt = LocalDateTime.now()` y guarda. No llama `deleteById`. No-op si no existe.
- `cambiarEstado(id, EstadoVenta)` — valida transición via `TRANSICIONES_VALIDAS`; lanza `RuntimeException` si es inválida. Al despachar: `generarRemisionNumero()` + `descontarEnvases()` + `crearNotificacionDespacho()`.
- `expirarCotizaciones()` — llama `ventaRepo.findCotizacionesVencidas(LocalDate.now())` y transiciona cada venta a EXPIRADO; registra historial y crea notificación SISTEMA. Llamado por `AlertaScheduler` en el loop per-tenant. `@Transactional` — el filtro `@TenantId` aplica automáticamente.
- `validarCantidadDisponible(List<VentaItemFormDto> items, Long excludeVentaId)` — `@Transactional(readOnly=true)`. Itera los ítems del DTO; para cada uno con `loteId` no null llama `validarItemCantidad()`. Retorna mensaje concatenado de advertencias o null si todo OK. No bloquea — solo informa.
- `validarItemCantidad(loteId, cantidad, unidad, excludeVentaId)` — (privado) primero verifica mezcla de unidades via `findUnidadesActivasByLote`; si el lote ya tiene ventas en una unidad distinta, retorna advertencia. Luego parsea `carbDestino` con `parseDestino()`: si hay entradas, busca la que coincide con `unidad` y valida solo contra esa entrada usando `sumCantidadActivaByLoteAndUnidad` — vender Botella 330ml no consume el cupo de Barril 20L. Si no hay entrada coincidente o `carbDestino` es null, cae al comportamiento anterior (litrosFinales).
- `generarRemisionNumero(venta)` — (privado) si la venta aún no tiene remisionNumero: `em.flush()` luego native `MAX(CAST(SUBSTRING(remision_numero FROM 5) AS INTEGER))` filtrando por tenant. Formatea `REM-%03d`. Thread-safe por el flush previo.
- `descontarEnvases(venta)` — (privado) busca ítems de packaging via `findItemsConEnvase()` y descuenta stock via `InsumoInventarioService.descontarIngrediente()`. Fallo de stock solo registra WARN (no bloquea).
- `suggestClientes(q)` — `@Transactional(readOnly=true)`, mínimo 1 char; delega a `clienteRepo.searchActivos()`. Retorna `List<String>` con nombres de clientes activos. Usado por `GET /ventas/suggest-clientes`.
- `suggestLotesParaVenta(q)` — `@Transactional(readOnly=true)`. Sin filtro: carga hasta 50 lotes completados y devuelve los 20 con disponibilidad > 0. Con query: hasta 20 candidatos, devuelve 6. Calcula disponibilidad con `parseDestino(carbDestino)`: si hay entradas, suma todas las cantidades como capacidad total; si no hay entradas, usa `litrosFinales`. `unidadDisponible` = formato si es entrada única, `"uds"` si son múltiples entradas. Retorna `[{id, codigoLote, estilo, carbDestino, litrosFinales, litrosDisponibles}]`. Usado por `GET /ventas/suggest-lotes`.
- `parseDestino(carbDestino)` — (privado) parsea `carbDestino` separando por `" | "` y aplicando `DESTINO_PATTERN` (`^\d+(?:[.,]\d+)?\s*[×x]\s*(.+)$`) a cada parte. Retorna `List<DestinoEntry>` (record privado `{BigDecimal cantidad, String formato}`). Entradas sin patrón numérico (ej. "A granel") se omiten. Retorna lista vacía si `carbDestino` es null o blank.
- `topClientes()` — `@Transactional(readOnly=true)`, delega a `ventaRepo.findTopClientes(tenantId)`. Retorna `List<Map>` con top 5 clientes por ingresos despachados. Usado en `lista.html`.
- `suggest(q)` — `@Transactional(readOnly=true)`, query corta (< 2 chars) retorna lista vacía; busca via `ventaRepo.search()` (limit 6); retorna `[{titulo, sub, fecha, url}]`. `sub` usa `getPrimerCodigoLote()` (@Formula).
- `countTotal()`, `countByEstado(EstadoVenta)`, `countClientesUnicos()` — delegan a `ventaRepo`. `sumIngresosDespachados()` — delega a `ventaItemRepo.sumIngresosDespachados()` (no ventaRepo). Stats para las 4 stat-cards de la lista.
- `listarParaExport(estado, desde, hasta)` y `listarPorPeriodo(desde, hasta)` — `@Transactional(readOnly=true)`, usan `ventaRepo.findByPeriodo()` (nullable). `open-in-view=true` permite acceso lazy a `items` en los servicios de export.
- Inyecta `VentaRepository ventaRepo`, `VentaItemRepository ventaItemRepo`, `LoteCervezaRepository loteRepo`, `VentaHistorialEstadoRepository historialRepo`, `NotificacionService notificacionService`, `ClienteRepository clienteRepo`, `InsumoInventarioService insumoService`, `EntityManager em`.
- `pageSize` inyectado via `@Value("${app.page-size:15}")`. `expiracionDias` via `@Value("${app.cotizacion.expiracion-dias:15}")`.

### LecturaFermentacionService
- `listarPorLote(loteId)` — `@Transactional(readOnly=true)`, delega a `findByLoteIdOrdenadas` (orden fecha ASC, id ASC)
- `agregar(loteId, fecha, densidad, temperatura, notas)` — crea `LecturaFermentacion`, vincula al lote via `loteRepo.findById`. `densidad` y `temperatura` son opcionales (null permitido). `notas` se normaliza a null si está en blanco.
- `eliminar(lecturaId)` — `repo.deleteById(lecturaId)`

### EvaluacionSensorialService
- `listarPorLote(loteId)` — `@Transactional(readOnly=true)`, delega a `findByLoteIdOrdenadas` (orden fecha DESC, id DESC — más recientes primero)
- `agregar(loteId, fecha, catador, aroma, apariencia, sabor, sensacionBoca, impresionGeneral, notas)` — crea `EvaluacionSensorial`, vincula al lote via `loteRepo.findById`. Todos los puntajes son opcionales (null permitido). `catador` y `notas` se normalizan a null si están en blanco. Lanza `RuntimeException("Lote no encontrado: {id}")` si el lote no existe.
- `eliminar(evalId)` — `repo.deleteById(evalId)`
- `calcularPromedio(List<EvaluacionSensorial> evaluaciones)` — acepta lista ya cargada (evita segunda query). Filtra evaluaciones con `getPuntajeTotal() != null`, calcula promedio como `Double`. Retorna `0.0` si lista vacía o todas sin puntaje. **No hace query a BD** — trabaja sobre la lista pasada como parámetro.
- **Patrón de uso en controller**: `TrazabilidadController.ver()` carga la lista una sola vez y la pasa tanto para mostrar el historial (`"evaluaciones"`) como para calcular el promedio (`"promedioEvaluacion"`). `promedioEvaluacion` es `null` si la lista está vacía.

### BarrilService
- `listarPaginado(codigo, estado, page)` — pasa `""` cuando `codigo` es null (patrón `:codigo = '' OR LOWER(b.codigo) LIKE ...`; evita error `lower(bytea)` de PostgreSQL con null)
- `buscarPorId(id)` — lanza `RuntimeException("Barril no encontrado: {id}")` si no existe
- `listarMovimientos(barrilId)` — delega a `movimientoRepo.findByBarrilIdOrderByFechaDesc()`
- `guardar(barril)` — valida unicidad de código (case-insensitive), normaliza blancos a null, guarda, crea `MovimientoBarril` inicial con `estadoAnterior=null`. Estado por defecto: DISPONIBLE.
- `actualizar(id, barril)` — busca el existente, actualiza campos, solo verifica unicidad de código si cambió (`existsByCodigoIgnoreCaseAndIdNot`), guarda sin crear movimiento.
- `cambiarEstado(id, nuevoEstado, notas)` — actualiza `estado`; si `nuevoEstado` ∈ {DISPONIBLE, VACIO, LIMPIEZA, BAJA} limpia `loteId`, `codigoLote`, `clienteNombre`, `fechaDespacho`; guarda; crea `MovimientoBarril` con estado anterior y nuevo.
- `eliminar(id)` — **borrado físico** (no soft delete); lanza `RuntimeException` si no existe.
- `countTotal()`, `countByEstado(EstadoBarril)` — delegan a `barrilRepo`. Para stat-cards.
- Helpers privados: `normalizar(barril)` (blancos → null), `validarCodigoUnico(codigo, excludeId)`, `usuarioActual()` (SecurityContextHolder).
- `pageSize` inyectado via `@Value("${app.page-size:15}")`.

### OrdenCompraService
- `listarPaginado(estado, page)` — paginado con filtro opcional por `EstadoOrdenCompra`; orden `fechaEmision DESC NULLS LAST, id DESC`.
- `buscarPorId(id)` — lanza `RuntimeException("Orden no encontrada: {id}")` si no existe.
- `suggest(q)` — filtra por `numeroOc` o `proveedor`; retorna hasta 6 mapas `{titulo, proveedor, fecha, estado, url}`.
- `guardar(dto)` — genera `numeroOc` auto en formato `OC-001`; vincula `proveedorRef` si `proveedorId != null`; estado inicial siempre `BORRADOR`.
- `actualizar(id, dto)` — solo editable en estado BORRADOR; lanza `RuntimeException` si la OC no es editable.
- `cambiarEstado(id, EstadoOrdenCompra)` — valida transiciones via `TRANSICIONES_VALIDAS`: BORRADOR → {ENVIADA, CANCELADA}, ENVIADA → {RECIBIDA_PARCIAL, RECIBIDA, CANCELADA}, RECIBIDA_PARCIAL → {RECIBIDA, CANCELADA}, RECIBIDA → {}, CANCELADA → {}.
- `eliminar(id)` — solo en BORRADOR o CANCELADA; **borrado físico** (no soft delete).
- `convertirAFactura(id, facturaService)` — crea `FacturaProveedor` desde la OC via `facturaService.crearDesdeOrdenCompra(oc)`; vincula `factura_id` en la OC. Retorna el `id` de la nueva factura para redireccionar al editor.
- `transicionesValidas(estado)` — retorna `List<EstadoOrdenCompra>` de destinos válidos para el estado actual; usado en `detalle.html` para mostrar solo los botones de estado relevantes.
- `countTotal()`, `countByEstado(EstadoOrdenCompra)` — delegan a `repo`. Para stat-cards.
- `pageSize` inyectado via `@Value("${app.page-size:15}")`.

### TenantService
- `listarTodos()` — `@Transactional(readOnly=true)`, ordenados por subdomain
- `buscarPorSubdomain(subdomain)` — `Optional<Tenant>` por PK
- `guardar(tenant)` — `repo.save()` + `tenantFilter.evictCache(subdomain)` — invalida la caché en memoria de `TenantFilter` para que el siguiente request lea los datos actualizados de BD
- `evictAllCache()` — llama `tenantFilter.evictAll()` — limpia todo el cache de tenants. Usado por `POST /admin/tenants/cache/evict`.
- `toggleActivo(subdomain)` — invierte `active`, guarda, evicta cache y registra `ACTIVADO`/`DESACTIVADO` en historial.
- `guardar(tenant)` — detecta si es nuevo (`existsById`) antes de guardar para registrar `CREADO` o `EDITADO` en historial.
- `listarHistorial(subdomain)` — `@Transactional(readOnly=true)`, delega a `HistorialTenantRepository.findBySubdomainOrderByFechaDesc`.
- `registrarAccion(subdomain, accion, detalles)` — crea `HistorialTenant` con usuario de `SecurityContextHolder`. Llamado desde controller para acciones como `USUARIO_CREADO`, `USUARIO_ELIMINADO`, `CONFIG_IMPORTADA`.
- `usuarioActual()` — método privado que lee `Authentication.getName()` del `SecurityContextHolder`. Fallback: `"sistema"`.
- Inyecta `TenantFilter` y `HistorialTenantRepository`. El subdomain es la PK inmutable — no se puede cambiar una vez creado.

---

## CONTROLADORES Y ENDPOINTS

### TrazabilidadController ("/")
- `GET /` — filtros: estilo, fase, `?desde=`, `?hasta=` (rango de fechas), page
- `GET /kanban` — lotes agrupados en 6 columnas por fase
- `POST /actualizar/{id}/fase` — `@ResponseBody` JSON. Cubierto por regla `POST /actualizar/**` → solo ADMIN. Param: `fase` (String). Delega a `service.moverFase()`. Devuelve `{success:true}` o `{success:false, error:"..."}`. Usado por SortableJS en kanban.html vía fetch con CSRF header.
- **Kanban drag & drop**: SortableJS 1.15.2 (CDN). Cada `.kanban-col-body` tiene `data-fase`, cada `.kanban-card` tiene `data-lote-id`. `group:'kanban'` permite mover entre columnas. `disabled:!esAdmin` — no-ADMIN/SUPERADMIN solo visualiza. Al soltar: opacity 0.45 (saving), AJAX POST con CSRF, actualiza badges de conteo en cliente, revert DOM si falla. Toast propio (esquina inferior derecha, 2.8s) en verde/rojo. La columna Completados siempre visible (antes se ocultaba con `th:if` si estaba vacía — eliminado para permitir drop ahí). **JS en `static/js/trazabilidad-kanban.js`** — `kanban.html` solo inyecta `var esAdmin` via `th:inline="javascript"` (`hasAnyRole('ADMIN','SUPERADMIN')`); CSRF se lee lazily via `_csrfToken()`/`_csrfHeader()` del navbar (null-safe).
- `GET /suggest?q=` — `@ResponseBody`, `produces=JSON`. Busca lotes por codigoLote o estilo. Delega a `service.suggest(q)`. Devuelve `[{codigoLote, estilo, fase, completado, url}]`. Accesible todos los roles autenticados.
- `GET /duplicar/{id}` — limpia fechas de fase, `notasCata`, `observaciones`, `recetaId`, items de costo; también limpia **resultados** de carbonatación (`carbCo2Real`, `carbValidacion`, `carbDestino`) pero conserva los **parámetros de planificación** (`carbMetodo`, `carbCo2Objetivo`, `carbAzucarTipo`, `carbAzucarGramos`, `carbPresionPsi`, `carbTiempoHoras`, `carbTecnica`).
- `GET /ver/{id}` (+ historial), `GET /nuevo`, `POST /guardar`, `POST /actualizar/{id}` etc. **Validación cross-field**: si `fermFechaInicial` tiene valor pero `equipoFermentadorId` es null → `result.rejectValue("equipoFermentadorId", ...)` y devuelve el formulario (panel de fermentación con error). La misma regla se aplica en `guardar` y `actualizar`. El `formulario.html` muestra `invalid-feedback` bajo el select de fermentador al volver del server.
- **`POST /guardar` — primer registro automático en curva de fermentación**: tras crear el lote exitosamente, si `dto.getDensidadInicial() != null`, crea automáticamente la primera `LecturaFermentacion` via `lecturaService.agregar()` con: `fecha` = `fermFechaInicial` si existe, sino `fechaElaboracion`; `densidad` = `densidadInicial` (OG); `temperatura` = `fermTemperatura` (nullable); `notas` = null. Garantiza que la curva de fermentación siempre empiece desde el OG medido el día de elaboración.
- `GET /nuevo?planId={id}` (opcional) — si `planId` está presente, carga la `ElaboracionPlanificada` con receta e ingredientes (via `buscarConRecetaEIngredientes`), pre-llena el `LoteFormDto` con: `estilo` ← `nombreElaboracion`, `fechaElaboracion` ← `fechaPlaneada`, `litrosFinales` ← `volumenEstimado`, `recetaId` ← `receta.id`, `densidadInicial/Final` ← `ogObjetivo/fgObjetivo`, listas de ingredientes (maltas/lúpulos/levaduras/clarificantes) parseadas desde `RecetaIngrediente.cantidad` ("5000 gr" → `{cantidad:"5000", unidad:"gr"}`). Cambia el estado de la planificación a EN_PROCESO al abrir el formulario. Método privado `toInsumoDtoList(List<RecetaIngrediente>)` hace el parseo.
- Inyecta `PlanificacionService` (nuevo). El test agrega `@MockBean PlanificacionService`.
- `GET /ver/{id}/pdf` — descarga PDF del detalle de lote. Lee el tenant del `request.getAttribute("currentTenant")` para el nombre del branding en el encabezado. Devuelve `application/pdf` con `Content-Disposition: attachment`. Botón "PDF" en `detalle.html`.
- `POST /ver/{id}/lecturas/agregar` — `@PreAuthorize("hasRole('ADMIN')")`. Params: `fecha` (DATE ISO), `densidad` (Integer, opcional), `temperatura` (BigDecimal, opcional), `notas` (String, opcional). Delega a `LecturaFermentacionService.agregar()`.
- `POST /ver/{id}/lecturas/{lecturaId}/eliminar` — `@PreAuthorize("hasRole('ADMIN')")`. Elimina una lectura por ID.
- `POST /ver/{id}/evaluaciones/agregar` — `@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")`. Params: `fecha` (@DateTimeFormat ISO DATE), `catador` (opcional), `aroma`/`apariencia`/`sabor`/`sensacionBoca`/`impresionGeneral` (Integer, todos opcionales), `notas` (opcional). Delega a `EvaluacionSensorialService.agregar()`. Redirige a `/ver/{id}`.
- `POST /ver/{id}/evaluaciones/{evalId}/eliminar` — `@PreAuthorize("hasAnyRole('ADMIN','SUPERADMIN')")`. Elimina una evaluación por ID. Redirige a `/ver/{id}`.
- `GET /ver/{id}` pasa al modelo: `lecturas` (List ordenada por fecha ASC), `chartFechas` (List<String>), `chartDensidad` (List<Integer>), `chartTemp` (List<BigDecimal>) — arrays paralelos para Chart.js; `evaluaciones` (List<EvaluacionSensorial> ordenada por fecha DESC), `promedioEvaluacion` (Double — null si lista vacía). **JS en `static/js/trazabilidad-detalle.js`** — `detalle.html` inyecta `CHART_FECHAS`, `CHART_DENSIDAD`, `CHART_TEMP` via `th:inline="javascript"`; el archivo externo construye el gráfico leyendo esas variables globales.
- `agregarInventarioAlModelo()` — llama `insumoRepo.findAll()` una sola vez y filtra en memoria. Pasa al modelo: listas por tipo de insumo + `agentesCarbonatacion` (List<InsumoInventario> tipo AGENTE_CARBONATACION, para el select dinámico del Tab 5) + llama `facturaRepo.findAllWithItems()` para construir `todosItemsFactura` (List<Map<String,Object>>) para el buscador de costos
- `todosItemsFactura` — lista plana de todos los ítems de todas las facturas con campos: `{id, nombre, tipoInsumo, unidad, cantidad, valorLinea, facturaId, facturaNumero, proveedor, fechaFactura}`. Serializada como JSON via `<script th:inline="javascript">` en el formulario.
- `formulario.html` — sección Costos de Producción: buscador en tiempo real sobre `ITEMS_FACTURA` JS, tabla de ítems asignados con cantidad editable, botón "Aplicar a Receta e Insumos" (auto-llena ingredientes por tipo y navega al tab 2). **JS en `static/js/trazabilidad-costos.js`** (costos) y **`static/js/trazabilidad-ingredientes.js`** (wizard, volumen, ingredientes, receta). El bloque `th:inline="javascript"` del template solo inyecta `ITEMS_FACTURA`, `INIT_IDS`, `INIT_CANTIDADES`.

### RecetaController ("/recetas")
- `GET /recetas?activa=true|false&page=N` — lista paginada con filtro opcional por estado activa. Pasa `lotesCountMap` (Map<Long, Long>) al modelo — consulta bulk `countPorReceta()` para mostrar badge de lotes por receta sin N+1.
- `GET /recetas/suggest?q=&activa=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q, activa)`. El parámetro `activa` es opcional; si se omite busca en todas. Devuelve `[{nombre, estilo, activa, url}]`.
- CRUD completo + `GET /api/{id}` (@ResponseBody JSON) — incluye `phAgua` en la respuesta JSON cuando no es null
- `GET /ver/{id}` — incluye `lotesDeReceta` (lotes elaborados con esa receta) y `costosIngredientes` (List<Map>) con precio estimado por ingrediente desde `FacturaItemRepository.findUltimosPrecios()`. Si algún ingrediente tiene precio, agrega `totalCostoEstimado` (BigDecimal). El header muestra el badge de versión (`v1`, `v2`, etc.) y botones "Duplicar" y "PDF".
- `GET /ver/{id}/pdf` — descarga `receta-{nombre}.pdf`. Lee el tenant del `request.getAttribute("currentTenant")`, construye `ExportBranding.from(tenant)`. Delega a `PdfExportService.generarPdfReceta(receta, branding)`. Botón "PDF" en `detalle.html`.
- `GET /duplicar/{id}` — delega a `service.duplicarComoFormDto(id)`, inyecta `insumosInventario` y `tiposCerveza`, retorna `recetas/formulario`. El submit crea una receta nueva (no edita la original). La copia siempre empieza en version 1.
- `GET /nueva` y `GET /editar/{id}` — inyectan al modelo:
  - `insumosInventario` (List<InsumoInventario>) para datalists de ingredientes por tipo
  - `tiposCerveza` (List<TipoCerveza> activos) para datalist del campo Estilo
- `calcularCostosEstimados(Receta, Model)` — método privado: recopila nombres de ingredientes, llama `facturaItemRepo.findUltimosPrecios(nombres)`, toma el más reciente por nombre, normaliza unidades con `UnidadUtils` y calcula el costo estimado de cada ingrediente. Normalización: convierte cantidad de la receta y precio unitario de la factura a la misma unidad base (gr o mL).
- `estimarCosto(cantidadTexto, valorUnitario, unidadFactura)` — método privado: parsea "5000 gr" → BigDecimal+unidad, convierte a base via `UnidadUtils`, calcula precio por unidad base y multiplica. Si las bases son incompatibles (ej: peso vs volumen) aplica valorUnitario directo.
- Inyecta `InsumoInventarioService`, `TipoCervezaService`, `LoteCervezaRepository`, `FacturaItemRepository`, `PdfExportService`
- **`@WebMvcTest`**: agregar `@MockBean FacturaItemRepository facturaItemRepo` y `@MockBean PdfExportService pdfExportService`

### InsumoInventarioController ("/inventario")
- CRUD estándar
- `GET /inventario?filtroBajoStock=true` — activa el filtro "Bajo Stock": llama `service.listarBajoStock()`, devuelve lista completa sin paginar (totalPaginas=1). `?filtroPorVencer=true` — activa el filtro "Por Vencer": llama `service.listarProximosAVencer(30)`. Sin filtro especial: paginación normal. Los botones "Todos / Bajo Stock / Por Vencer" en `inventario/lista.html` aplican el filtro y muestran un badge con el conteo. La paginación y el botón "Excel" preservan el filtro activo via query params.
- `GET /inventario/suggest?nombre=&tipo=` — `@ResponseBody`, `produces=JSON`. Delega a `service.listarPaginado(nombre, tipo, 0)` (limit 6). El parámetro `tipo` es opcional (`String` (display name)). Devuelve `[{id, nombre, tipoNombre, colorTipo, bajoStock, url}]`. La template pasa el tipo seleccionado via `data-activa` para respetar el filtro activo.
- `POST /inventario/guardar-rapido` — `@ResponseBody` JSON. Crea insumo con stock 0 sin redirigir. Devuelve `{success, id, nombre}`. Accesible: ADMIN, INVENTARIO. Usado desde formularios de receta y factura vía AJAX + CSRF header.
- `GET /inventario/export?nombre=&tipo=&filtroBajoStock=&filtroPorVencer=` — descarga `inventario-YYYY-MM-DD.xlsx`. Respeta todos los filtros de la lista (incluyendo `filtroBajoStock` y `filtroPorVencer`). Sin filtros exporta todo (via `listarTodos()`). Lee branding del request. Delega a `ExcelExportService.generarExcelInventario()`.
- `POST /inventario/{id}/ajuste` — ajuste rápido de stock. `@RequestParam TipoMovimiento tipo, BigDecimal cantidad, String motivo`. Delega a `service.ajustar()`. Flash success/danger. Solo ADMIN/INVENTARIO (hereda de `/inventario/**`).
- `GET /inventario/{id}/historial?page=` — historial de movimientos del insumo. Paginado. Template `inventario/historial.html`. Modelo: `insumo`, `movimientos`, `paginaActual`, `totalPaginas`.
- `GET /inventario/precios?nombre=X` — **Historial de precios** para el insumo con nombre X. Busca en `FacturaItem` por nombre (case-insensitive) via `findHistorialPreciosPorNombre`. Calcula: último precio, promedio, mínimo, máximo, variación (último vs primero), N compras, N proveedores. Pasa arrays `chartFechas`, `chartPrecios`, `chartProveedores` para Chart.js (barras). La fila más reciente se resalta en la tabla. Botón 📈 en `inventario/lista.html` abre directamente con el nombre del insumo. **Nota**: usa `fi.getFactura().getFechaFactura()` (no `getFecha()`) — campo correcto en `FacturaProveedor`.
- Inyecta `ExcelExportService` y `ProveedorService` vía constructor. `nuevo()` y `editar()` pasan `proveedores` (List<Proveedor> activos) al modelo para el `<select>` del campo Proveedor (ya no es input libre).
- **`@WebMvcTest`**: agregar `@MockBean ProveedorService proveedorService` y stubear `proveedorService.listarActivos()` en `@BeforeEach`.

### TipoCervezaController ("/tipos-cerveza") — solo ADMIN
- CRUD + toggle activo
- `POST /tipos-cerveza/guardar-rapido` — `@ResponseBody` JSON. Crea tipo de cerveza si no existe (valida con `existePorNombre`). Devuelve `{success, id, nombre}`. Usado desde formulario de receta vía AJAX.

### CategoriaController ("/admin/categorias") — solo ADMIN/SUPERADMIN (hereda de `/admin/**`)
- `GET /admin/categorias` — página con dos tabs: Tipos de Insumo y Tipos de Equipo. Modelo: `categoriasInsumo` (List<CategoriaInsumo>), `categoriasEquipo` (List<CategoriaEquipo>).
- `POST /admin/categorias/insumo/guardar` — crea categoría de insumo. Flash success o danger si nombre duplicado.
- `POST /admin/categorias/insumo/{id}/toggle` — activa/desactiva categoría de insumo.
- `POST /admin/categorias/insumo/{id}/eliminar` — elimina categoría de insumo.
- `POST /admin/categorias/insumo/guardar-rapido` — `@ResponseBody` JSON `{success, id, nombre}` — creación inline desde formularios de inventario y facturas.
- Mismos 4 endpoints para equipo: `/admin/categorias/equipo/...`

### FacturaProveedorController ("/facturas")
- CRUD + `GET /ver/{id}`
- `GET /facturas?estado=RECIBIDA|VERIFICADA|PAGADA&desde=yyyy-MM-dd&hasta=yyyy-MM-dd` — filtros opcionales por estado y rango de fechas. Pasa `estadoFiltro`, `desde`, `hasta`, `estados` (enum values) y `extraParams` al modelo para que paginación, tabs y Excel respeten todos los filtros activos. El card principal permanece visible cuando cualquier filtro está activo (permite limpiar incluso sin resultados).
- `POST /facturas/{id}/estado` — cambia el estado de la factura. `@RequestParam EstadoFactura estado`. Si el servicio lanza `RuntimeException`, devuelve flash danger; en caso exitoso, flash success. Redirige a `/facturas/ver/{id}`.
- `GET /facturas/export` — descarga `facturas-YYYY-MM-DD.xlsx`. Acepta filtros opcionales `?estado=`, `?desde=` (ISO date), `?hasta=` (ISO date). Lee el branding del tenant del `request.getAttribute("currentTenant")`. Delega a `ExcelExportService.generarExcelFacturas()`. El botón "Excel" en `lista.html` respeta todos los filtros activos.
- `GET /facturas/duplicar/{id}` — pre-llena el formulario de nueva factura con los datos de la factura original (mismo proveedor, ítems, descripción, envío) pero sin número ni fecha, y estado RECIBIDA. Usa `service.duplicarComoFormDto(id)`. No pasa `facturaId` al modelo — el submit va a `POST /facturas/guardar` (crea nueva, no edita).
- `GET /facturas/suggest?q=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q)`. Busca por N° factura o proveedor. Devuelve `[{titulo, proveedor, fecha, total, url}]`.
- `agregarDatosFormulario()` construye:
  - `tiposInsumo` — `List<String>` desde `categoriaInsumoService.listarNombresActivos()` — display names activos del tenant.
  - `tiposEquipo` — `List<String>` desde `categoriaEquipoService.listarNombresActivos()`.
  - `insumosPorTipo` — `Map<String, List<String>>` con display name como clave ("Malta", "Lúpulo"…) y lista de nombres de insumos que coinciden. El select de categoría del formulario usa el display name como `value` — clave y valor ya son el mismo string. `INSUMOS_POR_TIPO["Malta"]` en JS retorna la lista correcta.
  - `equiposPorTipo` — ídem con display names de equipo.
  - `estados` — `EstadoFactura.values()` para el select en el formulario de edición y las tabs de la lista.
- `lista()` pasa al modelo `statsTotal` (monto total filtrado), `statsPendiente` (monto RECIBIDA+VERIFICADA), `statsCountPend` (cantidad pendiente) — usados por las 4 stat-cards en `lista.html`
- `POST /facturas/guardar-insumo-rapido` — `@ResponseBody` JSON. Crea insumo con stock 0. Accesible: ADMIN, FACTURACION.
- `POST /facturas/guardar-equipo-rapido` — `@ResponseBody` JSON. Crea equipo en estado OPERATIVO. Accesible: ADMIN, FACTURACION.
- Inyecta `InsumoInventarioService`, `EquipoService` y `ExcelExportService`

### AuthController ("/api/auth") — público, produce JSON
- `POST /api/auth/login` — body `{username, password}`. Autentica con Spring `AuthenticationManager`. Retorna `{token, tipo:"Bearer", expiresIn, username, rol}`. El tenant se resuelve del `Host` header (ya establecido por `TenantFilter`). En caso de credenciales inválidas: HTTP 401 `{error:"Credenciales inválidas"}`. Body vacío/inválido: HTTP 400.
- Documentado en Swagger UI con esquema `bearerAuth`.
- CSRF deshabilitado para `/api/**` — clientes REST usan el token, no cookies de sesión.

### ApiController ("/api/v1") — REST JSON con Swagger
- `GET /api/v1/lotes` + `GET /api/v1/lotes/{id}` + `GET /api/v1/lotes/{id}/historial`
  - `GET /api/v1/lotes/{id}` incluye un bloque `"carbonatacion"` con los 10 campos avanzados: `{metodo, co2Objetivo, co2Real, azucarTipo, azucarGramos, presionPsi, tiempoHoras, tecnica, validacion, destino}` — campos null cuando no están registrados.
- `GET /api/v1/recetas` + `GET /api/v1/recetas/{id}`
- `GET /api/v1/inventario/alertas` + `GET /api/v1/dashboard`
- Autenticación: HTTP Basic, sesión, **o Bearer JWT** (obtenido de `POST /api/auth/login`)
- Anotado con `@Tag` y `@Operation` (SpringDoc) — documentado en `/swagger-ui.html`
- Lanza `LoteNoEncontradoException` → GlobalExceptionHandler devuelve HTTP 404
- **`produces = MediaType.APPLICATION_JSON_VALUE` a nivel de clase** — CRÍTICO: sin esto, un navegador que accede directamente con `Accept: text/html` hace que Spring negocie HTML, no puede serializar el `LinkedHashMap` devuelto y lanza `HttpMessageNotWritableException`. Con `produces`, el navegador recibe 406 en lugar de una excepción descontrolada.

### ReporteController ("/reportes")
- `GET /reportes/produccion?desde=&hasta=&estilo=` — reporte de producción. Sin parámetros muestra **todo el historial** (sin restricción de fecha — `desde`/`hasta` null = sin filtro). 8 stat-cards (Total Lotes, Litros, Prom/Lote, ABV Prom, Eficiencia Prom, Costo Total, Estilos Únicos, Completados%). Filtro opcional por estilo (`<select>` dinámico con los estilos del período). Atajos de período: Este mes, Último mes, 3 meses (pasa fecha explícita), Este año. 3 gráficos Chart.js (tendencia mensual de litros, litros por estilo, distribución ABV). Tabla con paginación client-side (15 filas/página) con columnas Eficiencia (color-coded: verde ≥75%, `var(--dorado)` ≥60%, rojo <60%) y Costo/L. Los 3 gráficos y todas las estadísticas se calculan en Java desde la lista `lotes` (ya filtrada por estilo) — no usan queries nativas adicionales.
- **`produccion.html` — colores tenant**: código de lote y ABV usan `color:var(--dorado)` (antes `var(--verde-zymos)` que no existe). Los 3 gráficos Chart.js se crean dentro de `DOMContentLoaded`; `DORADO`, `DORADO_CL` y `VERDE` se leen de `--dorado`, `--dorado-claro` y `--verde-alera` vía `getComputedStyle`. Helper `hexToRgba(hex, alpha)` convierte el dorado dinámico a rgba para el área del gráfico de línea. PDF y Excel usan `ExportBranding.from(tenant)` — ya respetan el branding del tenant sin cambios adicionales.
- `GET /reportes/produccion/excel?desde=&hasta=&estilo=` — descarga `.xlsx` con dos hojas: "Reporte de Producción" (2 filas de resumen + 14 columnas de datos: código, estilo, receta, fecha, fase, OG, FG, ABV, atenuación, eficiencia, litros, costo total, costo/litro, creado por) y "Por Estilo" (estilo, cantidad, litros). Acepta filtro `estilo` opcional. Botón "Excel" en `produccion.html`.
- `GET /reportes/produccion/pdf?desde=&hasta=&estilo=` — descarga PDF landscape A4. Delega a `PdfExportService.generarPdfReporteProduccion(lotes, desde, hasta, estiloFiltro, branding)`. Botón "PDF" en `produccion.html`.
- `findResumenPorEstilo` se llama solo en el endpoint `/excel` (para la hoja "Por Estilo") — pasa `TenantContext.getCurrentTenant()` como parámetro explícito (nativeQuery no filtra automáticamente). El endpoint `/produccion` ya no llama queries nativas — todo se calcula desde `loteRepo.findByPeriodo(desde, hasta)`.

### DashboardController ("/dashboard")
- `GET /dashboard` — inyecta estadísticas del tenant al modelo. Atributos: todos los campos de `DashboardStats` como atributos individuales + `chartLitrosMes`, `chartEstilos`, `alertasBajoStock`, `alertasProxVencer`, `proximasElaboraciones`.
- `proximasElaboraciones` — lista de hasta 5 `ElaboracionPlanificada` desde ayer en adelante, via `PlanificacionService.listarProximas()` con `subList(0, 5)` si hay más de 5.
- Inyecta `DashboardService`, `InsumoInventarioService`, `PlanificacionService`.
- **`@WebMvcTest`**: agregar `@MockBean PlanificacionService planificacionService` y stubear `planificacionService.listarProximas()` → `List.of()` en `@BeforeEach`.

### CalendarioController ("/calendario")
- `GET /calendario` — template con FullCalendar
- `GET /calendario/eventos` — @ResponseBody JSON de eventos para FullCalendar

### BusquedaController ("/buscar")
- `GET /buscar?q=` — búsqueda global (lotes + recetas + insumos + proveedores + equipos), renderiza `busqueda.html`
- `GET /buscar/suggest?q=` — `@ResponseBody`, `produces=JSON`. Devuelve `{lotes, recetas, insumos, proveedores, equipos}` con hasta 4 resultados por categoría `[{titulo,sub,url}]`. Usado por el typeahead del navbar global. `proveedores` → sub = "NIT: X", url = `/proveedores/editar/{id}`; `equipos` → sub = tipo.displayName, url = `/equipos/ver/{id}`.

### PlanificacionController ("/planificacion") — todos los roles autenticados; escritura solo ADMIN
- `GET /planificacion` — página principal: FullCalendar + panel de próximas + tabla completa. Inyecta `proximas` (desde ayer), `todas`, `recetas` (activas), `estados` (enum values).
- `GET /planificacion/eventos?start=&end=` — `@ResponseBody`, `produces=JSON`. FullCalendar event feed por rango de fecha. Incluye `extendedProps` con todos los datos del plan para pre-llenar el modal al hacer clic en un evento.
- `POST /planificacion/guardar` — crea o actualiza plan. Si `id` está presente, edita el existente; si no, crea uno nuevo. Si `nombreElaboracion` está vacío y hay receta seleccionada, usa el nombre de la receta como fallback en el servicio.
- `POST /planificacion/{id}/estado` — cambia `EstadoPlanificacion`. Flujo natural: PLANIFICADA → EN_PROCESO → COMPLETADA. También permite → CANCELADA desde PLANIFICADA o EN_PROCESO.
- **Botón "Iniciar" (▶)**: visible solo cuando `estado == PLANIFICADA`. Es un `<a th:href="@{/nuevo(planId=${p.id})}">` (GET link, no form POST). Al hacer clic, navega a `/nuevo?planId={id}` donde `TrazabilidadController` pre-llena el formulario con los datos de la planificación y cambia el estado a EN_PROCESO. **No usa POST** — el cambio de estado ocurre en el GET de `/nuevo`.
- `POST /planificacion/{id}/eliminar` — elimina permanentemente.
- **FullCalendar**: `dateClick` → `abrirModalNuevo(fecha)`, `eventClick` → `abrirModalEditar(...)` pre-llenando el modal con `extendedProps`. Ambos solo activos cuando `esAdmin = true` (variable Thymeleaf inline). El `esAdmin` se resuelve en el template con `#authorization.expression('hasRole(''ADMIN'')')`.
- **Estado colors**: PLANIFICADA → dorado `#C9A028`, EN_PROCESO → azul `#0288D1`, COMPLETADA → verde `#198754`, CANCELADA → gris `#6c757d`. Definidos en `EstadoPlanificacion.getColor()`.

### AlertaController ("/alertas") — todos los roles autenticados
- `GET /alertas/contadores` — `@RestController`, `produces = APPLICATION_JSON_VALUE`. Retorna `AlertaContadores {bajoStock, vencimientos, mantenimiento, total}`. Sigue disponible para uso programático pero el navbar ya no lo usa (ver Campana).
- `POST /alertas/ejecutar` — `@PreAuthorize("hasRole('ADMIN')")`. Llama `AlertaScheduler.enviarAlertasDiarias()` de forma síncrona y retorna `{success:true}`. Permite forzar la creación de notificaciones sin esperar el cron. Invocado desde el botón "Verificar alertas" en `/admin/tenants`. Inyecta `AlertaScheduler`.
- **Campana en navbar** (notificaciones in-app): `<li id="alertaBellItem" class="nav-item dropdown" style="display:none">` — al cargar la página hace `fetch('/notificaciones/recientes')` (async). Si `total > 0` muestra el badge rojo ("99+" si supera 99) y el dropdown. El dropdown lista las últimas 5 notificaciones no leídas: icono por tipo, título, tiempo relativo, botón `×` (marcar leída via AJAX) y footer "Marcar todas leídas" + "Ver todas →". El JS inyecta `ALERA_CSRF_TOKEN` y `ALERA_CSRF_HEADER` via `<script th:inline="javascript">` en el navbar para los POST sin depender de meta tags del template. `_csrfToken()` y `_csrfHeader()` son helpers que prefieren los meta tags del template (si existen) y hacen fallback a las variables inline. Al abrir el dropdown se recargan las notificaciones (`show.bs.dropdown`). Falla silenciosamente.

### ComparativaController ("/comparativa") — todos los roles autenticados
- `GET /comparativa?q=` — página de selección: tabla de lotes (últimos 100) con checkboxes, búsqueda client-side, clic en fila activa checkbox, contador JS "X seleccionados", máx. 6. Botón "Comparar" habilitado desde 2 seleccionados.
- `GET /comparativa/resultado?ids=1&ids=2...` — tabla transpuesta (métricas como filas, lotes como columnas) + Chart.js grouped bar (ABV, Atenuación, Eficiencia). Celdas con mejor valor marcadas con `mejor-valor` (dorado + ★ para máximos) o `cpl-mejor` (verde + flecha para costo/litro mínimo). Notas de cata al pie. Redirige a `/comparativa` si se envían menos de 2 IDs.
- **Lógica de "mejor"**: ABV ↑, Atenuación ↑, Eficiencia ↑, Litros ↑ → `mejorMax`. Costo/litro ↓ → `mejorMin`. Map `mejores: String → Long(loteId)` pasado al modelo. En Thymeleaf: `${mejores['abv'] == lote.id}` (OGNL usa `.equals()` en `==`).

### AdminController ("/admin")
- `GET /admin/logs?tipo=&page=` — visor de log de accesos (solo ADMIN)

### TenantAdminController ("/admin/tenants") — solo ADMIN
- `GET /admin/tenants` — lista todos los tenants en grid de cards con franja de colores y mini-preview del navbar. Botones en el header: "Verificar alertas" (POST AJAX a `/alertas/ejecutar` con feedback spinner/confirmación) y "Limpiar cache" (`POST /admin/tenants/cache/evict`).
- `GET /admin/tenants/nuevo` — formulario de creación (subdomain editable)
- `GET /admin/tenants/editar/{subdomain}` — formulario de edición (subdomain readonly — es la PK). Secciones: info básica, paleta de colores (con preview en vivo), tipografías (con preview en vivo de heading + body), **Límites del plan** (selector `planTipo` + datepicker `planInicio` + display readonly de `planFin` calculado con indicador de estado verde/amarillo/rojo vía JS inline).
- `POST /admin/tenants/guardar` — llama `calcularPlanFin(tenant)` antes de persistir (calcula `planFin` automáticamente según `planTipo`); invalida cache de `TenantFilter` con `evictCache(subdomain)`
- `POST /admin/tenants/{subdomain}/toggle` — activa/desactiva tenant; invalida cache
- `POST /admin/tenants/cache/evict` — limpia todo el cache en memoria de `TenantFilter` (`evictAll()`). Útil cuando se modifica un tenant directamente en BD sin pasar por la UI.
- `GET /admin/tenants/{subdomain}/usuarios` — lista usuarios del tenant con `findAllByTenantId` (native SQL). Inyecta `UsuarioRepository` y `PasswordEncoder` directamente — no usa `UsuarioService` para evitar el filtro automático `@TenantId`.
- `POST /admin/tenants/{subdomain}/usuarios/guardar` — crea usuario via `insertarConTenant` (native SQL INSERT con tenant_id explícito). Valida unicidad con `countByUsernameAndTenantId`.
- `POST /admin/tenants/{subdomain}/usuarios/{id}/toggle` — `toggleActivoByIdAndTenantId` (native SQL `NOT activo`).
- `POST /admin/tenants/{subdomain}/usuarios/{id}/password` — `updatePasswordByIdAndTenantId` (native SQL, password BCrypt).
- `POST /admin/tenants/{subdomain}/usuarios/{id}/rol` — `updateRolByIdAndTenantId` (native SQL, rol como String).
- `POST /admin/tenants/{subdomain}/usuarios/{id}/eliminar` — `deleteByIdAndTenantId` (native SQL DELETE). Registra `USUARIO_ELIMINADO` en historial.
- `GET /admin/tenants/{subdomain}/metricas` — panel de métricas del tenant: stat-cards con totales de producción (lotes, en proceso, completados, litros), ventas (total ventas, ingresos, clientes activos), compras (facturas, total gastado), inventario (insumos, bajo stock, equipos) y sistema (usuarios activos, último acceso). Template: `admin/tenant-metricas.html`. Delega a `TenantMetricsService.obtener(subdomain)`. Redirige a `/admin/tenants` si el tenant no existe.
- `GET /admin/tenants/{subdomain}/historial` — lista `HistorialTenant` del tenant ordenado por fecha DESC. Template: `admin/tenant-historial.html`.
- `GET /admin/tenants/{subdomain}/config` — `@ResponseBody` JSON con los 11 campos de branding. Usado por el "Copiar de..." client-side en el formulario.
- `GET /admin/tenants/{subdomain}/export` — descarga `{subdomain}-branding.json` con los 11 campos de branding (name, tagline, logoUrl, colores, fuentes). NO incluye emailAdmin, active ni alertas*.
- `POST /admin/tenants/{subdomain}/import` — multipart upload de JSON. Aplica solo campos conocidos (ignora desconocidos), guarda via `TenantService.guardar()`, registra `CONFIG_IMPORTADA` en historial.
- `buildConfigMap(Tenant)` — helper privado que construye el `Map` de 11 campos de branding para export/config.
- `applyConfig(Tenant, Map)` — helper privado que aplica campos del Map al Tenant, ignorando nulls y campos desconocidos.
- `calcularPlanFin(Tenant)` — helper privado invocado en `POST /guardar`. Si `planTipo` es null/blank limpia `planInicio` y `planFin`; si no, calcula `planFin = planInicio + meses` según el tipo (MENSUAL=1, TRIMESTRAL=3, SEMESTRAL=6, ANUAL=12, BIANUAL=24). Si `planInicio` no está seteado usa `LocalDate.now()` como fallback.
- Inyecta `ObjectMapper` (Jackson) para serialización/deserialización JSON.
- `formularioEditar` pasa `otrosTenants` (todos los tenants excepto el actual) para el select "Copiar de...".
- Hereda restricción `ADMIN` de `/admin/**` en `SecurityConfig`

### MigracionController ("/admin/migracion") — solo ADMIN (hereda de `/admin/**`)
- `GET /admin/migracion/{subdomain}` — página de migración del tenant. Carga el tenant por subdomain, lista el historial via `migracionService.historial(subdomain)`. Modelo: `tenant`, `historial`. Template: `admin/migracion/detalle.html`.
- `GET /admin/migracion/{subdomain}/plantilla/{modulo}` — descarga plantilla Excel. `modulo` ∈ {almacen, equipos, comercial, produccion, **clientes**, **ventas**}. Delega a `MigracionTemplateService`. Nombre de archivo: `plantilla-{modulo}-{subdomain}.xlsx`. Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`.
- `POST /admin/migracion/{subdomain}/importar/{modulo}` — procesa la importación. Rechaza archivos vacíos con flash warning. Delega a `MigracionService.importar*()` según módulo. Flash success/warning/danger con resumen: filas procesadas, exitosas, errores y primeros 3 mensajes de error. Siempre redirige a `GET /admin/migracion/{subdomain}`.
- Accesible desde el botón "Migración" en cada card de `/admin/tenants`.
- Inyecta `MigracionTemplateService`, `MigracionService`, `TenantRepository`.

### ClienteController ("/clientes") — ADMIN, FACTURACION, SUPERADMIN
- `GET /clientes?nombre=&activo=true|false&page=` — lista paginada con 1 stat-card (totalClientes). Filtros: nombre/NIT (texto), tabs Activos/Inactivos. Typeahead en card-header llama `/clientes/suggest?q=`.
- `GET /clientes/nuevo` — formulario de creación. Modelo: `cliente` (ClienteFormDto vacío), `listasPrecio` (ListaPrecio.values()), `regimenes` (RegimenTributario.values()).
- `POST /clientes/guardar` — bean validation + NIT unique check. Flash success o error (NIT duplicado).
- `GET /clientes/editar/{id}` — formulario de edición con datos pre-llenados via `toFormDto()`.
- `POST /clientes/actualizar/{id}` — misma validación que guardar, excluye el propio registro en la verificación de NIT.
- `GET /clientes/ver/{id}` — página de detalle: Identificación fiscal, Contacto y Ubicación, Notas Internas, panel lateral de Acciones y Registro.
- `POST /clientes/{id}/toggle` — invierte el flag `activo`. Flash success.
- `GET /clientes/suggest?q=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q)`. Devuelve `[{id, nombre, nit, listaPrecio, ciudad}]`. Usado por el typeahead del formulario de venta.

### BarrilController ("/barriles") — ADMIN, INVENTARIO, PRODUCCION, SUPERADMIN
- `GET /barriles?codigo=&estado=&page=` — lista paginada con 4 stat-cards (Total, Disponibles, Llenos, Despachados). Filtros: texto libre por código y select de `EstadoBarril`. Pasa `tiposBarril` (lista estática), `estados` (enum values), `statsTotal`/`statsDisponibles`/`statsLlenos`/`statsDespachados` al modelo.
- `GET /barriles/nuevo` — formulario de creación. Modelo: `barril` (Barril vacío), `tiposBarril`, `estados`.
- `POST /barriles/guardar` — valida `@Valid` + que `codigo` no esté en blanco. Si el servicio lanza `RuntimeException` (código duplicado), redirige con flash danger; si OK redirige a `/barriles` con flash success.
- `GET /barriles/editar/{id}` — formulario de edición. Redirige a `/barriles` con flash danger si no existe.
- `POST /barriles/actualizar/{id}` — misma validación que guardar; redirige a `/barriles` con success o danger.
- `GET /barriles/ver/{id}` — detalle con info del barril + historial de movimientos. Redirige a `/barriles` con flash danger si no existe.
- `POST /barriles/{id}/estado` — llama `service.cambiarEstado(id, estado, notas)`. Redirige a `/barriles/ver/{id}`.
- `POST /barriles/eliminar/{id}` — borrado físico, redirige a `/barriles` con flash success.
- `TIPOS_BARRIL` — lista estática en el controller: "Keg 20L", "Keg 30L", "Keg 50L", "Barril 30L", "Barril 60L", "Otro".

### OrdenCompraController ("/ordenes-compra") — ADMIN, FACTURACION, SUPERADMIN
- `GET /ordenes-compra?estado=&page=` — lista paginada con 4 stat-cards (Total OC, Borrador, Enviadas, Recibidas). Filtro por `EstadoOrdenCompra`. Pasa `ordenes`, `estadoFiltro`, `estados`, `statsTotal`, `statsBorrador`, `statsEnviadas`, `statsRecibidas`, `paginaActual`, `baseUrl` al modelo.
- `GET /ordenes-compra/nueva` — formulario de creación. Modelo: `oc` (OrdenCompraFormDto vacío) + datos de formulario.
- `POST /ordenes-compra/guardar` — crea la OC; redirige a `/ordenes-compra/ver/{saved.id}` con flash success o a `/ordenes-compra` con flash danger si el servicio lanza excepción.
- `GET /ordenes-compra/editar/{id}` — formulario de edición; solo si `oc.isEditable()` (estado BORRADOR). Si no es editable redirige a `/ordenes-compra/ver/{id}` con flash warning. Usa `toFormDto()` para pre-llenar el DTO desde la entidad.
- `POST /ordenes-compra/actualizar/{id}` — actualiza la OC; redirige a `/ordenes-compra/ver/{id}`.
- `GET /ordenes-compra/ver/{id}` — detalle con transiciones válidas. Modelo: `oc`, `estados`, `tiposItem`, `transicionesValidas`.
- `POST /ordenes-compra/{id}/estado` — cambia estado según `EstadoOrdenCompra nuevoEstado`. Delega a `service.cambiarEstado(id, nuevoEstado)`.
- `POST /ordenes-compra/{id}/convertir` — convierte la OC a factura; redirige a `/facturas/editar/{facturaId}` con flash success.
- `POST /ordenes-compra/{id}/eliminar` — elimina la OC; redirige a `/ordenes-compra` con flash success.
- `GET /ordenes-compra/{id}/pdf` — descarga PDF de la OC. Lee el tenant de `request.getAttribute("currentTenant")`, construye `ExportBranding.from(tenant)`. Nombre: `oc-{numeroOc}.pdf`.
- `GET /ordenes-compra/suggest?q=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q)`. Devuelve lista de mapas con `{titulo, proveedor, fecha, estado, url}`.
- `agregarDatosFormulario()` construye: `proveedores` (List<Proveedor> activos), `tiposInsumo`/`tiposEquipo` (List<String> desde `CategoriaInsumoService`/`CategoriaEquipoService`), `tiposItem` (TipoItemFactura.values()), `insumosPorTipo` (Map<String, List<String>> nombres de insumos agrupados por categoría, para datalist en formulario JS), `equiposPorTipo` (ídem para equipos). Inyecta `InsumoInventarioRepository` y `EquipoRepository` directamente — mismo patrón que `FacturaProveedorController`.
- `toFormDto(OrdenCompra)` — convierte la entidad a DTO para pre-llenar el formulario de edición: copia proveedor, proveedorId, fechas, notas y los ítems con todos sus campos.

### VentaController ("/ventas") — ADMIN, FACTURACION, SUPERADMIN
- `GET /ventas?estado=&desde=&hasta=&page=` — lista paginada con 4 stat-cards (total ventas, pendientes, clientes únicos, ingresos despachados) + filtros opcionales por estado y rango de fechas. Typeahead en card-header busca por cliente o código de lote. Pasa `topClientes` al modelo (lista colapsable de top 5 por ingresos). Fila de la lista incluye: badge `+N` cuando la venta tiene más de 1 ítem, botón PDF directo, y botón "Despachar" (visible solo cuando `estado == PENDIENTE`). Los nuevos estados COTIZACION y EXPIRADO aparecen automáticamente en el select de filtro (usa `EstadoVenta.values()`).
- `GET /ventas/nuevo?loteId=` — formulario nuevo con lote pre-seleccionado si `loteId` está presente. El formulario soporta múltiples ítems. Campo cliente: input de búsqueda con typeahead que llama `GET /clientes/suggest?q=`; selección carga chip con nombre+NIT y setea el hidden `clienteId`. Typeahead de lote usa `GET /ventas/suggest-lotes?q=`. Preview de total en tiempo real. `step` del campo cantidad se adapta automáticamente: entero para envases (Botella/Lata/Barril/Growler/und), decimal (0.001) para volumen. Campo "Válida hasta" visible solo cuando estado=COTIZACION.
- `POST /ventas/guardar` — llama `validarCantidadDisponible(dto.getItems(), null)` antes de guardar; flash warning si supera litros de algún lote, success si todo OK.
- `GET /ventas/ver/{id}` — detalle con tabla de ítems, total general, datos del cliente (NIT, lista de precio, link a ficha si `clienteRef != null`), número de remisión (si existe), fecha de expiración (si COTIZACION/EXPIRADO), **historial de cambios de estado**, panel cambio de estado y botón eliminar (solo ADMIN/SUPERADMIN). El select de cambio de estado filtra EXPIRADO (no seleccionable manualmente).
- `GET /ventas/editar/{id}` — formulario de edición con datos pre-llenados. Setea `dto.clienteId` desde `venta.getClienteRef()` (lazy, open-in-view) y `dto.cotizacionExpiraEn`.
- `POST /ventas/actualizar/{id}` — llama `validarCantidadDisponible(dto.getItems(), id)` antes de actualizar; flash warning/success según resultado.
- `POST /ventas/{id}/eliminar` — soft delete, redirige a `/ventas`.
- `POST /ventas/{id}/estado` — cambia `EstadoVenta`. Si la transición es inválida (ej: DESPACHADO → PENDIENTE), el servicio lanza `RuntimeException` → flash danger. Redirige a `/ventas/ver/{id}`.
- `GET /ventas/duplicar/{id}` — carga la venta, limpia id/fecha, retorna formulario pre-llenado. Setea `dto.clienteId` desde `venta.getClienteRef()`. Modelo incluye `duplicadoDe` para mostrar aviso informativo.
- `GET /ventas/{id}/pdf` — descarga remisión PDF. Construye `ExportBranding.from(tenant)`. Delega a `PdfExportService.generarPdfVenta()`. Nombre: `remision-venta-{id}.pdf`.
- `GET /ventas/export?estado=&desde=&hasta=` — descarga `ventas-YYYY-MM-DD.xlsx`. Filtros opcionales. Lee branding del tenant. Delega a `ExcelExportService.generarExcelVentas()` con 4 hojas (Ventas, Ítems, Por Cliente, Por Estado).
- `GET /ventas/suggest?q=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q)`. Devuelve `[{titulo, sub, fecha, url}]`.
- `GET /ventas/suggest-lotes?q=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggestLotesParaVenta(q)`. Sin query devuelve los 20 lotes con mayor disponibilidad; con query filtra y devuelve 6. Devuelve `[{id, codigoLote, estilo, carbDestino, litrosFinales, litrosDisponibles}]`.
- `GET /ventas/suggest-clientes?q=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggestClientes(q)`. Devuelve `List<String>` con nombres de clientes activos que hacen LIKE. Mínimo 1 char.
- **Integración con detalle de lote**: `TrazabilidadController.ver()` pasa `ventasLote` al modelo; `detalle.html` muestra la sección "Ventas y Despacho" con botón "Registrar Venta" (link a `/ventas/nuevo?loteId={id}`) solo para ADMIN/FACTURACION/SUPERADMIN.
- **formulario.html — badge de empaque**: al seleccionar un lote, el badge aparece como `input-group-text` (addon a la derecha del buscador). Muestra los formatos reales del `carbDestino` (ej: `"48 × Botella 330ml | 2 × Barril 20L"`); si el lote no tiene destino, muestra `"Disp: N L"`. El texto se trunca a 120px con `text-overflow: ellipsis`; el texto completo aparece en `title` (hover). Clase CSS `.has-lote-badge` controla el `border-radius` del input adyacente. Al limpiar el buscador, el badge se oculta.
- **formulario.html — select de Unidad contextual**: al seleccionar un lote, el select de Unidad se reemplaza dinámicamente con solo los formatos del `carbDestino` del lote (ej: solo "Botella 330ml" y "Barril 20L"). Si el lote tiene un único formato, se auto-selecciona. Las opciones muestran solo el nombre del formato (ej: `"Botella 330ml"`), sin la cantidad envasada — la cantidad ya es visible en el badge del buscador. Al limpiar el lote o si el lote no tiene `carbDestino`, se restaura `UNIT_OPTIONS` (lista completa). Helper JS `parseDestinoJS(cdFull)` parsea el string multi-entrada en `[{cantidad, formato}]`; se usa también en `renderLotes()` para construir el sub-texto del dropdown de sugerencias.
- **`@WebMvcTest`**: `@MockBean VentaService ventaService` + `@MockBean TrazabilidadService trazabilidadService` + `@MockBean ExcelExportService excelExportService` + `@MockBean PdfExportService pdfExportService`. Stubs adicionales en `@BeforeEach`: `ventaService.topClientes()` → `List.of()`, `ventaService.listarHistorial(anyLong())` → `List.of()`.

### ProveedorController ("/proveedores")
- CRUD + acceso ADMIN y FACTURACION
- `GET /proveedores/suggest?q=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q)`. Busca por nombre o NIT. Devuelve `[{nombre, nit, activo, url}]`.

### EquipoController ("/equipos")
- CRUD + filtro por `EstadoEquipo` + paginación
- `GET /equipos` — lista paginada con stat-cards: total, operativos, en mantenimiento, próximos (ventana 7 días). Modelo: `statsTotal`, `statsOperativos`, `statsMantenimiento`, `statsPendientes`. Inyecta `MantenimientoEquipoService` via constructor.
- `POST /equipos/{id}/estado` — cambio rápido de estado. `@RequestParam EstadoEquipo estado`. Delega a `service.cambiarEstado(id, estado)`. Accionado desde un `<select onchange="this.form.submit()">` en cada fila de la lista y en el detalle.
- `GET /equipos/ver/{id}` — página de detalle del equipo. Modelo: `equipo`, `mantenimientos` (lista completa ordenada DESC), `costoTotal` (BigDecimal sum de todos los mantenimientos del equipo), `totalMantenimientos` (long count), `estadosEquipo`.
- `GET /equipos/suggest?q=&estado=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q, estado)`. El parámetro `estado` es opcional (`EstadoEquipo` enum, Spring lo convierte). Devuelve `[{nombre, tipo, estado, colorEstado, pendiente, url}]`. La template pasa el estado seleccionado via `data-estado` para respetar el filtro activo.

### UsuarioController ("/usuarios") — solo ADMIN
- `GET /usuarios` — lista todos los usuarios; pasa `roles = RolUsuario.values()` al modelo
- `GET /usuarios/suggest?q=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q)`. Devuelve `[{username, rol, activo, anchor}]` donde `anchor = "usuario-{id}"`. El JS hace scroll a la fila y dispara animación CSS `:target`.
- `POST /usuarios/guardar` — crea usuario. Valida: contraseña ≥6 chars, confirmación coincide, username único. Acepta `@RequestParam RolUsuario rol` (Spring convierte "ADMIN" → enum automáticamente)
- `POST /usuarios/{id}/toggle` — activa/desactiva. **Bloqueado si es el usuario en sesión.**
- `POST /usuarios/{id}/eliminar` — elimina. **Bloqueado si es el usuario en sesión.**
- `POST /usuarios/{id}/password` — cambia contraseña. Valida: ≥6 chars, confirmación coincide.
- `POST /usuarios/{id}/rol` — cambia rol. **Bloqueado si es el usuario en sesión.**
- Protección en todos los endpoints mediante `service.esElMismoUsuario(id, auth.getName())`

### PerfilController ("/perfil") — todos los roles autenticados
- `GET /perfil/password` — renderiza `perfil/password.html` (formulario de cambio de contraseña propio)
- `POST /perfil/password` — valida: `nuevaPassword.length >= 6`, `nuevaPassword == confirmarPassword`. Busca el usuario por `auth.getName()` via `usuarioService.buscarPorUsername()`, llama `cambiarPassword(id, nuevaPassword)`. Redirige a `/dashboard` con flash success o de vuelta a `/perfil/password` con flash danger. **No requiere contraseña actual** — confía en la sesión activa.

### NotificacionController ("/notificaciones") — todos los roles autenticados
- `GET /notificaciones` — página historial completo paginado. Modelo: `notificaciones` (Page), `totalNoLeidas`, `paginaActual`, `totalPaginas`. Template `notificaciones/index.html`.
- `GET /notificaciones/recientes` — `@ResponseBody`, `produces=JSON`. Para el dropdown del navbar: retorna `{total, items:[{id, tipo, icono, colorClase, titulo, mensaje, urlAccion, leida, tiempoRelativo}]}`. `tiempoRelativo` calculado en el controller (< 1min → "Hace un momento", minutos, horas, días).
- `POST /notificaciones/{id}/leer` — `@ResponseBody` JSON. Marca una notificación como leída, retorna `{success:true, noLeidas:N}`.
- `POST /notificaciones/leer-todas` — marca todas como leídas, redirige a `/notificaciones`. Usado también desde el navbar via fetch (la redirección es seguida y el body HTML descartado).
- Cae en `anyRequest().authenticated()` — accesible a todos los roles. Sin regla explícita en `SecurityConfig`.

### LoginController ("/login")
- `GET /login` — renderiza `login.html` (Spring Security gestiona el `POST /login` directamente)

### CustomErrorController ("/error")
- Implementa `org.springframework.boot.web.servlet.error.ErrorController` — intercepta el endpoint `/error` que Spring Boot usa cuando Tomcat reenvía errores HTTP (ej: `ZymosAccessDeniedHandler` redirige a `/error?status=403`).
- Lee `RequestDispatcher.ERROR_STATUS_CODE` del request y puebla `codigo`, `titulo`, `descripcion` según el status. Casos: 403 → "Acceso denegado", 404 → "Página no encontrada", 503 → "Servicio no disponible", resto → "Error inesperado".
- Devuelve vista `"error/error"` — el mismo template que usa `GlobalExceptionHandler` para excepciones Java.
- `/error` está en `permitAll()` en `SecurityConfig` y en `shouldNotFilter()` en `TenantFilter` para evitar bucles de redirección.
- **NOTA**: `GlobalExceptionHandler` maneja excepciones Java lanzadas desde controllers; `CustomErrorController` maneja el endpoint `/error` generado por Tomcat/Spring Boot para errores HTTP. Son complementarios, no redundantes.

---

## SEGURIDAD

- `@EnableMethodSecurity` activo
- **Sesión**: timeout 30 min, `invalidSessionUrl("/login?expired=true")`
- **Protección contra fuerza bruta**: `LoginAttemptService` (Caffeine TTL) rastrea intentos fallidos por IP. `LoginAttemptFilter` intercepta POST `/login` — si la IP está bloqueada, redirige a `/login?bloqueado=true` sin intentar autenticar. `ZymosAuthFailureHandler` llama `registrarFallo(ip)` en cada fallo; `ZymosAuthSuccessHandler` llama `resetear(ip)` en login exitoso. Configurable: `app.login.max-intentos` (def: 5, env: `LOGIN_MAX_INTENTOS`), `app.login.bloqueo-minutos` (def: 15, env: `LOGIN_BLOQUEO_MINUTOS`). `LoginAttemptFilter` es un bean creado en `SecurityConfig` (NO `@Component`) para evitar problemas en `@WebMvcTest`.
- **Handlers**:
  - `ZymosAuthSuccessHandler` → resetea contador de intentos por IP + registra `LOGIN_OK` en `log_accesos`
  - `ZymosAuthFailureHandler` → registra fallo por IP + registra `LOGIN_FALLIDO` + redirige a `/login?error` o `/login?bloqueado=true`
  - `ZymosAccessDeniedHandler` → registra `ACCESO_DENEGADO` + redirige a `/error?status=403`
- **Restricciones por URL:**
  - `/error`, `/error/**` → público (`permitAll`) — necesario para que usuarios no autenticados puedan ver la página de error sin generar otro redirect 403
  - `/admin/**`, `/usuarios/**`, `/tipos-cerveza/**` → solo ADMIN
  - `/actuator/**` → ADMIN (excepto `/actuator/health` que es público)
  - `POST /guardar`, `POST /actualizar/**`, `POST /eliminar/**`, `POST /duplicar/**`, `GET /nuevo`, `GET /editar/**` → ADMIN, SUPERADMIN, PRODUCCION (escritura de trazabilidad y planificación). Recetas/inventario/equipos usan `@PreAuthorize` a nivel de método para bloquear PRODUCCION en esos módulos.
  - `/facturas/**`, `/proveedores/**`, `/clientes/**`, `/ventas/**`, `/ordenes-compra/**` → ADMIN, FACTURACION, SUPERADMIN
  - `/inventario/**`, `/recetas/**` → ADMIN, INVENTARIO, PRODUCCION (lectura+escritura para INVENTARIO; solo lectura para PRODUCCION — write bloqueado por `@PreAuthorize`)
  - `/equipos/**` → ADMIN, EQUIPOS, PRODUCCION (lectura para PRODUCCION; write bloqueado por `@PreAuthorize`)
  - `/barriles/**` → ADMIN, INVENTARIO, PRODUCCION, SUPERADMIN
  - `/api/auth/**` → público (sin autenticación — endpoint de login JWT)
  - `/api/**` → cualquier usuario autenticado (HTTP Basic, sesión, o Bearer JWT)
  - Todo lo demás (incluido `/swagger-ui/**`, `/v3/api-docs/**`) → cualquier rol autenticado
- **Endpoints quick-create**: `POST /inventario/guardar-rapido` hereda `/inventario/**` (ADMIN, INVENTARIO). `POST /facturas/guardar-insumo-rapido` y `/facturas/guardar-equipo-rapido` heredan `/facturas/**` (ADMIN, FACTURACION). `POST /tipos-cerveza/guardar-rapido` hereda `/tipos-cerveza/**` (ADMIN).
- **Rate limiting — `ApiRateLimitFilter`** (`OncePerRequestFilter`, bean en SecurityConfig): actúa solo en `/api/**`. Cuenta peticiones por IP usando `Cache<String, AtomicInteger>` Caffeine con `expireAfterWrite(1, MINUTES)` — ventana fija de 1 minuto desde la primera petición. Al superar el límite devuelve HTTP 429 `{"error":"Rate limit exceeded"}`. Resuelve IP desde `X-Forwarded-For` (primer valor) o `RemoteAddr`. Límite configurable: `app.api.rate-limit` (def: 100). `FilterRegistrationBean.setEnabled(false)` evita doble registro. **CRÍTICO**: `ApiRateLimitFilter.class` NO puede usarse como anchor en `addFilterBefore` — usar `UsernamePasswordAuthenticationFilter.class`.
- **JWT — `JwtFilter`** (`OncePerRequestFilter`, bean en SecurityConfig): actúa solo en `/api/**`. Lee el header `Authorization: Bearer <token>`, valida la firma HMAC-SHA256, verifica que el tenant del claim coincida con `TenantContext.getCurrentTenant()`, y si todo es válido establece la autenticación en `SecurityContextHolder`. Si no hay token o es inválido, la request continúa sin autenticación (HTTP Basic puede tomar el relevo). CSRF deshabilitado para `/api/**` — clientes REST usan el token, no cookies. El tenant del token se embebe al generarlo y se verifica en cada request para evitar que un token de tenant A acceda a datos de tenant B. `JwtService` genera tokens con claims `{sub: username, tenant, rol}` y TTL configurable. **CRÍTICO**: en `@WebMvcTest`, mockear `JwtService` (no `JwtFilter`) — mismo patrón que `LoginAttemptService`.
- **HTTP Security Headers** (configurados en `SecurityConfig.filterChain()` via `.headers()`): HSTS (`max-age=31536000; includeSubDomains`), `X-Frame-Options: SAMEORIGIN`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`, `Permissions-Policy: camera=(), microphone=(), geolocation=(), payment=()`. CSP explícitamente omitido — el app usa múltiples CDNs y Thymeleaf inline JS que requieren `'unsafe-inline'`, lo cual vacía el beneficio de CSP.
- **CSRF en AJAX**: todos los endpoints `@ResponseBody POST` requieren el token CSRF. Los templates que los usan incluyen `<meta name="_csrf" th:content="${_csrf.token}"/>` y `<meta name="_csrf_header" th:content="${_csrf.headerName}"/>`. El JS lee estos metas y los envía como header en el `fetch()`.
- **JPA Auditing**: `JpaConfig` con `@EnableJpaAuditing(auditorAwareRef="auditorAwareImpl")`, `AuditorAwareImpl` lee usuario de SecurityContext. Fallback a `"sistema"` si no hay sesión activa.
- **Navbar**: `sec:authorize` oculta links según rol. Los ítems están agrupados en dropdowns: **Producción** (todos los roles): Trazabilidad, Kanban, Planificación, Comparativa, Calendario, **Reportes** (divider antes de Reportes — accesible a todos los roles); **Almacén** (ADMIN/INVENTARIO/PRODUCCION): Inventario, Recetas, Barriles / Kegs (icono `bi-bucket`); **Comercial** (ADMIN/FACTURACION/SUPERADMIN): Ventas, Clientes, Facturas, Proveedores, Órdenes de Compra; **Admin** (ADMIN): dropdown con 3 secciones etiquetadas — *Gestión* (Usuarios, Tipos de Cerveza), *Sistema* (Log de Accesos), *Plataforma* (Tenants — solo SUPERADMIN). Notificaciones ya no está en Admin — accesibles a todos los roles vía la campana. Equipos queda como ítem standalone (ADMIN/EQUIPOS/PRODUCCION). El botón `+` muestra acciones rápidas filtradas por rol: "Lote de cerveza" visible a ADMIN/SUPERADMIN/PRODUCCION. El dropdown de usuario muestra nombre, badge de rol y link a `/perfil/password`. El `active` check del dropdown Comercial incluye `/clientes` y `/ordenes-compra` además de `/ventas`, `/facturas`, `/proveedores`. El `active` check del dropdown Almacén incluye `/barriles`.
- **Campana de notificaciones** (`<li id="alertaBellItem">`): siempre visible en el DOM (antes tenía `style="display:none"` y se revelaba via JS). Al cargar la página hace `fetch('/notificaciones/recientes')`. Si hay notificaciones no leídas, muestra el badge rojo; si no las hay, el badge se oculta (`badge.style.display='none'`) pero la campana permanece visible. `notifMarcarLeida()` también solo oculta el badge (no el elemento `<li>`) cuando `noLeidas` llega a 0.
- **`/perfil/**`** cae en `anyRequest().authenticated()` — accesible a todos los roles. Sin regla explícita en `SecurityConfig`.
- **Multi-tenant — TenantFilter** (`OncePerRequestFilter`):
  - Extrae subdomain del header `Host` (ej: `cerveceria1.app.com` → `cerveceria1`)
  - En localhost/127.0.0.1 usa `app.default-subdomain` (normalmente `"default"`). Para probar múltiples tenants en local, agregar entradas en `hosts` (`127.0.0.1 mosto.localhost`) y acceder via `http://mosto.localhost:8080`.
  - Busca `Tenant` en BD usando `findBySubdomainAndActiveTrue` — **si `active=false` devuelve 503** aunque el tenant exista en BD. Cache en memoria Caffeine (`Cache<String, Tenant>`) con TTL configurable (`app.tenant-cache-ttl-minutes`, def: 5 min), `maximumSize(200)`. Se invalida explícitamente con `evictCache(subdomain)` o `evictAll()`.
  - Llama `TenantContext.setCurrentTenant(subdomain)` + guarda en `request.setAttribute("currentTenant", tenant)`
  - `finally` llama `TenantContext.clear()` — nunca hay fuga de contexto entre requests
  - Registrado con `addFilterBefore(tenantFilter, SecurityContextHolderFilter.class)` para que corra antes de cualquier autenticación de Spring Security
  - `FilterRegistrationBean.setEnabled(false)` evita doble registro como servlet filter
  - Salta recursos estáticos (`/css/`, `/js/`, `/img/`, etc.) y `/error` via `shouldNotFilter` — permite que la página de error se renderice aunque no haya tenant resuelto
  - `evictCache(subdomain)` — elimina un tenant del cache. `evictAll()` — limpia todo el cache (útil tras edición directa en BD).
- **Multi-tenant — Hibernate**: `TenantIdentifierResolver` implementa `CurrentTenantIdentifierResolver<String>` y lee de `TenantContext`. `HibernateMultiTenancyConfig` lo registra via `HibernatePropertiesCustomizer`. Todas las entidades con `@TenantId` son filtradas automáticamente.
- **Branding**: `GlobalControllerAdvice.branding()` lee `request.getAttribute("currentTenant")` y lo expone como `${branding}`. Si no hay tenant resuelto, cae a `BrandingProperties` (valores de `application.properties`). Usa `try-catch` defensivo — durante el dispatch de errores el request puede estar en estado inconsistente. Los templates usan `${branding.name}`, `${branding.colorAccent}`, `${branding.fontHeadings}`, `${branding.fontBody}`, etc. `BrandingProperties` también tiene `fontHeadings` (def: Cinzel) y `fontBody` (def: Raleway) como fallback.
- **Branding null-safety en navbar**: el fragment `navbar.html` usa expresiones null-safe en `<style th:inline="text">` (`branding != null ? branding.colorNavbar : '#242E0D'`) y en `th:text`/`th:if` para `branding.name` y `branding.logoUrl`. Esto evita `SpelEvaluationException` cuando `branding` es null durante el renderizado de la página de error (cascade del `HttpMessageNotWritableException`).

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
19. **Fechas en filtros JPQL**: para `LocalDate` nullable usar `:param IS NULL OR campo >= :param` — no hay equivalente al truco `""` de strings.
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
32. **Trazabilidad — Costo de Producción** (activo): asignación a nivel de ítem con cantidad parcial. La sección en `formulario.html` muestra un buscador de ítems de factura (filtrable por nombre/proveedor/tipo). Los ítems seleccionados van a `lote.itemsFactura` (OneToMany `LoteItemFactura`). `detalle.html` muestra tabla con factura, proveedor, cantidad asignada y valor proporcional. Cantidad 0 = costo completo del ítem sin ingrediente. **Auto-población al cargar receta**: al hacer click en "Cargar Receta", `verificarStockReceta()` busca el ítem de factura que coincide por nombre con cada ingrediente y pasa `cantidadReceta`/`unidadReceta` en el objeto sugerido; `autoAgregarCostosReceta()` usa esa cantidad convertida a la unidad del ítem de factura como `cantidadAsignada` inicial.
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
- Dashboard personalizable (todo localStorage, sin backend):
  - **Visibilidad**: dropdown "Personalizar" con checkboxes por sección → `localStorage` key `zymos-dashboard-secciones`. `restaurarVisibilidad()` aplica al cargar.
  - **Orden drag & drop**: SortableJS 1.15.2 sobre `#dash-sortable`, `handle: '.dash-handle'` → `localStorage` key `zymos-dashboard-orden`. `restaurarOrden()` reordena el DOM antes de aplicar visibilidad (orden primero, luego show/hide). `guardarOrden()` se llama en `onEnd`.
  - **Secciones** (`id="dash-{nombre}"`): `stats-lotes`, `stats-inventario`, `alertas`, `elaboraciones`, `charts`, `finanzas`. Cada una tiene `class="dash-section"` con `<div class="dash-handle">` (grip icon, visible en hover). `alertas` usa `th:if` → puede no existir en DOM; `restaurarOrden()` lo ignora con `getElementById` null-check. `elaboraciones` siempre está en el DOM (el empty-state está dentro).
  - **Stat-cards clickeables**: cada stat-card está envuelta en `<a class="stat-card-link">`. CSS: `display:block; text-decoration:none; transition:transform 0.15s` + `translateY(-2px)` en hover. Links: totalLotes → `/`, enProceso → `/kanban`, completados → `/?fase=completados`, estilosDistintos → `/reportes/produccion`, totalInsumos → `/inventario`, bajoStock → `/inventario?filtroBajoStock=true`, proximosAVencer → `/inventario?filtroPorVencer=true`, mantenimientoPendiente → `/equipos?estado=MANTENIMIENTO`.
  - **Stats Lotes** — 4 cards: `totalLotes`, `enProceso`, `completados`, `estilosDistintos` (4ª card; antes era `totalEquipos` — movido a Stats Inventario implícitamente via mantenimientoPendiente link a `/equipos`).
  - **Chart.js — colores en runtime**: `VERDE` y `DORADO` se leen con `getComputedStyle(document.documentElement).getPropertyValue('--verde-alera')` y `'--dorado'` dentro de `DOMContentLoaded`, después de que el navbar inyecta las CSS vars del tenant. Fallback a literales `'#364318'` / `'#C9A028'`.
  - **Próximas Elaboraciones** (`dash-elaboraciones`): tabla con hasta 5 elaboraciones futuras (desde ayer). Columnas: Fecha, Nombre, Receta, Volumen, Estado (badge con color del enum), acción. Estado PLANIFICADA → botón ▶ "Iniciar" (`btn-sm btn-primary`) link a `/nuevo?planId={id}`; otros estados → ícono ojo (`btn-sm btn-outline-secondary`) link a `/planificacion`. Alimentado por `PlanificacionService.listarProximas()` (usa `LEFT JOIN FETCH receta`).
  - **Botón "Restablecer"**: borra ambas claves localStorage y recarga.
  - **SortableJS**: mismo CDN que kanban (`sortablejs@1.15.2`). `ghostClass:'dash-ghost'`, `dragClass:'dash-drag'`.
- Búsqueda global: `GET /buscar?q=` (página completa) + `GET /buscar/suggest?q=` (JSON para typeahead del navbar)
- **Patrón typeahead/suggest**: cada módulo con lista expone `GET /{modulo}/suggest?q=` (`@ResponseBody`, `produces=JSON`). El servicio filtra en memoria (listas pequeñas) o via query DB con LIKE (facturas). El JS del template usa debounce 260ms, flechas ↑↓ para navegar, Escape para cerrar. CSS del dropdown definido en `navbar.html` (clases `.search-suggest`, `.ss-item`, `.ss-title`, `.ss-sub`, `.ss-section`, `.ss-footer`, `.ss-empty`) — disponibles globalmente por ser parte del fragment. El `data-*` attribute en el input inyecta filtros activos (tipo, estado, activa) sin inline JS. Usuarios: sin página de detalle — el click usa `href="#usuario-{id}"` + CSS `tr:target { animation: rowFlash }` para scroll+flash sin navegación.
- Mapeos entidad→DTO: usar `LoteMapper` en `com.alera.mapper` (MapStruct). No escribir asignaciones manuales campo a campo en servicios.
- Templates con AJAX: incluir siempre `<meta name="_csrf">` y `<meta name="_csrf_header">` en el `<head>`. Leer en JS con `document.querySelector('meta[name="_csrf"]')?.content`.
- **Branding en templates**: `${branding.name}`, `${branding.tagline}`, `${branding.logoUrl}`, `${branding.colorAccent}`, etc. — disponible en todos los templates via `GlobalControllerAdvice`. NO hardcodear "Alera" en HTML. Patrón obligatorio para el `<title>`: `<title th:text="${branding.name} + ' - Sección'">Alera - Sección</title>`. En contextos donde `branding` puede ser null (página de error, dispatches de error de Servlet), usar la expresión null-safe: `<title th:text="${branding != null ? branding.name : 'Alera'} + ' - Error'">Alera - Error</title>`. Los 25 templates de la app ya siguen este patrón.
- **Costos en formulario**: `ITEMS_FACTURA`, `INIT_IDS`, `INIT_CANTIDADES` se inyectan como JS via `<script th:inline="javascript">`. El submit handler construye `itemsIds[]` + `itemsCantidades[]` como hidden inputs desde el array `asignados`. El botón "Aplicar a Receta e Insumos" llama `sincronizarIngredientesDesdeItems()` que rellena los containers de ingredientes y navega al tab 1 (`goTab(1)`).
- **JS estático de trazabilidad** (`src/main/resources/static/js/`): la lógica JS de los templates de trazabilidad está extraída a archivos externos para facilitar mantenimiento. Patrón: el `<script th:inline="javascript">` del template inyecta solo los datos Thymeleaf como variables globales; el archivo `.js` externo lee esas variables. Archivos:
  - `trazabilidad-ingredientes.js` — wizard de tabs, conversión de volumen, filas dinámicas de ingredientes, carga de receta. `cargarRecetaEnLote()` guarda los datos de la receta en `_recetaPendiente` (no aplica ingredientes inmediatamente). `_actualizarEstadoAplicar(bloqueado)` deshabilita/habilita el botón "Aplicar" y el botón Siguiente según estado de stock. `verificarStockReceta()` recopila todos los ítems de costo para la advertencia de stock — los empuja a `costosSugeridos` con `cantidadReceta` y `unidadReceta` del ingrediente de la receta (via `Object.assign`). `goTab()` re-habilita el botón Siguiente al cambiar de tab; **también añade/remueve clase `done` en los elementos `.wz-tab`** (no solo en los dots) para mostrar el indicador visual de tab completado. `UNIT_OPTIONS_CLAR = UNIT_OPTIONS + '<option value="und">und</option>'` — opciones de unidad para filas de clarificantes (incluye "und"). `unitOptionsSelected(unidad, includePcs)` — segundo parámetro booleano añade "und" cuando `true` (usado en `poblarDesdeReceta` para clarificantes). `addRow` usa `tipo === 'clarificantes' ? UNIT_OPTIONS_CLAR : UNIT_OPTIONS`. Usado por `formulario.html`.
  - `trazabilidad-costos.js` — buscador de ítems de factura, asignación de costos, sincronización con ingredientes, submit handler. `sincronizarIngredientesDesdeItems()` verifica primero `_recetaPendiente`; si está seteado aplica los ingredientes de la receta y navega al tab 1. Alerta de stock insuficiente incluye botón "Ignorar advertencias y continuar". **Auto-población de cantidad asignada al cargar receta**: `autoAgregarCostosReceta()` usa `cantidadReceta`/`unidadReceta` del ítem sugerido para calcular `cantidadAsignada`, convirtiendo unidades con `convertirCantidadUnidades(cantidad, unidadOrigen, unidadDestino)` — convierte entre gr/kg/mL/L/gal respetando la unidad del ítem de factura; si las bases son incompatibles (peso vs volumen) devuelve la cantidad sin convertir. **Validación en submit**: si `fermFechaInicial` tiene valor pero `equipoFermentadorId` está vacío, llama `e.preventDefault()`, navega a tab 2 (`goTab(2)`), marca el select `is-invalid`, muestra mensaje y hace scroll al campo; un `change` listener en el select limpia el error al seleccionar un fermentador. Depende de `trazabilidad-ingredientes.js` (llama `goTab`, `poblarDesdeReceta`). Usado por `formulario.html`.
  - `trazabilidad-detalle.js` — construcción del gráfico Chart.js dual-eje (densidad + temperatura). Lee `CHART_FECHAS`, `CHART_DENSIDAD`, `CHART_TEMP`. Usado por `detalle.html`.
  - `trazabilidad-kanban.js` — drag & drop SortableJS, AJAX POST de cambio de fase, toast, contadores. Lee `esAdmin` (inyectado por el template). CSRF leído lazily via `_csrfToken()`/`_csrfHeader()` del navbar. **Validación de fermentador**: en `onEnd`, si `targetFase === 'fermentacion'` y `card.dataset.tieneFermentador !== 'true'`, revierte el DOM inmediatamente y llama `mostrarToastFermentador(loteId)` — toast de 5s con link directo a `/editar/{id}`, sin hacer el POST al servidor. Cada card tiene `data-tiene-fermentador="true|false"` inyectado por Thymeleaf en `kanban.html`. Usado por `kanban.html`.
  - **Orden de carga en `formulario.html`**: (1) `th:inline` data block, (2) `trazabilidad-ingredientes.js`, (3) `trazabilidad-costos.js`.

---

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

---

## TESTS

**Unitarios** (`src/test/java/com/alera/service/`):
- `InsumoInventarioServiceTest` (13 tests), `TrazabilidadServiceTest` (10 tests), `DashboardServiceTest`
- `InsumoInventarioServiceTest` — requiere `@Mock MovimientoInventarioRepository movimientoRepo` y stub `lenient().when(movimientoRepo.save(any())).thenReturn(new MovimientoInventario())` en `@BeforeEach` — el servicio registra movimientos en `descontarIngrediente`, `restaurarIngrediente` y `ajustar`. `descontarIngrediente` y `restaurarIngrediente` son de 3 args: `(nombre, cantidadTexto, referencia)` — actualizar stubs en `TrazabilidadServiceTest` con el arg adicional `any()`.
- `TrazabilidadServiceTest` — 10 tests: generación de código (IPA-001, consecutivo), advertencias de stock (sin/con), normalización de unidades (kg→gr, gal→mL), eliminar (restaura inventario, lote no existe → excepción), **límites del plan** (`guardar_limiteLotesAlcanzado_lanzaExcepcion` — lanza RuntimeException cuando `loteRepo.count() >= maxLotes`; `guardar_conEspacioEnPlan_noLanzaExcepcion` — no lanza cuando hay espacio). Requiere `@Mock TenantRepository tenantRepo` y stub `lenient().when(tenantRepo.findById(any())).thenReturn(Optional.empty())` en `@BeforeEach` para simular tenant sin límite por defecto.
- `FacturaProveedorServiceTest`, `UnidadUtilsTest` — `FacturaProveedorServiceTest` requiere `@Mock FacturaHistorialEstadoRepository historialRepo` y `@Mock ProveedorRepository proveedorRepo` (además de los mocks previos) porque el constructor del servicio los inyecta. El `@BeforeEach` stubea `historialRepo.save(any())` para que `guardar()` no lance NPE. 7 tests en total: sin descuento/IVA, con IVA 19%, descuento antes de IVA, múltiples ítems, costo de envío, `ivaIncluido=true` (base extraída correctamente), `ivaIncluido=true` con descuento.
- `LogAccesoServiceTest` — 12 tests: cubre `registrar`, `listarPaginado` (con/sin filtro) y `fallidosUltimaHora` (verifica ventana de 1 hora). Usa `ReflectionTestUtils.setField` para inyectar `pageSize` sin contexto Spring.
- `EquipoServiceTest` — 17 tests: listar/paginar (con y sin filtro de estado), buscarPorId, guardar, eliminar (happy path, no encontrado, con lotes activos → EquipoEnUsoException), fermentadores disponibles, mantenimiento pendiente (verifica ventana de 7 días), `cambiarEstado` (actualiza y persiste, no existe → excepción), `countByEstado`, `countMantenimientoPendiente`, `countTotal`.
- `RecetaServiceTest` — 16 tests: listarActivas/Todas/Paginado (filtros null/true/false), buscarPorId (found/not found), guardar (campos básicos, normalización kg→gr, ignorar vacíos, escalones en orden), actualizar (limpia ingredientes anteriores), eliminar, toFormDto (mapeo directo, parseo "5000 gr"→{cantidad,unidad}, fila vacía si lista vacía), duplicarComoFormDto (limpia id, añade " (Copia)", versión 1), suggest (filtro activa, límite 6). OG/FG objetivo usan literales Integer (ej: `1050`, `1010`) — NO BigDecimal.
- `UsuarioServiceTest` — 25 tests: `loadUserByUsername` (usuario válido, no existe, inactivo, mapeo de todos los roles → `ROLE_X`), `guardar` (BCrypt encode, rol específico, null→ADMIN), `toggleActivo` (activo→inactivo, inactivo→activo, no existe no-op), `cambiarPassword` (encode y guarda, no existe no-op), `cambiarRol`, `eliminar`, `existeUsername`, `esElMismoUsuario` (mismo, distinto, no existe), `suggest` (null/corta, filtro, límite 6, estructura del mapa con displayName). Requiere `@Mock SuperAdminRepository superAdminRepo` — `loadUserByUsername` lo consulta primero (super-admins no tienen tenant). Los tests de `loadUserByUsername` deben stubear `superAdminRepo.findByUsernameAndActivoTrue(username)` → `Optional.empty()` para que la lógica pase al repositorio de usuario regular.
- `TenantServiceTest` — 19 tests: `listarTodos` (orden por subdomain), `buscarPorSubdomain`, `guardar` (CREADO/EDITADO, evicta cache, registra historial, retorna tenant), `evictAllCache`, `toggleActivo` (ACTIVADO/DESACTIVADO, evicta cache, no existe no-op), `listarHistorial`, `registrarAccion` (con/sin autenticación → "sistema"), `registrarEnvioExitoso` (resetea contador, timestamps, no existe no-op), `registrarEnvioFallido` (incrementa, no existe no-op). Usa `SecurityContextHolder` para simular usuario autenticado; limpia en `@AfterEach`.
- `EmailService` usa `@Autowired` en campos (no constructor) → tests usan `ReflectionTestUtils.setField` para inyectar `mailSender`, `templateEngine`, `fromAddress`, `baseUrl`. `MimeMessage` creado con `new MimeMessage((jakarta.mail.Session) null)` — permite que `MimeMessageHelper` opere sin SMTP real.
- `EmailServiceTest` — 25 tests: `mailConfigurado` (con/sin SMTP), `enviarAlertasDiarias` (sin SMTP, email null/vacío, sin alertas, con bajoStock/vencimientos/mantenimiento, fallo SMTP → RuntimeException, variables al template), `enviarBienvenida` (sin SMTP, emailAdmin null/vacío, exitoso → true, fallo SMTP → false, variables al template — 6 tests), `enviarEmailPrueba` (sin SMTP, destinatario null/vacío, éxito → null, fallo → mensaje error), `diasHasta` (hoy/futuro/pasado).
- `TipoCervezaServiceTest` — 11 tests: `listarActivos/Todos`, `buscarPorId`, `existePorNombre`, `guardar`, `eliminar`, `toggleActivo` (activo→inactivo, inactivo→activo, no existe no-op).
- `ProveedorServiceTest` — 15 tests: `listarActivos/Todos`, `buscarPorId`, `suggest` (null/corta, delega a `repo.search()`, retorna resultados del repo, límite 6, estructura mapa con url, NIT null → string vacío), `guardar`, `eliminar`, `contarFacturas`, `totalFacturas`.
- `MantenimientoEquipoServiceTest` — 9 tests: `listarPorEquipo` (vacío y con resultados), `registrar` (campos del DTO en MantenimientoEquipo, actualiza `fechaUltimoMantenimiento`, actualiza/no-actualiza `proximoMantenimiento` según null, equipo no existe → RuntimeException, retorna guardado), `eliminar`.
- `ClienteServiceTest` — 25 tests. Único mock: `@Mock ClienteRepository repo`. `@BeforeEach` inyecta `pageSize=15` vía `ReflectionTestUtils`. Helpers: `buildDto(nombre, nit)` crea `ClienteFormDto` con `activo=true`; `clienteConId(id, nit)` crea `Cliente` con id y nit seteados (para tests de unicidad). Tests — `listarActivos` (1): delega a `findAllByActivoTrueOrderByNombreAsc`, verifica nombre. `listarTodos` (1): delega a `findAllByOrderByNombreAsc`, hasSize(2). `buscarPorId` (2): found → `isPresent()`, not found → `isEmpty()`. `guardar` (6): (1) `guardar_persisteCliente` — captor verifica nombre, email y `isActivo=true`; (2) `guardar_nitNull_noVerificaUnicidad` — NIT null, `verify(repo, never()).findByNit(any())`; (3) `guardar_nitBlanco_noVerificaUnicidad` — NIT `"   "`, mismo verify; (4) `guardar_nitNoDuplicado_persiste` — `findByNit("900-1")` → empty, `verify(repo).save(any())`; (5) `guardar_nitDuplicado_lanzaExcepcion` — `findByNit` devuelve cliente con `id=99L`, dto.id=null → `assertThatThrownBy` con mensaje que contiene "900-1"; (6) `guardar_blancosNormalizadosANull` — razonSocial `"  "`, telefono `"   "`, notas `""` → captor verifica los tres campos son null (helper `blank()` normaliza). `actualizar` (4): (1) `actualizar_noExiste_lanzaExcepcion` — id 99L, empty → RuntimeException con mensaje que contiene "99"; (2) `actualizar_modificaCampos` — cliente existente "Viejo Nombre", dto "Nuevo Nombre"; `thenAnswer(inv→inv.getArgument(0))` en save; captor verifica nombre actualizado; (3) `actualizar_nitDelMismoCliente_noLanzaExcepcion` — `existente.getId()=1L == id=1L` → no excepción, `verify(repo).save(any())`; (4) `actualizar_nitDuplicadoEnOtroCliente_lanzaExcepcion` — `findByNit` devuelve cliente con `id=55L`, `id param=1L` → RuntimeException con mensaje que contiene "700-2". `toggleActivo` (3): activo→false, inactivo→true (ambos usan `thenAnswer` + captor), no existe → `verify(repo, never()).save(any())`. `suggest` (8): (1) null → empty, `verifyNoInteractions(repo)`; (2) vacío `""` → empty, sin interacciones; (3) `suggest_delegaASearchActivos` — verifica `repo.searchActivos(eq("mosto"), any(Pageable.class))` llamado; (4) `suggest_limiteSeisResultados` — repo devuelve 8 clientes, resultado hasSize(6); (5) `suggest_estructuraDelMapa` — cliente con nombre/nit/listaPrecio(BAR)/ciudad; resultado contiene claves `{id, nombre, nit, listaPrecio, ciudad}`; `listaPrecio` = `ListaPrecio.BAR.getDisplayName()`; (6) `suggest_nitNull_usaStringVacio` — nit null → `""` en el mapa; (7) `suggest_listaPrecioNull_usaStringVacio` — listaPrecio null → `""`; (8) `suggest_ciudadNull_usaStringVacio` — ciudad null → `""`. **Clave de implementación**: `searchActivos` retorna `List<Cliente>` (no `Page`) — stubs usan `List.of(...)` no `new PageImpl<>()`. Unicidad NIT: en `guardar` compara `existente.getId().equals(dto.getId())` (dto.getId()=null para nuevos → siempre lanza); en `actualizar` compara `existente.getId().equals(id)` (parámetro del método → mismo cliente no lanza).
- `JwtServiceTest` — 9 tests: generarToken (incluye claims username/tenant/rol), validarToken (válido, expirado, firma inválida, subject correcto), extraerUsername, extraerTenant, extraerRol, ttl configurable. Instancia `JwtService` directamente con `ReflectionTestUtils.setField` para `secret` y `ttlHours`.
- `VentaServiceTest` — 25 tests. Mocks: `@Mock VentaRepository ventaRepo`, `@Mock VentaItemRepository ventaItemRepo`, `@Mock LoteCervezaRepository loteRepo`, `@Mock VentaHistorialEstadoRepository historialRepo`, `@Mock NotificacionService notificacionService`, `@Mock ClienteRepository clienteRepo`, `@Mock InsumoInventarioService insumoService`, `@Mock EntityManager em`, `@Mock Query nativeQuery`. `em` se inyecta vía `ReflectionTestUtils.setField(service, "em", em)` — es `@PersistenceContext`, no constructor. `@BeforeEach` inyecta `pageSize=15`, `expiracionDias=15` vía `ReflectionTestUtils`, y hace `lenient()` stubs: `historialRepo.save → VentaHistorialEstado`, `em.createNativeQuery → nativeQuery`, `nativeQuery.setParameter → nativeQuery`, `nativeQuery.getSingleResult → 0`, `ventaItemRepo.findItemsConEnvase(anyLong()) → List.of()`. Helper `buildDto(cliente, cantidad, precio)` crea `VentaFormDto` con fecha hoy, estado PENDIENTE y un `VentaItemFormDto` (unidad "L", descuentoPct=0) añadido a `dto.getItems()`. Tests — guardar: (1) `guardar_persisteVenta` — "Cervecería Prueba", cantidad=10, precio=5000; `ArgumentCaptor` verifica cliente, estado PENDIENTE, `items.hasSize(1)`, cantidad y precio del ítem; (2) `guardar_registraHistorial` — captor en `historialRepo.save()` verifica `estadoAnterior=null` y `estadoNuevo=PENDIENTE`; (3) `guardar_vinculaLote` — `dto.getItems().get(0).setLoteId(1L)`, lote stub devuelve lote con `codigoLote="IPA-001"`; captor verifica `item0.getLote()=lote` y `item0.getCodigoLote()="IPA-001"`; (4) `guardar_sinLote` — sin loteId; captor verifica `item0.getLote()=null` y `item0.getCodigoLote()=null`; (5) `guardar_estadoNullDefaultPendiente` — `dto.setEstado(null)`; captor verifica estado=PENDIENTE. Actualizar: (6) `actualizar_modificaCampos` — existente cliente "Anterior", dto "Nuevo Cliente"; `thenAnswer(inv→inv.getArgument(0))` en save; captor verifica nuevo cliente; (7) `actualizar_registraHistorialAlCambiarEstado` — existente PENDIENTE, dto DESPACHADO; captor historial verifica `estadoAnterior=PENDIENTE`, `estadoNuevo=DESPACHADO`; (8) `actualizar_noRegistraHistorialSinCambioEstado` — PENDIENTE→PENDIENTE; `verify(historialRepo, never()).save(any())`; (9) `actualizar_noExiste_lanzaExcepcion` — id 99L, `Optional.empty()` → `assertThrows(RuntimeException.class)`. Eliminar (soft delete): (10) `eliminar_softDelete` — verifica `never().deleteById(any())`; captor verifica `getDeletedAt() != null`; (11) `eliminar_noExiste_noOp` — id 99L; verifica `never().save()` y `never().deleteById()`. Cambiar estado: (12) `cambiarEstado_actualizaEstado` — PENDIENTE→DESPACHADO; captor verifica estado=DESPACHADO; (13) `cambiarEstado_registraHistorial` — PENDIENTE→CANCELADO; captor historial verifica `estadoAnterior=PENDIENTE`, `estadoNuevo=CANCELADO`; (14) `cambiarEstado_despachado_creaNotificacion` — venta.cliente="Bar La Espuma"; verifica `notificacionService.crear(any(), contains("Bar La Espuma"), any(), any())`. Validar cantidad: (15) `validarCantidad_listaNull_retornaNull` — null → null; (16) `validarCantidad_sinLoteId_retornaNull` — ítem sin loteId → null; (17) `validarCantidad_dentroDelLimite_retornaNull` — lote litrosFinales=100, existente en BD=50, nuevo ítem=30 (50+30=80 ≤ 100) → null; (18) `validarCantidad_superaLimite_retornaAdvertencia` — existente=80, nuevo=30 (80+30=110 > 100) → advertencia no null que contiene "IPA-001". Historial: (19) `listarHistorial_delegaARepo` — id 5L; verifica `findByVentaIdOrderByFechaDesc(5L)` llamado, resultado hasSize(1). Suggest: (20) `suggest_queryCorta_retornaVacio` — "a", null y "  " retornan vacío; (21) `suggest_retornaEstructura` — "Distribu", venta.cliente="Distribuidora Norte"; resultado hasSize(1), contiene clave "titulo"="Distribuidora Norte" y "url" que contiene "/ventas/ver/" — `primerCodigoLote` es `@Formula`, null en tests sin BD. Stats: (22) `countTotal_delegaARepo` — `ventaRepo.count()=42L`; (23) `countByEstado_delegaARepo` — PENDIENTE→7L; (24) `sumIngresos_nullRetornaZero` — `ventaItemRepo.sumIngresosDespachados()=null` → ZERO (delega a `ventaItemRepo`, no `ventaRepo`); (25) `listarPaginado_delegaARepo` — verifica `findAllFiltered(eq(PENDIENTE), any(), any(), any())` llamado, resultado no null.
- `EvaluacionSensorialServiceTest` — 15 tests. Mocks: `@Mock EvaluacionSensorialRepository repo`, `@Mock LoteCervezaRepository loteRepo`. Tests — `listarPorLote` (2): delega a `findByLoteIdOrdenadas`, lista vacía retorna vacía. `agregar` (4): persiste con todos los campos, catador en blanco → null, notas en blanco → null, lote no existe → RuntimeException con mensaje que contiene el id. `eliminar` (1): llama `deleteById`. `calcularPromedio` (3): promedio correcto con dos evaluaciones (44+35=39.5), lista vacía → 0.0, evaluación sin puntaje (todos null) excluida del cálculo. Métodos computados de entidad (5): `puntajeTotal` calcula correctamente (10+3+18+4+9=44), todos null → null, algunos null → suma los presentes, clasificación correcta por rango (Excepcional/Excelente/Muy buena/Buena/Aceptable/Deficiente/Inaceptable), badgeClass correcta por rango. Helpers: `evalConPuntajes(aroma,apariencia,sabor,boca,general)`, `evalConTotal(total)` — distribuye el total entre los 5 campos respetando los máximos BJCP.
- `BarrilServiceTest` — 16 tests. Mocks: `@Mock BarrilRepository barrilRepo`, `@Mock MovimientoBarrilRepository movimientoRepo`. `@BeforeEach` inyecta `pageSize=15` via `ReflectionTestUtils`, `lenient().when(movimientoRepo.save(any())).thenAnswer(inv->inv.getArgument(0))`. Helper `barrilConId(id, codigo)`. Tests — `listarPaginado` (2): delega a `findByFiltros` con `""` cuando codigo null + filtro de estado; con filtros pasa correctamente. `buscarPorId` (2): encontrado → retorna barril; noExiste → lanza RuntimeException con "99". `guardar` (4): persiste con estado DISPONIBLE por defecto; codigo duplicado → lanza RuntimeException con código; crea MovimientoBarril inicial (estadoAnterior=null, estadoNuevo=DISPONIBLE); normaliza blancos (clienteNombre `"  "` → null, observaciones `"   "` → null). `actualizar` (2): modifica codigo, verifica save con id correcto; mismo codigo no llama `existsByCodigoIgnoreCaseAndIdNot`. `cambiarEstado` (4): actualiza estado y crea MovimientoBarril con notas; a DISPONIBLE limpia codigoLote/clienteNombre/fechaDespacho; noExiste → RuntimeException con "99". `eliminar` (2): llama `deleteById`; noExiste → RuntimeException, `never().deleteById()`. `countTotal` (1): delega a `repo.count()`. `countByEstado` (1): delega a `repo.countByEstado()`. `listarMovimientos` (1): delega a `movimientoRepo.findByBarrilIdOrderByFechaDesc()`.
- `PdfExportServiceTest` — 8 smoke tests: verifica magic bytes `%PDF`, lote mínimo sin lecturas, lote completo (densidades, fases, obs), lecturas con densidad+temp, solo densidad, solo temperatura, lecturas null, tamaño >1KB, PDFs distintos para lotes distintos. Instancia `PdfExportService` directamente (sin Spring context — no tiene dependencias). Usa `private static final ExportBranding BRANDING = ExportBranding.defaults("Alera")` como constante de test.
- `ExcelExportServiceTest` — 8 smoke tests: verifica magic bytes `PK` (ZIP/XLSX), listas vacías, lote mínimo, lotes con métricas, resumen por estilos, 50 lotes sin excepción, contenido distinto para lotes distintos. Usa `ExportBranding.defaults("Alera")` como constante de test. **Bug descubierto**: fechas `null` en `desde`/`hasta` → `RuntimeException` (NPE interno al formatear) — el test lo documenta y verifica el comportamiento real. **NOTA**: `List.of(Object[])` causa ambigüedad de tipos en Java 26 — usar `new ArrayList<>()` para listas de `Object[]`.

**DTOs** (`src/test/java/com/alera/dto/`) — Bean Validation API, sin contexto Spring:
- `FacturaFormDtoValidationTest` — 13 tests. Instancia `Validator` directamente vía `Validation.buildDefaultValidatorFactory().getValidator()`. Helper `dtoValido()` crea `FacturaFormDto` con `numeroFactura`, `proveedor` y `fechaFactura` rellenos; `itemValido()` crea `FacturaItemDto` con todos los campos obligatorios. Tests: (1) `dto_valido_sinViolaciones` — sin violaciones cuando todos los campos están bien; (2) `numeroFactura_nulo_violacion` — viola `@NotBlank`; (3) `numeroFactura_blanco_violacion` — `"   "` viola `@NotBlank`; (4) `fechaFactura_nula_violacion` — viola `@NotNull`; (5) `proveedor_nulo_violacion` — viola `@NotBlank`; (6) `item_tipoItem_nulo_violacion` — viola `@NotNull` en cascada via `@Valid`; (7) `item_nombre_blanco_violacion` — viola `@NotBlank` en cascada; (8) `item_cantidad_cero_violacion` — viola `@DecimalMin("0.001")` en cascada; (9) `item_valorUnitario_negativo_violacion` — viola `@DecimalMin("0.0")`; (10) `item_porcentajeDescuento_negativo_violacion`; (11) `item_porcentajeIva_negativo_violacion`; (12) `items_listaVacia_sinViolaciones` — lista vacía de ítems no produce violaciones; (13) `item_valido_sinViolaciones` — ítem con todos los campos obligatorios válidos no produce violaciones. La cascada funciona porque `FacturaFormDto.items` tiene `@Valid`.

**Controladores** (`src/test/java/com/alera/controller/`) — `@WebMvcTest` + `@MockBean`:
- `TrazabilidadControllerTest` — 15 tests: seguridad (sin-autenticar → 401; con rol no-admin → controller corre porque URL-based security no se enforce con handler mock), index, kanban, nuevo/guardar (válido, inválido, advertencia stock), ver/404, eliminar. `@MockBean`: `PdfExportService`, `LecturaFermentacionService`, `PlanificacionService`, `EvaluacionSensorialService` (los cuatro requeridos por el constructor del controller). `@BeforeEach` stub: `ventaService.listarPorLote(any())` → `List.of()`.
- `AuthControllerTest` — 3 tests (`@AutoConfigureMockMvc(addFilters=false)` para aislar la lógica del controller): login con credenciales válidas retorna token + campos del `AuthResponse`, credenciales inválidas → 401 con `{error}`, body vacío → 400. `@MockBean AuthenticationManager` y `JwtService`.
- `ApiControllerTest` — 9 tests: seguridad (401), lotes (lista, por id, 404, historial), recetas, alertas inventario, dashboard
- `AlertaControllerTest` — 6 tests: seguridad (401), estructura JSON, totales (suma de 3 contadores), sin alertas, solo mantenimiento, `POST /alertas/ejecutar` llama al scheduler y retorna `{success:true}`. Requiere `@MockBean AlertaScheduler`.
- `NotificacionControllerTest` — 5 tests: seguridad (401), GET /notificaciones (página con modelo), GET /recientes (JSON con total e items), POST /{id}/leer (JSON con noLeidas), POST /leer-todas (redirect)
- `PlanificacionControllerTest` — 11 tests: seguridad (401 sin autenticar; 302 via `ZymosAccessDeniedHandler` para acceso denegado), página principal, eventos JSON, guardar/cambiarEstado/eliminar (ADMIN vs no-ADMIN)
- `LoginControllerTest` — 3 tests: GET /login público (200), con ?error, con ?bloqueado. **Nota**: en `@WebMvcTest`, Spring Security puede interceptar GET /login con su propio filtro antes del DispatcherServlet — no verificar `view().name("login")`, solo `status().isOk()`.
- `DashboardControllerTest` — 2 tests: 401 sin auth, 200 con cualquier rol (incluye `stats` y `proximasElaboraciones` en modelo). Requiere `@MockBean PlanificacionService planificacionService` + stub `planificacionService.listarProximas()` → `List.of()` en `@BeforeEach`.
- `CalendarioControllerTest` — 3 tests: 401 sin auth, 200 autenticado, eventos JSON
- `AdminControllerTest` — 2 tests: 401, 200 ADMIN con lista vacía de logs
- `PerfilControllerTest` — 4 tests: 401, 200 con cualquier rol (GET), POST cambio contraseña válido redirige a dashboard, POST contraseña inválida (< 6 chars) redirige con error
- `BusquedaControllerTest` — 4 tests: 401, 200 con query, suggest retorna JSON, suggest incluye claves `proveedores` y `equipos`. **Nota**: `loteRepo.search()` y `recetaRepo.search()` retornan `List<>` (no `Page`) — usar `when(...).thenReturn(List.of())`
- `TipoCervezaControllerTest` — 3 tests: 401, 200 ADMIN, `guardarRapido` → JSON 200. **Nota**: stub `service.guardar(any())` para devolver un `TipoCerveza` con id/nombre, si no el NPE cae al catch → 400
- `UsuarioControllerTest` — 4 tests: 401, 200 ADMIN, suggest JSON, guardar con contraseña inválida redirige. **Nota**: el parámetro del controller se llama `confirmPassword` (no `confirmarPassword`)
- `RecetaControllerTest` — 4 tests: 401, 200 con filtro activas, suggest JSON, GET /editar retorna formulario
- `EquipoControllerTest` — 6 tests: 401, 200 ADMIN, suggest JSON, GET /ver/{id} retorna detalle, `ver_noExiste_redirige` (GET /equipos/ver/99 → 3xx + flash danger), `editar_noExiste_redirige` (GET /equipos/editar/99 → 3xx + flash danger). **Nota**: método se llama `listarFermentadoresDisponibles()` (no `fermentadoresDisponibles()`). Usar `doReturn(new PageImpl<>(Collections.emptyList())).when(service).listarPaginado(any(), anyInt())`. Requiere `@MockBean MantenimientoEquipoService`. Stubs adicionales: `countTotal()`, `countByEstado(any())`, `countMantenimientoPendiente()` → 0L.
- `ProveedorControllerTest` — 4 tests: 401, 200 con roles ADMIN/FACTURACION, suggest JSON, `editar_noExiste_redirige` (GET /proveedores/editar/99 → 3xx + flash danger)
- `InsumoInventarioControllerTest` — 5 tests: 401, 200 ADMIN, suggest JSON con filtro nombre, `editar_noExiste_redirige` (GET /inventario/editar/99 → 3xx + flash danger), `historial_noExiste_redirige` (GET /inventario/99/historial → 3xx + flash danger). Requiere `@MockBean ExcelExportService excelService` y `@MockBean ProveedorService proveedorService` — ambos inyectados en el constructor del controller. Stubear `proveedorService.listarActivos()` → `List.of()` en `@BeforeEach`.
- `FacturaProveedorControllerTest` — 5 tests: 401, 200 ADMIN, suggest JSON, `ver_noExiste_redirige` (GET /facturas/ver/99 → 3xx + flash danger), `editar_noExiste_redirige` (GET /facturas/editar/99 → 3xx + flash danger). `@MockBean InsumoInventarioRepository`, `EquipoRepository`, `ExcelExportService` y `FacturaItemRepository` adicionales — el constructor del controller recibe `FacturaItemRepository` como 10° parámetro; sin mock el contexto no carga. **Nota**: stub usa `listarPaginado(any(), any(), any(), anyInt())`. El `@BeforeEach` también stubea `sumTotal(any(),any(),any()) → BigDecimal.ZERO`, `sumPendiente(any(),any()) → BigDecimal.ZERO`, `countPendiente(any(),any()) → 0L` — necesarios porque `lista()` los pasa al modelo y el template los renderiza en las stat-cards.
- `ReporteControllerTest` — 6 tests: 401, 200 con rango de fechas, 200 sin filtros, excel retorna descarga, pdf retorna descarga con `Content-Disposition` que contiene "reporte-produccion", filtro por estilo. Requiere `@MockBean PdfExportService pdfService` y stub `pdfService.generarPdfReporteProduccion(any(),any(),any(),any(),any())` en `@BeforeEach`.
- `MantenimientoEquipoControllerTest` — 3 tests: 401, 200 ADMIN, `lista_equipoNoExiste_redirige` (GET /equipos/99/mantenimientos → 3xx + flash danger). **Nota**: el equipo mock debe tener `tipo` y `estado` seteados (`"Fermentador"` como String, `EstadoEquipo.OPERATIVO`) — el template accede a `equipo.tipo` directamente sin null-check. Stubs adicionales: `sumCostoPorEquipo(1L)` → `BigDecimal.ZERO`, `countPorEquipo(1L)` → 0L.
- `TenantAdminControllerTest` — 10 tests: (1) `lista_sinAuth_retorna401`; (2) `lista_conAdmin_retorna200`; (3) `nuevo_conAdmin_retornaFormulario`; (4) `config_retornaJson`; (5) `formularioEditar_noExiste_redirige`; (6) `historial_noExiste_redirige`; (7) `usuarios_noExiste_redirige`; (8) `config_noExiste_retorna404`; (9) `metricas_conAdmin_retorna200` — stubea tenant + `metricsService.obtener("mosto")` con `TenantMetrics` completo; verifica 200, view "admin/tenant-metricas", model tiene `tenant` y `metricas`; (10) `metricas_noExiste_redirige` — tenant inexistente → 3xx redirect a /admin/tenants + flash `tipoMensaje=danger`. Requiere `@MockBean PasswordEncoder` y `@MockBean TenantMetricsService`. **CRÍTICO**: NO agregar `@MockBean ObjectMapper` — mockear Jackson rompe la autoconfiguración de Spring (`routerFunctionMapping` falla al crear porque `objectMapper.reader()` retorna null en el mock)
- `ComparativaControllerTest` — 3 tests: 401, 200 autenticado, resultado con <2 ids redirige
- `VentaControllerTest` — 7 tests. `@MockBean` adicionales a los estándar: `VentaService ventaService`, `TrazabilidadService trazabilidadService`, `ExcelExportService excelExportService`, `PdfExportService pdfExportService`, `ClienteService clienteService`. `@BeforeEach` stubs: `listarPaginado(any,any,any,anyInt)` → `PageImpl(List.of())`, `countTotal()` → 0L, `countByEstado(any)` → 0L, `countClientesUnicos()` → 0L, `sumIngresosDespachados()` → ZERO, `suggest(anyString)` → `List.of()`, `topClientes()` → `List.of()`, `listarHistorial(anyLong)` → `List.of()`. Tests: (1) `lista_sinAuth_retorna401` — GET /ventas sin auth → 401; (2) `lista_conAdmin_retorna200` — ADMIN → 200, view "ventas/lista"; (3) `lista_conFacturacion_retorna200` — FACTURACION → 200; (4) `suggest_retornaJson` — ADMIN, GET /ventas/suggest?q=norte; stubs `ventaService.suggest("norte")` con `Map.of("titulo","Distribuidora Norte","sub","IPA-001","url","/ventas/ver/1")`; verifica `jsonPath("$[0].titulo").value("Distribuidora Norte")`; (5) `ver_retornaDetalle` — ADMIN, GET /ventas/ver/1; venta con cliente "Cliente Test" y estado PENDIENTE; verifica 200, view "ventas/detalle", `model().attributeExists("historial")`; **NO** setear campos que ya no existen en `Venta` (`cantidad`, `precioUnitario`, `descuentoPct`) — `valorTotal` es `@Formula` (null sin BD = ZERO vía `getValorTotal()`); (6) `nuevo_retornaFormulario` — ADMIN, GET /ventas/nuevo → 200, view "ventas/formulario"; (7) `pdf_retornaPdf` — ADMIN, GET /ventas/1/pdf; venta con estado DESPACHADO; `pdfExportService.generarPdfVenta(any,any)` devuelve bytes `{0x25,0x50,0x44,0x46}` (magic bytes `%PDF`); verifica `header("Content-Disposition")` contiene "remision-venta-1.pdf".
- `ClienteControllerTest` — 8 tests. Único `@MockBean` adicional al set estándar: `ClienteService clienteService`. `@BeforeEach` stubs: `listarPaginado(any,any,anyInt)` → `PageImpl(List.of())`, `suggest(anyString)` → `List.of()`. Tests: (1) `lista_sinAuth_retorna401` — GET /clientes sin auth → 401; (2) `lista_conAdmin_retorna200` — ADMIN → 200, view "clientes/lista", `model().attributeExists("clientes","totalClientes")`; (3) `lista_conFacturacion_retorna200` — FACTURACION → 200; (4) `suggest_retornaJson` — ADMIN, GET /clientes/suggest?q=mosto; stubs `suggest("mosto")` con `Map.of("id",1L,"nombre","Cervecería Mosto","nit","900-1","ciudad","Bogotá")`; verifica `jsonPath("$[0].nombre").value("Cervecería Mosto")`; (5) `nuevo_retornaFormulario` — ADMIN, GET /clientes/nuevo → 200, view "clientes/formulario", `model().attributeExists("cliente","listasPrecio","regimenes")`; (6) `ver_retornaDetalle` — ADMIN, GET /clientes/ver/1; `buscarPorId(1L)` devuelve cliente con nombre "Distribuidora Norte"; verifica 200, view "clientes/detalle", `model().attributeExists("cliente")`; (7) `guardar_nitDuplicado_redirige` — ADMIN, POST /clientes/guardar con CSRF y param `nombre="Cliente Test"`; `doThrow(RuntimeException("NIT ya registrado para otro cliente")).when(clienteService).guardar(any())`; verifica 3xx redirect a "/clientes" y `flash().attribute("tipoMensaje","danger")`; (8) `toggle_redirige` — ADMIN, POST /clientes/1/toggle con CSRF → 3xx redirect a "/clientes".
- `MigracionControllerTest` — 3 tests: `detalle_sinAuth_retorna401` (GET /admin/migracion/mosto sin auth → 401), `detalle_tenantExiste_retorna200` (ADMIN + tenant existente → 200, view "admin/migracion/detalle"), `detalle_noExiste_redirige` (ADMIN + tenant inexistente → 3xx redirect a /admin/tenants + flash `tipoMensaje=danger`). `@MockBean MigracionTemplateService` y `@MockBean MigracionService` adicionales. Stub `@BeforeEach`: `migracionService.historial(anyString())` → `List.of()`.
- `BarrilControllerTest` — 8 tests: `lista_sinAuth_retorna401` (GET /barriles sin auth → 401), `lista_conAdmin_retorna200` (ADMIN → 200, view "barriles/lista", model tiene `barriles`, `estados`, `statsTotal`), `lista_conInventario_retorna200` (INVENTARIO → 200), `nuevo_retornaFormulario` (GET /barriles/nuevo → 200, view "barriles/formulario", model tiene `barril`, `tiposBarril`, `estados`), `ver_retornaDetalle` (GET /barriles/ver/1 → 200, model tiene `barril`, `movimientos`, `estados`), `ver_noExiste_redirige` (GET /barriles/ver/99 → 3xx + flash danger), `guardar_codigoVacio_retornaFormulario` (POST codigo vacío → formulario), `cambiarEstado_redirigaAlDetalle` (POST /barriles/1/estado → redirect `/barriles/ver/1`). `@MockBean BarrilService` adicional. `@BeforeEach` stubs: `listarPaginado(any(),any(),anyInt())` → `PageImpl(List.of())`, `countTotal()` → 0L, `countByEstado(any())` → 0L.
- `WebMvcTestHelper` — utilidad con `configureTenantMock(TenantRepository)` que configura el tenant "default" con colores válidos para que TenantFilter resuelva correctamente en el test context

**@WebMvcTest — mocks requeridos** (todos los tests de controlador necesitan estos `@MockBean`):
- `TenantRepository` — SecurityConfig crea TenantFilter que lo inyecta; sin mock → TenantFilter devuelve 503
- `BrandingProperties` — GlobalControllerAdvice la inyecta como fallback; sin mock → contexto no carga
- `ZymosAuthSuccessHandler`, `ZymosAuthFailureHandler`, `ZymosAccessDeniedHandler` — SecurityConfig.filterChain() los recibe como parámetros; sin mock → Spring usa la seguridad por defecto (sin URL-based restrictions)
- `LoginAttemptService` — requerido por `LoginAttemptFilter` (bean en SecurityConfig); sin mock → contexto no carga. **CRÍTICO**: NO mockear `LoginAttemptFilter` directamente (es creado por SecurityConfig vía `@Bean`, no auto-detectado). Mockear `LoginAttemptService` para que el filtro real pueda ser creado con la dependencia satisfecha.
- `JwtService` — requerido por `JwtFilter` (bean en SecurityConfig); sin mock → contexto no carga. Mismo patrón que `LoginAttemptService`. **CRÍTICO**: NO mockear `JwtFilter` directamente.
- `UsuarioService`, `LogAccesoService` — requeridos por los auth handlers y DaoAuthenticationProvider
- `PasswordEncoder` — si el controller lo inyecta directamente (ej: `TenantAdminController`), agregar `@MockBean PasswordEncoder`
- **NO mockear `ObjectMapper`**: Spring Boot lo autoconfigura en `@WebMvcTest`. Mockearlo hace que `routerFunctionMapping` falle al crear (`objectMapper.reader()` retorna null). Si el controller usa `ObjectMapper`, usa el bean autoconfigurdo directamente.
- **Comportamiento de seguridad en @WebMvcTest**: con `httpBasic()` configurado, requests sin autenticar devuelven `401` (no `302`). Los handlers mockeados (void, no-op) no comiten la respuesta → URL-based security no se enforce plenamente → las pruebas de seguridad URL-based verifican que el controller SE EJECUTA (no que SE BLOQUEA). La seguridad URL-based real se verifica en tests de integración.

**@WebMvcTest — Java 26 + Byte Buddy**: el proyecto corre en JVM 26 y Byte Buddy (bundled con Mockito) solo soporta oficialmente hasta Java 24. El `maven-surefire-plugin` tiene configurado `<argLine>-Dnet.bytebuddy.experimental=true</argLine>` y `<systemPropertyVariables><net.bytebuddy.experimental>true</net.bytebuddy.experimental></systemPropertyVariables>` para habilitar instrumentación experimental en JVM 26.

**Integración** (`src/test/java/com/alera/`) — Testcontainers + `postgres:16-alpine`:
- `AbstractIntegrationTest` — base con `@ServiceConnection` (Spring Boot 3.4). **NO usa `@Testcontainers` ni `@Container`** — en su lugar arranca el contenedor en un `static { POSTGRES.start(); }`. Esto evita que Testcontainers detenga y reinicie el contenedor entre clases de test, lo que causaría que el contexto Spring Boot cacheado intentara reconectar a un puerto que ya no existe. Perfil `test` con credenciales dummy (`DB_PASSWORD=test`).
- `FlywayMigrationIntegrationTest` — 4 tests sobre el estado Flyway en la BD Testcontainers: `todasLasMigracionesAplicadas` (cero pendientes — si falla, hay una migración nueva sin aplicar), `hayMigracionesAplicadas` (applied no vacío), `ningunaMigracionFallida` (ningún estado `isFailed()` en `info.all()`), `conteoMigracionesIncluyeV1HastaV19` (applied.length ≥ 19 — umbral conservador; actualmente se aplican V1–V52). Inyecta `Flyway flyway` directo vía `@Autowired`.
- `LoteCervezaRepositoryIntegrationTest` — 6 tests `@Transactional` (rollback automático por test) sobre BD vacía. Smoke tests de queries clave: `countQueriesRetornanCeroEnBdVacia` (`countEnProceso`, `countCompletados`, `countDistinctEstilos` → 0), `findByFiltrosConParametrosVaciosRetornaPaginaVacia` (parámetros `""` y null, página vacía no nula), `findTop5SinDatosRetornaListaVacia`, `findLitrosPorMesNoLanzaExcepcion` (native query con `tenantId="default"` y fecha desde hace 6 meses — verifica que no lanza excepción), `findLotesPorEstiloNoLanzaExcepcion` (idem, verifica resultado no null y vacío), `findParaKanbanNoLanzaExcepcion` (fecha `now()-7d`). El objetivo principal es garantizar que las native queries (casting, filtros de tenant explícitos) son válidas en PostgreSQL real.
- `TrazabilidadServiceIntegrationTest` — 5 tests con `@Transactional` a nivel de clase (rollback por test). `@BeforeEach TenantContext.setCurrentTenant("default")` y `@AfterEach TenantContext.clear()` — sin esto `generarCodigo()` pasa `null` a la native query de secuencia y todos los lotes colisionan con el mismo código. `@BeforeTransaction` (corre FUERA de la transacción del test, auto-commit) limpia lotes de tests anteriores por prefijo: `DELETE FROM lotes_cerveza WHERE codigo_lote LIKE 'IND-%' OR 'STO-%' OR 'POR-%' OR 'LAG-%' OR 'WHE-%'` — evita `UniqueConstraintViolation` en reruns. Tests: (1) `guardarLoteGeneraCodigoCorrecto` — estilo "India Pale Ale", verifica `codigoLote.startsWith("IND-")` e `id > 0`; (2) `guardarDosLotesMismoEstiloGeneraCodigosConsecutivos` — estilo "Stout" dos veces, verifica `"STO-001"` y `"STO-002"` respectivamente; (3) `guardarLoteConIngredientesLosPersiste` — estilo "Porter", añade `InsumoDto` "Malta Pilsen" a `dto.getMaltas()`, luego `buscarPorId()` verifica que el ingrediente fue persistido con el nombre correcto; (4) `eliminarLoteLoBorraDeBaseDeDatos` — estilo "Lager", `existsById(loteId)` retorna `true` antes de `service.eliminar()` y `false` después — el soft delete setea `deletedAt` y `@SQLRestriction("deleted_at IS NULL")` hace que JPA no lo encuentre aunque la fila siga físicamente en BD; (5) `historialRegistraCreacionYEliminacion` — estilo "Wheat", tras `guardar()` el historial tiene 1 entrada `"CREADO"`; tras `eliminar()` tiene 2 entradas y la primera es `"ARCHIVADO"` (no "ELIMINADO") — confirma que el historial persiste tras la eliminación porque `HistorialLote` no tiene FK a `lotes_cerveza`.
- `PlanificacionServiceIntegrationTest` — 9 tests con `@Transactional` a nivel de clase (rollback por test). `@BeforeEach TenantContext.setCurrentTenant("default")` y `@AfterEach TenantContext.clear()`. Helper privado `buildPlan(nombre, fecha, volumen)` construye `ElaboracionPlanificada` sin persistir. Tests — Guardar: (1) `guardarPlanLoGuardaConEstadoPlanificadaPorDefecto` — nombre "IPA Test", fecha +7 días, verifica `findAllOrdenadas()` hasSize(1) y estado `PLANIFICADA`; (2) `guardarPlanConVolumenLoPersisteCorrectamente` — nombre "Stout Imperial", volumen `20.5`, verifica `isEqualByComparingTo("20.5")`; (3) `guardarDosPlanesMismoNombreLosPersisteSeparados` — dos planes "APA" con fechas distintas, verifica `hasSize(2)` (no hay constraint de unicidad por nombre). Cambiar estado: (4) `cambiarEstadoActualizaCorrectamente` — "Wheat", PLANIFICADA → EN_PROCESO, verifica con `buscarPorId()`; (5) `flujoCompletoEstados` — "Lager", PLANIFICADA → EN_PROCESO → COMPLETADA, verifica estado en cada paso; (6) `cancelarPlanActualizaEstado` — "Porter", → CANCELADA. Listar: (7) `listarProximasRetornaDesdeAyerEnAdelante` — crea 3 planes (pasado -5 días, hoy, futuro +3 días); `listarProximas()` usa `findProximas(now().minusDays(1))` → devuelve 2 (hoy y futuro), excluye "Plan Pasado"; (8) `listarPorRangoRetornaSoloElRangoIndicado` — crea "Fuera de rango" (+2 meses) y "Dentro del rango" (+7 días); `listarPorRango(now(), now().plusMonths(1))` devuelve solo 1. Eliminar: (9) `eliminarPlanLoEliminaDeBaseDeDatos` — "Para Eliminar", `repo.findById()` present antes, empty después — **borrado físico** (no soft delete, a diferencia de `LoteCerveza`).
- `LecturaFermentacionServiceIntegrationTest` — 10 tests con `@Transactional` a nivel de clase (rollback por test). `@BeforeEach TenantContext.setCurrentTenant("default")` y `@AfterEach TenantContext.clear()`. Inyecta `TrazabilidadService` además del `LecturaFermentacionService`. Helper privado `crearLote(estilo)` usa `LoteFormDto.empty()` + `trazService.guardar(dto).getLote()` — crea el lote anfitrión en la misma transacción. Tests — Agregar: (1) `agregarLecturaLaPersisteCorrectamente` — "Porter", densidad 1040, temp 18.5, notas "Primera medición"; verifica los 3 campos vía `listarPorLote()`; (2) `agregarLecturaSinTemperaturaPermiteNull` — "IPA Sin Temp", temp `null`; verifica `getTemperatura()` null y densidad 1055; (3) `agregarLecturaSinDensidadPermiteNull` — "Stout Solo Temp", densidad `null`, temp 20.0; verifica `getDensidad()` null; (4) `notasBlancoSeSanitizanANull` — "Sanitize Test", notas `"   "` (solo espacios); verifica que `getNotas()` retorna null (el servicio normaliza blancos con `isBlank()`). Ordenamiento: (5) `lecturasSeOrdenanPorFechaAscendente` — "Orden Test", agrega 3 lecturas fuera de orden (10-mar, 1-mar, 5-mar); verifica que `listarPorLote()` las devuelve en orden ASC: 1-mar → 5-mar → 10-mar. ABV parcial: (6) `abvParcialCalculaCorrectamenteConOgDelLote` — crea lote con `densidadInicial=1060`, agrega lectura con densidad 1020; verifica `getAbvParcial(1060)` = 5.25 (fórmula: `(1060-1020)*131.25/1000`); (7) `abvParcialRetornaNullSiDensidadLecturaEsNull` — lectura con densidad null; verifica `getAbvParcial(1060)` null; (8) `abvParcialRetornaNullSiLecturaIgualOG` — lectura densidad 1060 = OG 1060; verifica null (condición `densidad >= ogLote → null`). Eliminar: (9) `eliminarLecturaLaEliminaDeBaseDeDatos` — "Eliminar Test", obtiene id de la lectura persitida, llama `eliminar(id)`, verifica `listarPorLote()` vacío — **borrado físico**; (10) `eliminarUnaLecturaNoAfectaLasOtras` — "Multi Lectura", 2 lecturas (1-abr densidad 1058, 5-abr densidad 1040); elimina la primera (id de `listarPorLote().get(0)`); verifica que quedan 1 y su densidad es 1040.
- `MigracionServiceIntegrationTest` — 9 tests usando tenant aislado `"mig-test"` con cleanup JDBC en `@AfterEach`. Crea archivos Excel programáticamente con `XSSFWorkbook`. Cubre: `importarAlmacen` (happy path 2 insumos, tipo inválido PARCIAL, nombre vacío silenciosamente ignorado, log guardado); `importarEquipos` (estado OPERATIVO por defecto); `importarComercial` (proveedor+factura+ítem con subtotal, proveedor duplicado skip idempotente); `importarProduccion` (receta+escalón+lote, código duplicado reporta error). **NOTA**: filas con primera celda vacía/blank son saltadas por `vacio(row,0)` antes de incrementar `total` — no se cuentan como errores. `stock_minimo NOT NULL DEFAULT 0` requiere pasar `BigDecimal.ZERO` cuando es null (PostgreSQL rechaza null explícito aunque exista DEFAULT). **Tipo conversion**: los tests pasan tipo en mayúsculas (`"LUPULO"`, `"MALTA"`) — `MigracionService` valida el enum name y lo convierte a display name (`"Lúpulo"`, `"Malta"`) vía `TipoInsumo.valueOf(tipo).getDisplayName()` antes de insertar. Esto requiere que la V46 haya eliminado el CHECK constraint `insumos_inventario_tipo_check` de V36 (que solo aceptaba enum names). **Helpers**: `excelConHoja(sheetName, data[][])` crea las filas 0-2 vacías y los datos desde fila 3; `excelConHojasComercial` y `excelConHojasProduccion` crean workbooks multi-hoja en el mismo patrón; `agregarHoja` reutilizable interno. `@BeforeEach` fija `TenantContext` a `"default"` (no a `"mig-test"`) — el tenantId para las inserciones se pasa explícitamente a los métodos del servicio.
- `TenantIsolationIntegrationTest` — 6 tests que verifican aislamiento de datos entre tenants. Constantes `TENANT_A = "test-iso-a"` y `TENANT_B = "test-iso-b"`. **Sin `@Transactional` en el test** (ver regla 41) — cada repo call crea su propio `EntityManager` que captura `TenantContext` en ese momento. Patrón inline: `setCurrentTenant → acción → clear()` dentro de cada test (no en `@BeforeEach`). `@AfterEach`: `TenantContext.clear()` + `JdbcTemplate` DELETE explícito por `tenant_id IN (TENANT_A, TENANT_B)` — bypass al filtro `@TenantId` para no depender del contexto en teardown. Tests — Aislamiento automático via `@TenantId` en `TipoCerveza`: (1) `tipoCerveza_noEsVisibleDesdeOtroTenant` — guarda "IsolTestIPA" en TENANT_A; verifica `findByNombreIgnoreCase` retorna `empty()` desde TENANT_B y `isPresent()` desde TENANT_A; (2) `existsByNombre_soloDelTenantActivo` — guarda "IsolExistsStout" en TENANT_A; verifica `existsByNombreIgnoreCase` retorna `false` desde TENANT_B y `true` desde TENANT_A; (3) `count_soloDelTenantActivo` — inserta 3 tipos en TENANT_A y 2 en TENANT_B; verifica `count()` retorna 3 para A y 2 para B — el `count()` de Spring Data respeta el filtro `@TenantId`. Aislamiento automático via `@TenantId` en `Usuario`: (4) `usuario_findByUsername_soloDelTenantActivo` — usa `insertarConTenant` (native SQL, sin TenantContext) para insertar "isol-user-a" en TENANT_A; verifica `findByUsername` (JPQL con `@TenantId`) retorna `empty()` desde TENANT_B y `isPresent()` desde TENANT_A. Queries nativas cross-tenant (admin): (5) `findAllByTenantId_retornaSoloElTenantCorrecto` — inserta "isol-admin-a" en TENANT_A y "isol-inv-b" en TENANT_B; `findAllByTenantId(TENANT_A)` tiene size 1 con username "isol-admin-a"; `findAllByTenantId(TENANT_B)` tiene size 1 con username "isol-inv-b"; además verifica cruzadamente que la lista de A no contiene usuarios de B y viceversa; (6) `countByUsername_respetaTenantExplicito` — inserta "isol-count-user" en TENANT_A; `countByUsernameAndTenantId("isol-count-user", TENANT_A)` = 1; con TENANT_B = 0.
- **NOTA multi-tenant en tests de integración**: los tests deben llamar `TenantContext.setCurrentTenant("default")` en `@BeforeEach` y `TenantContext.clear()` en `@AfterEach` para que Hibernate pueda filtrar/insertar correctamente con el tenant discriminador. **NUNCA poner `@Transactional` en tests de aislamiento multi-tenant** — ver regla 41.

**Workaround Docker Desktop 4.74 + WSL2** (`src/test/java/com/alera/WindowsDockerStrategy.java`):
- Docker Desktop 4.74 con backend WSL2 devuelve HTTP 400 con `ServerVersion:""` para cualquier API Docker < 1.40 en el endpoint `/info` desde procesos Windows.
- Testcontainers 1.20.6 hardcodea `VERSION_1_32` en la validación interna (`getDockerClient()` → `getClientForConfig()` → `withApiVersion(VERSION_1_32)`), causando `BadRequestException` al arrancar.
- `WindowsDockerStrategy` sobreescribe `test()` (valida vía HTTP directo a `/v1.40/info`) y `getDockerClient()` (crea cliente con `RemoteApiVersion.VERSION_1_40` vía TCP `127.0.0.1:2375`).
- Se activa en `~/.testcontainers.properties`: `docker.client.strategy=com.alera.WindowsDockerStrategy`
- Docker Desktop debe tener habilitado: **Settings → General → Expose daemon on tcp://localhost:2375 without TLS**

Ejecutar: `mvn test` (requiere Docker Desktop corriendo con daemon TCP habilitado) — 467 tests, BUILD SUCCESS
Perfil test: `src/test/resources/application-test.properties` (credenciales dummy + flags de test)

---

## ARCHIVOS DE PRUEBA — MIGRACIÓN

Archivos Excel listos para subir en `/admin/migracion/{subdomain}`. Ubicación: `C:\Users\Juancho\IdeaProjects\BD\Migracion\`
Generados con `generar_pruebas3.py` (requiere `openpyxl`). Estructura idéntica a las plantillas (fila 0=cabecera, 1=leyenda, 2=ejemplo, 3+=datos).

| Archivo | Módulo | Contenido |
|---|---|---|
| `prueba_almacen.xlsx` | Almacén | 25 insumos: maltas, lúpulos, levaduras, clarificantes, agentes de carbonatación, envases, agua, químicos |
| `prueba_equipos.xlsx` | Equipos | 16 equipos: fermentadores, ollas de macerado/hervor, enfriadores, bombas, filtro, medidores de pH, densímetros, báscula, compresor |
| `prueba_comercial.xlsx` | Comercial | 7 proveedores · 10 facturas (2024–2025, estados mixtos RECIBIDA/VERIFICADA/PAGADA) · 22 ítems con IVA 19% |
| `prueba_produccion.xlsx` | Producción | 6 recetas completas con escalones de macerado y adiciones de hervor · 9 lotes con carbonatación natural y forzada |
| `prueba_clientes.xlsx` | Clientes | 10 clientes: bares, distribuidoras, tiendas, restaurantes, exportación — con NITs que coinciden con los usados en `prueba_ventas.xlsx` |
| `prueba_ventas.xlsx` | Ventas | 12 ventas (DESPACHADO/PENDIENTE) · 16 ítems en 2 hojas (Ventas + Venta_Items) — códigos de lote coinciden con `prueba_produccion.xlsx` |

**Orden de importación recomendado** (respetar dependencias entre módulos):
1. `prueba_almacen.xlsx` → `/importar/almacen`
2. `prueba_equipos.xlsx` → `/importar/equipos`
3. `prueba_comercial.xlsx` → `/importar/comercial`
4. `prueba_produccion.xlsx` → `/importar/produccion` (crea los lotes referenciados en ventas)
5. `prueba_clientes.xlsx` → `/importar/clientes` (crea los NITs que resuelven las ventas)
6. `prueba_ventas.xlsx` → `/importar/ventas`

**Formato del campo `tipo` en cada módulo**:
- **Almacén** (`prueba_almacen.xlsx`): la columna `tipo` usa nombres de enum uppercase: `MALTA`, `LUPULO`, `LEVADURA`, `CLARIFICANTE`, `AGENTE_CARBONATACION`, `AGUA`, `QUIMICO`, `ENVASE`, `OTRO`. `MigracionService.importarAlmacen()` valida el enum y convierte automáticamente al nombre display ("Lúpulo", "Malta"...) antes de insertar en BD.
- **Equipos** (`prueba_equipos.xlsx`): la columna `tipo` usa nombres de enum uppercase: `FERMENTADOR`, `OLLA_MACERADO`, `OLLA_HERVOR`, `ENFRIADOR`, `BOMBA`, `FILTRO`, `MEDIDOR_PH`, `DENSIMETRO`, `BASCULA`, `COMPRESOR`, `OTRO`. `MigracionService.importarEquipos()` convierte a display name antes de insertar.
- **Comercial** (`prueba_comercial.xlsx`): la columna `tipo_insumo` en la hoja `Factura_Items` usa nombres de enum uppercase (`MALTA`, `LUPULO`...). `MigracionService.importarComercial()` los convierte automáticamente al display name ("Malta", "Lúpulo"...) vía `TIPO_INSUMO_DISPLAY`/`TIPO_EQUIPO_DISPLAY` antes de insertar en BD (alineado con V47 que migró `factura_items.tipo_insumo/tipo_equipo` a display names).
- **Producción** (`prueba_produccion.xlsx`): la columna `tipo` en `Receta_Ingredientes` y `Lote_Ingredientes` usa nombres de enum upstream de `TipoIngrediente` — se almacenan tal cual.
- **Clientes** (`prueba_clientes.xlsx`): `regimen_tributario` usa enum names (`SIMPLIFICADO`/`RESPONSABLE_IVA`) y `lista_precio` usa enum names (`BAR`, `DISTRIBUIDOR`, `MAYORISTA`, etc.) — almacenados directamente en BD via `@Enumerated(EnumType.STRING)`.
- **Ventas** (`prueba_ventas.xlsx`): `estado` usa valores del enum `EstadoVenta` (`DESPACHADO`, `PENDIENTE`, `COTIZACION`, `CANCELADO`). `cliente_nit` se resuelve a `cliente_id` primero; si no encuentra, cae a `cliente_nombre` como fallback. `codigo_lote` en `Venta_Items` se resuelve a `lote_id` de forma tolerante (deja null si el lote no existe).

**Dependencias entre módulos**: los lotes en `prueba_produccion.xlsx` referencian las recetas del mismo archivo (se resuelven por nombre en el orden de hojas). Las facturas en `prueba_comercial.xlsx` referencian los proveedores de la hoja "Proveedores" — se procesan en orden. Los ítems de `prueba_ventas.xlsx` referencian los códigos de lote de `prueba_produccion.xlsx` y los NITs de `prueba_clientes.xlsx`.

**Casos borde incluidos**: un equipo en estado `MANTENIMIENTO`, una receta inactiva (Imperial Stout), un lote con carbonatación `SOBRECARBONATADA`, lotes con y sin receta asociada, facturas en todos los estados (RECIBIDA/VERIFICADA/PAGADA), ventas con descuento, una venta en estado PENDIENTE, un cliente persona natural (sin NIT — resolución por nombre).

**Notas sobre `generar_pruebas3.py`**: versión actual (2026-06-04), genera los 6 archivos de prueba para todos los módulos. Versiones anteriores: `generar_pruebas2.py` (29/05/2026), `generar_pruebas1.py` (27/05/2026). Requiere `pip install openpyxl`. Ejecutar desde el directorio `C:\Users\Juancho\IdeaProjects\BD\Migracion\`.