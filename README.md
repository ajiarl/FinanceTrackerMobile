# 💸 FinanceTracker Mobile (Neobrutalism Edition)

A high-performance, offline-first personal finance tracker for Android built with **Jetpack Compose**, **Room Database**, and integrated with **Groq AI (Llama 3)**.

Designed with a bold **Neobrutalism** aesthetic, strict financial validation, and production-grade security architecture.

---

## 🌟 Key Highlights

- **Bold Neobrutalism Design System**: High-contrast typography, heavy borders (2-3dp), hard solid drop-shadow offsets, and vibrant retro color palettes (RetroYellow, NeoCyan, NeoPurple, Slate).
- **100% Offline-First Architecture**: Built on Room SQLite (Schema v6) with reactive StateFlow streams, instant in-memory balance recalculations, and automated ledger balance reconciliations.
- **Pak Hemat · AI Insight Engine**:
  - **Cloud Mode**: Direct integration with Groq API (Llama 3 70B/8B) with XML prompt isolation and anti-prompt injection sanitization.
  - **Dynamic Local Fallback**: Deterministic offline roasting engine categorized into 8+ spending clusters (F&B, E-Commerce, Gaming/Entertainment, Transport, Gadgets/Hardware, Utilities, etc.) that cites actual user transaction titles and amounts.
- **Hardware-Backed Keystore Security**: API keys are securely encrypted via Android Keystore (`ApiKeyStorage`) with AES-256-GCM encryption and masked previews (`gsk_••••••••xxxx`).
- **Comprehensive Test Suite**: **175 Unit Tests (100% passing)** covering currency parsing, cashflow chart collision math, ledger reconciliations, CSV atomic exports, and security barriers.

---

## 🏗 Tech Stack & Architecture

- **Language**: Kotlin 2.0+
- **UI Toolkit**: Jetpack Compose (Material3 + Custom Neobrutal Design System)
- **Architecture**: MVI / MVVM Pattern with StateFlow & SharedFlow
- **Local Persistence**: Room SQLite v6 with Migration Engine
- **Asynchronous**: Kotlin Coroutines & Flow
- **Security**: Android Keystore AES-256-GCM (`EncryptedSharedPreferences`)
- **Networking**: OkHttp 4 / Retrofit for Groq AI endpoints
- **Testing**: JUnit 4, Robolectric, Kotlinx Coroutines Test (175 tests)

---

## 📊 Features

1. **Dashboard & Fast-Add Modal**:
   - Quick expense & income recording with smart category selectors.
   - Neobrutal visual feedback with haptic integration.
   - Form validation with clear visual status helpers.
2. **Account & Multi-Wallet Management**:
   - Cash, Bank, E-Wallet, and Investment wallets.
   - Atomic inter-account transfers with single-transaction rollback safety.
3. **Interactive Cashflow & Financial Reports**:
   - Monthly cashflow bar charts (Income vs Expense) with smart horizontal label isolation and collision avoidance.
   - Category composition breakdowns and Net Savings Rate calculation.
4. **AI Financial Advisor (Pak Hemat)**:
   - Evaluates monthly burn rates and delivers witty, actionable financial roasts.
   - Cloud vs Local toggle with real-time connection status badges.
5. **Data Portability**:
   - Atomic CSV ledger export & import with RFC-4180 parsing compliance.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 17 or JDK 21
- Android SDK 35 (compileSdk 35, minSdk 26)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/ajiarl/FinanceTrackerMobile.git
cd FinanceTrackerMobile

# Run unit tests (175 tests)
./gradlew testDebugUnitTest

# Build debug APK
./gradlew assembleDebug
```

---

## 🔒 Security & Privacy

- **Zero Data Leakage**: All financial transaction records reside locally on your physical device in SQLite. No tracking, no telemetry, no analytics.
- **API Key Safety**: Groq API Keys are stored strictly inside the device hardware Keystore. Keys are never logged in Logcat, never sent to external third parties, and never committed to source control.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
