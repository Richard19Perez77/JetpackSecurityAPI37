# Jetpack Security API 37

A **review catalog** of `androidx.security` (plus a few platform neighbors), compiled against SDK 37 with minSdk 24. Not a production app.

**New to this repo?** Read [BEGINNER.md](BEGINNER.md) first, then run the app.

Deeper reference: [OVERVIEW.md](OVERVIEW.md)

## Run

1. Open the folder in Android Studio.
2. Sync Gradle.
3. Run on a device or emulator (API 24+).

```
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
```

Instrumented tests live in `:app` and use `:authenticator-testing`.

## Layout

```
:app
  crypto/          prefs, files, migration (three API eras)
  keystore/        KeyInfo / StrongBox
  authenticator/   XML + cert digest checks
  biometric/       auth-bound AES key
  banking/         device policy banks use
  capture/         FLAG_SECURE
  overlay/         tapjacking
  install/         Play vs sideload
  payment/         NFC / HCE floor
  backup/          excluded backup paths
  identity/        mDL-style store probe
  state/           SPL bundle + mock OEM
  credentials/     Credential Manager probe
  policy/          shared PASS/FAIL report
  ui/              catalog + labs

:authenticator-testing
  TestAuthenticatorFactory for instrumented tests
```
