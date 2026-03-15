import { useEffect, useMemo, useRef } from 'react';
import type { AlertItem } from '../models/alert';
import {
  formatSeverity,
  normalizeResourceKey,
  resourceColors,
  resourceLabels,
  severityMapStyles
} from '../lib/alertMeta';

interface AlertMapProps {
  alerts: AlertItem[];
}

const defaultCenter: [number, number] = [62.0, 15.0];

function escapeHtml(value: string) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;');
}

function formatTimestamp(value?: string) {
  if (!value) {
    return 'No timestamp';
  }

  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(new Date(value));
}

function sortAlertsByLatest(alerts: AlertItem[]) {
  return [...alerts].sort((left, right) => {
    const leftTime = left.publishedAt ? Date.parse(left.publishedAt) : 0;
    const rightTime = right.publishedAt ? Date.parse(right.publishedAt) : 0;
    return rightTime - leftTime;
  });
}

function locationKey(alert: AlertItem) {
  if (alert.geoJson) {
    return `geo:${JSON.stringify(alert.geoJson)}`;
  }

  if (typeof alert.latitude === 'number' && typeof alert.longitude === 'number') {
    return `point:${alert.latitude.toFixed(5)},${alert.longitude.toFixed(5)}`;
  }

  return `fallback:${alert.id}`;
}

function popupContent(alerts: AlertItem[]) {
  const sortedAlerts = sortAlertsByLatest(alerts);

  const items = sortedAlerts
    .map((alert) => {
      const sourceKey = normalizeResourceKey(alert.source);
      const resourceColor = resourceColors[sourceKey] || resourceColors.polisen;
      const severityStyle = severityMapStyles[alert.severity] || severityMapStyles.info;
      const counties = alert.areas?.length
        ? `
          <div class="map-popup__section">
            <span class="map-popup__label">Counties</span>
            <div class="map-popup__chips">
              ${alert.areas
                .map(
                  (area) =>
                    `<span class="map-popup__chip map-popup__chip--county">${escapeHtml(area)}</span>`
                )
                .join('')}
            </div>
          </div>
        `
        : '';
      const description = alert.description
        ? `<p class="map-popup__description">${escapeHtml(alert.description)}</p>`
        : '';
      const link = alert.url
        ? `<a class="map-popup__link" href="${escapeHtml(alert.url)}" target="_blank" rel="noreferrer">Open source</a>`
        : '';

      return `
        <article class="map-popup__alert">
          <strong class="map-popup__title">${escapeHtml(alert.headline)}</strong>
          <div class="map-popup__chips">
            <span class="map-popup__chip map-popup__chip--resource" style="background:${resourceColor};color:#ffffff;">
              ${escapeHtml(resourceLabels[sourceKey] || alert.source)}
            </span>
            <span class="map-popup__chip map-popup__chip--severity" style="background:${severityStyle.background};color:${severityStyle.color};">
              ${escapeHtml(formatSeverity(alert.severity))}
            </span>
          </div>
          <div class="map-popup__meta">${escapeHtml(formatTimestamp(alert.publishedAt))}</div>
          ${counties}
          ${description}
          ${link}
        </article>
      `;
    })
    .join('');

  return `
    <div class="map-popup map-popup--grouped">
      <div class="map-popup__header">
        <strong>${sortedAlerts.length} alert${sortedAlerts.length === 1 ? '' : 's'} at this location</strong>
        <span class="map-popup__meta">Newest first</span>
      </div>
      <div class="map-popup__list">
        ${items}
      </div>
    </div>
  `;
}

function AlertMap({ alerts }: AlertMapProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<any>(null);
  const layerRef = useRef<any>(null);

  const groupedAlerts = useMemo(() => {
    const groups = new Map<string, AlertItem[]>();

    alerts.forEach((alert) => {
      if (!alert.geoJson && (typeof alert.latitude !== 'number' || typeof alert.longitude !== 'number')) {
        return;
      }

      const key = locationKey(alert);
      const existing = groups.get(key) || [];
      existing.push(alert);
      groups.set(key, existing);
    });

    return Array.from(groups.values()).map(sortAlertsByLatest);
  }, [alerts]);

  useEffect(() => {
    if (!containerRef.current || mapRef.current || !window.L) {
      return;
    }

    mapRef.current = window.L.map(containerRef.current, {
      scrollWheelZoom: true
    }).setView(defaultCenter, 5);

    window.L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 18
    }).addTo(mapRef.current);

    layerRef.current = window.L.layerGroup().addTo(mapRef.current);

    return () => {
      mapRef.current?.remove();
      mapRef.current = null;
      layerRef.current = null;
    };
  }, []);

  useEffect(() => {
    if (!mapRef.current || !layerRef.current || !window.L) {
      return;
    }

    layerRef.current.clearLayers();
    const bounds = window.L.latLngBounds([]);

    groupedAlerts.forEach((group) => {
      const leadAlert = group[0];
      const sourceKey = normalizeResourceKey(leadAlert.source);
      const color = resourceColors[sourceKey] || resourceColors.polisen;
      const severityStyle = severityMapStyles[leadAlert.severity] || severityMapStyles.info;

      if (leadAlert.geoJson) {
        const geoLayer = window.L.geoJSON(leadAlert.geoJson, {
          style: {
            color,
            fillColor: color,
            opacity: severityStyle.strokeOpacity,
            fillOpacity: severityStyle.fillOpacity,
            weight: severityStyle.weight
          }
        });
        geoLayer.bindPopup(popupContent(group), {
          maxWidth: 440
        });
        geoLayer.addTo(layerRef.current);
        if (typeof geoLayer.getBounds === 'function' && geoLayer.getBounds().isValid()) {
          bounds.extend(geoLayer.getBounds());
        }
        return;
      }

      if (typeof leadAlert.latitude === 'number' && typeof leadAlert.longitude === 'number') {
        const marker = window.L.circleMarker([leadAlert.latitude, leadAlert.longitude], {
          color,
          fillColor: color,
          fillOpacity: severityStyle.fillOpacity + 0.5,
          radius: leadAlert.severity === 'high' ? 9 : leadAlert.severity === 'medium' ? 8 : 7,
          weight: severityStyle.weight
        });
        marker.bindPopup(popupContent(group), {
          maxWidth: 440
        });
        marker.addTo(layerRef.current);
        bounds.extend(marker.getLatLng());
      }
    });

    if (bounds.isValid()) {
      mapRef.current.fitBounds(bounds, { padding: [24, 24], maxZoom: 8 });
    } else {
      mapRef.current.setView(defaultCenter, 5);
    }
  }, [groupedAlerts]);

  return <div ref={containerRef} className="alert-map" />;
}

export default AlertMap;
