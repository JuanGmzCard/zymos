document.addEventListener('DOMContentLoaded', function() {
    var csrf       = document.querySelector('meta[name="_csrf"]')?.content;
    var csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content;

    document.querySelectorAll('.btn-marcar').forEach(function(btn) {
        btn.addEventListener('click', function() {
            var id = btn.dataset.id;
            fetch('/notificaciones/' + id + '/leer', {
                method: 'POST',
                credentials: 'same-origin',
                headers: csrfHeader ? { [csrfHeader]: csrf } : {}
            })
            .then(function(r) { return r.ok ? r.json() : null; })
            .then(function(data) {
                if (!data) return;
                var row = btn.closest('.notif-row');
                row.classList.add('leida');
                btn.remove();
                row.querySelectorAll('.bg-danger').forEach(function(b) { b.remove(); });
            })
            .catch(function() {});
        });
    });
});
