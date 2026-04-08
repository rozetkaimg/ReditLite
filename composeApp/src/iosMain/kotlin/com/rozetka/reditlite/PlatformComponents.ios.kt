package com.rozetka.reditlite

import coil3.ComponentRegistry

actual fun ComponentRegistry.Builder.addPlatformComponents() {
    // GIF support not yet available on iOS in Coil 3
}
