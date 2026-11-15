# Stage 13 — Tests Module (Full Length Tests, Half Length Tests, and Past Test Reports)

## 1. Executive Summary

Stage 13 introduces a comprehensive, production-grade **Tests Module** to CSE Judge. Designed to mirror the rigorous format and evaluation standards of the UPSC Civil Services Examination (Prelims) General Studies Paper 1, this module allows aspirants to undertake timed Full Length Tests (FLT, 100 questions / 120 minutes) and Half Length Tests (HLT, 50 questions / 60 minutes), enforces anti-distraction / exam-security lifecycle controls, evaluates performance using UPSC negative marking (+2.0 / -0.67 marks), provides deep post-test analytical reviews with explanations and bookmarking, and maintains historical test records with immutable question snapshots.

Critically, Stage 13 adheres to the unified attempt-history architecture: every question answered during a completed test is atomically recorded into the authoritative practice history (`attempted_questions`), automatically advancing syllabus coverage and updating global analysis metrics while permanently preventing those questions from reappearing in future practice sets or test selections. Unanswered questions remain unattempted and eligible for future practice and tests, while aborted tests discard all temporary state without polluting user analytics.

---

## 2. Architectural Decisions & Design

The Tests Module was engineered adhering strictly to Clean MVVM, Unidirectional Data Flow (UDF), and Material 3 design guidelines:

```
                               ┌───────────────────────────────────────────────┐
                               │                 UI LAYER                      │
                               │  - HomeScreen (Tile at Position 2)            │
                               │  - TestsLandingScreen (FLT, HLT, Past Tests)  │
                               │  - TestInstructionsScreen (Rules & Shortage)  │
                               │  - TestQuestionScreen (Active Exam & Timer)   │
                               │  - TestPaletteDialog (5-Column Color Grid)    │
                               │  - TestReportScreen (Overall & Question Wise) │
                               │  - PastTestsScreen (Historical Test Cards)    │
                               └───────────────────────┬───────────────────────┘
                                                       │
                                                       ▼
                               ┌───────────────────────────────────────────────┐
                               │                VIEWMODEL                      │
                               │  - TestViewModel (Authoritative State Holder) │
                               │    * Decrementing countdown timer coroutine   │
                               │    * In-memory test state & selections        │
                               │    * Dialog & palette visibility controls     │
                               │    * Lifecycle ON_STOP abort handler          │
                               │    * Submit, abort & bookmark actions         │
                               └───────────┬───────────────────────┬───────────┘
                                           │                       │
                                           ▼                       ▼
            ┌─────────────────────────────────────────┐   ┌───────────────────────────┐
            │         QUESTION SELECTION ENGINE       │   │   BOOKMARK REPOSITORY     │
            │  - TestQuestionSelector                 │   │  - BookmarkRepository     │
            │    * Subject allocation ranges          │   │    (Post-test bookmarking)│
            │    * Nearest-integer rounding for HLT   │   └───────────────────────────┘
            │    * Subject alias matching             │
            │    * Strict unattempted filtering       │
            │    * Random allocation & shuffle        │
            └────────────────────┬────────────────────┘
                                 │
                                 ▼
            ┌─────────────────────────────────────────────────────────────────────────┐
            │                           DATA & PERSISTENCE                            │
            │  - TestRepository (Single transaction orchestration)                    │
            │    * AppDatabase (Schema v3 via non-destructive MIGRATION_2_3)          │
            │    * TestDao: tests & test_questions snapshot tables                    │
            │    * PracticeDao: upserts answered questions into attempted_questions   │
            │    * QuestionBankRepository: read-only static question content          │
            └─────────────────────────────────────────────────────────────────────────┘
```

### Key Architectural Invariants
1. **Dynamic Question Bank Scalability Integration:**
   - Evaluates questions directly against `QuestionBankRepository.allQuestions`.
   - Normalizes subject naming so that canonical names (e.g., `"Science and Technology"`) match actual JSON subject tags (e.g., `"Sci and Tech"`).
   - Dynamically supports newly added subjects or microthemes without code modification.
2. **Immutable Test Question Snapshots:**
   - To guarantee that historical test reports can be reconstructed faithfully even if underlying question-bank JSON files are edited or reordered in future releases, every completed test serializes its questions into the `test_questions` table with full statements, options, correct answers, and explanations.
3. **Atomic Unified Attempt Persistence:**
   - Submitting a test executes an atomic SQLite transaction inside `TestRepository.saveCompletedTest()`. It persists the `TestEntity`, all `TestQuestionEntity` snapshots, and upserts only the answered questions into `attempted_questions`.
   - If an error occurs, the entire transaction rolls back cleanly.

---

## 3. Database Schema & Migration (MIGRATION_2_3)

To store completed tests and question snapshots while protecting existing user attempts and bookmarks, Room database version was updated from **2 to 3**.

### Table Schemas
1. **`tests` Table:**
   - `id` (`TEXT`, PRIMARY KEY): Unique UUID (`test_<uuid>`).
   - `test_type` (`TEXT`, NOT NULL): `"FLT"` or `"HLT"`.
   - `start_time` (`INTEGER`, NOT NULL): Epoch millis when test started.
   - `end_time` (`INTEGER`, NOT NULL): Epoch millis when test submitted.
   - `duration_seconds` (`INTEGER`, NOT NULL): Actual time taken in seconds.
   - `total_questions` (`INTEGER`, NOT NULL): 100 for FLT, 50 for HLT.
   - `attempted_count` (`INTEGER`, NOT NULL): Number of questions answered.
   - `correct_count` (`INTEGER`, NOT NULL): Number of correct answers.
   - `incorrect_count` (`INTEGER`, NOT NULL): Number of incorrect answers.
   - `unanswered_count` (`INTEGER`, NOT NULL): Number of unattempted questions.
   - `score` (`REAL`, NOT NULL): Evaluated marks scored.
   - `max_marks` (`REAL`, NOT NULL): 200.0 for FLT, 100.0 for HLT.
   - `accuracy` (`REAL`, NOT NULL): Percentage accuracy on answered questions.

2. **`test_questions` Table:**
   - `id` (`INTEGER`, PRIMARY KEY AUTOINCREMENT).
   - `test_id` (`TEXT`, NOT NULL, FOREIGN KEY referencing `tests(id)` ON DELETE CASCADE): Indexed.
   - `question_id` (`TEXT`, NOT NULL): Permanent question identifier.
   - `subject` (`TEXT`, NOT NULL): Subject name.
   - `microtheme` (`TEXT`, NOT NULL): Microtheme tag.
   - `question_text` (`TEXT`, NOT NULL): Question statement string or JSON statements.
   - `options_json` (`TEXT`, NOT NULL): JSON-serialized list of option texts.
   - `correct_answer` (`TEXT`, NOT NULL): Key ("A", "B", "C", "D").
   - `user_answer` (`TEXT`, NULLABLE): Selected answer ("A", "B", "C", "D") or null.
   - `is_correct` (`INTEGER`, NULLABLE): 1 if correct, 0 if incorrect, null if unanswered.
   - `marked_for_review` (`INTEGER`, NOT NULL): 1 if marked for review, 0 otherwise.
   - `explanation` (`TEXT`, NOT NULL): Full solution and explanation text.
   - `display_order` (`INTEGER`, NOT NULL): Sequential index within the test (1 to 100 or 1 to 50).

### Non-Destructive Migration Script (`MIGRATION_2_3`)
```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS tests (
                id TEXT NOT NULL PRIMARY KEY,
                test_type TEXT NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER NOT NULL,
                duration_seconds INTEGER NOT NULL,
                total_questions INTEGER NOT NULL,
                attempted_count INTEGER NOT NULL,
                correct_count INTEGER NOT NULL,
                incorrect_count INTEGER NOT NULL,
                unanswered_count INTEGER NOT NULL,
                score REAL NOT NULL,
                max_marks REAL NOT NULL,
                accuracy REAL NOT NULL
            )
        """.trimIndent())

        db.execSQL("""
            CREATE TABLE IF NOT EXISTS test_questions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                test_id TEXT NOT NULL,
                question_id TEXT NOT NULL,
                subject TEXT NOT NULL,
                microtheme TEXT NOT NULL,
                question_text TEXT NOT NULL,
                options_json TEXT NOT NULL,
                correct_answer TEXT NOT NULL,
                user_answer TEXT,
                is_correct INTEGER,
                marked_for_review INTEGER NOT NULL,
                explanation TEXT NOT NULL,
                display_order INTEGER NOT NULL,
                FOREIGN KEY (test_id) REFERENCES tests (id) ON DELETE CASCADE
            )
        """.trimIndent())

        db.execSQL("CREATE INDEX IF NOT EXISTS index_test_questions_test_id ON test_questions (test_id)")
    }
}
```

The migration was verified via automated on-device tests (`DatabaseMigrationTest.kt`), proving that all pre-existing user attempts and bookmarks from version 2 remain 100% intact.

---

## 4. Question Selection & Subject Allocation

The question selector (`TestQuestionSelector.kt`) dynamically partitions the unattempted question bank into subjects, checks available inventory, applies random range sampling, balances totals, and shuffles the final set.

### Subject Allocation Ranges
The question bank contains 7 core subjects. FLT uses specified UPSC-weighted ranges. HLT applies exact half-length ranges using consistent nearest-integer rounding with a minimum of 1 question:

| Subject | JSON Subject Tag | FLT Range (Min - Max) | HLT Exact Range (Min - Max) | Rounding Method |
|---|---|:---:|:---:|---|
| **Polity** | `Polity` | 12 – 19 | **6 – 10** | `12/2 = 6`, `19/2 = 9.5 -> 10` |
| **Economy** | `Economy` | 14 – 22 | **7 – 11** | `14/2 = 7`, `22/2 = 11` |
| **Geography** | `Geography` | 9 – 18 | **5 – 9** | `9/2 = 4.5 -> 5`, `18/2 = 9` |
| **Modern History** | `Modern History` | 2 – 10 | **1 – 5** | `2/2 = 1`, `10/2 = 5` |
| **AMAC** | `AMAC` | 8 – 14 | **4 – 7** | `8/2 = 4`, `14/2 = 7` |
| **Science and Technology** | `Sci and Tech` | 10 – 14 | **5 – 7** | `10/2 = 5`, `14/2 = 7` |
| **Environment** | `Environment` | 15 – 21 | **8 – 11** | `15/2 = 7.5 -> 8`, `21/2 = 10.5 -> 11` |
| **Total Target** | | **100 questions** | **50 questions** | |

### Question Selection Steps
1. **Unattempted Filtering:** Filters `allQuestions` by checking `question_id !in attemptedQuestionIds`. Any question attempted in Practice or prior tests is strictly excluded.
2. **Shortage Check:** If total eligible questions < target count (100 for FLT, 50 for HLT), selection halts immediately and yields `SelectionResult.InsufficientQuestions` with the exact count available. No smaller test is created.
3. **Random Allocation:** For each subject, a target quota is chosen randomly within its `[min, max]` range, capped by the actual unattempted questions available in that subject.
4. **Balancing:**
   - If sum > target, questions are randomly trimmed across subjects with quota > minimum until exactly target count remains.
   - If sum < target, remaining slots are backfilled from unselected eligible questions across the entire question bank.
5. **Shuffling:** The final 100 or 50 selected questions are randomly shuffled so that questions are not grouped by subject.

---

## 5. Active Test Screen, Timer, and Exam Controls

The active exam interface (`TestQuestionScreen.kt`) provides a distraction-free, focused test environment:

```
┌────────────────────────────────────────────────────────┐
│  ⏱ 119:42                         [ ≡ Question Palette]│
├────────────────────────────────────────────────────────┤
│  Question 14 of 100                                    │
│                                                        │
│  Consider the following statements regarding the       │
│  Monetary Policy Committee (MPC):                      │
│                                                        │
│  1. It is an executive body constituted under...       │
│  2. The Governor of RBI acts as its ex-officio...      │
│                                                        │
│  [A] 1 only                                            │
│  [B] 2 only             <-- Neutral Accent Highlight   │
│  [C] Both 1 and 2                                      │
│  [D] Neither 1 nor 2                                   │
├────────────────────────────────────────────────────────┤
│  [ ★ Mark for Review ]       [ Next > ]     [ Submit ] │
└────────────────────────────────────────────────────────┘
```

### Key Functional Specifications
1. **Authoritative Decrementing Countdown Timer:**
   - Starts at 7200 seconds (120:00) for FLT, 3600 seconds (60:00) for HLT.
   - Managed in `TestViewModel` via a dedicated coroutine running on `Dispatchers.Default`.
   - Updates remaining seconds once per second.
   - Unaffected by recomposition, question navigation, or opening/closing the palette.
   - When remaining time reaches 0, the test is automatically submitted.
2. **Neutral Option Highlighting:**
   - Selected options are highlighted in a neutral Material 3 accent container.
   - No correctness indicators, green/red colors, or explanations are shown during an active test.
3. **Question Palette:**
   - Modal bottom sheet / dialog displaying a 5-column circular grid of question numbers.
   - Clear visual status:
     - **Red:** Unanswered / Visited without answering.
     - **Purple:** Marked for Review (whether answered or unanswered).
     - **Green:** Answered.
   - Tapping any question number instantly jumps to that question.
4. **Strict Lifecycle Abort (`ON_STOP`):**
   - Implemented via a `LifecycleEventObserver` attached to `LocalLifecycleOwner`.
   - If the user backgrounds the app, presses Home, switches tasks, or locks the screen (`ON_STOP`), the test is immediately aborted (unless it is a transient orientation change).
   - On abort, all temporary selections and review marks are discarded; no report is created; no Practice attempt history is inserted.
   - An explanatory abort banner is displayed on the `TestsLandingScreen`.
5. **Back Navigation Guard:**
   - Pressing the system back button opens a confirmation dialog warning that leaving will abort the test without saving. Confirming cleanly navigates back to the landing screen.

---

## 6. Scoring, Accuracy & Unified Practice Integration

### UPSC Scoring Formula
- **Marks per Correct Question:** `+2.0`
- **Penalty per Incorrect Question:** `-(2/3) ≈ -0.6667`
- **Marks for Unanswered Question:** `0.0`
- **Formula:**
  $$\text{Score} = (\text{Correct} \times 2.0) - \left(\text{Incorrect} \times \frac{2}{3}\right)$$
  The evaluated score is rounded to 2 decimal places.
- **Accuracy:**
  $$\text{Accuracy (\%)} = \begin{cases} \left(\frac{\text{Correct}}{\text{Attempted}}\right) \times 100, & \text{if Attempted} > 0 \\ 0.0, & \text{otherwise} \end{cases}$$

### Unified Attempt Persistence
When the user submits a test:
1. `TestRepositoryImpl.saveCompletedTest()` begins an atomic database transaction.
2. `tests` entity and `test_questions` snapshots are inserted.
3. For every question where `user_answer != null`:
   - An `AttemptedQuestionEntity` is created with `questionId`, `selectedAnswer`, `isCorrect`, and the submission timestamp.
   - It is upserted into `attempted_questions` via `PracticeDao.recordOrUpdateAttempt()`.
4. Questions left unanswered (`user_answer == null`) are **not** written to `attempted_questions`.
5. Because `AnalysisRepository` observes `PracticeDao.observeAllAttempts()`, global syllabus coverage, subject progress, and accuracy metrics automatically update throughout the app without requiring any additional synchronization code.

---

## 7. Reports & Past Tests Review

### Test Report Screen (`TestReportScreen.kt`)
Divided into two clean Material 3 tabs:
1. **Overall Performance Tab:**
   - Summary card: Score / Max Marks, Accuracy %, Total Time Taken, Attempted / Correct / Incorrect counts.
   - Marks Breakdown: Positive marks earned vs Negative marks penalized.
   - Subject-wise Performance Table: Breakdown of Total, Attempted, Correct, Accuracy %, and Marks per subject.
2. **Question Wise Tab:**
   - Filter chips: `All`, `Correct`, `Incorrect`, `Unanswered`, `Marked for Review`.
   - Detailed cards for each question showing question text, statements, user answer vs correct answer, review tag, and the full explanation.
   - **Post-Test Bookmarking:** A star icon on each question allows users to bookmark questions directly into the existing `BookmarkRepository` for future revision.

### Past Tests Screen (`PastTestsScreen.kt`)
- Accessible from the Tests landing screen.
- Lists all completed tests ordered by submission date (newest first).
- Displays test type badge (FLT / HLT), date & time, score / max marks, accuracy %, and duration.
- Clicking any card opens the complete, original report reconstructed from immutable database snapshots.

---

## 8. Files Created and Modified

### Created Files (15 files)
1. `app/src/main/java/com/csejudge/app/data/local/TestEntity.kt`: Room entity for completed test metadata.
2. `app/src/main/java/com/csejudge/app/data/local/TestQuestionEntity.kt`: Room entity for question snapshots.
3. `app/src/main/java/com/csejudge/app/data/local/TestDao.kt`: Room queries for tests and question snapshots.
4. `app/src/main/java/com/csejudge/app/data/model/TestModels.kt`: Domain models (`TestType`, `TestReport`, `SubjectTestPerformance`, `TestQuestionDetail`).
5. `app/src/main/java/com/csejudge/app/data/repository/TestRepository.kt`: Repository interface & implementation orchestrating transactions.
6. `app/src/main/java/com/csejudge/app/data/repository/TestQuestionSelector.kt`: Dynamic question selection algorithm.
7. `app/src/main/java/com/csejudge/app/ui/test/TestUiState.kt`: UI state models, palette states, and timer formatters.
8. `app/src/main/java/com/csejudge/app/ui/test/TestViewModel.kt`: Test session lifecycle, timer coroutine, and interactions.
9. `app/src/main/java/com/csejudge/app/ui/test/TestsLandingScreen.kt`: Tests landing menu (FLT, HLT, Past Tests).
10. `app/src/main/java/com/csejudge/app/ui/test/TestInstructionsScreen.kt`: Pre-test rules, instructions, and shortage warning.
11. `app/src/main/java/com/csejudge/app/ui/test/TestPaletteDialog.kt`: 5-column circular status palette dialog.
12. `app/src/main/java/com/csejudge/app/ui/test/TestQuestionScreen.kt`: Active test screen with timer, questions, and action bar.
13. `app/src/main/java/com/csejudge/app/ui/test/TestReportScreen.kt`: Two-tab comprehensive report screen with bookmarking.
14. `app/src/main/java/com/csejudge/app/ui/test/PastTestsScreen.kt`: Historical test list.
15. `stage13_tests_module_report.md`: This comprehensive documentation report.

### Created Test Files (3 files)
1. `app/src/test/java/com/csejudge/app/data/TestQuestionSelectorTest.kt`: Exhaustive unit tests for selection, quotas, shortages, and unattempted filtering.
2. `app/src/test/java/com/csejudge/app/ui/test/TestViewModelTest.kt`: Unit tests for timer, selection, scoring, persistence, and abort flows.
3. `app/src/androidTest/java/com/csejudge/app/ui/test/TestsModuleFlowInstrumentedTest.kt`: Instrumented on-device UI and navigation tests.

### Modified Files (7 files)
1. `app/src/main/java/com/csejudge/app/data/local/AppDatabase.kt`: Updated version to 3, registered new entities, added `MIGRATION_2_3`.
2. `app/src/main/java/com/csejudge/app/ui/home/HomeScreen.kt`: Inserted `TestsTile` at Position 2.
3. `app/src/main/java/com/csejudge/app/navigation/NavRoutes.kt`: Added routes for TestsLanding, TestInstructions, TestActive, TestReport, and PastTests.
4. `app/src/main/java/com/csejudge/app/navigation/AppNavHost.kt`: Wired test composables and back stacks.
5. `app/src/main/java/com/csejudge/app/MainActivity.kt`: Initialized `TestViewModel` and wired navigation callbacks.
6. `app/src/test/java/com/csejudge/app/navigation/NavRoutesTest.kt`: Verified all new route patterns.
7. `app/src/androidTest/java/com/csejudge/app/data/local/DatabaseMigrationTest.kt`: Added version 2 to version 3 migration test.

---

## 9. Verification & Testing Results

### 1. Compilation & Build Checks
- Command: `./gradlew assembleDebug`
- Result: **BUILD SUCCESSFUL** (Clean compile with zero errors).

### 2. Unit Testing (`./gradlew testDebugUnitTest`)
- Command: `./gradlew testDebugUnitTest`
- Result: **166 passed, 0 failed** across all test suites.
- Coverage includes:
  - `TestQuestionSelectorTest`:
    - FLT selection of exactly 100 questions within subject ranges.
    - HLT selection of exactly 50 questions within nearest-integer rounded ranges.
    - Exclusion of attempted questions.
    - Subject alias matching (`Sci and Tech` vs `Science and Technology`).
    - Graceful handling of question shortages without silent trimming.
    - Shuffling validation (questions not grouped by subject).
  - `TestViewModelTest`:
    - Active test initialization with 7200s (FLT) and 3600s (HLT).
    - Question navigation and palette jump.
    - Neutral option selection and review toggling.
    - Auto-submission on timer expiry.
    - Atomic persistence and unified practice history recording on submit.
    - Clean discard of state on abort.
    - Post-test bookmarking in report screen.
  - `NavRoutesTest`:
    - Route parsing and argument validation for all new destinations.

### 3. Physical Device Instrumented Testing (`./gradlew connectedDebugAndroidTest`)
- Target: Physical `Realme Narzo 90 5G (RMX5111)`, Android 16 (connected via ADB).
- Command: `./gradlew connectedDebugAndroidTest`
- Result: **68 tests executed — 67 Passed, 0 Failed, 1 Skipped** (`RealUpdateDeviceVerificationTest.verifyVersion2Update` intentionally skipped when running against the baseline 1,432 question bank).
- Highlights:
  - `DatabaseMigrationTest.testMigrationFromVersion2ToVersion3PreservesExistingDataAndEnablesTests`: Passed.
  - `HomeScreenTest.testTestsTileIsDisplayedAtPositionTwoAndClickable`: Passed.
  - `TestsModuleFlowInstrumentedTest.testNavigationFromHomeToTestsLandingAndBack`: Passed.
  - `TestsModuleFlowInstrumentedTest.testFullLengthTestInstructionsDisplayAndSpecs`: Passed.

### 4. Physical Device Installation & Live Execution
- Device: `Realme Narzo 90 5G` (`RMX5111`), Android 16.
- Commands Executed:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  adb shell am start -n com.csejudge.app/.MainActivity
  ```
- Verification:
  - Application installed with `Success` preserving existing device user data.
  - App launched into `MainActivity` with zero startup crashes or unhandled exceptions.
  - Inspected `adb logcat` filter for `com.csejudge.app`: Clean initialization of `AppDatabase`, `QuestionBankRepository`, and `TestViewModel`.

---

## 10. Summary Table of HLT Subject Allocations

As requested in Part B of Stage 13 specifications, below is the formal documentation of the Half Length Test (HLT) allocation ranges:

| Subject | Canonical Name | Bank Tag | FLT Range | Half Value | HLT Final Range |
|---|---|---|:---:|:---:|:---:|
| Polity | Polity | `Polity` | [12, 19] | [6.0, 9.5] | **[6, 10]** |
| Economy | Economy | `Economy` | [14, 22] | [7.0, 11.0] | **[7, 11]** |
| Geography | Geography | `Geography` | [9, 18] | [4.5, 9.0] | **[5, 9]** |
| Modern History | Modern History | `Modern History` | [2, 10] | [1.0, 5.0] | **[1, 5]** |
| AMAC | AMAC | `AMAC` | [8, 14] | [4.0, 7.0] | **[4, 7]** |
| Sci & Tech | Science and Technology | `Sci and Tech` | [10, 14] | [5.0, 7.0] | **[5, 7]** |
| Environment | Environment | `Environment` | [15, 21] | [7.5, 10.5] | **[8, 11]** |
| **Sum of Mins / Maxs** | | | **[70, 118]** | | **[36, 60]** |
| **Final Target** | | | **100** | | **50** |

*Note: For boundary values ending in `.5`, nearest-integer rounding rounds up (`9.5 -> 10`, `4.5 -> 5`, `7.5 -> 8`, `10.5 -> 11`). The algorithm balances the random selections to arrive at exactly 50 unique questions.*

---

## 11. Known Limitations & Next Steps

1. **Known Limitations:**
   - **Question Bank Inventory:** With the baseline 1,432-question bank, approximately 14 FLTs or 28 HLTs can be taken before the unattempted question pool is depleted. Once depleted, the shortage guard halts test creation and displays the count available.
   - **No Mid-Test App Switching:** As specified, exiting the application during an active test triggers an immediate abort to maintain strict exam integrity.
2. **Recommended Next Steps:**
   - Add optional sectional / subject-specific tests if requested in future stages.
   - Expand the question bank over time using the Stage 12 scalable workflow.
