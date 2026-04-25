# Android Development Guidelines

> Best practices for Android development with Kotlin and Jetpack Compose.

---

## Overview

This project is a **native Android application** built with:
- **Kotlin** (2.2.10)
- **Jetpack Compose** (BOM 2026.02.01) with **Material3**
- **Gradle** (AGP 9.2.0) with version catalog (`gradle/libs.versions.toml`)
- **Min SDK**: 24, **Target SDK**: 36

---

## Guidelines Index

| Guide | Description | Status |
|-------|-------------|--------|
| [Directory Structure](./directory-structure.md) | Module and package organization | Filled |
| [Compose Conventions](./compose-conventions.md) | Composable patterns, state, theming | Filled |
| [Error Handling](./error-handling.md) | Kotlin error types, sealed results, UI states | Filled |
| [Quality Guidelines](./quality-guidelines.md) | Lint, testing, forbidden patterns | Filled |

---

## How to Fill These Guidelines

For each guideline file:

1. Document the project's **actual conventions** (not ideals)
2. Include **code examples** from the codebase (`app/src/`)
3. List **forbidden patterns** and why
4. Add **common mistakes** to avoid

---

**Language**: All documentation should be written in **English**. Code comments may be in Chinese.
