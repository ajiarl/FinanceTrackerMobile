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

## Referensi Skill & Tooling Radar (Guidelines & Best Practices)
Koda dan asisten wajib menyelaraskan implementasi dengan panduan tooling ekosistem yang relevan:
1. **Arsitektur & Clean Code**:
   - `improve-codebase-architecture`: Prinsip modularitas tinggi, reduksi kopling, dan pemisahan domain logic dari presentation layer.
   - `test-driven-development` / `systematic-debugging`: Menjamin setiap modifikasi logika matematis (saldo, rekonsiliasi, overbudget) didahului atau divalidasi dengan unit test Room/SQLite yang deterministik.
   - `error-handling-patterns`: Pola penanganan error terstruktur di coroutine Kotlin (hindari catch-all diam-diam tanpa notifikasi UI).
2. **UI/UX & Design System**:
   - `design-taste-frontend` & `statusboard-design-system`: Prinsip anti-slop, utilitarian, zero useless decoration, penekanan data tabular, serta konsistensi tipografi Neobrutalism.
3. **Security & Data Integrity**:
   - `security-best-practices` & `aif-security-checklist`: Pencegahan SQL Injection via prepared statements, sanitasi input nominal numerik, dan enkripsi data lokal.
4. **Tool & Repo Radar**:
   - Referensi kurasi open-source ekosistem (`[[Repo & Tool Radar — Kurasi Open Source]]`): Pola otomasi, state-caching, dan pengujian deterministik.

## Optional Checks (Skip Reasons Declared)
- [x] Auth strategy: Ditunda ke Phase 2 (Sync via Retrofit) karena Phase 1 fokus 100% offline-first local DB
- [x] Cloud sync / API: Ditunda ke Phase 2 setelah Room DB stabil
- [x] Remote telemetry: Skip karena app utilitas personal lokal tanpa background analytics

## Constraints & Engineering Standards untuk Koda
- **Build toolchain**: Jalankan `./gradlew.bat assembleDebug --no-daemon`. JDK home wajib merujuk ke JBR Android Studio (`org.gradle.java.home=C:\Program Files\Android\Android Studio\jbr` di `gradle.properties`).
- **Memory limit**: Alokasi heap Gradle max 2GB (`-Xmx2048m`), dilarang spawn background Gradle daemon persisten.
- **Approval Gate**: Dilarang menghapus file konfigurasi root (`gradle.properties`, `local.properties`, `settings.gradle.kts`).

### 🛡️ Standar Kualitas Kode & Keamanan (Wajib Dipatuhi):
1. **Safety First & Confirmation Gates**:
   - Seluruh tindakan destruktif pengguna (Hapus Transaksi, Hapus Anggaran, Hapus Akun, Reset Filter) WAJIB menggunakan dialog konfirmasi Neobrutal (`NeobrutalConfirmDialog`). Dilarang keras mengeksekusi aksi hapus instan tanpa konfirmasi.
2. **KDoc Documentation & Maintainability**:
   - Setiap class, interface, fungsi repository, dan ViewModel WAJIB memiliki dokumentasi KDoc formal (`/** ... */`) berbahasa Indonesia.
   - Cantumkan tujuan fungsi, penjelasan rumus bisnis (contoh: kalkulasi selisih rekonsiliasi, persentase budget), parameter input, dan efek samping (side effects) pada database.
3. **Database Security & SQL Injection Prevention**:
   - Gunakan selalu query terparameterisasi (`selectionArgs`, SQLite/Room prepared statements). Dilarang menyambung query string mentah (`rawQuery("... " + input)`).
   - Seluruh mutasi saldo akun wajib atomik (`transaction`) agar mencegah race condition atau saldo melayang (*balance drift*).
4. **Code Hygiene & Anti-Bloat**:
   - Dilarang meninggalkan file dead code, file duplikat, atau class cadangan usang. Setelah refactor, bersihkan file lama.
   - Hindari try-catch kosong yang menelan exception (`catch (e: Exception) {}`). Minimal log atau set pesan error ke `uiState.errorMessage`.
5. **Strict Neobrutalism UI Matching Web**:
   - Border tebal: `border: 2dp / 3dp solid #000000`.
   - Hard drop shadow: kotak pekat tanpa blur (`offset(4.dp, 4.dp)` atau `offset(6.dp, 6.dp)` background hitam pekat).
   - Zero rounded radius (`RectangleShape` atau `RoundedCornerShape(0.dp)`).
   - Skema warna identik web: RetroYellow `#FAFF00` (utama/aksi), Emerald `#00A878` (Income), Merah Retro `#DC2626` (Expense/Hapus/Kritis), RetroTransferBlue `#007AFF` / `#2563EB` (Transfer/Akun).
   - Font tabular untuk angka mata uang (`fontFeatureSettings = "tnum"`).

- **Definition of Done (DoD)**:
  1. Kompilasi Gradle lolos exit code 0 (`./gradlew.bat compileDebugKotlin`).
  2. Seluruh unit tests lolos 100% (`./gradlew.bat testDebugUnitTest --no-daemon`).
  3. Kompilasi APK sukses (`./gradlew.bat assembleDebug --no-daemon`).
  4. File APK valid ter-generate di `app/build/outputs/apk/debug/app-debug.apk`.
  5. Kode terstruktur bersih dalam package `com.sena.financetracker` (data, repository, viewmodel, ui) dengan KDoc lengkap.
  6. Wajib jalankan `git commit` di branch worktree sebelum `kanban_complete`.
