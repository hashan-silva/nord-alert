# NordAlert — Agent Guide (Java + React)

This document adapts the sample agent guidelines for a Java backend, a React web dashboard, and Terraform IaC. Follow it when contributing or using an AI agent on this repo.

## Project Structure
- `backend/`: Java 17 backend
  - `src/main/java/com/hashan0314/nordalert/backend/NordAlertApplication.java`: Spring Boot entrypoint
  - `src/main/java/com/hashan0314/nordalert/backend/adapters/`: External data sources (Polisen, SMHI, Krisinformation)
  - `src/main/java/com/hashan0314/nordalert/backend/services/`: Orchestration and aggregation
  - `src/main/java/com/hashan0314/nordalert/backend/models/`: Domain types
  - `src/main/java/com/hashan0314/nordalert/backend/controllers/`: HTTP endpoints
  - Build artifacts in `target/`
- `web/`: React dashboard (TypeScript, Material UI, SCSS)
- `terraform/`: AWS serverless IaC (`main.tf`, `variables.tf`, `outputs.tf`)
- `.github/workflows/`: CI/CD (build, deploy, Terraform, security scans)
- `sonar-project.properties`: SonarCloud configuration (scans `backend/`)

## Golden Rules
- Make minimal, task-scoped changes; avoid unrelated edits.
- Read existing docs first (`README.md`, this file, workflow YAMLs).
- Explore the repo before coding (`ls`, open files, skim structure).
- Prefer configured MCP servers over ad hoc shell or web lookups when they cover the task.
- Do not commit from the agent; use read-only git commands for context.
- Validate locally before ending a task. See Validation Checklists.
- Always propose a plan with a short TODO list and wait for explicit user approval before executing.

## MCP Usage
- Use the configured `github` MCP server for repository contents, pull requests, issues, branches, releases, and review actions when possible.
- Use the configured `terraform` MCP server for provider, module, and registry documentation before falling back to manual searches.
- Use the configured `sonarqube` MCP server for code quality findings, metrics, rules, and issue status when Sonar data is relevant.
- Run Sonar analysis through the configured `sonarqube` MCP server for backend code changes when practical, and review the reported findings before finishing the task.
- Fall back to shell commands or web research only when the MCP servers do not expose the needed data or action.

## Architecture & State
- Backend: Keep I/O in `adapters`, pure domain in `models`, coordination in `services`, and HTTP wiring in `controllers`.
- Web: Keep API access in small client modules, UI composition in focused React components, and styling in SCSS files layered around Material UI.
- Single source of truth: Avoid duplicating state across layers.
- Keep backend and web models in separate dedicated files under their respective model/type folders; avoid inline model declarations in unrelated files.
- Do not use inner classes or inner records for public data models; extract them into standalone classes/files.

## Coding Style
- Java: target Amazon Corretto 17, use Spring Boot conventions, 2 spaces, and keep packages under `com.hashan0314.nordalert.backend`.
- React/TypeScript: prefer small typed components, keep API models explicit, and use SCSS for layout/theme structure rather than inline styling sprawl.
- Naming: PascalCase for Java types/classes; camelCase for vars/functions; lowercase package names.
- Errors: Backend code should bubble meaningful errors; avoid blanket try/catch. Web UI should surface loading and error states explicitly.
- Public upstream API URLs should come from Spring configuration (`application.properties`) rather than being hardcoded in adapters.

## Backend — Dev & Build
- Run dev: `cd backend && mvn spring-boot:run` (Spring Boot on `http://localhost:8080`)
- Build: `cd backend && mvn package` (emits `target/`)
- Docker: `docker build -t nord-alert-backend backend && docker run -p 8080:8080 nord-alert-backend`
- Lambda image: `docker build -f Dockerfile.lambda -t nord-alert-backend-lambda backend && docker run -p 9000:8080 nord-alert-backend-lambda`

## Web — Dev & Checks
- Setup: `cd web && npm install`
- Run dev server: `cd web && npm start`
- Build: `cd web && npm run build`
- Backend URL: set `REACT_APP_BACKEND_BASE_URL=http://<host>:8080`

## Terraform — IaC
- Validate: `terraform fmt -check -recursive`
- Init/validate (no backend): `terraform init -backend=false -input=false && terraform validate`
- Lint: `tflint --init && tflint -f compact --recursive`
- Security: `tfsec .`
- Plan/apply: `terraform plan && terraform apply` (use a remote backend for idempotent CI applies)

## Security — Prevent OWASP Top 10 Issues
- Treat OWASP Top 10 prevention as a default engineering requirement for every change, not a later review step.
- Validate and normalize all backend inputs at controller boundaries. Reject malformed, oversized, or unexpected input early and explicitly.
- Enforce authorization on every state-changing or data-revealing endpoint. Do not rely on client-side checks for access control.
- Never build queries, paths, commands, HTML, or JSON fragments through unsafe string concatenation when untrusted input is involved.
- Escape or sanitize untrusted content before rendering it into HTML, emails, logs, or generated documents. Prefer safe framework APIs over manual string assembly.
- Keep secrets, tokens, API keys, and credentials out of source, logs, test fixtures, Terraform variables files, and client bundles. Use environment variables and GitHub Secrets.
- Do not expose internal stack traces, raw exception messages, infrastructure identifiers, or sensitive config values in API responses.
- Prefer maintained dependencies and pinned versions. When updating dependencies, consider whether the change addresses known security issues or introduces risky transitive packages.
- Use secure defaults in Terraform and AWS resources: least-privilege IAM, encryption at rest, restricted public access, explicit logging, and narrowly scoped network/resource policies.
- For web and mobile clients, assume all client input is attacker-controlled. Do not trust hidden fields, local state, query params, or device-side filtering for security decisions.
- Avoid insecure deserialization and unsafe dynamic evaluation. Do not introduce reflection-based object loading, script execution, or parsing of untrusted serialized objects without a strong reason and explicit validation.
- Add or update tests for security-sensitive behavior when touching validation, authentication, authorization, serialization, HTML rendering, configuration, or infrastructure policies.
- If a task touches auth, input parsing, HTML/email rendering, secrets, or public infrastructure exposure, call out the security impact explicitly in the final summary.

## Mandatory Validation (Before Finishing a Task)
Perform the checks relevant to the files you changed:

Backend (Java)
- `cd backend && mvn package`
- Run Sonar analysis for backend code changes via the configured `sonarqube` MCP server when the project is available there.
- If `backend/Dockerfile` changed: `docker build backend` and verify `/alerts` responds from `docker run -p 8080:8080 <image>`.
- If `backend/Dockerfile.lambda` changed: `docker build -f backend/Dockerfile.lambda backend` and verify the Lambda container starts locally with `docker run -p 9000:8080 <image>` and a Lambda invoke request.

Web (React)
- `cd web && npm install`
- `cd web && npm run build`

Terraform
- `cd terraform && terraform fmt -check -recursive`
- `terraform init -backend=false -input=false && terraform validate`
- `tflint --init && tflint -f compact --recursive`
- `tfsec .`

## Branching & PRs
- Branch from `main` (do not commit directly to `main`).
- Use Conventional Commit prefixes where possible (`feat:`, `fix:`, `chore:`) and reference issues (e.g., `#23`).
- Open a PR with: summary, scope, verification steps, expected deployment impact, and screenshots/logs when helpful.
- Merge only after CI passes (backend container build, Terraform checks, tfsec, tflint, web build).

## Secrets & Configuration
- Never commit secrets. Use GitHub Secrets for CI/CD (see README for required secrets: Docker Hub, AWS credentials). Default AWS region is Stockholm (`eu-north-1`) unless CI overrides it.
- Backend: `PORT` defaults to 8080 for local Spring Boot execution; the Lambda container runtime also listens on port 8080 locally.
- Web: Provide `REACT_APP_BACKEND_BASE_URL` when building the dashboard.
- Terraform: Prefer a remote backend (e.g., Terraform Cloud) to keep state stable and idempotent between runs. The frontend is deployed to S3 behind CloudFront.

## Reviewer/Agent Checklist
- Changes are minimal and focused; no stray file churn.
- Code aligns with module boundaries and naming conventions.
- All relevant validation steps above are green.
- Sonar findings were checked for backend code changes, or the lack of Sonar access was noted.
- Docs updated if behavior or interfaces changed.
## Planning & Approvals
- Before making any changes, present a concise plan and a short TODO list that outlines the intended steps.
- Do not execute the plan until the user explicitly approves it. If unclear, ask for confirmation.
- After approval, follow the agreed plan; if scope changes, submit an updated plan for re-approval.
- Keep TODO items small (5–7 words each) and logically ordered; mark progress as steps complete.
