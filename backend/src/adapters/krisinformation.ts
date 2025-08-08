import { request } from 'undici';

export interface KrisinformationItem {
  source: 'krisinfo';
  id: string;
  headline: string;
  preamble: string;
  counties: string[];
  publishedAt: Date;
  url: string;
  pushMessage?: string;
}

async function fetchEndpoint(endpoint: string, counties?: string[]): Promise<any[]> {
  const qs = counties && counties.length ? `?counties=${counties.join(',')}` : '';
  const { body } = await request(`https://api.krisinformation.se/v3/${endpoint}${qs}`);
  return (await body.json()) as any[];
}

/**
 * Fetches and normalizes news and VMAS alerts from the Krisinformation API.
 */
export async function fetchKrisinformationItems(counties?: string[]): Promise<KrisinformationItem[]> {
  const [news, vmas] = await Promise.all([
    fetchEndpoint('news', counties),
    fetchEndpoint('vmas', counties),
  ]);
  const normalize = (item: any): KrisinformationItem => ({
    source: 'krisinfo',
    id: item.id,
    headline: item.headline ?? item.title ?? '',
    preamble: item.preamble ?? '',
    counties: item.counties ?? [],
    publishedAt: new Date(item.published ?? item.date),
    url: item.web ?? item.url ?? '',
    pushMessage: item.pushMessage,
  });
  return [...news, ...vmas].map(normalize);
}
