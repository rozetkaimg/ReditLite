package com.rozetka.presentation.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.AVKit.AVPlayerViewController
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSURL
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.removeFromParentViewController
import platform.UIKit.willMoveToParentViewController

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
    val rootViewController = LocalUIViewController.current

    LaunchedEffect(Unit) {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    LaunchedEffect(url) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null) {
            val asset = AVAsset.assetWithURL(nsUrl)
            val playerItem = AVPlayerItem(asset = asset)
            player.replaceCurrentItemWithPlayerItem(playerItem)
            if (autoPlay) {
                player.play()
            }
        }
    }

    LaunchedEffect(isMuted) {
        player.muted = isMuted
    }

    DisposableEffect(viewController) {
        rootViewController.addChildViewController(viewController)
        viewController.didMoveToParentViewController(rootViewController)
        onDispose {
            player.pause()
            viewController.willMoveToParentViewController(null)
            viewController.removeFromParentViewController()
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
        update = { _ ->
            viewController.player = player
        }
    )
}
