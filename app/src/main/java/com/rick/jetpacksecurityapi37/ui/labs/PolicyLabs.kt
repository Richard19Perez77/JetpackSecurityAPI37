package com.rick.jetpacksecurityapi37.ui.labs

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.rick.jetpacksecurityapi37.banking.BankingReadinessLab
import com.rick.jetpacksecurityapi37.capture.ScreenCaptureLab
import com.rick.jetpacksecurityapi37.install.InstallSourceLab
import com.rick.jetpacksecurityapi37.overlay.OverlayTapjackingLab
import com.rick.jetpacksecurityapi37.payment.ContactlessPaymentLab
import com.rick.jetpacksecurityapi37.ui.PolicyLabScreen

@Composable
fun BankingLabScreen() {
    val context = LocalContext.current
    val lab = remember { BankingReadinessLab(context) }
    PolicyLabScreen(
        title = "Banking app readiness",
        body = "Play Store does not publish “would this bank install here?” to third-party apps. This lab scores the device signals banks actually use: OS version, emulator, lock screen, hardware Keystore, Play, verified boot, root hints, and patch age. Green means no blocking failures. Red lists what would typically block install or first login.",
        onEvaluate = { lab.evaluate() },
    )
}

@Composable
fun ScreenCaptureLabScreen() {
    val activity = LocalContext.current as Activity
    val lab = remember(activity) { ScreenCaptureLab(activity) }
    DisposableEffect(lab) {
        lab.applyWindowGuards()
        onDispose { lab.clearWindowGuards() }
    }
    PolicyLabScreen(
        title = "Screenshot / FLAG_SECURE",
        body = "Banks and password managers set WindowManager.LayoutParams.FLAG_SECURE on login and transfer screens so screenshots, recents thumbnails, and most screen-share are blocked. This lab applies that flag to this window, then scores PiP, split-screen, and extra displays. It cannot prove a kernel recorder is absent, and it cannot see another app’s APK.",
        onEvaluate = { lab.evaluate() },
    )
}

@Composable
fun OverlayTapjackingLabScreen() {
    val activity = LocalContext.current as Activity
    val lab = remember(activity) { OverlayTapjackingLab(activity) }
    DisposableEffect(lab) {
        lab.applyWindowGuards()
        onDispose { lab.clearWindowGuards() }
    }
    PolicyLabScreen(
        title = "Overlay / tapjacking",
        body = "A login Confirm button under a fake overlay can steal taps. This lab sets View.filterTouchesWhenObscured and, on API 31+, Window.setHideOverlayWindows. It also checks whether this app can draw overlays. It cannot enumerate every overlay-capable package without QUERY_ALL_PACKAGES. Accessibility services are optional so TalkBack is not treated as malware.",
        onEvaluate = { lab.evaluate() },
    )
}

@Composable
fun InstallSourceLabScreen() {
    val context = LocalContext.current
    val lab = remember { InstallSourceLab(context) }
    PolicyLabScreen(
        title = "Installer source",
        body = "PackageManager.getInstallSourceInfo says who installed this APK. Play-only banks want com.android.vending and PACKAGE_SOURCE_STORE. An Android Studio debug run fails on purpose (FLAG_DEBUGGABLE and usually com.android.shell). This is not a Play Integrity verdict.",
        onEvaluate = { lab.evaluate() },
    )
}

@Composable
fun ContactlessPaymentLabScreen() {
    val context = LocalContext.current
    val lab = remember { ContactlessPaymentLab(context) }
    PolicyLabScreen(
        title = "Contactless payment (NFC)",
        body = "Host card emulation (HCE) wallets need NFC hardware, the radio on, FEATURE_NFC_HOST_CARD_EMULATION, and a secure lock screen. The default CATEGORY_PAYMENT app is optional here because this catalog is not a wallet. No card network or Google Wallet backend is called.",
        onEvaluate = { lab.evaluate() },
    )
}
