# NordAlert Mobile

Expo + React Native mobile client for NordAlert.

## Development

```bash
cd mobile
npm install
cp .env.example .env
npm run android
```

Set `EXPO_PUBLIC_BACKEND_BASE_URL` to the backend base URL without a trailing slash.

For the Android emulator talking to a backend running on the same machine, use:

```bash
EXPO_PUBLIC_BACKEND_BASE_URL=http://10.0.2.2:8080
```

For a physical Android device, use the host machine IP reachable from that device.

## Included Screens

- Live alert feed backed by `/alerts`
- Severity filter
- County filter chips backed by `/counties`
- Pull to refresh and loading/error states
