# Repository Guidelines

## Project Structure & Module Organization
- `backend/`: Node.js + TypeScript backend.
  - `src/index.ts`: Fastify HTTP entrypoint (`/alerts`).
  - `src/adapters/`: External data sources (Polisen, SMHI, Krisinformation, SCB, ArcGIS).
  - `src/services/`: Orchestration (aggregation, notifications).
  - `src/models/`: Domain types (e.g., `alert.ts`).
  - `Dockerfile`, `package.json`, `tsconfig.json`, build output in `dist/`.
- `terraform/`: IaC for deployment (Oracle Cloud). Key files: `main.tf`, `variables.tf`, `outputs.tf`.
- `.github/workflows/`: CI/CD for build, deploy, Terraform, and security scans.
- `sonar-project.properties`: SonarCloud configuration (scans `backend/`).

## Build, Test, and Development Commands
- Install: `cd backend && npm ci`
- Run dev: `npm run start` (Fastify on `http://localhost:3000`)
- Build: `npm run build` (emits `dist/`)
- Docker: `docker build -t nord-alert-backend backend && docker run -p 3000:3000 nord-alert-backend`
- Terraform: `cd terraform && terraform init && terraform plan && terraform apply`
 - Validate (always on PRs/changes):
   - Terraform fmt/validate: `terraform fmt -check -recursive && terraform validate`
   - TFLint: `tflint --init && tflint -f compact --recursive`
   - TFSec: `tfsec .`

## Coding Style & Naming Conventions
- TypeScript `strict` mode; prefer 2-space indentation, single quotes, and semicolons.
- Naming: PascalCase for types/classes; camelCase for variables/functions; filenames lowercase (e.g., `polisen.ts`).
- Organization: keep external I/O in `src/adapters/`, pure domain in `src/models/`, coordination in `src/services/`, and HTTP wiring in `src/index.ts`.

## Testing Guidelines
- No test runner is configured yet. Prefer adding Vitest or Jest (`*.spec.ts`).
- Focus on deterministic tests for adapters (parsing/mapping) and services (aggregation/filters).
- Once added: `npm test` from `backend/`. Keep unit tests fast; consider contract tests for external APIs.

## Validation Workflow (Before Submitting Changes)
- Run TypeScript build: `npm run build` inside `backend/`.
- Run infrastructure checks in `terraform/`:
  - `terraform fmt -check -recursive`
  - `terraform init -backend=false -input=false && terraform validate`
  - `tflint --init && tflint -f compact --recursive`
  - `tfsec .` (security scanning)
- If modifying Dockerfile, build locally: `docker build backend` and confirm server responds: `docker run -p 3000:3000 <image>` then `curl http://localhost:3000/alerts`.
 - Mobile checks in `mobile/`:
   - `flutter pub get`
   - `dart format .` (auto-format) then `dart format --output=none --set-exit-if-changed .`
   - `flutter analyze --fatal-infos --fatal-warnings`
   - `flutter test`

## Branching & PR Policy
- Do not commit directly to `main`. Create feature branches (e.g., `feat/...`, `fix/...`, `chore/...`).
- Open a PR to `main` with a clear description, verification steps, and linked issues.
- Wait for CI (build, tfsec, tflint, Terraform validate) to pass before merging.

## Terraform State & Idempotency
- CI deployments rely on Terraform state to avoid re-creating resources. Configure a remote backend (e.g., Terraform Cloud) so subsequent `apply` runs update existing resources.
- Without a remote backend, each clean CI run may treat the infra as new. Use a backend to ensure stable, incremental applies.

## Commit & Pull Request Guidelines
- Commits: use Conventional Commits when possible (`feat:`, `fix:`, `chore:`) and reference issues (e.g., `#23`). Keep messages imperative and concise (≤72 chars subject).
- PRs: include a clear summary, scope of changes, verification steps (e.g., `curl "http://localhost:3000/alerts?county=Stockholm&severity=medium"`), and deployment impact. Add screenshots/logs when helpful.

## Security & Configuration Tips
- Never commit secrets. Use GitHub Secrets for CI/CD. Required secrets for deploy are listed in `README.md` (Docker Hub, OCI credentials).
- Local config: `PORT` (defaults to 3000). Provide Firebase/API credentials via environment and cloud secret managers.
- Infrastructure: run `terraform plan` before `apply`; keep changes minimal and reviewed. Consider running `tfsec` locally for policy checks.
