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
    'https://opendata-download-warnings.smhi.se/ibww/api/version/1/warning.json'
  );
  const data = (await body.json()) as any;
  const warnings = data?.warnings ?? [];
  return warnings.map((w: any) => {
    const levelStr = (
      w.level ??
      w.severity ??
      w.significance ??
      w.awareness_level ??
      ''
    )
      .toString()
      .toLowerCase();

    return {
      source: 'smhi',
      id: w.id ?? w.identifier ?? w.eventId ?? '',
      eventType:
        w.eventType ?? w.event?.event_type ?? w.event?.text ?? w.event ?? '',
      level: (['yellow', 'orange', 'red'].includes(levelStr)
        ? levelStr
        : 'yellow') as 'yellow' | 'orange' | 'red',
      description:
        w.description ?? w.information?.description ?? w.message ?? '',
      areas: (w.areas ?? w.area ?? w.regions ?? []).map(
        (a: any) => a.area ?? a.name ?? a.region ?? a
      ),
      validFrom: new Date(
        w.start ?? w.validFrom ?? w.valid_from ?? w.onset ?? w.from
      ),
      validTo: new Date(
        w.end ?? w.validTo ?? w.valid_to ?? w.expires ?? w.to
      ),
      url: w.urls?.[0]?.url ?? w.url ?? w.links?.[0]?.href ?? '',
    };
  });
}
