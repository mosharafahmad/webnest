package com.example.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.SystemClock

sealed class LaunchResult {
    data object LaunchedInChrome : LaunchResult()
    data object LaunchedInFallbackBrowser : LaunchResult()
    data class InvalidUrl(val rawUrl: String) : LaunchResult()
    data object NoBrowserAvailable : LaunchResult()
    data object DebouncedDuplicateTap : LaunchResult()
}

object BrowserLauncher {

    private val CHROME_PACKAGES = listOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary"
    )

    private const val LAUNCH_DEBOUNCE_MS = 650L
    private var lastLaunchTimestampMs: Long = 0L

    /**
     * Launches a website URL directly in Google Chrome when available and preferred,
     * or falls back to the system browser / chooser.
     * Includes tap debouncing to prevent accidental multiple browser launches.
     */
    fun openWebsite(
        context: Context,
        rawUrl: String,
        preferChrome: Boolean = true
    ): LaunchResult {
        val now = SystemClock.elapsedRealtime()
        if (now - lastLaunchTimestampMs < LAUNCH_DEBOUNCE_MS) {
            return LaunchResult.DebouncedDuplicateTap
        }

        val normalizedUrl = UrlAndFaviconUtils.normalizeUrl(rawUrl)
            ?: return LaunchResult.InvalidUrl(rawUrl)

        lastLaunchTimestampMs = now
        val uri = Uri.parse(normalizedUrl)

        if (preferChrome) {
            val chromePackage = findInstalledChromePackage(context.packageManager, uri)
            if (chromePackage != null) {
                try {
                    val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage(chromePackage)
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chromeIntent)
                    return LaunchResult.LaunchedInChrome
                } catch (_: Exception) {
                    // Fall through to standard browser intent if Chrome direct launch fails
                }
            }
        }

        return try {
            val viewIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
            LaunchResult.LaunchedInFallbackBrowser
        } catch (_: ActivityNotFoundException) {
            try {
                val chooser = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, uri),
                    null
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
                LaunchResult.LaunchedInFallbackBrowser
            } catch (_: Exception) {
                LaunchResult.NoBrowserAvailable
            }
        } catch (_: Exception) {
            LaunchResult.NoBrowserAvailable
        }
    }

    private fun findInstalledChromePackage(
        packageManager: PackageManager,
        uri: Uri
    ): String? {
        for (pkg in CHROME_PACKAGES) {
            val testIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage(pkg)
                addCategory(Intent.CATEGORY_BROWSABLE)
            }
            val resolved = packageManager.resolveActivity(
                testIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            if (resolved != null) {
                return pkg
            }
        }
        return null
    }
}
