# Zymos — Resumen Ejecutivo

## ¿Qué es Zymos?

Zymos es una plataforma SaaS multi-tenant de gestión integral para cervecerías artesanales. Corre en la nube y sirve a múltiples clientes de forma simultánea, con datos completamente aislados por subdominio (`cliente.zymos.app`). Cada cervecería accede a su propio entorno sin infraestructura propia.

---

## Problema que resuelve

Las cervecerías artesanales gestionan simultáneamente inventario de insumos perecederos, recetas técnicas, múltiples lotes en distintas fases de fermentación, entregas a clientes, compras a proveedores y cumplimiento de normas de inocuidad alimentaria (BPM). Hacerlo con hojas de cálculo genera errores de trazabilidad, pérdida de historial y dificultad para escalar. Zymos centraliza todo esto en una sola herramienta accesible desde cualquier navegador.

---

## Módulos principales

| Módulo | Descripción |
|---|---|
| **Trazabilidad de lotes** | Ciclo de vida completo de cada lote: planificación, cocción (1-3 sesiones), fermentación, acondicionamiento, maduración, carbonatación y despacho. Registro de densidades, temperaturas, lecturas diarias y evaluaciones BJCP. |
| **Recetas** | Biblioteca de recetas con ingredientes, escalones de macerado, adiciones de hervor, objetivos OG/FG/ABV, agua de macerado y sparge. Calculadora de escala incorporada. |
| **Inventario** | Control de stock de insumos (maltas, lúpulos, levaduras, clarificantes, envases, químicos). Alertas de bajo stock y vencimientos próximos. Descuento automático al crear lotes. |
| **Equipos** | Registro de fermentadores, ollas, enfriadores y demás equipos con historial de mantenimientos preventivos, correctivos y calibraciones. |
| **BPM** | Módulo de Buenas Prácticas de Manufactura: estado de salud diario con firma digital, control de plagas, soluciones desinfectantes, limpieza y evacuación de residuos. Bloqueo automático de operarios con síntomas no autorizados. PDFs por registro. |
| **Comercial** | Gestión de proveedores, facturas de compra con ítems, IVA por ítem, seguimiento de estado (Recibida → Verificada → Pagada) y asignación de costos a lotes. |
| **Ventas** | Cotizaciones y despachos a clientes con seguimiento de estado, ítems por lote y cálculo de descuentos. |
| **Barriles** | Trazabilidad de barriles por código, asociados a lotes y clientes, con estados (Disponible, Lleno, Despachado, Limpieza, Baja). |
| **Órdenes de compra** | Emisión y seguimiento de OC a proveedores, con ítems de insumos y equipos. |
| **Planificación** | Calendario de elaboraciones futuras con FullCalendar, enlazadas a recetas y lotes. |
| **Reportes** | Dashboard de métricas, reportes de producción por estilo y período, exportación a Excel (.xlsx) y PDF. |
| **Notificaciones** | Alertas in-app de bajo stock, vencimientos, mantenimientos, salud BPM y estado del plan, filtradas según el rol del usuario. |
| **Migración de datos** | Importación masiva vía plantillas XLSX para onboarding de nuevos tenants (11 módulos: almacén, equipos, comercial, producción, clientes, ventas, barriles, órdenes, seguimiento, catálogos, mantenimientos). |

---

## Arquitectura y tecnología

- **Backend**: Spring Boot 3.4.4 + Java 21, Spring Data JPA (Hibernate 6), Spring Security
- **Base de datos**: PostgreSQL con Flyway (73 migraciones versionadas). Multi-tenant via filtro `@TenantId` de Hibernate — un schema, datos aislados por columna.
- **Frontend**: Thymeleaf 3.1 + Bootstrap 5.3 + Chart.js + FullCalendar. Dark mode completo. Internacionalización ES/EN.
- **Generación de documentos**: PDF (OpenPDF), Excel (Apache POI)
- **API REST**: JWT HS256 con JJWT + rate limiting (100 req/min por IP)
- **Búsqueda**: typeahead global + suggest por módulo
- **Cache**: Caffeine en memoria con TTL configurable
- **Métricas**: Micrometer + Prometheus + Spring Boot Actuator
- **CI/CD**: GitHub Actions (build + tests + SpotBugs en paralelo), Dependabot semanal
- **Deploy**: Docker + docker-compose. Perfil `prod` con cookies seguras, cache Thymeleaf y pool HikariCP ampliado.

---

## Seguridad

- Autenticación con BCrypt, protección contra fuerza bruta (bloqueo configurable por intentos/minutos)
- **RBAC dinámico por tenant**: cada cervecería define sus propios roles con permisos granulares por módulo (Ver / Crear / Editar / Eliminar). 5 roles de sistema predefinidos + roles custom ilimitados.
- Content Security Policy (CSP) enforced: sin scripts inline, nonces en todos los `<script>` y `<style>` dinámicos.
- Sesiones con timeout de 30 minutos. Cookies `HttpOnly`, `Secure`, `SameSite=Strict` en producción.
- Log de accesos en base de datos, separado de la transacción principal.
- **BpmSaludFilter**: intercepta requests de operarios para verificar registro diario de salud; bloquea acceso si hay síntomas no autorizados por un administrador.

---

## Modelo de negocio (SaaS multi-tenant)

- Un tenant = una cervecería, identificada por subdominio
- Cada tenant tiene su propio **branding** (logo, colores primario/acento/navbar, tipografías), configurable desde el panel de administración sin reiniciar la app
- **Plan con límites**: `maxLotes`, `maxUsuarios`, `planFin`. Alertas automáticas al 90% y 100% de uso. Bloqueo tras 7 días de gracia post-vencimiento con redirección a página de renovación.
- **Onboarding**: el módulo de migración permite importar el historial completo de una cervecería en minutos desde hojas de cálculo existentes.

---

## Estado actual

| Área | Estado |
|---|---|
| Trazabilidad + Recetas | Completo — multi-cocción, Brix/Terrill, BJCP |
| Inventario + Equipos | Completo |
| BPM | Completo — firma digital, bloqueo por salud, PDFs |
| Comercial + Ventas + Barriles + OC | Completo |
| RBAC dinámico | Completo (V72+V73) |
| Internacionalización ES/EN | Cobertura completa — todos los templates, PDFs y controllers |
| Dark mode | Auditoría completa — todos los templates cubiertos |
| API REST con JWT | Completo |
| Migración de datos (11 módulos) | Completo con plantillas XLSX y log de auditoría |
| CSP enforced | Completo (Fase D) |
| Tests | 912 tests (unitarios + integración con Testcontainers) |

---

## Diferenciadores clave

1. **Multi-tenant real** — un solo deploy sirve a N cervecerías con aislamiento completo de datos y branding propio por cliente.
2. **BPM integrado** — el módulo de inocuidad alimentaria está embebido en el flujo operativo diario, no es un addon externo.
3. **RBAC granular por tenant** — cada cervecería define quién puede hacer qué en cada módulo, sin depender de roles fijos del sistema.
4. **Onboarding acelerado** — el migrador XLSX permite pasar de cero a operativo en minutos importando el historial existente en hojas de cálculo.
5. **Sin dependencia de infraestructura del cliente** — accesible desde cualquier navegador; el cliente solo necesita una cuenta.
