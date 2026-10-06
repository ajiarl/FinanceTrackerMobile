# AGENTS.md

## Project Context
- **Name**: FinanceTrackerMobile
- **Objective**: Aplikasi Android Native Finance Tracker (Level 3: Offline-first Room Database & Jetpack Compose UI) yang kompatibel dengan ekosistem Laravel backend di `D:/Projects/finance-tracker`.
- **Target User**: Aji Arlando (Personal finance management harian di HP Redmi 12).
- **MVP Scope (Phase 1 — Local Storage & Dashboard)**:
  - Local persistence menggunakan SQLite / Room DB (Entity `TransactionEntity`: id, title, amount, type [INCOME/EXPENSE], category, timestamp).
  - Unidirectional Data Flow (UDF) via MVVM: `TransactionDao` -> `TransactionRepository` -> `FinanceViewModel` (StateFlow) -> Compose Screens.
  - UI Material 3 anti-slop: Dashboard Card (Total Balance, Income, Expense), Quick Add Dialog/Sheet, Transaction History List (LazyColumn) dengan format Rupiah `Rp...` dan digit tabular (`tabular-nums`).

## Stack & Decisions
| Layer | Decision | Rationale |
|---|---|---|
| Platform | Android Native (Kotlin 2.2+, AGP 9.4+) | Performa maksimal, 0 overhead runtime bridge |
| UI Framework | Jetpack Compose + Material 3 | Modern declarative UI, standar resmi Google |
| Local Persistence | Room DB / SQLite | Type-safe, reactive Flow integration, offline-first |
| Target Device | Redmi 12 (Android 14 / HyperOS) via ADB | Physical device verification, 0 emulator overhead |
| Architecture | Clean MVVM (Entity, DAO, Repo, ViewModel) | Separation of concerns, testable, maintainable |

## Non-Negotiable Checklist & Grounding
- [x] Problem statement & MVP scope terdefinisi jelas
- [x] User flow selesai sebelum mulai ngoding (Dashboard -> Add Transaction -> Instant Reactive Update)
- [x] Stack decision terisi lengkap dengan alasan
- [x] AGENTS.md tersedia di root repo sebelum worker spawn
- [x] Data contract grounded: Selaras dengan skema tabel `transactions` di backend Laravel `D:/Projects/finance-tracker` (type: income/expense, amount: numeric, description, category, date)
- [x] Global error handler: Null-safety Kotlin & safe parsing pada input amount
- [x] Zero mock domain: Format mata uang riil Indonesia (`Rp...`)
- [x] No slop UI: Mengikuti `DESIGN.md` spec — high-contrast utilitarian palette, no decorative purple glows/gradients

## Optional Checks (Skip Reasons Declared)
- [x] Auth strategy: Ditunda ke Phase 2 (Sync via Retrofit) karena Phase 1 fokus 100% offline-first local DB
- [x] Cloud sync / API: Ditunda ke Phase 2 setelah Room DB stabil
- [x] Remote telemetry: Skip karena app utilitas personal lokal tanpa background analytics

## Constraints untuk Koda
- **Build toolchain**: Jalankan `./gradlew.bat assembleDebug --no-daemon`. JDK home wajib merujuk ke JBR Android Studio (`org.gradle.java.home=C:\Program Files\Android\Android Studio\jbr` di `gradle.properties`).
- **Memory limit**: Alokasi heap Gradle max 2GB (`-Xmx2048m`), dilarang spawn background Gradle daemon persisten.
- **Approval Gate**: Dilarang menghapus file konfigurasi root (`gradle.properties`, `local.properties`, `settings.gradle.kts`).
- **Definition of Done (DoD)**:
  1. Kompilasi Gradle lolos exit code 0 (`./gradlew.bat assembleDebug --no-daemon`).
  2. File APK valid ter-generate di `app/build/outputs/apk/debug/app-debug.apk`.
  3. Kode terstruktur bersih dalam package `com.sena.financetracker` (data, repository, viewmodel, ui).
  4. Wajib jalankan `git commit` di branch worktree sebelum `kanban_complete`.
