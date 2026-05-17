package com.android.rut.miit.productinventory.ui.notification

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.android.rut.miit.productinventory.core.push.DeviceTokenRegistrar
import com.android.rut.miit.productinventory.core.push.isPostNotificationsPermissionGranted
import com.android.rut.miit.productinventory.core.push.showUnreadProductInventoryNotifications
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationSettingsUseCase
import com.android.rut.miit.productinventory.feature.notifications.api.GetNotificationsUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.time.Duration.Companion.seconds
import org.koin.compose.koinInject

@Composable
internal fun AndroidNotificationBootstrap(
    isAuthenticated: Boolean,
    getNotificationsUseCase: GetNotificationsUseCase = koinInject(),
    getNotificationSettingsUseCase: GetNotificationSettingsUseCase = koinInject(),
    deviceTokenRegistrar: DeviceTokenRegistrar = koinInject(),
    syncInterval: kotlin.time.Duration = NOTIFICATION_SYNC_INTERVAL
) {
    val context = LocalContext.current.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    var isPostNotificationsGranted by remember {
        mutableStateOf(context.isPostNotificationsPermissionGranted())
    }
    var isResumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    var permissionRequestedThisSession by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isPostNotificationsGranted = granted || context.isPostNotificationsPermissionGranted()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, _ ->
            isResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(isAuthenticated) {
        if (!isAuthenticated) return@LaunchedEffect
        runCatching { deviceTokenRegistrar.registerCurrentToken() }
    }

    LaunchedEffect(isAuthenticated, isPostNotificationsGranted) {
        if (!isAuthenticated || isPostNotificationsGranted || permissionRequestedThisSession) {
            return@LaunchedEffect
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect

        val pushEnabled = runCatching { getNotificationSettingsUseCase().pushEnabled }
            .getOrDefault(false)
        if (pushEnabled) {
            permissionRequestedThisSession = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(isAuthenticated, isPostNotificationsGranted, isResumed) {
        if (!isAuthenticated || !isResumed) return@LaunchedEffect

        while (isActive) {
            val pushEnabled = runCatching { getNotificationSettingsUseCase().pushEnabled }
                .getOrDefault(false)
            if (pushEnabled && context.isPostNotificationsPermissionGranted()) {
                val notifications = runCatching { getNotificationsUseCase() }
                    .getOrDefault(emptyList())
                context.showUnreadProductInventoryNotifications(notifications)
            }
            delay(syncInterval)
        }
    }
}

private val NOTIFICATION_SYNC_INTERVAL = 15.seconds
