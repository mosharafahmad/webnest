package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.ui.theme.BookmarkAccentPalettes
import com.example.util.UrlAndFaviconUtils

@Composable
fun FaviconAvatar(
    name: String,
    url: String,
    faviconUrl: String,
    accentColorIndex: Int,
    modifier: Modifier = Modifier,
    size: Dp = 46.dp,
    cornerRadius: Dp = 14.dp
) {
    val isDark = isSystemInDarkTheme()
    val palette = BookmarkAccentPalettes[accentColorIndex.coerceIn(0, BookmarkAccentPalettes.lastIndex)]
    val containerColor = if (isDark) palette.containerDark else palette.containerLight
    val accentColor = palette.primary
    val monogram = UrlAndFaviconUtils.extractMonogram(name, url)
    val shape = RoundedCornerShape(cornerRadius)

    val effectiveFaviconUrl = faviconUrl.ifBlank {
        UrlAndFaviconUtils.resolveFaviconUrl(url)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
            .border(
                width = 1.dp,
                color = accentColor.copy(alpha = 0.28f),
                shape = shape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (effectiveFaviconUrl.isNotBlank()) {
            val context = LocalContext.current
            val imageRequest = ImageRequest.Builder(context)
                .data(effectiveFaviconUrl)
                .crossfade(true)
                .diskCachePolicy(CachePolicy.ENABLED)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .build()

            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = "$name icon",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                loading = {
                    MonogramBadgeText(
                        monogram = monogram,
                        color = accentColor,
                        size = size
                    )
                },
                error = {
                    MonogramBadgeText(
                        monogram = monogram,
                        color = accentColor,
                        size = size
                    )
                }
            )
        } else {
            MonogramBadgeText(
                monogram = monogram,
                color = accentColor,
                size = size
            )
        }
    }
}

@Composable
private fun MonogramBadgeText(
    monogram: String,
    color: androidx.compose.ui.graphics.Color,
    size: Dp
) {
    Text(
        text = monogram,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = if (size >= 44.dp) 18.sp else 15.sp
        ),
        color = color
    )
}
