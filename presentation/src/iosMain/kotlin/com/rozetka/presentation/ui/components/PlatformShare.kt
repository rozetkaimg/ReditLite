package com.rozetka.presentation.ui.components

import androidx.compose.runtime.Composable
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import androidx.compose.runtime.LaunchedEffect

@Composable
actual fun ShareText(text: String, title: String?) {
    LaunchedEffect(text) {
        val window = UIApplication.sharedApplication.keyWindow
        val rootViewController = window?.rootViewController
        
        val activityViewController = UIActivityViewController(
            activityItems = listOf(text),
            applicationActivities = null
        )
        
        rootViewController?.presentViewController(
            viewControllerToPresent = activityViewController,
            animated = true,
            completion = null
        )
    }
}
