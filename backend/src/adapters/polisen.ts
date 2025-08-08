import { request } from 'undici';

export interface PolisenEvent {
  source: 'polisen';
  id: string;
  title: string;
  type: string;
  summary: string;
  url: string;
  occurredAt: Date;
  location: {
    name: string;
    lat: number | null;
    lon: number | null;
  };
}

/**
 * Fetches and normalizes police events from the public Polisen API.
 */
export async function fetchPolisenEvents(): Promise<PolisenEvent[]> {
  const { body } = await request('https://polisen.se/api/events');
  const events = (await body.json()) as any[];
  return events.map((e: any) => {
    let lat: number | null = null;
    let lon: number | null = null;
    if (typeof e.location?.gps === 'string') {
      const [latStr, lonStr] = e.location.gps.split(',').map((s: string) => s.trim());
      lat = Number(latStr);
      lon = Number(lonStr);
    }
    return {
      source: 'polisen',
      id: String(e.id ?? e.eventid),
      title: e.name ?? '',
      type: e.type ?? '',
      summary: e.summary ?? '',
      url: e.url ?? `https://polisen.se/aktuellt/handelser/${e.id ?? ''}`,
      occurredAt: new Date(e.datetime),
      location: {
        name: e.location?.name ?? '',
        lat,
        lon,
      },
    };
  });
}
