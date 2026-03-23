package com.rozetka.domain.model

enum class VoteDirection(val value: Int) {
    UP(1),
    DOWN(-1),
    NONE(0)
}