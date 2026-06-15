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
- `densidadInicial` (`Integer`) — formato XXXX, ej: 1056. **NO usar BigDecimal.** Cuando el instrumento es refractómetro, se calcula automáticamente desde `ogBrix` con la fórmula `SG = brix / (258.6 - brix/258.2 × 227.1) + 1`, multiplicado por 1000 y redondeado.
- `densidadFinal` (`Integer`) — formato XXXX, ej: 1015. **NO usar BigDecimal.** Cuando el instrumento es refractómetro, se calcula con la **corrección Sean Terrill** desde `ogBrix` + `fgBrix`: `FG = 1 - 0.0044993×OB + 0.011774×FB + 0.00027581×OB² - 0.0012717×FB² - 0.0000072800×OB³ + 0.000063293×FB³`, multiplicado por 1000 y redondeado.
- `densidadFinalFecha`
- `ogBrix` (`BigDecimal`, nullable) — `@Column(name="og_brix")` — lectura original del refractómetro en °Brix para la densidad inicial. `null` → instrumento hidrómetro (SG directo). Rango válido: 1.0–40.0 °Brix.
- `fgBrix` (`BigDecimal`, nullable) — `@Column(name="fg_brix")` — lectura original del refractómetro en °Brix para la densidad final. Solo significativo cuando `ogBrix != null`.
- **Instrumento de medición** (derivado, no almacenado): `ogBrix != null` → "BRIX" (refractómetro); `ogBrix == null` → "SG" (hidrómetro). `LoteMapper.mapInstrumentoMedicion()` establece `LoteFormDto.instrumentoMedicion` con este criterio.
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
  - `getAbv()` → `(OG - FG) * 131.25 / 1000.0` → BigDecimal con scale 2. Usa `densidadInicial`/`densidadFinal` directamente (ya derivados desde Brix si aplica).
  - `getAtenuacionAparente()` → `(OG - FG) * 100.0 / (OG - 1000)` → BigDecimal con scale 1
  - `getEficienciaMacerado()` → `ogPuntos = OG - 1000` (ya en puntos, NO multiplicar por 1000)
  - `getOgSgFromBrix()` → SG derivado de `ogBrix`: `brix / (258.6 - brix/258.2 × 227.1) + 1.0` × 1000, redondeado. Retorna `null` si `ogBrix` es null.
  - `getFgSgTerrill()` → FG corregido via **Sean Terrill**: fórmula cúbica con `ogBrix` y `fgBrix` → × 1000, redondeado. Retorna `null` si alguno es null.
  - `getAbvTerrill()` → `(getOgSgFromBrix() - getFgSgTerrill()) * 131.25 / 1000.0`, scale 2. Retorna `null` si algún Brix es null. En `detalle.html` se usa este método cuando `ogBrix != null`; en caso contrario se usa `getAbv()`. Badge "Terrill" visible junto al ABV cuando el instrumento es refractómetro.
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

