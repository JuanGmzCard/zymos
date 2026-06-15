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
- **Alertas de plan**: tras las alertas de facturas, calcula `loteCervezaRepo.count()` y `usuarioRepo.countByTenantId(subdomain)` y llama `notificacionService.crearAlertaPlan(tenant, totalLotes, totalUsuarios)` — ver `NotificacionService`.
- Loggea resumen: "N notificación(es) in-app creada(s), M email(s) enviado(s) de K tenant(s)"
- Inyecta `NotificacionService`, `FacturaProveedorService`, `VentaService`, `LoteCervezaRepository` y `UsuarioRepository`.

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
- `crearAlertaPlan(tenant, totalLotes, totalUsuarios)` — notificaciones in-app sobre el estado del plan, con deduplicación diaria:
  - `PLAN_VENCIMIENTO` (dedup. por `existeEnPeriodo(PLAN_VENCIMIENTO, ...)`): si `tenant.isPlanVencido()` → "Plan vencido"; si `tenant.isPlanPorVencer()` (≤7 días) → "Plan por vencer". `urlAccion=null`.
  - `PLAN_LIMITE` (dedup. por `existeEnPeriodo(PLAN_LIMITE, ...)`): si `totalLotes >= maxLotes` → "Límite de lotes alcanzado"; si `>= maxLotes*0.9` → "Cerca del límite de lotes"; análogo para `totalUsuarios`/`maxUsuarios`. Solo evalúa lotes antes que usuarios (un único tipo de alerta por día). `maxLotes`/`maxUsuarios` null = sin límite, no genera alerta.
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

