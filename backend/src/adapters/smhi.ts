import { request } from 'undici';

export interface SmhiWarning {
  source: 'smhi';
  id: string;
  eventType: string;
  level: 'yellow' | 'orange' | 'red';
  description: string;
  areas: string[];
  validFrom: Date;
  validTo: Date;
  url: string;
}

/**
 * Fetches impact-based weather warnings from the SMHI API and normalizes them.
 */
export async function fetchSmhiWarnings(): Promise<SmhiWarning[]> {
  const { body } = await request(
    'https://opendata-download-warnings.smhi.se/api/category/severe-weather/version/2/warning.json'
  );
  const data = (await body.json()) as any;
  const warnings = data?.warnings ?? [];
  return warnings.map((w: any) => ({
    source: 'smhi',
    id: w.id ?? w.identifier,
    eventType: w.event ?? w.title ?? '',
    level: (w.level ?? w.severity ?? '').toLowerCase(),
    description: w.description ?? '',
    areas: (w.areas ?? []).map((a: any) => a.area ?? a.name ?? a),
    validFrom: new Date(w.start ?? w.validFrom ?? w.from),
    validTo: new Date(w.end ?? w.validTo ?? w.to),
    url: w.urls?.[0]?.url ?? w.url ?? '',
  }));
}
