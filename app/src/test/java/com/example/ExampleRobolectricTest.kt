package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BookmarkEntity
import com.example.data.local.WebNestDatabase
import com.example.data.repository.BookmarkRepository
import com.example.util.BookmarkBackupManager
import com.example.util.ImportParseResult
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var database: WebNestDatabase
    private lateinit var repository: BookmarkRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, WebNestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = BookmarkRepository(database.bookmarkDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("WebNest", appName)
    }

    @Test
    fun `pinned bookmarks appear before unpinned bookmarks in Room DAO`() = runBlocking {
        repository.addBookmark(
            name = "Wikipedia",
            normalizedUrl = "https://wikipedia.org",
            faviconUrl = "",
            accentColorIndex = 0,
            isPinned = false
        )
        repository.addBookmark(
            name = "Google",
            normalizedUrl = "https://google.com",
            faviconUrl = "",
            accentColorIndex = 1,
            isPinned = true
        )

        val all = repository.getAllBookmarksSnapshot()
        assertEquals(2, all.size)
        assertEquals("Google", all[0].name)
        assertTrue(all[0].isPinned)
        assertEquals("Wikipedia", all[1].name)
    }

    @Test
    fun `backup serialization and parsing roundTrip preserves data`() {
        val sample = listOf(
            BookmarkEntity(
                id = 1L,
                name = "GitHub",
                url = "https://github.com",
                faviconUrl = "https://www.google.com/s2/favicons?domain=github.com&sz=128",
                accentColorIndex = 0,
                isPinned = true,
                displayOrder = 0
            ),
            BookmarkEntity(
                id = 2L,
                name = "Wikipedia",
                url = "https://wikipedia.org",
                faviconUrl = "https://www.google.com/s2/favicons?domain=wikipedia.org&sz=128",
                accentColorIndex = 2,
                isPinned = false,
                displayOrder = 1
            )
        )

        val json = BookmarkBackupManager.serializeToJson(sample)
        val parsed = BookmarkBackupManager.parseAndValidateJson(json)
        assertTrue(parsed is ImportParseResult.Success)
        val success = parsed as ImportParseResult.Success
        assertEquals(2, success.validBookmarks.size)
        assertEquals("GitHub", success.validBookmarks[0].name)
        assertTrue(success.validBookmarks[0].isPinned)
    }

    @Test
    fun `parseAndValidateJson handles corrupted JSON safely`() {
        val result = BookmarkBackupManager.parseAndValidateJson("{invalid_json")
        assertTrue(result is ImportParseResult.Error)
        assertNotNull((result as ImportParseResult.Error).reason)
    }

    @Test
    fun `merge and replace import work accurately`() = runBlocking {
        repository.addBookmark(
            name = "Existing Site",
            normalizedUrl = "https://example.com",
            faviconUrl = "",
            accentColorIndex = 0,
            isPinned = false
        )

        val importedBatch = listOf(
            BookmarkEntity(
                name = "Updated Example",
                url = "https://example.com",
                isPinned = true,
                displayOrder = 0
            ),
            BookmarkEntity(
                name = "GitHub",
                url = "https://github.com",
                isPinned = false,
                displayOrder = 1
            )
        )

        val mergeSummary = repository.mergeImportedBookmarks(importedBatch)
        assertEquals(2, mergeSummary.importedOrUpdatedCount)
        assertEquals(1, mergeSummary.duplicatesUpdatedCount)

        val afterMerge = repository.getAllBookmarksSnapshot()
        assertEquals(2, afterMerge.size)
        assertTrue(afterMerge.first { it.url == "https://example.com" }.isPinned)

        val replaceBatch = listOf(
            BookmarkEntity(
                name = "Only Android",
                url = "https://developer.android.com",
                isPinned = true,
                displayOrder = 0
            )
        )
        repository.replaceAllWithImported(replaceBatch)
        val afterReplace = repository.getAllBookmarksSnapshot()
        assertEquals(1, afterReplace.size)
        assertEquals("Only Android", afterReplace[0].name)
    }
}
