# Stage 12 — Question Bank Scalability and Persistent User Progress Report

## 1. Executive Summary

Stage 12 successfully establishes a **scalable, data-driven question-bank architecture** for CSE Judge that completely decouples read-only static question content from user activity state. Future question additions, new microthemes, and entirely new subjects can be introduced simply by updating or adding JSON files in the assets directory without modifying a single line of application source code.

All existing user attempts, answer selections, timestamps, reattempt records, and bookmarks are guaranteed to survive question-bank updates with zero data loss, zero duplication, and zero database corruption.

---

## 2. Existing Architecture Audit

Prior to making changes, an exhaustive inspection of the CSE Judge codebase was conducted:

1. **Question Content Storage:**
   - Question bank files are stored under `$rootDir/question-bank/` as 7 subject-wise JSON files: `AMAC-qid.json`, `Economy-qid.json`, `Environment-qid.json`, `Geography-qid.json`, `Modern History-qid.json`, `Polity-qid.json`, and `Sci and Tech-qid.json`.
   - `build.gradle.kts` configures `sourceSets.main.assets.directories.add("$rootDir/question-bank")`.
   - The question bank contains 1,432 questions in the initial baseline.
   - **Audit finding on hardcoded counts:** There were no hardcoded "1432" constants in application runtime code; `totalQuestions` was already computed dynamically via `allQuestionsList.size`.
   - **Audit finding on subject discovery:** In `QuestionBankRepositoryImpl`, file enumeration was previously biased towards `-qid.json`. We enhanced it to dynamically discover any `.json` or `.enc` file in the assets directory, allowing completely arbitrary file naming for new subjects.

2. **User Activity Storage (Room SQLite):**
   - User state lives in `cse_judge_database.db` version 2 (`AppDatabase.kt`).
   - `attempted_questions`: `@Entity(tableName = "attempted_questions")` with `@PrimaryKey val questionId: String`, `selected_answer`, `is_correct`, `attempted_at`.
   - `bookmarked_questions`: `@Entity(tableName = "bookmarked_questions")` with `@PrimaryKey val questionId: String`, `bookmarked_at`.
   - **Audit finding on separation:** Database entities reference questions **exclusively by permanent `question_id`**. No question text, options, or subject strings are duplicated into SQLite.

3. **Practice & Analysis Layers:**
   - `PracticeViewModel`: Queries unattempted questions dynamically by filtering `allQuestions` against Room attempted IDs.
   - `BookmarkViewModel`: Reconstructs subject and microtheme groupings on the fly via `groupBy { it.subject }` and `groupBy { it.microtheme }`.
   - `AnalysisRepository`: Aggregates coverage and accuracy dynamically from loaded questions and Room attempts. Unmatched attempts (attempts for questions no longer present in JSON) are safely flagged without deleting records from SQLite.

---

## 3. Changes Made

1. **Flexible Dynamic Asset Discovery:**
   - Updated `QuestionBankRepositoryImpl.create(context: Context)` to dynamically enumerate all `.json` and `.enc` asset files, eliminating any dependency on specific file naming patterns.
   - Added `createFromDirectory(directory: File)` factory method to facilitate offline testing and CLI verification of question bank folders.

2. **Schema & Content Validation Safeguards:**
   - Enhanced `Question.validate()` to reject empty or whitespace-only question and solution texts.
   - Added `QuestionBankUpdateValidator.validateDirectory(directory: File)` to enable standalone validation of production JSON files, checking syntax, schema, non-blank fields, option constraints, and global ID uniqueness.

3. **Complete 17-Criteria Update Simulation:**
   - Expanded both the JVM unit test (`QuestionBankUpdateCompatibilityTest`) and the on-device instrumented test (`QuestionBankUpdateInstrumentedTest`) to explicitly verify all **17 mandatory compatibility criteria** on the same persistent database instance.
   - Added explicit verification for Criteria 16 (handling accidental deletion of questions from JSON without erasing Room records) and 17 (repeated update idempotence).

4. **Scalability & Performance Benchmarking:**
   - Created `QuestionBankScalabilityBenchmarkTest` benchmarking 10,000 questions (approx 7x current size) across 15 subjects and 150 microthemes.

5. **Physical Device Update Verification:**
   - Performed genuine in-place package updates on the physical test device (`Realme Narzo 90 5G / RMX5111`, Android 16) via `adb install -r`.
   - Verified that user attempts, reattempts, and bookmarks persisted in SQLite with zero corruption.

---

## 4. Files Created and Modified

### Created Files
- `app/src/test/java/com/csejudge/app/data/QuestionBankScalabilityBenchmarkTest.kt` (Synthetic 10k question benchmark)
- `QUESTION_BANK_UPDATE_GUIDE.md` (Operational guide for future releases and Play Store publishing)
- `stage12_scalability_report.md` (This comprehensive stage delivery report)

### Modified Files
- `app/src/main/java/com/csejudge/app/data/model/Question.kt` (Enhanced validation for whitespace-only text)
- `app/src/main/java/com/csejudge/app/data/repository/QuestionBankRepository.kt` (Dynamic `.json` asset discovery and `createFromDirectory` factory)
- `app/src/main/java/com/csejudge/app/data/repository/QuestionBankUpdateValidator.kt` (Added `DirectoryValidationReport` and `validateDirectory` standalone validator)
- `app/src/test/java/com/csejudge/app/data/QuestionBankUpdateCompatibilityTest.kt` (Complete 17-criteria update simulation test)
- `app/src/test/java/com/csejudge/app/data/QuestionBankUpdateValidatorTest.kt` (Added production JSON directory validation test)
- `app/src/androidTest/java/com/csejudge/app/data/local/QuestionBankUpdateInstrumentedTest.kt` (Complete 17-criteria on-device Room test)
- `app/src/androidTest/java/com/csejudge/app/ui/home/HomeScreenTest.kt` (Updated UI assertion to match dynamic "all subjects" label)

---

## 5. Version 1 → Version 2 Test Results (17 Criteria)

Both `QuestionBankUpdateCompatibilityTest` (JVM unit test) and `QuestionBankUpdateInstrumentedTest` (on-device Room SQLite) verified all 17 mandatory criteria:

| # | Criterion | Verification Outcome |
|---|---|---|
| 1 | **All old attempts remain intact** | Passed (`POL00001`, `POL00002`, `ECO00001` preserved with answers and correctness) |
| 2 | **All old bookmarks remain intact** | Passed (`POL00001`, `POL00003` remain bookmarked) |
| 3 | **All reattempt records remain intact** | Passed (`ECO00001` reattempt answer 'D' and timestamp preserved) |
| 4 | **No duplicate attempt records exist** | Passed (Attempt count strictly remains 3; all IDs unique) |
| 5 | **No duplicate bookmark records exist** | Passed (Bookmark count strictly remains 2; all IDs unique) |
| 6 | **All new questions are loaded** | Passed (`POL00004`, `ECO00002`, `ETH00001`, `ETH00002` loaded; total = 9) |
| 7 | **All new questions initially unattempted** | Passed (`isQuestionAttempted` returns false for all new IDs) |
| 8 | **All new questions initially unbookmarked** | Passed (`isBookmarked` returns false for all new IDs) |
| 9 | **New questions appear in practice** | Passed (Eligible unattempted practice pool contains 6 questions) |
| 10 | **Attempted questions excluded from practice** | Passed (`POL00001`, `POL00002`, `ECO00001` absent from practice pool) |
| 11 | **New subjects appear automatically** | Passed (New subject `Ethics` automatically discovered and available) |
| 12 | **New microthemes appear automatically** | Passed (`Fiscal Policy` under Economy and `Human Values` under Ethics discovered) |
| 13 | **Analysis reflects new question-bank totals** | Passed (Total questions = 9, attempted = 3, unattempted = 6) |
| 14 | **Coverage and accuracy calculations correct** | Passed (Coverage = 33.33%, Accuracy = 66.67%, Polity coverage = 50.0%) |
| 15 | **Reordering does not affect identity** | Passed (Reordering JSON array positions had zero impact on IDs or user state) |
| 16 | **Missing questions do not erase Room records** | Passed (Accidental deletion flagged by validator; SQLite records retained; analysis handled gracefully) |
| 17 | **Repeated updates do not duplicate records** | Passed (Repeated load/upsert/insert idempotent; counts stay strictly 3 and 2) |

---

## 6. Performance & Scalability Benchmark Results

Benchmarked with **10,000 synthetic questions** (6.56 MB JSON payload, 15 subjects, 150 microthemes, 3,000 simulated attempts, 500 bookmarks):

| Operation | Scale Tested | Measured Latency | Target / Sla | Result |
|---|---|---|---|---|
| **JSON Deserialization + Validation + Index Building** | 10,000 questions | **209 ms** | < 2,000 ms | **Exceeds target by 9.5x** |
| **Random ID Lookup** | 1,000 lookups | **2 ms** (~2.0 µs / lookup) | < 1 ms / lookup | **Instantaneous** |
| **Subject Filtering** | All 15 subjects | **0 ms** (< 0.01 ms / subject) | < 10 ms | **Instantaneous** |
| **Microtheme Filtering** | All 150 microthemes | **0 ms** (< 0.01 ms / microtheme) | < 10 ms | **Instantaneous** |
| **Practice Session Pool Initialization** | 6,000 unattempted pool | **1 ms** | < 50 ms | **Instantaneous** |
| **Question Advance & Skip Cycling** | 100 transitions | **0 ms** (~0.000 ms / action) | < 1 ms / action | **Zero UI lag** |
| **Bookmark Hierarchy Reconstruction** | 500 bookmarks | **1 ms** | < 50 ms | **Instantaneous** |
| **Full Mathematical Analysis Calculation** | 10k items, 3k attempts, 150 microthemes | **33 ms** | < 200 ms | **Fast & Smooth** |

---

## 7. Regression & Hardware Test Summary

### Unit Tests
- Command: `./gradlew testDebugUnitTest`
- Outcome: **145 tests completed, 0 failures, 100% passing** (execution time: 9s).

### Connected Instrumented Tests (Physical Device)
- Hardware: **Realme Narzo 90 5G (`RMX5111`), Android 16**
- Command: `./gradlew connectedDebugAndroidTest`
- Outcome: **60 tests completed, 0 failures, 100% passing** (execution time: 1m 30s).
- Tests covered:
  - `QuestionBankUpdateInstrumentedTest` (17-criteria V1→V2 SQLite simulation)
  - `PracticeDaoTest` & `BookmarkDaoTest`
  - `DatabaseMigrationTest`
  - `HomeScreenTest`, `PracticeQuestionScreenTest`, `PracticeFilterScreenTest`
  - `AnalysisScreenTest`, `BookmarkFlowInstrumentedTest`
  - `AppNavigationTest`

### Physical Device Genuine Two-APK In-Place Update Verification
To unequivocally establish that the question-bank assets actually changed between two installed APK versions on the physical device (`Realme Narzo 90 5G / RMX5111`, Android 16) without uninstalling the application or clearing its database, the following live test was executed:

#### 1. Isolated Test Fixtures & Build Integrity
- **Production Safety:** Production files in `question-bank/*.json` remained 100% untouched (verified via SHA-256 / MD5 checksums).
- **Isolated V2 Fixture:** Created `fixtures/question-bank-v2/` containing all 1,432 original questions from the 7 production JSON files, plus:
  - Appended 1 new question to `Polity-qid.json`: `POL99999` (Microtheme: *Judicial Reforms*).
  - Appended 5 new questions in a brand new subject file `Ethics-qid.json`: `ETH00001` through `ETH00005` (Microtheme: *Human Values*).
  - Total questions in Version 2 fixture: **1,438 questions across 8 JSON files**.

#### 2. Distinct APK Specifications & Compatible Signing
| Property | Version 1 APK (`test-apks/app-v1.apk`) | Version 2 APK (`test-apks/app-v2.apk`) |
|---|---|---|
| **Build Command** | `./gradlew assembleDebug` | `./gradlew assembleDebug "-PqbDir=$PWD/fixtures/question-bank-v2" -PappVersionCode=2 -PappVersionName=2.0` |
| **Application ID** | `com.csejudge.app` | `com.csejudge.app` (Identical) |
| **Version Code / Name** | `1` / `"1.0"` | `2` / `"2.0"` |
| **Asset Directory** | `$rootDir/question-bank` | `$rootDir/fixtures/question-bank-v2` |
| **Asset Files Count** | 7 JSON files | 8 JSON files (+ `Ethics-qid.json`) |
| **Total Question Count** | **1,432 questions** | **1,438 questions** (+6 questions) |
| **Polity Questions Count** | 252 questions | 253 questions (+ `POL99999`) |
| **Signing Identity** | Debug keystore (APK Sig Scheme v2) | Identical debug keystore (APK Sig Scheme v2) |
| **`apksigner verify -v`** | Verified (Scheme v2: true, 1 signer) | Verified (Scheme v2: true, 1 signer) |

#### 3. Execution Commands & Database State Evolution
- **Step 1: Install Version 1 on Device:**
  ```bash
  adb install -r test-apks/app-v1.apk
  # Output: Performing Streamed Install -> Success
  adb shell am start -n com.csejudge.app/.MainActivity
  ```
- **Step 2: Seed & Verify Version 1 Baseline User State (`seedAndVerifyVersion1`):**
  - Command:
    ```bash
    adb shell am instrument -w -e class com.csejudge.app.data.local.RealUpdateDeviceVerificationTest#seedAndVerifyVersion1 com.csejudge.app.test/androidx.test.runner.AndroidJUnitRunner
    ```
  - Result: `OK (1 test)`.
  - Logcat Verified Output:
    ```text
    === PHASE 1: VERSION 1 BASELINE VERIFICATION ===
    V1 Questions loaded from APK assets: 1432
    V1 Available subjects: [AMAC, Economy, Environment, Geography, Modern History, Polity, Sci and Tech]
    SQLite V1 Attempt Count: 3
    SQLite V1 Bookmark Count: 2
    V1 Overall Analysis -> Total: 1432, Attempted: 3, Correct: 2, Unattempted: 1429
    === PHASE 1 BASELINE VERIFIED SUCCESSFULLY ===
    ```
- **Step 3: Genuine In-Place Update to Version 2 via `adb install -r`:**
  - Command:
    ```bash
    adb install -r test-apks/app-v2.apk
    # Output: Performing Streamed Install -> Success
    ```
  - Crucial condition: The application was **never uninstalled**, and `adb shell pm clear` was **never executed**.
- **Step 4: Launch Updated App:**
  ```bash
  adb shell am start -n com.csejudge.app/.MainActivity
  # Clean startup; foreground focus confirmed; zero crashes.
  ```
- **Step 5: Execute Version 2 Verification (`verifyVersion2Update`):**
  - Command:
    ```bash
    adb shell am instrument -w -e class com.csejudge.app.data.local.RealUpdateDeviceVerificationTest#verifyVersion2Update com.csejudge.app.test/androidx.test.runner.AndroidJUnitRunner
    ```
  - Result: `OK (1 test)`.
  - Logcat Verified Output:
    ```text
    === PHASE 2: VERSION 2 IN-PLACE UPDATE VERIFICATION ===
    V2 Questions loaded from APK assets: 1438
    V2 Available subjects: [AMAC, Economy, Environment, Ethics, Geography, Modern History, Polity, Sci and Tech]
    SQLite V2 Attempt Count: 3
    SQLite V2 Bookmark Count: 2
    Persistent Attempt: POL00001 | answer=C | correct=true | time=1794663608510
    Persistent Attempt: POL00002 | answer=D | correct=false | time=1794663608540
    Persistent Attempt: ECO00001 | answer=D | correct=true | time=1794663608558
    Persistent Bookmark: POL00003
    Persistent Bookmark: POL00001
    V2 Overall Analysis -> Total: 1438, Attempted: 3, Correct: 2, Unattempted: 1435, Coverage: 0.2086230876216968%, Accuracy: 66.66666666666666%
    === PHASE 2: ALL 7 UPDATE VERIFICATION POINTS CONFIRMED ON PHYSICAL DEVICE ===
    ```

#### 4. Explicit Answers to the 7 Verification Points
1. **Was Version 1 built with the original question-bank JSON files?**
   - **YES.** Built directly from `$rootDir/question-bank` containing the original 7 JSON files with 1,432 questions.
2. **Was Version 2 built with modified JSON files containing all old questions plus newly appended questions?**
   - **YES.** Built from `fixtures/question-bank-v2/` containing all 1,432 original questions plus newly appended `POL99999` in `Polity-qid.json` and `ETH00001`–`ETH00005` in `Ethics-qid.json`, totaling 1,438 questions.
3. **Were both APKs installed using the same application ID and compatible signing identity?**
   - **YES.** Both used `com.csejudge.app` and were signed with the identical debug keystore via APK Signature Scheme v2.
4. **Was Version 2 installed over Version 1 using `adb install -r`, without uninstalling the application or clearing its data?**
   - **YES.** Streamed in-place install via `adb install -r test-apks/app-v2.apk` succeeded without package removal or SQLite data clearance.
5. **After installing Version 2, were the newly appended questions actually visible in the application?**
   - **YES.** `POL99999` appeared in Polity practice pools; `ETH00001` through `ETH00005` appeared under new subject *Ethics* (5 unattempted questions in session pool).
6. **Were the old attempts, reattempts and bookmarks still intact?**
   - **YES.** Attempt count remained strictly 3 (`POL00001`, `POL00002`, `ECO00001` with reattempted answer 'D' and correctness true preserved); bookmark count remained strictly 2 (`POL00001`, `POL00003`). Zero duplication and zero data loss.
7. **Did analysis automatically reflect the new total question count?**
   - **YES.** Total questions dynamically recalculated from 1,432 to 1,438; unattempted questions updated from 1,429 to 1,435; overall coverage recalculated to 0.2086%; subject analysis for *Ethics* showed 5 total, 0 attempted, 0.0% coverage; subject analysis for *Polity* showed 253 total, 2 attempted, 251 unattempted.

---

## 8. Remaining Limitations & Future Recommendations

1. **Asset Pack Splitting (Google Play Asset Delivery):**
   - Currently, all question-bank JSON assets (~3.4 MB) are bundled directly inside the main APK/AAB (Option A). This is completely optimal and within Google Play's 150 MB APK limit. If the question bank grows beyond 100,000 questions (>50 MB), Google Play Asset Delivery (Option B) can be enabled to deliver assets as an `install-time` asset pack without changing database logic.
2. **Dynamic OTA Question Updates (Option C):**
   - For over-the-air updates without publishing new AABs to Google Play, a remote CDN and digital signature verification layer can be added in a future stage. The repository architecture is already decoupled to support loading from `filesDir`.
3. **No Automatic In-Place Content Modification:**
   - In accordance with safety rules, the system flags material wording/answer changes for manual review and does not automatically alter existing questions.

---

## 9. Verification Artifacts & Reference Links

- [QUESTION_BANK_UPDATE_GUIDE.md](file:///home/ashwani/Desktop/development/CSE%20Judge/QUESTION_BANK_UPDATE_GUIDE.md)
- [stage12_scalability_report.md](file:///home/ashwani/Desktop/development/CSE%20Judge/stage12_scalability_report.md)
- Unit Test Benchmark: [QuestionBankScalabilityBenchmarkTest.kt](file:///home/ashwani/Desktop/development/CSE%20Judge/app/src/test/java/com/csejudge/app/data/QuestionBankScalabilityBenchmarkTest.kt)
- Compatibility Simulation Test: [QuestionBankUpdateCompatibilityTest.kt](file:///home/ashwani/Desktop/development/CSE%20Judge/app/src/test/java/com/csejudge/app/data/QuestionBankUpdateCompatibilityTest.kt)
- Instrumented Room Test: [QuestionBankUpdateInstrumentedTest.kt](file:///home/ashwani/Desktop/development/CSE%20Judge/app/src/androidTest/java/com/csejudge/app/data/local/QuestionBankUpdateInstrumentedTest.kt)
- Physical Device Genuine Update Test: [RealUpdateDeviceVerificationTest.kt](file:///home/ashwani/Desktop/development/CSE%20Judge/app/src/androidTest/java/com/csejudge/app/data/local/RealUpdateDeviceVerificationTest.kt)
