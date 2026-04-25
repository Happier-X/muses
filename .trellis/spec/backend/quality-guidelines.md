# Quality Guidelines

> Code quality standards for this Android/Kotlin project.

---

## Testing

### Unit Tests (`app/src/test/`)

- Pure Kotlin/Java tests, no Android framework dependency
- Test ViewModels, repositories, utility functions
- Use JUnit 4 (`libs.junit`)

```kotlin
// Existing scaffold: ExampleUnitTest.kt
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }
}
```

### Instrumentation Tests (`app/src/androidTest/`)

- Compose UI tests: `libs.androidx.compose.ui.test.junit4`
- Espresso: `libs.androidx.espresso.core`
- Run on device or emulator

### Minimum coverage per change:
- ViewModels: unit test for each state transition
- Repositories: integration test for primary data path
- Composables: UI test for happy path + error state (instrumented)

---

## Code Style

### Kotlin conventions:
- Follow [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html)
- 4-space indentation (no tabs)
- Trailing commas for multi-line parameter lists
- Omit semicolons
- Prefer `val` over `var`
- Use expression bodies for simple functions: `fun isPlaying() = state is Ready`

### Compose-specific:
- One composable per file for screens and large components
- Small helper composables can share a file with their parent
- Keep composable bodies readable — extract complex lambdas to named functions
- Use `@Composable` annotation on every composable function, no exceptions

---

## Forbidden Patterns

| Pattern | Why forbidden | Alternative |
|---------|---------------|-------------|
| `!!` (non-null assertion) | Crashes on null | `?.let {}`, null check with early return, or proper nullable types |
| `mutableStateListOf` for large datasets | Triggers recomposition of entire list | `SnapshotStateList` or immutable list + key-based updates |
| Global mutable state (`object MyState { var x = ... }`) | Untestable, unpredictable | ViewModel + StateFlow |
| Calling suspend functions directly in composables | Blocks UI thread | `LaunchedEffect` or `produceState` |
| `Modifier.clickable` without accessibility | Inaccessible to screen readers | Use `Modifier.clickable` + `semantics {}`, or use Material `IconButton`/`Button` |
| Hardcoded strings in UI code | Not translatable | `stringResource(R.string.xxx)` |
| `System.currentTimeMillis()` for timing | Unstable for tests | DI a clock abstraction if testing is needed |

---

## Code Review Checklist

Before marking a PR as ready:

- [ ] All new composables have `@Preview` with sample data
- [ ] State changes go through ViewModel, not directly in composable bodies
- [ ] Error states are handled (loading, empty, error, success)
- [ ] No `!!` or empty catch blocks
- [ ] New dependencies added to `libs.versions.toml`, not hardcoded in `build.gradle.kts`
- [ ] Strings use `stringResource()`, not hardcoded
- [ ] Modifier parameter accepted and applied as outermost modifier in reusable composables
- [ ] Tests cover: primary success path + at least one error/edge case
- [ ] No debug `TODO()` or commented-out code committed

---

## Build Quality Gates

Run before committing:
```bash
# Type check
./gradlew compileDebugKotlin

# Lint
./gradlew lintDebug

# Unit tests
./gradlew testDebug

# Instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest
```
