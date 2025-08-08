import { Alert, Severity } from '../models/alert';
import { fetchPolisenEvents } from '../adapters/polisen';
import { fetchSmhiWarnings } from '../adapters/smhi';
import { fetchKrisinformationItems } from '../adapters/krisinformation';

function levelToSeverity(level: string): Severity {
  switch (level) {
    case 'red':
      return 'high';
    case 'orange':
      return 'medium';
    case 'yellow':
      return 'low';
    default:
      return 'info';
  }
}

/**
 * Fetch alerts from all sources and return a combined, sorted list.
 */
export async function fetchAllAlerts(): Promise<Alert[]> {
  const [polisenEvents, smhiWarnings, krisItems] = await Promise.all([
    fetchPolisenEvents(),
    fetchSmhiWarnings(),
    fetchKrisinformationItems(),
  ]);

  const alerts: Alert[] = [
    ...polisenEvents.map<Alert>((e) => ({
      source: 'polisen',
      id: e.id,
      headline: e.title,
      description: e.summary,
      areas: e.location.name ? [e.location.name] : [],
      severity: 'info',
      publishedAt: e.occurredAt,
      url: e.url,
    })),
    ...smhiWarnings.map<Alert>((w) => ({
      source: 'smhi',
      id: w.id,
      headline: w.eventType,
      description: w.description,
      areas: w.areas,
      severity: levelToSeverity(w.level),
      publishedAt: w.validFrom,
      url: w.url,
    })),
    ...krisItems.map<Alert>((k) => ({
      source: 'krisinformation',
      id: k.id,
      headline: k.headline,
      description: k.preamble,
      areas: k.counties,
      severity: 'info',
      publishedAt: k.publishedAt,
      url: k.url,
    })),
  ];

  return alerts.sort((a, b) => b.publishedAt.getTime() - a.publishedAt.getTime());
}
