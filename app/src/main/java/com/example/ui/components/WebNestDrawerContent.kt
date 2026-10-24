package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.ChromeBlue
import com.example.ui.theme.ChromeGreen
import com.example.ui.theme.ChromeRed
import com.example.ui.theme.ChromeYellow
import com.example.ui.viewmodel.BookmarkFilterMode

@Composable
fun WebNestDrawerContent(
    currentFilterMode: BookmarkFilterMode,
    totalCount: Int,
    pinnedCount: Int,
    onSelectHome: () -> Unit,
    onSelectAddWebsite: () -> Unit,
    onSelectImport: () -> Unit,
    onSelectExport: () -> Unit,
    onSelectPinnedWebsites: () -> Unit,
    onSelectSettings: () -> Unit,
    onSelectAbout: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalDrawerSheet(
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxHeight()
            .testTag("navigation_drawer_sheet"),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            // Polished Drawer Header
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_webnest_logo),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            modifier = Modifier.size(52.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Chrome-inspired 4-color accent bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(50))
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(ChromeBlue)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(ChromeRed)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(ChromeYellow)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(ChromeGreen)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "$totalCount Saved",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$pinnedCount Pinned",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val itemPadding = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)

            // 1. Home
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_home)) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Home,
                        contentDescription = null
                    )
                },
                badge = {
                    if (totalCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ) {
                            Text(
                                text = totalCount.toString(),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                },
                selected = currentFilterMode == BookmarkFilterMode.ALL,
                onClick = onSelectHome,
                modifier = itemPadding.testTag("drawer_item_home"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            // 2. Pinned Websites
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_pinned_websites)) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = null,
                        tint = ChromeYellow
                    )
                },
                badge = {
                    if (pinnedCount > 0) {
                        Badge(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        ) {
                            Text(
                                text = pinnedCount.toString(),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                },
                selected = currentFilterMode == BookmarkFilterMode.PINNED_ONLY,
                onClick = onSelectPinnedWebsites,
                modifier = itemPadding.testTag("drawer_item_pinned"),
                colors = NavigationDrawerItemDefaults.colors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )

            // 3. Add Website
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_add_website)) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = null,
                        tint = ChromeBlue
                    )
                },
                selected = false,
                onClick = onSelectAddWebsite,
                modifier = itemPadding.testTag("drawer_item_add")
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )

            // 4. Import Bookmarks
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_import_bookmarks)) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.FileDownload,
                        contentDescription = null,
                        tint = ChromeGreen
                    )
                },
                selected = false,
                onClick = onSelectImport,
                modifier = itemPadding.testTag("drawer_item_import")
            )

            // 5. Export Bookmarks
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_export_bookmarks)) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.FileUpload,
                        contentDescription = null,
                        tint = ChromeBlue
                    )
                },
                selected = false,
                onClick = onSelectExport,
                modifier = itemPadding.testTag("drawer_item_export")
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )

            // 6. Settings
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_settings)) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = null
                    )
                },
                selected = false,
                onClick = onSelectSettings,
                modifier = itemPadding.testTag("drawer_item_settings")
            )

            // 7. About WebNest
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.nav_about)) },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null
                    )
                },
                selected = false,
                onClick = onSelectAbout,
                modifier = itemPadding.testTag("drawer_item_about")
            )
        }
    }
}
