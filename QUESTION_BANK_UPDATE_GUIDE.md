# CSE Judge — Question Bank Update & Maintenance Guide

## 1. Overview and Core Philosophy

In **CSE Judge**, the question bank is **100% data-driven**. The application codebase has **no hardcoded question counts (such as the legacy 1,432)**, no hardcoded subject rosters, and no hardcoded microtheme enumerations.

Questions, subjects, and microthemes are dynamically discovered at runtime from the JSON assets directory (`$rootDir/question-bank/`).

Most importantly:
> **Question content and user state are strictly separated.**
> - **Question Bank:** Static, immutable, read-only content defined in JSON files.
> - **User Progress:** Mutable user state (attempts, timestamps, selected answers, correctness, bookmarks) stored in local Room SQLite database keyed exclusively by permanent `question_id`.

Updating the JSON files in future releases **never erases, resets, or corrupts existing user data**.

---

## 2. Permanent Question Identity

The `question_id` field is the permanent, globally unique identity of every question.

### Permanent Identity Rules
1. **Never regenerate existing IDs:** Every question previously published retains its original `question_id` forever.
2. **Never derive IDs from array index:** Question identity must never depend on line number or array position in JSON files.
3. **No runtime regeneration:** IDs are immutable strings loaded directly from the JSON.
4. **No ID re-assignment:** Never assign an existing ID to a different or modified question.
5. **Globally unique IDs for new questions:** Every newly added question must receive a fresh, globally unique ID (e.g. `POL00253`, `ETH00001`).
6. **Reordering invariance:** Reordering questions within or across files has zero impact on user attempt history, bookmark records, or analysis.
7. **Flag material content changes:** If an existing question's wording, options, or correct answer changes materially, it must be flagged for manual review rather than silently treated as the same question.

### Naming Convention for Question IDs
Question IDs follow a standard prefix convention based on subject code and zero-padded sequence number:
- Ancient, Medieval, Art & Culture: `AMAC00001`, `AMAC00002`, ...
- Economy: `ECO00001`, `ECO00002`, ...
- Environment & Ecology: `ENV00001`, `ENV00002`, ...
- Geography: `GEO00001`, `GEO00002`, ...
- Modern History: `MOD00001`, `MOD00002`, ...
- Indian Polity & Governance: `POL00001`, `POL00002`, ...
- Science & Technology: `SCI00001`, `SCI00002`, ...
- New subjects (e.g. Ethics): `ETH00001`, `ETH00002`, ...

---

## 3. Step-by-Step Question Bank Update Workflow

### Step 1: Appending Questions to Existing Subject JSON
To add new questions to an existing subject:
1. Open the target JSON file in `question-bank/` (e.g. `question-bank/Polity-qid.json`).
2. Navigate to the end of the `"questions"` array.
3. Append your new question object complying with the schema:
```json
{
  "subject": "Polity",
  "microtheme": "Fundamental Rights",
  "question": [
    "Which of the following Articles of the Constitution of India guarantees the Right to Education?",
    "1. Article 21A",
    "2. Article 19",
    "Select the correct answer using the code given below:"
  ],
  "options": {
    "A": "1 only",
    "B": "2 only",
    "C": "Both 1 and 2",
    "D": "Neither 1 nor 2"
  },
  "correct_answer": "A",
  "solution": [
    "Article 21A of the Constitution guarantees the Right to Education as a Fundamental Right.",
    "It was inserted by the 86th Constitutional Amendment Act, 2002."
  ],
  "question_id": "POL00253"
}
```
4. Ensure the trailing comma is placed correctly between items and omitted after the final item.

### Step 2: Adding a New Microtheme to an Existing Subject
To introduce a new microtheme under an existing subject (e.g. "Electoral Reforms" under "Polity"):
1. Set `"microtheme": "Electoral Reforms"` in the question object.
2. The application automatically discovers "Electoral Reforms" at startup when parsing the subject's questions.
3. The new microtheme immediately appears in:
   - Practice screen microtheme filter chips
   - Bookmark screen microtheme hierarchy
   - Analysis screen microtheme-level accuracy & coverage breakdown

### Step 3: Adding an Entirely New Subject
To introduce an entirely new subject (e.g. "Ethics"):
1. **Option A (New File):** Create a new JSON file in `question-bank/`, e.g. `question-bank/Ethics-qid.json` (or `Ethics.json`).
   ```json
   {
     "questions": [
       {
         "subject": "Ethics",
         "microtheme": "Human Values",
         "question": ["Consider the following statements regarding ethics in public administration..."],
         "options": { "A": "...", "B": "...", "C": "...", "D": "..." },
         "correct_answer": "B",
         "solution": ["Explanation..."],
         "question_id": "ETH00001"
       }
     ]
   }
   ```
2. **Option B (Append to existing):** Questions can also belong to any subject string.
3. Because `app/build.gradle.kts` bundles `$rootDir/question-bank` into the APK's asset root and `QuestionBankRepositoryImpl.create()` dynamically enumerates all `.json` assets, the new file is automatically loaded.
4. The new subject immediately appears in:
   - Practice subject dropdown and filter options
   - Bookmark subject list
   - Analysis subject list and overall statistics

---

## 4. Validating Updated Question Bank Files

Before preparing any release, you **must validate** the updated JSON files.

### Validation Tooling
CSE Judge includes built-in automated validation in `QuestionBankUpdateValidator`:

1. **Standalone Directory Validation:**
   Validates syntax, schema, non-blank fields, valid options (A/B/C/D), non-empty question/solution text, and global question ID uniqueness across all files.

2. **Update Compatibility Validation (Baseline vs Candidate):**
   Compares candidate question bank against baseline release to detect:
   - Accidental deletion of existing question IDs
   - Duplicate question IDs
   - Material content changes (unintended alterations in question wording, options, or correct answers)

### Running Validation via Gradle
Execute the validation test suite from the terminal:
```bash
./gradlew testDebugUnitTest --tests com.csejudge.app.data.QuestionBankUpdateValidatorTest
```
This test:
- Scans and validates all files in `question-bank/`.
- Verifies zero JSON syntax errors.
- Verifies zero schema violations.
- Verifies zero duplicate question IDs.
- Confirms SHA-256 fingerprint consistency.

### Automated Update Compatibility Simulation
To run the full Version 1 → Version 2 compatibility simulation:
```bash
./gradlew testDebugUnitTest --tests com.csejudge.app.data.QuestionBankUpdateCompatibilityTest
```
This tests all **17 mandatory update compatibility criteria** on the same persistent database.

---

## 5. Building and Publishing the Updated Release

### Step 1: Pre-Build Sanity Checks
Ensure all automated tests pass:
```bash
./gradlew testDebugUnitTest
```
Ensure instrumented tests pass on the connected physical device (Realme Narzo 90 5G / `RMX5111`):
```bash
./gradlew connectedDebugAndroidTest
```

### Step 2: Version Bump in `app/build.gradle.kts`
Open `app/build.gradle.kts` and increment `versionCode` and update `versionName`:
```kotlin
defaultConfig {
    applicationId = "com.csejudge.app"
    minSdk = 26
    targetSdk = 37
    versionCode = 2       // Increment for each Play Store release
    versionName = "1.1"   // User-facing version string
    ...
}
```

### Step 3: Build the Android App Bundle (AAB) or Release APK
To assemble the production App Bundle:
```bash
./gradlew bundleRelease
```
Output location:
`app/build/outputs/bundle/release/app-release.aab`

To assemble release APK:
```bash
./gradlew assembleRelease
```
Output location:
`app/build/outputs/apk/release/app-release.apk`

### Step 4: Publish through Google Play Console
1. Log into Google Play Console (`play.google.com/console`).
2. Select **CSE Judge**.
3. Navigate to **Production** (or **Internal testing** / **Closed testing**).
4. Create a new release.
5. Upload the signed `app-release.aab`.
6. Add release notes (e.g. *"Added 200 new questions in Polity, Economy, and Ethics"*).
7. Review and rollout the release.

---

## 6. How Existing Users Receive the Update & Why Data is Preserved

### Android Package Manager Invariant
When a user updates CSE Judge from Google Play:
1. Android's Package Manager verifies that the application ID (`com.csejudge.app`) and app signing key match.
2. The APK/AAB is updated in `/data/app/`.
3. The user data directory (`/data/data/com.csejudge.app/`) is **strictly preserved**.
4. The local SQLite database (`cse_judge_database.db`) is **never touched or cleared**.

### Runtime Execution on First Launch Post-Update
1. `QuestionBankRepository` reads the newly bundled JSON assets.
2. `allQuestions` is loaded with all old + new questions.
3. In-memory lookup tables (`questionsByIdMap`, `subjectIndexMap`, `subjectMicrothemeIndexMap`) are generated dynamically.
4. Room queries `attempted_questions` and `bookmarked_questions`:
   - All old questions retain their exact attempt records, correctness flags, and bookmarks.
   - Newly added question IDs do not exist in `attempted_questions` → automatically treated as **unattempted** and eligible for practice.
   - Newly added question IDs do not exist in `bookmarked_questions` → automatically treated as **unbookmarked**.
5. `AnalysisRepository` recalculates totals:
   - `totalQuestions` = new total count
   - `attemptedQuestions` = count of matched attempts (unchanged)
   - `unattemptedQuestions` = `totalQuestions - attemptedQuestions` (increased)
   - `coveragePercentage` and `accuracyPercentage` reflect the expanded question bank accurately.

---

## 7. Evaluation of Data Delivery Options for Google Play

The user requested an evaluation of three strategies for question-bank distribution:

### Option A: Bundled JSON in Application Release (Current & Recommended)
- **Mechanism:** Updated JSON files are committed to `question-bank/` and packaged directly into the App Bundle (`app-release.aab`).
- **New App Release Required?** **Yes.** A new version code must be published via Google Play Console.
- **Download Size:** Google Play generates dynamic split APKs; with Play feature delivery and app bundle compression, users only download differential patch deltas (typically only a few hundred kilobytes for asset updates).
- **Offline Functionality:** **100% offline.** All assets reside locally in APK assets. Zero network required.
- **Persistence:** **100% preserved.** Room database in `/data/data/com.csejudge.app/databases/` is retained intact by Android.
- **Implementation Changes:** **Zero.** Fully implemented, verified, and operational today.
- **Security & Maintenance:** Extremely simple, no backend server to maintain, no API authentication, no remote tamper vulnerabilities.

### Option B: Google Play Asset Delivery (PAD)
- **Mechanism:** Assets are split into separate asset packs (`install-time`, `fast-follow`, or `on-demand`) managed by Google Play.
- **New App Release Required?** **Yes.** Play Asset Delivery still requires publishing an updated App Bundle (`.aab`) with updated asset pack versions through Google Play. Google Play does not permit updating standalone asset packs independently of an application release version.
- **Download Size:** Google Play serves updated asset packs with byte-level delta compression.
- **Offline Functionality:** If using `install-time` asset packs, the app remains 100% functional offline. If using `fast-follow` or `on-demand`, initial app launch requires an active internet connection to download the asset packs.
- **Persistence:** Preserved. Room database is unaffected.
- **Implementation Changes:** Requires Gradle configuration for asset packs (`assetPack` plugins, `Play Core` SDK dependencies).
- **Recommendation:** Not justified at present. Question-bank JSON files total only ~3.4 MB. Google Play's standard APK size limit is 150 MB (and AAB supports up to 2 GB). Play Asset Delivery adds SDK complexity without saving release steps.

### Option C: Remote Question-Bank Updates (OTA / Controlled Download)
- **Mechanism:** App queries a remote backend server or CDN for updated question JSON files, downloads them to device internal storage (`context.filesDir`), and loads them into `QuestionBankRepository`.
- **New App Release Required?** **No.** Content can be updated over-the-air without submitting to Google Play Console.
- **Download Size:** Only the changed JSON delta or checksum-verified files.
- **Offline Functionality:** App continues working offline using the last downloaded question bank (or fallback bundled assets). An active internet connection is only required when checking for updates.
- **Persistence:** Preserved. Room SQLite database remains local and independent.
- **Implementation Changes:** Requires HTTP client (Ktor/Retrofit), background sync worker (WorkManager), remote CDN or server infrastructure, JSON file caching in internal storage, and digital signature verification.
- **Security & Maintenance:** High maintenance burden. Requires cryptographic signature checks (e.g. RSA / Ed25519) to prevent man-in-the-middle attacks, hostile payload injection, server uptime monitoring, and API versioning.

### Summary Recommendation
| Feature | Option A (Bundled AAB) | Option B (Play Asset Delivery) | Option C (Remote OTA) |
|---|---|---|---|
| **Release Required** | Yes (Play Console) | Yes (Play Console) | No |
| **Offline Operation** | Full (Always) | Full (Install-Time) | Full (Cached) |
| **User Data Safety** | 100% Safe | 100% Safe | 100% Safe |
| **Backend Cost** | $0 (Google Play hosts) | $0 (Google Play hosts) | Server / CDN costs |
| **Complexity** | Minimal / Robust | Moderate | High |
| **Current Status** | **Production Ready** | Future candidate if >150MB | Future candidate for live updates |

**Conclusion:** **Option A is prioritized.** It satisfies all offline-first invariants of CSE Judge, has zero operational cost, zero maintenance overhead, and guarantees 100% user data safety across updates. The current clean separation between `QuestionBankRepository` and Room ensures that transitioning to Option B or Option C in the future requires modifying only the asset stream provider (`openAssetStream`) without touching any user data persistence logic.
