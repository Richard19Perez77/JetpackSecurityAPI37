# Jetpack Security on API 37

This project is a **review catalog**, not a production app. It targets **compile/target SDK 37** and keeps **minSdk 24** so the older Jetpack Security crypto APIs still compile and run.

**Start with [BEGINNER.md](BEGINNER.md)** (tap the app, then open one file). This file is the deeper reference.

The app screens are thin labs: a short explanation, a few buttons, and a result string. Storage and file labs can **switch API eras** (previous Jetpack helper vs current recommendation vs raw platform APIs). Policy labs (banking, FLAG_SECURE, overlay, installer, NFC) use one Evaluate button and a green/red banner.

---

## What “Jetpack Security” means

**Jetpack** is a set of Android libraries (`androidx.*`) that sit on top of the operating system. They exist so you write less boilerplate, get bug fixes on old devices, and follow a documented pattern.

**Jetpack Security** is the `androidx.security` group. It is **not** every security feature on Android. Login screens, HTTPS, Play Integrity, and most permission APIs live elsewhere.

On this project’s date, the group looks like this:

| Artifact | Role | Status to remember |
| --- | --- | --- |
| `androidx.security:security-crypto` 1.1.0 | Encrypt SharedPreferences and files with a Keystore master key | **Deprecated** (`EncryptedSharedPreferences`, `EncryptedFile`, `MasterKey`) |
| `androidx.security:security-app-authenticator` 1.0.0 | Check that another app is signed with the cert you expect | Stable |
| `androidx.security:security-app-authenticator-testing` 1.0.0 | Fake those checks in tests | Stable; used by `:authenticator-testing` |
| `androidx.security:security-identity-credential` 1.0.0-alpha03 | Mobile identity documents (for example mDL) | Still alpha |
| `androidx.security:security-state` 1.1.0-beta03 | Read security patch levels (SPL) and related state | Beta; richer on API 35+ devices |
| `androidx.security:security-state-provider` | OEM/OTA apps **publish** update info | Privileged; this app **mocks** it instead of hosting a real provider |

This app also shows **neighbor platform APIs** people often mix up with Jetpack Security:

- **Android Keystore** (`AndroidKeyStore`) — where keys live
- **BiometricPrompt** — unlock a Keystore key with a fingerprint/face
- **Credential Manager** — passwords and passkeys for *user login*, not app-to-app identity
- **Auto Backup / data extraction rules** — ciphertext must not restore without keys
- **Window / PackageManager / NFC** — screenshot flags, installer source, tap-to-pay hardware (policy labs)

---

## Mental model: keys, ciphertext, and “who is calling”

Three beginner questions cover almost every lab:

1. **Where is the secret?** On disk it should be *ciphertext*. The *key* should live in **Android Keystore**, which can keep key material inside hardware.
2. **Who may use the key?** Sometimes anyone in your app process. Sometimes only after **biometric** (or PIN) unlock.
3. **Who is the other app?** Package name is not enough (anyone can reuse a name on a sideload). You check the **signing certificate digest**.

Jetpack Security helpers are shortcuts around those three ideas. When a helper is deprecated, the ideas stay; you pick a newer shortcut.

---

## API 37 vs minSdk 24

- **API 37** is the **target/compile** platform this project is written against (new constants, lint, and OS services).
- **minSdk 24** means the APK can still install on older phones. Labs that need newer APIs **check `Build.VERSION.SDK_INT`** and print a skip message.

You do **not** need an API 37 device to *review the code*. You need a device/emulator only if you want to tap the buttons.

---

## 1. Previous best practice: `security-crypto`

### What it tried to do

Around 2020, the usual advice for “store a token on disk” was:

1. Create a **MasterKey** in Android Keystore (`AES256_GCM`).
2. Wrap **SharedPreferences** with `EncryptedSharedPreferences`:
   - preference **keys** encrypted with **AES256-SIV** (so the same key always encrypts to the same ciphertext; needed because prefs are a map)
   - preference **values** encrypted with **AES256-GCM** (authenticated encryption)
3. Wrap files with `EncryptedFile` using **AES256_GCM_HKDF_4KB** streaming encryption.

Internally this used **Google Tink**. The Jetpack types were a friendly facade.

This project still implements that path behind the era named **Previous: security-crypto**.

### Why Google deprecated it in 1.1.0

The classes still *exist* in `security-crypto:1.1.0`, but they are marked **deprecated**. Common reasons you will read in release notes and post-mortems:

- **Main-thread I/O.** `SharedPreferences` is synchronous. Encrypting on first access caused StrictMode jank and ANRs.
- **Keyset / Keystore brittleness** on some OEM devices (corrupted Tink keysets, crashes after backup/restore).
- **SharedPreferences itself is the old persistence API.** Jetpack **DataStore** is the async replacement for prefs.

Official deprecation stubs sometimes say “use SharedPreferences / File / KeyGenerator instead.” That is *technically* true (those are the lower-level types) but incomplete. For *secrets*, you still encrypt. The **current** recipe is usually **Tink (or Keystore AES-GCM) + DataStore/files**, not plaintext prefs.

### When you would still touch it

- Reading old installs during **migration**
- Reviewing historical code (this catalog)

Do not start a new feature on `EncryptedSharedPreferences`.

---

## 2. Current encrypted preferences: Tink + DataStore

**DataStore** writes preferences asynchronously and is transactional. It does **not** encrypt by itself.

**Tink** is Google’s cryptography library. On Android you typically:

1. `AeadConfig.register()`
2. Build an `AndroidKeysetManager` that:
   - stores a **Tink keyset** in a small SharedPreferences file (the keyset is itself encrypted)
   - wraps that keyset with a Keystore key (`android-keystore://your-alias`)
3. Get an **AEAD** primitive (`AES256_GCM`)
4. `encrypt(plaintext, associatedData)` / `decrypt(...)`
5. Save the Base64 ciphertext in DataStore

**Associated data** in this app is the preference key’s bytes. That ties ciphertext to the map key so an attacker cannot swap two values.

Era name in the UI: **Current: Tink + DataStore**.

### Raw Keystore neighbor

The third era, **Neighbor: Android Keystore**, skips Tink. It uses `KeyGenerator` + `AES/GCM/NoPadding`, prepends the 12-byte IV, calls `Cipher.updateAAD` with the preference key (same “do not swap values” idea as Tink), and stores Base64 in ordinary SharedPreferences. This is what you do when you want fewer libraries. You must get IV, tag length, AAD, and key spec right yourself. Tink exists so fewer apps invent broken crypto.

---

## 3. Encrypted files

| Era | Helper | Primitive |
| --- | --- | --- |
| Previous | `EncryptedFile` | Tink streaming AEAD, 4 KB chunks |
| Current | `StreamingAead` via `AndroidKeysetManager` | `AES256_GCM_HKDF_4KB` |
| Neighbor | `Cipher` AES-GCM | Whole file in one `doFinal` (fine for tiny demos, not huge videos) |

Streaming AEAD is for **large** files: encrypt in chunks without loading the whole file into RAM.

Associated data in the Tink file lab is the file name, for the same “do not swap files” reason.

---

## 4. Migration lab

A realistic upgrade path:

1. Keep `security-crypto` **temporarily**.
2. On first launch, read values from `EncryptedSharedPreferences`.
3. Write them into Tink+DataStore.
4. Delete the old prefs file only after the new read succeeds.
5. Later, drop the `security-crypto` dependency.

This app’s **Crypto migration** lab does that with a fake `session_token`. Production code should also handle type variants (not only strings), crashes mid-migration, and multiple processes.

**Backup warning:** if Auto Backup restored the old encrypted XML onto a phone with a **new** Keystore, decryption throws. Exclude those files (see Â§10).

---

## 5. Android Keystore and StrongBox

Think of Keystore as a **safe**. Your app asks the OS to create key `jetsec_direct_aes`. The safe can:

- refuse to export the raw key
- insist on biometric before use
- live in a **Trusted Execution Environment (TEE)** or in **StrongBox** (a dedicated secure chip, `FEATURE_STRONGBOX_KEYSTORE`)

`KeyInfo` tells you whether a key is inside secure hardware and (on newer APIs) its security level. StrongBox generation **fails** on devices without the chip; the lab catches that.

Jetpack `MasterKey` was a wrapper around this. The deprecation text points you back to `KeyGenerator` + `AndroidKeyStore`.

---

## 6. App Authenticator

### The problem

You have an exported service, FileProvider, or custom permission. Another app calls you. You must not trust:

- package name alone
- “it is installed from Play” without checking

You trust a **SHA-256 digest of the signing certificate**.

### The XML

`res/xml/app_authenticator.xml` lists package names and `cert-digest` values.

```xml
<app-authenticator>
  <expected-identity>
    <package name="com.example.other">
      <cert-digest>ab12…</cert-digest>
    </package>
  </expected-identity>
</app-authenticator>
```

You can also nest packages under a `<permission>` tag to describe who may hold a custom permission.

### The API

- `AppAuthenticator.createFromResource(context, R.xml.app_authenticator)`
- `checkAppIdentity(packageName)` → `SIGNATURE_MATCH` or `SIGNATURE_NO_MATCH`
- `enforceAppIdentity(packageName)` → throws `SecurityException` on mismatch
- Calling-app variants exist for Binder callers (`checkCallingAppIdentity`)

The lab computes **this app’s** debug cert digest. The checked-in XML uses a **placeholder** digest, so the static check is usually `NO_MATCH` on your machine. The lab then builds XML **in memory** with the real digest (expect MATCH) and a fake digest (expect NO_MATCH).

Play Store’s digest in the sample XML is illustrative only.

### Testing module (`:authenticator-testing`)

You should not need a second signed APK to unit-test policies. `TestAppAuthenticatorBuilder` (artifact `security-app-authenticator-testing`) can:

- `POLICY_SIGNATURE_ACCEPTED_FOR_DECLARED_PACKAGES` — MATCH for names listed in XML
- `POLICY_DENY_ALL` — always NO_MATCH

`TestAuthenticatorFactory` wraps those policies. Instrumented tests in `AppAuthenticatorPolicyTest` use `app/src/androidTest/res/xml/test_app_authenticator.xml`.

---

## 7. Identity Credential (still alpha)

This is for **government-style documents** in the phone (ISO mDL, etc.), not for “login with Google.”

On **API 30+**, the platform class `android.security.identity.IdentityCredentialStore` is **hardware-backed** and returns `null` when the device has no identity HAL.

The **Jetpack** library `security-identity-credential` forwards to hardware when present; otherwise it uses an **Android Keystore-backed software** implementation. That software path is weaker privacy-wise (Google’s own docs say this) but enough when the **issuer signs** all data.

A real issuance flow needs:

- an issuing authority
- `PersonalizationData` (namespaces, CBOR, access control profiles)
- later, a presentation session to a verifier, often with biometrics

This catalog only **probes** the store and tries to create a **CredentialKey** certificate chain, then deletes the demo credential. If `createCredential` throws `DocTypeNotSupportedException`, the result text still teaches you that doc types are store-specific.

---

## 8. Security State (SPL, CVEs, OEM providers)

**Security Patch Level (SPL)** is the date-like string in `Build.VERSION.SECURITY_PATCH` (and vendor/kernel/module variants). It answers “which Android Security Bulletin is this build supposed to include?”

On **API 35+**, `android.os.SecurityStateManager` exposes a **bundle** of component → version/SPL mappings, plus supplemental CVE lists. Jetpack `SecurityStateManagerCompat` wraps that for older compile paths and extra module metadata.

`security-state` 1.1 also talks to **trusted update providers** (OEM updaters, Play system updates) to ask “is a newer SPL available?” Those providers are **system/privileged** (the library evolved from a ContentProvider to a bound `UpdateInfoService` with identity checks). A normal Play app does not implement `security-state-provider`.

This lab:

1. Prints the **live** compat bundle (often **empty on emulators**).
2. Prints a **scripted OEM sample** so reviewers can see the *shape* of `UpdateInfo` (component, SPL, published date, URI) without fake system services.

---

## 9. Biometric-gated keys (neighbor)

`BiometricPrompt` is `androidx.biometric`, not `androidx.security`. It becomes a Security topic when you set:

```
KeyGenParameterSpec.Builder(...)
  .setUserAuthenticationRequired(true)
```

Then `Cipher.init` prepares the object, and you pass that same `Cipher` into `BiometricPrompt.CryptoObject` so the OS unlocks **that** crypto object (not a generic “user is in” flag you might forget to check). This lab uses a **per-use** key (timeout 0 / validity `-1` on older APIs). A 15-second validity window is a different pattern: `Cipher.init` throws `UserNotAuthenticatedException` until the user authenticates, and you must not mix that with CryptoObject.

`BIOMETRIC_STRONG` is the Class 3 authenticator, which is what you want for keys. Emulators without an enrolled fingerprint will report `BIOMETRIC_ERROR_NONE_ENROLLED` or a prompt error. That is expected.

`setInvalidatedByBiometricEnrollment(true)` destroys the key if the user adds a new biometric. That is usually what you want for high-value secrets.

---

## 10. Credential Manager (neighbor)

**Credential Manager** (`androidx.credentials`) stores **user** passwords and **passkeys** (WebAuthn) with the system password manager. It replaces many custom “remember me” files and the old `Smart Lock for Passwords` flow.

It does **not** replace:

- App Authenticator (that is *other apps’ signing certs*)
- Tink/DataStore (that is *your app’s private tokens*)

The lab only calls `CredentialManager.create()` and `getCredential` with `GetPasswordOption`. A clean emulator typically throws `NoCredentialException`. Passkeys further need Digital Asset Links / a real relying party ID; this review app does not set that up.

---

## 11. Backup and device transfer

`android:allowBackup="true"` (the manifest default people copy) will upload app files to Google’s backup and restore them on a new phone.

**Keystore keys do not travel with the backup.** Restored `EncryptedSharedPreferences` XML or Tink ciphertext will not decrypt. Apps then crash in production.

This project keeps backup **enabled** on purpose and **excludes** encrypted prefs, Tink keyset prefs, DataStore files, and encrypted file directories in:

- `res/xml/backup_rules.xml` (devices below API 31)
- `res/xml/data_extraction_rules.xml` (API 31+, cloud backup **and** device-to-device transfer)

Alternative: `android:allowBackup="false"` if you have nothing worth restoring.

---

## 12. Banking app readiness (neighbor)

You **cannot** ask Play “will *this* bank’s APK install on *this* phone?” from a third-party app. Country, ABI, `minSdk` in *that* APK, and the bank’s Play Integrity Cloud project are private.

What you **can** do is score the same device signals banks use after install (and that Play uses for certified devices):

- API 28+ (typical 2026 floor in this lab)
- not an emulator
- `user` build without `test-keys`
- secure lock screen
- hardware-backed Keystore (TEE/StrongBox)
- Play Store + Play services
- Verified Boot green / bootloader locked (when the property is readable)
- no obvious `su` / Magisk package
- security patch newer than one year

**Green banner** = no *blocking* failures. **Red** = at least one blocking failure; each row still shows PASS/FAIL. StrongBox, Class 3 biometrics, and ADB are optional (they do not turn the banner red).

Play Integrity’s `MEETS_STRONG_INTEGRITY` token still needs your backend. This lab only checks that Play services exist.

Shared types for this banner style live in `policy/PolicyReport.kt` (`PolicyCheck` rows, blocking vs optional).

---

## 13. Screenshot / FLAG_SECURE (neighbor)

Banks and password managers set `WindowManager.LayoutParams.FLAG_SECURE` on login and transfer screens. The system then blocks screenshots, recents thumbnails, and most screen-share of that window.

The catalog lab applies the flag to **this** activity while the screen is open, then scores picture-in-picture, split-screen, and extra displays. Screen-capture callbacks (`Activity.registerScreenCaptureCallback`, API 34+) are asynchronous; the lab does not pretend it can prove a kernel recorder is absent.

---

## 14. Overlay / tapjacking (neighbor)

A second window can draw a fake Confirm button over yours. `View.filterTouchesWhenObscured` ignores those taps. On API 31+, `Window.setHideOverlayWindows(true)` (permission `HIDE_OVERLAY_WINDOWS`) hides `TYPE_APPLICATION_OVERLAY` windows.

The lab also fails if *this* app can draw overlays (`Settings.canDrawOverlays`). It cannot list every overlay-capable package without `QUERY_ALL_PACKAGES`. Enabled accessibility services are **optional** so TalkBack is not treated as malware.

---

## 15. Installer source (neighbor)

`PackageManager.getInstallSourceInfo` (API 30+) reports `installingPackageName`, `initiatingPackageName`, and on API 33+ `packageSource` (`PACKAGE_SOURCE_STORE` vs `LOCAL_FILE` / `DOWNLOADED_FILE`).

Play-only banks want `com.android.vending` and a non-debuggable APK. An Android Studio debug run **fails on purpose**. This is not a Play Integrity token.

---

## 16. Contactless payment / NFC (neighbor)

Host-card-emulation wallets need `FEATURE_NFC`, `NfcAdapter.isEnabled`, `FEATURE_NFC_HOST_CARD_EMULATION`, and a secure lock screen. The default payment component (`Settings.Secure` key `nfc_payment_default_component`) is optional here because this catalog is not a wallet. No card network is called.

---

## How the sample app is laid out

```
:app
  crypto/          era switch: prefs, files, migration
  keystore/        KeyInfo / StrongBox
  authenticator/   XML + runtime digest checks
  identity/        Jetpack vs platform store
  state/           live SPL + mock OEM
  biometric/       auth-bound AES key
  credentials/     Credential Manager probe
  banking/         typical banking-app device policy
  capture/         FLAG_SECURE on this window
  overlay/         tapjacking / hide overlays
  install/         Play vs sideload vs debug
  payment/         NFC + HCE floor
  policy/          shared PASS/FAIL report
  backup/          lists excluded paths
  ui/              catalog + simple labs

:authenticator-testing
  TestAuthenticatorFactory for instrumented tests
```

Switch **API era** on Encrypted preferences and Encrypted files. Policy labs share one Evaluate button and a green/red banner.

---

## What this catalog does not cover

Still important, but not `androidx.security` and not implemented here:

- TLS / certificate pinning
- Play Integrity token decryption (needs a Cloud project and your backend)
- Encrypted File-Based Encryption of the whole device (that is Android itself)
- SQLCipher / encrypted Room
- Network security config
- Privileged `UpdateInfoService` implementations

---

## Suggested reading order

Follow [BEGINNER.md](BEGINNER.md) in the app. This file is the background for those steps.

Official index: [Jetpack Security releases](https://developer.android.com/jetpack/androidx/releases/security).

