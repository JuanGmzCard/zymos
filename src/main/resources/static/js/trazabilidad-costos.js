// Depende de: INIT_ITEMS_DATA, INIT_IDS, INIT_CANTIDADES (inyectados por Thymeleaf)
//             poblarDesdeReceta, goTab (de trazabilidad-ingredientes.js)

var asignados = [];
var ultimosResultados = {};
var _filtroCostoTimer;

function initAsignados() {
    if (INIT_IDS && INIT_IDS.length) {
        INIT_IDS.forEach(function(id, i) {
            var item = INIT_ITEMS_DATA.find(function(it) { return it.id == id; });
            if (!item) return;
            var cant = (INIT_CANTIDADES && INIT_CANTIDADES[i] != null) ? parseFloat(INIT_CANTIDADES[i]) : 0;
            asignados.push({ itemId: id, cantidadAsignada: cant, itemData: item });
        });
    }
    renderizarAsignados();
}

function filtrarItemsCosto() {
    clearTimeout(_filtroCostoTimer);
    _filtroCostoTimer = setTimeout(buscarItemsCosto, 260);
}

function buscarItemsCosto() {
    var q = (document.getElementById('costo-search').value || '').trim();
    var tipo = document.getElementById('costo-tipo-filter').value;
    var resultadosDiv = document.getElementById('costo-resultados');
    if (!q && !tipo) { resultadosDiv.style.display = 'none'; return; }

    fetch('/suggest-items?q=' + encodeURIComponent(q) + '&tipo=' + encodeURIComponent(tipo))
        .then(function(r) { return r.json(); })
        .then(function(items) { renderizarResultadosCosto(items); });
}

function renderizarResultadosCosto(items) {
    var resultadosDiv = document.getElementById('costo-resultados');
    items.forEach(function(it) { ultimosResultados[it.id] = it; });

    var tbody = document.getElementById('costo-resultados-body');
    tbody.innerHTML = '';
    if (!items.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-2 small">Sin resultados</td></tr>';
    } else {
        items.forEach(function(it) {
            var ya = asignados.some(function(a) { return a.itemId == it.id; });
            var tr = document.createElement('tr');
            if (ya) tr.classList.add('table-warning');
            tr.innerHTML =
                '<td class="ps-2">' + esc(it.nombre) + '</td>' +
                '<td class="text-muted small">' + esc(it.tipoInsumo || '—') + '</td>' +
                '<td><a href="/facturas/ver/' + it.facturaId + '" target="_blank">' + esc(it.facturaNumero) + '</a>' +
                    (it.fechaFactura ? ' <span class="text-muted small">' + esc(it.fechaFactura) + '</span>' : '') + '</td>' +
                '<td class="text-muted">' + esc(it.proveedor) + '</td>' +
                '<td class="text-muted">' + fmtNum(it.cantidad) + ' ' + esc(it.unidad || '') + '</td>' +
                '<td class="text-end fw-bold">$' + fmtMoney(it.valorLinea) + '</td>' +
                '<td class="pe-2"><button type="button" class="btn btn-sm py-0 px-2 ' +
                    (ya ? 'btn-secondary disabled' : 'btn-outline-primary btn-agregar-item-costo') + '" ' +
                    'data-costo-item-id="' + it.id + '">' +
                    (ya ? '✓' : '+') + '</button></td>';
            tbody.appendChild(tr);
        });
    }
    resultadosDiv.style.display = '';
}

function agregarItemCosto(itemId) {
    var it = ultimosResultados[itemId];
    if (!it || asignados.some(function(a) { return a.itemId == itemId; })) return;
    asignados.push({ itemId: itemId, cantidadAsignada: parseFloat(it.cantidad) || 0, itemData: it });
    renderizarAsignados();
    buscarItemsCosto();
}

function removerItemCosto(itemId) {
    asignados = asignados.filter(function(a) { return a.itemId != itemId; });
    renderizarAsignados();
    buscarItemsCosto();
}

function actualizarCantidadCosto(itemId, val) {
    var a = asignados.find(function(x) { return x.itemId == itemId; });
    if (a) a.cantidadAsignada = parseFloat(val) || 0;
    var tr = document.querySelector('[data-item-id="' + itemId + '"]');
    if (tr) {
        var td = tr.closest('tr').querySelector('.costo-valor');
        if (td && a) td.textContent = '$' + fmtMoney(calcularValorAsignado(a));
    }
}

function calcularValorAsignado(a) {
    var cant = a.cantidadAsignada;
    var vl = parseFloat(a.itemData.valorLinea) || 0;
    var ct = parseFloat(a.itemData.cantidad) || 0;
    if (cant === 0) return vl;
    if (!ct) return 0;
    return cant * vl / ct;
}

function renderizarAsignados() {
    var tbody = document.getElementById('costo-asignados-body');
    var emptyMsg = document.getElementById('costo-asignados-empty');
    var wrap = document.getElementById('costo-asignados-wrap');
    var badge = document.getElementById('costo-count-badge');
    if (!tbody) return;
    tbody.innerHTML = '';
    if (!asignados.length) {
        if (emptyMsg) emptyMsg.style.display = '';
        if (wrap) wrap.style.display = 'none';
        if (badge) badge.style.display = 'none';
        return;
    }
    if (emptyMsg) emptyMsg.style.display = 'none';
    if (wrap) wrap.style.display = '';
    if (badge) { badge.style.display = ''; badge.textContent = asignados.length; }

    asignados.forEach(function(a) {
        var tr = document.createElement('tr');
        var esCostoTotal = a.cantidadAsignada === 0;
        tr.innerHTML =
            '<td class="ps-2 fw-600">' + esc(a.itemData.nombre) + '</td>' +
            '<td class="text-muted small"><a href="/facturas/ver/' + a.itemData.facturaId +
                '" target="_blank">' + esc(a.itemData.facturaNumero) + '</a> — ' + esc(a.itemData.proveedor) + '</td>' +
            '<td>' +
                '<div class="input-group input-group-sm">' +
                    '<input type="number" class="form-control form-control-sm costo-cantidad-input" min="0" step="0.001" ' +
                        'data-num-fmt="skip" data-item-id="' + a.itemId + '" value="' + a.cantidadAsignada + '">' +
                    '<span class="input-group-text">' + esc(a.itemData.unidad || '') + '</span>' +
                '</div>' +
                (esCostoTotal ? '<small class="text-muted d-block mt-1">Costo total del ítem</small>' : '') +
            '</td>' +
            '<td class="text-end pe-2 fw-bold costo-valor">$' + fmtMoney(calcularValorAsignado(a)) + '</td>' +
            '<td><button type="button" class="btn-remove-ingrediente btn-remover-item-costo" ' +
                'data-costo-item-id="' + a.itemId + '"><i class="bi bi-x"></i></button></td>';
        tbody.appendChild(tr);
    });
}

function sincronizarIngredientesDesdeItems() {
    // Si hay receta pendiente, aplicar sus ingredientes directamente al panel-1
    if (typeof _recetaPendiente !== 'undefined' && _recetaPendiente) {
        poblarDesdeReceta('maltas-container',        'maltas',        'lista-maltas',        'Malta',        _recetaPendiente.maltas        || []);
        poblarDesdeReceta('lupulos-container',       'lupulos',       'lista-lupulos',       'Lúpulo',       _recetaPendiente.lupulos       || []);
        poblarDesdeReceta('levaduras-container',     'levaduras',     'lista-levaduras',     'Levadura',     _recetaPendiente.levaduras     || []);
        poblarDesdeReceta('clarificantes-container', 'clarificantes', 'lista-clarificantes', 'Clarificante', _recetaPendiente.clarificantes || []);
        _recetaPendiente = null;
        // Re-agregar ingredientes de sesiones 2, 3 y 4 que poblarDesdeReceta acaba de borrar
        var _gruposAd = [
            { key: 'maltas',        containerId: 'maltas-container',        tipo: 'maltas',        listId: 'lista-maltas',        ph: 'Malta' },
            { key: 'lupulos',       containerId: 'lupulos-container',       tipo: 'lupulos',       listId: 'lista-lupulos',       ph: 'Lúpulo' },
            { key: 'levaduras',     containerId: 'levaduras-container',     tipo: 'levaduras',     listId: 'lista-levaduras',     ph: 'Levadura' },
            { key: 'clarificantes', containerId: 'clarificantes-container', tipo: 'clarificantes', listId: 'lista-clarificantes', ph: 'Clarificante' }
        ];
        [_recetaData2, _recetaData3, _recetaData4].forEach(function(rdata) {
            if (!rdata) return;
            _gruposAd.forEach(function(cfg) {
                (rdata[cfg.key] || []).forEach(function(item) {
                    if (item.nombre || item.cantidad) sumarOAgregarIngrediente(cfg.containerId, cfg.tipo, cfg.listId, cfg.ph, item);
                });
            });
        });
        _recetaData2 = null;
        _recetaData3 = null;
        _recetaData4 = null;
        goTab(1);
        return;
    }

    // Sin receta pendiente: aplicar desde ítems de costo asignados
    var TIPOS_ING = {
        'MALTA':        ['maltas-container',        'maltas',        'lista-maltas',        'Malta'],
        'LUPULO':       ['lupulos-container',        'lupulos',       'lista-lupulos',       'Lúpulo'],
        'LEVADURA':     ['levaduras-container',      'levaduras',     'lista-levaduras',     'Levadura'],
        'CLARIFICANTE': ['clarificantes-container',  'clarificantes', 'lista-clarificantes', 'Clarificante']
    };
    var grupos = { MALTA: [], LUPULO: [], LEVADURA: [], CLARIFICANTE: [] };
    asignados.forEach(function(a) {
        var tipo = (a.itemData.tipoInsumo || '').toUpperCase();
        if (!grupos[tipo]) return;
        // cantidadAsignada is a float kept in sync by actualizarCantidadCosto.
        // Reading inp.value via numVal was wrong: the input is type="number" so
        // inp.value is always "1.35" (standard), but numVal("1.35") strips the
        // dot as a thousands separator → 135. Use the float directly instead.
        var cant = parseFloat(a.cantidadAsignada) || 0;
        if (cant <= 0) return;
        grupos[tipo].push({ nombre: a.itemData.nombre, cantidad: String(cant), unidad: a.itemData.unidad || 'gr' });
    });
    var alguno = Object.keys(grupos).some(function(k) { return grupos[k].length > 0; });
    if (!alguno) { alert('No hay ítems de ingrediente asignados con cantidad > 0.'); return; }
    Object.keys(TIPOS_ING).forEach(function(tipo) {
        if (grupos[tipo].length > 0) {
            var cfg = TIPOS_ING[tipo];
            poblarDesdeReceta(cfg[0], cfg[1], cfg[2], cfg[3], grupos[tipo]);
        }
    });
    goTab(1);
}

// ── Conversión de unidades para cantidad asignada ─────────────────
function convertirCantidadUnidades(cantidad, unidadOrigen, unidadDestino) {
    var FACTORES = { gr: 1, kg: 1000, ml: 1, l: 1000, gal: 3785.41, und: 1 };
    var BASE     = { gr: 'gr', kg: 'gr', ml: 'ml', l: 'ml', gal: 'ml', und: 'und' };
    var uo = (unidadOrigen  || 'gr').toLowerCase();
    var ud = (unidadDestino || 'gr').toLowerCase();
    if (uo === ud) return cantidad;
    var fo = FACTORES[uo] || 1, fd = FACTORES[ud] || 1;
    if ((BASE[uo] || 'gr') !== (BASE[ud] || 'gr')) return cantidad; // unidades incompatibles (peso vs volumen)
    return (cantidad * fo) / fd;
}

// ── Auto-agregar ítems de factura desde receta con bajo stock ─────
function autoAgregarCostosReceta(costosSugeridos, advertencias, acumular) {
    var agregados = 0;
    (costosSugeridos || []).forEach(function(it) {
        var cantNueva;
        if (it.cantidadReceta != null && it.cantidadReceta > 0) {
            cantNueva = convertirCantidadUnidades(it.cantidadReceta, it.unidadReceta, it.unidad);
        } else {
            cantNueva = parseFloat(it.cantidad) || 0;
        }
        var existente = asignados.find(function(a) { return a.itemId == it.id; });
        if (!existente) {
            var itemData = Object.assign({}, it);
            delete itemData.cantidadReceta;
            delete itemData.unidadReceta;
            asignados.push({ itemId: it.id, cantidadAsignada: cantNueva, itemData: itemData });
            agregados++;
        } else if (acumular) {
            // Elaboración adicional: sumar cantidad al ítem ya asignado
            existente.cantidadAsignada = (existente.cantidadAsignada || 0) + cantNueva;
            agregados++;
        }
    });
    if (agregados) {
        renderizarAsignados();
        var collapse = document.getElementById('costos-collapse');
        if (collapse && !collapse.classList.contains('show')) {
            var bsCol = bootstrap.Collapse.getOrCreateInstance(collapse, { toggle: false });
            bsCol.show();
        }
    }

    var warn = document.getElementById('stock-warnings');
    if (warn) {
        warn.innerHTML = '';
        if (advertencias && advertencias.length) {
            var msgCostos = agregados
                ? ' Se agregaron <strong>' + agregados + ' ítem(s)</strong> en Costos de Producción para su seguimiento.'
                : ' No se encontraron ítems en facturas para estos ingredientes.';
            warn.innerHTML =
                '<div class="alert alert-warning gap-2 p-2 mb-0 mt-1" style="font-size:0.82rem;">' +
                '<div class="d-flex align-items-start gap-2">' +
                '<i class="bi bi-exclamation-triangle-fill flex-shrink-0 mt-1"></i>' +
                '<div class="flex-grow-1"><strong>Stock insuficiente</strong> para: ' +
                advertencias.map(function(n) { return '<em>' + esc(n) + '</em>'; }).join(', ') +
                '.' + msgCostos + '</div>' +
                '<button type="button" class="btn-close btn-sm btn-cerrar-stock-alert"></button>' +
                '</div>' +
                '<div class="mt-2 ps-4">' +
                '<button type="button" class="btn btn-sm btn-warning py-0 px-2 btn-ignorar-stock">' +
                '<i class="bi bi-arrow-right-circle me-1"></i>Ignorar advertencias y continuar</button>' +
                '</div>' +
                '</div>';
        }
    }
    return agregados;
}

document.getElementById('loteForm').addEventListener('submit', function(e) {
    // Validar: fecha inicio fermentación requiere fermentador asignado
    var fermFecha   = document.getElementById('fermFechaInicial');
    var fermentador = document.getElementById('equipoFermentadorId');
    if (fermFecha && fermFecha.value && fermentador && !fermentador.value) {
        e.preventDefault();
        goTab(0);
        fermentador.classList.add('is-invalid');
        var fb = document.getElementById('fermentador-feedback');
        if (!fb) {
            fb = document.createElement('div');
            fb.id = 'fermentador-feedback';
            fb.className = 'invalid-feedback d-block';
            fermentador.parentNode.appendChild(fb);
        }
        fb.textContent = 'Seleccione un fermentador para registrar la fecha de inicio de fermentación.';
        fermentador.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return;
    }
    if (fermentador) fermentador.classList.remove('is-invalid');

    var container = document.getElementById('items-asignados-container');
    if (!container) return;
    container.innerHTML = '';
    asignados.forEach(function(a) {
        var inp = document.querySelector('[data-item-id="' + a.itemId + '"]');
        var cant = inp ? parseFloat(inp.value) : a.cantidadAsignada;
        if (isNaN(cant) || cant < 0) return;
        var hId = document.createElement('input');
        hId.type = 'hidden'; hId.name = 'itemsIds'; hId.value = a.itemId;
        container.appendChild(hId);
        var hCant = document.createElement('input');
        hCant.type = 'hidden'; hCant.name = 'itemsCantidades'; hCant.value = cant;
        container.appendChild(hCant);
    });

    // Prevenir doble submit
    _formSubmitted = true;
    var btn = this.querySelector('button[type="submit"]');
    if (btn) {
        btn.disabled = true;
        btn.innerHTML = '<span class="spinner-border spinner-border-sm me-2" role="status"></span>Guardando...';
    }
});

document.addEventListener('DOMContentLoaded', function() {
    initAsignados();
    // Limpiar error de fermentador cuando el usuario selecciona uno
    var fermentador = document.getElementById('equipoFermentadorId');
    if (fermentador) {
        fermentador.addEventListener('change', function() {
            if (this.value) {
                this.classList.remove('is-invalid');
                var fb = document.getElementById('fermentador-feedback');
                if (fb) fb.textContent = '';
            }
        });
    }

    // Wire "Aplicar a Receta e Insumos" button — one-shot persistente via localStorage
    var btnAplicar = document.getElementById('btnAplicarReceta');
    var _costoLsKey = (typeof INIT_LOTE_ID !== 'undefined' && INIT_LOTE_ID)
        ? 'costos-aplicados-' + INIT_LOTE_ID
        : null;

    function _marcarCostosAplicados() {
        var msgIng = btnAplicar.dataset.msgIng || 'Ingredientes agregados';
        var msgCos = btnAplicar.dataset.msgCos || 'Costos asignados';
        btnAplicar.parentNode.innerHTML =
            '<span class="text-success small me-3"><i class="bi bi-check-circle-fill me-1"></i>' + msgIng + '</span>' +
            '<span class="text-success small"><i class="bi bi-check-circle-fill me-1"></i>' + msgCos + '</span>';
    }

    if (btnAplicar) {
        // Ocultar botón si ya hay ítems asignados guardados en servidor (INIT_IDS)
        // o si localStorage indica que fue aplicado en esta sesión
        var _yaAplicado = (typeof INIT_IDS !== 'undefined' && INIT_IDS && INIT_IDS.length > 0)
                       || (_costoLsKey && localStorage.getItem(_costoLsKey));
        if (_yaAplicado) {
            _marcarCostosAplicados();
        }

        btnAplicar.addEventListener('click', function() {
            sincronizarIngredientesDesdeItems();
            if (_costoLsKey) localStorage.setItem(_costoLsKey, '1');
            _marcarCostosAplicados();
        });
    }

    // Wire costo search inputs
    var costoSearch = document.getElementById('costo-search');
    if (costoSearch) costoSearch.addEventListener('input', filtrarItemsCosto);
    var costoTipo = document.getElementById('costo-tipo-filter');
    if (costoTipo) costoTipo.addEventListener('change', filtrarItemsCosto);
});

// ── CSP-safe delegated event listeners ────────────────────────────

// Agregar ítem de costo desde resultados de búsqueda
document.addEventListener('click', function(e) {
    var btn = e.target.closest('.btn-agregar-item-costo');
    if (btn) agregarItemCosto(btn.dataset.costoItemId);
});

// Remover ítem de costo asignado
document.addEventListener('click', function(e) {
    var btn = e.target.closest('.btn-remover-item-costo');
    if (btn) removerItemCosto(btn.dataset.costoItemId);
});

// Actualizar cantidad de ítem de costo asignado
document.addEventListener('input', function(e) {
    var inp = e.target.closest('.costo-cantidad-input');
    if (inp && inp.dataset.itemId) actualizarCantidadCosto(inp.dataset.itemId, inp.value);
});

// Cerrar alerta de stock (btn-close)
document.addEventListener('click', function(e) {
    var btn = e.target.closest('.btn-cerrar-stock-alert');
    if (btn) btn.closest('.alert').remove();
});

// Ignorar advertencias de stock
document.addEventListener('click', function(e) {
    var btn = e.target.closest('.btn-ignorar-stock');
    if (btn) {
        if (typeof _actualizarEstadoAplicar === 'function') _actualizarEstadoAplicar(false);
        btn.closest('.alert').remove();
    }
});

// ── Prevenir pérdida de datos ──────────────────────────────────────
var _formSubmitted = false;
var _formDirty = false;
document.getElementById('loteForm').addEventListener('change', function() { _formDirty = true; }, true);
document.getElementById('loteForm').addEventListener('input',  function() { _formDirty = true; }, true);
window.addEventListener('beforeunload', function(e) {
    if (_formDirty && !_formSubmitted) {
        e.preventDefault();
        e.returnValue = '';
    }
});

// ── Utilidades ────────────────────────────────────────────────────
function esc(s) {
    if (s == null) return '';
    return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
function fmtNum(n) {
    return n != null ? parseFloat(n).toLocaleString('es-CO', { minimumFractionDigits: 0, maximumFractionDigits: 3 }) : '—';
}
function fmtMoney(n) {
    return n != null ? Math.round(parseFloat(n)).toLocaleString('es-CO') : '0';
}
