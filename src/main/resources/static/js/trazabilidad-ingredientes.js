// ── Wizard tabs ──────────────────────────────────────────────────
const TOTAL_TABS = 6;
let currentTab = 0;
var _recetaPendiente = null; // receta cargada pero aún no aplicada a panel-1

function goTab(idx) {
    document.getElementById('panel-' + currentTab).classList.add('d-none');
    document.querySelectorAll('.wz-tab')[currentTab].classList.remove('active');
    document.getElementById('dot-' + currentTab).classList.remove('active');
    document.getElementById('dot-' + currentTab).classList.add('done');

    currentTab = idx;
    document.getElementById('panel-' + currentTab).classList.remove('d-none');
    document.querySelectorAll('.wz-tab')[currentTab].classList.add('active');

    // Al salir del tab 0, re-habilitar btnNext (el bloqueo por stock es solo en tab 0)
    if (idx !== 0) {
        var _bn = document.getElementById('btnNext');
        if (_bn) { _bn.disabled = false; _bn.title = ''; }
    }

    const allTabs = document.querySelectorAll('.wz-tab');
    for (let i = 0; i < TOTAL_TABS; i++) {
        const dot = document.getElementById('dot-' + i);
        const tab = allTabs[i];
        dot.classList.remove('active', 'done');
        tab.classList.remove('done');
        if (i < currentTab) {
            dot.classList.add('done');
            tab.classList.add('done');
        } else if (i === currentTab) {
            dot.classList.add('active');
        }
    }

    document.getElementById('btnPrev').style.display = currentTab > 0 ? 'inline-flex' : 'none';
    document.getElementById('btnNext').style.display = currentTab < TOTAL_TABS - 1 ? 'inline-flex' : 'none';

    document.querySelector('.wizard-panel').scrollIntoView({ behavior: 'smooth', block: 'start' });
}

function nextTab() { if (currentTab < TOTAL_TABS - 1) goTab(currentTab + 1); }
function prevTab() { if (currentTab > 0) goTab(currentTab - 1); }

document.getElementById('btnPrev').style.display = 'none';

// ── Conversión de volumen ─────────────────────────────────────────
const TO_LITERS = { L: 1, mL: 0.001, gal: 3.78541 };

function volUpdate(field) {
    const displayInput = document.getElementById(field + '-display');
    const unit = document.getElementById(field + '-unit').value;
    const displayVal = window.numVal ? numVal(displayInput.value || 0) : parseFloat(displayInput.value);
    if (isNaN(displayVal)) {
        document.getElementById(field + '-value').value = '';
        document.getElementById(field + '-equiv').textContent = '';
        return;
    }
    const liters = displayVal * TO_LITERS[unit];
    document.getElementById(field + '-value').value = parseFloat(liters.toFixed(6));
    showEquiv(field, liters);
}

function volUnitChange(field) {
    const hiddenVal = parseFloat(document.getElementById(field + '-value').value);
    const newUnit = document.getElementById(field + '-unit').value;
    if (!isNaN(hiddenVal) && hiddenVal > 0) {
        const newDisplay = hiddenVal / TO_LITERS[newUnit];
        document.getElementById(field + '-display').value = parseFloat(newDisplay.toFixed(4));
        showEquiv(field, hiddenVal);
    }
}

function showEquiv(field, liters) {
    const unit = document.getElementById(field + '-unit').value;
    const parts = [];
    if (unit !== 'L')   parts.push(parseFloat((liters).toFixed(3)) + ' L');
    if (unit !== 'mL')  parts.push(parseFloat((liters * 1000).toFixed(1)) + ' mL');
    if (unit !== 'gal') parts.push(parseFloat((liters / 3.78541).toFixed(3)) + ' gal');
    const el = document.getElementById(field + '-equiv');
    if (el) el.textContent = parts.length ? '≈ ' + parts.join(' · ') : '';
}

window.addEventListener('DOMContentLoaded', () => {
    ['agua', 'litros'].forEach(field => {
        const val = parseFloat(document.getElementById(field + '-value').value);
        if (!isNaN(val) && val > 0) showEquiv(field, val);
    });
});

// ── Ingredientes dinámicos ────────────────────────────────────────
function removeRow(btn) {
    btn.closest('.ingrediente-row').remove();
    renumberRows();
}

function renumberRows() {
    ['maltas', 'lupulos', 'levaduras', 'clarificantes'].forEach(tipo => {
        const c = document.getElementById(tipo + '-container');
        if (!c) return;
        c.querySelectorAll('.ingrediente-row').forEach((row, idx) => {
            row.querySelectorAll('input, select').forEach(el => {
                if (el.name) el.name = el.name.replace(/\[\d+\]/, '[' + idx + ']');
            });
        });
    });
}

const UNIT_OPTIONS = `
    <option value="gr">gr</option>
    <option value="kg">kg</option>
    <option value="mL">mL</option>
    <option value="L">L</option>
    <option value="gal">gal</option>`;
const UNIT_OPTIONS_CLAR = UNIT_OPTIONS + `
    <option value="und">und</option>`;

function addRow(containerId, tipo, listId, placeholder) {
    const container = document.getElementById(containerId);
    const idx = container.querySelectorAll('.ingrediente-row').length;
    container.insertAdjacentHTML('beforeend', `
        <div class="ingrediente-row">
            <div class="row g-2 align-items-center">
                <div class="col-md-5">
                    <input type="text" name="${tipo}[${idx}].nombre" class="form-control form-control-sm"
                           placeholder="${placeholder}" list="${listId}">
                </div>
                <div class="col-md-3">
                    <input type="text" name="${tipo}[${idx}].cantidad" class="form-control form-control-sm" placeholder="Cantidad">
                </div>
                <div class="col-md-3">
                    <select name="${tipo}[${idx}].unidad" class="form-select form-select-sm">
                        ${tipo === 'clarificantes' ? UNIT_OPTIONS_CLAR : UNIT_OPTIONS}
                    </select>
                </div>
                <div class="col-md-1 text-end">
                    <button type="button" class="btn-remove-ingrediente" onclick="removeRow(this)">
                        <i class="bi bi-x"></i>
                    </button>
                </div>
            </div>
        </div>`);
}

// ── Conversión de unidades para comparar stock ────────────────────
const BASE_FACTOR_STOCK = { gr: 1, kg: 1000, ml: 1, l: 1000, gal: 3785.41, und: 1 };
const BASE_UNIT_STOCK   = { gr: 'gr', kg: 'gr', ml: 'ml', l: 'ml', gal: 'ml', und: 'und' };

function toBaseStock(amount, unit) {
    var u = (unit || 'gr').toLowerCase();
    return (parseFloat(amount) || 0) * (BASE_FACTOR_STOCK[u] || 1);
}
function baseUnitOf(unit) {
    var u = (unit || 'gr').toLowerCase();
    return BASE_UNIT_STOCK[u] || 'gr';
}

// ── Cargar receta en lote ─────────────────────────────────────────
function cargarRecetaEnLote() {
    var sel = document.getElementById('recetaSelectLote');
    if (!sel) return;
    var id = sel.value;
    if (!id) { alert('Seleccioná una receta primero'); return; }
    document.getElementById('recetaIdHidden').value = id;
    fetch('/recetas/api/' + id)
        .then(function(r) { return r.json(); })
        .then(function(data) {
            _recetaPendiente = data; // guardar para poblar panel-1 al hacer clic en Aplicar

            // 1. Estilo (solo si está vacío)
            if (data.estilo) {
                var estiloEl = document.querySelector('[name="estilo"]');
                if (estiloEl && !estiloEl.value) estiloEl.value = data.estilo;
            }

            // 2. Agua de macerado
            if (data.aguaMacerado != null) {
                var rawU = (data.unidadAguaMacerado || 'L');
                var unit = rawU === 'mL' ? 'mL' : rawU === 'gal' ? 'gal' : 'L';
                document.getElementById('agua-unit').value = unit;
                document.getElementById('agua-display').value = data.aguaMacerado;
                var liters = parseFloat(data.aguaMacerado) * TO_LITERS[unit];
                document.getElementById('agua-value').value = liters.toFixed(6);
                showEquiv('agua', liters);
            }

            // 3. Volumen base → litros finales
            if (data.volumenBase != null) {
                document.getElementById('litros-unit').value = 'L';
                document.getElementById('litros-display').value = data.volumenBase;
                document.getElementById('litros-value').value = parseFloat(data.volumenBase);
                showEquiv('litros', parseFloat(data.volumenBase));
            }

            // 4. Densidades
            if (data.ogObjetivo != null) {
                var ogEl = document.querySelector('[name="densidadInicial"]');
                if (ogEl) ogEl.value = data.ogObjetivo;
            }
            if (data.fgObjetivo != null) {
                var fgEl = document.querySelector('[name="densidadFinal"]');
                if (fgEl) fgEl.value = data.fgObjetivo;
            }

            // 5. pH del Agua
            if (data.phAgua != null) {
                var phEl = document.querySelector('[name="phAgua"]');
                if (phEl) phEl.value = data.phAgua;
            }

            // 6. Verificar stock e integrar con costos
            verificarStockReceta(data);
        })
        .catch(function() { alert('Error al cargar la receta'); });
}

// ── Verificar stock de ingredientes de la receta ──────────────────
function verificarStockReceta(data) {
    var grupos = [
        { key: 'maltas',        containerId: 'maltas-container',        tipo: 'MALTA' },
        { key: 'lupulos',       containerId: 'lupulos-container',       tipo: 'LUPULO' },
        { key: 'levaduras',     containerId: 'levaduras-container',     tipo: 'LEVADURA' },
        { key: 'clarificantes', containerId: 'clarificantes-container', tipo: 'CLARIFICANTE' }
    ];

    var advertencias = [];
    var costosSugeridos = [];

    grupos.forEach(function(cfg) {
        var items = data[cfg.key] || [];
        var container = document.getElementById(cfg.containerId);
        if (!container) return;
        var rows = container.querySelectorAll('.ingrediente-row');

        items.forEach(function(item, idx) {
            if (!item.nombre || !item.cantidad) return;
            var cantRecetaBase = toBaseStock(parseFloat(item.cantidad) || 0, item.unidad);
            var baseReceta = baseUnitOf(item.unidad);

            var nombreNorm = (item.nombre || '').toLowerCase().trim();
            var stockItem = (INVENTARIO_STOCK || []).find(function(s) {
                return s.nombre === nombreNorm;
            });

            var cantStockBase = stockItem ? toBaseStock(parseFloat(stockItem.cantidad) || 0, stockItem.unidad) : 0;
            var baseStock = stockItem ? baseUnitOf(stockItem.unidad) : baseReceta;

            var insuficiente = !stockItem || (baseReceta === baseStock && cantStockBase < cantRecetaBase);

            // Siempre buscar ítem de factura para todos los ingredientes
            var costoItem = encontrarItemCostoPorNombre(item.nombre);
            if (costoItem) {
                costosSugeridos.push(Object.assign({}, costoItem, {
                    cantidadReceta: parseFloat(item.cantidad) || 0,
                    unidadReceta:   item.unidad || 'gr'
                }));
            }

            if (insuficiente) {
                advertencias.push(item.nombre);
                if (rows[idx]) marcarIngredienteBajoStock(rows[idx], item, stockItem, cfg.tipo);
            }
        });
    });

    // Bloquear/habilitar "Aplicar a Receta e Insumos" y "Siguiente" según stock
    _actualizarEstadoAplicar(advertencias.length > 0);

    // Delega la integración con costos al archivo trazabilidad-costos.js
    if (typeof autoAgregarCostosReceta === 'function') {
        autoAgregarCostosReceta(costosSugeridos, advertencias);
    }
}

function _actualizarEstadoAplicar(bloqueado) {
    var btnAplicar = document.getElementById('btnAplicarReceta');
    var btnNext = document.getElementById('btnNext');
    if (btnAplicar) {
        btnAplicar.disabled = bloqueado;
        btnAplicar.title = bloqueado ? 'Corregí los avisos de stock antes de aplicar' : '';
    }
    if (btnNext && currentTab === 0) {
        btnNext.disabled = bloqueado;
        btnNext.title = bloqueado ? 'Corregí los avisos de stock antes de continuar' : '';
    }
}

// ── Marcar fila con badge de bajo stock + panel de reemplazo ──────
function marcarIngredienteBajoStock(row, item, stockItem, tipo) {
    if (row.querySelector('.stock-badge')) return; // ya marcado
    row.style.borderColor = '#ffc107';
    row.style.background = '#fffdf0';

    var nameInput = row.querySelector('input[type="text"]');
    if (nameInput) {
        var disponible = stockItem
            ? 'Disponible: ' + (parseFloat(stockItem.cantidad) || 0) + ' ' + (stockItem.unidad || '')
            : 'No está en inventario';
        var badge = document.createElement('div');
        badge.className = 'stock-badge d-flex align-items-center gap-2 mb-1';
        badge.innerHTML =
            '<span class="badge bg-warning text-dark" style="font-size:0.65rem;">' +
            '<i class="bi bi-exclamation-triangle-fill me-1"></i>Stock insuficiente</span>' +
            '<small class="text-muted">' + disponible + '</small>';
        nameInput.parentNode.insertBefore(badge, nameInput);
    }

    var panel = construirPanelReemplazo(item, stockItem, tipo, row);
    row.appendChild(panel);
}

// ── Panel de reemplazo por ingrediente alternativo ────────────────
function construirPanelReemplazo(item, stockItem, tipo, row) {
    var alternativas = (INVENTARIO_STOCK || []).filter(function(s) {
        return s.tipo === tipo &&
               s.nombre !== (item.nombre || '').toLowerCase().trim() &&
               parseFloat(s.cantidad) > 0;
    });

    var div = document.createElement('div');
    div.className = 'reemplazo-panel mt-2 pt-2 border-top';

    if (!alternativas.length) {
        div.innerHTML = '<small class="text-muted"><i class="bi bi-info-circle me-1"></i>No hay alternativas con stock disponible.</small>';
    } else {
        var opts = alternativas.map(function(a) {
            var nombreDisplay = a.nombre.charAt(0).toUpperCase() + a.nombre.slice(1);
            return '<option value="' + escAttr(a.nombre) + '">' +
                   nombreDisplay + ' (' + (parseFloat(a.cantidad) || 0) + ' ' + (a.unidad || '') + ')</option>';
        }).join('');
        div.innerHTML =
            '<div class="d-flex align-items-center gap-2 flex-wrap">' +
            '<small class="fw-600 text-warning-emphasis"><i class="bi bi-arrow-left-right me-1"></i>Reemplazar con:</small>' +
            '<select class="form-select form-select-sm reemplazo-select" style="max-width:300px;">' +
            '<option value="">— Seleccionar alternativa —</option>' + opts +
            '</select>' +
            '<button type="button" class="btn btn-sm btn-outline-warning py-0" onclick="aplicarReemplazo(this)">' +
            '<i class="bi bi-check-lg me-1"></i>Aplicar</button>' +
            '</div>';
    }
    return div;
}

function aplicarReemplazo(btn) {
    var panel = btn.closest('.reemplazo-panel');
    var sel = panel.querySelector('.reemplazo-select');
    if (!sel || !sel.value) return;
    var nuevoNombre = sel.value.charAt(0).toUpperCase() + sel.value.slice(1);
    var row = panel.closest('.ingrediente-row');
    var nameInput = row.querySelector('input[type="text"]');
    if (nameInput) {
        nameInput.value = nuevoNombre;
        row.style.borderColor = '';
        row.style.background = '';
        var badge = row.querySelector('.stock-badge');
        if (badge) badge.remove();
        panel.remove();
    }
}

function encontrarItemCostoPorNombre(nombre) {
    var nombreLower = (nombre || '').toLowerCase().trim();
    return (ITEMS_FACTURA || []).find(function(it) {
        return (it.nombre || '').toLowerCase().trim() === nombreLower;
    }) || null;
}

function escAttr(s) {
    return (s || '').replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

function poblarDesdeReceta(containerId, tipo, listId, placeholder, items) {
    const container = document.getElementById(containerId);
    container.innerHTML = '';
    const list = items.length > 0 ? items : [{ nombre: '', cantidad: '', unidad: 'gr' }];
    list.forEach((item, idx) => {
        const nombre   = (item.nombre   || '').replace(/"/g, '&quot;');
        const cantidad = (item.cantidad || '').replace(/"/g, '&quot;');
        const unidad   = item.unidad || 'gr';
        container.insertAdjacentHTML('beforeend', `
            <div class="ingrediente-row">
                <div class="row g-2 align-items-center">
                    <div class="col-md-5">
                        <input type="text" name="${tipo}[${idx}].nombre" value="${nombre}"
                               class="form-control form-control-sm" placeholder="${placeholder}" list="${listId}">
                    </div>
                    <div class="col-md-3">
                        <input type="text" name="${tipo}[${idx}].cantidad" value="${cantidad}"
                               class="form-control form-control-sm" placeholder="Cantidad">
                    </div>
                    <div class="col-md-3">
                        <select name="${tipo}[${idx}].unidad" class="form-select form-select-sm">
                            ${unitOptionsSelected(unidad, tipo === 'clarificantes')}
                        </select>
                    </div>
                    <div class="col-md-1 text-end">
                        <button type="button" class="btn-remove-ingrediente" onclick="removeRow(this)">
                            <i class="bi bi-x"></i>
                        </button>
                    </div>
                </div>
            </div>`);
    });
}

function unitOptionsSelected(selected, includePcs) {
    var units = ['gr', 'kg', 'mL', 'L', 'gal'];
    if (includePcs) units.push('und');
    return units.map(u =>
        `<option value="${u}"${u === selected ? ' selected' : ''}>${u}</option>`
    ).join('');
}
