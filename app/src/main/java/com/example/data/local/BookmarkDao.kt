package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query(
        """
        SELECT * FROM bookmarks 
        ORDER BY is_pinned DESC, display_order ASC, created_at DESC
        """
    )
    fun observeAllBookmarks(): Flow<List<BookmarkEntity>>

    @Query(
        """
        SELECT * FROM bookmarks 
        WHERE is_pinned = 1 
        ORDER BY display_order ASC, created_at DESC
        """
    )
    fun observePinnedBookmarks(): Flow<List<BookmarkEntity>>

    @Query(
        """
        SELECT * FROM bookmarks 
        ORDER BY is_pinned DESC, display_order ASC, created_at DESC
        """
    )
    suspend fun getAllBookmarksSnapshot(): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE LOWER(url) = LOWER(:normalizedUrl) LIMIT 1")
    suspend fun findByUrl(normalizedUrl: String): BookmarkEntity?

    @Query("SELECT MAX(display_order) FROM bookmarks")
    suspend fun getMaxDisplayOrder(): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmarks(bookmarks: List<BookmarkEntity>)

    @Update
    suspend fun updateBookmark(bookmark: BookmarkEntity)

    @Update
    suspend fun updateBookmarks(bookmarks: List<BookmarkEntity>)

    @Delete
    suspend fun deleteBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Long)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()

    @Transaction
    suspend fun replaceAllBookmarks(newBookmarks: List<BookmarkEntity>) {
        deleteAllBookmarks()
        if (newBookmarks.isNotEmpty()) {
            insertBookmarks(newBookmarks)
        }
    }
}
