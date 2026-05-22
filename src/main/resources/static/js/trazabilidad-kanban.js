// Depende de: esAdmin (inyectado por Thymeleaf), Sortable (CDN)

var CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]').content;
var CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]').content;

var FASE_LABELS = {
    sinIniciar:        'Sin Iniciar',
    fermentacion:      'Fermentación',
    acondicionamiento: 'Acondicionamiento',
    maduracion:        'Maduración',
    carbonatacion:     'Carbonatación',
    completados:       'Completados'
};

document.addEventListener('DOMContentLoaded', function () {
    var cols = document.querySelectorAll('.kanban-col-body');

    cols.forEach(function (col) {
        Sortable.create(col, {
            group:      'kanban',
            animation:  150,
            ghostClass: 'kanban-ghost',
            dragClass:  'kanban-drag',
            disabled:   !esAdmin,
            onStart: function () {
                document.querySelectorAll('.kanban-col-body').forEach(function (c) {
                    c.classList.add('drag-over-target');
                });
            },
            onEnd: function (evt) {
                document.querySelectorAll('.kanban-col-body').forEach(function (c) {
                    c.classList.remove('drag-over-target');
                });
                var targetFase = evt.to.dataset.fase;
                var sourceFase = evt.from.dataset.fase;
                if (targetFase === sourceFase) return;

                var loteId = evt.item.dataset.loteId;
                enviarCambioFase(loteId, targetFase, evt.item, evt.from, evt.oldIndex);
            }
        });
    });

    if (esAdmin) {
        document.querySelectorAll('.kanban-card').forEach(function (card) {
            card.classList.add('draggable');
        });
    }
});

function enviarCambioFase(loteId, fase, card, fromCol, oldIndex) {
    card.classList.add('saving');

    var headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
    headers[CSRF_HEADER] = CSRF_TOKEN;

    fetch('/actualizar/' + loteId + '/fase', {
        method:  'POST',
        headers: headers,
        body:    'fase=' + encodeURIComponent(fase)
    })
    .then(function (resp) { return resp.json(); })
    .then(function (data) {
        card.classList.remove('saving');
        if (data.success) {
            actualizarContadores();
            mostrarToast('Movido a ' + FASE_LABELS[fase], 'success');
        } else {
            revertirMovimiento(card, fromCol, oldIndex);
            mostrarToast(data.error || 'Error al mover lote', 'danger');
        }
    })
    .catch(function () {
        card.classList.remove('saving');
        revertirMovimiento(card, fromCol, oldIndex);
        mostrarToast('Error de conexión', 'danger');
    });
}

function revertirMovimiento(card, fromCol, oldIndex) {
    var ref = fromCol.children[oldIndex] || null;
    fromCol.insertBefore(card, ref);
}

function actualizarContadores() {
    document.querySelectorAll('.kanban-col-body').forEach(function (col) {
        var fase = col.dataset.fase;
        var count = col.querySelectorAll('.kanban-card').length;
        var badge = document.getElementById('count-' + fase);
        if (badge) badge.textContent = count;
        var empty = col.querySelector('.kanban-empty');
        if (empty) empty.style.display = count === 0 ? '' : 'none';
    });
}

function mostrarToast(msg, tipo) {
    var container = document.getElementById('kanban-toast-container');
    var t = document.createElement('div');
    t.className = 'kanban-toast ' + tipo;
    t.textContent = msg;
    container.appendChild(t);
    requestAnimationFrame(function () {
        requestAnimationFrame(function () { t.classList.add('show'); });
    });
    setTimeout(function () {
        t.classList.remove('show');
        setTimeout(function () { t.remove(); }, 300);
    }, 2800);
}
