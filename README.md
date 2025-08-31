# NordAlert

NordAlert is a cross-platform mobile application that aggregates and pushes official alerts from Polisen (the Swedish Police Authority), SMHI (Swedish Meteorological and Hydrological Institute) weather warnings, and Krisinformation (Swedish Crisis Information).

[![Build and Deploy Backend](https://github.com/hashan-silva/nord-alert/actions/workflows/deploy.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/deploy.yml)
[![Build and Analyze](https://github.com/hashan-silva/nord-alert/actions/workflows/build.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/build.yml)
[![CodeQL](https://github.com/hashan-silva/nord-alert/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/github-code-scanning/codeql)
[![Terraform Lint & Validate](https://github.com/hashan-silva/nord-alert/actions/workflows/terraform-ci.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/terraform-ci.yml)
[![Trivy IaC Scan](https://github.com/hashan-silva/nord-alert/actions/workflows/tfsec.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/tfsec.yml)
[![Flutter CI](https://github.com/hashan-silva/nord-alert/actions/workflows/flutter-ci.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/flutter-ci.yml)

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

This repo includes a minimal Flutter client in `mobile/` to list alerts and filter by county and provider.

- Run (emulator):
  - Android: `cd mobile && flutter pub get && flutter run --dart-define=BACKEND_BASE_URL=http://10.0.2.2:3000`
  - iOS/Web: `flutter run --dart-define=BACKEND_BASE_URL=http://localhost:3000`
- Run (physical device): use the in‑app Settings panel (top‑right gear) to enter your backend base URL (e.g., `http://<LAN-IP>:3000`). Use “Test” to verify connectivity, then Save.
- Filtering: provider chips (Polisen/SMHI/Krisinformation) are client‑side; county dropdown refetches using the backend `county` query.
- Data shape: `{ id, source, headline, description, areas, severity, publishedAt, url }`.

### Data Sources

The backend retrieves information from a number of official Swedish services:

- **Polisen events** – https://polisen.se/api/events
- **SMHI impact-based warnings** – https://opendata-download-warnings.smhi.se/warnings/objects
- **Krisinformation** – https://api.krisinformation.se/v3/news and https://api.krisinformation.se/v3/vmas
- **SCB PxWeb** – region lists (county and municipality codes/names)
- **County Administrative Boards ArcGIS** – GeoJSON polygons for counties and municipalities

### Deployment & CI

GitHub Actions builds and pushes the backend Docker image (tagged with commit SHA) and deploys it to Oracle Cloud via Terraform on pushes to `main`.

- Workflows: Deploy (`deploy.yml`), Sonar (`build.yml`), Terraform lint/validate, tfsec (SARIF → Code Scanning), and Flutter CI for `mobile/`.
- Terraform: provisions VCN, subnet, IGW/route, NSG, and a VM; cloud‑init installs Docker and runs the container mapping port 80 → 3000.
- Docker: runs as non‑root `node` user and exposes `/health` for container health checks.
- Secrets required: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `OCI_TENANCY_OCID`, `OCI_USER_OCID`, `OCI_FINGERPRINT`, `OCI_PRIVATE_KEY`, `OCI_REGION`, `OCI_COMPARTMENT_OCID`, `SSH_PUBLIC_KEY`.
- Recommendation: use a remote Terraform backend to persist state across runs for reliable, incremental applies.
