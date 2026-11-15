# Stage 8: Bookmark Questions UI & Re-attempt Workflow Delivery Report

**Project:** CSE Judge (`com.csejudge.app`)  
**Domain:** UPSC Civil Services Examination (Prelims) Practice Application  
**Stage:** Stage 8 — Bookmark Questions UI & Re-attempt Workflow  
**Date:** November 10, 2026  
**Status:** COMPLETE & VERIFIED ON PHYSICAL HARDWARE  

---

## 1. Executive Summary

Stage 8 completes the end-to-end user-facing Bookmark feature and the Re-attempt workflow in **CSE Judge**. Building seamlessly upon the Stage 7 backend foundation (`BookmarkedQuestionEntity`, `BookmarkDao`, `BookmarkRepository`, and `upsertAttempt`), all user interface components, reactive counts, 3-level navigation hierarchies, attempt displays, and in-place re-answering capabilities were implemented with surgical precision.

### Key Highlights
- **Question Screen Bookmark Star Button:** Added an accessible, custom-drawn 5-point star icon button immediately to the left of the existing "Filters" button on the question screen. Displays an unselected outline when not bookmarked and vibrant yellow/gold (`#F59E0B`) with subtle warm container tint when bookmarked. Debounced against rapid multiple taps (`isBookmarkToggling`).
- **Home Screen Bookmarked Questions Tile:** Prominent rectangular tile visually consistent with `PracticeProblemsTile`. Displays reactive count badge when bookmarks exist (`X questions saved`) and an informative empty state description when 0 bookmarks exist. Updates in real-time as bookmarks are toggled.
- **3-Level Navigation Hierarchy:**
  - Level 1 (`BookmarkSubjectsScreen`): Displays only subjects containing active bookmarks, with per-subject question counts. When 0 bookmarks exist, renders a dedicated empty state explaining how to use the star button, with a direct "Go to Practice" action button.
  - Level 2 (`BookmarkMicrothemesScreen`): Displays only microthemes containing bookmarked questions within the chosen subject, with per-microtheme question counts.
  - Level 3 (`BookmarkQuestionScreen`): Displays bookmarked questions belonging to the chosen microtheme. Retrieves original question definitions by immutable `question_id` without duplicating question bank records. Includes Previous / Next navigation between questions.
- **Previous Attempt State Detection:**
  - If a bookmarked question was never attempted, renders the standard unanswered interface.
  - If previously attempted, detects and displays the previously selected answer, correctness status (Green if correct, Red if incorrect), correct option, and full solution/explanation.
- **Re-attempt Workflow:**
  - Users can answer a previously attempted question again by clicking any option.
  - New answer is accepted, re-evaluated against the correct answer, and updated in-place via Room's `@Upsert` (`practiceDao.upsertAttempt(...)`).
  - No duplicate attempt history records are created.
  - UI evaluates and updates immediately with solution and new selection colors.
- **Independence of Bookmarks and Attempt History:**
  - Removing a bookmark deletes only the record from `bookmarked_questions`.
  - Attempt records in `attempted_questions` are never deleted or affected.
  - Re-bookmarking restores the bookmark while retaining existing attempt history.
- **Physical Device & Automated Verification:**
  - **JVM Unit Tests:** 87/87 passed (100%).
  - **Connected Device Tests (Realme Narzo 90 5G / `RMX5111` / Android 16):** 46/46 passed (100%).
  - **Live Hardware Interaction:** Verified cold restart persistence, star toggling, live SQLite inspection, and re-attempt record upsert via ADB.

---

## 2. Files Created & Modified

### 2.1 Files Created
1. `app/src/main/java/com/csejudge/app/ui/bookmark/BookmarkStarButton.kt`
   - Custom 5-pointed geometric star vector canvas, debounced button container, accessible semantics, test tags.
2. `app/src/main/java/com/csejudge/app/ui/bookmark/BookmarkUiState.kt`
   - Immutable data models: `BookmarkSubjectItem`, `BookmarkMicrothemeItem`, `BookmarkQuestionState`, `BookmarkUiState`.
3. `app/src/main/java/com/csejudge/app/ui/bookmark/BookmarkViewModel.kt`
   - Reactive flow observation from `BookmarkRepository` and `PracticeHistoryRepository`.
   - Subject and microtheme hierarchy filtering (strictly excluding 0-count categories).
   - Attempt state resolution and in-place re-attempt execution via `recordOrUpdateAttempt`.
   - Debounced star toggling.
4. `app/src/main/java/com/csejudge/app/ui/bookmark/BookmarkSubjectsScreen.kt`
   - Level 1 screen displaying filtered subjects with counts, or empty state view.
5. `app/src/main/java/com/csejudge/app/ui/bookmark/BookmarkMicrothemesScreen.kt`
   - Level 2 screen displaying filtered microthemes within the chosen subject.
6. `app/src/main/java/com/csejudge/app/ui/bookmark/BookmarkQuestionScreen.kt`
   - Level 3 question review screen with previous attempt detection, clickable re-attempt options, solution rendering, star toggling, and Previous/Next buttons.
7. `app/src/test/java/com/csejudge/app/ui/bookmark/BookmarkViewModelTest.kt`
   - 8 unit tests covering empty state, subject grouping, microtheme filtering, attempt loading, re-attempt upsert, attempt preservation on unbookmark, and navigation.
8. `app/src/androidTest/java/com/csejudge/app/ui/bookmark/BookmarkFlowInstrumentedTest.kt`
   - 4 instrumented device tests covering end-to-end hierarchy navigation, back navigation, re-attempt workflow, and attempt preservation.

### 2.2 Files Modified
1. `app/src/main/java/com/csejudge/app/ui/practice/PracticeTheme.kt`
   - Added amber/yellow bookmark tokens: `BookmarkYellow`, `BookmarkYellowLight`, `BookmarkYellowBorder`.
2. `app/src/main/java/com/csejudge/app/ui/practice/PracticeQuestionScreen.kt`
   - Added `BookmarkStarButton` inside `HeaderSection` immediately to the left of the `Filters` button.
   - Wired `onToggleBookmark` to `viewModel.toggleBookmark()`.
3. `app/src/main/java/com/csejudge/app/ui/home/HomeScreen.kt`
   - Integrated `BookmarkedQuestionsTile` with reactive count pill badge, dynamic subtitle, and empty state text.
4. `app/src/main/java/com/csejudge/app/navigation/NavRoutes.kt`
   - Added `RouteUtils` (UTF-8 encoding/decoding) and type-safe routes: `BookmarkSubjects`, `BookmarkMicrothemes`, `BookmarkQuestions`.
5. `app/src/main/java/com/csejudge/app/navigation/AppNavHost.kt`
   - Wired `BookmarkViewModel` and added composable destinations for the 3 bookmark hierarchy levels with backstack navigation.
6. `app/src/main/java/com/csejudge/app/MainActivity.kt`
   - Instantiated and provided `BookmarkViewModel` via `ViewModelProvider.Factory`.
7. `app/src/test/java/com/csejudge/app/navigation/NavRoutesTest.kt`
   - Added route uniqueness and URL parameter encoding/decoding verification.
8. `app/src/androidTest/java/com/csejudge/app/ui/home/HomeScreenTest.kt`
   - Added tests for `BookmarkedQuestionsTile` empty state and count badge.
9. `app/src/androidTest/java/com/csejudge/app/ui/practice/PracticeQuestionScreenTest.kt`
   - Added tests verifying star button visibility, clickability, and unselected/selected visual states.

---

## 3. UI Changes & Navigation Structure

### 3.1 Bookmark Star Button on Practice Question Screen
- **Placement:** Positioned in `HeaderSection` immediately to the left of the `Filters` `OutlinedButton`.
- **States:**
  - *Unbookmarked:* 36dp surface with 1dp border (`BorderNeutral`), white background, outlined 5-pointed star in `TextSecondary` (`contentDescription = "Bookmark question"`).
  - *Bookmarked:* 36dp surface with 1dp amber border (`BookmarkYellowBorder`), subtle warm amber tint background (`BookmarkYellowLight`), filled gold star (`BookmarkYellow = #F59E0B`, `contentDescription = "Bookmarked"`).
- **Debounce:** Disables click interaction while `isBookmarkToggling` is active.

### 3.2 Home Screen Tile
- **Title:** "Bookmarked Questions"
- **Styling:** Same rounded corners (12dp), neutral border (1dp), white card container, and navy chevron navigation indicator as `PracticeProblemsTile`.
- **Badge:** Prominent amber pill badge displaying `"X questions saved"` when count > 0.
- **Empty State:** When count == 0, badge is hidden and description prompts: *"No bookmarked questions yet. Tap the star button while practicing to bookmark questions for revision."*

### 3.3 Navigation Hierarchy
```
Home Screen
  └── [Bookmarked Questions Tile]
        └── Level 1: BookmarkSubjectsScreen (route: "bookmark_subjects")
              ├── Back: ← (returns to Home)
              ├── Empty State: "No Bookmarked Questions" + [Go to Practice]
              └── List of Subjects with bookmark counts
                    └── Level 2: BookmarkMicrothemesScreen (route: "bookmark_microthemes/{subject}")
                          ├── Back: ← (returns to BookmarkSubjectsScreen)
                          └── List of Microthemes within Subject with counts
                                └── Level 3: BookmarkQuestionScreen (route: "bookmark_questions/{subject}/{microtheme}")
                                      ├── Back: [Back] button (returns to BookmarkMicrothemesScreen)
                                      ├── Star: [BookmarkStarButton] (toggles bookmark in-place)
                                      ├── Content: Question statements + Option cards + Solution
                                      └── Navigation: [← Previous] (X/Y) [Next →]
```

---

## 4. Re-attempt Implementation Details

When a user answers a question in `BookmarkQuestionScreen`:
1. `BookmarkViewModel.answerQuestion(answer: String)` validates that the chosen option is valid (`A`, `B`, `C`, or `D`).
2. Correctness is evaluated by comparing the option against `question.correctAnswer.trim().uppercase()`.
3. `PracticeHistoryRepository.recordOrUpdateAttempt(...)` dispatches to `PracticeDao.upsertAttempt(attempt: AttemptedQuestionEntity)`.
4. Because `question_id` is the primary key in `attempted_questions`, SQLite executes an `INSERT OR REPLACE` (upsert), modifying the existing attempt record in-place:
   - `selected_answer` is updated to the new selection.
   - `is_correct` is recalculated and updated.
   - `attempted_at` timestamp is updated to `System.currentTimeMillis()`.
5. **No Duplicate Rows:** SQLite `count(*)` on `attempted_questions` remains strictly 1 per `question_id`.
6. **UI Reactivity:** `BookmarkUiState.currentQuestionState` updates immediately, re-evaluating option colors:
   - Selected option: Green if correct (`SELECTED_CORRECT`), Red if wrong (`SELECTED_INCORRECT`).
   - Correct option: Highlighted Green (`UNSELECTED_CORRECT`).
   - Solution & Explanation card is displayed immediately.
7. **Normal Practice Preservation:** Normal practice (`PracticeViewModel.submitAnswer`) continues to use `recordAttempt` (`OnConflictStrategy.ABORT`), guaranteeing that standard practice workflows are not disrupted.

---

## 5. Database Interaction & Persistence Verification

### 5.1 Schema Stability
- Database version remains at `2`.
- Entities:
  - `attempted_questions` (`question_id` PK, `selected_answer`, `is_correct`, `attempted_at`)
  - `bookmarked_questions` (`question_id` PK, `bookmarked_at`)
- Non-destructive: No tables were altered or dropped.

### 5.2 Independence Verification via SQLite (ADB Shell on Hardware)
1. **Initial Answer & Bookmark:**
   - Attempt: `GEO00083|A|0|1794294094206` (Option A, Incorrect)
   - Bookmark: `GEO00083|1794294069664`
2. **Re-attempt via UI:**
   - User selected Option D (Correct).
   - Query: `SELECT * FROM attempted_questions;`
   - Output: `GEO00083|D|1|1794294266928`
   - Row count: `1` (Verified in-place update).
3. **Unbookmark via Star Button:**
   - User tapped yellow star on `BookmarkQuestionScreen`.
   - Query: `SELECT * FROM bookmarked_questions;` -> `0 rows`
   - Query: `SELECT * FROM attempted_questions;` -> `GEO00083|D|1|1794294266928` (100% preserved).
4. **Re-bookmarking:**
   - User tapped star again.
   - Query: `SELECT * FROM bookmarked_questions;` -> `GEO00083|1794294324640`
   - Attempt record still intact.
5. **Cold Kill & Restart:**
   - Executed `adb shell am force-stop com.csejudge.app` followed by `adb shell am start -n com.csejudge.app/.MainActivity`.
   - Home Screen immediately rendered `"1 question saved"`.
   - Navigating to question re-loaded attempt `D`, correct indication, and explanation intact.

---

## 6. Test Suite & Verification Results

### 6.1 JVM Unit Tests (Robolectric / JUnit 4)
Executed via `./gradlew testDebugUnitTest`: **87/87 PASSED (100%)**

| Test Class | Focus Area | Result |
|---|---|:---:|
| `BookmarkViewModelTest` (8 tests) | Empty state, reactive count observation, subject/microtheme filtering, attempt loading, re-attempt in-place upsert, attempt retention upon unbookmarking, navigation | **PASSED** |
| `PracticeViewModelBookmarkTest` (4 tests) | Bookmark loading on question change, `toggleBookmark()` add/remove, debounce guard | **PASSED** |
| `BookmarkRepositoryTest` (9 tests) | Master question bank foreign key validation, add/remove, reactive Flows, error handling | **PASSED** |
| `PracticeHistoryReattemptTest` (4 tests) | `recordOrUpdateAttempt` upsert verification, attempt overwriting without duplicate rows | **PASSED** |
| `NavRoutesTest` (2 tests) | Route uniqueness and safe URL encoding/decoding across special characters | **PASSED** |
| `PracticeHistoryRepositoryTest` (5 tests) | Attempt persistence, query, observation, validation | **PASSED** |
| `PracticeViewModelTest` (20 tests) | Mode switching, filtering, session state machine, answer submission, skip/next flow | **PASSED** |
| `PracticeSessionTest` (7 tests) | Question pool exhaustion, skip-cycling, stats tracking | **PASSED** |
| `Stage6QualityAssuranceTest` (13 tests) | 1,432 master question audit, schema integrity, JSON consistency | **PASSED** |
| `QuestionBankRepositoryTest` (15 tests) | JSON asset deserialization, subject and microtheme indexing | **PASSED** |

### 6.2 Connected Instrumented Tests (Physical Device)
Executed via `./gradlew connectedDebugAndroidTest` on **Realme Narzo 90 5G (`RMX5111` / Android 16)**: **46/46 PASSED (100%)**

| Test Class | Device Execution Details | Result |
|---|---|:---:|
| `BookmarkFlowInstrumentedTest` (4 tests) | End-to-end hierarchy navigation (Home -> Subjects -> Microthemes -> Questions), back navigation through all 3 levels, previous attempt detection, re-attempt upsert, star toggling | **PASSED** |
| `HomeScreenTest` (4 tests) | Branding, PracticeProblemsTile click, BookmarkedQuestionsTile 0-count empty state, count badge rendering | **PASSED** |
| `PracticeQuestionScreenTest` (9 tests) | Question statements order, option card rendering, skip/next toggle, solution visibility, BookmarkStarButton visibility, clickability, and unselected/selected visual states | **PASSED** |
| `AppNavigationTest` (5 tests) | Full navigation Compose flow between Home, Practice Filter, and Practice Question screens | **PASSED** |
| `BookmarkDaoTest` (7 tests) | Insert, duplicate replace, delete, isBookmarked query, Flow reactivity, count | **PASSED** |
| `DatabaseMigrationTest` (1 test) | Migration from Room v1 to v2; verifies existing attempts survive and bookmark table works | **PASSED** |
| `PracticeDaoTest` (10 tests) | Attempt CRUD, abort conflict strategy, upsert attempt verification | **PASSED** |
| `QuestionBankInstrumentedTest` (6 tests) | Android asset loading across all 7 subject JSON files | **PASSED** |

---

## 7. Physical Device Verification Summary

- **Device:** Realme Narzo 90 5G (`RMX5111`)
- **OS:** Android 16 (API 36)
- **APK Path:** `app/build/outputs/apk/debug/app-debug.apk`
- **Build Status:** `./gradlew assembleDebug` succeeded with 0 warnings/errors.
- **Install Command:** `adb install -r app/build/outputs/apk/debug/app-debug.apk` (Exit code: 0, Success).
- **Launch Command:** `adb shell am start -n com.csejudge.app/.MainActivity` (Exit code: 0).
- **Logcat Inspection:** Zero crashes, zero Room exceptions, zero memory leaks or Compose runtime warnings.
- **Visual & Functional Checks Confirmed on Hardware:**
  1. Bookmark star button rendered directly to the left of the Filters button on the question screen.
  2. Tapping star turns it yellow; tapping again returns it to unselected state.
  3. Home screen Bookmarked Questions tile reactively increments count when questions are bookmarked and decrements when unbookmarked.
  4. Only subjects with bookmarks appear in the subject list.
  5. Only microthemes with bookmarks appear under the selected subject.
  6. Opening an attempted bookmarked question shows the previous answer (red/green) and full solution.
  7. Tapping a different option re-attempts the question, updating SQLite in-place with zero duplicate rows.
  8. Unbookmarking preserves the attempt record in SQLite and leaves the explanation visible on screen.
  9. Cold restart preserves all attempt and bookmark states.

---

## 8. Known Limitations & Next Steps

### Known Limitations
- None. All requirements for Stage 8 have been met without technical debt or workaround compromises.

### Out of Scope (Reserved for Future Stages)
- Mock Test Session bookmarking (Stage 9+).
- Custom user revision notes attached to bookmarks (Future Stage).
- Global bookmark search filter (Future Stage).

---

## 9. Conclusion

Stage 8 is **100% COMPLETE, VERIFIED ON AUTOMATED UNIT TESTS (87/87), VERIFIED ON CONNECTED INSTRUMENTED TESTS (46/46), AND LIVE ON TARGET HARDWARE**. Awaiting user review and approval before starting any further work.
