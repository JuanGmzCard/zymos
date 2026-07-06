document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('.firma-canvas').forEach(function (canvas) {
        var inputId = canvas.dataset.input;
        var input = document.getElementById(inputId);
        var wrap = canvas.closest('.firma-wrap');
        var placeholder = wrap ? wrap.querySelector('.firma-placeholder') : null;

        // Ajustar resolución al tamaño real del canvas (retina)
        var ratio = Math.max(window.devicePixelRatio || 1, 1);
        canvas.width = canvas.offsetWidth * ratio;
        canvas.height = canvas.offsetHeight * ratio;
        canvas.getContext('2d').scale(ratio, ratio);

        var pad = new SignaturePad(canvas, { backgroundColor: 'rgb(255,255,255)' });
        canvas._signaturePad = pad;

        // Restaurar firma existente al editar
        if (input && input.value && input.value.startsWith('data:image/')) {
            pad.fromDataURL(input.value, { ratio: ratio });
            if (placeholder) placeholder.style.display = 'none';
        }

        // Ocultar placeholder al empezar a firmar
        canvas.addEventListener('pointerdown', function () {
            if (placeholder) placeholder.style.display = 'none';
        });

        // Botón limpiar
        if (wrap) {
            var clearBtn = wrap.querySelector('.firma-clear');
            if (clearBtn) {
                clearBtn.addEventListener('click', function () {
                    pad.clear();
                    if (input) input.value = '';
                    if (placeholder) placeholder.style.display = '';
                });
            }
        }
    });

    // Antes de enviar el form, volcar datos de canvas a hidden inputs
    document.querySelectorAll('form').forEach(function (form) {
        form.addEventListener('submit', function () {
            document.querySelectorAll('.firma-canvas').forEach(function (canvas) {
                var inputId = canvas.dataset.input;
                var input = document.getElementById(inputId);
                var pad = canvas._signaturePad;
                if (input && pad) {
                    input.value = pad.isEmpty() ? '' : pad.toDataURL('image/png');
                }
            });
        });
    });
});
