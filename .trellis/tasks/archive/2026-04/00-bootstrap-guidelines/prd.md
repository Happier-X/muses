# Bootstrap Task: Fill Project Development Guidelines

**You (the AI) are running this task. The developer does not read this file.**

The developer just ran `trellis init` on this project for the first time.
`.trellis/` now exists with empty spec scaffolding, and this task has been
set as their current task. They'll open their AI tool, run `/trellis:continue`,
and you'll land here.

**Your job**: help them populate `.trellis/spec/` with the team's real
coding conventions. Every future AI session — this project's
`trellis-implement` and `trellis-check` sub-agents — auto-loads spec files
listed in per-task jsonl manifests. Empty spec = sub-agents write generic
code. Real spec = sub-agents match the team's actual patterns.

Don't dump instructions. Open with a short greeting, figure out if the repo
has any existing convention docs (CLAUDE.md, .cursorrules, etc.), and drive
the rest conversationally.

---

## Status (update the checkboxes as you complete each item)

- [x] Fill Android guidelines (adapted from backend template)
- [x] Add code examples

---

## Spec files populated (Android/Kotlin/Compose)

| File | Status |
|------|--------|
| `.trellis/spec/backend/index.md` | Rewritten for Android/Kotlin/Compose |
| `.trellis/spec/backend/directory-structure.md` | Android project layout, package conventions |
| `.trellis/spec/backend/compose-conventions.md` | Compose patterns, state, theming, modifiers |
| `.trellis/spec/backend/error-handling.md` | Sealed results, UI error states, coroutine handling |
| `.trellis/spec/backend/quality-guidelines.md` | Testing, lint, forbidden patterns, review checklist |

Removed: `database-guidelines.md`, `logging-guidelines.md` (not applicable to Android app)


### Thinking guides (already populated)

`.trellis/spec/guides/` contains general thinking guides pre-filled with
best practices.

---

## Completion

Bootstrap complete. Spec files populated with Android/Kotlin/Compose conventions
based on the actual codebase (Material3 theme, Compose scaffold, version catalog).

To finalize:

```bash
python ./.trellis/scripts/task.py finish
python ./.trellis/scripts/task.py archive 00-bootstrap-guidelines
```
