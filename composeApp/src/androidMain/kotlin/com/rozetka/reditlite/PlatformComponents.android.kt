package com.rozetka.reditlite

import coil3.ComponentRegistry
import coil3.gif.GifDecoder

actual fun ComponentRegistry.Builder.addPlatformComponents() {
    add(GifDecoder.Factory())
}
