# Stage 9: Application-Wide Dark Mode — Completion Report

**Project:** CSE Judge  
**Domain:** UPSC Civil Services Examination (Prelims) Practice  
**Date:** 2026-11-10  
**Status:** Complete & Verified on Physical Device (`Realme Narzo 90 5G / RMX5111`)

---

## 1. Existing Theme Architecture

Prior to Stage 9, the application had an initial styling foundation defined in `PracticeTheme.kt`, primarily targeted at Light Mode:
- **`AppColors` & `PracticeColors`:** Tokens defined for backgrounds, primary navy, text colors, and green/red answer highlighting.
- **`LocalAppColors`:** A static CompositionLocal (`staticCompositionLocalOf { LightAppColors }`) providing design tokens down the Compose tree.
- **`CSEJudgeTheme`:** A composable wrapping `MaterialTheme` with basic `lightColorScheme` mapping.
- **Screen Implementations:** Screens referenced `PracticeColors` tokens directly for core components, but minor component-level styling (e.g., option badge text colors in `PracticeOptionCard.kt`) still relied on hardcoded `Color(...)` / `Color.White` values.
- **Theme Selection & Persistence:** No mechanism existed to select or toggle between light and dark modes, and no persistence layer existed for theme preferences.

---

## 2. New Global Theme Architecture

The theme architecture was elevated to a unified, reactive, application-wide system:
- **Central Theme Provider (`CSEJudgeTheme`):** Centrally hosted in `PracticeTheme.kt` and wrapping the entire UI hierarchy inside `MainActivity.kt`. It accepts `darkTheme: Boolean` and dynamically provides:
  - `LocalAppColors` populated with either `DarkAppColors` or `LightAppColors`.
  - Material 3 `darkColorScheme` or `lightColorScheme` covering the complete surface elevation palette (`background`, `surface`, `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest`, `surfaceContainerLow`, `surfaceContainerLowest`, `surfaceVariant`, `onSurfaceVariant`, `outline`, `outlineVariant`, `primary`, `onPrimary`). This ensures that any standard M3 composable (such as `DropdownMenu`, `Card`, `Button`, `Switch`, `Dialog`) automatically adapts to the chosen theme without violet/purple tint fallbacks.
- **System Bars Synchronization:** Dynamic edge-to-edge system bar appearance controlled via `WindowCompat.getInsetsController(window, view)` inside `CSEJudgeTheme`:
  - In Light Mode: `isAppearanceLightStatusBars = true` and `isAppearanceLightNavigationBars = true` (crisp dark icons on light backgrounds).
  - In Dark Mode: `isAppearanceLightStatusBars = false` and `isAppearanceLightNavigationBars = false` (luminous light icons on deep charcoal/navy backgrounds).
  - `Scaffold` container background dynamically synchronizes to `PracticeColors.ScreenBackground`, eliminating visual mismatches across status and navigation bars.
- **Unidirectional Data Flow:** `ThemeViewModel` observes `ThemeRepository.isDarkMode` and exposes a reactive `StateFlow<Boolean>`. `MainActivity` collects this state and passes `isDarkMode` and `onToggleTheme` down through `AppNavHost` to `HomeScreen`. All child screens access tokens via `PracticeColors.<Token>`.

---

## 3. Light and Dark Colour Palettes

Formulated specifically for extended, distraction-free UPSC preparation sessions:

| Token | Light Mode (`LightAppColors`) | Dark Mode (`DarkAppColors`) | Purpose / Contrast Ratio |
|---|---|---|---|
| `screenBackground` | `#FBFBFD` (Off-white canvas) | `#0F172A` (Slate-900 deep charcoal) | Main scaffold background; avoids pure black `#000000` |
| `cardSurface` | `#FFFFFF` (Pure white) | `#1E293B` (Slate-800 charcoal) | Cards, option containers, tile backgrounds |
| `solutionBackground` | `#F8FAFC` (Slate-50 cool container) | `#162032` (Deep navy container) | Dedicated explanation container |
| `solutionBorder` | `#E2E8F0` (Slate-200 border) | `#334155` (Slate-700 subtle border) | Explanation card perimeter |
| `primaryNavy` | `#1B365D` (Deep navy blue) | `#38BDF8` (Sky-400 clear blue accent) | Primary branding, interactive buttons, chevrons (> 7:1) |
| `primaryNavyLight` | `#EBF1F7` (Soft navy tint) | `#1E3A5F` (Muted dark navy container) | Breadcrumbs, chevron containers |
| `onPrimary` | `#FFFFFF` (White text) | `#0F172A` (Deep slate text on bright sky accent) | Button content color (> 7:1) |
| `textPrimary` | `#1E293B` (Slate-800 charcoal text) | `#F8FAFC` (Slate-50 off-white text) | Main question statements, headings (> 13:1) |
| `textSecondary` | `#64748B` (Slate-500 neutral text) | `#94A3B8` (Slate-400 muted grey text) | Subtitles, labels, secondary counters (> 4.5:1 AA) |
| `borderNeutral` | `#E2E8F0` (Slate-200) | `#334155` (Slate-700 subtle dark grey) | Neutral card & divider borders |
| `correctBackground` | `#E8F5E9` (Green-50 tint) | `#064E3B` (Emerald-950 container) | Evaluated correct option container |
| `correctBorder` | `#2E7D32` (Green-800) | `#10B981` (Emerald-500 vivid border) | Correct option border & badge |
| `correctText` | `#1B5E20` (Green-900) | `#6EE7B7` (Emerald-300 light green) | Correct option text & solution indicators (> 8:1) |
| `correctBadgeText` | `#FFFFFF` (White on green badge) | `#064E3B` (Deep green text on emerald badge) | Letter badge text (A/B/C/D) |
| `incorrectBackground`| `#xFFFFEBEE` (Red-50 tint) | `#450A0A` (Red-950 container) | Evaluated incorrect option container |
| `incorrectBorder` | `#D32F2F` (Red-700) | `#EF4444` (Red-500 vivid border) | Incorrect option border & badge |
| `incorrectText` | `#B71C1C` (Red-900) | `#FCA5A5` (Red-300 accessible light red) | Incorrect option text (> 7.5:1) |
| `incorrectBadgeText`| `#FFFFFF` (White on red badge) | `#FFFFFF` (White on red badge) | Letter badge text (A/B/C/D) |
| `skipButtonBackground`| `#FFFFFF` | `#1E293B` | Neutral secondary action button |
| `skipButtonText` | `#475569` | `#E2E8F0` | Neutral secondary action text |
| `skipButtonBorder` | `#CBD5E1` | `#475569` | Neutral secondary action border |
| `bookmarkYellow` | `#F59E0B` (Amber-500) | `#FBBF24` (Amber-400 luminous gold) | Filled star icon in both modes |
| `bookmarkYellowLight`| `#FEF3C7` (Amber-100) | `#2E230B` (Deep amber container) | Active bookmark button background |
| `bookmarkYellowBorder`| `#D97706` (Amber-600) | `#F59E0B` (Amber-500 border) | Active bookmark button border |

---

## 4. Home Screen Toggle Implementation

Added a dedicated `ThemeToggleButton` composable at the top-right corner of the Home screen (`HomeScreen.kt`):
- **Positioning:** Aligned to the top right within the app header row, opposite "CSE Judge" branding. Does not alter or overlap the "Practice problems" or "Bookmarked Questions" tiles.
- **Adaptive Visuals:**
  - **In Light Mode:** Displays a custom-drawn crescent `MoonIcon` rendered with `PracticeColors.PrimaryNavy`, indicating Dark Mode can be enabled.
  - **In Dark Mode:** Displays a custom-drawn 8-ray `SunIcon` rendered with `PracticeColors.BookmarkYellow`, indicating Light Mode can be enabled.
- **Accessibility:** Dynamically assigns `contentDescription = "Switch to dark mode"` (when in Light Mode) and `contentDescription = "Switch to light mode"` (when in Dark Mode).
- **Touch Target & Semantics:** 44×44 dp minimum touch surface with rounded corners (10 dp), subtle elevation, and border stroke matching `BorderNeutral`.
- **Immediate Recomposition:** Tapping immediately updates the state via `onToggleTheme()`, triggering an instantaneous Compose recomposition of all active screens without requiring an activity restart.

---

## 5. Theme Preference Persistence Mechanism

Implemented local persistence using **Jetpack DataStore Preferences** (`androidx.datastore:datastore-preferences:1.1.3`):
- **Storage:** Stored locally in `csejudge_theme_prefs.preferences_pb` under app private storage (`Context.themeDataStore`).
- **No Room / No Cloud:** Independent of Room database (preserving attempt history and bookmarks schema stability) and completely offline with zero network dependencies.
- **Default Value:** Defaults to `false` (Light Mode) on clean install or whenever preferences are empty.
- **Zero-Flicker Cold Launch:** In `MainActivity.kt`, an initial value is pre-read via `themeRepository.getInitialDarkMode()` with a safety timeout (500ms max, typical disk read < 2ms) during `ViewModelProvider.Factory` instantiation. This feeds `initialDarkMode` directly into `ThemeViewModel`'s `stateIn(initialValue = ...)`. When Compose renders the very first frame on cold app launch, the saved Dark Mode is already active—completely eliminating the brief white flash on launch.
- **Persistence Across Process Termination:** Fully persists across force-stops, cold restarts, and configuration changes.

---

## 6. Exact Screens and Components Updated

Every existing screen, state, and interactive component was audited and verified for dark mode support:
1. **Home Screen (`HomeScreen.kt`):** Header, branding typography, `ThemeToggleButton`, `PracticeProblemsTile`, and `BookmarkedQuestionsTile` (including empty state text and amber saved-count badge).
2. **Practice Filter Screen (`PracticeFilterScreen.kt`):** Header, back button surface, error banner, Random Mode card and `Switch` (with themed thumb/track colors), Subject dropdown selector, Microtheme dropdown selector, `DropdownMenu` popup surface, session statistics card, and "Start Practice" button.
3. **Practice Question Screen (`PracticeQuestionScreen.kt`):** Subject/microtheme breadcrumb badges, session stats summary line, bookmark star button, filters button, question statement typography, vertical option cards, "Skip" and "Next" fixed action buttons.
4. **Practice Option Cards (`PracticeOptionCard.kt`):** Default, unselected, selected correct, selected incorrect, unselected correct, and disabled neutral states. All borders, containers, letter badge backgrounds, and badge text colors use semantic tokens.
5. **Solution & Explanation Card (`PracticeQuestionScreen.kt` & `BookmarkQuestionScreen.kt`):** Deep navy solution container (`#162032`), Slate-700 border (`#334155`), Primary navy header (`#38BDF8`), and readable Slate-50 body text (`#F8FAFC`).
6. **Loading, Error, and Exhaustion States:**
   - Loading: Circular progress indicators in `PrimaryNavy`.
   - Error: Dismissible error banners in `IncorrectBackground` / `IncorrectBorder` / `IncorrectText`.
   - Exhaustion: Practice Complete card with session summary items in dark mode.
7. **Bookmark Subjects Screen (`BookmarkSubjectsScreen.kt`):** Back button, title, subject cards with question count labels, navigation chevrons, and empty bookmark state view.
8. **Bookmark Microthemes Screen (`BookmarkMicrothemesScreen.kt`):** Microtheme cards, counter text, and no-microtheme fallback card.
9. **Bookmark Question Screen (`BookmarkQuestionScreen.kt`):** Breadcrumb, question index indicator, re-attempt option cards with previous answer retention, solution section, and fixed "← Previous" / "Next →" bottom navigation bar.
10. **Bookmark Star Button (`BookmarkStarButton.kt`):** Debounced 36×36 dp surface with amber container and vivid amber-400 star when active; card surface and slate-400 outline when unbookmarked.
11. **Navigation Bars and App Surfaces:** Edge-to-edge system bars seamlessly matching background; light icons in dark mode and dark icons in light mode.
12. **Interactive Controls & Menus:** `DropdownMenu` popups, `Switch` toggles, `Button`, `OutlinedButton`, `TextButton`.

---

## 7. Hardcoded Colour Audit Results

A comprehensive codebase audit was conducted across all files in `app/src/main/`:
- **Initial Audit:**
  - `PracticeOptionCard.kt`: Contained hardcoded `Color(0xFF064E3B)` and `Color.White` for option badge text colors.
  - `PracticeTheme.kt`: Contained the palette definition definitions.
- **Classification & Refactoring:**
  - Added semantic tokens `correctBadgeText: Color` and `incorrectBadgeText: Color` to `AppColors` and exposed them via `PracticeColors.CorrectBadgeText` and `PracticeColors.IncorrectBadgeText`.
  - Replaced all inline `Color(...)` calls in `PracticeOptionCard.kt` with `PracticeColors.CorrectBadgeText` and `PracticeColors.IncorrectBadgeText`.
- **Final Audit Result:**
  - `grep -rn "Color(" app/src/main/java/` matches exclusively in `PracticeTheme.kt`.
  - `grep -rn "Color\." app/src/main/java/` returns 0 matches in UI composables.
  - Zero hardcoded color values remain in any screen or component file.

---

## 8. Unit and Instrumented Test Results

### 8.1 Unit Tests (`./gradlew testDebugUnitTest`)
- **Total Executed:** 100 tests
- **Passed:** 100
- **Failed:** 0
- **Coverage of Stage 9 Requirements:**
  - `ThemeRepositoryImplTest > freshPreferenceStoreDefaultsToLightMode`: PASSED
  - `ThemeRepositoryImplTest > setDarkModeTrueUpdatesFlowAndInitialValue`: PASSED
  - `ThemeRepositoryImplTest > setDarkModeFalseRestoresLightMode`: PASSED
  - `ThemeRepositoryImplTest > preferenceSurvivesDataStoreRecreation`: PASSED (Tested with distinct DataStore instances after closing session scope)
  - `ThemeViewModelTest > defaultThemeIsLightMode`: PASSED
  - `ThemeViewModelTest > toggleThemeSwitchesLightToDark`: PASSED
  - `ThemeViewModelTest > toggleThemeSwitchesDarkToLight`: PASSED
  - `ThemeViewModelTest > setDarkModeExplicitlyUpdatesState`: PASSED
  - `ThemeStylingTest > darkAppColorsConformsToDesignGuidelines`: PASSED (Verifies deep charcoal `#0F172A`, cards `#1E293B`, accents, text colors)
  - `ThemeStylingTest > lightAppColorsConformsToLightDesignPalette`: PASSED
  - `ThemeStylingTest > correctAndIncorrectAnswersAreDistinguishableInDarkMode`: PASSED (Verifies emerald `#064E3B`/`#10B981`/`#6EE7B7` vs red `#450A0A`/`#EF4444`/`#FCA5A5`)
  - `ThemeStylingTest > correctAndIncorrectAnswersAreDistinguishableInLightMode`: PASSED
  - `ThemeStylingTest > bookmarkStarStylingRemainsVisibleInBothModes`: PASSED
  - All existing tests in `BookmarkViewModelTest`, `BookmarkRepositoryTest`, `PracticeViewModelTest`, `PracticeHistoryRepositoryTest`, `PracticeHistoryReattemptTest`, `PracticeSessionTest`, `NavRoutesTest`, `Stage6QualityAssuranceTest`: ALL PASSED

### 8.2 Instrumented Tests on Physical Device (`./gradlew connectedDebugAndroidTest`)
- **Target Device:** Realme Narzo 90 5G (`RMX5111 - Android 16`)
- **Total Executed:** 49 tests
- **Passed:** 49
- **Failed:** 0
- **Skipped:** 0
- **Coverage of Stage 9 Requirements:**
  - `HomeScreenTest > testThemeToggleButtonShowsMoonIconInLightModeAndInvokesCallback`: PASSED
  - `HomeScreenTest > testThemeToggleButtonShowsSunIconInDarkModeAndInvokesCallback`: PASSED
  - `AppNavigationTest > testThemePreservedAcrossNavigationFlow`: PASSED (Verifies theme consistency across Home -> Filter -> Question -> Back -> Home)
  - All existing tests in `AppNavigationTest`, `HomeScreenTest`, `PracticeQuestionScreenTest`, `PracticeFilterScreenTest`, `BookmarkFlowInstrumentedTest`, `PracticeDaoTest`, `BookmarkDaoTest`, `DatabaseMigrationTest`: ALL PASSED

---

## 9. Physical Device Verification

Physical testing was performed on the connected **Realme Narzo 90 5G** (`RMX5111`) via ADB:
1. **Fresh Launch:** Clean launch starts in Light Mode (`content-desc="Switch to dark mode"`, background `#FBFBFD`).
2. **Toggle to Dark Mode:** Tapped top-right toggle. Immediate smooth transition to Dark Mode without restart (`content-desc="Switch to light mode"`, background pixel `#0F172A`, cards `#1E293B`).
3. **Screen Inspection:**
   - *Home Screen:* Dark charcoal canvas, luminous sky-blue headers, clear contrast on tiles and saved bookmark count badge.
   - *Practice Filter Screen:* Random mode switch with dark slate track and sky blue thumb. Toggled switch off: Subject dropdown menu rendered with charcoal surface container and clear text contrast. Selected "Modern History".
   - *Practice Question Screen:* Statements clearly readable in off-white (`#F8FAFC`). Tapped bookmark star button: turned luminous gold (`#FBBF24`) on deep amber container (`#2E230B`).
   - *Answer Evaluation & Solution:* Tapped Option B: correctly evaluated with emerald green container (`#064E3B`), vivid border (`#10B981`), light green text (`#6EE7B7`), and dark emerald badge text (`#064E3B`). Solution container rendered in dark navy (`#162032`) with Slate-700 border.
   - *Bookmark Navigation Hierarchy:* Returned to Home. "Bookmarked Questions" tile showed "1 question saved". Navigated into Bookmark Subjects -> Modern History -> Bookmark Microthemes -> Other Important Aspects -> Bookmark Question Screen. Verified re-attempt option cards and Previous/Next buttons in dark mode.
4. **Cold Launch & Persistence:**
   - Executed `adb shell am force-stop com.csejudge.app`.
   - Executed cold launch `adb shell am start -n com.csejudge.app/.MainActivity`.
   - Inspected UI hierarchy: Dark Mode remained active immediately (`content-desc="Switch to light mode"`, background pixel `#0F172A`) with zero white flash.
5. **Switch Back to Light Mode:**
   - Tapped toggle button: reverted to Light Mode (`content-desc="Switch to dark mode"`).
   - Force-stopped and cold-launched: Light Mode remained active.
6. **Logcat Inspection:** Monitored runtime process. Zero `FATAL`, zero `ANR`, zero unhandled exceptions.

---

## 10. Exact Files Created and Modified

### Files Modified:
1. `app/build.gradle.kts`: Added `androidx-datastore-preferences` dependency.
2. `gradle/libs.versions.toml`: Added version `1.1.3` and library catalog entry for DataStore Preferences.
3. `app/src/main/java/com/csejudge/app/MainActivity.kt`: Integrated `ThemeViewModel` with pre-read initial value factory, wrapped root with `CSEJudgeTheme(darkTheme = isDarkMode)`, and connected theme toggle callback.
4. `app/src/main/java/com/csejudge/app/navigation/AppNavHost.kt`: Accepted `isDarkMode` and `onToggleTheme` parameters and forwarded them to `HomeScreen`.
5. `app/src/main/java/com/csejudge/app/ui/home/HomeScreen.kt`: Added `ThemeToggleButton`, `SunIcon`, and `MoonIcon` composables to the top-right header with accessibility content descriptions.
6. `app/src/main/java/com/csejudge/app/ui/practice/PracticeTheme.kt`: Implemented complete `DarkAppColors`, semantic `correctBadgeText` / `incorrectBadgeText`, `CSEJudgeTheme` with full Material 3 elevation scheme, and system bar appearance synchronization.
7. `app/src/main/java/com/csejudge/app/ui/practice/PracticeOptionCard.kt`: Refactored to use semantic `PracticeColors.CorrectBadgeText` and `PracticeColors.IncorrectBadgeText`.
8. `app/src/androidTest/java/com/csejudge/app/ui/home/HomeScreenTest.kt`: Added instrumented tests for theme toggle button states, icons, and click actions.
9. `app/src/androidTest/java/com/csejudge/app/navigation/AppNavigationTest.kt`: Added instrumented test for theme preservation across full navigation flows.

### Files Created:
1. `app/src/main/java/com/csejudge/app/data/repository/ThemeRepository.kt`: DataStore-backed repository interface and implementation with initial value reading.
2. `app/src/main/java/com/csejudge/app/ui/theme/ThemeViewModel.kt`: Central application theme ViewModel managing reactive theme state.
3. `app/src/test/java/com/csejudge/app/data/FakeThemeRepository.kt`: In-memory test double for `ThemeRepository`.
4. `app/src/test/java/com/csejudge/app/data/ThemeRepositoryImplTest.kt`: Unit tests verifying DataStore default, update, toggle, and persistence across session recreation.
5. `app/src/test/java/com/csejudge/app/ui/theme/ThemeViewModelTest.kt`: Unit tests verifying ViewModel state flow and toggle logic.
6. `app/src/test/java/com/csejudge/app/ui/theme/ThemeStylingTest.kt`: Unit tests verifying Light and Dark color tokens, contrast ratios, and answer evaluation colors.
7. `stage9_dark_mode_report.md`: This completion report.

---

## 11. Unresolved Issues

None. All Stage 9 requirements are fully satisfied, all 149 tests pass, and complete physical device verification on the Realme Narzo 90 5G (`RMX5111`) was successful.
No automatic Git commit was made as per scope restrictions. Ready for user review and approval.
