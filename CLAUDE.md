# Alera — Sistema de Gestión de Trazabilidad de Cerveza Artesanal

Alera es una aplicación web desarrollada con Spring Boot 3.4.4 + Java 21 (runtime Java 26) + Thymeleaf + PostgreSQL.
Sistema de gestión integral para una cervecería artesanal.
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
- Usuarios adicionales por rol (opcionales): `INVENTARIO_USERNAME/PASSWORD`, `FACTURACION_USERNAME/PASSWORD`, `EQUIPOS_USERNAME/PASSWORD`
- Flyway: `baseline-on-migrate=true`, migraciones en `db/migration/` (V1–V26)
- Sesión: timeout 30 minutos de inactividad (`server.servlet.session.timeout=30m`)
- Docker: `Dockerfile` + `docker-compose.yml` disponibles en raíz del proyecto
- Actuator: `GET /actuator/health` (público), `/actuator/**` solo ADMIN
- Swagger UI: `GET /swagger-ui.html` (requiere autenticación)
- Paginación configurable: `app.page-size=15` (servicios), `app.log-page-size=25` (LogAccesoService)
- Perfil prod: `application-prod.properties` — elimina fallbacks de credenciales BD. Docker activa `SPRING_PROFILES_ACTIVE=prod`.
- **Multi-tenant**: `app.default-subdomain=${DEFAULT_SUBDOMAIN:default}` — subdomain usado en localhost y como tenant inicial
- **Branding por tenant** (env vars con fallback): `APP_BRAND_NAME`, `APP_BRAND_TAGLINE`, `APP_BRAND_LOGO_URL`, `APP_BRAND_COLOR_NAVBAR`, `APP_BRAND_COLOR_PRIMARY`, `APP_BRAND_COLOR_ACCENT`, `APP_BRAND_COLOR_ACCENT_HOVER`, `APP_BRAND_COLOR_CREAM`, `APP_BRAND_COLOR_BODY_BG`, `APP_BRAND_FONT_HEADINGS` (def: Cinzel), `APP_BRAND_FONT_BODY` (def: Raleway)
- **Email/Alertas** (opcionales — si no se definen, las notificaciones quedan deshabilitadas): `SMTP_HOST`, `SMTP_PORT` (def: 587), `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH` (def: true), `SMTP_STARTTLS` (def: true), `SMTP_FROM` (def: noreply@alera.app), `APP_BASE_URL` (def: http://localhost:8080), `ALERT_CRON` (def: `0 0 8 * * MON-FRI`), `ALERT_VENCIMIENTO_DIAS` (def: 30), `FACTURAS_ALERTA_DIAS` → `app.facturas.alerta-dias` (def: 30) — días sin procesar para disparar alerta de facturas RECIBIDA/VERIFICADA
- **Protección contra fuerza bruta**: `LOGIN_MAX_INTENTOS` (def: 5), `LOGIN_BLOQUEO_MINUTOS` (def: 15)
- **JWT API**: `JWT_SECRET` (obligatorio en prod — sin fallback en `application-prod.properties`; en dev usa `alera-dev-secret-key-change-in-production-2024`), `JWT_TTL_HOURS` (def: 24). Configurado en `app.jwt.secret` y `app.jwt.ttl-hours`.

---

## PALETA DE COLORES (style.css)

- Verde oscuro: `#242E0D` (navbar) — CSS var `--verde-oscuro`
- Verde Alera: `#364318` (hero, section-headers) — CSS var `--verde-alera`
- Dorado: `#C9A028` (acento principal) — CSS var `--dorado`
- Dorado claro: `#E0B840` (hover) — CSS var `--dorado-claro`
- Crema: `#F5EDD0` (texto sobre fondos oscuros) — CSS var `--crema`
- Fondo body: `#F0EDE2` — CSS var `--fondo`
- Dark mode: fondo `#111606`, cards `#1c2410`, texto crema — activado con clase `html.dark-mode`
- Componentes clave: `.card-alera`, `.hero-section`, `.stat-card`, `.phase-badge`, `.detail-label`, `.detail-value`, `.ingrediente-chip`, `.densidad-box`, `.fase-col`, `.comparativa-box`, `.kanban-card`, `.kanban-col`
- **Multi-tenant**: el navbar fragment inyecta `<style th:inline="text">:root { --verde-oscuro: [[${branding.colorNavbar}]]; ... }</style>` que sobrescribe las variables CSS del archivo. `login.html` hace lo mismo en su `<head>`. Los templates NO cambian — siguen usando `${branding.*}` y las CSS vars son transparentes.

---

## ESTRUCTURA DE PAQUETES

```
com.alera/
├── config/     SecurityConfig, JpaConfig (@EnableJpaAuditing), AuditorAwareImpl,
│               DataInitializer, GlobalExceptionHandler, GlobalControllerAdvice, UnidadUtils,
│               CacheConfig (@EnableCaching + Caffeine), SchedulingConfig (@EnableScheduling),
│               OpenApiConfig (Swagger),
│               AleraAuthSuccessHandler, AleraAuthFailureHandler, AleraAccessDeniedHandler,
│               LoginAttemptService (protección fuerza bruta — cache Caffeine por IP),
│               LoginAttemptFilter (OncePerRequestFilter — creado como @Bean en SecurityConfig, no @Component),
│               PasswordPolicy (utilidad estática — MIN_LENGTH=8, requiere letra + número; `validar(pwd)` retorna null si OK o mensaje de error),
│               BrandingProperties (@ConfigurationProperties prefix=app.brand),
│               TenantContext (ThreadLocal), TenantFilter (OncePerRequestFilter),
│               TenantIdentifierResolver (CurrentTenantIdentifierResolver<String>),
│               HibernateMultiTenancyConfig (HibernatePropertiesCustomizer),
│               JwtFilter (OncePerRequestFilter — valida Bearer tokens para /api/**; creado como @Bean en SecurityConfig, no @Component)
├── exception/  EquipoEnUsoException, LoteNoEncontradoException
├── controller/ 24 controladores:
│               TrazabilidadController, DashboardController, EquipoController,
│               FacturaProveedorController, InsumoInventarioController,
│               RecetaController, ProveedorController, CalendarioController,
│               ReporteController, BusquedaController, AdminController, ApiController,
│               TipoCervezaController, UsuarioController, MantenimientoController,
│               LoginController, TenantAdminController, ComparativaController, AlertaController,
│               PlanificacionController, PerfilController, NotificacionController,
│               AuthController (POST /api/auth/login — obtención de token JWT)
├── service/    TrazabilidadService, RecetaService, EquipoService, FacturaProveedorService,
│               InsumoInventarioService, ProveedorService, LogAccesoService,
│               DashboardService, MantenimientoEquipoService, TipoCervezaService,
│               UsuarioService (implements UserDetailsService — integración Spring Security),
│               TenantService, PdfExportService, ExcelExportService, LecturaFermentacionService, PlanificacionService,
│               EmailService, AlertaScheduler, NotificacionService,
│               JwtService (generación/validación tokens HS256 — secret via @Value, claims: subject=username, tenant, rol)
├── model/      23 entidades:
│               AuditableEntity (@MappedSuperclass — base de auditoría + @TenantId),
│               Tenant (tabla tenants — subdomain PK + branding),
│               LoteCerveza, Ingrediente, Receta, RecetaIngrediente, EscalonMacerado,
│               AdicionHervor, HistorialLote, LogAcceso, Equipo, MantenimientoEquipo,
│               InsumoInventario, FacturaProveedor, FacturaItem,
│               Proveedor, TipoCerveza, Usuario,
│               LoteItemFactura (tabla lote_items_factura — asignación parcial de ítems a lotes),
│               Notificacion (tabla notificaciones — notificaciones in-app persistentes por tenant),
│               FacturaHistorialEstado (tabla factura_historial_estado — auditoría de cambios de estado por factura)
│               + 10 enums (incluye RolUsuario: ADMIN, INVENTARIO, FACTURACION, EQUIPOS;
│               EstadoPlanificacion: PLANIFICADA, EN_PROCESO, COMPLETADA, CANCELADA;
│               EstadoFactura: RECIBIDA, VERIFICADA, PAGADA;
│               TipoNotificacion: BAJO_STOCK, VENCIMIENTO, MANTENIMIENTO, SISTEMA)
├── repository/ 14 repositorios JPA (+ TenantRepository, FacturaItemRepository, LecturaFermentacionRepository,
│               ElaboracionPlanificadaRepository, NotificacionRepository, FacturaHistorialEstadoRepository)
├── dto/        LoteFormDto, LoteGuardadoResult, InsumoDto, FacturaFormDto,
│               FacturaItemDto, MantenimientoDto, DashboardStats,
│               RecetaFormDto (incluye EscalonDto y AdicionHervorDto inner classes),
│               AlertaContadores (bajoStock, vencimientos, mantenimiento + getTotal() — devuelto por AlertaController),
│               AuthRequest (@NotBlank username + password — body de POST /api/auth/login),
│               AuthResponse (token, tipo="Bearer", expiresIn, username, rol — respuesta del login JWT)
└── mapper/     LoteMapper (MapStruct — LoteCerveza → LoteFormDto),
                MantenimientoMapper (MapStruct — MantenimientoDto → MantenimientoEquipo, ignora `id` y `equipo`)

templates/
├── fragments/  navbar.html (dropdowns Producción/Almacén/Comercial/Admin + botón `+` acciones rápidas + campana notificaciones in-app + búsqueda global con typeahead + dropdown usuario con rol badge + perfil), paginacion.html
├── error/      error.html
├── trazabilidad/ index.html (filtros con typeahead en campo "Estilo / Código" busca por codigoLote o estilo, badge de fase),
│               formulario.html, detalle.html (detalle incluye sección "Curva de Fermentación" con Chart.js dual-eje + tabla + formulario inline de registro de lecturas; JS de formulario y detalle en `static/js/`),
│               kanban.html (SortableJS 1.15.2 — drag & drop entre 6 columnas; solo ADMIN puede arrastrar; JS en `static/js/trazabilidad-kanban.js`)
├── login.html, dashboard.html (personalizable), calendario.html, busqueda.html
├── usuarios.html  (tabla con modales: nuevo usuario, cambiar contraseña, cambiar rol; fila del usuario en sesión marcada y botones destructivos deshabilitados; typeahead en card-header, `th:id="'usuario-'+${u.id}"` en cada `<tr>`, click hace scroll+flash `:target` dorado)
├── perfil/     password.html (formulario autogestionado de cambio de contraseña — accesible todos los roles via `GET /perfil/password`)
├── equipos/    lista (typeahead en card-header respeta filtro estado), formulario, mantenimientos
├── inventario/ lista (typeahead en campo nombre respeta filtro tipo), formulario,
│               precios.html (buscador con datalist + 4 stat-cards + Chart.js barras + tabla de compras)
├── tipos-cerveza/ lista
├── facturas/   lista (typeahead en card-header busca por N° o proveedor; 4 stat-cards: total facturas, monto total, pendientes de pago, monto pendiente), formulario, detalle (historial de cambios de estado + botón Duplicar)
├── recetas/    lista (tabla paginada con filtros activa/inactiva + typeahead a la derecha; respeta filtro estado), formulario, detalle (+ calculadora escala)
├── proveedores/ lista (typeahead en card-header busca por nombre o NIT), formulario
├── reportes/   produccion.html
├── comparativa/ seleccion.html (tabla con checkboxes, filtro por código/estilo, máx. 6 lotes),
│               resultado.html (tabla transpuesta con métricas por columna + Chart.js grouped bar)
├── planificacion/ index.html (FullCalendar + panel próximas + tabla completa + modal crear/editar)
│               — dateClick → modal nuevo con fecha pre-llenada; eventClick → modal editar con extendedProps
│               — botón Editar en tabla usa `data-*` attrs (`th:attr`) + `onclick="abrirModalEditarDesdeBtn(this)"` para pasar strings sin violar restricción Thymeleaf 3.1 (regla 8c)
├── notificaciones/ index.html (historial paginado con badges por tipo, marcar leída por fila, marcar todas, paginación)
└── admin/      logs.html, tenants.html (lista de tenants con cards + franja de colores + botón "Limpiar cache" → `POST /admin/tenants/cache/evict` + botón "Usuarios" por card → `/admin/tenants/{subdomain}/usuarios`),
                tenant-formulario.html (crear/editar tenant con color pickers y preview en vivo del navbar + selectores de tipografía con preview en vivo — `fontHeadings` y `fontBody`; campo `logoUrl` es `type="text"` para aceptar rutas relativas `/img/` además de URLs externas),
                tenant-usuarios.html (gestión de usuarios por tenant: tabla con toggle activo/inactivo, cambiar contraseña, cambiar rol, eliminar + modal "Nuevo Usuario"; todas las queries usan SQL nativo explícito — ver regla 40),
                tenant-historial.html (auditoría de cambios del tenant: tabla fecha/acción/usuario/detalles; badges de color por tipo de acción),
                tenant-formulario.html (edición) incluye sección "Importar / Exportar": botón Exportar JSON, form upload Importar JSON, select "Copiar de..." + botón AJAX que llama `/config` y rellena el form con previews en vivo
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
- `fontHeadings` (VARCHAR 100, default `'Cinzel'`) — fuente de títulos y navbar. Opciones disponibles: Cinzel, Playfair Display, Cormorant Garamond, EB Garamond, Oswald, Montserrat, Bowlby One SC.
- `fontBody` (VARCHAR 100, default `'Raleway'`) — fuente de cuerpo. Opciones: Raleway, Inter, Roboto, Open Sans, Poppins, Nunito, DM Sans.
- `emailAdmin` (VARCHAR 200, nullable) — destinatario de alertas diarias. Si es null o vacío, el tenant no recibe emails.
- `active` (boolean) — tenants inactivos retornan 503
- `alertasIntentosFallidos` (INTEGER, NOT NULL, default 0) — contador de fallos SMTP consecutivos. Se incrementa en cada fallo, se resetea a 0 al enviar exitosamente. Visible en `/admin/tenants` como badge amarillo.
- `alertasUltimoIntento` (TIMESTAMP, nullable) — fecha/hora del último intento de envío (exitoso o fallido).
- `alertasUltimoExito` (TIMESTAMP, nullable) — fecha/hora del último envío exitoso.
- Creado por `DataInitializer` al arrancar. Al inicio, itera **todos los tenants** existentes en BD y crea usuarios/tipos de cerveza para los que no tengan ninguno. Si un tenant ya tiene usuarios, no se modifica.
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
- `volumenBase`, `notas`
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
- `FacturaProveedor`: `proveedor` (String original) + `@ManyToOne proveedorRef → Proveedor` (LAZY, nullable) — coexisten para compat. histórica. V10 backfill vincula automáticamente donde los nombres coincidan.
- **Campo `estado` en `FacturaProveedor`**: `@Enumerated(EnumType.STRING) EstadoFactura estado` — default `RECIBIDA`. Valores: `RECIBIDA` (badge gris), `VERIFICADA` (badge amarillo), `PAGADA` (badge verde). Cada valor tiene `getDisplayName()` y `getBadgeClass()` (clase Bootstrap). Se puede cambiar desde el detalle via `POST /facturas/{id}/estado` o desde el formulario de edición via select.
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

### Usuario
No extiende `AuditableEntity`. Gestiona su propia auditoría con `@PrePersist createdAt`. Campos:
- `id`, `tenantId` (@TenantId — usuarios aislados por tenant), `username` (unique por tenant)
- `password` — siempre BCrypt encodeado, nunca texto plano
- `@Enumerated(EnumType.STRING) RolUsuario rol` — enum type-safe. Valores válidos: `ADMIN`, `INVENTARIO`, `FACTURACION`, `EQUIPOS`. **No usar Strings libres.**
- `activo` (boolean, default true) — los usuarios inactivos no pueden autenticarse (`loadUserByUsername` lanza `UsernameNotFoundException` si `!activo`)
- `createdAt` (LocalDateTime) — seteado por `@PrePersist`
- **RolUsuario** (`com.alera.model.enums`): `ADMIN("Administrador")`, `INVENTARIO("Inventario")`, `FACTURACION("Facturación")`, `EQUIPOS("Equipos")`. Cada valor tiene `getDisplayName()` para mostrar en UI.
- **Multi-tenant**: `loadUserByUsername` filtra automáticamente por tenant activo (Hibernate añade `WHERE tenant_id = :current`). El mismo `username` puede existir en distintos tenants.

---

## REPOSITORIOS (queries clave)

### LoteCervezaRepository
- `findByFiltros(estilo, fase, desde, hasta, Pageable)` — filtros + paginación + rango de fechas; `desde`/`hasta` nullable con `IS NULL` en JPQL
- `findTop5(Pageable)`, `findByIdWithIngredientes(id)`
- `countDistinctEstilos()`, `countEnProceso()`, `countCompletados()`
- `countLotesActivosByEquipo(equipoId)`
- `findLitrosPorMes(desde)` — nativeQuery, usa `CAST(EXTRACT(...) AS integer)` (NO `::int`)
- `findLotesPorEstilo()`, `findParaKanban(limite)`, `findByPeriodo(desde, hasta)`
- `findResumenPorEstilo(desde, hasta)` — nativeQuery para reporte
- `findByRecetaId(recetaId)` — lotes elaborados con una receta
- `findByIds(List<Long> ids)` — `SELECT DISTINCT ... LEFT JOIN FETCH ingredientes WHERE id IN :ids` — para comparativa; DISTINCT evita filas duplicadas del join con colección
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

### ElaboracionPlanificadaRepository
- `findProximas(desde)` — elaboraciones con `fechaPlaneada >= :desde`, `LEFT JOIN FETCH receta`, orden ASC
- `findAllOrdenadas()` — todas las elaboraciones con `LEFT JOIN FETCH receta`, orden ASC por fecha
- `findByEstado(estado)` — filtrado por `EstadoPlanificacion`, `LEFT JOIN FETCH receta`
- `findByRangoFecha(desde, hasta)` — para el feed de eventos de FullCalendar (`BETWEEN`)
- `findByIdWithRecetaEIngredientes(id)` — `SELECT DISTINCT … LEFT JOIN FETCH receta r LEFT JOIN FETCH r.ingredientes` — carga el plan con receta e ingredientes en una sola query; necesario para pre-llenar el formulario de lote sin LazyInitializationException

### LecturaFermentacionRepository
- `findByLoteIdOrdenadas(loteId)` — `ORDER BY l.fecha ASC, l.id ASC`. Hibernate agrega filtro de tenant automáticamente vía `@TenantId`.

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
- Usado también por `TrazabilidadService.mapearDto()` para resolver ítems por ID al guardar lotes

---

## SERVICIOS (lógica de negocio)

### TrazabilidadService
- `listarPaginado(estilo, fase, page)` — sobrecarga sin fechas
- `listarPaginado(estilo, fase, desde, hasta, page)` — con rango de fechas
- `guardar/actualizar/eliminar` → registra historial + auditing JPA automático + `@CacheEvict` en las 3 caches del dashboard
- `listarParaKanban()` — lotes activos + completados últimos 7 días
- `moverFase(id, fase)` — cambia las fechas de fase del lote. **Avanzar**: setea `*FechaInicial` solo si era null (preserva fecha real de inicio); setea `*FechaFinal` de la fase que se deja solo si era null. **Retroceder**: limpia `*FechaInicial` y `*FechaFinal` de todas las fases posteriores a la destino, y `*FechaFinal` de la fase destino misma (el lote vuelve a estar "en curso" en esa fase). Comportamiento por destino: `sinIniciar` → limpia todo; `fermentacion` → preserva `fermFechaInicial`, limpia `fermFechaFinal` y todo lo posterior; `acondicionamiento` → cierra ferm, abre acond; `maduracion` → cierra ferm+acond, abre madur; `carbonatacion` → cierra ferm+acond+madur, abre carb; `completados` → cierra todas las fases. `@CacheEvict("dashboard-stats")` + registra `HistorialLote` con acción "EDITADO" y notas "Fase → {fase}". Valores válidos de `fase`: `sinIniciar`, `fermentacion`, `acondicionamiento`, `maduracion`, `carbonatacion`, `completados`.
- `obtenerHistorial(loteId)` → historial manual (complementa auditing JPA)
- `toLoteFormDto(lote)` — delega a `LoteMapper` (MapStruct). No hace mapeo manual.
- `suggest(q)` — busca por codigoLote o estilo via `loteRepo.search()`, retorna hasta 6 mapas con `{codigoLote, estilo, fase, completado, url}` — usado por `GET /suggest`
- Lanza `LoteNoEncontradoException` (HTTP 404) cuando no encuentra un lote — ya no usa `RuntimeException` genérica
- **CRÍTICO**: `@DateTimeFormat(iso=DATE)` en todos los `LocalDate` de `LoteFormDto`
- `pageSize` inyectado via `@Value("${app.page-size:15}")`
- Inyecta `FacturaItemRepository` (no `FacturaProveedorRepository`) — `mapearDto()` resuelve ítems por ID y construye `LoteItemFactura` con `cantidadAsignada`
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
- `toFormDto` parsea `cantidad` normalizada de vuelta a `{cantidad, unidad}` y mapea `adicionesHervor`
- `actualizar()` → limpia `ingredientes`, `escalones` **y `adicionesHervor`** antes de remapear
- `mapDtoToEntity()` → persiste `adicionesHervor` además de ingredientes y escalones
- `suggest(q, Boolean activa)` — filtra via `repo.search()` (limit 10) + stream filter por `activa` si no es null, retorna hasta 6 mapas con `{nombre, estilo, activa, url}` — usado por `GET /recetas/suggest`
- `pageSize` inyectado via `@Value("${app.page-size:15}")`

### InsumoInventarioService
- `buscarPorId(id)` — `Optional<InsumoInventario>`
- `buscarPorNombreExacto(nombre)` — delega a `repo.findByNombreExacto()`, usado para validar duplicados en quick-create
- `descontarIngrediente(nombre, cantidadTexto)` — retorna nombre si stock insuficiente, null si OK
- `restaurarIngrediente(nombre, cantidadTexto)` — suma cantidad de vuelta al inventario
- `listarBajoStock()`, `listarProximosAVencer(dias)`
- `listarPaginado(nombre, tipo, page)` — paginado con filtros opcionales; usado también por `/inventario/suggest`
- `detectarTipo(nombre)` — infiere `TipoInsumo` del nombre por palabras clave
- `parsearCantidad(texto)` — extrae BigDecimal del texto "5000 gr" → 5000

### ProveedorService / FacturaProveedorService
- `ProveedorService.contarFacturas/totalFacturas` — estadísticas para vista edición
- `ProveedorService.suggest(q)` — filtra en memoria sobre `findAllByOrderByNombreAsc()` por nombre o NIT, retorna hasta 6 mapas con `{nombre, nit, activo, url}` — usado por `GET /proveedores/suggest`
- `FacturaProveedorService` inyecta `ProveedorRepository` y `FacturaHistorialEstadoRepository` para vincular proveedor y registrar historial al guardar/cambiar estado
- `FacturaProveedorService.guardar/actualizar/eliminar` → `@CacheEvict("dashboard-stats")` — invalida caché al modificar datos financieros. `guardar()` además registra el estado inicial en `factura_historial_estado`.
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
- `getLitrosPorMes()` — datos para Chart.js — `@Cacheable("dashboard-litros-mes")` TTL 10 min
- `getLotesPorEstilo()` — datos para Chart.js — `@Cacheable("dashboard-estilos")` TTL 10 min
- `obtenerEstadisticas()` — 13 COUNT queries a nivel BD — `@Cacheable("dashboard-stats")` TTL 5 min
- Caché Caffeine configurada en `CacheConfig`: `dashboard-stats` (1 entrada, 5 min), `dashboard-litros-mes` y `dashboard-estilos` (1 entrada c/u, 10 min)
- Las 3 caches se invalidan automáticamente al crear/editar/eliminar lotes; `dashboard-stats` también al modificar facturas

### EquipoService
- `suggest(q, EstadoEquipo estado)` — filtra en memoria sobre `listarPorEstado(estado)` o `listarTodos()`, retorna hasta 6 mapas con `{nombre, tipo, estado, colorEstado, pendiente, url}` — usado por `GET /equipos/suggest`

### MantenimientoEquipoService
- `listarPorEquipo(equipoId)` — historial de mantenimientos ordenado por fecha desc
- `registrar(equipoId, dto)` — crea `MantenimientoEquipo` Y actualiza `equipo.fechaUltimoMantenimiento` y `equipo.proximoMantenimiento` en la misma transacción
- `eliminar(id)` — elimina registro de mantenimiento

### TipoCervezaService
- `listarActivos()` — `findByActivoTrueOrderByNombreAsc()` — para selects y datalists en formularios
- `listarTodos()` — todos incluyendo inactivos
- `existePorNombre(nombre)` — delega a `repo.existsByNombreIgnoreCase()`, usado en quick-create
- `guardar(tipo)` / `eliminar(id)` — CRUD básico
- `toggleActivo(id)` — invierte el flag `activo` sin borrar el tipo

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
- `generarPdfLote(LoteCerveza, String brandName, List<LecturaFermentacion>)` → `byte[]` — genera PDF A4 con OpenPDF. Secciones: encabezado, info del lote, parámetros/métricas, ingredientes, fases, **curva de fermentación** (si hay lecturas), costos, observaciones/notas de cata, pie de página. La curva usa **Java2D** (BufferedImage 2x → PNG → bytes → `Image.getInstance(bytes)`), evitando los problemas de tipo de `PdfTemplate` con OpenPDF. El gráfico muestra: eje Y izquierdo dorado (densidad) + eje Y derecho azul (temperatura °C, aparece solo si hay lecturas con temperatura), línea dorada sólida de densidad, línea azul sólida de temperatura, puntos de colores en cada lectura, línea verde punteada de FG real, etiquetas X de fecha (dd/MM), leyenda con ambas series. El margen derecho se expande automáticamente (8pt → 40pt) cuando hay temperatura. El X axis usa el rango de TODAS las lecturas (no solo las de densidad). Bajo el gráfico: tabla con columnas adaptativas (temperatura y notas solo aparecen si alguna lectura las tiene).
- Solo importa `com.lowagie.text.*` — sin colisión con POI.
- Inyectado en `TrazabilidadController`.

### ExcelExportService
- `generarExcelReporteProduccion(lotes, resumen, desde, hasta, brandName)` → `byte[]` — genera `.xlsx` con Apache POI. Dos hojas: hoja 1 con título, período, resumen estadístico, datos de lotes con autofilter; hoja 2 con producción agrupada por estilo. Filas alternas con fondo crema.
- `generarExcelFacturas(facturas, estadoFiltro, desde, hasta, brandName)` → `byte[]` — genera `.xlsx` de facturas. Dos hojas: hoja 1 "Facturas" con título, fila de filtros activos, fila de totales (N facturas, subtotal, IVA, total), tabla con 11 columnas (N°, proveedor, fecha, estado, ítems, subtotal, IVA, envío, total, notas, creada por) con autofilter; hoja 2 "Por Proveedor" con agrupación por nombre de proveedor (N facturas, total). Inyectado también en `FacturaProveedorController`.
- `generarExcelFacturas(facturas, estadoFiltro, desde, hasta, brandName)` → `byte[]` — genera `.xlsx` de facturas. Hoja 1 "Facturas": título, fila de filtros activos (estado + período), fila de resumen (count, subtotal, IVA, total general), 11 columnas con autofilter (N° factura, proveedor, fecha, estado, ítems, subtotal, IVA, envío, total, descripción, creado por). Hoja 2 "Por Proveedor": resumen agrupado por nombre de proveedor (count de facturas + total comprado). Filas alternas con fondo crema.
- Solo importa `org.apache.poi.*` — sin colisión con OpenPDF. Usa `XSSFFont` con cast `(XSSFFont) wb.createFont()`.
- Inyectado en `ReporteController`.

### EmailService
- `mailConfigurado()` → boolean — true si `JavaMailSender` fue auto-configurado (requiere `spring.mail.host` no vacío)
- `enviarAlertasDiarias(Tenant, bajoStock, proximosAVencer, mantenimientoPendiente)` → boolean — usa `SpringTemplateEngine` para renderizar `emails/alertas.html`, envía con `JavaMailSender`. Retorna false si: SMTP no configurado, `tenant.emailAdmin` vacío, o no hay alertas. Loggea error sin propagar excepción.
- `diasHasta(LocalDate)` → long — método estático auxiliar usado en el template Thymeleaf vía `T(com.alera.service.EmailService).diasHasta(...)`
- Usa `@Autowired(required = false)` para `JavaMailSender` — la app arranca sin SMTP configurado
- Variables de entorno: `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_FROM`, `APP_BASE_URL`

### AlertaScheduler (`@Component`)
- `@Scheduled(cron = "${app.alert.cron:0 0 8 * * MON-FRI}")` — lunes a viernes a las 8 AM por defecto. Configurable con `ALERT_CRON` env var.
- Itera **todos** los tenants activos (ya no filtra por `emailAdmin` — notificaciones in-app funcionan sin SMTP). Para cada uno: establece `TenantContext`, carga alertas, llama `NotificacionService.crearAlertas()` siempre, luego envía email solo si SMTP configurado y tenant tiene email. Limpia contexto en `finally`.
- **Notificaciones in-app**: se crean independientemente de SMTP — la app no necesita email configurado para generar notificaciones en la UI.
- **Facturas sin procesar**: tras las alertas de inventario/equipos, llama `facturaService.listarSinProcesar(facturaAlertaDias)` y pasa la lista a `notificacionService.crearAlertaFacturas()`. Configurable via `app.facturas.alerta-dias` (def: 30). Si hay facturas RECIBIDA/VERIFICADA con más de ese número de días, se crea una notificación SISTEMA deduplicada por día.
- **Tracking de fallos**: solo aplica al canal email. Si `enviarAlertasDiarias()` lanza excepción, llama `TenantService.registrarEnvioFallido()`. Si exitoso, `registrarEnvioExitoso()`. Las notifs in-app no afectan el tracking.
- **WARN escalado**: si `alertasIntentosFallidos >= UMBRAL_WARN (3)`, loggea WARN antes de cada intento de email.
- **EmailService**: `enviarAlertasDiarias()` relanza excepción SMTP como `RuntimeException` para que el scheduler pueda trackearla.
- Loggea resumen: "N notificación(es) in-app creada(s), M email(s) enviado(s) de K tenant(s)"
- Inyecta `NotificacionService` y `FacturaProveedorService`.

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

### LecturaFermentacionService
- `listarPorLote(loteId)` — `@Transactional(readOnly=true)`, delega a `findByLoteIdOrdenadas` (orden fecha ASC, id ASC)
- `agregar(loteId, fecha, densidad, temperatura, notas)` — crea `LecturaFermentacion`, vincula al lote via `loteRepo.findById`. `densidad` y `temperatura` son opcionales (null permitido). `notas` se normaliza a null si está en blanco.
- `eliminar(lecturaId)` — `repo.deleteById(lecturaId)`

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
- **Kanban drag & drop**: SortableJS 1.15.2 (CDN). Cada `.kanban-col-body` tiene `data-fase`, cada `.kanban-card` tiene `data-lote-id`. `group:'kanban'` permite mover entre columnas. `disabled:!esAdmin` — no-ADMIN solo visualiza. Al soltar: opacity 0.45 (saving), AJAX POST con CSRF, actualiza badges de conteo en cliente, revert DOM si falla. Toast propio (esquina inferior derecha, 2.8s) en verde/rojo. La columna Completados siempre visible (antes se ocultaba con `th:if` si estaba vacía — eliminado para permitir drop ahí). **JS en `static/js/trazabilidad-kanban.js`** — `kanban.html` solo inyecta `var esAdmin` via `th:inline="javascript"`; CSRF se lee del DOM en el archivo externo.
- `GET /suggest?q=` — `@ResponseBody`, `produces=JSON`. Busca lotes por codigoLote o estilo. Delega a `service.suggest(q)`. Devuelve `[{codigoLote, estilo, fase, completado, url}]`. Accesible todos los roles autenticados.
- `GET /duplicar/{id}`, `GET /ver/{id}` (+ historial), `GET /nuevo`, `POST /guardar` etc.
- `GET /nuevo?planId={id}` (opcional) — si `planId` está presente, carga la `ElaboracionPlanificada` con receta e ingredientes (via `buscarConRecetaEIngredientes`), pre-llena el `LoteFormDto` con: `estilo` ← `nombreElaboracion`, `fechaElaboracion` ← `fechaPlaneada`, `litrosFinales` ← `volumenEstimado`, `recetaId` ← `receta.id`, `densidadInicial/Final` ← `ogObjetivo/fgObjetivo`, listas de ingredientes (maltas/lúpulos/levaduras/clarificantes) parseadas desde `RecetaIngrediente.cantidad` ("5000 gr" → `{cantidad:"5000", unidad:"gr"}`). Cambia el estado de la planificación a EN_PROCESO al abrir el formulario. Método privado `toInsumoDtoList(List<RecetaIngrediente>)` hace el parseo.
- Inyecta `PlanificacionService` (nuevo). El test agrega `@MockBean PlanificacionService`.
- `GET /ver/{id}/pdf` — descarga PDF del detalle de lote. Lee el tenant del `request.getAttribute("currentTenant")` para el nombre del branding en el encabezado. Devuelve `application/pdf` con `Content-Disposition: attachment`. Botón "PDF" en `detalle.html`.
- `POST /ver/{id}/lecturas/agregar` — `@PreAuthorize("hasRole('ADMIN')")`. Params: `fecha` (DATE ISO), `densidad` (Integer, opcional), `temperatura` (BigDecimal, opcional), `notas` (String, opcional). Delega a `LecturaFermentacionService.agregar()`.
- `POST /ver/{id}/lecturas/{lecturaId}/eliminar` — `@PreAuthorize("hasRole('ADMIN')")`. Elimina una lectura por ID.
- `GET /ver/{id}` pasa al modelo: `lecturas` (List ordenada por fecha ASC), `chartFechas` (List<String>), `chartDensidad` (List<Integer>), `chartTemp` (List<BigDecimal>) — arrays paralelos para Chart.js. **JS en `static/js/trazabilidad-detalle.js`** — `detalle.html` inyecta `CHART_FECHAS`, `CHART_DENSIDAD`, `CHART_TEMP` via `th:inline="javascript"`; el archivo externo construye el gráfico leyendo esas variables globales.
- `agregarInventarioAlModelo()` — llama `insumoRepo.findAll()` una sola vez y filtra en memoria + llama `facturaRepo.findAllWithItems()` para construir `todosItemsFactura` (List<Map<String,Object>>) para el buscador de costos
- `todosItemsFactura` — lista plana de todos los ítems de todas las facturas con campos: `{id, nombre, tipoInsumo, unidad, cantidad, valorLinea, facturaId, facturaNumero, proveedor, fechaFactura}`. Serializada como JSON via `<script th:inline="javascript">` en el formulario.
- `formulario.html` — sección Costos de Producción: buscador en tiempo real sobre `ITEMS_FACTURA` JS, tabla de ítems asignados con cantidad editable, botón "Aplicar a Receta e Insumos" (auto-llena ingredientes por tipo y navega al tab 2). **JS en `static/js/trazabilidad-costos.js`** (costos) y **`static/js/trazabilidad-ingredientes.js`** (wizard, volumen, ingredientes, receta). El bloque `th:inline="javascript"` del template solo inyecta `ITEMS_FACTURA`, `INIT_IDS`, `INIT_CANTIDADES`.

### RecetaController ("/recetas")
- `GET /recetas?activa=true|false&page=N` — lista paginada con filtro opcional por estado activa
- `GET /recetas/suggest?q=&activa=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q, activa)`. El parámetro `activa` es opcional; si se omite busca en todas. Devuelve `[{nombre, estilo, activa, url}]`.
- CRUD completo + `GET /api/{id}` (@ResponseBody JSON)
- `GET /ver/{id}` — incluye `lotesDeReceta` (lotes elaborados con esa receta)
- `GET /nueva` y `GET /editar/{id}` — inyectan al modelo:
  - `insumosInventario` (List<InsumoInventario>) para datalists de ingredientes por tipo
  - `tiposCerveza` (List<TipoCerveza> activos) para datalist del campo Estilo
- Inyecta `InsumoInventarioService` y `TipoCervezaService`

### InsumoInventarioController ("/inventario")
- CRUD estándar
- `GET /inventario/suggest?nombre=&tipo=` — `@ResponseBody`, `produces=JSON`. Delega a `service.listarPaginado(nombre, tipo, 0)` (limit 6). El parámetro `tipo` es opcional (`TipoInsumo` enum). Devuelve `[{id, nombre, tipoNombre, colorTipo, bajoStock, url}]`. La template pasa el tipo seleccionado via `data-activa` para respetar el filtro activo.
- `POST /inventario/guardar-rapido` — `@ResponseBody` JSON. Crea insumo con stock 0 sin redirigir. Devuelve `{success, id, nombre}`. Accesible: ADMIN, INVENTARIO. Usado desde formularios de receta y factura vía AJAX + CSRF header.
- `GET /inventario/precios?nombre=X` — **Historial de precios** para el insumo con nombre X. Busca en `FacturaItem` por nombre (case-insensitive) via `findHistorialPreciosPorNombre`. Calcula: último precio, promedio, mínimo, máximo, variación (último vs primero), N compras, N proveedores. Pasa arrays `chartFechas`, `chartPrecios`, `chartProveedores` para Chart.js (barras). La fila más reciente se resalta en la tabla. Botón 📈 en `inventario/lista.html` abre directamente con el nombre del insumo. **Nota**: usa `fi.getFactura().getFechaFactura()` (no `getFecha()`) — campo correcto en `FacturaProveedor`.

### TipoCervezaController ("/tipos-cerveza") — solo ADMIN
- CRUD + toggle activo
- `POST /tipos-cerveza/guardar-rapido` — `@ResponseBody` JSON. Crea tipo de cerveza si no existe (valida con `existePorNombre`). Devuelve `{success, id, nombre}`. Usado desde formulario de receta vía AJAX.

### FacturaProveedorController ("/facturas")
- CRUD + `GET /ver/{id}`
- `GET /facturas?estado=RECIBIDA|VERIFICADA|PAGADA&desde=yyyy-MM-dd&hasta=yyyy-MM-dd` — filtros opcionales por estado y rango de fechas. Pasa `estadoFiltro`, `desde`, `hasta`, `estados` (enum values) y `extraParams` al modelo para que paginación, tabs y Excel respeten todos los filtros activos. El card principal permanece visible cuando cualquier filtro está activo (permite limpiar incluso sin resultados).
- `POST /facturas/{id}/estado` — cambia el estado de la factura. `@RequestParam EstadoFactura estado`. Redirige a `/facturas/ver/{id}` con flash success.
- `GET /facturas/export` — descarga `facturas-YYYY-MM-DD.xlsx`. Acepta filtros opcionales `?estado=`, `?desde=` (ISO date), `?hasta=` (ISO date). Lee el branding del tenant del `request.getAttribute("currentTenant")`. Delega a `ExcelExportService.generarExcelFacturas()`. El botón "Excel" en `lista.html` respeta todos los filtros activos.
- `GET /facturas/duplicar/{id}` — pre-llena el formulario de nueva factura con los datos de la factura original (mismo proveedor, ítems, descripción, envío) pero sin número ni fecha, y estado RECIBIDA. Usa `service.duplicarComoFormDto(id)`. No pasa `facturaId` al modelo — el submit va a `POST /facturas/guardar` (crea nueva, no edita).
- `GET /facturas/suggest?q=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q)`. Busca por N° factura o proveedor. Devuelve `[{titulo, proveedor, fecha, total, url}]`.
- `agregarDatosFormulario()` construye:
  - `insumosPorTipo` — `Map<String, List<String>>` agrupando nombres por `TipoInsumo.name()` para datalist JS
  - `equiposPorTipo` — `Map<String, List<String>>` agrupando nombres por `TipoEquipo.name()` para datalist JS
  - `estados` — `EstadoFactura.values()` para el select en el formulario de edición y las tabs de la lista
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
- `GET /api/v1/recetas` + `GET /api/v1/recetas/{id}`
- `GET /api/v1/inventario/alertas` + `GET /api/v1/dashboard`
- Autenticación: HTTP Basic, sesión, **o Bearer JWT** (obtenido de `POST /api/auth/login`)
- Anotado con `@Tag` y `@Operation` (SpringDoc) — documentado en `/swagger-ui.html`
- Lanza `LoteNoEncontradoException` → GlobalExceptionHandler devuelve HTTP 404
- **`produces = MediaType.APPLICATION_JSON_VALUE` a nivel de clase** — CRÍTICO: sin esto, un navegador que accede directamente con `Accept: text/html` hace que Spring negocie HTML, no puede serializar el `LinkedHashMap` devuelto y lanza `HttpMessageNotWritableException`. Con `produces`, el navegador recibe 406 en lugar de una excepción descontrolada.

### ReporteController ("/reportes")
- `GET /reportes/produccion?desde=&hasta=` — reporte con Chart.js y tabla de lotes
- `GET /reportes/produccion/excel?desde=&hasta=` — descarga `.xlsx` con dos hojas: "Reporte de Producción" (14 columnas: código, estilo, receta, fecha, fase, OG, FG, ABV, atenuación, eficiencia, litros, costo total, costo/litro, creado por) y "Por Estilo" (estilo, cantidad, litros). Botón "Excel" en `produccion.html`.

### CalendarioController ("/calendario")
- `GET /calendario` — template con FullCalendar
- `GET /calendario/eventos` — @ResponseBody JSON de eventos para FullCalendar

### BusquedaController ("/buscar")
- `GET /buscar?q=` — búsqueda global (lotes + recetas + insumos), renderiza `busqueda.html`
- `GET /buscar/suggest?q=` — `@ResponseBody`, `produces=JSON`. Devuelve `{lotes:[{titulo,sub,url}], recetas:[...], insumos:[...]}` con hasta 4 resultados por categoría. Usado por el typeahead del navbar global.

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
- **Campana en navbar** (notificaciones in-app): `<li id="alertaBellItem" class="nav-item dropdown" style="display:none">` — al cargar la página hace `fetch('/notificaciones/recientes')` (async). Si `total > 0` muestra el badge rojo ("99+" si supera 99) y el dropdown. El dropdown lista las últimas 5 notificaciones no leídas: icono por tipo, título, tiempo relativo, botón `×` (marcar leída via AJAX) y footer "Marcar todas leídas" + "Ver todas →". El JS inyecta `ALERA_CSRF_TOKEN` y `ALERA_CSRF_HEADER` via `<script th:inline="javascript">` en el navbar para los POST sin depender de meta tags del template. `_csrfToken()` y `_csrfHeader()` son helpers que prefieren los meta tags del template (si existen) y hacen fallback a las variables inline. Al abrir el dropdown se recargan las notificaciones (`show.bs.dropdown`). Falla silenciosamente.

### ComparativaController ("/comparativa") — todos los roles autenticados
- `GET /comparativa?q=` — página de selección: tabla de lotes (últimos 100) con checkboxes, búsqueda client-side, clic en fila activa checkbox, contador JS "X seleccionados", máx. 6. Botón "Comparar" habilitado desde 2 seleccionados.
- `GET /comparativa/resultado?ids=1&ids=2...` — tabla transpuesta (métricas como filas, lotes como columnas) + Chart.js grouped bar (ABV, Atenuación, Eficiencia). Celdas con mejor valor marcadas con `mejor-valor` (dorado + ★ para máximos) o `cpl-mejor` (verde + flecha para costo/litro mínimo). Notas de cata al pie. Redirige a `/comparativa` si se envían menos de 2 IDs.
- **Lógica de "mejor"**: ABV ↑, Atenuación ↑, Eficiencia ↑, Litros ↑ → `mejorMax`. Costo/litro ↓ → `mejorMin`. Map `mejores: String → Long(loteId)` pasado al modelo. En Thymeleaf: `${mejores['abv'] == lote.id}` (OGNL usa `.equals()` en `==`).

### AdminController ("/admin")
- `GET /admin/logs?tipo=&page=` — visor de log de accesos (solo ADMIN)

### TenantAdminController ("/admin/tenants") — solo ADMIN
- `GET /admin/tenants` — lista todos los tenants en grid de cards con franja de colores y mini-preview del navbar. Botón "Limpiar cache" en el header.
- `GET /admin/tenants/nuevo` — formulario de creación (subdomain editable)
- `GET /admin/tenants/editar/{subdomain}` — formulario de edición (subdomain readonly — es la PK). Secciones: info básica, paleta de colores (con preview en vivo), tipografías (con preview en vivo de heading + body).
- `POST /admin/tenants/guardar` — crea o actualiza tenant; invalida cache de `TenantFilter` con `evictCache(subdomain)`
- `POST /admin/tenants/{subdomain}/toggle` — activa/desactiva tenant; invalida cache
- `POST /admin/tenants/cache/evict` — limpia todo el cache en memoria de `TenantFilter` (`evictAll()`). Útil cuando se modifica un tenant directamente en BD sin pasar por la UI.
- `GET /admin/tenants/{subdomain}/usuarios` — lista usuarios del tenant con `findAllByTenantId` (native SQL). Inyecta `UsuarioRepository` y `PasswordEncoder` directamente — no usa `UsuarioService` para evitar el filtro automático `@TenantId`.
- `POST /admin/tenants/{subdomain}/usuarios/guardar` — crea usuario via `insertarConTenant` (native SQL INSERT con tenant_id explícito). Valida unicidad con `countByUsernameAndTenantId`.
- `POST /admin/tenants/{subdomain}/usuarios/{id}/toggle` — `toggleActivoByIdAndTenantId` (native SQL `NOT activo`).
- `POST /admin/tenants/{subdomain}/usuarios/{id}/password` — `updatePasswordByIdAndTenantId` (native SQL, password BCrypt).
- `POST /admin/tenants/{subdomain}/usuarios/{id}/rol` — `updateRolByIdAndTenantId` (native SQL, rol como String).
- `POST /admin/tenants/{subdomain}/usuarios/{id}/eliminar` — `deleteByIdAndTenantId` (native SQL DELETE). Registra `USUARIO_ELIMINADO` en historial.
- `GET /admin/tenants/{subdomain}/historial` — lista `HistorialTenant` del tenant ordenado por fecha DESC. Template: `admin/tenant-historial.html`.
- `GET /admin/tenants/{subdomain}/config` — `@ResponseBody` JSON con los 11 campos de branding. Usado por el "Copiar de..." client-side en el formulario.
- `GET /admin/tenants/{subdomain}/export` — descarga `{subdomain}-branding.json` con los 11 campos de branding (name, tagline, logoUrl, colores, fuentes). NO incluye emailAdmin, active ni alertas*.
- `POST /admin/tenants/{subdomain}/import` — multipart upload de JSON. Aplica solo campos conocidos (ignora desconocidos), guarda via `TenantService.guardar()`, registra `CONFIG_IMPORTADA` en historial.
- `buildConfigMap(Tenant)` — helper privado que construye el `Map` de 11 campos de branding para export/config.
- `applyConfig(Tenant, Map)` — helper privado que aplica campos del Map al Tenant, ignorando nulls y campos desconocidos.
- Inyecta `ObjectMapper` (Jackson) para serialización/deserialización JSON.
- `formularioEditar` pasa `otrosTenants` (todos los tenants excepto el actual) para el select "Copiar de...".
- Hereda restricción `ADMIN` de `/admin/**` en `SecurityConfig`

### ProveedorController ("/proveedores")
- CRUD + acceso ADMIN y FACTURACION
- `GET /proveedores/suggest?q=` — `@ResponseBody`, `produces=JSON`. Delega a `service.suggest(q)`. Busca por nombre o NIT. Devuelve `[{nombre, nit, activo, url}]`.

### EquipoController ("/equipos")
- CRUD + filtro por `EstadoEquipo` + paginación
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

---

## SEGURIDAD

- `@EnableMethodSecurity` activo
- **Sesión**: timeout 30 min, `invalidSessionUrl("/login?expired=true")`
- **Protección contra fuerza bruta**: `LoginAttemptService` (Caffeine TTL) rastrea intentos fallidos por IP. `LoginAttemptFilter` intercepta POST `/login` — si la IP está bloqueada, redirige a `/login?bloqueado=true` sin intentar autenticar. `AleraAuthFailureHandler` llama `registrarFallo(ip)` en cada fallo; `AleraAuthSuccessHandler` llama `resetear(ip)` en login exitoso. Configurable: `app.login.max-intentos` (def: 5, env: `LOGIN_MAX_INTENTOS`), `app.login.bloqueo-minutos` (def: 15, env: `LOGIN_BLOQUEO_MINUTOS`). `LoginAttemptFilter` es un bean creado en `SecurityConfig` (NO `@Component`) para evitar problemas en `@WebMvcTest`.
- **Handlers**:
  - `AleraAuthSuccessHandler` → resetea contador de intentos por IP + registra `LOGIN_OK` en `log_accesos`
  - `AleraAuthFailureHandler` → registra fallo por IP + registra `LOGIN_FALLIDO` + redirige a `/login?error` o `/login?bloqueado=true`
  - `AleraAccessDeniedHandler` → registra `ACCESO_DENEGADO` + redirige a `/error?status=403`
- **Restricciones por URL:**
  - `/admin/**`, `/usuarios/**`, `/tipos-cerveza/**` → solo ADMIN
  - `/actuator/**` → ADMIN (excepto `/actuator/health` que es público)
  - `POST /guardar`, `POST /actualizar/**`, `POST /eliminar/**`, `GET /nuevo`, `GET /editar/**` → solo ADMIN (trazabilidad escritura)
  - `/facturas/**`, `/proveedores/**` → ADMIN, FACTURACION
  - `/inventario/**`, `/recetas/**` → ADMIN, INVENTARIO
  - `/equipos/**` → ADMIN, EQUIPOS
  - `/api/auth/**` → público (sin autenticación — endpoint de login JWT)
  - `/api/**` → cualquier usuario autenticado (HTTP Basic, sesión, o Bearer JWT)
  - Todo lo demás (incluido `/swagger-ui/**`, `/v3/api-docs/**`) → cualquier rol autenticado
- **Endpoints quick-create**: `POST /inventario/guardar-rapido` hereda `/inventario/**` (ADMIN, INVENTARIO). `POST /facturas/guardar-insumo-rapido` y `/facturas/guardar-equipo-rapido` heredan `/facturas/**` (ADMIN, FACTURACION). `POST /tipos-cerveza/guardar-rapido` hereda `/tipos-cerveza/**` (ADMIN).
- **JWT — `JwtFilter`** (`OncePerRequestFilter`, bean en SecurityConfig): actúa solo en `/api/**`. Lee el header `Authorization: Bearer <token>`, valida la firma HMAC-SHA256, verifica que el tenant del claim coincida con `TenantContext.getCurrentTenant()`, y si todo es válido establece la autenticación en `SecurityContextHolder`. Si no hay token o es inválido, la request continúa sin autenticación (HTTP Basic puede tomar el relevo). CSRF deshabilitado para `/api/**` — clientes REST usan el token, no cookies. El tenant del token se embebe al generarlo y se verifica en cada request para evitar que un token de tenant A acceda a datos de tenant B. `JwtService` genera tokens con claims `{sub: username, tenant, rol}` y TTL configurable. **CRÍTICO**: en `@WebMvcTest`, mockear `JwtService` (no `JwtFilter`) — mismo patrón que `LoginAttemptService`.
- **CSRF en AJAX**: todos los endpoints `@ResponseBody POST` requieren el token CSRF. Los templates que los usan incluyen `<meta name="_csrf" th:content="${_csrf.token}"/>` y `<meta name="_csrf_header" th:content="${_csrf.headerName}"/>`. El JS lee estos metas y los envía como header en el `fetch()`.
- **JPA Auditing**: `JpaConfig` con `@EnableJpaAuditing(auditorAwareRef="auditorAwareImpl")`, `AuditorAwareImpl` lee usuario de SecurityContext. Fallback a `"sistema"` si no hay sesión activa.
- **Navbar**: `sec:authorize` oculta links según rol. Los ítems están agrupados en dropdowns: **Producción** (todos los roles): Trazabilidad, Kanban, Planificación, Comparativa, Calendario; **Almacén** (ADMIN/INVENTARIO): Inventario, Recetas; **Comercial** (ADMIN/FACTURACION): Facturas, Proveedores; **Admin** (ADMIN): Reportes, Tipos de Cerveza, Usuarios, Log de Accesos, Tenants. Equipos queda como ítem standalone (ADMIN/EQUIPOS). El botón `+` muestra acciones rápidas de creación filtradas por rol. El dropdown de usuario muestra nombre, badge de rol y link a `/perfil/password`.
- **`/perfil/**`** cae en `anyRequest().authenticated()` — accesible a todos los roles. Sin regla explícita en `SecurityConfig`.
- **Multi-tenant — TenantFilter** (`OncePerRequestFilter`):
  - Extrae subdomain del header `Host` (ej: `cerveceria1.app.com` → `cerveceria1`)
  - En localhost/127.0.0.1 usa `app.default-subdomain` (normalmente `"default"`). Para probar múltiples tenants en local, agregar entradas en `hosts` (`127.0.0.1 mosto.localhost`) y acceder via `http://mosto.localhost:8080`.
  - Busca `Tenant` en BD usando `findBySubdomainAndActiveTrue` — **si `active=false` devuelve 503** aunque el tenant exista en BD. Cache en memoria Caffeine (`Cache<String, Tenant>`) con TTL configurable (`app.tenant-cache-ttl-minutes`, def: 5 min), `maximumSize(200)`. Se invalida explícitamente con `evictCache(subdomain)` o `evictAll()`.
  - Llama `TenantContext.setCurrentTenant(subdomain)` + guarda en `request.setAttribute("currentTenant", tenant)`
  - `finally` llama `TenantContext.clear()` — nunca hay fuga de contexto entre requests
  - Registrado con `addFilterBefore(tenantFilter, SecurityContextHolderFilter.class)` para que corra antes de cualquier autenticación de Spring Security
  - `FilterRegistrationBean.setEnabled(false)` evita doble registro como servlet filter
  - Salta recursos estáticos (`/css/`, `/js/`, `/img/`, etc.) via `shouldNotFilter`
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
8. **Thymeleaf — CRÍTICO**: `eq`, `ne`, `lt`, `gt`, `le`, `ge` son palabras reservadas. NO usar como variables de iteración en `th:each`.
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
18. **FacturaProveedor**: tiene dos campos de proveedor: `proveedor` (String original) y `proveedorRef` (FK LAZY nullable). Coexisten para compat. histórica. El campo de fecha es `fechaFactura` — **NO** `fecha`. En JPQL: `f.fechaFactura`; en Java: `getFechaFactura()`. Escribir `f.fecha` en un `@Query` provoca `UnknownPathException` al arrancar.
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
30. **Receta — datalists en formulario**: el campo `estilo` usa datalist de `TipoCerveza` activos. Cada grupo de ingredientes usa datalist del inventario filtrado por tipo. Si el ítem no existe, el botón `⊞` abre un modal de creación rápida vía AJAX.
31. **Factura — datalist dinámico por categoría**: el campo `nombre` de cada ítem usa un datalist compartido (`#datalist-item-nombres`) que se actualiza según tipo+categoría seleccionados. Los mapas `insumosPorTipo` y `equiposPorTipo` se serializan como JSON en la página y se usan en JS. El botón `⊞` abre modal según el tipo del ítem.
32. **Trazabilidad — Costo de Producción** (activo): asignación a nivel de ítem con cantidad parcial. La sección en `formulario.html` muestra un buscador de ítems de factura (filtrable por nombre/proveedor/tipo). Los ítems seleccionados van a `lote.itemsFactura` (OneToMany `LoteItemFactura`). `detalle.html` muestra tabla con factura, proveedor, cantidad asignada y valor proporcional. Cantidad 0 = costo completo del ítem sin ingrediente.
33. **Calculadora de escala (recetas/detalle)**: escala ingredientes, agua macerado y agua sparge. Variables JS: `AGUA_MACERADO`, `UNIDAD_MACERADO`, `AGUA_SPARGE`, `UNIDAD_SPARGE` (inline Thymeleaf). Al resetear llama `resetAgua()`.
34. **Multi-tenant — @TenantId en todas las entidades**: todas las 17 entidades de datos tienen `@TenantId private String tenantId`. Las 6 que extienden `AuditableEntity` lo heredan; las 11 restantes lo declaran directamente. Hibernate agrega `WHERE tenant_id = :current` automáticamente a todos los SELECT. NO setear `tenantId` manualmente — Hibernate lo gestiona.
35. **Multi-tenant — DataInitializer**: al arrancar, crea el tenant `defaultSubdomain` si no existe, luego itera **todos los tenants** en BD. Para cada uno con cero usuarios, crea los usuarios de las env vars (`ADMIN_USERNAME/PASSWORD`, etc.) usando `insertarConTenant` (native SQL) y tipos de cerveza. Los tenants con usuarios ya configurados no se tocan. Esto garantiza que cualquier tenant creado vía UI reciba su admin al reiniciar la app. El método `run()` tiene `@Transactional` porque `insertarConTenant` es `@Modifying` y requiere una transacción activa. **CRÍTICO**: los métodos `@Modifying` en repositorios (`insertarConTenant`, `toggleActivoByIdAndTenantId`, etc.) deben tener `@Transactional` propio si se llaman fuera de un contexto transaccional externo — de lo contrario lanzan `TransactionRequiredException`.
36. **Multi-tenant — agregar cliente nuevo**: (1) crear tenant en `/admin/tenants/nuevo` (UI) o insertar fila en `tenants` directamente en BD; (2) DNS `cliente.tuapp.com` → servidor; (3) crear usuario admin del tenant en `/usuarios` (el contexto de tenant ya estará activo vía subdominio) o con `INSERT INTO usuarios (username, password, rol, activo, tenant_id) VALUES (...)`.
37. **Branding — orden de prioridad**: `Tenant.color*` / `Tenant.font*` > `BrandingProperties` (env vars/properties). `Tenant` se crea con valores de `BrandingProperties` pero puede actualizarse via `/admin/tenants/editar/{subdomain}` o directamente en BD. Tras edición directa en BD, usar "Limpiar cache" en `/admin/tenants` para reflejar cambios sin reiniciar.
37b. **Login page — logo**: sin círculo decorativo. Si `branding.logoUrl` no está vacío, muestra la imagen (`max-height:90px; max-width:240px`). Si está vacío, muestra ícono `bi-droplet-fill`. El campo `logoUrl` acepta URL externa o ruta relativa (`/img/logo.png`) — archivos locales van en `src/main/resources/static/img/`.
38. **@WebMvcTest — seguridad URL-based no se enforce con handler mock**: `AleraAccessDeniedHandler` mockeado es un no-op (void). Cuando Spring Security lanza `AccessDeniedException` y el handler no comite la respuesta, el request puede llegar al controller. Las pruebas de seguridad URL-based (como "rol no-admin no puede acceder a /nuevo") NO funcionan correctamente en `@WebMvcTest` — deben testearse en integración. Las pruebas `@PreAuthorize` (method-level) SÍ funcionan porque `@EnableMethodSecurity` está activo en `SecurityConfig`.
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
- Dark mode: toggle luna/sol en navbar, clase `html.dark-mode` en `<html>`, persiste en `localStorage`
- Dashboard personalizable (todo localStorage, sin backend):
  - **Visibilidad**: dropdown "Personalizar" con checkboxes por sección → `localStorage` key `alera-dashboard-secciones`. `restaurarVisibilidad()` aplica al cargar.
  - **Orden drag & drop**: SortableJS 1.15.2 sobre `#dash-sortable`, `handle: '.dash-handle'` → `localStorage` key `alera-dashboard-orden`. `restaurarOrden()` reordena el DOM antes de aplicar visibilidad (orden primero, luego show/hide). `guardarOrden()` se llama en `onEnd`.
  - **Secciones** (`id="dash-{nombre}"`): `stats-lotes`, `stats-inventario`, `alertas`, `charts`, `finanzas`. Cada una tiene `class="dash-section"` con `<div class="dash-handle">` (grip icon, visible en hover). `alertas` usa `th:if` → puede no existir en DOM; `restaurarOrden()` lo ignora con `getElementById` null-check.
  - **Botón "Restablecer"**: borra ambas claves localStorage y recarga.
  - **SortableJS**: mismo CDN que kanban (`sortablejs@1.15.2`). `ghostClass:'dash-ghost'`, `dragClass:'dash-drag'`.
- Búsqueda global: `GET /buscar?q=` (página completa) + `GET /buscar/suggest?q=` (JSON para typeahead del navbar)
- **Patrón typeahead/suggest**: cada módulo con lista expone `GET /{modulo}/suggest?q=` (`@ResponseBody`, `produces=JSON`). El servicio filtra en memoria (listas pequeñas) o via query DB con LIKE (facturas). El JS del template usa debounce 260ms, flechas ↑↓ para navegar, Escape para cerrar. CSS del dropdown definido en `navbar.html` (clases `.search-suggest`, `.ss-item`, `.ss-title`, `.ss-sub`, `.ss-section`, `.ss-footer`, `.ss-empty`) — disponibles globalmente por ser parte del fragment. El `data-*` attribute en el input inyecta filtros activos (tipo, estado, activa) sin inline JS. Usuarios: sin página de detalle — el click usa `href="#usuario-{id}"` + CSS `tr:target { animation: rowFlash }` para scroll+flash sin navegación.
- Mapeos entidad→DTO: usar `LoteMapper` en `com.alera.mapper` (MapStruct). No escribir asignaciones manuales campo a campo en servicios.
- Templates con AJAX: incluir siempre `<meta name="_csrf">` y `<meta name="_csrf_header">` en el `<head>`. Leer en JS con `document.querySelector('meta[name="_csrf"]')?.content`.
- **Branding en templates**: `${branding.name}`, `${branding.tagline}`, `${branding.logoUrl}`, `${branding.colorAccent}`, etc. — disponible en todos los templates via `GlobalControllerAdvice`. NO hardcodear "Alera" en HTML. En contextos donde `branding` puede ser null (página de error, dispatches de error de Servlet), usar `${branding != null ? branding.name : 'Alera'}` o el operador safe-navigation `${branding?.name}`.
- **Costos en formulario**: `ITEMS_FACTURA`, `INIT_IDS`, `INIT_CANTIDADES` se inyectan como JS via `<script th:inline="javascript">`. El submit handler construye `itemsIds[]` + `itemsCantidades[]` como hidden inputs desde el array `asignados`. El botón "Aplicar a Receta e Insumos" llama `sincronizarIngredientesDesdeItems()` que rellena los containers de ingredientes y navega al tab 1 (`goTab(1)`).
- **JS estático de trazabilidad** (`src/main/resources/static/js/`): la lógica JS de los templates de trazabilidad está extraída a archivos externos para facilitar mantenimiento. Patrón: el `<script th:inline="javascript">` del template inyecta solo los datos Thymeleaf como variables globales; el archivo `.js` externo lee esas variables. Archivos:
  - `trazabilidad-ingredientes.js` — wizard de tabs, conversión de volumen, filas dinámicas de ingredientes, carga de receta. Usado por `formulario.html`.
  - `trazabilidad-costos.js` — buscador de ítems de factura, asignación de costos, sincronización con ingredientes, submit handler. Depende de `trazabilidad-ingredientes.js` (llama `goTab`, `poblarDesdeReceta`). Usado por `formulario.html`.
  - `trazabilidad-detalle.js` — construcción del gráfico Chart.js dual-eje (densidad + temperatura). Lee `CHART_FECHAS`, `CHART_DENSIDAD`, `CHART_TEMP`. Usado por `detalle.html`.
  - `trazabilidad-kanban.js` — drag & drop SortableJS, AJAX POST de cambio de fase, toast, contadores. Lee `esAdmin`. Usado por `kanban.html`.
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
docker compose up --build

# Variables de entorno opcionales (.env)
DB_USERNAME=alera
DB_PASSWORD=alera2024
ADMIN_USERNAME=admin
ADMIN_PASSWORD=alera2024
INVENTARIO_USERNAME=inventario   # opcional — crea usuario con rol INVENTARIO
INVENTARIO_PASSWORD=inv2024
FACTURACION_USERNAME=facturacion
FACTURACION_PASSWORD=fac2024
EQUIPOS_USERNAME=equipos
EQUIPOS_PASSWORD=eq2024

# JWT — obligatorio en producción (docker-compose falla si no está definido)
JWT_SECRET=cambia-esto-por-un-secreto-seguro-de-32-chars-minimo
JWT_TTL_HOURS=24                 # opcional — default 24h

# Multi-tenant
DEFAULT_SUBDOMAIN=default        # subdomain del tenant inicial (localhost usa este valor)

# Branding del tenant inicial (los valores por defecto son los de Alera)
APP_BRAND_NAME=Alera
APP_BRAND_TAGLINE=Sistema de Trazabilidad Cervecera
APP_BRAND_LOGO_URL=              # URL pública del logo (vacío = ícono de gota)
APP_BRAND_COLOR_NAVBAR=#242E0D
APP_BRAND_COLOR_PRIMARY=#364318
APP_BRAND_COLOR_ACCENT=#C9A028
APP_BRAND_COLOR_ACCENT_HOVER=#E0B840
APP_BRAND_COLOR_CREAM=#F5EDD0
APP_BRAND_COLOR_BODY_BG=#F0EDE2
```

- Build multi-etapa: `maven:3.9-eclipse-temurin-21` + `eclipse-temurin:21-jre-alpine`
- Healthcheck: `GET /actuator/health` cada 30s (requiere `spring-boot-starter-actuator`)
- Docker activa automáticamente `SPRING_PROFILES_ACTIVE=prod` → usa `application-prod.properties` (sin fallbacks de credenciales BD ni JWT — la app falla al iniciar si `DB_PASSWORD` o `JWT_SECRET` no están definidos)
- Desarrollo local: `application.properties` mantiene fallbacks (ej: `DB_PASSWORD:12345`, `JWT_SECRET:alera-dev-secret-key-change-in-production-2024`) para arrancar sin variables de entorno

---

## TESTS

**Unitarios** (`src/test/java/com/alera/service/`):
- `InsumoInventarioServiceTest`, `TrazabilidadServiceTest`, `DashboardServiceTest`
- `FacturaProveedorServiceTest`, `UnidadUtilsTest` — `FacturaProveedorServiceTest` requiere `@Mock FacturaHistorialEstadoRepository historialRepo` y `@Mock ProveedorRepository proveedorRepo` (además de los mocks previos) porque el constructor del servicio los inyecta. El `@BeforeEach` stubea `historialRepo.save(any())` para que `guardar()` no lance NPE.
- `LogAccesoServiceTest` — cubre `registrar`, `listarPaginado` (con/sin filtro) y `fallidosUltimaHora` (verifica ventana de 1 hora). Usa `ReflectionTestUtils.setField` para inyectar `pageSize` sin contexto Spring.
- `EquipoServiceTest` — 11 tests: listar/paginar (con y sin filtro de estado), buscarPorId, guardar, eliminar (happy path, no encontrado, con lotes activos → EquipoEnUsoException), fermentadores disponibles, mantenimiento pendiente (verifica ventana de 7 días).
- `RecetaServiceTest` — 14 tests: listarActivas/Todas/Paginado (filtros null/true/false), buscarPorId (found/not found), guardar (campos básicos, normalización kg→gr, ignorar vacíos, escalones en orden), actualizar (limpia ingredientes anteriores), eliminar, toFormDto (mapeo directo, parseo "5000 gr"→{cantidad,unidad}, fila vacía si lista vacía). OG/FG objetivo usan literales Integer (ej: `1050`, `1010`) — NO BigDecimal.
- `UsuarioServiceTest` — 25 tests: `loadUserByUsername` (usuario válido, no existe, inactivo, mapeo de todos los roles → `ROLE_X`), `guardar` (BCrypt encode, rol específico, null→ADMIN), `toggleActivo` (activo→inactivo, inactivo→activo, no existe no-op), `cambiarPassword` (encode y guarda, no existe no-op), `cambiarRol`, `eliminar`, `existeUsername`, `esElMismoUsuario` (mismo, distinto, no existe), `suggest` (null/corta, filtro, límite 6, estructura del mapa con displayName).
- `TenantServiceTest` — 19 tests: `listarTodos` (orden por subdomain), `buscarPorSubdomain`, `guardar` (CREADO/EDITADO, evicta cache, registra historial, retorna tenant), `evictAllCache`, `toggleActivo` (ACTIVADO/DESACTIVADO, evicta cache, no existe no-op), `listarHistorial`, `registrarAccion` (con/sin autenticación → "sistema"), `registrarEnvioExitoso` (resetea contador, timestamps, no existe no-op), `registrarEnvioFallido` (incrementa, no existe no-op). Usa `SecurityContextHolder` para simular usuario autenticado; limpia en `@AfterEach`.
- `EmailService` usa `@Autowired` en campos (no constructor) → tests usan `ReflectionTestUtils.setField` para inyectar `mailSender`, `templateEngine`, `fromAddress`, `baseUrl`. `MimeMessage` creado con `new MimeMessage((jakarta.mail.Session) null)` — permite que `MimeMessageHelper` opere sin SMTP real.
- `EmailServiceTest` — 19 tests: `mailConfigurado` (con/sin SMTP), `enviarAlertasDiarias` (sin SMTP, email null/vacío, sin alertas, con bajoStock/vencimientos/mantenimiento, fallo SMTP → RuntimeException, variables al template), `enviarEmailPrueba` (sin SMTP, destinatario null/vacío, éxito → null, fallo → mensaje error), `diasHasta` (hoy/futuro/pasado).
- `TipoCervezaServiceTest` — 11 tests: `listarActivos/Todos`, `buscarPorId`, `existePorNombre`, `guardar`, `eliminar`, `toggleActivo` (activo→inactivo, inactivo→activo, no existe no-op).
- `ProveedorServiceTest` — 15 tests: `listarActivos/Todos`, `buscarPorId`, `suggest` (null/corta, filtro nombre, filtro NIT, límite 6, estructura mapa con url, NIT null → string vacío), `guardar`, `eliminar`, `contarFacturas`, `totalFacturas`.
- `MantenimientoEquipoServiceTest` — 9 tests: `listarPorEquipo` (vacío y con resultados), `registrar` (campos del DTO en MantenimientoEquipo, actualiza `fechaUltimoMantenimiento`, actualiza/no-actualiza `proximoMantenimiento` según null, equipo no existe → RuntimeException, retorna guardado), `eliminar`.
- `PdfExportServiceTest` — 8 smoke tests: verifica magic bytes `%PDF`, lote mínimo sin lecturas, lote completo (densidades, fases, obs), lecturas con densidad+temp, solo densidad, solo temperatura, lecturas null, tamaño >1KB, PDFs distintos para lotes distintos. Instancia `PdfExportService` directamente (sin Spring context — no tiene dependencias).
- `ExcelExportServiceTest` — 8 smoke tests: verifica magic bytes `PK` (ZIP/XLSX), listas vacías, lote mínimo, lotes con métricas, resumen por estilos, 50 lotes sin excepción, contenido distinto para lotes distintos. **Bug descubierto**: fechas `null` en `desde`/`hasta` → `RuntimeException` (NPE interno al formatear) — el test lo documenta y verifica el comportamiento real. **NOTA**: `List.of(Object[])` causa ambigüedad de tipos en Java 26 — usar `new ArrayList<>()` para listas de `Object[]`.

**Controladores** (`src/test/java/com/alera/controller/`) — `@WebMvcTest` + `@MockBean`:
- `TrazabilidadControllerTest` — 15 tests: seguridad (sin-autenticar → 401; con rol no-admin → controller corre porque URL-based security no se enforce con handler mock), index, kanban, nuevo/guardar (válido, inválido, advertencia stock), ver/404, eliminar. `@MockBean`: `PdfExportService`, `LecturaFermentacionService`, `PlanificacionService` (los tres requeridos por el constructor del controller).
- `AuthControllerTest` — 3 tests (`@AutoConfigureMockMvc(addFilters=false)` para aislar la lógica del controller): login con credenciales válidas retorna token + campos del `AuthResponse`, credenciales inválidas → 401 con `{error}`, body vacío → 400. `@MockBean AuthenticationManager` y `JwtService`.
- `ApiControllerTest` — 9 tests: seguridad (401), lotes (lista, por id, 404, historial), recetas, alertas inventario, dashboard
- `AlertaControllerTest` — 5 tests: seguridad (401), estructura JSON, totales (suma de 3 contadores), sin alertas, solo mantenimiento
- `NotificacionControllerTest` — 5 tests: seguridad (401), GET /notificaciones (página con modelo), GET /recientes (JSON con total e items), POST /{id}/leer (JSON con noLeidas), POST /leer-todas (redirect)
- `PlanificacionControllerTest` — 11 tests: seguridad (401 sin autenticar; 302 via `AleraAccessDeniedHandler` para acceso denegado), página principal, eventos JSON, guardar/cambiarEstado/eliminar (ADMIN vs no-ADMIN)
- `LoginControllerTest` — 3 tests: GET /login público (200), con ?error, con ?bloqueado. **Nota**: en `@WebMvcTest`, Spring Security puede interceptar GET /login con su propio filtro antes del DispatcherServlet — no verificar `view().name("login")`, solo `status().isOk()`.
- `DashboardControllerTest` — 3 tests: 401 sin auth, 200 con cualquier rol, modelo tiene `stats` attribute
- `CalendarioControllerTest` — 3 tests: 401 sin auth, 200 autenticado, eventos JSON
- `AdminControllerTest` — 3 tests: 401, 200 ADMIN con lista vacía de logs, filtro por tipo
- `PerfilControllerTest` — 3 tests: 401, 200 con cualquier rol, POST cambio de contraseña redirige
- `BusquedaControllerTest` — 3 tests: 401, 200 con query, suggest retorna JSON. **Nota**: `loteRepo.search()` y `recetaRepo.search()` retornan `List<>` (no `Page`) — usar `when(...).thenReturn(List.of())`
- `TipoCervezaControllerTest` — 3 tests: 401, 200 ADMIN, `guardarRapido` → JSON 200. **Nota**: stub `service.guardar(any())` para devolver un `TipoCerveza` con id/nombre, si no el NPE cae al catch → 400
- `UsuarioControllerTest` — 4 tests: 401, 200 ADMIN, suggest JSON, guardar con contraseña inválida redirige. **Nota**: el parámetro del controller se llama `confirmPassword` (no `confirmarPassword`)
- `RecetaControllerTest` — 4 tests: 401, 200 con filtro activas, suggest JSON, GET /editar retorna formulario
- `EquipoControllerTest` — 3 tests: 401, 200 ADMIN, suggest JSON. **Nota**: método se llama `listarFermentadoresDisponibles()` (no `fermentadoresDisponibles()`). Usar `doReturn(new PageImpl<>(Collections.emptyList())).when(service).listarPaginado(any(), anyInt())`
- `ProveedorControllerTest` — 3 tests: 401, 200 con roles ADMIN/FACTURACION, suggest JSON
- `InsumoInventarioControllerTest` — 3 tests: 401, 200 ADMIN, suggest JSON con filtro nombre
- `FacturaProveedorControllerTest` — 3 tests: 401, 200 ADMIN, suggest JSON. `@MockBean InsumoInventarioRepository`, `EquipoRepository` y `ExcelExportService` adicionales. **Nota**: stub usa `listarPaginado(any(), any(), any(), anyInt())`. El `@BeforeEach` también stubea `sumTotal(any(),any(),any()) → BigDecimal.ZERO`, `sumPendiente(any(),any()) → BigDecimal.ZERO`, `countPendiente(any(),any()) → 0L` — necesarios porque `lista()` los pasa al modelo y el template los renderiza en las stat-cards.
- `ReporteControllerTest` — 2 tests: 401, 200 con rango de fechas
- `MantenimientoEquipoControllerTest` — 2 tests: 401, 200 ADMIN. **Nota**: el equipo mock debe tener `tipo` y `estado` seteados (`TipoEquipo.FERMENTADOR`, `EstadoEquipo.OPERATIVO`) — el template accede a `equipo.tipo.displayName` directamente sin null-check
- `TenantAdminControllerTest` — 4 tests: 401, 200 lista ADMIN, formulario nuevo, config JSON. Requiere `@MockBean PasswordEncoder` (inyectado en constructor del controller). **CRÍTICO**: NO agregar `@MockBean ObjectMapper` — mockear Jackson rompe la autoconfiguración de Spring (`routerFunctionMapping` falla al crear porque `objectMapper.reader()` retorna null en el mock)
- `ComparativaControllerTest` — 3 tests: 401, 200 autenticado, resultado con <2 ids redirige
- `WebMvcTestHelper` — utilidad con `configureTenantMock(TenantRepository)` que configura el tenant "default" con colores válidos para que TenantFilter resuelva correctamente en el test context

**@WebMvcTest — mocks requeridos** (todos los tests de controlador necesitan estos `@MockBean`):
- `TenantRepository` — SecurityConfig crea TenantFilter que lo inyecta; sin mock → TenantFilter devuelve 503
- `BrandingProperties` — GlobalControllerAdvice la inyecta como fallback; sin mock → contexto no carga
- `AleraAuthSuccessHandler`, `AleraAuthFailureHandler`, `AleraAccessDeniedHandler` — SecurityConfig.filterChain() los recibe como parámetros; sin mock → Spring usa la seguridad por defecto (sin URL-based restrictions)
- `LoginAttemptService` — requerido por `LoginAttemptFilter` (bean en SecurityConfig); sin mock → contexto no carga. **CRÍTICO**: NO mockear `LoginAttemptFilter` directamente (es creado por SecurityConfig vía `@Bean`, no auto-detectado). Mockear `LoginAttemptService` para que el filtro real pueda ser creado con la dependencia satisfecha.
- `JwtService` — requerido por `JwtFilter` (bean en SecurityConfig); sin mock → contexto no carga. Mismo patrón que `LoginAttemptService`. **CRÍTICO**: NO mockear `JwtFilter` directamente.
- `UsuarioService`, `LogAccesoService` — requeridos por los auth handlers y DaoAuthenticationProvider
- `PasswordEncoder` — si el controller lo inyecta directamente (ej: `TenantAdminController`), agregar `@MockBean PasswordEncoder`
- **NO mockear `ObjectMapper`**: Spring Boot lo autoconfigura en `@WebMvcTest`. Mockearlo hace que `routerFunctionMapping` falle al crear (`objectMapper.reader()` retorna null). Si el controller usa `ObjectMapper`, usa el bean autoconfigurdo directamente.
- **Comportamiento de seguridad en @WebMvcTest**: con `httpBasic()` configurado, requests sin autenticar devuelven `401` (no `302`). Los handlers mockeados (void, no-op) no comiten la respuesta → URL-based security no se enforce plenamente → las pruebas de seguridad URL-based verifican que el controller SE EJECUTA (no que SE BLOQUEA). La seguridad URL-based real se verifica en tests de integración.

**@WebMvcTest — Java 26 + Byte Buddy**: el proyecto corre en JVM 26 y Byte Buddy (bundled con Mockito) solo soporta oficialmente hasta Java 24. El `maven-surefire-plugin` tiene configurado `<argLine>-Dnet.bytebuddy.experimental=true</argLine>` y `<systemPropertyVariables><net.bytebuddy.experimental>true</net.bytebuddy.experimental></systemPropertyVariables>` para habilitar instrumentación experimental en JVM 26.

**Integración** (`src/test/java/com/alera/`) — Testcontainers + `postgres:16-alpine`:
- `AbstractIntegrationTest` — base con `@ServiceConnection` (Spring Boot 3.4). **NO usa `@Testcontainers` ni `@Container`** — en su lugar arranca el contenedor en un `static { POSTGRES.start(); }`. Esto evita que Testcontainers detenga y reinicie el contenedor entre clases de test, lo que causaría que el contexto Spring Boot cacheado intentara reconectar a un puerto que ya no existe. Perfil `test` con credenciales dummy (`DB_PASSWORD=test`).
- `FlywayMigrationIntegrationTest` — verifica V1–V26 sin errores ni migraciones pendientes; también verifica que haya ≥19 migraciones aplicadas
- `LoteCervezaRepositoryIntegrationTest` — valida queries clave con BD real + rollback automático
- `TrazabilidadServiceIntegrationTest` — guardar, código consecutivo, ingredientes, eliminar, historial
- `PlanificacionServiceIntegrationTest` — 8 tests: guardar (estado, volumen, duplicados), cambiar estado (EN_PROCESO, flujo completo, cancelar), listarProximas (excluye pasados), listarPorRango, eliminar
- `LecturaFermentacionServiceIntegrationTest` — 9 tests: agregar (con temp, sin temp, sin densidad, notas blank→null), ordenamiento ASC, ABV parcial (fórmula, null si sin densidad, null si igual OG), eliminar (una sola, sin afectar otras)
- `TenantIsolationIntegrationTest` — 6 tests que verifican aislamiento de datos entre tenants: `@TenantId` filtra `TipoCerveza` y `Usuario` correctamente entre tenants distintos; queries nativas cross-tenant (`findAllByTenantId`, `countByUsernameAndTenantId`) retornan solo el tenant especificado. **Sin `@Transactional` en el test** — cada repo call crea su propio `EntityManager` que captura `TenantContext` en ese momento. Cleanup via `JdbcTemplate` en `@AfterEach`.
- **NOTA multi-tenant en tests de integración**: los tests deben llamar `TenantContext.setCurrentTenant("default")` en `@BeforeEach` y `TenantContext.clear()` en `@AfterEach` para que Hibernate pueda filtrar/insertar correctamente con el tenant discriminador. **NUNCA poner `@Transactional` en tests de aislamiento multi-tenant** — ver regla 41.

**Workaround Docker Desktop 4.74 + WSL2** (`src/test/java/com/alera/WindowsDockerStrategy.java`):
- Docker Desktop 4.74 con backend WSL2 devuelve HTTP 400 con `ServerVersion:""` para cualquier API Docker < 1.40 en el endpoint `/info` desde procesos Windows.
- Testcontainers 1.20.6 hardcodea `VERSION_1_32` en la validación interna (`getDockerClient()` → `getClientForConfig()` → `withApiVersion(VERSION_1_32)`), causando `BadRequestException` al arrancar.
- `WindowsDockerStrategy` sobreescribe `test()` (valida vía HTTP directo a `/v1.40/info`) y `getDockerClient()` (crea cliente con `RemoteApiVersion.VERSION_1_40` vía TCP `127.0.0.1:2375`).
- Se activa en `~/.testcontainers.properties`: `docker.client.strategy=com.alera.WindowsDockerStrategy`
- Docker Desktop debe tener habilitado: **Settings → General → Expose daemon on tcp://localhost:2375 without TLS**

Ejecutar: `mvn test` (requiere Docker Desktop corriendo con daemon TCP habilitado)
Perfil test: `src/test/resources/application-test.properties` (credenciales dummy + flags de test)