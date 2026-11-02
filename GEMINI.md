# CSE Judge — Agent Instructions & Engineering Guidelines

This document governs all AI-assisted development on **CSE Judge**. It defines the project context, technical stack, core architectural invariants, engineering behaviors, and verification protocols. Every agent session must adhere to these instructions.

---

## 1. Conflict Priority Hierarchy

When instructions conflict, resolve them using the following strict precedence:

1. **Explicit instructions in the current user prompt** (highest priority).
2. **Data safety & preservation of existing user data/work**.
3. **Project-specific architectural decisions & constraints** (offline-first, JSON master source, Room user state separation).
4. **General engineering & behavioral guidelines** (simplicity, surgical changes, verification).
5. **Agent defaults and preferences** (lowest priority).

> *Note: Apply engineering judgment rather than interpreting guidelines mechanically, but never violate higher-priority constraints.*

---

## 2. Project Context & Environment

### Project Identity
- **Name:** CSE Judge
- **Domain:** UPSC Civil Services Examination (Prelims) preparation.
- **Core Capabilities:** Prelims question practice, structured question-bank management, timed mock tests, performance analytics, revision workflows, and syllabus progress tracking.

### Development Environment & Hardware Constraints
- **OS:** Ubuntu Linux
- **Editor / Workspace:** VS Code
- **AI Coding Agent:** Antigravity CLI
- **Testing & Run Target:** Physical Android device connected via ADB.
- **Environment Invariant:** **No Android Studio and no Android emulator** are used or expected in the standard workflow. All builds, tests, installations, and log inspections must execute via command-line tools (`./gradlew`, `adb`).

### Technology Stack
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material 3 (M3)
- **Architecture:** Clean MVVM (Model-View-ViewModel) with unidirectional data flow (UDF)
- **Local Persistence:** Room Database (user data) + Static JSON assets (master question bank)
- **Serialization:** Kotlinx Serialization
- **Async & Concurrency:** Kotlin Coroutines & Flow
- **Navigation:** Navigation Compose
- **Build System:** Gradle with Kotlin DSL (`build.gradle.kts`)

---

## 3. Core Architectural & Domain Invariants

1. **Separation of Question Content and User State:**
   - **Static Question-Bank (Master Source):** Static JSON files are the single source of truth for questions, options, explanations, subjects, and tags. Treat this content as read-only source data, not mutable user data.
   - **User Activity State (Room Database):** User-specific data—including question attempts, mock test sessions, bookmarks, custom notes, accuracy metrics, and revision queues—must live in Room and reference questions by stable identifiers.
   - **Stable Question Identifiers:** Questions must have persistent, immutable IDs across imports, updates, and releases.
2. **Offline-First Functionality:**
   - The application must remain fully functional offline. Core practice, mock exams, bookmarking, and local analytics must never depend on an active network connection.
3. **Database Schema Stability:**
   - Avoid unnecessary Room migrations or destructive schema alterations. Plan entity designs carefully to protect local user progress and mock history.
4. **UI & State Integrity:**
   - Strictly implement Material 3 design guidelines with a polished, professional aesthetic suited for serious competitive exam preparation.
   - Explicitly handle all realistic UI states: **Loading**, **Success**, **Empty**, and **Error**.
   - Create reusable, modular Compose components with clear separation between stateful containers and stateless presentation.

---

## 4. Engineering Behavioral Guidelines (Karpathy Principles)

### 4.1 Think Before Coding
- **No Unexamined Assumptions:** Clarify ambiguity. Inspect existing files, schemas, and architecture before writing code.
- **Surface Tradeoffs:** If an implementation has non-obvious tradeoffs or multiple viable interpretations, state them concisely before executing.
- **Push Back on Overengineering:** If a requested approach is needlessly complex, fragile, or ill-suited to the existing codebase, recommend the simpler, robust alternative.
- **Ask Meaningful Questions:** If critical information or domain requirements are missing, stop and ask. For minor, unambiguous details, exercise sound judgment and proceed without unnecessary interruptions.
- **Never Invent Requirements:** Build strictly what is needed; do not hallucinate features, configuration options, or speculative future requirements.

### 4.2 Simplicity First
- **Minimal Code Footprint:** Write the smallest amount of readable code that cleanly solves the problem.
- **Zero Speculative Abstraction:** Do not create generic layers, factory patterns, or configuration hooks for requirements that do not yet exist.
- **Avoid Premature Optimization:** Optimize for clarity, readability, and correct state handling first.
- **Dependency Discipline:** Do not add third-party libraries without verifying compatibility, size impact, and genuine necessity. Prefer standard AndroidX/Kotlin libraries.
- **The Senior Android Developer Test:** Before finalizing any design or implementation, ask:
  > *"Would an experienced senior Android developer consider this implementation unnecessarily complicated?"*
  If yes, refactor and simplify immediately.

### 4.3 Surgical Changes
- **Minimal Blast Radius:** Modify only the files directly required for the task.
- **No Drive-by Refactoring:** Do not reformat unrelated code, alter existing naming conventions, or rewrite functional components simply because an alternative syntax exists.
- **Clean Diffs:** Clean up unused imports, variables, temporary debug prints, and dead code introduced by your changes.
- **Code Preservation:** Do not delete or overwrite pre-existing code or assets without explicit justification and user approval.
- **Traceability:** Every changed line must be justifiable against the specific task objective.

### 4.4 Goal-Driven Execution
For every non-trivial task:
1. Define the clear objective and explicit verification criteria upfront.
2. For multi-step tasks, outline a concise step-by-step plan:
   - `Step 1` → Verification criteria
   - `Step 2` → Verification criteria
   - `Step 3` → Verification criteria
3. Implement the smallest logical step.
4. Run concrete checks (build, lint, unit tests, or device runs).
5. Inspect outputs and fix issues immediately.
6. **Never claim code works simply because generation finished without tool errors.**

### 4.5 Systematic Debugging
When errors or build failures occur:
- Read full compiler diagnostics, stack traces, and `adb logcat` output.
- Isolate the true root cause before modifying code; never blindly tweak multiple files hoping for a fix.
- Make one logically justified change at a time, then re-verify.
- Avoid repeating identical unsuccessful attempts.
- Clearly differentiate environment issues (ADB disconnect, permission errors, Gradle daemon locks) from application code bugs.

### 4.6 Verification & Testing Standards
- **Build Checks:** Use the Gradle wrapper (`./gradlew assembleDebug`, `./gradlew testDebugUnitTest`, etc.).
- **Physical Device Deployment:** When a device is connected via ADB, install builds (`adb install -r <apk-path>`) and check runtime behavior.
- **Runtime Verification:** Use `adb logcat -d` filtered by application package/tag to inspect crashes, ANRs, or ViewModel/Room exceptions.
- **Integrity in Reporting:**
  - Never fabricate build outputs, test results, or verification claims.
  - Explicitly distinguish between:
    - *Compilation verified* (Gradle build passed)
    - *Unit/integration test verified* (Automated tests passed)
    - *Physical device verified* (App installed and executed on real hardware)

---

## 5. Working Rules & Development Workflow

1. **Incremental Delivery:** Build feature by feature. Never attempt to generate the entire application or multiple complex modules in a single monolithic step.
2. **Pre-Implementation Inspection:** Always check the current directory structure, Gradle setup, dependencies, and existing source files before introducing new files or code.
3. **Preserve User Data:** Never perform destructive actions on local databases, device storage, or configuration files without explicit user consent.
4. **Architectural Transparency:** Briefly explain major architectural choices (e.g., entity relationships, navigation routes, state management paradigms) before writing implementation code.
5. **Living Documentation:** Keep relevant documentation, schema definitions, and instructions updated as the codebase evolves.

---

## 6. Antigravity Agent Protocol

- **Session Start:** Read this `GEMINI.md` file at the beginning of each session to align with project rules.
- **Planning vs. Direct Action:**
  - *Substantial tasks:* Inspect codebase, state assumptions, and provide a short phased plan with verification criteria.
  - *Trivial tasks / Quick fixes:* Execute directly with minimal overhead.
- **End-of-Task Delivery Summary:**
  At the conclusion of each task, provide a concise structured summary containing:
  1. **What Changed:** High-level summary of the implementation.
  2. **Files Created or Modified:** Explicit list of affected file paths.
  3. **Builds & Tests Performed:** Exact Gradle commands or ADB actions executed.
  4. **Verification Results:** Actual outcomes (build success, tests passed, logcat clean, device launch confirmed).
  5. **Known Limitations & Next Steps:** Any pending work, technical debt, or recommended follow-up tasks.
