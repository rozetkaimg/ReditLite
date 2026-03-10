package com.rozetka.reditlite

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform