AI Development Quick Reference
⭐️ Golden Rules
Task Focus: Only modify files directly related to the current task. No extra changes.

Context First: Read all relevant docs (README.md, etc.) before coding.

Explore: Start every task with ls to understand the project structure.

Read-Only Git: NEVER use git add, commit, or push. Only use read-only commands like status, diff, and log.

Validate Everything: You MUST run validation checks and fix all errors before completing any task. See the Validation section below.

🏛️ Architecture & State Management
State Philosophy: Timestamps, Not Flags
The core principle is to derive state from timestamped events, not mutable flags.

❌ NEVER USE: Mutable flags like is_speaking = true. They get out of sync.

✅ ALWAYS USE: Record the timestamp of an event (e.g., last_spoke_timestamp). Calculate the current state from that timestamp and a duration.

Single Source of Truth: Each piece of state must have one and only one authoritative source.

Avoid: Global mutable variables, duplicated state, and state-flipping logic.

Configuration: Use manager classes (ConfigManager). NEVER assign os.getenv() to global variables; call it where needed.

Code Philosophy
Simplicity Over Abstraction: If only one implementation exists, remove the abstraction layer.

Refactoring: When removing a feature, trace it from configuration to initialization and usage. Remove everything related: imports, configs, docs, and unused variables. Use git diff to verify.

✍️ Code Style
Optional Parameter Usage (CRITICAL)
Abuse of Optional is forbidden. Use it only for its intended purpose.

✅ When to use Optional:
External API Calls: When a network or service can fail (e.g., -> Optional[UserData]).

Truly Optional Behavior: When a feature can legitimately be disabled (e.g., logger: Optional[Logger] = None).

❌ When NOT to use Optional:
Core Business Logic: add_numbers(a: int, b: int) -> int should never have Optional parameters.

Required Configuration: If you check if param is None: raise ValueError, the parameter should not be Optional. Make it required.

Python
Imports: Standard lib -> third-party -> local.

Naming: snake_case for functions/variables, PascalCase for classes.

Types: Use type hints.

Logging: Use the configured logger from logging_config.py.

Error Handling
Production Code: Use specific try/except blocks with logging.

Development Tools/Scripts: Let errors fail loudly. DO NOT use try/except.

✅ MANDATORY VALIDATION
You MUST run these checks and fix ALL errors before completing a task. NO EXCEPTIONS.

1. Web Projects (.ts, .tsx, .js, .jsx)
   Next.js:

Bash

npm run build
Other JS/React:

Bash

npm run lint 2. Python (.py)
Run the full validation suite. This includes ruff, mypy, vulture, and pyflakes.

Bash

# Run all checks: format, lint, type check, dead code

.venv/bin/python -m ruff format .
.venv/bin/python -m ruff check --fix .
.venv/bin/python -m mypy --strict .
.venv/bin/python -m vulture . --min-confidence 99 3. Terraform (.tf)
Initialize, format, and validate.

Bash

terraform -chdir=terraform init -backend=false && terraform -chdir=terraform fmt -recursive && terraform -chdir=terraform validate
🚀 Deployment & Infrastructure
NEVER deploy manually. All deployments are through GitHub Actions.

Workspaces map to environments: main -> production, dev -> development.

Credentials come from GitHub Secrets, not .tfvars files.
