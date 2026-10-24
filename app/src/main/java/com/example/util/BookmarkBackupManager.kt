package com.example.util

import com.example.data.local.BookmarkEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

sealed class ImportParseResult {
    data class Success(
        val validBookmarks: List<BookmarkEntity>,
        val skippedInvalidCount: Int,
        val formatVersion: Int
    ) : ImportParseResult()

    data class Error(val reason: String) : ImportParseResult()
}

object BookmarkBackupManager {

    const val EXPORT_FORMAT_VERSION = 1
    const val DEFAULT_BACKUP_FILENAME = "WebNest_Backup.json"

    /**
     * Serializes the bookmark collection into a portable, human-readable JSON string.
     */
    fun serializeToJson(bookmarks: List<BookmarkEntity>): String {
        val root = JSONObject()
        root.put("app", "WebNest")
        root.put("formatVersion", EXPORT_FORMAT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("bookmarkCount", bookmarks.size)

        val bookmarksArray = JSONArray()
        bookmarks.forEachIndexed { index, bookmark ->
            val obj = JSONObject().apply {
                put("name", bookmark.name)
                put("url", bookmark.url)
                put("faviconUrl", bookmark.faviconUrl)
                put("accentColorIndex", bookmark.accentColorIndex)
                put("isPinned", bookmark.isPinned)
                put("displayOrder", bookmark.displayOrder.takeIf { it >= 0 } ?: index)
                put("createdAt", bookmark.createdAt)
            }
            bookmarksArray.put(obj)
        }
        root.put("bookmarks", bookmarksArray)
        return root.toString(2)
    }

    /**
     * Validates and parses a JSON backup string into BookmarkEntity objects.
     * Handles both the standard WebNest root object and raw JSON arrays gracefully.
     */
    fun parseAndValidateJson(rawJson: String): ImportParseResult {
        val trimmed = rawJson.trim()
        if (trimmed.isEmpty()) {
            return ImportParseResult.Error("Selected file is empty.")
        }

        return try {
            val bookmarksArray: JSONArray
            val formatVersion: Int

            if (trimmed.startsWith("{")) {
                val root = JSONObject(trimmed)
                formatVersion = root.optInt("formatVersion", 1)
                if (formatVersion > EXPORT_FORMAT_VERSION + 2) {
                    return ImportParseResult.Error("Unsupported backup format version ($formatVersion).")
                }
                if (!root.has("bookmarks")) {
                    return ImportParseResult.Error("Missing 'bookmarks' list in JSON backup file.")
                }
                bookmarksArray = root.getJSONArray("bookmarks")
            } else if (trimmed.startsWith("[")) {
                formatVersion = 1
                bookmarksArray = JSONArray(trimmed)
            } else {
                return ImportParseResult.Error("File is not a valid JSON bookmark backup.")
            }

            if (bookmarksArray.length() == 0) {
                return ImportParseResult.Error("Backup file contains 0 bookmarks.")
            }

            val parsedBookmarks = mutableListOf<BookmarkEntity>()
            val seenUrls = mutableSetOf<String>()
            var skippedCount = 0

            for (i in 0 until bookmarksArray.length()) {
                val itemObj = bookmarksArray.optJSONObject(i)
                if (itemObj == null) {
                    skippedCount++
                    continue
                }

                val rawUrl = itemObj.optString("url", "").trim()
                val normalizedUrl = UrlAndFaviconUtils.normalizeUrl(rawUrl)
                if (normalizedUrl == null) {
                    skippedCount++
                    continue
                }

                val urlKey = normalizedUrl.lowercase()
                if (seenUrls.contains(urlKey)) {
                    skippedCount++
                    continue
                }
                seenUrls.add(urlKey)

                val rawName = itemObj.optString("name", "").trim()
                val safeName = if (rawName.isNotEmpty()) {
                    rawName.take(120)
                } else {
                    UrlAndFaviconUtils.extractDomain(normalizedUrl)
                }

                val rawFavicon = itemObj.optString("faviconUrl", "").trim()
                val resolvedFavicon = UrlAndFaviconUtils.resolveFaviconUrl(normalizedUrl, rawFavicon)

                val accentIndex = itemObj.optInt(
                    "accentColorIndex",
                    UrlAndFaviconUtils.defaultAccentIndexFor(safeName)
                ).coerceIn(0, 3)

                val isPinned = itemObj.optBoolean("isPinned", false)
                val displayOrder = itemObj.optInt("displayOrder", i).coerceAtLeast(0)
                val createdAt = itemObj.optLong("createdAt", System.currentTimeMillis())

                parsedBookmarks.add(
                    BookmarkEntity(
                        id = 0L,
                        name = safeName,
                        url = normalizedUrl,
                        faviconUrl = resolvedFavicon,
                        accentColorIndex = accentIndex,
                        isPinned = isPinned,
                        displayOrder = displayOrder,
                        createdAt = createdAt
                    )
                )
            }

            if (parsedBookmarks.isEmpty()) {
                return ImportParseResult.Error("No valid website URLs were found in this backup file.")
            }

            // Sort imported items so pinned appear first and displayOrder is preserved
            val sorted = parsedBookmarks.sortedWith(
                compareByDescending<BookmarkEntity> { it.isPinned }
                    .thenBy { it.displayOrder }
                    .thenByDescending { it.createdAt }
            )

            ImportParseResult.Success(
                validBookmarks = sorted,
                skippedInvalidCount = skippedCount,
                formatVersion = formatVersion
            )
        } catch (e: JSONException) {
            ImportParseResult.Error("Corrupted or invalid JSON syntax: ${e.localizedMessage ?: "parse error"}")
        } catch (e: Exception) {
            ImportParseResult.Error("Unable to read backup file: ${e.localizedMessage ?: "unknown error"}")
        }
    }
}
