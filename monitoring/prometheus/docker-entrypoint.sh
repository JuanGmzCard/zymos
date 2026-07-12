#!/bin/sh
set -e
# Sustituye $ZYMOS_SCRAPE_USER y $ZYMOS_SCRAPE_PASSWORD en el template antes de arrancar
sed \
  -e "s|\$ZYMOS_SCRAPE_USER|${ZYMOS_SCRAPE_USER}|g" \
  -e "s|\$ZYMOS_SCRAPE_PASSWORD|${ZYMOS_SCRAPE_PASSWORD}|g" \
  /etc/prometheus/prometheus.yml.tmpl > /tmp/prometheus.yml
exec /bin/prometheus \
  --config.file=/tmp/prometheus.yml \
  --storage.tsdb.path=/prometheus \
  --web.console.libraries=/etc/prometheus/console_libraries \
  --web.console.templates=/etc/prometheus/consoles \
  --storage.tsdb.retention.time=15d \
  --web.enable-lifecycle
