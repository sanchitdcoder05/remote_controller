package com.san.productivity.tvremotewifibluetooth

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.san.productivity.tvremotewifibluetooth.data.model.RemoteCommand
import com.san.productivity.tvremotewifibluetooth.data.repository.SamsungTvRepository
import com.san.productivity.tvremotewifibluetooth.ui.navigation.NavRoutes
import com.san.productivity.tvremotewifibluetooth.ui.screens.BluetoothScreen
import com.san.productivity.tvremotewifibluetooth.ui.screens.DeviceDiscoveryScreen
import com.san.productivity.tvremotewifibluetooth.ui.screens.HomeScreen
import com.san.productivity.tvremotewifibluetooth.ui.screens.RemoteScreen
import com.san.productivity.tvremotewifibluetooth.ui.screens.SettingsScreen
import com.san.productivity.tvremotewifibluetooth.ui.screens.TroubleshootingScreen
import com.san.productivity.tvremotewifibluetooth.ui.theme.TVRemoteWifiBluetoothTheme
import com.san.productivity.tvremotewifibluetooth.viewmodel.BluetoothViewModel
import com.san.productivity.tvremotewifibluetooth.viewmodel.DeviceDiscoveryViewModel
import com.san.productivity.tvremotewifibluetooth.viewmodel.HomeViewModel
import com.san.productivity.tvremotewifibluetooth.viewmodel.RemoteViewModel
import com.san.productivity.tvremotewifibluetooth.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var appRepository: SamsungTvRepository? = null

    private val requestBluetoothPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permission result handled automatically in ViewModels
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SamsungRemoteApplication
        appRepository = app.repository
        val repository = app.repository

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repository))
            val themeMode by settingsViewModel.themeMode.collectAsState()

            val darkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemInDarkTheme()
            }

            TVRemoteWifiBluetoothTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TvRemoteApp(
                        repository = repository,
                        onRequestBluetoothPermissions = { requestBluetoothPermissions() }
                    )
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val repo = appRepository
        if (repo != null) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP -> {
                    lifecycleScope.launch { repo.sendCommand(RemoteCommand.VOLUME_UP) }
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    lifecycleScope.launch { repo.sendCommand(RemoteCommand.VOLUME_DOWN) }
                    return true
                }
                KeyEvent.KEYCODE_VOLUME_MUTE -> {
                    lifecycleScope.launch { repo.sendCommand(RemoteCommand.MUTE) }
                    return true
                }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
            requestBluetoothPermissionLauncher.launch(permissions)
        } else {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            requestBluetoothPermissionLauncher.launch(permissions)
        }
    }
}

@Composable
fun TvRemoteApp(
    repository: com.san.productivity.tvremotewifibluetooth.data.repository.SamsungTvRepository,
    onRequestBluetoothPermissions: () -> Unit
) {
    val navController = rememberNavController()

    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
    val remoteViewModel: RemoteViewModel = viewModel(
        factory = RemoteViewModel.Factory(
            context = androidx.compose.ui.platform.LocalContext.current.applicationContext,
            repository = repository
        )
    )
    val discoveryViewModel: DeviceDiscoveryViewModel = viewModel(factory = DeviceDiscoveryViewModel.Factory(repository))
    val bluetoothViewModel: BluetoothViewModel = viewModel(factory = BluetoothViewModel.Factory(repository))
    val settingsViewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory(repository))

    NavHost(
        navController = navController,
        startDestination = NavRoutes.Remote.route
    ) {
        composable(NavRoutes.Remote.route) {
            RemoteScreen(
                viewModel = remoteViewModel,
                onNavigateToDiscovery = { navController.navigate(NavRoutes.Discovery.route) },
                onNavigateToSettings = { navController.navigate(NavRoutes.Settings.route) }
            )
        }

        composable(NavRoutes.Discovery.route) {
            DeviceDiscoveryScreen(
                viewModel = discoveryViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.Bluetooth.route) {
            BluetoothScreen(
                viewModel = bluetoothViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToRemote = { navController.navigate(NavRoutes.Remote.route) },
                onRequestPermissions = onRequestBluetoothPermissions
            )
        }

        composable(NavRoutes.Settings.route) {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToTroubleshooting = { navController.navigate(NavRoutes.Troubleshooting.route) }
            )
        }

        composable(NavRoutes.Troubleshooting.route) {
            TroubleshootingScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
