# Stage 10: Analysis Data Layer, Calculations and ViewModel — Completion Report

**Project:** CSE Judge  
**Domain:** UPSC Civil Services Examination (Prelims) preparation  
**Stage:** Stage 10 (Analysis Data Layer, Calculations and ViewModel)  
**Target Hardware:** Physical Android device (Realme Narzo 90 5G / `RMX5111`)  
**Package Name:** `com.csejudge.app`  

---

## 1. Existing Question-Bank and Attempt-History Architecture

### Question-Bank Subsystem
- **Master Data Source:** Static JSON assets (`AMAC-qid.json`, `Economy-qid.json`, `Environment-qid.json`, `Geography-qid.json`, `Modern History-qid.json`, `Polity-qid.json`, `Sci and Tech-qid.json`).
- **Total Questions:** 1,432 immutable questions across 7 subjects.
- **Access & Indexing:** `QuestionBankRepositoryImpl` loads the JSON assets once on `Dispatchers.IO`, strictly validates schema and uniqueness, and builds immutable in-memory lookup maps (`questionsByIdMap`, `subjectIndexMap`, `subjectMicrothemeIndexMap`).
- **Separation Invariant:** Static question bank is treated as read-only source data, strictly separated from user attempt state.

### Attempt-History Subsystem (Room)
- **Table Name:** `attempted_questions` (`AttemptedQuestionEntity`).
- **Primary Key:** `question_id: String`. Each question has at most one current attempt record.
- **Columns:** `question_id`, `selected_answer`, `is_correct`, `attempted_at`.
- **Re-answering Semantics:** Updating an attempt (via `@Upsert upsertAttempt`) updates `selected_answer`, `is_correct`, and `attempted_at` in-place without creating duplicates or inflating total attempted counts.
- **Bookmark & Practice Independence:** Bookmarks and attempts are independent. Adding or removing bookmarks does not alter attempt history; skipping questions does not create attempt records.

---

## 2. New Analysis Data Models

Created strongly typed domain models in `com.csejudge.app.data.model.AnalysisModels.kt`:

### `AnalysisRecord` (Interface)
Defines the common contract for metrics at any hierarchical level:
- `val totalQuestions: Int`
- `val attemptedQuestions: Int`
- `val unattemptedQuestions: Int`
- `val coveragePercentage: Double`
- `val correctAnswers: Int`
- `val incorrectAnswers: Int`
- `val accuracyPercentage: Double?` *(null indicates N/A when `attemptedQuestions == 0`)*

### `OverallAnalysis` (Level 1)
- Implements `AnalysisRecord`.
- Represents overall progress across all 1,432 questions in the question bank.
- Factory method `OverallAnalysis.create(totalQuestions, correctAnswers, incorrectAnswers)`.

### `SubjectAnalysis` (Level 2)
- Implements `AnalysisRecord`.
- Holds `val subject: String` and metrics for one of the 7 subjects.
- Factory method `SubjectAnalysis.create(subject, totalQuestions, correctAnswers, incorrectAnswers)`.

### `MicrothemeAnalysis` (Level 3)
- Implements `AnalysisRecord`.
- Holds `val subject: String`, `val microtheme: String`, and metrics for a specific microtheme.
- Strictly excludes microthemes with zero questions.
- Factory method `MicrothemeAnalysis.create(subject, microtheme, totalQuestions, correctAnswers, incorrectAnswers)`.

### `FullAnalysisData`
- Immutable composite snapshot:
  - `val overall: OverallAnalysis`
  - `val subjects: List<SubjectAnalysis>`
  - `val microthemesBySubject: Map<String, List<MicrothemeAnalysis>>`
  - `val unmatchedAttemptIds: List<String>`

---

## 3. AnalysisRepository Implementation

Implemented in `com.csejudge.app.data.repository.AnalysisRepository.kt` (`AnalysisRepository` interface & `AnalysisRepositoryImpl`):

- **Question Bank Integration:** Reads static question totals and subject/microtheme mappings from `QuestionBankRepository` without re-parsing JSON or rebuilding in-memory indexes.
- **Room History Integration:** Obtains all current attempt records via `PracticeHistoryRepository.observeAllAttempts()` / `PracticeDao.observeAllAttempts()`.
- **Unmatched Attempt Handling:** If an attempt record contains an unknown question ID not present in the question bank, it is safely recorded in `FullAnalysisData.unmatchedAttemptIds`, triggers an optional warning callback, and is excluded from syllabus totals so it does not corrupt subject/microtheme consistency.
- **Asynchronous Execution:** Database queries and calculations execute on background dispatchers (`Dispatchers.IO` / `flowOn(ioDispatcher)`), completely off the main thread.
- **Factory Provider:** `AnalysisRepositoryImpl.create(context)` supports clean factory construction.

---

## 4. AnalysisViewModel and AnalysisUiState

Implemented in `com.csejudge.app.ui.analysis`:

### `AnalysisUiState`
```kotlin
data class AnalysisUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val overall: OverallAnalysis? = null,
    val subjects: List<SubjectAnalysis> = emptyList(),
    val availableSubjects: List<String> = emptyList(),
    val selectedSubject: String? = null,
    val microthemes: List<MicrothemeAnalysis> = emptyList()
)
```

### `AnalysisViewModel`
- Observes `analysisRepository.observeFullAnalysis()` combined with `_selectedSubject: MutableStateFlow<String?>`.
- Automatically selects the first available subject (`"AMAC"`) when none is explicitly selected.
- `selectSubject(subject: String)` updates the active subject and retrieves its microthemes from the existing analysis snapshot in memory without resetting attempt history or reloading the question bank.
- Lifecycle-aware coroutine scope (`viewModelScope.launch(ioDispatcher)`).
- Safe error handling with `.catch { e -> ... }` setting `errorMessage` and clearing `isLoading`.

---

## 5. Exact Formulas and Aggregation Rules

### A. Question Coverage
Measures how much of the syllabus has been attempted:
$$\text{Attempted Questions} = \text{Correct Answers} + \text{Incorrect Answers}$$
$$\text{Unattempted Questions} = \text{Total Questions} - \text{Attempted Questions}$$
$$\text{Coverage \%} = \frac{\text{Attempted Questions}}{\text{Total Questions}} \times 100 \quad (\text{0.0\% if Total} = 0)$$

### B. Answer Accuracy
Measures performance exclusively on attempted questions:
$$\text{Accuracy \%} = \frac{\text{Correct Answers}}{\text{Attempted Questions}} \times 100$$
- **Zero Attempts Rule:** When $\text{Attempted Questions} = 0$, `accuracyPercentage` returns `null` ($\text{N/A}$), **never 0.0%**.
- Unattempted questions are strictly excluded from the accuracy denominator.

### C. Precision
Calculations use standard 64-bit floating point precision (`Double`) without premature intermediate rounding. Presentation rounding is deferred to Stage 11 UI components.

---

## 6. Reactive Update Behaviour

- **Single Source of Truth:** `PracticeDao.observeAllAttempts()` returns `Flow<List<AttemptedQuestionEntity>>`.
- **New Attempt:** Inserting an attempt emits a new list of entities, prompting `AnalysisRepository` to re-aggregate and emit an updated `FullAnalysisData`.
- **Re-answering:** Updating an existing question attempt via `@Upsert` triggers the Flow; the question ID remains the same, `attemptedQuestions` does not increase, while `correctAnswers`, `incorrectAnswers`, and `accuracyPercentage` recalculate reactively.
- **Bookmark Actions:** Adding or removing bookmarks modifies `bookmarked_questions`, leaving `attempted_questions` unchanged; analysis figures remain completely stable.
- **Question Skipping:** Skipping does not write to Room; coverage and accuracy figures remain unaffected.

---

## 7. Data Consistency Validation Results

Implemented `AnalysisValidator` (`com.csejudge.app.data.repository.AnalysisValidator.kt`) executing automated checks on every analysis snapshot:

1. **Record-level Invariants:**
   - $\text{Attempted} = \text{Correct} + \text{Incorrect}$
   - $\text{Total} = \text{Attempted} + \text{Unattempted}$
   - When $\text{Attempted} = 0$, $\text{Accuracy} = \text{null}$
2. **Hierarchical Consistency Invariants:**
   - $\sum \text{Subject Totals} = \text{Overall Total} = 1,432$
   - $\sum \text{Subject Attempted} = \text{Overall Attempted}$
   - $\sum \text{Subject Correct} = \text{Overall Correct}$
   - $\sum \text{Subject Incorrect} = \text{Overall Incorrect}$
   - For every subject $S$:
     - $\sum_{m \in S} \text{Microtheme Totals} = \text{Subject Total}$
     - $\sum_{m \in S} \text{Microtheme Attempted} = \text{Subject Attempted}$
     - $\sum_{m \in S} \text{Microtheme Correct} = \text{Subject Correct}$
     - $\sum_{m \in S} \text{Microtheme Incorrect} = \text{Subject Incorrect}$
3. **Domain Content Invariants:**
   - Dynamic discovery matches all 7 subjects: AMAC (149), Economy (243), Environment (248), Geography (233), Modern History (127), Polity (252), Sci and Tech (180).
   - Modern History retains `From 1857 to 1884 CE` (5 questions) and strictly excludes `From 1757 to 1884 CE`.
   - Economy contains `Macro economy` (5 questions).

---

## 8. Unit Test Results

Command: `./gradlew testDebugUnitTest`  
**Total Tests Completed:** 126  
**Passed:** 126  
**Failed:** 0  
**Ignored:** 0  

### Test Suites Covering Analysis Data Layer:
1. **`AnalysisValidatorTest`:**
   - `testValidRecordPassesValidation` — PASSED
   - `testZeroAttemptRecordHasNullAccuracyAndZeroCoverage` — PASSED
   - `testRecordWithCorruptedAttemptedSumFailsValidation` — PASSED
   - `testRecordWithCorruptedTotalSumFailsValidation` — PASSED
   - `testZeroAttemptWithNonNullAccuracyFailsValidation` — PASSED
   - `testValidFullAnalysisPassesValidation` — PASSED
   - `testFullAnalysisWithSubjectTotalMismatchFailsValidation` — PASSED
   - `testFullAnalysisWithMicrothemeTotalMismatchFailsValidation` — PASSED
2. **`AnalysisRepositoryTest`:**
   - `testOverallAnalysisWithNoAttempts` — PASSED
   - `testOverallAnalysisWithOnlyCorrectAttempts` — PASSED
   - `testOverallAnalysisWithOnlyIncorrectAttempts` — PASSED
   - `testOverallAnalysisWithMixedAttempts` — PASSED
   - `testSubjectWiseAnalysisContainsAllSevenSubjectsWithExactTotals` — PASSED
   - `testSubjectWiseAttemptsAndAccuracyCalculations` — PASSED
   - `testMicrothemeDynamicDiscoveryAndTotals` — PASSED
   - `testMicrothemeAttemptAggregationAndAccuracy` — PASSED
   - `testReactiveFlowEmitsUpdatesWhenAttemptsChange` — PASSED
   - `testReansweringQuestionUpdatesAccuracyWithoutInflatingAttemptCount` — PASSED
   - `testSkippingDoesNotCountAsAttempted` — PASSED
   - `testHierarchicalConsistencyInvariantsAcrossAllLevels` — PASSED
   - `testUnmatchedAttemptIdIsSafelyTrackedAndDoesNotCorruptHierarchy` — PASSED
3. **`AnalysisViewModelTest`:**
   - `testInitialLoadingAndSuccessfulStateEmission` — PASSED
   - `testSelectSubjectUpdatesMicrothemesWithoutAffectingTotals` — PASSED
   - `testReactiveUpdatesReflectInViewModelState` — PASSED
   - `testRepositoryFailureEmitsErrorMessage` — PASSED

---

## 9. Regression Test Results

### Unit Test Regression
- All pre-existing test suites (Stages 1–9) passed without regressions:
  - `NavRoutesTest`
  - `ThemeRepositoryImplTest`
  - `QuestionBankRepositoryTest`
  - `PracticeHistoryRepositoryTest`
  - `PracticeHistoryReattemptTest`
  - `PracticeViewModelTest`
  - `PracticeViewModelBookmarkTest`
  - `PracticeSessionTest`
  - `Stage6QualityAssuranceTest`
  - `ThemeStylingTest`
  - `ThemeViewModelTest`
  - `BookmarkRepositoryTest`
  - `BookmarkViewModelTest`

### Connected Instrumented Test Regression
Command: `./gradlew connectedDebugAndroidTest` on target hardware `RMX5111` (Realme Narzo 90 5G).  
**Total Tests Completed:** 50  
**Passed:** 50  
**Failed:** 0  
- Verified `AppNavigationTest`, `PracticeDaoTest`, `DatabaseMigrationTest`, `BookmarkDaoTest`, `PracticeQuestionScreenTest`, `PracticeFilterScreenTest`, `BookmarkFlowInstrumentedTest`, and `HomeScreenTest`.

---

## 10. Physical Device Verification

- **Target Device:** Realme Narzo 90 5G (`RMX5111`, serial `CMUSOJ9DYPNJEANV`).
- **Build Command:** `./gradlew assembleDebug` (Exit code: 0).
- **Installation:** `adb install -r app/build/outputs/apk/debug/app-debug.apk` (Exit code: 0, `Success`). Existing user data preserved.
- **Application Launch:** `adb shell am start -n com.csejudge.app/.MainActivity` (Exit code: 0).
- **Window State Verification:**
  - `mCurrentFocus=Window{dda6692 u0 com.csejudge.app/com.csejudge.app.MainActivity}`
  - `mFocusedApp=ActivityRecord{132725889 u0 com.csejudge.app/.MainActivity t5064}`
- **Logcat Inspection:** Logcat inspected for `com.csejudge.app`. Zero crashes, zero ANRs, zero `AndroidRuntime` exceptions.
- **Feature Stability:** Practice Problems, Bookmarked Questions, theme toggling, and attempt history persistence remain completely functional.

---

## 11. Exact Files Created and Modified

### Created Files:
1. `app/src/main/java/com/csejudge/app/data/model/AnalysisModels.kt` — Data models (`AnalysisRecord`, `OverallAnalysis`, `SubjectAnalysis`, `MicrothemeAnalysis`, `FullAnalysisData`, `AnalysisCalculations`).
2. `app/src/main/java/com/csejudge/app/data/repository/AnalysisValidator.kt` — Domain invariant validator for calculations and hierarchy consistency.
3. `app/src/main/java/com/csejudge/app/data/repository/AnalysisRepository.kt` — `AnalysisRepository` interface and `AnalysisRepositoryImpl`.
4. `app/src/main/java/com/csejudge/app/ui/analysis/AnalysisUiState.kt` — UI state representation for analysis.
5. `app/src/main/java/com/csejudge/app/ui/analysis/AnalysisViewModel.kt` — ViewModel coordinating reactive analysis data.
6. `app/src/test/java/com/csejudge/app/data/AnalysisValidatorTest.kt` — Unit tests for data invariants and boundary validation.
7. `app/src/test/java/com/csejudge/app/data/AnalysisRepositoryTest.kt` — Unit tests for repository calculations, hierarchy, and reactivity.
8. `app/src/test/java/com/csejudge/app/ui/analysis/AnalysisViewModelTest.kt` — Unit tests for ViewModel state and selection behavior.
9. `stage10_analysis_data_report.md` — This completion report.

### Modified Files:
1. `app/src/main/java/com/csejudge/app/data/local/PracticeDao.kt` — Added `getAllAttempts()` and `observeAllAttempts()`.
2. `app/src/main/java/com/csejudge/app/data/repository/PracticeHistoryRepository.kt` — Added `getAllAttempts()` and `observeAllAttempts()`.
3. `app/src/test/java/com/csejudge/app/data/PracticeHistoryRepositoryTest.kt` — Implemented new methods on test fake and added verification test.
4. `app/src/test/java/com/csejudge/app/data/PracticeHistoryReattemptTest.kt` — Implemented new methods on test fake.
5. `app/src/test/java/com/csejudge/app/ui/practice/PracticeViewModelTest.kt` — Implemented new methods on test fake.
6. `app/src/test/java/com/csejudge/app/ui/practice/PracticeViewModelBookmarkTest.kt` — Implemented new methods on test fake.
7. `app/src/test/java/com/csejudge/app/ui/practice/Stage6QualityAssuranceTest.kt` — Implemented new methods on test fake.
8. `app/src/test/java/com/csejudge/app/ui/bookmark/BookmarkViewModelTest.kt` — Implemented new methods on test fake.
9. `app/src/androidTest/java/com/csejudge/app/data/local/PracticeDaoTest.kt` — Added test for `getAllAttempts()` and `observeAllAttempts()`.
10. `app/src/androidTest/java/com/csejudge/app/navigation/AppNavigationTest.kt` — Implemented new methods on test fake.
11. `app/src/androidTest/java/com/csejudge/app/ui/bookmark/BookmarkFlowInstrumentedTest.kt` — Implemented new methods on test fake.

---

## 12. Unresolved Issues

- **None.** All Stage 10 requirements and invariants are fully implemented, verified, and passing across all unit tests, instrumented tests, and real hardware runs.
- **Stage Boundary:** Analysis UI screen, charts, and home-screen tile were excluded in strict accordance with Stage 10 scope restrictions, ready for implementation in Stage 11.
