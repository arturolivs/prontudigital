---
name: readme-generator
description: >
  Generates a comprehensive, professional README.md for any code repository by analyzing
  its structure, technologies, and purpose. Use this skill whenever the user asks to
  "generate a README", "create documentation for my project", "write a README for this repo",
  "document this codebase", or uploads/references a project folder and wants an overview.
  Also trigger when the user says things like "analyze my project and document it",
  "make a README for this", or "I need docs for my repo". Even if they don't say README
  explicitly, trigger this skill whenever someone wants project documentation generated
  from a codebase.
---

# README Generator Skill

Generate a detailed, professional README.md by deeply analyzing a repository's structure,
source code, configuration files, and commit history.

---

## Workflow

### Step 1 — Locate the repository

Identify where the repository is:
- If the user uploaded files, check `/mnt/user-data/uploads/`
- If the user gave a local path, use that directly
- If they gave a GitHub URL, clone it first:
  ```bash
  git clone <url> /tmp/repo && cd /tmp/repo
  ```

### Step 2 — Collect information (run all in parallel where possible)

Use bash and file-reading tools aggressively. Don't ask the user — go find it yourself.

```bash
# Project structure (full tree)
find . -not -path '*/node_modules/*' -not -path '*/.git/*' \
       -not -path '*/__pycache__/*' -not -path '*/.venv/*' \
       -not -path '*/dist/*' -not -path '*/build/*' \
  | sort | head -200

# Package / dependency files — read ALL of these that exist
cat package.json 2>/dev/null
cat requirements.txt 2>/dev/null
cat pyproject.toml 2>/dev/null
cat Cargo.toml 2>/dev/null
cat go.mod 2>/dev/null
cat pom.xml 2>/dev/null
cat build.gradle 2>/dev/null
cat composer.json 2>/dev/null
cat Gemfile 2>/dev/null

# Config and environment
cat .env.example 2>/dev/null || cat .env.sample 2>/dev/null
cat docker-compose.yml 2>/dev/null || cat docker-compose.yaml 2>/dev/null
cat Dockerfile 2>/dev/null
cat .github/workflows/*.yml 2>/dev/null | head -200

# Entry points and main files
cat main.py 2>/dev/null || cat app.py 2>/dev/null || cat index.js 2>/dev/null \
  || cat src/main.ts 2>/dev/null || cat src/index.ts 2>/dev/null \
  || cat src/main.rs 2>/dev/null || cat main.go 2>/dev/null | head -100

# Git history (last 30 commits)
git log --oneline -30 2>/dev/null

# Existing README (for reference, don't copy it)
cat README.md 2>/dev/null | head -50
```

Read the 3–5 most important source files to understand what the project actually does.
Use judgment: for a web app, read the main router/controller; for a library, read the
public API; for a CLI, read the command definitions.

### Step 3 — Identify technologies

Build a clear picture of:
- **Language(s)**: primary + secondary
- **Frameworks**: web framework, ORM, test framework, etc.
- **Infrastructure**: Docker, CI/CD, cloud provider, DB
- **Dev tooling**: linter, formatter, bundler, package manager
- **Notable libraries**: key dependencies worth mentioning

### Step 4 — Write the README.md

Output the file to `/mnt/user-data/outputs/README.md`.

Follow the structure in [references/structure.md](references/structure.md) exactly,
including all required sections and emoji headers. Adapt optional sections
(flowchart, changelog) based on what data is available.

**Writing standards:**
- Be specific — name actual files, commands, env vars, endpoints
- Don't pad with generic filler ("This project aims to...")
- Code blocks for every command, config snippet, and example
- Badges at the top when you can infer them (language, license, CI)
- Mermaid diagrams for architecture/flow when the project is non-trivial
- Keep tone professional but approachable

### Step 5 — Present the file

Call `present_files` with the output path so the user can download it.
Then give a 2–3 sentence summary of what you found and any gaps you couldn't fill
(e.g., "I couldn't find env var docs — you may want to expand the Configuration section").

---

## Edge cases

| Situation | How to handle |
|-----------|---------------|
| No git history | Skip the Changelog section entirely |
| Monorepo | Add a "Packages / Services" section listing each sub-project |
| Private dependencies | Note them but don't fabricate installation steps |
| Minimal codebase (< 5 files) | Still complete all sections; mark optional ones as N/A |
| Non-English codebase/comments | Write the README in the user's language or English if unclear |
| Existing README | Use it as a reference but rewrite from scratch — don't copy |

---

## Quality checklist (self-review before saving)

- [ ] Every install command is real and tested against the actual package manager found
- [ ] All env vars listed actually appear in .env.example or source code
- [ ] Tech list matches actual dependencies, not guesses
- [ ] Structure tree reflects the actual repo (not fabricated)
- [ ] Mermaid diagram (if present) renders valid syntax
- [ ] No placeholder text like "TODO" or "[your description here]"