# 💸 FinanceTracker Mobile

[![Platform](https://img.shields.io/badge/Platform-Android%20(API%2024%2B)-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2F%20MVI-FF6F00?style=for-the-badge)](https://developer.android.com/topic/architecture)
[![Database](https://img.shields.io/badge/Storage-Room%20(SQLite%20v6)-4169E1?style=for-the-badge&logo=sqlite&logoColor=white)](https://developer.android.com/training/data-storage/room)
[![Unit Tests](https://img.shields.io/badge/Unit%20Tests-175%20Passing%20(100%25)-00C853?style=for-the-badge&logo=githubactions&logoColor=white)](https://github.com/ajiarl/FinanceTrackerMobile)
[![Download APK](https://img.shields.io/badge/Download-APK%20(v1.0.0)-FF6F00?style=for-the-badge&logo=android&logoColor=white)](https://github.com/ajiarl/FinanceTrackerMobile/releases/latest)
[![License](https://img.shields.io/badge/License-MIT-000000?style=for-the-badge)](LICENSE)

> **High-performance, offline-first personal finance tracker for Android built with Jetpack Compose, high-contrast Neobrutalism design system, Room Database, and dual-tier intelligence (Cloud Groq AI + On-Device Fallback).**

---

## 📑 Table of Contents

- [Overview](#-overview)
- [Design Philosophy: Authentic Neobrutalism](#-design-philosophy-authentic-neobrutalism)
- [Key Features](#-key-features)
- [System Architecture](#-system-architecture)
- [Security & Hardware Cryptography](#-security--hardware-cryptography)
- [Technology Stack](#-technology-stack)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Build and Run](#build-and-run)
  - [Running Tests](#running-tests)
- [Project Directory Structure](#-project-directory-structure)
- [Testing & Quality Assurance](#-testing--quality-assurance)
- [License & Author](#-license--author)

---

## 🎯 Overview

**FinanceTracker Mobile** is designed from the ground up to solve common flaws in conventional finance apps: cluttered dashboards, intrusive cloud tracking, soft low-contrast UI, and fragile network-dependent analytics. 

By combining a **local-first SQLite database**, zero-latency offline transaction handling, and an **Android Keystore-backed AES-256-GCM** vault, your sensitive financial records stay strictly on your device. For financial intelligence, the app bridges remote **Groq Cloud AI (Llama 3.3 70B Versatile)** with an autonomous **dynamic local fallback engine** that parses spending clusters without internet access.

---

## 🎨 Design Philosophy: Authentic Neobrutalism

Unlike generic material dashboards, FinanceTracker embraces bold, utilitarian **Neobrutalism**:
- **Hard High-Contrast Geometry**: 2–3dp solid black borders (`#000000`) across cards, badges, and modals.
- **Offset Drop Shadows**: Crisp 4dp offset solid elevation shadows without blurred gradients.
- **Vibrant Functional Accents**:
  - `RetroYellow (#FAFF00)` — Primary calls to action and interactive highlights.
  - `CyberMint (#00F0FF)` & `SoftCyan (#E0F7FA)` — Incomes and net balance indicators.
  - `CoralPink (#FF6B6B)` & `CrimsonRed (#DC2626)` — Expense warnings, critical limits, and overbudget indicators.
  - `LavenderCard (#E2D9F3)` & `PaperCard (#F8F9FA)` — High-readability surfaces.
- **Strict WCAG AAA Contrast**: Dark foreground typography (`#0F172A`) over light slate surfaces (`#E2E8F0`) with minimum 13.75:1 contrast ratios.

---

## ⚡ Key Features

### 1. 📊 Real-Time Financial Dashboard & Analytics
- **Live Net Balance & Velocity**: Immediate calculation of total balance, income vs. expense run-rates, and monthly savings rate.
- **Multi-Period Cashflow Bar Chart**: Responsive monthly breakdown with isolated axis alignment to eliminate overlapping text collisions.
- **Dynamic Category Allocation**: Visual spend distribution across categories (Food, Transport, Bills, Shopping, Health, etc.).

### 2. 🤖 Hybrid Dual-Tier AI Financial Insights ("Pak Hemat")
- **Cloud AI (Groq Llama 3.3 70B)**: Deep financial audits, savings recommendations, and spending habit critiques.
- **Prompt Injection Defense**: Multi-stage input sanitizer stripping jailbreaks, control tokens, and instruction overrides before API delivery.
- **Autonomous Local Fallback**: Dynamic 8-cluster offline analysis evaluating multi-category outliers, negative cashflow anomalies, and fixed overhead ratios.

### 3. 🚨 Predictive Budgeting & Overbudget Alert Engine
- **Category Threshold Monitoring**: Real-time evaluation against customizable category limits.
- **Tiered Warning System**: Dual-state indicators (**Warning 80%** vs. **Critical Overbudget 100%+**).
- **In-App Notification Center**: Instant visual alert badges highlighting actionable savings opportunities.

### 4. 🔒 Enterprise-Grade Privacy & Data Portability
- **Zero-Cloud Requirement**: Operates 100% offline out of the box.
- **Atomic CSV Import/Export**: Robust backup engine with RFC 4180 parsing, automated column reconciliation, and duplicate transaction prevention.
- **Android Keystore AES-256-GCM**: Hardware-level encryption for sensitive configurations and third-party API keys (`EncryptedSharedPreferences`).

---

## 🏛 System Architecture

The project follows clean architecture principles combined with modern **MVI (Model-View-Intent) / unidirectional data flow**:

```
┌────────────────────────────────────────────────────────┐
│                   UI Layer (Compose)                   │
│  Activities · Screens (Dashboard, Settings, Reports)   │
│  Components (Neobrutal Cards, Dialogs, Charts)         │
└───────────────────────────┬────────────────────────────┘
                            │ StateFlow / Events
┌───────────────────────────▼────────────────────────────┐
│                  ViewModel Layer (MVI)                 │
│  FinanceDashboardViewModel · Budget & Alert Evaluators │
└───────────────────────────┬────────────────────────────┘
                            │ Domain Models / Operations
┌───────────────────────────▼────────────────────────────┐
│                    Repository Layer                    │
│  FinanceRepositoryImpl · CsvTransactionRepository      │
└─────────────┬───────────────────────────┬──────────────┘
              │                           │
┌─────────────▼─────────────┐ ┌───────────▼──────────────┐
│       Storage Layer       │ │      Service Layer       │
│  Room Database (SQLite v6) │ │  AiInsightService (Groq) │
│  EncryptedSharedPreferences│ │  Dynamic Fallback Engine │
└───────────────────────────┘ └──────────────────────────┘
```

---

## 🛡 Security & Hardware Cryptography

- **Keystore Isolation**: Sensitive keys (such as Groq Cloud API tokens) are never stored in plaintext SQLite databases or shared properties. They are protected using `ApiKeyStorage.kt` backed by Android's hardware security module (`MasterKey` / AES-256-GCM).
- **Logcat Redaction**: Kiosk and release builds enforce strict token sanitization; sensitive strings appear as `[REDACTED]` across execution logs.
- **Memory Hygiene**: Encrypted key values are wiped and retrieved strictly upon active HTTPS network requests.

---

## 🛠 Technology Stack

| Layer | Technologies |
|---|---|
| **Language** | Kotlin 2.0.21 (JVM 17 Target) |
| **UI Framework** | Jetpack Compose (BOM 2024.10.00), Compose Material 3 |
| **Architecture** | Clean Architecture, MVI Pattern, StateFlow / SharedFlow |
| **Local Database** | Room Database 2.6.1 (SQLite v6, Flow-based queries) |
| **Cryptography** | AndroidX Security Crypto 1.1.0-alpha06 (MasterKeys, Keystore) |
| **Cloud AI** | Groq Cloud API (Llama 3.3 70B Versatile), OkHttp3 4.12.0 |
| **Build System** | Gradle 8.13, Android Gradle Plugin 8.7.1, Kotlin KSP 2.0.21 |
| **Testing** | JUnit 4, Kotlinx Coroutines Test, Turbine, Robolectric |

---

## 🚀 Getting Started

### 📱 Quick Install (Direct APK)
Anyone can directly download and try the app without setting up Android Studio:
1. Go to [**GitHub Releases (Latest v1.0.0)**](https://github.com/ajiarl/FinanceTrackerMobile/releases/latest).
2. Download **`FinanceTrackerMobile-v1.0.0.apk`**.
3. Open the downloaded APK on your Android device and tap **Install** (allow "Install from Unknown Sources" if prompted).

---

### 💻 Prerequisites (For Developers)
- **Android Studio**: Ladybug (2024.2.1) or newer.
- **JDK**: Java Development Kit 17 (LTS).
- **Android SDK**: `compileSdk = 37`, `minSdk = 24` (Android 7.0 Nougat or higher).
- **Device**: Android physical device with USB Debugging enabled, or Android Emulator.

### Build and Run

1. **Clone the repository:**
   ```bash
   git clone https://github.com/ajiarl/FinanceTrackerMobile.git
   cd FinanceTrackerMobile
   ```

2. **Assemble the debug build:**
   ```bash
   # Linux / macOS
   ./gradlew assembleDebug

   # Windows
   gradlew.bat assembleDebug
   ```

3. **Install on target device via ADB:**
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

*(Optional)* To enable online Groq AI insights, open the in-app **Settings** menu and insert your personal API key from [Groq Console](https://console.groq.com). If left empty, the application automatically runs on the autonomous local fallback engine.

### Running Tests

Execute the comprehensive test suite directly from your terminal:
```bash
./gradlew testDebugUnitTest
```

---

## 📂 Project Directory Structure

```text
app/src/
├── main/
│   ├── java/com/sena/financetracker/
│   │   ├── data/                 # Database entities, DAOs, Room database migrations
│   │   │   ├── dao/              # TransactionDao, BudgetDao, NotificationDao
│   │   │   ├── entity/           # TransactionEntity, BudgetEntity, NotificationEntity
│   │   │   └── security/         # ApiKeyStorage (AES-256-GCM Keystore)
│   │   ├── domain/               # Domain models, CSV parser, validation contracts
│   │   ├── repository/           # Repository implementations & data boundaries
│   │   ├── service/              # AiInsightService, Groq client, prompt defense
│   │   ├── ui/                   # Jetpack Compose UI
│   │   │   ├── components/       # Neobrutal buttons, dialogs, badges, inputs
│   │   │   ├── dashboard/        # Dashboard screen, cards, cashflow charts, reports
│   │   │   └── theme/            # Color palettes, typography, Brutalist design tokens
│   │   └── viewmodel/            # DashboardViewModel, MVI state & intent evaluators
│   └── res/                      # Android resources, XML configs, strings
└── test/                         # Unit tests suite (175 tests, 100% passing)
```

---

## 🧪 Testing & Quality Assurance

Code stability is enforced through a strict double-gate verification pipeline:
- **175 Automated Unit Tests** covering:
  - Cryptographic Keystore key lifecycle and encryption/decryption boundaries.
  - CSV import/export RFC 4180 parsing edge cases.
  - Prompt sanitization and jailbreak neutralization.
  - Cashflow chart math alignment and label collision prevention (`CashflowChartLogicTest`).
  - Budget calculations, 80%/100% threshold triggers, and in-app alerts.
- **Zero-Warning Architecture**: Verified clean imports, lint-checked Kotlin code, and deterministic Git worktrees.

---

## 📄 License & Author

Distributed under the **MIT License**. See `LICENSE` for more information.

- **Author**: [Aji Arlando](https://github.com/ajiarl)
- **Repository**: [https://github.com/ajiarl/FinanceTrackerMobile](https://github.com/ajiarl/FinanceTrackerMobile)
