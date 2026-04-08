package com.rozetka.reditlite

import androidx.compose.ui.window.ComposeUIViewController
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import platform.UIKit.UIViewController

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
fun MainViewController(
    authCode: String?,
    onLoginClick: () -> Unit
): UIViewController = ComposeUIViewController {
    val windowSizeClass = calculateWindowSizeClass()
    App(
        windowSizeClass = windowSizeClass,
        authCode = authCode,
        onLoginClick = onLoginClick
    )
}