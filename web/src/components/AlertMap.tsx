import { useEffect, useRef } from 'react';
import type { AlertItem } from '../lib/api';

interface AlertMapProps {
  alerts: AlertItem[];
}

const defaultCenter: [number, number] = [62.0, 15.0];

const severityPalette: Record<string, string> = {
  high: '#b53a3f',
  info: '#4a6a84',
  low: '#2f7a5f',
  medium: '#b7860b'
};

function popupContent(alert: AlertItem) {
  const areas = alert.areas?.length ? `<div>${alert.areas.join(', ')}</div>` : '';
  return `
    <div class="map-popup">
      <strong>${alert.headline}</strong>
      <div>${alert.source}</div>
      <div>${alert.severity}</div>
      ${areas}
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
      const color = severityPalette[alert.severity] || severityPalette.info;

      if (alert.geoJson) {
        const geoLayer = window.L.geoJSON(alert.geoJson, {
          style: {
            color,
            fillColor: color,
            fillOpacity: 0.22,
            weight: 2
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
          fillOpacity: 0.85,
          radius: 7,
          weight: 2
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
