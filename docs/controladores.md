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

