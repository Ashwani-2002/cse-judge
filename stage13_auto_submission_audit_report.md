# CSE Judge — Stage 13: Auto-Submission at Test Timer Expiry Audit Report

**Date:** 15 November 2026  
**Module:** Stage 13 — Tests Module (FLT, HLT & Past Tests)  
**Audit Focus:** Auto-submission verification at `00:00` countdown timer expiry, evaluation accuracy, Room transaction persistence, Practice history integration, and concurrency / lifecycle race condition resilience.

---

## 1. Executive Summary

A comprehensive code audit and empirical verification of the auto-submission mechanism at test timer expiry (`00:00`) was conducted for both Full Length Tests (FLT, 100 questions, 120 minutes) and Half Length Tests (HLT, 50 questions, 60 minutes).

### Key Audit Findings:
1. **Critical Lifecycle Window Identified & Patched:** Previously, `abortTest()` responded unconditionally to Android Activity `ON_STOP`. If the user backgrounded or locked their device in the small execution window during persistence after timer expiry, the test could have been marked aborted and wiped. A strict lifecycle guard was added to reject abort operations whenever submission is in progress (`isSubmitting == true`, `remainingSeconds <= 0`, or atomic submission guard engaged).
2. **Post-Expiry Interaction Elimination:** Option selection (`selectOption`) and review toggle (`toggleMarkForReview`) were hardened to reject any state mutations once `remainingSeconds <= 0` or while submission is ongoing.
3. **Modal Dialog Auto-Dismissal:** All active modal overlays (`QuestionPaletteDialog`, `SubmitConfirmationDialog`, `AbortConfirmationDialog`) are forcibly dismissed the moment auto-submission initiates, ensuring seamless automatic navigation to `TestReportScreen`.
4. **Strict Single-Submission Idempotency:** An atomic `AtomicBoolean` compare-and-set (`compareAndSet(false, true)`) ensures that simultaneous triggers (e.g. user tapping submit at the exact millisecond the countdown reaches `00:00`) cannot result in duplicate records.
5. **Exact Evaluation & Practice Integration:** All 11 required behavioral invariants were confirmed by both unit tests and physical device instrumented tests.

---

## 2. End-to-End Execution Trace

```
Countdown Timer (startTimer coroutine)
         │
         ▼
Countdown decrements: while (isActive && newSeconds > 0)
         │
         ▼
Remaining seconds reaches 00:00 (newSeconds == 0)
         │
         ├──> Loop breaks immediately
         ├──> Rejects any further option selections or review markings
         ├──> Cancels timer job
         └──> Invokes submitTest(isAutoSubmit = true)
                     │
                     ▼
         Atomic CAS Guard: isSubmittingGuard.compareAndSet(false, true)
         (If false -> aborts duplicate call; if true -> proceeds)
                     │
                     ▼
         UI State Update:
         - isSubmitting = true
         - isTimerActive = false
         - isPaletteOpen = false (dismisses palette)
         - showSubmitConfirmation = false (dismisses submit dialog)
         - showAbortConfirmation = false (dismisses abort dialog)
                     │
                     ▼
         Evaluation & Scoring:
         - Iterate over all questions in test sequence
         - If selected == null -> "UNANSWERED", 0 marks, unansweredCount++
         - If selected == correct -> "CORRECT", +2.0 marks, correctCount++
         - If selected != correct -> "INCORRECT", -(2/3) marks, incorrectCount++
         - Total Score = (correctCount * 2.0) - (incorrectCount * (2.0 / 3.0))
         - Accuracy = (correctCount / attemptedCount) * 100.0 (or -1.0 if 0 attempted)
                     │
                     ▼
         Room Database Atomic Transaction (TestRepository.saveCompletedTest):
         1. testDao.insertTest(testEntity)
         2. testDao.insertTestQuestions(questionEntities)
         3. practiceDao.upsertAttempt(attempt) FOR ANSWERED QUESTIONS ONLY
            (Unanswered questions are strictly excluded from practice history)
                     │
                     ▼
         Post-Persistence State Update:
         - isSubmitting = false
         - isCompleted = true
         - completedTestId = testId
         - currentReport = testRepository.getTestReport(testId)
                     │
                     ▼
         Navigation (LaunchedEffect in TestQuestionScreen):
         - Observes isCompleted == true && completedTestId != null
         - Executes onTestCompleted(completedTestId)
         - Navigates to Screen.TestReport (popUpTo TestsLanding)
```

---

## 3. Concurrency & Race Condition Audit

| Potential Race Condition | Vulnerability Assessment | Architectural Mitigation Implemented |
|---|---|---|
| **User selects an option at the exact millisecond of expiry** | Answer might be half-written or written after expiry. | `TestViewModel.selectOption` explicitly validates `state.remainingSeconds > 0 && !state.isSubmitting && !isSubmittingGuard.get()`. If the countdown is at 0, selections are completely ignored. If selected at `T=1s`, the mutation is recorded before the CAS lock and evaluated. |
| **Expiry while Question Palette is open** | Palette could linger over the completed test or block navigation. | `submitTest` forcefully sets `isPaletteOpen = false`. In `TestQuestionScreen`, dialog is conditionally rendered based on `uiState.isPaletteOpen`. |
| **Expiry while Manual Submit / Abort Dialog is open** | Dialog could remain open over the report screen or trigger secondary actions. | `submitTest` sets `showSubmitConfirmation = false` and `showAbortConfirmation = false`. |
| **Manual Submit and Auto-Submit trigger simultaneously** | Duplicate test entities and duplicate practice attempts created. | Guarded by `AtomicBoolean` CAS (`compareAndSet(false, true)`). The first caller takes the lock; the second caller immediately returns. |
| **Phone locked or app sent to background during persistence window** | `ON_STOP` lifecycle event calls `abortTest()`, which could wipe answers and mark test aborted during database write. | `TestViewModel.abortTest()` now checks: `if (state.questions.isEmpty() \|\| state.isCompleted \|\| state.isSubmitting \|\| state.remainingSeconds <= 0 \|\| isSubmittingGuard.get()) return`. The abort is strictly rejected once time expires or submission begins. |
| **Timer coroutine unexpected cancellation / restart** | Timer stopped prematurely or restarted multiple times. | Single structured `timerJob` in `viewModelScope`. Cancelled cleanly in `submitTest()` and `abortTest()`. Cannot start multiple instances. |
| **Database insertion failure** | Error leaves UI hanging in `isSubmitting = true`. | Handled inside `runCatching` / `try-catch`. In case of error, `isSubmittingGuard.set(false)`, `isSubmitting = false`, and `errorMessage` is surfaced. |

---

## 4. Verification Against Required Behaviors

| Invariant | Description | Verification Method | Status |
|---|---|---|---|
| **1** | Automatically submits at `00:00` without user interaction | Unit tests + Instrumented tests on Realme Narzo 90 5G | **VERIFIED** |
| **2** | No additional answer selection accepted after expiry | `testNoAdditionalSelectionsAcceptedAfterExpiryOrDuringSubmitting` | **VERIFIED** |
| **3** | All answers selected before expiry are included | `testLastSelectedAnswerImmediatelyBeforeExpiryIsIncluded` | **VERIFIED** |
| **4** | Unanswered questions receive zero marks | `testAutoSubmitEvaluatesCorrectAndIncorrectScoring` | **VERIFIED** |
| **5** | Correct answers receive +2 marks | `testAutoSubmitEvaluatesCorrectAndIncorrectScoring` | **VERIFIED** |
| **6** | Incorrect answers receive -(2/3) marks | `testAutoSubmitEvaluatesCorrectAndIncorrectScoring` | **VERIFIED** |
| **7** | Completed test report is saved | `testFltAutoSubmitsWhenTimerReachesZero` & `testHltAutoSubmitsWhenTimerReachesZero` | **VERIFIED** |
| **8** | Test appears in Past Tests | `testDao.observeCompletedTests()` queries database | **VERIFIED** |
| **9** | Answered questions recorded in Practice attempt history | `testUnansweredQuestionsReceiveZeroAndDoNotEnterPracticeHistory` | **VERIFIED** |
| **10** | Unanswered questions remain unattempted in Practice | Checked against `practiceDao.getAllAttempts()` | **VERIFIED** |
| **11** | Test is submitted exactly once (idempotent) | `testSubmissionIdempotencePreventsDuplicateReports` | **VERIFIED** |

---

## 5. Automated Test Suite Results

### 5.1 Unit Tests (`./gradlew testDebugUnitTest`)
- **Total Tests Executed:** 19 tests in `TestViewModelTest`
- **Result:** **19 Passed, 0 Failed, 0 Skipped (100% Success)**
- **Specific Auto-Submission Tests Added:**
  - `testFltAutoSubmitsWhenTimerReachesZero`: Verified FLT (100 questions, 120m) auto-submits at `00:00`.
  - `testHltAutoSubmitsWhenTimerReachesZero`: Verified HLT (50 questions, 60m) auto-submits at `00:00`.
  - `testAutoSubmitEvaluatesCorrectAndIncorrectScoring`: Verified exact marks (+2, -2/3, 0), accuracy, and counts.
  - `testUnansweredQuestionsReceiveZeroAndDoNotEnterPracticeHistory`: Verified unanswered questions never enter `practiceDao`.
  - `testLastSelectedAnswerImmediatelyBeforeExpiryIsIncluded`: Verified answer selected at `T=1s` before expiry is persisted.
  - `testNoAdditionalSelectionsAcceptedAfterExpiryOrDuringSubmitting`: Verified selections after 00:00 are rejected.
  - `testTimerExpiryWhilePaletteOrAbortDialogIsOpenDismissesDialogAndSubmits`: Verified modal dismissal on expiry.
  - `testAppBackgroundingDuringSubmissionDoesNotAbortTest`: Verified `ON_STOP` does not wipe test in submission.
  - `testSubmissionIdempotencePreventsDuplicateReports`: Verified CAS prevents duplicate persistence.

### 5.2 Connected Physical Device Tests (`./gradlew connectedDebugAndroidTest`)
- **Device Target:** Realme Narzo 90 5G (`RMX5111`, Android 16)
- **Suite:** `com.csejudge.app.ui.test.TestsModuleFlowInstrumentedTest`
- **Results:**
  - `testHomeScreenTestsTilePlacementAndNavigationToLanding`: **PASSED (1.666s)**
  - `testHalfLengthTestInstructionsAndStartFlow`: **PASSED (6.107s)**
  - `testPastTestsEmptyStateAndBackNavigation`: **PASSED (1.908s)**
  - `testAutoSubmissionAtTimerExpiryNavigatesToReportAndRecordsResults`: **PASSED (3.496s)**
- **Total:** **4 Passed, 0 Failed, 0 Skipped (100% Success)**

---

## 6. Physical Device Deployment & Runtime Verification

Per Section 4.6 of `GEMINI.md`:
1. **Device Check:** Connected via ADB (`CMUSOJ9DYPNJEANV`).
2. **Build:** `./gradlew assembleDebug` passed (`BUILD SUCCESSFUL in 2s`).
3. **Artifact Location:** [app-debug.apk](file:///home/ashwani/Desktop/development/CSE%20Judge/app/build/outputs/apk/debug/app-debug.apk) (15MB).
4. **Device Installation:** `adb install -r app/build/outputs/apk/debug/app-debug.apk` succeeded with `Success`.
5. **Package Verification:** `package:com.csejudge.app` confirmed active.
6. **Launch:** `adb shell am start -n com.csejudge.app/.MainActivity` started successfully.
7. **Logcat Diagnostic:** Zero crashes, zero runtime exceptions.
8. **Window Focus:** `mCurrentFocus=Window{4010da1 u0 com.csejudge.app/com.csejudge.app.MainActivity}`.

---

## 7. Audit Conclusion

The auto-submission mechanism at test timer expiry (`00:00`) for Full Length Tests and Half Length Tests is **fully verified, production-ready, and resilient against concurrency, lifecycle events, and duplicate submissions**.
