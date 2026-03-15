# NordAlert

NordAlert is a web dashboard and backend platform that aggregates official alerts from Polisen (the Swedish Police Authority), SMHI (Swedish Meteorological and Hydrological Institute), and Krisinformation (Swedish Crisis Information).

Current application versions:
- Backend: `1.1.1`
- Frontend: `1.1.1`

[![Build and Deploy Backend](https://github.com/hashan-silva/nord-alert/actions/workflows/deploy.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/deploy.yml)
[![Build and Analyze](https://github.com/hashan-silva/nord-alert/actions/workflows/build.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/build.yml)
[![CodeQL](https://github.com/hashan-silva/nord-alert/actions/workflows/github-code-scanning/codeql/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/github-code-scanning/codeql)
[![Terraform Lint & Validate](https://github.com/hashan-silva/nord-alert/actions/workflows/terraform-ci.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/terraform-ci.yml)
[![Trivy IaC Scan](https://github.com/hashan-silva/nord-alert/actions/workflows/tfsec.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/tfsec.yml)
[![Publish Docker Hub Images](https://github.com/hashan-silva/nord-alert/actions/workflows/dockerhub.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/dockerhub.yml)
[![Web CI](https://github.com/hashan-silva/nord-alert/actions/workflows/web-ci.yml/badge.svg)](https://github.com/hashan-silva/nord-alert/actions/workflows/web-ci.yml)

## Features

- View a real-time dashboard of alerts from multiple official sources.
- Filter alerts by one or more counties and severity threshold.
- Subscribe by email for new alerts that match counties, sources, and severity.
- Monitor source volumes across Polisen, SMHI, and Krisinformation.
- Publish the web frontend globally through S3 and CloudFront.

## Tech Stack

This project is built with a modern, production-ready tech stack:

- **Web App (React):**
  - **Framework:** React 19
  - **Language:** TypeScript (`.tsx`)
  - **UI:** Material UI
  - **Styling:** SCSS
  - **Bundler:** Webpack

- **Backend (Java on AWS Lambda):**
  - **Runtime:** Amazon Corretto 17
  - **Framework:** Spring Boot 3
  - **Build:** Maven
  - **HTTP Client:** Java `HttpClient`
  - **Serialization:** Jackson
  - **Serverless Adapter:** `aws-serverless-java-container`

- **Frontend Hosting:**
  - **Static Assets:** Amazon S3
  - **CDN:** Amazon CloudFront

## Repository Structure

This repository is a monorepo containing the web dashboard, backend service, and Terraform infrastructure.

- **/web/**: Contains the React web dashboard.
- **/backend/**: Contains the Java 17 backend service.
- **/terraform/**: Contains AWS infrastructure, including Lambda, API Gateway, S3, and CloudFront.

## Architecture

NordAlert is split into a React frontend, a Spring Boot backend running on AWS Lambda, and Terraform-managed AWS infrastructure.

- **Web dashboard**
  - The React app loads alerts from the backend `/alerts` API and county metadata from `/counties`.
  - Users can filter alerts by counties, severity, sources, and date range.
  - The map view renders point alerts and polygon-based warning areas directly in the browser with Leaflet and OpenStreetMap tiles.

- **Backend API**
  - `adapters/` fetch and normalize external source data from Polisen, SMHI, Krisinformation, and SCB.
  - `services/` aggregate alerts, resolve county data, and handle subscription workflows.
  - `controllers/` expose the HTTP API for alerts, counties, health, and subscriptions.
  - The backend exposes aggregated alert items with optional coordinates and GeoJSON so the frontend can render both list and map views.

- **AWS serverless platform**
  - API requests flow through API Gateway to the main backend Lambda.
  - The React frontend is deployed to S3 and served globally through CloudFront.
  - Email subscriptions are stored in DynamoDB.
  - SES is used for sender identity management, recipient verification, and outbound alert email delivery.
  - EventBridge triggers a scheduled Lambda dispatcher that checks confirmed subscriptions and sends new matching alerts.

### Subscription Flow

1. A user creates a subscription from the web dashboard.
2. The backend stores the subscription in DynamoDB with `pending` status.
3. The backend requests an SES email identity for the recipient address.
4. SES sends a verification email to the subscriber.
5. Once SES reports that address as verified, NordAlert treats the subscription as `confirmed`.
6. The scheduled dispatcher fetches confirmed subscriptions, filters new alerts, and sends matching emails through SES.

## Development

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API exposes a `/alerts` endpoint which accepts optional repeated `county` parameters and a `severity` query parameter for filtering.
It also exposes `/counties` for the official county reference list and `POST /subscriptions` for email alert subscriptions.
OpenAPI docs are available at `/v3/api-docs`, and Swagger UI is available at `/swagger-ui/index.html` during normal web execution.

### Web Dashboard

The web app in `web/` renders a React dashboard backed by the `/alerts` API.

```bash
cd web
npm install
REACT_APP_BACKEND_BASE_URL=http://localhost:8080 npm start
```

For a production build:

```bash
cd web
npm install
REACT_APP_BACKEND_BASE_URL=http://localhost:8080 npm run build
```

The dashboard expects alert items shaped like:

```json
{ "id": "1", "source": "POLISEN", "headline": "...", "description": "...", "areas": ["Stockholms län"], "severity": "high", "publishedAt": "2026-03-12T16:52:18Z", "url": "https://example.com" }
```

### Docker Compose

You can run the backend and web dashboard together locally with Docker Compose:

```bash
docker compose up --build
```

This starts:
- backend on `http://localhost:8080`
- web dashboard on `http://localhost:3000`

The compose setup builds the web app with `REACT_APP_BACKEND_BASE_URL=http://localhost:8080` so the browser can call the backend through the host-mapped port.

### Data Sources

The backend retrieves information from a number of official Swedish services:

- **Polisen events** – https://polisen.se/api/events
- **SMHI impact-based warnings** – https://opendata-download-warnings.smhi.se/warnings/objects
- **Krisinformation** – https://api.krisinformation.se/v3/news and https://api.krisinformation.se/v3/vmas
- **SCB PxWeb** – region lists (county and municipality codes/names)
- **County Administrative Boards ArcGIS** – GeoJSON polygons for counties and municipalities

### Deployment & CI

GitHub Actions builds the backend Docker image, pushes it to Docker Hub and Amazon ECR, deploys the backend to AWS Lambda plus API Gateway via Terraform, then builds and publishes the React dashboard to S3 behind CloudFront.

- Workflows: Deploy (`deploy.yml`), Sonar (`build.yml`), Terraform lint/validate, tfsec (SARIF → Code Scanning), and Web CI for `web/`.
- Terraform: provisions the Lambda backend, a scheduled Lambda dispatcher, API Gateway HTTP API, DynamoDB, SES sender identity, S3 frontend bucket, and CloudFront distribution.
- Docker: builds a standard Java 17 backend image from `backend/Dockerfile`, a Lambda-compatible image for ECR from `backend/Dockerfile.lambda`, and a web image from `web/Dockerfile`.
- Secrets required: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `TF_API_TOKEN`, `SES_SENDER_EMAIL`.
- Default AWS region is Stockholm (`eu-north-1`). Set `AWS_REGION` in GitHub Secrets only if you want to override that default in CI.
- Recommendation: use a remote Terraform backend to persist state across runs for reliable, incremental applies.
