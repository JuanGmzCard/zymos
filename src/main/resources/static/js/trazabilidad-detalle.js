// Depende de: CHART_FECHAS, CHART_DENSIDAD, CHART_TEMP, CHART_TEMP_OBJETIVO, CHART_FG_REAL
//             (inyectados por Thymeleaf) — Chart.js cargado antes que este script
(function () {
    const canvas = document.getElementById('chartLecturas');
    if (!canvas) return;
    if (CHART_FECHAS.length === 0) return;

    const dorado     = getComputedStyle(document.documentElement).getPropertyValue('--dorado').trim()      || '#C9A028';
    const azul       = '#2563eb';
    const naranja    = '#f97316';
    const verde      = '#16a34a';

    const labels = CHART_FECHAS.map(f => {
        const parts = f.split('-');
        return parts[2] + '/' + parts[1];
    });

    const datasets = [
        {
            label: 'Densidad',
            data: CHART_DENSIDAD,
            borderColor: dorado,
            backgroundColor: dorado + '22',
            pointBackgroundColor: dorado,
            borderWidth: 2,
            tension: 0.3,
            spanGaps: true,
            yAxisID: 'yDensidad'
        },
        {
            label: 'Temperatura (°C)',
            data: CHART_TEMP,
            borderColor: azul,
            backgroundColor: azul + '22',
            pointBackgroundColor: azul,
            borderWidth: 2,
            tension: 0.3,
            spanGaps: true,
            yAxisID: 'yTemp'
        }
    ];

    // Línea de temperatura objetivo (horizontal punteada naranja)
    if (CHART_TEMP_OBJETIVO != null && CHART_TEMP.some(v => v != null)) {
        datasets.push({
            label: 'T° objetivo (' + CHART_TEMP_OBJETIVO + '°C)',
            data: labels.map(() => CHART_TEMP_OBJETIVO),
            borderColor: naranja,
            borderWidth: 1.5,
            borderDash: [6, 4],
            pointRadius: 0,
            pointHoverRadius: 0,
            fill: false,
            tension: 0,
            yAxisID: 'yTemp'
        });
    }

    // Línea de FG real (horizontal punteada verde sobre eje densidad)
    if (CHART_FG_REAL != null && CHART_DENSIDAD.some(v => v != null)) {
        datasets.push({
            label: 'FG real (' + CHART_FG_REAL + ')',
            data: labels.map(() => CHART_FG_REAL),
            borderColor: verde,
            borderWidth: 1.5,
            borderDash: [6, 4],
            pointRadius: 0,
            pointHoverRadius: 0,
            fill: false,
            tension: 0,
            yAxisID: 'yDensidad'
        });
    }

    new Chart(canvas, {
        type: 'line',
        data: { labels, datasets },
        options: {
            responsive: true,
            maintainAspectRatio: true,
            interaction: { mode: 'index', intersect: false },
            plugins: {
                legend: { position: 'top' },
                tooltip: {
                    callbacks: {
                        label: ctx => ctx.dataset.label + ': ' + (ctx.raw != null ? ctx.raw : 'N/D')
                    }
                }
            },
            scales: {
                yDensidad: {
                    type: 'linear',
                    position: 'left',
                    title: { display: true, text: 'Densidad' },
                    ticks: { stepSize: 5 }
                },
                yTemp: {
                    type: 'linear',
                    position: 'right',
                    title: { display: true, text: '°C' },
                    grid: { drawOnChartArea: false }
                }
            }
        }
    });
})();
