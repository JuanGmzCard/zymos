-- V73: Migrar 5 roles enum a sistema RBAC dinámico
-- Agrega columna es_admin, crea roles de sistema para todos los tenants y migra usuarios

ALTER TABLE roles_tenant ADD COLUMN IF NOT EXISTS es_admin BOOLEAN NOT NULL DEFAULT FALSE;

-- Crear los 5 roles de sistema para todos los tenants existentes
INSERT INTO roles_tenant (tenant_id, nombre, descripcion, activo, es_sistema, es_admin, created_at)
SELECT t.subdomain, r.nombre, r.descripcion, true, true, r.es_admin, NOW()
FROM tenants t
CROSS JOIN (VALUES
    ('Administrador',  'Acceso completo al sistema',                              true),
    ('Producción',     'Gestión de lotes, recetas e inventario de producción',    false),
    ('Inventario',     'Gestión de inventario, equipos y barriles',               false),
    ('Facturación',    'Gestión de facturas, ventas y proveedores',               false),
    ('Equipos',        'Gestión y mantenimiento de equipos',                      false)
) AS r(nombre, descripcion, es_admin)
ON CONFLICT (tenant_id, nombre) DO NOTHING;

-- Permisos Administrador: todos los módulos, todos los permisos
INSERT INTO roles_modulos_permisos (rol_id, modulo, puede_ver, puede_crear, puede_editar, puede_eliminar)
SELECT rt.id, m.modulo, true, true, true, true
FROM roles_tenant rt
CROSS JOIN (VALUES
    ('TRAZABILIDAD'), ('RECETAS'), ('INVENTARIO'), ('FACTURACION'), ('COMERCIAL'),
    ('EQUIPOS'), ('REPORTES'), ('PLANIFICACION'), ('BPM'), ('BARRILES')
) AS m(modulo)
WHERE rt.nombre = 'Administrador' AND rt.es_sistema = true
ON CONFLICT (rol_id, modulo) DO NOTHING;

-- Permisos Producción: TRAZABILIDAD, RECETAS, PLANIFICACION, INVENTARIO, EQUIPOS, BARRILES (todo) + REPORTES (solo ver)
INSERT INTO roles_modulos_permisos (rol_id, modulo, puede_ver, puede_crear, puede_editar, puede_eliminar)
SELECT rt.id, m.modulo, true, true, true, true
FROM roles_tenant rt
CROSS JOIN (VALUES
    ('TRAZABILIDAD'), ('RECETAS'), ('PLANIFICACION'), ('INVENTARIO'), ('EQUIPOS'), ('BARRILES')
) AS m(modulo)
WHERE rt.nombre = 'Producción' AND rt.es_sistema = true
ON CONFLICT (rol_id, modulo) DO NOTHING;

INSERT INTO roles_modulos_permisos (rol_id, modulo, puede_ver, puede_crear, puede_editar, puede_eliminar)
SELECT rt.id, 'REPORTES', true, false, false, false
FROM roles_tenant rt
WHERE rt.nombre = 'Producción' AND rt.es_sistema = true
ON CONFLICT (rol_id, modulo) DO NOTHING;

-- Permisos Inventario: INVENTARIO, RECETAS, EQUIPOS, BARRILES (todo)
INSERT INTO roles_modulos_permisos (rol_id, modulo, puede_ver, puede_crear, puede_editar, puede_eliminar)
SELECT rt.id, m.modulo, true, true, true, true
FROM roles_tenant rt
CROSS JOIN (VALUES ('INVENTARIO'), ('RECETAS'), ('EQUIPOS'), ('BARRILES')) AS m(modulo)
WHERE rt.nombre = 'Inventario' AND rt.es_sistema = true
ON CONFLICT (rol_id, modulo) DO NOTHING;

-- Permisos Facturación: FACTURACION, COMERCIAL, REPORTES (todo)
INSERT INTO roles_modulos_permisos (rol_id, modulo, puede_ver, puede_crear, puede_editar, puede_eliminar)
SELECT rt.id, m.modulo, true, true, true, true
FROM roles_tenant rt
CROSS JOIN (VALUES ('FACTURACION'), ('COMERCIAL'), ('REPORTES')) AS m(modulo)
WHERE rt.nombre = 'Facturación' AND rt.es_sistema = true
ON CONFLICT (rol_id, modulo) DO NOTHING;

-- Permisos Equipos: EQUIPOS (todo)
INSERT INTO roles_modulos_permisos (rol_id, modulo, puede_ver, puede_crear, puede_editar, puede_eliminar)
SELECT rt.id, 'EQUIPOS', true, true, true, true
FROM roles_tenant rt
WHERE rt.nombre = 'Equipos' AND rt.es_sistema = true
ON CONFLICT (rol_id, modulo) DO NOTHING;

-- Migrar todos los usuarios existentes: asignar rol_custom_id al rol de sistema correspondiente
UPDATE usuarios u
SET rol_custom_id = rt.id
FROM roles_tenant rt
WHERE rt.tenant_id = u.tenant_id
  AND rt.es_sistema = true
  AND rt.nombre = CASE u.rol
      WHEN 'ADMIN'       THEN 'Administrador'
      WHEN 'PRODUCCION'  THEN 'Producción'
      WHEN 'INVENTARIO'  THEN 'Inventario'
      WHEN 'FACTURACION' THEN 'Facturación'
      WHEN 'EQUIPOS'     THEN 'Equipos'
  END
  AND u.rol_custom_id IS NULL;
