import { useEffect, useRef } from 'react';
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

function popupContent(alert: AlertItem) {
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
                `<span class="map-popup__chip map-popup__chip--county">${area}</span>`
            )
            .join('')}
        </div>
      </div>
    `
    : '';

  return `
    <div class="map-popup">
      <strong class="map-popup__title">${alert.headline}</strong>
      <div class="map-popup__chips">
        <span class="map-popup__chip map-popup__chip--resource" style="background:${resourceColor};color:#ffffff;">
          ${resourceLabels[sourceKey] || alert.source}
        </span>
        <span class="map-popup__chip map-popup__chip--severity" style="background:${severityStyle.background};color:${severityStyle.color};">
          ${formatSeverity(alert.severity)}
        </span>
      </div>
      ${counties}
    </div>
  `;
}

function AlertMap({ alerts }: AlertMapProps) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const mapRef = useRef<any>(null);
  const layerRef = useRef<any>(null);

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

    alerts.forEach((alert) => {
      const sourceKey = normalizeResourceKey(alert.source);
      const color = resourceColors[sourceKey] || resourceColors.polisen;
      const severityStyle = severityMapStyles[alert.severity] || severityMapStyles.info;

      if (alert.geoJson) {
        const geoLayer = window.L.geoJSON(alert.geoJson, {
          style: {
            color,
            fillColor: color,
            opacity: severityStyle.strokeOpacity,
            fillOpacity: severityStyle.fillOpacity,
            weight: severityStyle.weight
          }
        });
        geoLayer.bindPopup(popupContent(alert));
        geoLayer.addTo(layerRef.current);
        if (typeof geoLayer.getBounds === 'function' && geoLayer.getBounds().isValid()) {
          bounds.extend(geoLayer.getBounds());
        }
        return;
      }

      if (typeof alert.latitude === 'number' && typeof alert.longitude === 'number') {
        const marker = window.L.circleMarker([alert.latitude, alert.longitude], {
          color,
          fillColor: color,
          fillOpacity: severityStyle.fillOpacity + 0.5,
          radius: alert.severity === 'high' ? 9 : alert.severity === 'medium' ? 8 : 7,
          weight: severityStyle.weight
        });
        marker.bindPopup(popupContent(alert));
        marker.addTo(layerRef.current);
        bounds.extend(marker.getLatLng());
      }
    });

    if (bounds.isValid()) {
      mapRef.current.fitBounds(bounds, { padding: [24, 24], maxZoom: 8 });
    } else {
      mapRef.current.setView(defaultCenter, 5);
    }
  }, [alerts]);

  return <div ref={containerRef} className="alert-map" />;
}

export default AlertMap;
