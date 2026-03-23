package com.rozetka.domain.model

data class PaginatedData<T>(
    val items: List<T>,
    val after: String?,
    val before: String?
)