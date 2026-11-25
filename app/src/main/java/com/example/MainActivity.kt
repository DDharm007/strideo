package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.local.StrideoDatabase
import com.example.data.location.LocationTracker
import com.example.data.model.UiDataState
import com.example.data.repository.RoomFitnessRepository
import com.example.ui.NoirStrideApp
import com.example.ui.screens.LocalLoginScreen
import com.example.ui.theme.NoirAccent
import com.example.ui.theme.NoirBackgroundPrimary
import com.example.ui.theme.NoirStrideTheme
import com.example.viewmodel.NoirStrideViewModel
import com.example.widget.StrideoWidgetUpdater

class MainActivity : ComponentActivity() {
    private val pendingWidgetDestination = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        extractAllowedDestination(intent)?.let { pendingWidgetDestination.value = it }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            NoirStrideTheme {
                StrideoRoot(
                    requestedDestination = pendingWidgetDestination.value,
                    onConsumeDestination = { pendingWidgetDestination.value = null }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractAllowedDestination(intent)?.let { pendingWidgetDestination.value = it }
    }

    private fun extractAllowedDestination(intent: Intent?): String? {
        val raw = intent?.getStringExtra(StrideoWidgetUpdater.EXTRA_NAV_DESTINATION) ?: return null
        return when (raw) {
            StrideoWidgetUpdater.DEST_OVERVIEW,
            StrideoWidgetUpdater.DEST_LIVE_TRACKING,
            StrideoWidgetUpdater.DEST_HEALTH -> raw
            else -> null
        }
    }
}

@Composable
private fun StrideoRoot(
    requestedDestination: String?,
    onConsumeDestination: () -> Unit
) {
    val vm: NoirStrideViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val db = StrideoDatabase.getInstance(app)
                NoirStrideViewModel(
                    repository = RoomFitnessRepository(app, db.strideoDao()),
                    locationTracker = LocationTracker(app)
                )
            }
        }
    )

    val profileState by vm.profileState.collectAsStateWithLifecycle()

    when (val state = profileState) {
        is UiDataState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(NoirBackgroundPrimary),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = NoirAccent)
            }
        }

        is UiDataState.Error -> {
            LocalLoginScreen(
                existingProfile = null,
                onResumeExisting = {},
                onCompleteLocalLogin = vm::completeLocalLogin
            )
        }

        is UiDataState.Success -> {
            val userProfile = state.data
            if (userProfile != null && userProfile.signedIn) {
                NoirStrideApp(
                    viewModel = vm,
                    requestedDestination = requestedDestination,
                    onConsumeDestination = onConsumeDestination,
                    onSignOut = vm::signOutLocal
                )
            } else {
                LocalLoginScreen(
                    existingProfile = userProfile,
                    onResumeExisting = vm::resumeExistingLocalProfile,
                    onCompleteLocalLogin = vm::completeLocalLogin
                )
            }
        }
    }
}
