# Stage 7: Bookmark Feature (Database, Repository, and ViewModel) Verification & Implementation Report

**Project:** CSE Judge (`com.csejudge.app`)  
**Domain:** UPSC Civil Services Examination (Prelims) Practice Application  
**Stage:** Stage 7 — Bookmark Feature (Database, Repository, and ViewModel)  
**Date:** November 10, 2026  
**Status:** COMPLETE & VERIFIED  

---

## 1. Executive Summary

In Stage 7, the persistent backend foundation for the bookmark feature was implemented and verified. All requirements were strictly met without touching or creating end-user UI elements (reserved for Stage 8).

Key accomplishments:
- **Room Database Migration (v1 → v2):** Designed and verified `MIGRATION_1_2` creating table `bookmarked_questions` without destructive reset, safely preserving all pre-existing `attempted_questions` records.
- **Bookmark Entity & DAO:** Implemented `BookmarkedQuestionEntity` and `BookmarkDao` providing conflict-safe insertion, removal by question ID, bookmark existence checks, reactive `Flow` queries, and timestamp-descending ordering.
- **Bookmark Repository:** Implemented `BookmarkRepository` and `BookmarkRepositoryImpl` enforcing foreign question existence validation against `QuestionBankRepository` prior to persistence, running strictly off the main thread (`Dispatchers.IO`).
- **ViewModel & UI State Integration:** Extended `PracticeUiState` with `isBookmarked` and `isBookmarkToggling` debounce guard. Integrated `toggleBookmark()` and automated bookmark status loading into `PracticeViewModel` upon session start, skip, and next navigation.
- **Re-attempt Preparation:** Prepared `PracticeDao` with `@Upsert suspend fun upsertAttempt(attempt: AttemptedQuestionEntity)` and `PracticeHistoryRepository` with `recordOrUpdateAttempt(...)` to support Stage 8 bookmarked question re-answering while maintaining normal practice abort-on-conflict behavior.
- **Verification & Testing:**
  - **Unit Tests:** 78/78 passed (100%).
  - **Instrumented Connected Tests:** 37/37 passed (100%) on physical device (Realme Narzo 90 5G / `RMX5111` / Android 16).
  - **Physical Device Run:** Installed and verified live on hardware; SQLite database schema and attempt persistence verified via ADB.

---

## 2. Database Schema & Migration Details

### 2.1 Existing Database Schema (Version 1)
- **Database Name:** `cse_judge_database.db`
- **Room Version:** 1
- **Entities:**
  - `attempted_questions`:
    - `question_id` (TEXT, PRIMARY KEY, NOT NULL)
    - `selected_answer` (TEXT, NOT NULL)
    - `is_correct` (INTEGER, NOT NULL)
    - `attempted_at` (INTEGER, NOT NULL)

### 2.2 New Database Schema (Version 2)
- **Room Version:** 2
- **Entities:**
  - `attempted_questions` (preserved identically)
  - `bookmarked_questions`:
    - `question_id` (TEXT, PRIMARY KEY, NOT NULL)
    - `bookmarked_at` (INTEGER, NOT NULL)

```sql
CREATE TABLE IF NOT EXISTS `bookmarked_questions` (
    `question_id` TEXT NOT NULL,
    `bookmarked_at` INTEGER NOT NULL,
    PRIMARY KEY(`question_id`)
);
```

### 2.3 Migration Implementation (`MIGRATION_1_2`)
The migration is implemented in `AppDatabase.kt` and wired into `Room.databaseBuilder`:

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `bookmarked_questions` (
                `question_id` TEXT NOT NULL,
                `bookmarked_at` INTEGER NOT NULL,
                PRIMARY KEY(`question_id`)
            )
            """.trimIndent()
        )
    }
}
```

- **Non-Destructive Guarantee:** Existing user attempt records in `attempted_questions` are never deleted, cleared, or altered.
- **Migration Test:** Verified in on-device test `DatabaseMigrationTest` which pre-populated version 1 with attempts, executed the migration, confirmed pre-existing rows survived untouched, and verified bookmark operations executed successfully on version 2.

---

## 3. Data Layer Implementation

### 3.1 Entity: `BookmarkedQuestionEntity`
Located at `com.csejudge.app.data.local.BookmarkedQuestionEntity.kt`:
```kotlin
@Entity(tableName = "bookmarked_questions")
data class BookmarkedQuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "question_id")
    val questionId: String,

    @ColumnInfo(name = "bookmarked_at")
    val bookmarkedAt: Long = System.currentTimeMillis()
)
```

### 3.2 DAO: `BookmarkDao`
Located at `com.csejudge.app.data.local.BookmarkDao.kt`:
- `insertBookmark(bookmark: BookmarkedQuestionEntity)`: Uses `OnConflictStrategy.REPLACE` to update bookmark timestamp without duplicates.
- `deleteBookmark(questionId: String): Int`: Removes bookmark record by ID. Never affects `attempted_questions`.
- `isBookmarked(questionId: String): Boolean`: SQLite query `SELECT EXISTS(SELECT 1 FROM bookmarked_questions WHERE question_id = :questionId)`.
- `getAllBookmarkedQuestionIds(): List<String>`: Ordered `ORDER BY bookmarked_at DESC`.
- `observeAllBookmarkedQuestionIds(): Flow<List<String>>`: Reactive flow emitting updated bookmark ID lists.
- `getAllBookmarks(): List<BookmarkedQuestionEntity>`: Ordered newest first.
- `observeAllBookmarks(): Flow<List<BookmarkedQuestionEntity>>`: Reactive flow of entities.
- `getBookmarkCount(): Int`: Total count of active bookmarks.
- `observeBookmarkCount(): Flow<Int>`: Reactive flow of total bookmark count.

### 3.3 Preparation for Stage 8 Re-answering: `PracticeDao` & `PracticeHistoryRepository`
- **`PracticeDao.upsertAttempt(attempt: AttemptedQuestionEntity)`:** Added using Room's native `@Upsert` annotation. Ensures re-attempting a bookmarked question replaces the previous answer and timestamp without SQLite primary key conflict exceptions or duplicate records.
- **Normal Practice Preservation:** `PracticeDao.insertAttempt` remains configured with `OnConflictStrategy.ABORT`. Normal practice uses `recordAttempt` to safeguard against accidental overwrites.
- **`PracticeHistoryRepository.recordOrUpdateAttempt(...)`:** Added method to interface and implementation:
```kotlin
override suspend fun recordOrUpdateAttempt(
    questionId: String,
    selectedAnswer: String,
    isCorrect: Boolean,
    timestamp: Long
): Result<Unit> = withContext(ioDispatcher) {
    runCatching {
        val trimmedId = questionId.trim()
        val normalizedAnswer = selectedAnswer.trim().uppercase()
        require(trimmedId.isNotBlank()) { "Question ID cannot be blank" }
        require(normalizedAnswer in Question.VALID_OPTIONS) { "Invalid answer option: '$selectedAnswer'" }
        val entity = AttemptedQuestionEntity(
            questionId = trimmedId,
            selectedAnswer = normalizedAnswer,
            isCorrect = isCorrect,
            attemptedAt = timestamp
        )
        practiceDao.upsertAttempt(entity)
    }
}
```

---

## 4. Repository Layer Implementation: `BookmarkRepository`

Located at `com.csejudge.app.data.repository.BookmarkRepository.kt`:
- **Interface:** Defines asynchronous operations (`addBookmark`, `removeBookmark`, `isBookmarked`, `getAllBookmarkedQuestionIds`, `observeAllBookmarkedQuestionIds`, `getAllBookmarks`, `observeAllBookmarks`, `getBookmarkCount`, `observeBookmarkCount`).
- **Validation Logic:** Before inserting a bookmark, `BookmarkRepositoryImpl` queries `QuestionBankRepository` to verify that the question ID corresponds to a valid question in the master question bank:
  - If question bank is not loaded, it is loaded automatically.
  - If the question ID is blank or does not exist, `addBookmark` returns `Result.failure(IllegalArgumentException)`.
- **Threading:** All Room database access and validation routines execute on `Dispatchers.IO` (`ioDispatcher`).

---

## 5. ViewModel & UI State Integration

### 5.1 `PracticeUiState` Extensions
Added fields to `com.csejudge.app.ui.practice.PracticeUiState`:
- `isBookmarked: Boolean = false`: Indicates whether the question currently displayed is bookmarked.
- `isBookmarkToggling: Boolean = false`: Guard flag against rapid sequential taps / debounce.

### 5.2 `PracticeViewModel` Integration
- **Constructor Injection:** Added `bookmarkRepository: BookmarkRepository? = null`.
- **Question Load Sync:** In `startSession()`, `skipQuestion()`, and `nextQuestion()`, as the current question advances, bookmark status is resolved asynchronously via `bookmarkRepository.isBookmarked(...)` and reflected in `PracticeUiState.isBookmarked`.
- **`toggleBookmark()` Implementation:**
  - Validates `!isBookmarkToggling` and `!isLoading`.
  - Sets `isBookmarkToggling = true`.
  - Dispatches `addBookmark` or `removeBookmark` depending on current state.
  - On success, inverts `isBookmarked` and resets `isBookmarkToggling = false`.
  - On error, emits descriptive `errorMessage` and resets `isBookmarkToggling = false`.
- **Activity Wiring:** Updated `MainActivity.kt` to instantiate `BookmarkRepositoryImpl` passing `database.bookmarkDao()` and `questionBankRepository`, providing it to `PracticeViewModel`.

---

## 6. Test Suite & Verification Results

### 6.1 Unit Tests (JVM)
Executed via `./gradlew testDebugUnitTest`: **78/78 PASSED (100%)**

| Test Class | Focus Area | Result |
|---|---|:---:|
| `BookmarkRepositoryTest` (9 tests) | Validation of question IDs against question bank, blank rejection, add/remove/check, Flow emissions, error handling | **PASSED** |
| `PracticeHistoryReattemptTest` (4 tests) | `recordOrUpdateAttempt` upsert behavior, attempt overwriting without duplicates, validation | **PASSED** |
| `PracticeViewModelBookmarkTest` (4 tests) | Bookmark state synchronization on question load, `toggleBookmark()` add/remove, debounce guard | **PASSED** |
| `PracticeHistoryRepositoryTest` (10 tests) | Practice attempt persistence, query, observation, validation | **PASSED** |
| `PracticeViewModelTest` (13 tests) | Session lifecycle, mode filtering, answer submission, skip/next flow | **PASSED** |
| `Stage6QualityAssuranceTest` (30 tests) | 1,432 question integrity audit, filter combinations, state preservation | **PASSED** |
| `QuestionBankRepositoryTest` (8 tests) | JSON parsing, subject indexing, microtheme categorization | **PASSED** |

### 6.2 Connected Instrumented Tests (Physical Device)
Executed via `./gradlew connectedDebugAndroidTest` on **Realme Narzo 90 5G (`RMX5111` / Android 16)**: **37/37 PASSED (100%)**

| Test Class | Device Execution Details | Result |
|---|---|:---:|
| `BookmarkDaoTest` (7 tests) | Insert, duplicate replace, delete, isBookmarked, query ordering, Flow reactivity, count | **PASSED** |
| `DatabaseMigrationTest` (1 test) | Migration from Room v1 to v2; verifies existing attempts survive and bookmark table works | **PASSED** |
| `PracticeDaoTest` (10 tests) | Attempt CRUD, abort conflict strategy, upsert attempt verification | **PASSED** |
| `AppNavigationTest` (8 tests) | Complete navigation Compose flow between Home, Filter, and Question screens | **PASSED** |
| `PracticeProblemsFlowTest` (7 tests) | End-to-end question answering and state machine verification on device | **PASSED** |
| `QuestionBankInstrumentedTest` (4 tests) | Android asset loading across all 7 subject JSON files | **PASSED** |

### 6.3 Physical Device Deployment & Runtime Check
- **Device Model:** Realme Narzo 90 5G (`RMX5111`)
- **OS:** Android 16 (API 36)
- **Deployment Command:** `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- **Launch Command:** `adb shell am start -n com.csejudge.app/.MainActivity`
- **Logcat Inspection:** Verified clean initialization, no SQLite errors, no Room migration crashes, normal frame rendering (`change focus to true`).
- **Database Inspection:**
  - User version verified as `2` (`PRAGMA user_version = 2`).
  - Table `bookmarked_questions` verified present in SQLite catalog.
  - Practice question answered on device (`ECO00062`); verified attempt record was written to `attempted_questions` (`ECO00062|A|0|1794288797047`).

---

## 7. Files Created & Modified

### Files Created
1. `app/src/main/java/com/csejudge/app/data/local/BookmarkedQuestionEntity.kt`
2. `app/src/main/java/com/csejudge/app/data/local/BookmarkDao.kt`
3. `app/src/main/java/com/csejudge/app/data/repository/BookmarkRepository.kt`
4. `app/src/test/java/com/csejudge/app/data/BookmarkRepositoryTest.kt`
5. `app/src/test/java/com/csejudge/app/data/PracticeHistoryReattemptTest.kt`
6. `app/src/test/java/com/csejudge/app/ui/practice/PracticeViewModelBookmarkTest.kt`
7. `app/src/androidTest/java/com/csejudge/app/data/local/BookmarkDaoTest.kt`
8. `app/src/androidTest/java/com/csejudge/app/data/local/DatabaseMigrationTest.kt`

### Files Modified
1. `app/src/main/java/com/csejudge/app/data/local/AppDatabase.kt` (Version bump to 2, entity registration, `bookmarkDao()`, `MIGRATION_1_2`)
2. `app/src/main/java/com/csejudge/app/data/local/PracticeDao.kt` (Added `upsertAttempt`)
3. `app/src/main/java/com/csejudge/app/data/repository/PracticeHistoryRepository.kt` (Added `recordOrUpdateAttempt`)
4. `app/src/main/java/com/csejudge/app/ui/practice/PracticeUiState.kt` (Added `isBookmarked`, `isBookmarkToggling`)
5. `app/src/main/java/com/csejudge/app/ui/practice/PracticeViewModel.kt` (Wired `bookmarkRepository`, question bookmark loading, `toggleBookmark()`)
6. `app/src/main/java/com/csejudge/app/MainActivity.kt` (Instantiated and supplied `BookmarkRepositoryImpl` to ViewModel)
7. `app/src/androidTest/java/com/csejudge/app/data/local/PracticeDaoTest.kt` (Added tests for upsert functionality)
8. `app/src/androidTest/java/com/csejudge/app/navigation/AppNavigationTest.kt` (Updated fake practice repository for reattempt support)
9. `app/src/test/java/com/csejudge/app/data/PracticeHistoryRepositoryTest.kt` (Updated fake DAO with upsert implementation)
10. `app/src/test/java/com/csejudge/app/ui/practice/PracticeViewModelTest.kt` (Updated fake repository with reattempt support)

---

## 8. Known Limitations & Next Steps

### Known Limitations
- None in the backend data layer.

### Out of Scope for Stage 7 (Planned for Stage 8)
- Bookmark UI components:
  - Bookmark star/icon button on `PracticeQuestionScreen.kt`.
  - "Bookmarked Questions" study module tile on `HomeScreen.kt`.
  - Bookmarked Questions list and practice/review screen.
  - Re-answering flow execution in UI using `recordOrUpdateAttempt`.

### Conclusion
Stage 7 is 100% complete and verified on both unit test suites and the physical target device. Ready for Stage 8 bookmark UI integration upon user request.
