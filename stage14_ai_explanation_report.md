# CSE Judge — Stage 14: Google AI Mode Explanation Feature Delivery Report

## 1. Executive Summary

Stage 14 introduces the **Google AI Mode Explanation** capability to CSE Judge, enabling exam aspirants to request an independent, comprehensive, and conceptually structured AI analysis for any UPSC Civil Services Preliminary Examination multiple-choice question.

This feature functions strictly without:
- Gemini API keys or tokens.
- Google AI / Vertex AI API integration.
- Backend proxy or cloud servers.
- Paid AI subscriptions.
- Application-side AI inference or heavy dependencies.

Instead, CSE Judge constructs a structured prompt payload directly from local question data (or immutable historical test snapshots), properly encodes the query once into Google's dedicated AI Mode search URL (`https://www.google.com/search?udm=50&q={ENCODED_TEXT}`), and delegates rendering to the user's default external browser via an Android `ACTION_VIEW` intent.

---

## 2. Architecture & Design

### 2.1 Separation of Concerns
The implementation is organized into modular, reusable components adhering to clean architecture and unidirectional data flow (UDF):

1. **`AiExplanationPromptBuilder` (`com.csejudge.app.ai`)**:
   - Pure domain logic for building structured prompts.
   - Preserves complete question statements, line breaks, statement numbering, all 4 options, and recorded official correct answers.
   - Provides overloaded factory methods for both `Question` (Practice and Bookmark modules) and `TestQuestionDetail` (Test Reports).
   - Validates question integrity prior to prompt generation (checks for empty statements or blank options).

2. **`AiExplanationUrlGenerator` (`com.csejudge.app.ai`)**:
   - Generates the standard Google AI Mode URL structure with `udm=50`.
   - Uses single-time UTF-8 percent-encoding (`%20` for spaces, `%0A` for newlines, `%E0%A4%...` for Unicode/Hindi, and reserved character percent-encoding).
   - Fully compatible with pure JVM unit test runners without Android SDK stubs.

3. **`AiExplanationLauncher` (`com.csejudge.app.ai`)**:
   - Android runtime integration layer.
   - Fired when the user taps the AI Explanation button.
   - Validates input, constructs the URL, builds an external browser `ACTION_VIEW` intent with `CATEGORY_BROWSABLE`, and starts the activity.
   - Robust error handling: catches `ActivityNotFoundException` (when no browser is installed) and generic launch failures, displaying user-friendly `Toast` notifications instead of crashing or failing silently.

4. **`AiExplanationButton` & `AiSparklesIcon` (`com.csejudge.app.ui.ai`)**:
   - Reusable Jetpack Compose Material 3 button component.
   - Renders a dual-sparkle AI icon on a resolution-independent `Canvas` with astroid concave star paths.
   - Distinct outlined visual design with subtle primary container tint (`PracticeColors.PrimaryNavyLight`), clearly distinguishing it from primary solid action buttons (e.g., "Next") and neutral navigation buttons (e.g., "Filters").
   - Dynamically responds to theme switching, supporting both Light Mode and Dark Mode via `PracticeColors`.

---

## 3. Query Construction Format

The query is formatted as an independent prompt without including the pre-stored answer explanation, prompting Google AI to conduct an independent conceptual evaluation:

```
Explain the following UPSC Civil Services Preliminary Examination multiple-choice question in detail.

Subject: [Subject]
Microtheme: [Microtheme]

QUESTION:
[Complete question statement, preserving all statements and their original order]

OPTIONS:
A. [Option A]
B. [Option B]
C. [Option C]
D. [Option D]

OFFICIAL/RECORDED CORRECT ANSWER:
[Correct answer]

INSTRUCTIONS:
1. Explain the question in a detailed and conceptually clear manner.
2. Explain the underlying concepts required to solve the question.
3. Evaluate all four options individually.
4. Explain why the correct option is correct.
5. Explain why each incorrect option is incorrect.
6. Explain relevant facts, provisions, examples, or conceptual distinctions wherever applicable.
7. Highlight important UPSC examination takeaways.
8. Do not invent facts. Clearly mention any ambiguity or factual uncertainty.
9. Structure the explanation using headings and bullet points for readability.
```

---

## 4. URL Generation & Encoding Approach

Google AI Mode is targeted via Google's `udm=50` URL parameter:
`https://www.google.com/search?udm=50&q={ENCODED_TEXT}`

### Encoding Characteristics:
- **Encoding Scheme**: `java.net.URLEncoder.encode(query, "UTF-8")` followed by `.replace("+", "%20")`.
- **Spaces**: Encoded as `%20` (standard percent-encoded space).
- **Newlines**: Preserved and encoded as `%0A`.
- **Special Characters**: Reserved characters (`?`, `&`, `=`, `"`, `%`, `/`, `#`, etc.) are percent-encoded (`%3F`, `%26`, `%3D`, `%22`, `%25`, `%2F`, `%23`) to ensure URL query string boundaries are preserved.
- **Literal Plus Signs**: An original `+` in question content is safely converted to `%2B` by `URLEncoder`, ensuring `.replace("+", "%20")` only touches actual space characters.
- **Unicode & Hindi**: Standard multi-byte UTF-8 percent-encoding (`%E0%A4%...`) guarantees perfect fidelity across regional scripts.
- **Single-time Encoding**: The query is encoded strictly once; decoding the query parameter once in tests recovers the exact original text string.

---

## 5. UI Placement & Module Integrations

### 5.1 Practice Question Screen (`PracticeQuestionScreen.kt`)
- Placed immediately below the `SolutionSection` card when `uiState.isAnswered == true`.
- Automatically shown after submitting an answer or viewing an answered question.
- Positioned inside the scrollable column above bottom navigation padding, ensuring it never overlaps or interferes with the fixed bottom action button ("Next" / "Skip").

### 5.2 Bookmarked Questions Review (`BookmarkQuestionScreen.kt`)
- Placed immediately below `BookmarkSolutionSection` when `qState.isAttempted == true`.
- Enables reviewing previously attempted/recorded questions and invoking AI explanation directly during bookmark revision workflows.

### 5.3 Test Report — Question Wise (`TestReportScreen.kt`)
- Placed immediately below each question's explanation card within `ReportQuestionCard` in the Question Wise report.
- Works for newly completed tests as well as historical tests opened through **Past Tests**.
- Completely reconstructed from immutable `TestQuestionDetail` / `TestQuestionEntity` snapshots, ensuring 100% decoupling from future question-bank file or schema updates.

### 5.4 Active Test Screen Exclusion (`TestQuestionScreen.kt`)
- Adhering strictly to constraints, the AI explanation button is **not** present on the active test screen, ensuring test simulation integrity and preventing unauthorized external lookups during active mocks.

---

## 6. Files Created & Modified

### Created Files:
1. `app/src/main/java/com/csejudge/app/ai/AiExplanationPromptBuilder.kt`
   - Prompt construction logic, parameter validation, and domain model overloads.
2. `app/src/main/java/com/csejudge/app/ai/AiExplanationUrlGenerator.kt`
   - URL encoding and Google AI Mode URL builder (`udm=50`).
3. `app/src/main/java/com/csejudge/app/ai/AiExplanationLauncher.kt`
   - External browser `ACTION_VIEW` intent launcher with user-friendly error handling.
4. `app/src/main/java/com/csejudge/app/ui/ai/AiExplanationButton.kt`
   - Compose Material 3 button with custom Canvas dual-sparkle icon and theme awareness.
5. `app/src/test/java/com/csejudge/app/ai/AiExplanationTest.kt`
   - Automated unit tests covering URL generation, encoding, prompt format, Unicode, and snapshots.

### Modified Files:
1. `app/src/main/java/com/csejudge/app/ui/practice/PracticeQuestionScreen.kt`
   - Added `AiExplanationButton` immediately below `SolutionSection`.
2. `app/src/main/java/com/csejudge/app/ui/bookmark/BookmarkQuestionScreen.kt`
   - Added `AiExplanationButton` immediately below `BookmarkSolutionSection`.
3. `app/src/main/java/com/csejudge/app/ui/test/TestReportScreen.kt`
   - Added `AiExplanationButton` immediately below explanation card in `ReportQuestionCard`.

---

## 7. Automated Test Results

A dedicated suite of 14 unit tests in `AiExplanationTest` was executed alongside the existing test suite:

| Test Case | Objective | Result |
| :--- | :--- | :--- |
| `test1_CorrectUrlGeneration` | Verifies endpoint starts with `https://www.google.com/search?udm=50&q=` | **PASSED** |
| `test2_CorrectEncodingOfSpaces` | Verifies spaces encoded as `%20` without literal spaces or `+` | **PASSED** |
| `test3_CorrectEncodingOfMultipleQuestionStatementsAndNewlines` | Verifies newlines encoded as `%0A` preserving line breaks | **PASSED** |
| `test4_CorrectEncodingOfSpecialCharacters` | Verifies `%25`, `%26`, `%3D`, `%3F`, `%2F`, `%23`, `%22` encoding | **PASSED** |
| `test5_CorrectHandlingOfUnicode` | Verifies Hindi UTF-8 multi-byte percent-encoding roundtrip | **PASSED** |
| `test6_InclusionOfAllFourOptions` | Verifies Options A, B, C, D present under `OPTIONS:` | **PASSED** |
| `test7_InclusionOfCorrectAnswer` | Verifies correct answer included under `OFFICIAL/RECORDED CORRECT ANSWER:` | **PASSED** |
| `test8_InclusionOfSubjectAndMicrotheme` | Verifies Subject and Microtheme headers present | **PASSED** |
| `test9_CorrectQueryConstructionForPracticeQuestions` | Verifies full prompt structure and exclusion of stored solution | **PASSED** |
| `test10_CorrectQueryConstructionUsingHistoricalTestSnapshots` | Verifies prompt construction from `TestQuestionDetail` snapshots | **PASSED** |
| `test11_UrlGenerationExactlyOnceWithoutDoubleEncoding` | Verifies single-time encoding (no `%2520`) and decode roundtrip | **PASSED** |
| `testValidation_EmptyQuestionStatementReturnsError` | Verifies validation flags empty/blank question statements | **PASSED** |
| `testValidation_BlankOptionReturnsError` | Verifies validation flags missing/blank option items | **PASSED** |
| `testValidation_ValidDataReturnsNull` | Verifies validation accepts valid questions | **PASSED** |

**Full Project Test Suite Result**:
- Command: `./gradlew testDebugUnitTest`
- Outcome: **BUILD SUCCESSFUL** (All 26 test tasks passed with zero failures).

---

## 8. Physical Device Verification

Verification was executed on the connected physical device (**Realme Narzo 90 5G / `RMX5111`**, Android 14):

1. **Build & Streamed Installation**:
   - Built debug APK via `./gradlew assembleDebug`.
   - Installed via `adb install -r app/build/outputs/apk/debug/app-debug.apk` (`Performing Streamed Install -> Success`).
   - Launched main activity via `adb shell am start -n com.csejudge.app/.MainActivity`.
   - Inspected `logcat`: Zero startup crashes, zero unhandled exceptions.

2. **Practice Flow Verification**:
   - Navigated to Practice -> Started session.
   - Selected option "A" on Modern History question (Lord Curzon policies).
   - Answer evaluated, solution card appeared.
   - Scrolled to the bottom of the solution: `AI explanation` button with sparkles icon displayed prominently.
   - Tapped `AI explanation`: Default external browser (`Brave Browser` / `ChromeTabbedActivity`) opened immediately.
   - Inspected `logcat`:
     ```
     capturedLink=https://www.google.com/search?udm=50&q=Explain%20the%20following%20UPSC%20Civil%20Services%20Preliminary%20Examination%20multiple-choice%20question%20in%20detail.%0A%0ASubject%3A%20Modern%20History%0AMicrotheme%3A%20From%201885%20to%201905%20CE%0A%0AQUESTION%3A%0AWhich%20of%20the%20following%20events%20happened%20during%20the%20tenure%20of%20Lord%20Curzon%3F...
     ```
   - Pressed device Back key: Seamlessly returned to CSE Judge, retaining practice session state, counter stats (1 Solved, 1 Correct), and question history.

3. **Newly Completed Test Report Verification**:
   - Started a Half Length Test (50 Questions).
   - Confirmed test screen has no solution or AI explanation button.
   - Answered Question 1 (HSRA & Kakori Conspiracy) and submitted the test.
   - Opened Question Wise tab: `AI explanation` button was present below the explanation card for Question 1.
   - Tapped `AI explanation`: External browser launched with the full HSRA question, all 4 options, and recorded answer "D".

4. **Historical Past Tests Verification**:
   - Returned to Tests Landing -> Opened Past Tests -> Selected the completed Half Length Test.
   - Switched to Question Wise tab.
   - Verified that questions reconstructed from Room immutable snapshots displayed the `AI explanation` button.
   - Tapped `AI explanation`: Successfully fired external browser intent with snapshot data.

5. **Bookmarked Questions Verification**:
   - Bookmarked the previously attempted question.
   - Navigated to Bookmarked Questions -> Modern History -> Microtheme -> Opened question.
   - Solution displayed previous attempt details with the `AI explanation` button positioned below.
   - Tapped button: Successfully opened Google AI search.

6. **Theme Compatibility Verification**:
   - Toggled dark mode on/off: Button colors, borders, and sparkle icon dynamically adapted between Slate/Navy tones without visual artifacts.

---

## 9. Google AI Mode (`udm=50`) Technical Notes & Limitations

1. **User-Defined Mode Parameter (`udm=50`)**:
   - `udm=50` is Google Search's parameter requesting AI Overview / Google AI Mode.
   - Because CSE Judge delegates this to the external browser, the actual search result rendering depends entirely on Google Search server capabilities and regional availability.

2. **Google-Side Regional & Account Dependencies**:
   - **Supported Regions & Accounts**: Google Search displays a synthesized AI Overview responding directly to the 9 prompt instructions (evaluating each option, explaining why incorrect options are incorrect, and providing UPSC takeaways).
   - **Search Labs / Regional Gate**: If Google AI Overview is unavailable for a user's IP region, language, or unsigned browser session, Google Search falls back gracefully to standard web search results containing the full query text in the search input without crashing.
   - CSE Judge intentionally does not attempt undocumented web workarounds or screen-scraping, preserving client-side stability and zero-dependency compliance.

3. **Intent & External Browser Handling**:
   - Uses `Intent.ACTION_VIEW` with `CATEGORY_BROWSABLE`.
   - Adheres to Android recommendations: prefers the user's default browser and handles missing browser scenarios gracefully via Toast notifications.

---

## 10. Verification Sign-Off

- **Gradle Build**: Passed (`BUILD SUCCESSFUL`).
- **Unit Tests**: Passed (All 14 AI explanation tests + full project test suite passed).
- **Physical Device**: Verified on Realme Narzo 90 5G (`RMX5111`).
- **Git State**: Clean working tree changes only, zero Git commits made.
