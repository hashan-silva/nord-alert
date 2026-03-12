# NordAlert — Agent Guide (Java + Flutter)

This document adapts the sample agent guidelines for a Java backend, a Flutter mobile app, and Terraform IaC. Follow it when contributing or using an AI agent on this repo.

## Project Structure
- `backend/`: Java 17 backend
  - `src/main/java/se/nordalert/backend/NordAlertApplication.java`: Spring Boot entrypoint
  - `src/main/java/se/nordalert/backend/adapters/`: External data sources (Polisen, SMHI, Krisinformation)
  - `src/main/java/se/nordalert/backend/services/`: Orchestration and aggregation
  - `src/main/java/se/nordalert/backend/models/`: Domain types
  - `src/main/java/se/nordalert/backend/controllers/`: HTTP endpoints
  - Build artifacts in `target/`
- `mobile/`: Flutter app (Dart, BLoC)
- `terraform/`: Oracle Cloud IaC (`main.tf`, `variables.tf`, `outputs.tf`)
- `.github/workflows/`: CI/CD (build, deploy, Terraform, security scans)
- `sonar-project.properties`: SonarCloud configuration (scans `backend/`)

## Golden Rules
- Make minimal, task-scoped changes; avoid unrelated edits.
- Read existing docs first (`README.md`, this file, workflow YAMLs).
- Explore the repo before coding (`ls`, open files, skim structure).
- Do not commit from the agent; use read-only git commands for context.
- Validate locally before ending a task. See Validation Checklists.
- Always propose a plan with a short TODO list and wait for explicit user approval before executing.

## Architecture & State
- Backend: Keep I/O in `adapters`, pure domain in `models`, coordination in `services`, and HTTP wiring in `controllers`.
- Mobile: Use Flutter BLoC for state; prefer deriving UI state from events/data rather than ad-hoc booleans. Persist only what’s necessary (e.g., base URL) via `shared_preferences`.
- Single source of truth: Avoid duplicating state across layers.

## Coding Style
- Java: target Amazon Corretto 17, use Spring Boot conventions, 2 spaces, and keep packages under `se.nordalert.backend`.
- Dart/Flutter: follow `flutter_lints`; keep widgets small and composable.
- Naming: PascalCase for Java types/classes; camelCase for vars/functions; lowercase package names.
- Errors: Backend code should bubble meaningful errors; avoid blanket try/catch. Flutter dev code can assert, but production UI should handle failures gracefully.

## Backend — Dev & Build
- Run dev: `cd backend && mvn spring-boot:run` (Spring Boot on `http://localhost:3000`)
- Build: `cd backend && mvn package` (emits `target/`)
- Docker: `docker build -t nord-alert-backend backend && docker run -p 3000:3000 nord-alert-backend`

## Mobile — Dev & Checks
- Setup: `cd mobile && flutter pub get`
- Format: `dart format .` then `dart format --output=none --set-exit-if-changed .`
- Analyze: `flutter analyze --fatal-infos --fatal-warnings`
- Test: `flutter test`
- Run: `flutter run --dart-define=BACKEND_BASE_URL=http://<host>:3000`

## Terraform — IaC
- Validate: `terraform fmt -check -recursive`
- Init/validate (no backend): `terraform init -backend=false -input=false && terraform validate`
- Lint: `tflint --init && tflint -f compact --recursive`
- Security: `tfsec .`
- Plan/apply: `terraform plan && terraform apply` (use a remote backend for idempotent CI applies)

## Mandatory Validation (Before Finishing a Task)
Perform the checks relevant to the files you changed:

Backend (Java)
- `cd backend && mvn package`
- If Dockerfile changed: `docker build backend` and verify `/alerts` responds: `docker run -p 3000:3000 <image>` then `curl http://localhost:3000/alerts`

Mobile (Flutter)
- `cd mobile && flutter pub get`
- `dart format --output=none --set-exit-if-changed .`
- `flutter analyze --fatal-infos --fatal-warnings`
- `flutter test`

Terraform
- `cd terraform && terraform fmt -check -recursive`
- `terraform init -backend=false -input=false && terraform validate`
- `tflint --init && tflint -f compact --recursive`
- `tfsec .`

## Branching & PRs
- Branch from `main` (do not commit directly to `main`).
- Use Conventional Commit prefixes where possible (`feat:`, `fix:`, `chore:`) and reference issues (e.g., `#23`).
- Open a PR with: summary, scope, verification steps, expected deployment impact, and screenshots/logs when helpful.
- Merge only after CI passes (backend container build, Terraform checks, tfsec, tflint, Flutter analyze/test).

## Secrets & Configuration
- Never commit secrets. Use GitHub Secrets for CI/CD (see README for required secrets: Docker Hub, OCI credentials).
- Backend: `PORT` defaults to 3000; external API keys via env/secret managers.
- Mobile: Provide `BACKEND_BASE_URL` via `Settings` dialog or `--dart-define`.
- Terraform: Prefer a remote backend (e.g., Terraform Cloud) to keep state stable and idempotent between runs.

## Reviewer/Agent Checklist
- Changes are minimal and focused; no stray file churn.
- Code aligns with module boundaries and naming conventions.
- All relevant validation steps above are green.
- Docs updated if behavior or interfaces changed.
-
## Planning & Approvals
- Before making any changes, present a concise plan and a short TODO list that outlines the intended steps.
- Do not execute the plan until the user explicitly approves it. If unclear, ask for confirmation.
- After approval, follow the agreed plan; if scope changes, submit an updated plan for re-approval.
- Keep TODO items small (5–7 words each) and logically ordered; mark progress as steps complete.
