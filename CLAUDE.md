# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

**Universidade do Servidor** — an offline Android training app (Jetpack Compose) for civil servants of the Prefeitura de São José dos Campos. Single Gradle module (`:app`), Portuguese-language domain (cursos / módulos / aulas / quiz). UI strings and all domain names are in Portuguese — keep new code consistent with that vocabulary.

## Commands

Use the Gradle wrapper (`./gradlew` on bash, `.\gradlew.bat` on PowerShell).

- Build debug APK: `./gradlew assembleDebug`
- Install on connected device/emulator: `./gradlew installDebug`
- Unit tests (JVM): `./gradlew test` — or a single test: `./gradlew test --tests "com.sgaf.universidadedoservidor.SomeTest.someMethod"`
- Instrumented tests (need a device/emulator): `./gradlew connectedAndroidTest`
- Force clean dependency re-download (see toolchain notes below): `./gradlew help --refresh-dependencies`

There is currently no real test suite beyond the generated stubs; most verification is done by running the app.

## Toolchain — read before touching the build

This project deliberately runs on a **bleeding-edge toolchain: AGP 9.2.1 + Gradle 9.x + Kotlin 2.2.10 + KSP2**. Many "obvious" build configs break on AGP 9. Before changing `build.gradle.kts`, `libs.versions.toml`, `settings.gradle.kts`, or `gradle.properties`, consult **`docs/guia_compatibilidade_agp9.md`** — it documents the exact, hard-won fixes. Key constraints already in place that must not regress:

- **Versions are pinned for compatibility, not freshness.** Do not bump blindly. Compose BOM is held at `2025.02.00` (newer BOMs cause `ComposableFunction0` binary errors with Kotlin 2.2.x); Hilt is `2.59.2`+ (older Hilt fails with `BaseExtension not found`); Room is `2.8.4`+ (older Room fails KSP2 with `unexpected jvm signature V`).
- **No manual `kotlin-android` plugin and no `kotlinOptions {}` block.** AGP 9 has Kotlin built in. JVM target is set via the top-level `kotlin { compilerOptions { jvmTarget.set(JVM_17) } }` block.
- `android.disallowKotlinSourceSets=false` in `gradle.properties` is a required escape hatch so KSP/Hilt can register generated sources.
- All dependencies go through the version catalog `gradle/libs.versions.toml` (referenced as `libs.*`), never hardcoded.

## Architecture

Clean Architecture + MVVM, packages under `com.sgaf.universidadedoservidor`:

- `domain/` — pure Kotlin models (`Curso`, `Modulo`, `Aula`, `QuizPergunta`), the `CursoRepository` interface, and `usecase/` classes that wrap single repository operations.
- `data/` — Room implementation: `local/entities`, `local/dao`, `local/database/AppDatabase`, and `repository/CursoRepositoryImpl` (binds to the domain interface).
- `ui/` — Compose: `screens/<feature>/` each with a `*Screen.kt` + `*ViewModel.kt` (Hilt `@HiltViewModel`, expose `StateFlow`), plus `theme/`, `components/`, and `navigation/`.
- `di/AppModule.kt` — Hilt `@Module` providing the database, DAOs, a singleton app-scoped `CoroutineScope`, and binding the repository.
- `core/` — early scaffolding for a future feature-modular split (`core/domain`, `core/data`, `core/navigation`) plus an anonymous `AuthRepository`. Not all of it is wired up yet.

`UniversidadeApplication` is the `@HiltAndroidApp` entry point; `MainActivity` is `@AndroidEntryPoint` and hosts the whole app in a single Compose `NavHost`.

### Data flow — the important part

Course **metadata** and course **content** are stored separately, then merged into one Room DB:

1. `app/src/main/assets/curso_data.json` holds the catalog (cursos → módulos → aulas, plus quizzes). Each aula references its lesson body either inline (`conteudo`) or, more commonly, by a `contentPath` pointing at a Markdown file under `app/src/main/assets/cursos/<curso>/<modulo>/<aula>.md`.
2. **Seeding happens once**, in `AppDatabase.DatabaseCallback.onCreate`: the JSON is parsed, each aula's `.md` file is read from assets, and the resolved text is baked into `AulaEntity.conteudo`. Quizzes are serialized to a JSON string column (`AulaEntity.quizJson`) and parsed back in the repository.
3. `CursoRepositoryImpl` reconstructs the domain graph by `combine`-ing the curso/módulo/aula DAOs with the `ProgressoDao` flows — progress (completed/favorite) lives in a separate `ProgressoEntity` keyed by `aulaId` and is overlaid onto each `Aula` reactively.

**Gotcha:** because seeding only runs on DB *creation*, editing `curso_data.json` or any `.md` will NOT show up in a running install. The DB uses `fallbackToDestructiveMigration()` and is named `universidade_database_v3`. To re-seed: bump the `@Database(version = ...)` (and/or `DATABASE_NAME`), or clear app data / reinstall.

### Navigation

Type-safe Navigation Compose. Routes are `@Serializable` objects/data classes in `ui/navigation/Destinations.kt` (`Splash`, `Home`, `Cursos`, `CursoDetail(cursoId)`, `Aula(aulaId)`); the graph is defined inline in `MainActivity.AppNavigation` using `composable<Route>`. ViewModels read their route args via `SavedStateHandle`.

### Markdown rendering

Lesson bodies are rendered with the `com.mikepenz:multiplatform-markdown-renderer-m3` library (Material 3 flavor), not raw text.

## Conventions

- Quiz completion gates progress: a lesson is marked concluded (`MarcarConclusaoUseCase`) only when the user answers its quiz correctly. `VerificarAprovacaoCursoUseCase` checks course-level approval.
- Theming follows the São José dos Campos municipal identity — official blue (`#003882`) and gold (`#FFD700`); see `ui/theme/Color.kt`. The animated splash gear logo is hand-drawn with Compose `Canvas` in `ui/components/SjcLogo.kt`.
- `SearchScreen` and `AvaliacaoScreen` (Likert module evaluation, `AvaliacaoEntity`/`AvaliacaoDao`) exist as forward-looking scaffolding and may not be fully wired into navigation.
