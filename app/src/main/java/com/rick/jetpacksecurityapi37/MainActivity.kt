package com.rick.jetpacksecurityapi37

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.rick.jetpacksecurityapi37.ui.CatalogScreen
import com.rick.jetpacksecurityapi37.ui.LabDestination
import com.rick.jetpacksecurityapi37.ui.labs.AuthenticatorLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.BackupLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.BankingLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.BiometricLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.ContactlessPaymentLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.CredentialManagerLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.EncryptedFilesLab
import com.rick.jetpacksecurityapi37.ui.labs.EncryptedPreferencesLab
import com.rick.jetpacksecurityapi37.ui.labs.IdentityLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.InstallSourceLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.KeystoreLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.MigrationLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.OverlayTapjackingLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.ScreenCaptureLabScreen
import com.rick.jetpacksecurityapi37.ui.labs.SecurityStateLabScreen
import com.rick.jetpacksecurityapi37.ui.theme.JetpackSecurityAPI37Theme

class MainActivity : FragmentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JetpackSecurityAPI37Theme {
                val navController = rememberNavController()
                val backStack by navController.currentBackStackEntryAsState()
                val route = backStack?.destination?.route
                val lab = LabDestination.entries.firstOrNull { it.route == route }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(lab?.title ?: "Jetpack Security") },
                            navigationIcon = {
                                if (lab != null) {
                                    TextButton(onClick = { navController.popBackStack() }) {
                                        Text("Back")
                                    }
                                }
                            },
                        )
                    },
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = LabDestination.CATALOG_ROUTE,
                        modifier = Modifier.padding(innerPadding),
                    ) {
                        composable(LabDestination.CATALOG_ROUTE) {
                            CatalogScreen { dest -> navController.navigate(dest.route) }
                        }
                        composable(LabDestination.STORAGE.route) { EncryptedPreferencesLab() }
                        composable(LabDestination.FILES.route) { EncryptedFilesLab() }
                        composable(LabDestination.MIGRATION.route) { MigrationLabScreen() }
                        composable(LabDestination.KEYSTORE.route) { KeystoreLabScreen() }
                        composable(LabDestination.AUTHENTICATOR.route) { AuthenticatorLabScreen() }
                        composable(LabDestination.IDENTITY.route) { IdentityLabScreen() }
                        composable(LabDestination.STATE.route) { SecurityStateLabScreen() }
                        composable(LabDestination.BIOMETRIC.route) { BiometricLabScreen() }
                        composable(LabDestination.CREDENTIALS.route) { CredentialManagerLabScreen() }
                        composable(LabDestination.BANKING.route) { BankingLabScreen() }
                        composable(LabDestination.SCREEN_CAPTURE.route) { ScreenCaptureLabScreen() }
                        composable(LabDestination.OVERLAY.route) { OverlayTapjackingLabScreen() }
                        composable(LabDestination.INSTALL_SOURCE.route) { InstallSourceLabScreen() }
                        composable(LabDestination.PAYMENT.route) { ContactlessPaymentLabScreen() }
                        composable(LabDestination.BACKUP.route) { BackupLabScreen() }
                    }
                }
            }
        }
    }
}
