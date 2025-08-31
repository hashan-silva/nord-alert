# NordAlert

NordAlert is a cross-platform mobile application that aggregates and pushes official alerts from Polisen (the Swedish Police Authority), SMHI (Swedish Meteorological and Hydrological Institute) weather warnings, and Krisinformation (Swedish Crisis Information).

[![Build and Deploy Backend](https://github.com/hashan-silva/nord-alert/actions/workflows/deploy.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/deploy.yml)
[![Build and Analyze](https://github.com/hashan-silva/nord-alert/actions/workflows/build.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/build.yml)
[![CodeQL](https://github.com/hashan-silva/nord-alert/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/github-code-scanning/codeql)
[![Terraform Lint & Validate](https://github.com/hashan-silva/nord-alert/actions/workflows/terraform-ci.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/terraform-ci.yml)
[![Trivy IaC Scan](https://github.com/hashan-silva/nord-alert/actions/workflows/tfsec.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/tfsec.yml)

## Features

- View a real-time feed of alerts from multiple official sources.
- Filter alerts by region or using the device's current location.
- Set severity thresholds to control which alerts you see.
- Receive push notifications for alerts that match your filter criteria.

## Tech Stack

This project is built with a modern, production-ready tech stack:

- **Mobile App (Flutter):**
  - **Framework:** Flutter (latest stable)
  - **State Management:** `flutter_bloc`
  - **Models:** `freezed`, `json_serializable`
  - **API Client:** `dio`
  - **Geolocation:** `geolocator`, `permission_handler`
  - **Push Notifications:** `firebase_core`, `firebase_messaging`, `flutter_local_notifications`
  - **Utilities:** `intl`, `url_launcher`

- **Backend (Node.js on Google Cloud):**
  - **Runtime:** Node.js 20 with TypeScript
  - **Platform:** Google Cloud Functions or Cloud Run
  - **Framework:** Fastify (if using Cloud Run)
  - **API Client:** `undici`
  - **Validation:** `zod`
  - **Scheduling:** Native cron jobs or Google Cloud Scheduler
  - **Firebase:** `firebase-admin`

- **Database:**
  - **Store:** Firestore for storing normalized alerts and managing deduplication.

- **Push Notifications:**
  - **Service:** Firebase Cloud Messaging (FCM) using topics for targeted notifications.

## Repository Structure

This repository is a monorepo containing the mobile app and the backend server.

- **/mobile/**: Contains the Flutter mobile application.
- **/backend/**: Contains the Node.js/TypeScript backend services.
- **/docs/**: Contains project documentation.

## Development

### Backend

```bash
cd backend
npm install
npm run start
```

The API exposes a `/alerts` endpoint which accepts optional `county` and `severity` query parameters for filtering.

### Mobile (Flutter)

Quickly scaffold and run a simple client to list and filter alerts by county and provider.

1) Create the app (if `mobile/` is missing):
   - `flutter create mobile`
   - `cd mobile`
2) Add minimal deps:
   - `flutter pub add dio flutter_bloc equatable`
3) Configure backend URL at run time:
   - Android emulator: `flutter run --dart-define=BACKEND_BASE_URL=http://10.0.2.2:3000`
   - iOS simulator/Web: `flutter run --dart-define=BACKEND_BASE_URL=http://localhost:3000`
4) Fetch alerts from: `${BACKEND_BASE_URL}/alerts?county=Stockholm&severity=medium`

Notes
- Filter by provider client‑side using the `source` field (`polisen`, `smhi`, `krisinformation`).
- A simple model matches the backend JSON: `{ id, source, headline, description, areas, severity, publishedAt, url }`.
- For production, store `BACKEND_BASE_URL` per‑flavor, and add caching/offline as needed.

### Data Sources

The backend retrieves information from a number of official Swedish services:

- **Polisen events** – https://polisen.se/api/events
- **SMHI impact-based warnings** – https://opendata-download-warnings.smhi.se/warnings/objects
- **Krisinformation** – https://api.krisinformation.se/v3/news and https://api.krisinformation.se/v3/vmas
- **SCB PxWeb** – region lists (county and municipality codes/names)
- **County Administrative Boards ArcGIS** – GeoJSON polygons for counties and municipalities

### Deployment

The repository includes a GitHub Actions workflow that builds the backend Docker image, pushes it to Docker Hub, and uses Terraform to deploy it to Oracle Cloud on every push to `main`. Configure the following secrets in your repository settings:

- `DOCKERHUB_USERNAME` and `DOCKERHUB_TOKEN` for Docker Hub access.
- `OCI_TENANCY_OCID`, `OCI_USER_OCID`, `OCI_FINGERPRINT`, `OCI_PRIVATE_KEY`, `OCI_REGION`, and `OCI_COMPARTMENT_OCID` for Oracle Cloud deployments. Terraform automatically determines the availability domain and subnet.
