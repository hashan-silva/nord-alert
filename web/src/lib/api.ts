import type { AlertItem } from '../models/alert';
import type { CountyItem } from '../models/county';

interface FetchAlertsParams {
  counties: string[];
  severity: string;
}

const configuredBaseUrl = process.env.REACT_APP_BACKEND_BASE_URL;

export const baseUrl =
  configuredBaseUrl && configuredBaseUrl.length > 0
    ? configuredBaseUrl.replace(/\/$/, '')
    : 'http://localhost:8080';

export async function fetchAlerts({ counties, severity }: FetchAlertsParams): Promise<AlertItem[]> {
  const url = new URL(`${baseUrl}/alerts`);

  counties.forEach((county) => url.searchParams.append('county', county));

  if (severity) {
    url.searchParams.set('severity', severity);
  }

  const response = await fetch(url.toString());

  if (!response.ok) {
    throw new Error(`Alerts request failed with status ${response.status}`);
  }

  return response.json() as Promise<AlertItem[]>;
}

export async function fetchCounties(): Promise<CountyItem[]> {
  const response = await fetch(`${baseUrl}/counties`);

  if (!response.ok) {
    throw new Error(`Counties request failed with status ${response.status}`);
  }

  return response.json() as Promise<CountyItem[]>;
}
