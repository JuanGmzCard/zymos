(function () {
    const checkboxes = document.querySelectorAll('.chk-lote');
    const btnComparar = document.getElementById('btnComparar');
    const contador = document.getElementById('contadorSeleccion');
    const pluralS = document.getElementById('pluralS');
    const MAX = 6;

    function actualizarContador() {
        const checked = document.querySelectorAll('.chk-lote:checked');
        const n = checked.length;
        contador.textContent = n;
        pluralS.style.display = n !== 1 ? 'inline' : 'none';
        btnComparar.disabled = n < 2;
        checkboxes.forEach(chk => {
            if (!chk.checked) chk.disabled = n >= MAX;
        });
    }

    checkboxes.forEach(chk => chk.addEventListener('change', actualizarContador));

    document.querySelectorAll('.lote-row').forEach(row => {
        row.addEventListener('click', () => {
            const chk = row.querySelector('.chk-lote');
            if (chk.disabled) return;
            chk.checked = !chk.checked;
            actualizarContador();
        });
    });

    actualizarContador();
})();
