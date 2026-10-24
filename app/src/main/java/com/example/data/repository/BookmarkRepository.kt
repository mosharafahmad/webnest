package com.example.data.repository

import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkEntity
import kotlinx.coroutines.flow.Flow

data class MergeImportSummary(
    val importedOrUpdatedCount: Int,
    val duplicatesUpdatedCount: Int
)

class BookmarkRepository(private val bookmarkDao: BookmarkDao) {

    val allBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.observeAllBookmarks()
    val pinnedBookmarks: Flow<List<BookmarkEntity>> = bookmarkDao.observePinnedBookmarks()

    suspend fun getAllBookmarksSnapshot(): List<BookmarkEntity> =
        bookmarkDao.getAllBookmarksSnapshot()

    suspend fun findByUrl(normalizedUrl: String): BookmarkEntity? =
        bookmarkDao.findByUrl(normalizedUrl)

    suspend fun addBookmark(
        name: String,
        normalizedUrl: String,
        faviconUrl: String,
        accentColorIndex: Int,
        isPinned: Boolean
    ): Long {
        val nextOrder = (bookmarkDao.getMaxDisplayOrder() ?: -1) + 1
        val entity = BookmarkEntity(
            name = name.trim(),
            url = normalizedUrl,
            faviconUrl = faviconUrl.trim(),
            accentColorIndex = accentColorIndex.coerceIn(0, 3),
            isPinned = isPinned,
            displayOrder = nextOrder,
            createdAt = System.currentTimeMillis()
        )
        return bookmarkDao.insertBookmark(entity)
    }

    suspend fun updateBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.updateBookmark(bookmark)
    }

    suspend fun togglePin(bookmark: BookmarkEntity): BookmarkEntity {
        val updated = bookmark.copy(isPinned = !bookmark.isPinned)
        bookmarkDao.updateBookmark(updated)
        return updated
    }

    suspend fun deleteBookmark(bookmark: BookmarkEntity) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun deleteAllBookmarks() {
        bookmarkDao.deleteAllBookmarks()
    }

    suspend fun swapBookmarkOrder(first: BookmarkEntity, second: BookmarkEntity) {
        val firstOrder = first.displayOrder
        val secondOrder = second.displayOrder
        val updatedFirst = first.copy(displayOrder = if (firstOrder == secondOrder) secondOrder + 1 else secondOrder)
        val updatedSecond = second.copy(displayOrder = firstOrder)
        bookmarkDao.updateBookmarks(listOf(updatedFirst, updatedSecond))
    }

    suspend fun replaceAllWithImported(imported: List<BookmarkEntity>): Int {
        val normalizedList = imported.mapIndexed { index, item ->
            item.copy(
                id = 0L,
                displayOrder = if (item.displayOrder >= 0) item.displayOrder else index
            )
        }
        bookmarkDao.replaceAllBookmarks(normalizedList)
        return normalizedList.size
    }

    suspend fun mergeImportedBookmarks(imported: List<BookmarkEntity>): MergeImportSummary {
        val existingList = bookmarkDao.getAllBookmarksSnapshot()
        val existingByUrl = existingList.associateBy { it.url.lowercase() }.toMutableMap()
        var maxOrder = (bookmarkDao.getMaxDisplayOrder() ?: -1)
        var addedOrUpdated = 0
        var duplicatesUpdated = 0

        val toInsert = mutableListOf<BookmarkEntity>()
        val toUpdate = mutableListOf<BookmarkEntity>()

        for (item in imported) {
            val key = item.url.lowercase()
            val existing = existingByUrl[key]
            if (existing != null) {
                val merged = existing.copy(
                    name = item.name.ifBlank { existing.name },
                    faviconUrl = item.faviconUrl.ifBlank { existing.faviconUrl },
                    isPinned = existing.isPinned || item.isPinned,
                    accentColorIndex = item.accentColorIndex.coerceIn(0, 3)
                )
                toUpdate.add(merged)
                existingByUrl[key] = merged
                addedOrUpdated++
                duplicatesUpdated++
            } else {
                maxOrder++
                val fresh = item.copy(
                    id = 0L,
                    displayOrder = maxOrder
                )
                toInsert.add(fresh)
                existingByUrl[key] = fresh
                addedOrUpdated++
            }
        }

        if (toUpdate.isNotEmpty()) {
            bookmarkDao.updateBookmarks(toUpdate)
        }
        if (toInsert.isNotEmpty()) {
            bookmarkDao.insertBookmarks(toInsert)
        }
        return MergeImportSummary(
            importedOrUpdatedCount = addedOrUpdated,
            duplicatesUpdatedCount = duplicatesUpdated
        )
    }
}
