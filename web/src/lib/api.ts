export type AlertSeverity = 'info' | 'medium' | 'high' | string;
export type AlertSource = 'POLISEN' | 'SMHI' | 'KRISINFORMATION' | string;

export interface AlertItem {
  areas?: string[];
  description?: string;
  headline: string;
  id: string;
  publishedAt?: string;
  severity: AlertSeverity;
  source: AlertSource;
  url?: string;
}

interface FetchAlertsParams {
  county: string;
  severity: string;
}

const configuredBaseUrl = process.env.REACT_APP_BACKEND_BASE_URL;

export const baseUrl =
  configuredBaseUrl && configuredBaseUrl.length > 0
    ? configuredBaseUrl.replace(/\/$/, '')
    : 'http://localhost:8080';

export async function fetchAlerts({ county, severity }: FetchAlertsParams): Promise<AlertItem[]> {
  const url = new URL(`${baseUrl}/alerts`);

  if (county) {
    url.searchParams.set('county', county);
  }

  if (severity) {
    url.searchParams.set('severity', severity);
  }

  const response = await fetch(url.toString());

  if (!response.ok) {
    throw new Error(`Alerts request failed with status ${response.status}`);
  }

  return response.json() as Promise<AlertItem[]>;
}
