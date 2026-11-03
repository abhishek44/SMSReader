# SMSReader

A small, offline-first personal Android finance utility built with Kotlin, Jetpack Compose, Room 3, MVVM and Coroutines.

## What it does

- Uses `READ_SMS` only. There is no SMS receiver, background service or real-time notification processing.
- On the first successful permission grant, scans the previous **3 calendar months** of inbox SMS.
- Filters likely bank/card/UPI transaction alerts and puts them in a **Pending Review** queue.
- On later app opens, scans incrementally with a one-day lookback and deduplicates by the Android SMS provider ID.
- User confirms the account, main/subcategory, amount, date and CREDIT/DEBIT type before a transaction enters the ledger.
- Analytics supports current month, current year and custom date ranges plus account/category filters.
- Budgets support monthly and yearly limits per main category.
- All data stays on-device in Room. App backup is disabled.

## Requirements

- Android Studio compatible with Android Gradle Plugin 9.3
- JDK 17+
- Android SDK 37 installed
- Physical Android device with SMS messages (recommended). Emulators normally have no useful bank SMS history.

The project uses:
- AGP 9.3.1 / Gradle 9.5
- Kotlin 2.4.21
- Compose BOM 2026.09.00
- Room 3.0.3
- SQLite 2.7.1

For reproducible compatibility with Kotlin 2.4.21, this AGP 9.x project explicitly opts out of AGP's built-in Kotlin mode and applies the Kotlin Android plugin. This opt-out is supported in AGP 9.x; it can be migrated to built-in Kotlin before an eventual AGP 10 upgrade.

## Run

1. Open this folder in Android Studio.
2. Let Gradle sync and install missing SDK 37 components if prompted.
3. Connect your Android device.
4. Run the `app` configuration.
5. Grant SMS permission when requested.
6. The first scan imports candidates from the previous 3 months.
7. Open **Manage** on the Review screen and add your accounts. Default categories are created automatically.
8. Review pending alerts and save confirmed transactions.

## Important permission note

`READ_SMS` is a dangerous, hard-restricted Android permission. This project is intended for personal sideloading, as requested. Depending on Android version, OEM and installer, the installer may need to allowlist the restricted permission before Android will grant it. The app cannot bypass that platform restriction.

On Android 17 / API 37, SMS that the OS classifies as OTP messages can also be withheld from normal SMS-provider access for a period of time. The incremental scan intentionally looks back one day so delayed messages can be picked up later.

## Parser behavior

The included parser is intentionally recall-oriented and generic. It detects INR/₹/Rs amounts and common debit/credit/UPI/card keywords. Because banks use different SMS templates, every candidate is reviewable and editable before saving.

If a legitimate bank template is missed, add a bank-specific parser or extend `GenericTransactionSmsParser.kt`. The UI and database do not depend on parser-specific logic.

## Main source areas

- `data/sms/` - Android SMS access and parsing
- `data/local/` - Room entities/DAOs/database
- `data/repository/` - business/data orchestration
- `ui/review/` - permission, import and transaction review
- `ui/analytics/` - expenditure analysis
- `ui/budget/` - category budgets

## Security choices

- No network permission
- No server/backend
- No raw SMS logging
- Android backup disabled
- Money stored as integer minor units (`Long`) instead of `Double`
## Validation performed before packaging

- Pure Kotlin parser/money smoke tests pass, including debit extraction, OTP exclusion and failed-payment exclusion.
- AndroidManifest and all XML resources parse successfully.
- Verified there is no `RECEIVE_SMS`, `BroadcastReceiver`, WorkManager or boot receiver in the app.
- Verified the first successful import uses a three-calendar-month lookback and later scans deduplicate by SMS provider ID.
- Dependency/API choices were checked against current Android/Kotlin/Room documentation.

A full Android Gradle compile was not possible in the packaging environment because it has no Android SDK and outbound Gradle artifact downloads are disabled. Android Studio will download Gradle/dependencies during the first sync on a normal development machine.

