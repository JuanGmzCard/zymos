(function () {
    'use strict';

    var EXCLUIR = /densidad|objetivo|brix|phagua|porcentaje|descuento|pct|\.iva|iva\b|co2|presion|psi|temperatura|temp\b|minutos|duracion|horas\b|tiempo\b|orden\b|aroma\b|apariencia|sabor|sensacion|impresion/i;

    function debeFormatear(inp) {
        if (inp.type !== 'number' && inp.getAttribute('data-num-fmt') !== 'es') {
            if (inp.type !== 'number') return false;
        }
        if (inp.getAttribute('data-num-fmt') === 'skip') return false;
        var nombre = (inp.name || inp.id || '').toLowerCase().replace(/[\[\]]/g, '.');
        if (EXCLUIR.test(nombre)) return false;
        var maxAttr = inp.getAttribute('max');
        if (maxAttr !== null && parseFloat(maxAttr) <= 100) return false;
        return true;
    }

    function fromDisplay(s) {
        if (!s) return '';
        return String(s).replace(/\./g, '').replace(',', '.');
    }

    window.numVal = function (v) {
        return parseFloat(fromDisplay(String(v || ''))) || 0;
    };

    function toDisplay(raw) {
        var s = String(raw || '').trim();
        if (!s) return '';
        var intStr, decStr = null;
        if (s.includes(',')) {
            var ci = s.indexOf(',');
            intStr = s.substring(0, ci).replace(/[^\d]/g, '');
            decStr = s.substring(ci + 1).replace(/[^\d]/g, '');
        } else if (/^\d+\.\d+$/.test(s)) {
            var di = s.lastIndexOf('.');
            intStr = s.substring(0, di);
            decStr = s.substring(di + 1);
        } else {
            intStr = s.replace(/[^\d]/g, '');
        }
        intStr = (intStr || '').replace(/\B(?=(\d{3})+(?!\d))/g, '.');
        return decStr !== null ? intStr + ',' + decStr : intStr;
    }

    function formatearInput(inp) {
        var raw = inp.value;
        var pos = inp.selectionStart;
        var antes = raw.substring(0, pos);
        var intPart, decPart = null;
        if (raw.includes(',')) {
            var ci = raw.indexOf(',');
            intPart = raw.substring(0, ci).replace(/[^\d]/g, '');
            decPart = raw.substring(ci + 1).replace(/[^\d]/g, '');
        } else {
            intPart = raw.replace(/[^\d]/g, '');
        }
        var formatted = intPart ? intPart.replace(/\B(?=(\d{3})+(?!\d))/g, '.') : '';
        if (decPart !== null) formatted += ',' + decPart;
        inp.value = formatted;
        var dotsAntes = (antes.match(/\./g) || []).length;
        var dotsNuevo = (inp.value.substring(0, pos).match(/\./g) || []).length;
        var nuevoPos = Math.max(0, pos + (dotsNuevo - dotsAntes));
        try { inp.setSelectionRange(nuevoPos, nuevoPos); } catch (e) {}
    }

    function activarInput(inp) {
        if (inp.dataset.numFmt) return;
        inp.dataset.numFmt = 'es';
        var origVal = inp.value;
        inp.type = 'text';
        inp.setAttribute('inputmode', 'decimal');
        inp.setAttribute('autocomplete', 'off');
        if (origVal) inp.value = toDisplay(origVal);
        inp.addEventListener('input', function () { formatearInput(this); });
        inp.addEventListener('focus', function () { this.select(); });
        inp.addEventListener('paste', function (e) {
            e.preventDefault();
            var pasted = ((e.clipboardData || window.clipboardData || {}).getData('text') || '').trim();
            inp.value = toDisplay(pasted);
            inp.dispatchEvent(new Event('input', { bubbles: true }));
        });
    }

    var observer = new MutationObserver(function (mutations) {
        mutations.forEach(function (m) {
            m.addedNodes.forEach(function (node) {
                if (node.nodeType !== 1) return;
                var inputs = [];
                if (node.tagName === 'INPUT') inputs.push(node);
                else if (node.querySelectorAll) inputs = Array.from(node.querySelectorAll('input'));
                inputs.forEach(function (inp) { if (debeFormatear(inp)) activarInput(inp); });
            });
        });
    });

    document.addEventListener('submit', function (e) {
        var form = e.target;
        if (!form || !form.querySelectorAll) return;
        form.querySelectorAll('input[data-num-fmt="es"]').forEach(function (inp) {
            inp.value = fromDisplay(inp.value);
        });
    }, true);

    document.addEventListener('DOMContentLoaded', function () {
        document.querySelectorAll('input').forEach(function (inp) {
            if (debeFormatear(inp)) activarInput(inp);
        });
        observer.observe(document.body, { childList: true, subtree: true });
    });
})();
