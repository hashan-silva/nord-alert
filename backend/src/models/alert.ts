export type AlertSource = 'polisen' | 'smhi' | 'krisinformation';

export type Severity = 'info' | 'low' | 'medium' | 'high';

export interface Alert {
  source: AlertSource;
  id: string;
  headline: string;
  description?: string;
  areas: string[];
  severity: Severity;
  publishedAt: Date;
  url: string;
}
