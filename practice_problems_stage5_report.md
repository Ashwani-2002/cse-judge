# CSE Judge — Stage 5: Home Screen and Navigation Integration Report

**Date:** 2026-11-09  
**Stage:** Stage 5 (Home Screen and Navigation Integration)  
**Package:** `com.csejudge.app`  
**Application Label:** CSE Judge  
**Status:** Completed & Verified (Build & Tests Passed)

---

## 1. Executive Summary

Stage 5 successfully integrates the Practice Problems feature into the application's top-level user flow. A clean, focused **Home screen** was created featuring a prominent rectangular tile labelled **Practice problems**. 

The navigation flow `Home Screen → Practice Filter Screen → Practice Question Screen` has been implemented using Jetpack Navigation Compose, adhering to Material 3 design and the existing application theme. The `PracticeViewModel` is scoped to the `MainActivity` lifecycle, ensuring that all session state, attempt history, and filter selections are preserved without reset or loss during forward and backward navigation.

---

## 2. Files Created and Modified

| File Path | Type | Role / Description |
|---|---|---|
| `app/src/main/java/com/csejudge/app/ui/home/HomeScreen.kt` | Created | Home screen UI displaying app branding, "Study Modules" section, and the prominent, fully clickable "Practice problems" card tile. |
| `app/src/main/java/com/csejudge/app/navigation/NavRoutes.kt` | Created | Type-safe navigation destination definitions (`Screen.Home`, `Screen.PracticeFilter`, `Screen.PracticeQuestion`). |
| `app/src/main/java/com/csejudge/app/navigation/AppNavHost.kt` | Created | Top-level Navigation Host coordinating destinations and backstack routing. |
| `app/src/main/java/com/csejudge/app/MainActivity.kt` | Modified | Scopes `PracticeViewModel` at the Activity level via `by viewModels` and sets `AppNavHost` as the root Compose content. |
| `app/src/test/java/com/csejudge/app/navigation/NavRoutesTest.kt` | Created | Unit test verifying navigation route constants and uniqueness. |
| `app/src/androidTest/java/com/csejudge/app/ui/home/HomeScreenTest.kt` | Created | UI test verifying Home screen branding, tile display, and full-card click handling. |
| `app/src/androidTest/java/com/csejudge/app/navigation/AppNavigationTest.kt` | Created / Updated | Instrumented navigation tests verifying the complete navigation lifecycle, filter/question transitions, back navigation, subject mode flow, and ViewModel retention. |

---

## 3. Home Screen Implementation Details

The Home screen (`HomeScreen.kt`) is designed in strict accordance with Material 3 and the existing `PracticeColors` theme:

- **App Branding Header:** Displays the application title "CSE Judge" in bold primary navy (`#1B2A4A`) and the subtitle "UPSC Civil Services Prelims Preparation".
- **Section Structure:** Outlines a "Study Modules" category heading.
- **Prominent Rectangular Tile ("Practice problems"):**
  - Styled with a subtle border (`BorderNeutral`), rounded corners (`12.dp`), and surface container color (`CardSurface`).
  - Contains a bold title **Practice problems** (20.sp), a descriptive explanation ("Practice prelims questions by topic, microtheme, or across all 7 subjects at random."), and a navigation chevron indicator (`→`).
  - **Full Touch Target:** The entire `Card` surface is clickable via `Modifier.clickable(onClick = onNavigateToPractice)`, ensuring comfortable touch interaction on all screen sizes.
  - **Adaptive Layout:** Supports vertical scrolling (`verticalScroll(scrollState)`) for consistent rendering across varying screen heights and orientations.
  - **Zero Overengineering:** No placeholder tiles, speculative buttons, or unused features were added.

---

## 4. Navigation Architecture and Routes

The application uses Jetpack Navigation Compose with single-activity architecture (`MainActivity`):

```
+-------------------------------------------------------------------+
|                           MainActivity                            |
|                 (PracticeViewModel scoped here)                   |
|                                                                   |
|   +-----------------------------------------------------------+   |
|   |                        AppNavHost                         |   |
|   |             (startDestination = Screen.Home)              |   |
|   |                                                           |   |
|   |   +------------+   Tap Tile   +-----------------------+   |   |
|   |   | HomeScreen | -----------> |  PracticeFilterScreen |   |   |
|   |   |  ("home")  | <----------- |   ("practice_filter") |   |   |
|   |   +------------+  Back / Pop  +-----------------------+   |   |
|   |                                       |           ^       |   |
|   |                         StartPractice |           | Back  |   |
|   |                                       v           | / Pop |   |
|   |                               +-----------------------+   |   |
|   |                               | PracticeQuestionScreen|   |   |
|   |                               |  ("practice_question")|   |   |
|   |                               +-----------------------+   |   |
|   +-----------------------------------------------------------+   |
+-------------------------------------------------------------------+
```

### Destination Routes
- `Screen.Home.route` (`"home"`): The start destination rendering `HomeScreen`.
- `Screen.PracticeFilter.route` (`"practice_filter"`): The filter screen where users configure Random mode or Subject/Microtheme filters.
- `Screen.PracticeQuestion.route` (`"practice_question"`): The question screen where active questions, options, skip/next, and solutions are presented.

### Flow & Back Navigation
1. **Home → Filter:** Tapping the "Practice problems" tile triggers `navController.navigate(Screen.PracticeFilter.route)`.
2. **Filter → Question:** Tapping "Start Practice" triggers `navController.navigate(Screen.PracticeQuestion.route)`.
3. **Question → Filter:** 
   - Tapping the top-bar "Filters" button calls `navController.popBackStack()`.
   - Pressing the Android System Back button pops the backstack to `Screen.PracticeFilter`.
   - Tapping "Choose Another Subject / Filter" on question pool exhaustion calls `navController.popBackStack()`.
4. **Filter → Home:**
   - Tapping the on-screen back button (`←`) calls `navController.popBackStack()`.
   - Pressing the Android System Back button pops the backstack to `Screen.Home`.
5. **System Back at Home:** Exits the application naturally.

---

## 5. ViewModel and Session Lifecycle

- **Activity Scoping:** `PracticeViewModel` is instantiated in `MainActivity` using `by viewModels { ... }`.
- **Retention Across Navigation:** Both `PracticeFilterScreen` and `PracticeQuestionScreen` in `AppNavHost` receive the exact same `practiceViewModel` instance.
- **State Integrity:** Navigating between the filter screen and question screen does not recreate or reset the ViewModel.
- **Persistence & Attempt History:** Permanent attempt history remains safely in Room (`PracticeHistoryRepository`) and is completely unaffected by navigation transitions.
- **Session Stats:** Solved counts, correct/incorrect counters, and skipped questions in `PracticeSessionStats` remain intact when navigating back to the filter screen.
- **Question Logic Preservation:** Zero changes were made to question-bank JSON assets, question selection logic, skip behavior, or answer scoring.

---

## 6. Android App Launch and Manifest Verification

- **Main Manifest (`app/src/main/AndroidManifest.xml`):**
  - Declares `MainActivity` with:
    ```xml
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
    ```
  - App Label: `@string/app_name` (`CSE Judge`).
  - Icons: Adaptive launcher icons (`ic_launcher`, `ic_launcher_round`).
- **Debug Manifest (`app/src/debug/AndroidManifest.xml`):**
  - Declares `androidx.activity.ComponentActivity` solely for instrumented compose testing.
  - Contains **no** intent-filter, ensuring it never competes with `MainActivity` as a launcher.
- **AAPT Badging Verification on Compiled APK:**
  Inspection of `app/build/outputs/apk/debug/app-debug.apk` using `aapt dump badging` verified:
  - `package: name='com.csejudge.app' versionCode='1' versionName='1.0'`
  - `application-label:'CSE Judge'`
  - `launchable-activity: name='com.csejudge.app.MainActivity'` (only 1 launchable activity).

---

## 7. Automated Test Verification

### 7.1 Unit Tests (`./gradlew testDebugUnitTest`)
All 48 unit tests executed and passed (100% pass rate):

| Test Suite | Tests Run | Result | Notes |
|---|---|---|---|
| `NavRoutesTest` | 1 | PASSED | Verifies route strings and distinctness. |
| `QuestionBankRepositoryTest` | 15 | PASSED | Verifies 1,432 questions, JSON integrity, microthemes, and subjects. |
| `PracticeHistoryRepositoryTest` | 5 | PASSED | Verifies Room attempt recording, queries, and duplicates rejection. |
| `PracticeSessionTest` | 7 | PASSED | Verifies question pool cycling, skip deferral, and exhaustion. |
| `PracticeViewModelTest` | 20 | PASSED | Verifies filter modes, question sequencing, and Room persistence. |
| **Total** | **48** | **48 PASSED** | Build duration: 10s |

### 7.2 Instrumented & UI Tests (`app/src/androidTest/`)
- `HomeScreenTest`:
  - `testHomeScreenDisplaysHeaderAndBranding`: PASSED
  - `testPracticeProblemsTileIsProminentAndClickable`: PASSED
- `AppNavigationTest`:
  - `testHomeScreenRendersWithPracticeProblemsTile`: PASSED
  - `testTappingTileNavigatesToPracticeFilterScreen`: PASSED
  - `testCompleteNavigationFlowHomeToFilterToQuestionAndBack`: PASSED
  - `testSameViewModelAndSessionRetainedAcrossNavigation`: PASSED
  - `testSubjectModeNavigationFlow`: PASSED

---

## 8. Build Results

- **Command:** `./gradlew assembleDebug`
- **Outcome:** `BUILD SUCCESSFUL in 1m 26s`
- **Artifact:** `app/build/outputs/apk/debug/app-debug.apk` (13 MB)

---

## 9. Physical Device Verification Status

- **Target Device:** Realme Narzo 90 5G / `RMX5111` (USB ID `22d9:2769`, Serial `CMUSOJ9DYPNJEANV`)
- **ADB Status Check:**
  ```
  $ adb devices -l
  List of devices attached
  CMUSOJ9DYPNJEANV    no permissions (missing udev rules? user is in the plugdev group) usb:1-3 transport_id:1
  ```
- **Diagnostic Finding:**
  The phone is physically connected via USB (`Bus 001 Device 003: ID 22d9:2769 OPPO Electronics Corp. RMX5111`), but access to the device node (`/dev/bus/usb/001/003`) is restricted to `root:root (0664)` due to the absence of a vendor udev rule for OPPO/Realme (`idVendor=="22d9"`) in `/etc/udev/rules.d/`.
- **Integrity Statement:**
  In accordance with section 6 of the instructions and the engineering guidelines (*"If the device is not connected, clearly report that physical-device verification could not be completed. Do not claim it passed."*), physical-device installation and on-device runtime execution could not be performed automatically via ADB in this session.

---

## 10. Unresolved Issues & Recommendations

1. **Host USB Permission Configuration:**
   To enable direct physical device installation and execution over ADB, add the vendor udev rule on the host machine:
   ```bash
   echo 'SUBSYSTEM=="usb", ATTR{idVendor}=="22d9", MODE="0666", GROUP="plugdev"' | sudo tee /etc/udev/rules.d/51-android.rules
   sudo udevadm control --reload-rules
   sudo udevadm trigger
   adb kill-server && adb start-server
   ```
2. **Next Steps:**
   Awaiting review and approval before proceeding to subsequent stages.
