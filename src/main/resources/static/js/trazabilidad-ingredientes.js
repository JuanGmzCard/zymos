// ── Wizard tabs ──────────────────────────────────────────────────
const TOTAL_TABS = 6;
let currentTab = 0;

function goTab(idx) {
    document.getElementById('panel-' + currentTab).classList.add('d-none');
    document.querySelectorAll('.wz-tab')[currentTab].classList.remove('active');
    document.getElementById('dot-' + currentTab).classList.remove('active');
    document.getElementById('dot-' + currentTab).classList.add('done');

    currentTab = idx;
    document.getElementById('panel-' + currentTab).classList.remove('d-none');
    document.querySelectorAll('.wz-tab')[currentTab].classList.add('active');

    for (let i = 0; i < TOTAL_TABS; i++) {
        const dot = document.getElementById('dot-' + i);
        dot.classList.remove('active', 'done');
        if (i < currentTab) dot.classList.add('done');
        else if (i === currentTab) dot.classList.add('active');
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
    const displayVal = parseFloat(displayInput.value);
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
                        ${UNIT_OPTIONS}
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

// ── Cargar receta en lote ─────────────────────────────────────────
function cargarRecetaEnLote() {
    const id = document.getElementById('recetaSelectLote').value;
    if (!id) return;
    document.getElementById('recetaIdHidden').value = id;
    fetch('/recetas/api/' + id)
        .then(r => r.json())
        .then(data => {
            poblarDesdeReceta('maltas-container',        'maltas',        'lista-maltas',        'Malta',        data.maltas        || []);
            poblarDesdeReceta('lupulos-container',       'lupulos',       'lista-lupulos',       'Lúpulo',       data.lupulos       || []);
            poblarDesdeReceta('levaduras-container',     'levaduras',     'lista-levaduras',     'Levadura',     data.levaduras     || []);
            poblarDesdeReceta('clarificantes-container', 'clarificantes', 'lista-clarificantes', 'Clarificante', data.clarificantes || []);
        })
        .catch(() => alert('Error al cargar la receta'));
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
                            ${unitOptionsSelected(unidad)}
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

function unitOptionsSelected(selected) {
    return ['gr', 'kg', 'mL', 'L', 'gal'].map(u =>
        `<option value="${u}"${u === selected ? ' selected' : ''}>${u}</option>`
    ).join('');
}
