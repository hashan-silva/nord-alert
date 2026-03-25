import type { AlertItem } from '../models/alert';
import type { CountyItem } from '../models/county';
import { baseUrl } from './config';

interface FetchAlertsParams {
  county?: string;
  severity: string;
}

function buildAlertsUrl({ county, severity }: FetchAlertsParams): string {
  const queryParts: string[] = [];

  if (county) {
    queryParts.push(`county=${encodeURIComponent(county)}`);
  }

  if (severity) {
    queryParts.push(`severity=${encodeURIComponent(severity)}`);
  }

  return queryParts.length > 0
    ? `${baseUrl}/alerts?${queryParts.join('&')}`
    : `${baseUrl}/alerts`;
}

export async function fetchAlerts(params: FetchAlertsParams): Promise<AlertItem[]> {
  const response = await fetch(buildAlertsUrl(params));

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
