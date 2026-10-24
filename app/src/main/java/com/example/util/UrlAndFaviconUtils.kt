package com.example.util

import java.net.URI
import kotlin.math.abs

object UrlAndFaviconUtils {

    private val VALID_HOST_REGEX = Regex(
        pattern = "^(([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\\-]*[a-zA-Z0-9])\\.)+([A-Za-z]{2,}|xn--[A-Za-z0-9]+)$|^localhost$|^(\\d{1,3}\\.){3}\\d{1,3}$"
    )

    /**
     * Normalizes a user-entered URL string.
     * Automatically adds https:// when the user enters a domain without a scheme.
     * Returns null if the URL is invalid or unsafe.
     */
    fun normalizeUrl(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty() || trimmed.contains(" ") || trimmed.contains("\n")) {
            return null
        }

        // Reject non-web schemes explicitly
        val lower = trimmed.lowercase()
        if (lower.startsWith("javascript:") ||
            lower.startsWith("data:") ||
            lower.startsWith("file:") ||
            lower.startsWith("mailto:") ||
            lower.startsWith("intent:")
        ) {
            return null
        }

        val withScheme = when {
            lower.startsWith("http://") || lower.startsWith("https://") -> trimmed
            trimmed.contains("://") -> return null
            else -> "https://$trimmed"
        }

        return try {
            val uri = URI(withScheme)
            val scheme = uri.scheme?.lowercase() ?: return null
            if (scheme != "http" && scheme != "https") return null

            val host = uri.host?.lowercase()?.removePrefix(".")?.removeSuffix(".") ?: return null
            if (host.isEmpty() || !VALID_HOST_REGEX.matches(host)) {
                return null
            }

            // Reconstruct normalized URL with lowercase scheme and host
            val portPart = if (uri.port != -1) ":${uri.port}" else ""
            val pathPart = uri.rawPath?.takeIf { it.isNotEmpty() && it != "/" } ?: ""
            val queryPart = uri.rawQuery?.let { "?$it" } ?: ""
            val fragmentPart = uri.rawFragment?.let { "#$it" } ?: ""

            "$scheme://$host$portPart$pathPart$queryPart$fragmentPart"
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extracts a clean display domain (e.g., "github.com") from a URL.
     */
    fun extractDomain(url: String): String {
        return try {
            val normalized = normalizeUrl(url) ?: url
            val uri = URI(normalized)
            val host = uri.host ?: return url
            host.removePrefix("www.")
        } catch (_: Exception) {
            url.removePrefix("https://").removePrefix("http://").removePrefix("www.").substringBefore("/")
        }
    }

    /**
     * Resolves a safe favicon URL. Uses customIconUrl if valid, otherwise builds a domain favicon URL.
     */
    fun resolveFaviconUrl(normalizedWebsiteUrl: String, customIconInput: String = ""): String {
        val trimmedCustom = customIconInput.trim()
        if (trimmedCustom.isNotEmpty()) {
            val normalizedCustom = normalizeUrl(trimmedCustom)
            if (normalizedCustom != null) {
                return normalizedCustom
            }
        }
        val domain = extractDomain(normalizedWebsiteUrl)
        return if (domain.isNotBlank()) {
            "https://www.google.com/s2/favicons?domain=$domain&sz=128"
        } else {
            ""
        }
    }

    /**
     * Computes a deterministic default Chrome-inspired accent index (0..3) from a domain or name.
     */
    fun defaultAccentIndexFor(text: String): Int {
        if (text.isBlank()) return 0
        return abs(text.trim().lowercase().hashCode()) % 4
    }

    /**
     * Extracts a 1-2 character monogram for fallback avatar display when offline or favicon is unavailable.
     */
    fun extractMonogram(name: String, url: String): String {
        val cleanName = name.trim()
        if (cleanName.isNotEmpty()) {
            val firstChar = cleanName.firstOrNull { it.isLetterOrDigit() }
            if (firstChar != null) {
                return firstChar.uppercaseChar().toString()
            }
        }
        val domain = extractDomain(url)
        return domain.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "W"
    }
}
