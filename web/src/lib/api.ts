import type { AlertItem } from '../models/alert';
import type { CountyItem } from '../models/county';
import type { CreateSubscriptionRequest, SubscriptionItem } from '../models/subscription';

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

export async function createSubscription(
  request: CreateSubscriptionRequest
): Promise<SubscriptionItem> {
  const response = await fetch(`${baseUrl}/subscriptions`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(request)
  });

  if (!response.ok) {
    throw new Error(`Subscription request failed with status ${response.status}`);
  }

  return response.json() as Promise<SubscriptionItem>;
}
