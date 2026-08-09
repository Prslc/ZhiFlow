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
| LaTeX | `huarangmeng:latex-renderer` for math formulas (inline + block) |
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
│           ├── core/
│           │   ├── exception/                  # ApiException sealed class + ErrorHandler
│           │   ├── native/Natives.kt           # JNI bridge: zse96Sign()
│           │   ├── network/
│           │   │   ├── HeaderProvider.kt       # UA, x-app-za, x-zse-96 signing
│           │   │   ├── HttpClientProvider.kt   # OkHttpClient with auth interceptor
│           │   │   └── NetworkExtensions.kt    # safeApiCall<T>(), safeExecute(), Response.body<T>()
│           │   └── utils/
│           │       ├── FormatHelper.kt
│           │       ├── JsonHelper.kt           # JSON encode/decode + HTML strip
│           │       ├── compose/                # LazyListExt, TextLayoutExt
│           │       └── platform/               # ClipboardHelper, ImageHelper
│           ├── data/
│           │   ├── dto/                        # Flat UI-ready data classes (FeedDto, AnswerDto, CommentDto, MomentDto, ReadHistoryDto)
│           │   ├── mapper/                     # DTO mappers: FeedItem→FeedDto, etc.
│           │   ├── model/
│           │   │   ├── content/                # ZhihuContent interface, Answer, Article, Pin (incl. PinImageList), Question, RichText (StructuredContent/Segment/Mark), ContentType enum
│           │   │   ├── comment/Comment.kt
│           │   │   ├── feed/                   # FeedItem (raw API JSON models), ZhihuResponse, PagingData
│           │   │   ├── moment/                 # Moment, MomentsFeed
│           │   │   └── user/                   # ZhihuUser, ReadHistory, Collection, History
│           │   ├── remote/
│           │   │   ├── parser/
│           │   │   │   ├── ContentParser.kt     # Segment→RichTextElement transform
│           │   │   │   ├── CommentParser.kt
│           │   │   │   ├── QuestionParser.kt
│           │   │   │   ├── LinkDestination.kt   # URL→internal route or external URL
│           │   │   │   ├── emoji/              # EmojiMap, EmojiParser
│           │   │   │   ├── engine/             # AnnotatedStringBuilder, FormulaHandler, TableParser
│           │   │   │   └── model/RichTextElement.kt
│           │   │   └── service/                # OkHttp-based API service classes (one per domain)
│           │   └── repository/                 # Domain repositories: map DTOs, return Result<T>
│           ├── di/
│           │   └── AppModule.kt                # Single Koin module: all singletons + viewModels
│           └── ui/
│               ├── theme/                      # Color.kt, Theme.kt (dynamic color + fallback), Type.kt
│               ├── navigation/
│               │   ├── Route.kt               # @Serializable route objects (MainContainer, AnswerDetail, etc.)
│               │   ├── NavGraph.kt            # NavGraphBuilder.contentGraph() — all composable destinations
│               │   └── Navigator.kt           # CompositionLocal-based Navigator (handleUrl, navigateTo*)
│               ├── component/
│               │   ├── common/                # AuthorRow, ContentMeta, ContentTypeLabel, EmptyView, ErrorView, LoadingView, PagingFooter, ThumbnailRow, LoadMoreErrorItem
│               │   ├── preference/            # ArrowPreference, PreferenceGroup
│               │   ├── richtext/              # RichText.kt (element dispatcher), ZRichText.kt (text rendering engine)
│               │   │   └── component/         # CardComponent, CodeComponent, LatexComponent, LayoutComponent, ListComponent, MediaComponent
│               │   └── widget/                # BottomBar, CollectionDialog, CustomBottomSheet, ImageLightbox
│               └── page/
│                   ├── feed/                  # FeedScreen, FeedItem, FeedViewModel
│                   ├── content/               # ContentDetailScreen (answer/article), ContentRichTextList, ContentDetailViewModel, CollectionViewModel
│                   ├── pin/                   # PinDetailScreen, PinViewModel (dedicated thought/idea page)
│                   ├── question/              # QuestionDetailScreen, QuestionAnswerList, QuestionViewModel
│                   ├── comment/               # CommentBottomSheet, CommentItem, CommentList, CommentViewModel
│                   ├── people/                # PeopleScreen, PeopleHeader, PeopleTabBar, PeopleViewModel + moment/ subpackage
│                   ├── profile/               # ProfileScreen, ProfileViewModel, SettingsScreen
│                   ├── history/               # ReadHistoryScreen, ReadHistoryViewModel
│                   ├── collection/            # CollectionContentsScreen, CollectionContentsViewModel
│                   └── debug/                 # DebugScreen, DebugViewModel (credentials config, URL parser test)
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
- **`safeExecute(requestBuilder)`** — Same but returns `Result<Boolean>` (success/failure only, no body parsing).
- **`Response.body<T>()`** — `inline reified` extension that `use`-closes the response and decodes JSON. Throws `HttpStatusException` on non-2xx.
- Auth headers (Cookie, Authorization, x-udid, x-zse-96) are injected by an OkHttp interceptor reading from `SharedPreferences`.
- **`HttpClientProvider`** holds the OkHttpClient singleton and a shared `Json` instance (lenient, coerce defaults, ignore unknown keys).
- **`HeaderProvider`** is an `object` that initializes the dynamic User-Agent via `WebSettings` at app startup, and signs requests via JNI `Natives.zse96Sign()`.

### DI (Koin)

Single module `appModule` in `di/AppModule.kt`. Uses DSL:
- `singleOf(::ClassName)` for services and repositories
- `viewModelOf(::ViewModelName)` for ViewModels
- `single { get<HttpClientProvider>().okHttpClient }` for the OkHttpClient instance
- `SharedPreferences` is provided as a `single` pointing to the `"temp_auth_prefs"` file
- Koin is started in `Application.onCreate()` with `startKoin { ... }`

### Navigation

- **Type-safe routes**: `@Serializable` data classes/objects in `Route.kt`. Uses `navigation-compose` 2.9 type-safe API (`composable<RouteType>`, `toRoute()`).
- **`MainContainer`** is the start destination — it contains three tabs (Home, Debug, Profile) in a `HorizontalPager` with a `NavigationBar`.
- Detail screens (`AnswerDetail`, `ArticleDetail`, `PinDetail`, `QuestionDetail`, `PeopleDetail`, `Settings`, `ReadHistory`, `CollectionContents`) are separate composable destinations pushed onto the NavHost stack. `PinDetail` uses a dedicated `PinDetailScreen` (thought page); `AnswerDetail`/`ArticleDetail` share `ContentDetailScreen`.
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
4. `ZRichText` composable wraps `Text` with clickable link interception, inline formula measurement via `LatexMeasurer`, and `key()`-based layout refresh for async measurement resolution
5. Inline math formulas use `InlineTextContent` + `Placeholder` with async bounds measurement

### Error Handling

- `ApiException` is a sealed class: `NetworkException`, `UnAuthorizedException`, `NotFoundException`, `ServerException(code)`, `UnknownException`
- Each carries an Android string resource ID; `.uiMessage` is a `@Composable` extension property
- `ErrorView` and `LoadMoreErrorItem` composables in `component/common/` render error states with retry buttons

## Key Conventions

- **All properties in API models have defaults** (empty strings, 0, null, empty lists) — never assume mandatory fields from the server.
- **Mappers are `internal`** — not exposed outside the `data.mapper` package.
- **Services use `Result<T>`** — never throw; all failures are caught and wrapped.
- **ViewModels use `viewModelScope.launch`** — always re-throw `CancellationException` in `.onFailure` blocks.
- **`@Immutable`/`@Stable`** annotations on data classes consumed by Compose for stability inference.
- **String resources** are in `res/values/strings.xml` (English) and `res/values-zh-rCN/strings.xml` (Chinese). Always reference via `R.string.*`, never hardcode user-facing strings.
- **Emoji**: Bundled as `.webp` assets in `assets/emoji/default/`; referenced by Zhihu emoji codes via `EmojiMap`.
- **Credentials**: Stored in `SharedPreferences` (`"temp_auth_prefs"`) — keys `auth`, `cookie`, `x_udid`. Managed via DebugScreen or programmatically.
- **Screen navigation**: Use `LocalNavigator.current` inside screens for item-click navigation (see `ReadHistoryScreen`, `CollectionContentsScreen`). Do NOT pass `onItemClick: (String, String) -> Unit` callbacks from NavGraph — the screen resolves its own navigation via `navigator.navigateToContent(id, type)`. This keeps NavGraph entries thin and avoids callback threading through multiple layers.

## Git Conventions

- Commit format: `<type>: <description>` header, blank line, `- ` bullet list of changes, optional closing paragraph for motivation. Types: `feat`, `fix`, `refactor`, `build`, `chore`.
- Never run destructive git commands (force push, hard reset, skip hooks) without explicit approval
