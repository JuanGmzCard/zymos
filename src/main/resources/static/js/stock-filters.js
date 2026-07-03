document.addEventListener('DOMContentLoaded', function() {

    var filas = document.querySelectorAll('.stock-row');
    var contador = document.getElementById('contadorVisible');

    function aplicarFiltros() {
        var filtroActivo = (document.querySelector('.filter-tab.active') || {}).dataset.filtro || 'todos';
        var busq = (document.getElementById('buscarLote').value || '').toLowerCase();
        var visibles = 0;
        filas.forEach(function(fila) {
            var filtroFila = fila.dataset.filtro;
            var buscarFila = (fila.dataset.buscar || '').toLowerCase();
            var pasaTab  = filtroActivo === 'todos' ||
                           (filtroActivo === 'con-stock' && filtroFila === 'con-stock') ||
                           (filtroActivo === 'agotados'  && filtroFila === 'agotado');
            var pasaBusq = busq === '' || buscarFila.includes(busq);
            var mostrar  = pasaTab && pasaBusq;
            fila.style.display = mostrar ? '' : 'none';
            if (mostrar) visibles++;
        });
        if (contador) contador.innerHTML = '<strong>' + visibles + '</strong> lotes';
    }

    document.querySelectorAll('.filter-tab').forEach(function(tab) {
        tab.addEventListener('click', function(e) {
            e.preventDefault();
            document.querySelectorAll('.filter-tab').forEach(function(t) {
                t.classList.remove('active');
                t.classList.add('inactive');
            });
            this.classList.remove('inactive');
            this.classList.add('active');
            aplicarFiltros();
        });
    });

    var buscarInput = document.getElementById('buscarLote');
    if (buscarInput) {
        buscarInput.addEventListener('input', aplicarFiltros);
    }

    var modalAjuste = document.getElementById('modalAjuste');
    var formAjuste  = document.getElementById('formAjuste');

    document.querySelectorAll('.btn-ajuste').forEach(function(btn) {
        btn.addEventListener('click', function() {
            var loteId    = this.dataset.loteId;
            var loteCodigo= this.dataset.loteCodigo;
            var unidad    = this.dataset.loteUnidad || 'L';

            document.getElementById('ajusteCodigoLote').textContent = loteCodigo;
            document.getElementById('ajusteUnidad').value = unidad;

            formAjuste.action = formAjuste.action.replace(/\/ajustar\/\d+$/, '/ajustar/' + loteId);

            new bootstrap.Modal(modalAjuste).show();
        });
    });

});
