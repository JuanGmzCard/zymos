// ── Wizard tabs ──────────────────────────────────────────────────
const TOTAL_TABS = 6;
let currentTab = 0;
var _recetaPendiente = null; // receta cargada pero aún no aplicada a panel-1
var _recetaData2 = null;    // datos de receta sesión 2 (para re-append tras poblarDesdeReceta)
var _recetaData3 = null;    // datos de receta sesión 3
var _recetaData4 = null;    // datos de receta sesión 4

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
    if (field === 'litros1' || field === 'litros2' || field === 'litros3' || field === 'litros4') {
        sincronizarVolumenFinalTotal();
    }
}

function sincronizarVolumenFinalTotal() {
    var sel = document.getElementById('numeroElaboracionesSelect');
    if (!sel) return;
    var n = parseInt(sel.value, 10) || 1;
    var displayEl = document.getElementById('litros-display');
    var hiddenEl  = document.getElementById('litros-value');
    if (!displayEl || !hiddenEl) return;
    if (n < 2) {
        displayEl.removeAttribute('readonly');
        return;
    }
    var v1 = parseFloat(document.getElementById('litros1-value').value) || 0;
    var v2 = parseFloat(document.getElementById('litros2-value').value) || 0;
    var v3 = n >= 3 ? (parseFloat(document.getElementById('litros3-value').value) || 0) : 0;
    var v4 = n >= 4 ? (parseFloat(document.getElementById('litros4-value').value) || 0) : 0;
    var totalLiters = v1 + v2 + v3 + v4;
    hiddenEl.value = parseFloat(totalLiters.toFixed(6));
    var unitEl = document.getElementById('litros-unit');
    var unit = unitEl ? unitEl.value : 'L';
    displayEl.value = parseFloat((totalLiters / TO_LITERS[unit]).toFixed(4));
    displayEl.setAttribute('readonly', '');
    showEquiv('litros', totalLiters);
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
    ['agua', 'litros', 'agua2', 'agua3', 'agua4', 'litros1', 'litros2', 'litros3', 'litros4'].forEach(field => {
        const el = document.getElementById(field + '-value');
        if (!el) return;
        const val = parseFloat(el.value);
        if (!isNaN(val) && val > 0) showEquiv(field, val);
    });
    sincronizarVolumenFinalTotal();
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
                    <button type="button" class="btn-remove-ingrediente">
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

// ── Cargar receta adicional (sesión 2 / 3) — hace append, no reemplaza ───────
function cargarRecetaAdicional(selId, hiddenId, btn) {
    var sel = document.getElementById(selId);
    if (!sel) return;
    var id = sel.value;
    if (!id) { alert('Seleccioná una receta primero'); return; }
    document.getElementById(hiddenId).value = id;

    var sesion = selId.indexOf('4') !== -1 ? 4 : selId.indexOf('3') !== -1 ? 3 : 2;
    var originalHtml = btn ? btn.innerHTML : '';
    if (btn) { btn.innerHTML = '<i class="bi bi-hourglass-split me-1"></i>Cargando...'; btn.disabled = true; }

    fetch('/recetas/api/' + id)
        .then(function(r) { return r.json(); })
        .then(function(data) { procesarRecetaAdicional(data, btn, originalHtml, sesion); })
        .catch(function() {
            if (btn) { btn.innerHTML = originalHtml; btn.disabled = false; }
            alert('Error al cargar la receta');
        });
}

function procesarRecetaAdicional(data, btn, originalHtml, sesion) {
    var grupos = [
        { key: 'maltas',        containerId: 'maltas-container',        tipo: 'MALTA',        listId: 'lista-maltas',        ph: 'Malta' },
        { key: 'lupulos',       containerId: 'lupulos-container',       tipo: 'LUPULO',       listId: 'lista-lupulos',       ph: 'Lúpulo' },
        { key: 'levaduras',     containerId: 'levaduras-container',     tipo: 'LEVADURA',     listId: 'lista-levaduras',     ph: 'Levadura' },
        { key: 'clarificantes', containerId: 'clarificantes-container', tipo: 'CLARIFICANTE', listId: 'lista-clarificantes', ph: 'Clarificante' }
    ];

    // Fase 1: append sincrónico — contar filas agregadas y guardar referencias
    var totalAgregados = 0;
    var nuevasFilas = {};
    grupos.forEach(function(cfg) {
        nuevasFilas[cfg.tipo] = [];
        (data[cfg.key] || []).forEach(function(item) {
            if (!item.nombre && !item.cantidad) return;
            var row = appendIngredienteRow(cfg.containerId, cfg.key, cfg.listId, cfg.ph, item);
            nuevasFilas[cfg.tipo].push({ row: row, item: item });
            totalAgregados++;
        });
    });

    // Restaurar botón y mostrar feedback inmediatamente
    if (btn) { btn.innerHTML = originalHtml; btn.disabled = false; }
    mostrarFeedbackReceta(btn, totalAgregados);

    // Guardar datos para re-append si poblarDesdeReceta borra las filas
    if (sesion === 2) _recetaData2 = data;
    if (sesion === 3) _recetaData3 = data;
    if (sesion === 4) _recetaData4 = data;

    // Poblar agua y volumen final para la sesión adicional
    if (sesion === 2 || sesion === 3 || sesion === 4) {
        var _mL = 0, _sL = 0;
        if (data.aguaMacerado != null) {
            var _ru = (data.unidadAguaMacerado || 'L');
            var _um = _ru === 'mL' ? 'mL' : _ru === 'gal' ? 'gal' : 'L';
            _mL = parseFloat(data.aguaMacerado) * TO_LITERS[_um];
        }
        if (data.aguaSparge != null) {
            var _rus = (data.unidadAguaSparge || 'L');
            var _us = _rus === 'mL' ? 'mL' : _rus === 'gal' ? 'gal' : 'L';
            _sL = parseFloat(data.aguaSparge) * TO_LITERS[_us];
        }
        var _tL = _mL + _sL;
        var _af = 'agua' + sesion;
        if (_tL > 0 && document.getElementById(_af + '-unit')) {
            document.getElementById(_af + '-unit').value = 'L';
            document.getElementById(_af + '-display').value = parseFloat(_tL.toFixed(3));
            document.getElementById(_af + '-value').value = _tL.toFixed(6);
            showEquiv(_af, _tL);
        }
        if (data.volumenBase != null) {
            var _lf = 'litros' + sesion;
            var _lfUnit = document.getElementById(_lf + '-unit');
            if (_lfUnit) {
                _lfUnit.value = 'L';
                document.getElementById(_lf + '-display').value = parseFloat(data.volumenBase);
                document.getElementById(_lf + '-value').value = parseFloat(data.volumenBase);
                showEquiv(_lf, parseFloat(data.volumenBase));
                sincronizarVolumenFinalTotal();
            }
        }
    }

    // Fase 2: verificar stock y agregar costos (asíncrono)
    var nombres = [];
    grupos.forEach(function(cfg) {
        (data[cfg.key] || []).forEach(function(item) { if (item.nombre) nombres.push(item.nombre); });
    });

    if (!nombres.length) {
        if (typeof autoAgregarCostosReceta === 'function') autoAgregarCostosReceta([], []);
        mostrarFeedbackCostos(btn, 0, !!document.getElementById('costos-collapse'), 0);
        return;
    }

    var qs = nombres.map(function(n) { return 'nombres=' + encodeURIComponent(n); }).join('&');
    fetch('/suggest-items-por-nombre?' + qs)
        .then(function(r) { return r.json(); })
        .then(function(costosPorNombre) {
            var costosSugeridos = [], advertencias = [];
            grupos.forEach(function(cfg) {
                (nuevasFilas[cfg.tipo] || []).forEach(function(entry) {
                    var item = entry.item;
                    if (!item.nombre || !item.cantidad) return;
                    var nombreNorm = item.nombre.toLowerCase().trim();
                    var stockItem = (INVENTARIO_STOCK || []).find(function(s) { return s.nombre === nombreNorm; });
                    var cantBase = toBaseStock(parseFloat(item.cantidad) || 0, item.unidad);
                    var baseR = baseUnitOf(item.unidad);
                    var cantStock = stockItem ? toBaseStock(parseFloat(stockItem.cantidad) || 0, stockItem.unidad) : 0;
                    var baseS = stockItem ? baseUnitOf(stockItem.unidad) : baseR;
                    if (!stockItem || (baseR === baseS && cantStock < cantBase)) {
                        advertencias.push(item.nombre);
                        marcarIngredienteBajoStock(entry.row, item, stockItem, cfg.tipo);
                    }
                    var costoItem = (costosPorNombre || {})[nombreNorm] || null;
                    if (costoItem) {
                        costosSugeridos.push(Object.assign({}, costoItem, {
                            cantidadReceta: parseFloat(item.cantidad) || 0,
                            unidadReceta:   item.unidad || 'gr'
                        }));
                    }
                });
            });
            var hayPanel = !!document.getElementById('costos-collapse');
            var totalSugeridos = costosSugeridos.length;
            var costosAgregados = 0;
            if (typeof autoAgregarCostosReceta === 'function') {
                // acumular=true: elaboración adicional suma cantidad a ítems ya asignados
                costosAgregados = autoAgregarCostosReceta(costosSugeridos, advertencias, true) || 0;
            }
            mostrarFeedbackCostos(btn, costosAgregados, hayPanel, totalSugeridos);
        })
        .catch(function() {
            if (typeof autoAgregarCostosReceta === 'function') autoAgregarCostosReceta([], []);
            mostrarFeedbackCostos(btn, 0, !!document.getElementById('costos-collapse'), 0);
        });
}

function mostrarFeedbackReceta(btn, count) {
    if (!btn) return;
    var parent = btn.parentNode;
    var existing = parent.querySelector('.receta-sesion-feedback');
    if (existing) existing.remove();
    var span = document.createElement('span');
    span.className = 'receta-sesion-feedback ms-2 badge';
    if (count > 0) {
        span.style.cssText = 'background:#27ae60;color:#fff;font-size:0.75rem;';
        span.innerHTML = '<i class="bi bi-check-lg me-1"></i>' + count + ' ingrediente' + (count > 1 ? 's' : '') + ' agregado' + (count > 1 ? 's' : '');
    } else {
        span.style.cssText = 'background:#6c757d;color:#fff;font-size:0.75rem;';
        span.innerHTML = '<i class="bi bi-info-circle me-1"></i>La receta no tiene ingredientes';
    }
    parent.appendChild(span);
    setTimeout(function() { if (span.parentNode) span.remove(); }, 6000);
}

function mostrarFeedbackCostos(btn, agregados, hayPanel, totalSugeridos) {
    if (!btn) return;
    var parent = btn.parentNode;
    var existing = parent.querySelector('.costos-sesion-feedback');
    if (existing) existing.remove();
    if (!hayPanel) return;
    var span = document.createElement('span');
    span.className = 'costos-sesion-feedback ms-1 badge';
    if (agregados > 0) {
        span.style.cssText = 'background:#2563eb;color:#fff;font-size:0.72rem;';
        span.innerHTML = '<i class="bi bi-receipt me-1"></i>' + agregados + ' costo' + (agregados > 1 ? 's' : '') + ' actualizado' + (agregados > 1 ? 's' : '');
    } else {
        span.style.cssText = 'background:#6c757d;color:#fff;font-size:0.72rem;';
        span.innerHTML = '<i class="bi bi-info-circle me-1"></i>Sin ítems de factura coincidentes';
    }
    parent.appendChild(span);
    setTimeout(function() { if (span.parentNode) span.remove(); }, 6000);
}

// Suma cantidad si el ingrediente ya existe con la misma unidad; si no, agrega nueva fila.
function sumarOAgregarIngrediente(containerId, tipo, listId, ph, item) {
    var container = document.getElementById(containerId);
    var nombreNorm = (item.nombre || '').trim().toLowerCase();
    var itemCant   = parseFloat(item.cantidad) || 0;
    var itemUnit   = item.unidad || 'gr';
    var found = false;
    container.querySelectorAll('.ingrediente-row').forEach(function(row) {
        if (found) return;
        var nameInp = row.querySelector('input[name*=".nombre"]');
        var cantInp = row.querySelector('input[name*=".cantidad"]');
        var unitSel = row.querySelector('select[name*=".unidad"]');
        if (nameInp && cantInp && unitSel
                && nameInp.value.trim().toLowerCase() === nombreNorm
                && unitSel.value === itemUnit) {
            cantInp.value = String((parseFloat(cantInp.value) || 0) + itemCant);
            found = true;
        }
    });
    if (!found) appendIngredienteRow(containerId, tipo, listId, ph, item);
}

function appendIngredienteRow(containerId, tipo, listId, placeholder, item) {
    var container = document.getElementById(containerId);
    var idx = container.querySelectorAll('.ingrediente-row').length;
    var nombre   = (item.nombre   || '').replace(/"/g, '&quot;');
    var cantidad = (item.cantidad || '').replace(/"/g, '&quot;');
    var unidad   = item.unidad || 'gr';
    container.insertAdjacentHTML('beforeend',
        '<div class="ingrediente-row">' +
        '<div class="row g-2 align-items-center">' +
        '<div class="col-md-5">' +
        '<input type="text" name="' + tipo + '[' + idx + '].nombre" value="' + nombre + '"' +
        ' class="form-control form-control-sm" placeholder="' + placeholder + '" list="' + listId + '">' +
        '</div>' +
        '<div class="col-md-3">' +
        '<input type="text" name="' + tipo + '[' + idx + '].cantidad" value="' + cantidad + '"' +
        ' class="form-control form-control-sm" placeholder="Cantidad">' +
        '</div>' +
        '<div class="col-md-3">' +
        '<select name="' + tipo + '[' + idx + '].unidad" class="form-select form-select-sm">' +
        unitOptionsSelected(unidad, tipo === 'clarificantes') +
        '</select>' +
        '</div>' +
        '<div class="col-md-1 text-end">' +
        '<button type="button" class="btn-remove-ingrediente"><i class="bi bi-x"></i></button>' +
        '</div></div></div>');
    return container.lastElementChild;
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

            // 2. Agua Utilizada = Agua Macerado + Agua Sparge (ambas a litros antes de sumar)
            var _aguaMaceradoL = 0, _aguaSpargeL = 0;
            if (data.aguaMacerado != null) {
                var _rawU = (data.unidadAguaMacerado || 'L');
                var _unitM = _rawU === 'mL' ? 'mL' : _rawU === 'gal' ? 'gal' : 'L';
                _aguaMaceradoL = parseFloat(data.aguaMacerado) * TO_LITERS[_unitM];
            }
            if (data.aguaSparge != null) {
                var _rawUS = (data.unidadAguaSparge || 'L');
                var _unitS = _rawUS === 'mL' ? 'mL' : _rawUS === 'gal' ? 'gal' : 'L';
                _aguaSpargeL = parseFloat(data.aguaSparge) * TO_LITERS[_unitS];
            }
            var _totalAguaL = _aguaMaceradoL + _aguaSpargeL;
            if (_totalAguaL > 0) {
                document.getElementById('agua-unit').value = 'L';
                document.getElementById('agua-display').value = parseFloat(_totalAguaL.toFixed(3));
                document.getElementById('agua-value').value = _totalAguaL.toFixed(6);
                showEquiv('agua', _totalAguaL);
            }

            // 3. Volumen base → litros (1 elaboración) o litros1 (multielaboración)
            if (data.volumenBase != null) {
                var _numCoc = parseInt((document.getElementById('numeroElaboracionesSelect') || {}).value) || 1;
                var _vKey = _numCoc >= 2 ? 'litros1' : 'litros';
                var _vDisp = document.getElementById(_vKey + '-display');
                var _vUnit = document.getElementById(_vKey + '-unit');
                var _vVal  = document.getElementById(_vKey + '-value');
                if (_vDisp && _vUnit && _vVal) {
                    _vUnit.value  = 'L';
                    _vDisp.value  = data.volumenBase;
                    _vVal.value   = parseFloat(data.volumenBase);
                    showEquiv(_vKey, parseFloat(data.volumenBase));
                    if (_numCoc >= 2) sincronizarVolumenFinalTotal();
                }
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

    var nombres = [];
    grupos.forEach(function(cfg) {
        (data[cfg.key] || []).forEach(function(item) {
            if (item.nombre) nombres.push(item.nombre);
        });
    });

    if (!nombres.length) {
        procesarStockReceta(data, grupos, {});
        return;
    }

    var qs = nombres.map(function(n) { return 'nombres=' + encodeURIComponent(n); }).join('&');
    fetch('/suggest-items-por-nombre?' + qs)
        .then(function(r) { return r.json(); })
        .then(function(costosPorNombre) { procesarStockReceta(data, grupos, costosPorNombre || {}); })
        .catch(function() { procesarStockReceta(data, grupos, {}); });
}

function procesarStockReceta(data, grupos, costosPorNombre) {
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
            var costoItem = costosPorNombre[nombreNorm] || null;
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
            '<button type="button" class="btn btn-sm btn-outline-warning py-0 btn-aplicar-reemplazo">' +
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
                        <button type="button" class="btn-remove-ingrediente">
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

// ── CSP-safe event listeners ──────────────────────────────────────

// Wizard tab clicks
document.addEventListener('click', function(e) {
    var btn = e.target.closest('.wz-tab[data-tab]');
    if (btn) goTab(parseInt(btn.dataset.tab));
});

// Remove ingredient row (delegation)
document.addEventListener('click', function(e) {
    var btn = e.target.closest('.btn-remove-ingrediente');
    if (btn) removeRow(btn);
});

// Add ingredient row (delegation) — data-container + data-field buttons
document.addEventListener('click', function(e) {
    var btn = e.target.closest('[data-container][data-field]');
    if (btn && !btn.closest('.wz-tab[data-tab]')) {
        addRow(btn.dataset.container, btn.dataset.field, btn.dataset.datalist, btn.dataset.label);
    }
});

// Apply replacement (delegation)
document.addEventListener('click', function(e) {
    var btn = e.target.closest('.btn-aplicar-reemplazo');
    if (btn) aplicarReemplazo(btn);
});

// Volume inputs (oninput via data-vol-field)
document.addEventListener('input', function(e) {
    var field = e.target.dataset.volField;
    if (field) volUpdate(field);
});

// Volume unit selects (onchange via data-vol-unit-field)
document.addEventListener('change', function(e) {
    var field = e.target.dataset.volUnitField;
    if (field) volUnitChange(field);
});

// Wire up prevTab / nextTab / btnCargarReceta in DOMContentLoaded
(function() {
    function initIngredientesListeners() {
        var _prev = document.getElementById('btnPrev');
        var _next = document.getElementById('btnNext');
        var _cargar = document.getElementById('btnCargarReceta');
        if (_prev) _prev.addEventListener('click', prevTab);
        if (_next) _next.addEventListener('click', nextTab);
        if (_cargar) _cargar.addEventListener('click', function() {
            var modalEl = document.getElementById('modalNumeroElaboraciones');
            if (!modalEl) { cargarRecetaEnLote(); return; }
            var curN = (document.getElementById('numeroElaboracionesSelect') || {}).value || '1';
            modalEl.querySelectorAll('.modal-coc-btn').forEach(function(btn) {
                var isActive = btn.getAttribute('data-coc-n') === curN;
                btn.classList.toggle('btn-primary', isActive);
                btn.classList.toggle('btn-outline-secondary', !isActive);
            });
            bootstrap.Modal.getOrCreateInstance(modalEl).show();
        });
        document.querySelectorAll('.modal-coc-btn').forEach(function(btn) {
            btn.addEventListener('click', function() {
                var n = this.getAttribute('data-coc-n');
                var sel = document.getElementById('numeroElaboracionesSelect');
                if (sel && sel.value !== n) {
                    sel.value = n;
                    sel.dispatchEvent(new Event('change'));
                }
                bootstrap.Modal.getInstance(document.getElementById('modalNumeroElaboraciones')).hide();
                cargarRecetaEnLote();
            });
        });
        var _cargar2 = document.getElementById('btnCargarReceta2');
        if (_cargar2) _cargar2.addEventListener('click', function() { cargarRecetaAdicional('recetaSelect2', 'receta2IdHidden', this); });
        var _cargar3 = document.getElementById('btnCargarReceta3');
        if (_cargar3) _cargar3.addEventListener('click', function() { cargarRecetaAdicional('recetaSelect3', 'receta3IdHidden', this); });
        var _cargar4 = document.getElementById('btnCargarReceta4');
        if (_cargar4) _cargar4.addEventListener('click', function() { cargarRecetaAdicional('recetaSelect4', 'receta4IdHidden', this); });
    }
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initIngredientesListeners);
    } else {
        initIngredientesListeners();
    }
}());
