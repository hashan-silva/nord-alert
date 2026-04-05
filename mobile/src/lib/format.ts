import type { AlertSeverity, AlertSource } from '../models/alert';

export function severityLabel(severity: AlertSeverity): string {
  switch (severity) {
    case 'high':
      return 'High';
    case 'medium':
      return 'Medium';
    case 'low':
      return 'Low';
    case 'info':
      return 'Info';
    default:
      return severity;
  }
}

export function sourceLabel(source: AlertSource): string {
  switch (source) {
    case 'polisen':
      return 'Polisen';
    case 'smhi':
      return 'SMHI';
    case 'krisinformation':
      return 'Krisinformation';
    default:
      return source;
  }
}

export function formatPublishedAt(value?: string): string {
  if (!value) {
    return 'Time unavailable';
  }

  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return 'Time unavailable';
  }

  return new Intl.DateTimeFormat('en-GB', {
    dateStyle: 'medium',
    timeStyle: 'short'
  }).format(date);
}
