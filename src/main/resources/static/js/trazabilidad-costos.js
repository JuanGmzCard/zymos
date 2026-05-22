// Depende de: ITEMS_FACTURA, INIT_IDS, INIT_CANTIDADES (inyectados por Thymeleaf)
//             poblarDesdeReceta, goTab (de trazabilidad-ingredientes.js)

var asignados = [];

function initAsignados() {
    if (INIT_IDS && INIT_IDS.length) {
        INIT_IDS.forEach(function(id, i) {
            var item = ITEMS_FACTURA.find(function(it) { return it.id == id; });
            if (!item) return;
            var cant = (INIT_CANTIDADES && INIT_CANTIDADES[i] != null) ? parseFloat(INIT_CANTIDADES[i]) : 0;
            asignados.push({ itemId: id, cantidadAsignada: cant, itemData: item });
        });
    }
    renderizarAsignados();
}

function filtrarItemsCosto() {
    var q = (document.getElementById('costo-search').value || '').trim().toLowerCase();
    var tipo = document.getElementById('costo-tipo-filter').value;
    var resultadosDiv = document.getElementById('costo-resultados');
    if (!q && !tipo) { resultadosDiv.style.display = 'none'; return; }

    var filtrados = ITEMS_FACTURA.filter(function(it) {
        var mq = !q || (it.nombre && it.nombre.toLowerCase().includes(q))
                    || (it.proveedor && it.proveedor.toLowerCase().includes(q))
                    || (it.facturaNumero && it.facturaNumero.toLowerCase().includes(q));
        return mq && (!tipo || it.tipoInsumo === tipo);
    });

    var tbody = document.getElementById('costo-resultados-body');
    tbody.innerHTML = '';
    if (!filtrados.length) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center text-muted py-2 small">Sin resultados</td></tr>';
    } else {
        filtrados.slice(0, 50).forEach(function(it) {
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
                    (ya ? 'btn-secondary disabled' : 'btn-outline-primary') + '" ' +
                    (ya ? '' : 'onclick="agregarItemCosto(' + it.id + ')"') + '>' +
                    (ya ? '✓' : '+') + '</button></td>';
            tbody.appendChild(tr);
        });
    }
    resultadosDiv.style.display = '';
}

function agregarItemCosto(itemId) {
    var it = ITEMS_FACTURA.find(function(x) { return x.id == itemId; });
    if (!it || asignados.some(function(a) { return a.itemId == itemId; })) return;
    asignados.push({ itemId: itemId, cantidadAsignada: parseFloat(it.cantidad) || 0, itemData: it });
    renderizarAsignados();
    filtrarItemsCosto();
}

function removerItemCosto(itemId) {
    asignados = asignados.filter(function(a) { return a.itemId != itemId; });
    renderizarAsignados();
    filtrarItemsCosto();
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
                    '<input type="number" class="form-control form-control-sm" min="0" step="0.001" ' +
                        'data-item-id="' + a.itemId + '" value="' + a.cantidadAsignada + '" ' +
                        'oninput="actualizarCantidadCosto(' + a.itemId + ', this.value)">' +
                    '<span class="input-group-text">' + esc(a.itemData.unidad || '') + '</span>' +
                '</div>' +
                (esCostoTotal ? '<small class="text-muted d-block mt-1">Costo total del ítem</small>' : '') +
            '</td>' +
            '<td class="text-end pe-2 fw-bold costo-valor">$' + fmtMoney(calcularValorAsignado(a)) + '</td>' +
            '<td><button type="button" class="btn-remove-ingrediente" onclick="removerItemCosto(' + a.itemId +
                ')"><i class="bi bi-x"></i></button></td>';
        tbody.appendChild(tr);
    });
}

function sincronizarIngredientesDesdeItems() {
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
        var inp = document.querySelector('[data-item-id="' + a.itemId + '"]');
        var cant = inp ? parseFloat(inp.value) : a.cantidadAsignada;
        if (!cant || cant <= 0) return;
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

document.getElementById('loteForm').addEventListener('submit', function() {
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
});

document.addEventListener('DOMContentLoaded', initAsignados);

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
