package com.rozetka.presentation.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSURL
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.UIView

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun VideoPlayer(
    url: String,
    modifier: Modifier,
    isMuted: Boolean,
    autoPlay: Boolean
) {
    val player = remember { AVPlayer() }
    val viewController = remember { AVPlayerViewController() }

    LaunchedEffect(Unit) {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    LaunchedEffect(url) {
        val asset = AVAsset.assetWithURL(NSURL.URLWithString(url)!!)
        val playerItem = AVPlayerItem(asset = asset)
        player.replaceCurrentItemWithPlayerItem(playerItem)
        if (autoPlay) {
            player.play()
        }
    }

    LaunchedEffect(isMuted) {
        player.muted = isMuted
    }

    DisposableEffect(Unit) {
        onDispose {
            player.pause()
        }
    }

    UIKitView(
        factory = {
            viewController.player = player
            viewController.showsPlaybackControls = true
            viewController.videoGravity = AVLayerVideoGravityResizeAspect
            viewController.view
        },
        modifier = modifier,
        update = { _ -> }
    )
}
