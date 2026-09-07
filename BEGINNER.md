# Start here

This app is a **review catalog** of Jetpack Security (`androidx.security`) plus a few Android APIs people mix up with it. It is not a bank, a wallet, or a password manager.

You will tap a screen, then open one Kotlin file. That is the whole loop. The catalog list is in this same order.

Deeper notes: [OVERVIEW.md](OVERVIEW.md)

---

## Run it

1. Open this folder in Android Studio.
2. Wait for Gradle sync.
3. Pick a phone or emulator (API 24 is enough; API 37 is the compile target).
4. Press Run.
5. You land on a list of labs.

You do not need an API 37 device to read the code.

---

## 1. Save a token (do this first)

Open **Encrypted preferences**. Leave the era on **Current: Tink + DataStore**.

Tap **Save sample**, then **Read sample**. You should see `review-secret`.

Open `TinkSecretStore.kt`. Ciphertext lives in DataStore. The key lives in Android Keystore via `com.google.crypto.tink.Aead`.

**You now know:** DataStore does not encrypt. Tink + Keystore does.

---

## 2. The old helper (same screen)

Switch era to **Previous: security-crypto**. Save and read again.

Open `LegacySecretStore.kt`. That is `androidx.security.crypto.EncryptedSharedPreferences` — deprecated in 1.1.0. Fine for reading old installs, not for new features.

Optional: switch era to **Neighbor: Android Keystore** to see raw `javax.crypto.Cipher` AES-GCM.

**You now know:** New secrets should not start on EncryptedSharedPreferences.

---

## 3. Encrypted files

Open **Encrypted files**. Try all three eras. Tap **Write sample file**, then **Read sample file**.

Open `TinkBlobStore.kt` (current), `LegacyBlobStore.kt` (deprecated `EncryptedFile`), `KeystoreBlobStore.kt` (raw Cipher).

**You now know:** Files use the same three eras as prefs. Streaming AEAD is for large blobs.

---

## 4. Move off security-crypto

Open **Crypto migration**.

Tap **1. Seed legacy prefs**, then **2. Migrate to Tink**, then **Show both stores**.

Open `MigrationLab.kt`. It copies the value, reads it back, and only then deletes the old prefs file.

**You now know:** Migrate by copy, verify, then delete. Do not drop the old library until the new read works.

---

## 5. Do not back up ciphertext

Open **Backup exclusion**. Tap **Show backup policy**.

Open `res/xml/data_extraction_rules.xml` and `BackupLab.kt`. Keystore keys do not travel with Auto Backup. Restored ciphertext will not decrypt.

**You now know:** Exclude encrypted files from backup, or turn backup off.

---

## 6. Who is the other app?

Open **App Authenticator**. Tap **Run identity checks**.

Open `AppAuthenticatorLab.kt` and `res/xml/app_authenticator.xml`. `androidx.security.app.authenticator.AppAuthenticator` checks a signing-cert digest, not a package name.

The checked-in XML uses a placeholder digest, so the static check is usually `NO_MATCH`. That is expected.

**You now know:** Package name is not identity. The signing certificate is.

---

## 7. Where keys live

Open **Android Keystore**. Tap **Inspect keys**.

Open `KeystoreInfoLab.kt`. Look at `android.security.keystore.KeyGenParameterSpec` and `KeyInfo`. StrongBox is a separate chip; it fails on phones that do not have one.

**You now know:** Your app holds a handle. The key stays in Keystore.

---

## 8. Unlock a key with a fingerprint

Open **Biometric-gated key**. Tap **Check availability**, then **Encrypt sample**.

Open `BiometricKeyLab.kt`. This is `androidx.biometric.BiometricPrompt`, not `androidx.security`. The `Cipher` goes into `BiometricPrompt.CryptoObject` so the OS unlocks that object.

Emulators with no enrolled fingerprint will fail. That is expected.

**You now know:** Auth-bound keys need a CryptoObject, not a boolean you remember to check.

---

## 9. Would a bank even run here?

Open **Banking app readiness**. Tap **Evaluate this device**.

Open `BankingReadinessLab.kt`. Green means no blocking failures. Red means at least one blocking failure. Optional rows can fail without turning the banner red.

This cannot see a specific bank’s Play listing or a Play Integrity token. An emulator is usually red. That is the lesson.

**You now know:** Banks score device signals. A third-party app cannot ask Play “would Chase install here?”

---

## 10. Block screenshots

Open **Screenshot / FLAG_SECURE**. Tap **Evaluate this device**. Try a screenshot while this screen is open.

Open `ScreenCaptureLab.kt`. Banks set `WindowManager.LayoutParams.FLAG_SECURE` on login screens. This lab sets it on *this* window. It cannot see another app’s APK.

**You now know:** Screenshot blocking is a window flag, not Jetpack Security.

---

## 11. Stop fake overlay buttons

Open **Overlay / tapjacking**. Tap **Evaluate this device**.

Open `OverlayTapjackingLab.kt`. `View.filterTouchesWhenObscured` drops taps when another window covers yours. Accessibility services are optional so TalkBack is not treated as malware.

**You now know:** A fake Confirm button drawn over yours is tapjacking.

---

## 12. How did this APK get here?

Open **Installer source**. Tap **Evaluate this device**.

Open `InstallSourceLab.kt`. Play-only apps want `com.android.vending`. An Android Studio debug run fails on purpose (`FLAG_DEBUGGABLE`). This is not Play Integrity.

**You now know:** Installer package is an on-device signal, not a store verdict.

---

## 13. Tap-to-pay floor

Open **Contactless payment (NFC)**. Tap **Evaluate this device**.

Open `ContactlessPaymentLab.kt`. Wallets need NFC, host card emulation (`FEATURE_NFC_HOST_CARD_EMULATION`), and a lock screen. This catalog is not a wallet and calls no card network.

**You now know:** Android tap-to-pay is HCE plus a lock screen, not `androidx.security`.

---

## 14. Specialized (optional)

Do these last. They are real APIs, just less common.

- **Identity Credential** — `IdentityCredentialLab.kt`. Mobile documents (mDL), still alpha. Not login.
- **Security State** — `SecurityStateLab.kt`. `androidx.security.state.SecurityStateManagerCompat`. Patch levels. Often empty on emulators.
- **Credential Manager** — `CredentialManagerLab.kt`. `androidx.credentials.CredentialManager`. User passwords and passkeys. A fresh emulator throws `NoCredentialException`.

**You now know:** Identity Credential, Security State, and Credential Manager solve three different problems. None of them replace Tink + DataStore.

---

## Want to add a lab?

Keep it small. Copy the banking-style labs, not the API-probe labs.

1. Add a route in `LabDestination.kt`.
2. Write a `*Lab.kt` that returns `PolicyReport` (`policy/PolicyReport.kt`): named checks, `passed`, `blocking`, a one-line `detail`.
3. Add a screen in `ui/labs/PolicyLabs.kt` with `PolicyLabScreen` (one Evaluate button, green/red banner).
4. Wire the route in `MainActivity.kt`.
5. Add a short step in **this file**.

Good labs are checklists a real app would care about: PASS/FAIL, blocking vs optional, honest about what you cannot see (no backend, no Play Integrity Cloud project, no “this is Chase’s policy”).

Bad labs are “call an API and print a bundle” with no allowed/not-allowed banner.
