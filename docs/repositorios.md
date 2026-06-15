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
- `findAllWithItems()` — DISTINCT + JOIN FETCH de todas las facturas con items. Usado por `FacturaProveedorService.listarTodas()`/`listarParaExport()`. **Ya no se usa para el buscador de costos de `TrazabilidadController`** — ver `FacturaItemRepository.search()` / `findByIdIn()`.
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
- `findByIdIn(List<Long> ids)` — `JOIN FETCH fi.factura`, sin orden. Usado por `TrazabilidadController.agregarInventarioAlModelo()` para precargar los datos de los ítems ya asignados al lote (`itemsFacturaAsignados` → `INIT_ITEMS_DATA`).
- `findByNombresIn(List<String> nombres)` — `JOIN FETCH fi.factura`, filtra por `LOWER(TRIM(fi.nombre)) IN :nombres`, orden `f.fechaFactura DESC NULLS LAST, fi.id DESC` (primer match por nombre = factura más reciente). Usado por `TrazabilidadController.suggestItemsPorNombre()` → `GET /suggest-items-por-nombre?nombres=...` para auto-sugerir costos al cargar una receta.
- `search(q, tipo, Pageable)` — `JOIN FETCH fi.factura`, filtra por `q` (LIKE en `fi.nombre`, `f.numeroFactura`, `f.proveedor`; `:q = ''` = sin filtro) y `tipo` (`fi.tipoInsumo = :tipo`; `:tipo = ''` = sin filtro), orden `f.fechaFactura DESC NULLS LAST, fi.id DESC`. Usado por `TrazabilidadController.suggestItems()` → `GET /suggest-items` (buscador AJAX de "Costos de Producción", paginado a 30).
- Usado también por `TrazabilidadService.mapearDto()` para resolver ítems por ID al guardar lotes

### MantenimientoEquipoRepository
- `JpaRepository<MantenimientoEquipo, Long>`
- `findByEquipoIdOrderByFechaDesc(equipoId)` — historial de mantenimientos de un equipo, orden cronológico inverso
- `findMantenimientoPendiente(fecha)` — equipos cuyo `proximoMantenimiento <= :fecha`; usado por `EquipoService.listarMantenimientoPendiente()`
- `countMantenimientoPendiente(fecha)` — `COUNT` de equipos con `proximoMantenimiento <= :fecha`; ventana por defecto 7 días
- `sumTotalCostos()` — `SUM(m.costo)` global; usado en el dashboard
- `sumCostoByEquipoId(equipoId)` — `COALESCE(SUM(m.costo), 0)` filtrado por equipo; retorna `BigDecimal` nunca null — para costoTotal en detalle y mantenimientos
- `countByEquipoId(equipoId)` — `COUNT(m)` filtrado por equipo; para totalMantenimientos en detalle y mantenimientos

