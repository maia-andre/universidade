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

- `domain/` — pure Kotlin models (`Curso`, `Modulo`, `Aula`, `QuizPergunta`, `ResultadoProvaFinal`), the repository interfaces (`CursoRepository`, `FerramentaRepository`, `ProvaFinalRepository`), and `usecase/` classes that wrap single repository operations.
- `data/` — Room implementation: `local/entities`, `local/dao`, `local/database/AppDatabase` (+ `Migrations.kt`), and `repository/*Impl` (bind to the domain interfaces).
- `ui/` — Compose: `screens/<feature>/` each with a `*Screen.kt` + `*ViewModel.kt` (Hilt `@HiltViewModel`, expose `StateFlow`), plus `theme/`, `components/`, and `navigation/`. Features: `splash`, `home`, `cursos`, `curso_detail`, `aula`, `provafinal`, `certificado`, `avaliacao`, `desempenho`, `ferramentas`, `search`, `settings`, `acessibilidade`.
- `di/` — Hilt modules: `AppModule` (database, DAOs, app-scoped `CoroutineScope`, repository bindings) and `PreferencesModule` (DataStore).
- `core/` — shared scaffolding: `data/preferences/UserPreferencesRepository` (DataStore: curso ativo, tema, fontScale, acessibilidade), `util/CertificadoPdfGenerator` + `util/FerramentaPdfGenerator` (PDF via Canvas + FileProvider), `components/StateViews`, `utils/Constants`, and `domain/repository/AuthRepository` + `data/repository/AnonymousAuthRepositoryImpl` (preparation for the V6 auth phase — still anonymous/mock, not wired to a backend).

`UniversidadeApplication` is the `@HiltAndroidApp` entry point; `MainActivity` is `@AndroidEntryPoint` and hosts the whole app in a single Compose `NavHost`.

### Data flow — the important part

Course **metadata** and course **content** are stored separately, then merged into one Room DB:

1. `app/src/main/assets/curso_data.json` holds the catalog (cursos → módulos → aulas, plus quizzes). Each aula references its lesson body either inline (`conteudo`) or, more commonly, by a `contentPath` pointing at a Markdown file under `app/src/main/assets/cursos/<curso>/<modulo>/<aula>.md`.
2. **Seeding happens once**, in `AppDatabase.DatabaseCallback.onCreate`: the JSON is parsed, each aula's `.md` file is read from assets, and the resolved text is baked into `AulaEntity.conteudo`. Quizzes are serialized to a JSON string column (`AulaEntity.quizJson`) and parsed back in the repository.
3. `CursoRepositoryImpl` reconstructs the domain graph by `combine`-ing the curso/módulo/aula DAOs with the `ProgressoDao` flows — progress (completed/favorite) lives in a separate `ProgressoEntity` keyed by `aulaId` and is overlaid onto each `Aula` reactively.

**Gotcha:** seeding runs only on DB *creation*, so editing `curso_data.json` or any `.md` will NOT show up in a running install. Since V3 the DB uses **versioned migrations** (`addMigrations(*ALL_MIGRATIONS)` from `Migrations.kt`, with `fallbackToDestructiveMigrationOnDowngrade` only) — **never destructive on upgrade**, to preserve beta users' progress. It is `@Database(version = 7)` (the file is still named `universidade_database_v3`; the name has not changed since V3, versioning is now by `@Database` version + migrations). Schemas `3..7.json` are exported under `app/schemas/`. **Bumping the version REQUIRES a matching `Migration` (v7→v8…) or the app crashes** — see `docs/backlog_v3.md` for the migration discipline. To re-seed content during dev: clear app data / reinstall. **Pattern for new content that must reach already-installed apps:** read it from assets at *runtime* rather than baking it into the DB at seed time — e.g. prova final questions live in `curso_data.json` (`provaFinal`) and are read live, so they arrive without a re-seed.

### Navigation

Type-safe Navigation Compose. Routes are `@Serializable` objects/data classes in `ui/navigation/Destinations.kt`: `Splash`, `Home`, `Cursos`, `CursoDetail(cursoId)`, `Aula(aulaId)`, `ProvaFinal(cursoId)`, `Certificado(cursoId)`, `Avaliacao(cursoId)`, `Desempenho`, `Ferramentas`, `FerramentaEditor(tipo, ferramentaId)`, `Busca`, `Configuracoes`, `Acessibilidade`. The graph is defined inline in `MainActivity.AppNavigation` using `composable<Route>`. ViewModels read their route args via `SavedStateHandle`.

### Markdown rendering

Lesson bodies are rendered with the `com.mikepenz:multiplatform-markdown-renderer-m3` library (Material 3 flavor), not raw text.

## Conventions

- **Two gates:** a lesson is concluded (`MarcarConclusaoUseCase`) only when its quiz is answered correctly; the **certificate** is gated by the course **prova final** — `VerificarAprovacaoCursoUseCase` requires 100% of lessons done **and** a prova final score ≥ 70% (`Constants.MINIMUM_PASSING_SCORE`). Flow: all lessons → `ProvaFinalScreen` → approved → `CertificadoScreen`.
- **Theming:** São José dos Campos municipal identity — official blue (`#003882`) and gold (`#FFD700`); see `ui/theme/Color.kt`. Three schemes are resolved in `MainActivity` (light / dark / high-contrast). **Screens must take card/surface colors from `MaterialTheme.colorScheme`, never `isSystemInDarkTheme()`** — that was the V5 theme-legibility bug (it reads the *device* theme, not the in-app `ThemeMode`). The animated splash gear logo is hand-drawn with Compose `Canvas` in `ui/components/SjcLogo.kt`.
- **Accessibility** is centralized in the `acessibilidade` screen + DataStore: global font scale (`fontScale` applied via `LocalDensity` in `MainActivity`), high-contrast scheme, reduce-motion (disables screen transitions + splash gear), and colorblind-safe quiz/prova feedback (✓/✗ icons, not color alone). Respect these when adding UI.
- **Local preferences** (curso ativo, tema, fontScale, acessibilidade) live in `UserPreferencesRepository` (DataStore), separate from the Room content DB. "Curso ativo" is currently heuristic (entering a course makes it active) — slated to become profile/enrollment-driven in V6.
- `SearchScreen` (wired: `SearchDao`, LIKE-based global search) and `AvaliacaoScreen` (Likert course evaluation, `AvaliacaoEntity`/`AvaliacaoDao`) are live features as of V3/V4, reachable from navigation.
