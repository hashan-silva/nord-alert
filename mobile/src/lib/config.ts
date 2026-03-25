const configuredBaseUrl = process.env.EXPO_PUBLIC_BACKEND_BASE_URL;

export const baseUrl =
  configuredBaseUrl && configuredBaseUrl.length > 0
    ? configuredBaseUrl.replace(/\/$/, '')
    : 'http://10.0.2.2:8080';
