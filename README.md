# M-Pesa Tracker

An offline Android expense tracker for Kenya's M-Pesa mobile money. It reads
M-Pesa confirmation SMS on the device, turns each one into a structured
transaction, sorts it into a spending category and shows where the month's
money went.

Built with **Kotlin, Jetpack Compose, Room and Coroutines/Flow**, following
MVVM with a repository layer. The SMS parser and categoriser are pure Kotlin
and covered by JVM unit tests.

[![Build APK](https://github.com/marky376/pesa-tracker/actions/workflows/build-apk.yml/badge.svg)](https://github.com/marky376/pesa-tracker/actions/workflows/build-apk.yml)

**Download:** the latest debug APK is on the
[Releases page](https://github.com/marky376/pesa-tracker/releases/tag/latest),
built by GitHub Actions on every push to `main`.

<!-- Screenshots: add captures taken with sample data loaded, then uncomment.
| Dashboard | History | Add transaction |
|-----------|---------|-----------------|
| ![Dashboard](docs/screenshots/dashboard.png) | ![History](docs/screenshots/history.png) | ![Add](docs/screenshots/add.png) |
-->

## Why

Most people in Kenya pay for food, transport, airtime and bills through
M-Pesa, and every payment arrives as a text message. Those messages already
hold the amount, payee, date and fee, but nobody reads back through hundreds
of SMS to see a monthly picture. This app does that on the phone, with no
account and no server.

## Features

- **SMS import:** reads M-Pesa messages from the inbox after a runtime
  `READ_SMS` permission prompt, and parses six message types: send money,
  Paybill, Buy Goods (Till), money received, airtime and agent withdrawal.
  Each message yields the transaction code, amount, counterparty, Paybill
  account, date, transaction fee and balance.
- **Duplicate-safe:** the M-Pesa code has a unique index, so importing the
  inbox again only adds new transactions.
- **Automatic categories:** whole-word keyword rules map payees to Food,
  Transport, Airtime, Bills or Other (for example `KPLC` is Bills, `NAIVAS` is
  Food), with a fallback on transaction type. Any category can be corrected
  in History.
- **Dashboard:** this month's spending, money received and M-Pesa charges, a
  spending-by-category donut chart drawn on a Compose `Canvas`, and the latest
  transactions.
- **History:** search by payee or M-Pesa code, filter by category and by
  period (this month, last 30 days, last month, all time); edit a category or
  delete a transaction.
- **Manual entry:** add cash or other spending by hand, with input
  validation, a date picker and a category suggested live from the same
  keyword rules.
- **Sample data:** one tap loads realistic demo messages that go through the
  real parser, so the app can be shown on an emulator with no SMS.

## Tech stack

| Area | Choice |
|------|--------|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.10.01), Material 3, Navigation Compose 2.8.4 |
| State | AndroidX ViewModel + Kotlin `StateFlow` / `Flow` |
| Storage | Room 2.6.1 (SQLite) with KSP code generation |
| Async | Kotlin Coroutines (`viewModelScope`, `Dispatchers.IO`) |
| Build | Gradle 8.9 (Kotlin DSL), AGP 8.7.3, version catalog in `gradle/libs.versions.toml` |
| Testing | JUnit 4 |
| Platform | minSdk 24 (Android 7.0), target/compile SDK 35 |

## Architecture

```
Compose screens  ──>  ViewModels  ──>  TransactionRepository  ──>  Room DAO  ──>  SQLite
 (Dashboard,          (StateFlow        (import logic:               (Flow-returning
  History, Add)        UI state)         parse → categorise → save)   SQL queries)
                                               │
                              SmsReader ───────┤  (Android SMS content provider)
                                               │
                          domain/ (pure Kotlin): MpesaSmsParser, Categorizer, TimeRange
```

- **MVVM with unidirectional data flow.** Screens only render state and pass
  events up. Each ViewModel exposes one `StateFlow`; the Dashboard combines
  five live queries into a single `DashboardUiState`, and History uses
  `flatMapLatest` to re-run its search whenever the filter changes.
- **Reactive data.** DAO queries return `Flow`, so a new import or edit shows
  up on every screen without manual refreshes. Aggregations (totals, spending
  by category) are done in SQL rather than in memory.
- **Testable core.** The `domain` package has no Android imports. Parsing is
  a set of ordered regular expressions with explicit handling for comma
  amounts, masked phone numbers, agent numbers, lowercase times, 2 or 4 digit
  years, and a fallback to the SMS arrival time when the date can't be read.
  Unrecognised messages (failed transactions, promos) are skipped, never
  saved half-parsed.
- **Simple dependency injection.** The `Application` class owns the database,
  repository and SMS reader; a `viewModelFactory` hands them to ViewModels.

```
app/src/main/java/com/example/mpesatracker/
├── domain/      MpesaSmsParser, Categorizer, TimeRange, SampleData, models
├── data/        Room entity, DAO, type converters, database, repository
├── sms/         SmsReader (content provider query)
└── ui/          Compose screens + ViewModels: dashboard/, history/, add/,
                 shared components (transaction row, donut chart), theme
```

## Build and run

Requirements: Android Studio Ladybug (2024.2.1) or newer, JDK 17+, Android SDK 35.

```
git clone https://github.com/marky376/pesa-tracker.git
cd pesa-tracker
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # install on a connected device or emulator
```

Or open the folder in Android Studio and press **Run**. On an emulator, tap
**Load sample data** on the Dashboard. On a phone, tap **Import M-Pesa SMS**
and allow SMS access.

## Testing

```
./gradlew test
```

20 JVM unit tests across three classes in
`app/src/test/java/com/example/mpesatracker/domain/`:

- `MpesaSmsParserTest` (12): every supported message type, masked phone
  numbers, large amounts with commas, 4-digit years and lowercase times,
  fallback when the date is unreadable, and rejection of non-confirmation
  messages.
- `CategorizerTest` (6): keyword rules per category, type-based rules,
  fallbacks and whole-word matching (so "SHAKES" is not read as the "SHA"
  bills keyword).
- `SampleDataAndTimeRangeTest` (2): every demo message parses with a unique
  code and the right timestamp, and "this month" boundaries are correct in
  Nairobi time.

GitHub Actions runs these tests before building the APK on every push.
Room queries and Compose UI are not covered by automated tests yet (see
roadmap).

## Privacy

- SMS are read **on the device only**. The app declares no `INTERNET`
  permission, so it has no way to send your messages or transactions anywhere.
- Only messages from the `MPESA` sender, or containing "M-PESA", are queried.
  The app reads and never sends SMS.
- Transactions live in a local SQLite database; **Delete all transactions** on
  the Dashboard clears it without touching the SMS inbox.
- Google Play restricts SMS permissions to a few app categories, so this is a
  personal and portfolio project rather than a Play Store release.

## Roadmap

- Automated tests for the Room DAO (in-memory database) and ViewModels.
- Monthly budgets per category with alerts.
- Spending trend chart and comparison with last month.
- Parsing for more message types: Fuliza, M-Shwari and Airtel Money.
- CSV export, and an app lock with biometric unlock.
- Exclude the database from Android Auto Backup (`allowBackup` is currently
  on) so the data never leaves the phone.

## Author

<!-- Your name, link to LinkedIn / portfolio, email -->
