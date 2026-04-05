export type AlertSeverity = 'info' | 'low' | 'medium' | 'high' | string;
export type AlertSource = 'polisen' | 'smhi' | 'krisinformation' | string;

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
