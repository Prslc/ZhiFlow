# ZhiFlow

A third-party Zhihu (知乎) client for Android, built with Kotlin, Jetpack Compose, and OkHttp. Under active development.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 + Navigation Compose (type-safe, serializable routes) |
| DI | Koin (`koin-android`, `koin-compose-viewmodel`) |
| HTTP | OkHttp 5 with custom interceptor for Zhihu API signing |
| Serialization | kotlinx-serialization-json (used for both DTOs and Navigation routes) |
| Image Loading | Coil 3 (OkHttp network fetcher, GIF support, disk + memory cache) |
| Zoom | Telephoto (`zoomable-image-coil3`) |
| LaTeX | Rendered as pre-rasterized images served by the Zhihu API (`img_url` + dp `width`/`height`), loaded via Coil |
| Arch | MVVM — ViewModels expose `mutableStateOf` UI state, one per screen |
| Min SDK | 33 (Android 13) |
| Target/Compile SDK | compileSdk 37, targetSdk 36 |
| Java | 21 |
| Kotlin | 2.4.10 |
| AGP | 9.2.1 |

## Build System

- **Gradle wrapper**: `./gradlew`
- **Version catalog**: `gradle/libs.versions.toml` — all dependency versions live here; use `libs.xxx.yyy` accessors
- **Root `build.gradle.kts`**: only declares plugins with `apply false`
- **App `build.gradle.kts`**: applies plugins, configures SDK versions, NDK ABI filter (`arm64-v8a`), release R8+shrink, Compose build feature, source sets
- **`gradle.properties`**: `useAndroidX=true`, `nonTransitiveRClass=true`, `R8.fullMode=true`, `kotlin.code.style=official`
- **Release**: R8 minification + resource shrink + multiDex; strips `kotlin.jvm.internal.Intrinsics` via ProGuard `-assumenosideeffects`
- **Native library**: loads `libencrypt.so` from `app/src/main/libs/` (JNI for `x-zse-96` request signing)
- **`local.properties`**: stores per-machine SDK path; gitignored

Key Gradle tasks:
```bash
./gradlew assembleDebug        # build debug APK
./gradlew assembleRelease      # build release APK (R8 + shrink)
./gradlew lint                 # run lint checks (includes compose-lint)
./gradlew test                 # run unit tests
```

**Important**: Do not run `./gradlew` build commands. The user compiles manually and reports results.

## Project Structure

```
ZhiFlow/
├── app/
│   ├── build.gradle.kts                        # App module build config
│   ├── proguard-rules.pro                      # R8 rules
│   ├── libs/                                   # JNI .so files (arm64-v8a)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── assets/emoji/default/               # Bundled emoji webp images (emoji_1..emoji_58, emoji_tab_icon)
│       ├── res/                                # Drawables, mipmap launcher icons, strings (en + zh-CN), XML config
│       └── java/com/prslc/zhiflow/
│           ├── Application.kt                  # App class: Koin init + Coil ImageLoader factory
│           ├── MainActivity.kt                 # Single Activity: NavHost + bottom-bar HorizontalPager
│           ├── core/                           # Infrastructure that knows nothing about Zhihu's domain
│           │   ├── exception/                  # ApiException, ErrorHandler, Result extensions (onApiFailure / ignoreOutcome)
│           │   ├── native/                     # JNI bridge for request signing
│           │   ├── network/                    # OkHttp client, auth headers, signing, the HTTP log
│           │   └── utils/                      # Format and JSON helpers; compose/ holds lazy-list and text-layout extensions; platform/ clipboard and images
│           ├── data/                           # Everything shaped by Zhihu's data
│           │   ├── dto/                        # Flat, UI-ready data classes
│           │   ├── mapper/                     # model → dto extensions
│           │   ├── model/                      # Raw API response models, one subpackage per domain (content, comment, feed, moment, user)
│           │   ├── remote/
│           │   │   ├── parser/                 # Segments → RichTextElement: links, formulas, tables, emoji (emoji/), the annotated-string engine (engine/)
│           │   │   └── service/                # One OkHttp service per API domain
│           │   ├── repository/                 # Service + mapper per domain, returning Result<T>
│           │   └── session/                    # The current user's hash id, resolved lazily and kept in memory
│           ├── di/                             # The single Koin module
│           └── ui/                             # Compose only
│               ├── theme/                      # Colour, type, dynamic-colour fallback
│               ├── navigation/                 # Type-safe routes, the NavHost graph, the Navigator
│               ├── component/                  # Reusable pieces no page owns
│               │   ├── common/                 # Error and empty states, paging footer, author row
│               │   ├── preference/             # Settings-screen widgets
│               │   ├── richtext/               # Rich text dispatch and rendering, with component/ holding one composable per element kind
│               │   └── widget/                 # Bottom bar, sheets, dialogs, lightbox, reading progress bar
│               └── page/                       # One package per screen: its composables and its ViewModel
│                   ├── feed/  content/  question/  comment/
│                   ├── pin/                    # The thought page, with a screen of its own instead of the one answers and articles share
│                   ├── people/                 # Profile page; moment/ is its second tab
│                   └── profile/  history/  collection/  debug/
```

## Architecture Patterns

### Data Flow (bottom-up)

```
API Server
  └─ Service (OkHttp: safeApiCall<T> → Result<ZhihuResponse>)
       └─ Repository (maps raw model → DTO, returns Result<DomainResult>)
            └─ ViewModel (manages UI state via mutableStateOf, calls repo in viewModelScope.launch)
                 └─ Composable Screen (observes ViewModel state, calls ViewModel actions)
```

### Layer Conventions

1. **`data/model/`** — Raw API response models, `@Serializable`, with `@SerialName` mapping. These mirror the Zhihu JSON structure exactly. Never expose these to UI.

2. **`data/dto/`** — Flat, UI-ready data classes (no nested serialization). These are what ViewModels expose and Composables consume. Marked `@Immutable` where applicable.

3. **`data/mapper/`** — Extension functions that convert model → dto. Named `toDto()`. All are `internal`.

4. **`data/remote/service/`** — One class per API domain (FeedService, ContentService, UserService, etc.). Accept `OkHttpClient` via constructor. Use `safeApiCall<T>` extension for typed responses.

5. **`data/repository/`** — Wraps service calls, applies mappers, returns `Result<T>`. May define domain result classes (e.g. `FeedResult`).

6. **`data/remote/parser/`** — Transforms raw API segment lists into `RichTextElement` lists (Compose UI primitives). CPU-heavy; runs on `Dispatchers.Default`.

### Network Layer

- **`safeApiCall<T>(requestBuilder)`** — Extension on `OkHttpClient`. Executes on `Dispatchers.IO`, parses JSON via `kotlinx.serialization`, catches all exceptions and maps to `ApiException` sealed types. Returns `Result<T>`.
- **`safeExecute(requestBuilder)`** — Same, for writes whose response body is not needed; returns `Result<Unit>`. **Neither helper ever reports a non-2xx as a success** — both fail with an `ApiException` carrying the status code. Pick between them only by whether you need the parsed body.
- **`Response.body<T>()`** — `inline reified` extension that `use`-closes the response and decodes JSON. Throws `HttpStatusException` on non-2xx.
- Auth headers (Cookie, Authorization, x-udid, x-zse-96) are injected by an OkHttp interceptor reading from `SharedPreferences`.
- **`HttpClientProvider`** holds the OkHttpClient singleton and a shared `Json` instance (lenient, coerce defaults, ignore unknown keys).
- **`HeaderProvider`** is an `object` that initializes the dynamic User-Agent via `WebSettings` at app startup, and signs requests via JNI `Natives.zse96Sign()`.
- **HTTP log** — `HttpLogInterceptor` records API traffic into `HttpLogStore` (bounded, in-memory, newest first) for the in-app log screen. It records only requests to the `BASE_URL` host (Coil shares the same client), never records headers, and never writes to logcat. Failures are always recorded; the Debug-page toggle (prefs key `http_log_enabled`) additionally captures successful requests.

### DI (Koin)

Single module `appModule` in `di/AppModule.kt`. Uses DSL:
- `singleOf(::ClassName)` for services and repositories
- `viewModelOf(::ViewModelName)` for ViewModels
- `single { get<HttpClientProvider>().okHttpClient }` for the OkHttpClient instance
- `SharedPreferences` is provided as a `single` pointing to the `"temp_auth_prefs"` file
- `UserSession` is a `single` caching the current user's hash id in memory; it depends only on `UserService` and must never be made to depend on `UserRepository` (that would form a DI cycle)
- Koin is started in `Application.onCreate()` with `startKoin { ... }`

### Navigation

- **Type-safe routes**: `@Serializable` data classes/objects in `Route.kt`. Uses `navigation-compose` 2.9 type-safe API (`composable<RouteType>`, `toRoute()`).
- **`MainContainer`** is the start destination — it contains three tabs (Home, Debug, Profile) in a `HorizontalPager` with a `NavigationBar`.
- Detail screens (`AnswerDetail`, `ArticleDetail`, `PinDetail`, `QuestionDetail`, `PeopleDetail`, `Settings`, `ReadHistory`, `CollectionContents`, `HttpLog`) are separate composable destinations pushed onto the NavHost stack. `PinDetail` uses a dedicated `PinDetailScreen` (thought page); `AnswerDetail`/`ArticleDetail` share `ContentDetailScreen`.
- **`Navigator`** — Wraps `NavHostController` + `Context` + `UriHandler`. Exposed via `CompositionLocalProvider` as `LocalNavigator`. Handles URL→route resolution via `LinkParser`.
- **`LinkParser`** — Parses Zhihu URLs, resolves `link.zhihu.com` redirects, extracts content type + ID from path patterns, returns `LinkDestination.Internal(route)` or `LinkDestination.External(url)`.
- Transition animations: horizontal slide (detail push = full right→left, pop = reversed with 1/5 parallax).

### State Management

ViewModels expose state via Compose `mutableStateOf` properties with `private set`. Pattern:

```kotlin
var uiState by mutableStateOf(UiState())
    private set
```

- **Optimistic updates**: Vote toggling updates state immediately, rolls back on API failure.
- **Pagination**: ViewModels track `nextPageUrl`, expose `loadIfEmpty()`, `refresh()`, `loadMore()`.
- **Load state**: `isLoading`, `isRefreshing`, `isNextLoading`, `globalError`, `loadMoreError` — each ViewModel defines its own UI State data class nested inside the ViewModel class.
- **Chunked parsing**: `ContentViewModel` parses segments in chunks of 10 on `Dispatchers.Default`, emitting incremental state updates for progressive rendering. Results cached in an `LruCache<String, List<RichTextElement>>`.
- **Pin rendering**: `PinViewModel` handles the thought page separately. When a pin has no `structured_content` (image-only pins), it falls back to `image_list` to build `RichTextElement.Image` elements.

### Rich Text Rendering

The rich text pipeline:
1. API returns `List<Segment>` (paragraph, heading, blockquote, code_block, list_node, table, image, card, formula, etc.)
2. `ContentParser.transform(segments, isDark)` → `List<RichTextElement>` (sealed interface hierarchy of Compose-ready primitives)
3. Each `RichTextElement` renders via a corresponding composable in `ui/component/richtext/component/`
4. `ZRichText` composable wraps `Text` with clickable link interception and inline formula support via `InlineTextContent`

#### Formula rendering (image-based)

Formulas are **pre-rasterized images served by the Zhihu API**, not rendered locally:
- Each `Formula` carries `content`, `img_url`, and dp `width`/`height` (the PNG is 3x that). The API never omits these.
- `LatexComponent` (in `component/LatexComponent.kt`) loads the image via Coil `AsyncImage`. Inline and block formulas share the same component; block formulas are centered with no horizontal scroll.
- Sizing follows the values the API sends:
  - Image and `Placeholder` bounds use the exact server dp `width`/`height`, so formulas keep their natural size variation (simple subscripts ~13dp, display fractions up to ~56dp).
  - Widths are clamped to screen width minus 42dp (`constrainedSize`), scaling height proportionally — over-wide formulas are scaled down, never cropped or scrolled.
  - The dp→sp conversion divides by `fontScale` (`formulaPlaceholder`), so rendered pixels stay constant regardless of the user's system font size.
- `FormulaTextSection` (paragraph path) raises the paragraph `lineHeight` to the tallest inline formula so tall fractions don't overlap adjacent rows (Compose does not grow a row for a tall placeholder).
- Dark mode inverts the white-background bitmap via a `ColorMatrix`, not a tint.
- Why images instead of `latex-renderer`: the library measured + laid out each formula synchronously on the main thread inside `LatexDocument`'s `remember`, causing ~23% janky frames and up to 1s p99 while scrolling formula-heavy pages. Image rendering uses the API's pre-rendered bitmaps with zero measurement; Coil loads/decodes off the main thread. The cost is extra network traffic for the formula images.

### Error Handling

- `ApiException` is a sealed class: `NetworkException`, `UnAuthorizedException`, `NotFoundException`, `ServerException(code)`, `UnknownException`
- Each carries an Android string resource ID; `.uiMessage` is a `@Composable` extension property
- `ErrorView` and `LoadMoreErrorItem` composables in `component/common/` render error states with retry buttons

**Presenting a failed user action** — the shape follows the surface, not taste:

| Surface | Use | Why |
|---|---|---|
| Screen with a `Scaffold` | `rememberActionErrorHost(viewModel.actionError, viewModel::consumeActionError)`, then `Scaffold(snackbarHost = { SnackbarHost(state) })` | the host can carry it |
| `Dialog()` (`CollectionDialog`) | render `error.uiMessage` inside the dialog | it draws in its own window; the host's snackbar would be hidden behind it |
| App-level non-Composable (`Navigator`) | Toast | no `SnackbarHostState` is reachable, and it outlives the current screen |
| Reusable widget with no host parameter (`ImageLightbox`) | Toast inside the widget | cannot require every call site to supply a host |
| Surface with no `Scaffold` (comment bottom sheet) | `ErrorView` / `LoadMoreErrorItem` inside the surface | there is no host to use |

Do not pass a snackbar host down through a `CompositionLocal`: two of these rows have no host to reach at all.

## Key Conventions

- **All properties in API models have defaults** (empty strings, 0, null, empty lists) — never assume mandatory fields from the server.
- **Mappers are `internal`** — not exposed outside the `data.mapper` package.
- **Services use `Result<T>`** — never throw; all failures are caught and wrapped.
- **Handling a failed `Result`** — use `onApiFailure { error -> … }` (`core/exception/ResultExtensions.kt`), never `Result.onFailure` directly. It is the only place cancellation is filtered out of the failure path, and its callback always receives a non-null `ApiException`, so a failure cannot become a silent no-op. Never `runCatching` inside a coroutine — it swallows `CancellationException`.
- **Discarding a `Result`** — only via `ignoreOutcome()`, so that "we do not care about this one" stays greppable instead of looking like a forgotten check.
- **`@Immutable`/`@Stable`** annotations on data classes consumed by Compose for stability inference.
- **String resources** are in `res/values/strings.xml` (English) and `res/values-zh-rCN/strings.xml` (Chinese). Always reference via `R.string.*`, never hardcode user-facing strings.
- **Emoji**: Bundled as `.webp` assets in `assets/emoji/default/`; referenced by Zhihu emoji codes via `EmojiMap`.
- **Credentials**: Stored in `SharedPreferences` (`"temp_auth_prefs"`) — keys `auth`, `cookie`, `x_udid`. Managed via DebugScreen or programmatically. Changing them must call `UserSession.invalidate()`, since the cached current user id would otherwise go stale. DebugScreen's clear action removes only these three keys, never the whole prefs file (it also holds `http_log_enabled`).
- **Screen navigation**: Use `LocalNavigator.current` inside screens for item-click navigation (see `ReadHistoryScreen`, `CollectionContentsScreen`). Do NOT pass `onItemClick: (String, String) -> Unit` callbacks from NavGraph — the screen resolves its own navigation via `navigator.navigateToContent(id, type)`. This keeps NavGraph entries thin and avoids callback threading through multiple layers.

## Comments

Comments are decided by layer, not by taste. The contract layers are documented, the UI is mostly
silent, and a note earns its place only by naming something the code itself cannot say. The shape of
that, roughly, as the tree stands: `data/repository` and `data/session` run near 40% comments with
every public function documented, `data/remote` near 20%, `core` near 25%, `ui/` at 5–8%, and
`data/model`, `data/dto`, `data/mapper` effectively silent.

| Where | What |
|---|---|
| `data/repository`, `data/session`, `data/remote/service` | KDoc on **every public function**, with `@param` / `@return`. Multi-line is fine. |
| ViewModel public methods | KDoc: behaviour, side effects, boundaries — "Sets `[FeedUiState.globalError]` on failure", "No-op when `[nextPageUrl]` is null". |
| Other public API (`core/`, `ui/navigation/`) | KDoc where the contract is not readable off the signature. |
| UI composables | Nothing by default. A mechanism note goes on the KDoc of the host composable, where it explains the widget as a whole. |
| A composable in `ui/component/` | Carries a KDoc, because another screen calls it. The summary says what the widget is; every parameter gets its own line. |
| `data/model`, `data/dto`, `data/mapper` | Nothing. The types and the server's own field names say it. |

**The test.** Delete the note and ask whether the next reader will simplify the code and quietly break
it. If yes, keep it: it names a framework or interface fact the code cannot show. If it only explains
why we built something this way, delete it — that belongs in the commit message. What survives reads
like the fact and its consequence:

```kotlin
// M3's Button enforces a 58x40dp minimum on its inner content row, and an outer `height()` is the
// only way under that.
// Judged by rest, not by targetState: a gesture's seek has already pointed that at this side.
```

**Shape.**

- In a function body: one line, two at most. Beyond that it is an essay and the reader skips it.
- Above a group of declarations: a short paragraph is allowed — the block over `CustomBottomSheet`'s
  timing constants explains how the three numbers relate, which none of them can say alone.
- Never between two local `val`/`var` declarations. A block of locals has to read in one glance, and
  the reason is a contract: it goes in the KDoc of the host, or on the line it actually explains.
- Backticks and `[Name]` links belong to KDoc. A line comment is plain prose, and refers to an
  identifier by writing it out.
- A KDoc lists **every** parameter, not only the ones with something to say. The list is part of
  what the caller reads, and a gap in it leaves them unable to tell a plain parameter from one that
  was forgotten. What each line must not be is empty.
- Section markers over long declarative lists are their own kind and take a bare noun — `// Feed`,
  `// Comment` in `AppModule`.
- An affordance that is drawn but does nothing yet is marked where it is inert —
  `onClick = { /* TODO: Search Action */ }`. In that shape rather than `//`, because a line comment
  would swallow the closing brace.

**Which syntax.** `/** */` is not a style choice: it is the only form that attaches to a declaration,
so it is what quick doc, Dokka, `[links]` and `@param` all read — worth remembering because a note
written with KDoc's markup under a plain `/*` looks documented and is not. `//` and `/* */` are
identical to the compiler and to every tool; Kotlin's block comments even nest, so `/* */` is not the
fragile one, and the only mechanical difference is that `//` runs to the end of the line. That is the
whole rule: `//` everywhere, except where a line comment cannot go (inside an expression) or a
licence header that has to stay verbatim as it came.

## Git Conventions

- Commit format: `<type>: <description>` header, blank line, `- ` bullet list of changes, optional closing paragraph for motivation. Types: `feat`, `fix`, `refactor`, `build`, `chore`.
- Never run destructive git commands (force push, hard reset, skip hooks) without explicit approval
