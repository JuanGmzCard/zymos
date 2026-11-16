/* Zymos PWA — Service Worker */
const CACHE_VERSION = 'v1';
const CACHE_NAME = 'zymos-' + CACHE_VERSION;

const PRE_CACHE = [
    '/offline.html',
    '/css/style.css'
];

self.addEventListener('install', function (event) {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(function (cache) { return cache.addAll(PRE_CACHE); })
            .then(function () { return self.skipWaiting(); })
    );
});

self.addEventListener('activate', function (event) {
    event.waitUntil(
        caches.keys()
            .then(function (keys) {
                return Promise.all(
                    keys.filter(function (k) { return k.startsWith('zymos-') && k !== CACHE_NAME; })
                        .map(function (k) { return caches.delete(k); })
                );
            })
            .then(function () { return self.clients.claim(); })
    );
});

self.addEventListener('fetch', function (event) {
    var req = event.request;
    var url = new URL(req.url);

    if (req.method !== 'GET') return;
    if (url.origin !== self.location.origin) return;
    if (url.pathname.startsWith('/api/') || url.pathname.startsWith('/actuator/')) return;
    if (url.pathname.startsWith('/login') || url.pathname === '/logout') return;

    if (/\.(css|js|woff2?|ttf|otf|png|jpg|jpeg|gif|svg|ico|webp)$/.test(url.pathname)) {
        event.respondWith(cacheFirst(req));
        return;
    }

    if (req.mode === 'navigate') {
        event.respondWith(networkFirstWithOffline(req));
        return;
    }
});

function cacheFirst(request) {
    return caches.match(request).then(function (cached) {
        if (cached) return cached;
        return fetch(request).then(function (response) {
            if (response.ok) {
                var clone = response.clone();
                caches.open(CACHE_NAME).then(function (c) { c.put(request, clone); });
            }
            return response;
        }).catch(function () {
            return new Response('', { status: 503 });
        });
    });
}

/* ── Push notifications ──────────────────────────────────────────── */

self.addEventListener('push', function (event) {
    var data = {};
    try { data = event.data ? event.data.json() : {}; } catch (e) {}
    var title   = data.title  || 'Zymos';
    var options = {
        body:  data.body  || '',
        icon:  data.icon  || '/img/icon.svg',
        badge: data.badge || '/img/icon.svg',
        data:  { url: data.url || '/notificaciones' }
    };
    event.waitUntil(self.registration.showNotification(title, options));
});

self.addEventListener('notificationclick', function (event) {
    event.notification.close();
    var url = (event.notification.data && event.notification.data.url) || '/notificaciones';
    event.waitUntil(
        clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function (clientList) {
            for (var i = 0; i < clientList.length; i++) {
                if ('focus' in clientList[i]) return clientList[i].focus();
            }
            if (clients.openWindow) return clients.openWindow(url);
        })
    );
});

/* ── Network helpers ─────────────────────────────────────────────── */

function networkFirstWithOffline(request) {
    return fetch(request).then(function (response) {
        if (response.ok) {
            var clone = response.clone();
            caches.open(CACHE_NAME).then(function (c) { c.put(request, clone); });
        }
        return response;
    }).catch(function () {
        return caches.match(request).then(function (cached) {
            return cached || caches.match('/offline.html');
        });
    });
}
