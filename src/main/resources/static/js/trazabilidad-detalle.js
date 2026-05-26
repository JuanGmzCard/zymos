// Depende de: CHART_FECHAS, CHART_DENSIDAD, CHART_TEMP (inyectados por Thymeleaf)
//             Chart.js cargado antes que este script
(function () {
    const canvas = document.getElementById('chartLecturas');
    if (!canvas) return;
    if (CHART_FECHAS.length === 0) return;

    const dorado     = getComputedStyle(document.documentElement).getPropertyValue('--dorado').trim()      || '#C9A028';
    const verdeZymos = getComputedStyle(document.documentElement).getPropertyValue('--verde-zymos').trim() || '#364318';

    const labels = CHART_FECHAS.map(f => {
        const parts = f.split('-');
        return parts[2] + '/' + parts[1];
    });

    new Chart(canvas, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [
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
                    borderColor: verdeZymos,
                    backgroundColor: verdeZymos + '22',
                    pointBackgroundColor: verdeZymos,
                    borderWidth: 2,
                    tension: 0.3,
                    spanGaps: true,
                    yAxisID: 'yTemp',
                    borderDash: [5, 3]
                }
            ]
        },
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
