export const resourceLabels: Record<string, string> = {
  krisinformation: 'Krisinformation',
  polisen: 'Polisen',
  smhi: 'SMHI'
};

export const resourceColors: Record<string, string> = {
  krisinformation: '#23715f',
  polisen: '#0a4f86',
  smhi: '#d4a514'
};

export const resourceChipSx: Record<string, object> = {
  krisinformation: {
    color: '#ffffff',
    backgroundColor: resourceColors.krisinformation
  },
  polisen: {
    color: '#ffffff',
    backgroundColor: resourceColors.polisen
  },
  smhi: {
    color: '#15324b',
    backgroundColor: '#f0c649'
  }
};

export const severityLabels: Record<string, string> = {
  high: 'High',
  info: 'Info',
  low: 'Low',
  medium: 'Medium'
};

export const severityChipSx: Record<string, object> = {
  high: {
    color: '#ffffff',
    backgroundColor: '#b53a3f'
  },
  info: {
    color: '#15324b',
    backgroundColor: '#dbe7f2'
  },
  low: {
    color: '#1e4d3d',
    backgroundColor: '#d9efe6'
  },
  medium: {
    color: '#4e3900',
    backgroundColor: '#f1d88a'
  }
};

export const severityMapStyles: Record<
  string,
  { background: string; color: string; strokeOpacity: number; fillOpacity: number; weight: number }
> = {
  high: {
    background: '#b53a3f',
    color: '#ffffff',
    strokeOpacity: 0.95,
    fillOpacity: 0.34,
    weight: 3
  },
  info: {
    background: '#dbe7f2',
    color: '#15324b',
    strokeOpacity: 0.7,
    fillOpacity: 0.18,
    weight: 2
  },
  low: {
    background: '#d9efe6',
    color: '#1e4d3d',
    strokeOpacity: 0.78,
    fillOpacity: 0.22,
    weight: 2
  },
  medium: {
    background: '#f1d88a',
    color: '#4e3900',
    strokeOpacity: 0.88,
    fillOpacity: 0.28,
    weight: 3
  }
};

export const countyChipSx = {
  color: '#0a4f86',
  backgroundColor: '#eef4f8',
  borderColor: 'rgba(10, 79, 134, 0.18)'
};

export function normalizeResourceKey(value: string) {
  return value.toLowerCase();
}

export function formatSeverity(value: string) {
  return severityLabels[value] || (value.charAt(0).toUpperCase() + value.slice(1));
}
