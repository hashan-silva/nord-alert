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

- **Backend (Java on AWS Lambda):**
  - **Runtime:** Amazon Corretto 17
  - **Framework:** Spring Boot 3
  - **Build:** Maven
  - **HTTP Client:** Java `HttpClient`
  - **Serialization:** Jackson
  - **Serverless Adapter:** `aws-serverless-java-container`

- **Database:**
  - **Store:** Firestore for storing normalized alerts and managing deduplication.

- **Push Notifications:**
  - **Service:** Firebase Cloud Messaging (FCM) using topics for targeted notifications.

## Repository Structure

This repository is a monorepo containing the mobile app and the backend server.

- **/mobile/**: Contains the Flutter mobile application.
- **/backend/**: Contains the Java 17 backend service.
- **/docs/**: Contains project documentation.

## Development

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API exposes a `/alerts` endpoint which accepts optional `county` and `severity` query parameters for filtering.
OpenAPI docs are available at `/v3/api-docs`, and Swagger UI is available at `/swagger-ui/index.html` during normal web execution.

### Mobile (Flutter)

This repo includes a minimal Flutter client in `mobile/` to list alerts and filter by county and provider.

- Run (emulator):
  - Android: `cd mobile && flutter pub get && flutter run --dart-define=BACKEND_BASE_URL=http://10.0.2.2:8080`
  - iOS/Web: `flutter run --dart-define=BACKEND_BASE_URL=http://localhost:8080`
- Run (physical Android/iOS device):
  - Ensure phone and backend host are on the same Wi‑Fi/LAN.
  - Start backend listening on all interfaces (for example `mvn spring-boot:run`). Allow firewall on port 8080.
  - Find your computer’s LAN IP (e.g., `ipconfig` on Windows; `ifconfig`/`ip addr` on macOS/Linux).
  - Launch the app (`flutter run -d <device>`). Open Settings (top‑right gear), enter `http://<LAN-IP>:8080`, tap Test, then Save.
  - Android 9+ cleartext HTTP: if requests fail, set `android:usesCleartextTraffic="true"` in `android/app/src/main/AndroidManifest.xml` (dev only), or use HTTPS.
  - iOS ATS: if HTTP is blocked, add a temporary ATS exception for development or use HTTPS.
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

GitHub Actions builds the backend Docker image, pushes it to Docker Hub and Amazon ECR, and deploys the backend to AWS Lambda plus API Gateway via Terraform on pushes to `main`.

- Workflows: Deploy (`deploy.yml`), Sonar (`build.yml`), Terraform lint/validate, tfsec (SARIF → Code Scanning), and Flutter CI for `mobile/`.
- Terraform: provisions an ECR repository, Lambda execution role, Lambda function, and API Gateway HTTP API.
- Docker: builds a standard Java 17 web image for Docker Hub from `backend/Dockerfile` and a Lambda-compatible image for ECR from `backend/Dockerfile.lambda`.
- Secrets required: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `TF_API_TOKEN`.
- Default AWS region is Stockholm (`eu-north-1`). Set `AWS_REGION` in GitHub Secrets only if you want to override that default in CI.
- Recommendation: use a remote Terraform backend to persist state across runs for reliable, incremental applies.
