export type AlertSeverity = 'info' | 'medium' | 'high' | string;
export type AlertSource = 'POLISEN' | 'SMHI' | 'KRISINFORMATION' | string;

export interface AlertItem {
  areas?: string[];
  description?: string;
  geoJson?: Record<string, unknown>;
  headline: string;
  id: string;
  latitude?: number;
  longitude?: number;
  publishedAt?: string;
  severity: AlertSeverity;
  source: AlertSource;
  url?: string;
}

export interface CountyItem {
  code: string;
  name: string;
}

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
