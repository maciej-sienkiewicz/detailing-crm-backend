package com.carslab.crm.production.shared.dto

data class CursorPageRequest(
    val limit: Int = 20,
    val cursor: String? = null
) {
    init {
        require(limit > 0 && limit <= 100) { "Limit must be between 1 and 100" }
    }

    fun getLastId(): Long {
        return cursor?.toLongOrNull() ?: 0L
    }
}

data class CursorPageResponse<T>(
    val data: List<T>,
    val nextCursor: String?,
    val hasMore: Boolean,
    val totalCount: Long? = null
) {
    companion object {
        fun <T> of(
            data: List<T>,
            limit: Int,
            extractId: (T) -> Long,
            totalCount: Long? = null
        ): CursorPageResponse<T> {
            val hasMore = data.size >= limit
            val nextCursor = if (hasMore && data.isNotEmpty()) {
                extractId(data.last()).toString()
            } else null

            return CursorPageResponse(
                data = data,
                nextCursor = nextCursor,
                hasMore = hasMore,
                totalCount = totalCount
            )
        }
    }
}