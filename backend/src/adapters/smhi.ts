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
  const data = (await body.json()) as any[];
  const warnings: SmhiWarning[] = [];

  for (const w of data ?? []) {
    const eventType = w.event?.en ?? w.event?.sv ?? w.event?.code ?? '';

    for (const area of w.warningAreas ?? []) {
      const levelCode = (area.warningLevel?.code ?? '')
        .toString()
        .toLowerCase();
      let level: 'yellow' | 'orange' | 'red' = 'yellow';
      if (['yellow', 'orange', 'red'].includes(levelCode)) {
        level = levelCode as 'yellow' | 'orange' | 'red';
      } else if (levelCode === 'message') {
        level = 'yellow';
      }

      const description = (area.descriptions ?? [])
        .map((d: any) => d.text?.en ?? d.text?.sv ?? '')
        .filter(Boolean)
        .join('\n');

      const areas = (area.affectedAreas ?? [])
        .map((a: any) => a.en ?? a.sv ?? '')
        .filter(Boolean);

      warnings.push({
        source: 'smhi',
        id: `${w.id}-${area.id}`,
        eventType,
        level,
        description,
        areas,
        validFrom: new Date(area.approximateStart ?? area.published ?? w.created),
        validTo: new Date(
          area.approximateEnd ?? area.approximateStart ?? area.published ?? w.created
        ),
        url: '',
      });
    }
  }

  return warnings;
}
