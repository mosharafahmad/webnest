package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.preferences.BookmarkLayoutMode
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserSettings
import com.example.ui.theme.BookmarkAccentPalettes
import com.example.ui.theme.ChromeRed
import com.example.ui.viewmodel.AddEditFormState
import com.example.ui.viewmodel.PendingImportState
import com.example.util.UrlAndFaviconUtils

@Composable
fun AddEditBookmarkDialog(
    formState: AddEditFormState,
    onNameChange: (String) -> Unit,
    onUrlChange: (String) -> Unit,
    onCustomFaviconChange: (String) -> Unit,
    onAccentColorChange: (Int) -> Unit,
    onPinChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val isEditing = formState.editingBookmark != null
    val previewUrl = UrlAndFaviconUtils.normalizeUrl(formState.url) ?: formState.url
    val previewFavicon = UrlAndFaviconUtils.resolveFaviconUrl(
        previewUrl,
        formState.customFaviconUrl
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("add_edit_bookmark_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp)
            ) {
                // Header with live Favicon Preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FaviconAvatar(
                        name = formState.name.ifBlank { "WebNest" },
                        url = previewUrl,
                        faviconUrl = previewFavicon,
                        accentColorIndex = formState.accentColorIndex,
                        size = 48.dp,
                        cornerRadius = 14.dp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(
                                if (isEditing) R.string.dialog_edit_title else R.string.dialog_add_title
                            ),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (previewUrl.isNotBlank()) {
                            Text(
                                text = previewUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 1. Website Name Input
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.label_website_name)) },
                    placeholder = { Text(stringResource(R.string.hint_website_name)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Language, contentDescription = null)
                    },
                    isError = formState.nameError != null,
                    supportingText = formState.nameError?.let { err ->
                        { Text(text = err, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("website_name_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 2. Website URL Input
                OutlinedTextField(
                    value = formState.url,
                    onValueChange = onUrlChange,
                    label = { Text(stringResource(R.string.label_website_url)) },
                    placeholder = { Text(stringResource(R.string.hint_website_url)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Link, contentDescription = null)
                    },
                    isError = formState.urlError != null,
                    supportingText = formState.urlError?.let { err ->
                        { Text(text = err, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("website_url_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Optional Custom Icon / Favicon URL
                OutlinedTextField(
                    value = formState.customFaviconUrl,
                    onValueChange = onCustomFaviconChange,
                    label = { Text(stringResource(R.string.label_custom_icon_url)) },
                    placeholder = { Text(stringResource(R.string.hint_custom_icon_url)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("website_favicon_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Fallback Badge Color Picker (Chrome 4 colors)
                Text(
                    text = stringResource(R.string.label_icon_style),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BookmarkAccentPalettes.forEachIndexed { index, palette ->
                        val isSelected = formState.accentColorIndex == index
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(palette.primary)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        Color.Transparent
                                    },
                                    shape = CircleShape
                                )
                                .clickable { onAccentColorChange(index) }
                                .testTag("accent_color_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = palette.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Pin to Top Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .clickable { onPinChange(!formState.isPinned) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.label_pin_to_top),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Switch(
                        checked = formState.isPinned,
                        onCheckedChange = onPinChange,
                        modifier = Modifier.testTag("pin_bookmark_switch")
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Cancel and Save Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_bookmark_button")
                    ) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onSave,
                        enabled = !formState.isSaving,
                        modifier = Modifier.testTag("save_bookmark_button")
                    ) {
                        Text(
                            stringResource(
                                if (isEditing) R.string.btn_update else R.string.btn_save
                            )
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkContextMenuSheet(
    bookmark: BookmarkEntity,
    onOpenInBrowser: () -> Unit,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onCopyUrl: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.testTag("bookmark_context_menu_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Bookmark summary header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FaviconAvatar(
                    name = bookmark.name,
                    url = bookmark.url,
                    faviconUrl = bookmark.faviconUrl,
                    accentColorIndex = bookmark.accentColorIndex,
                    size = 44.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bookmark.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = bookmark.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )

            ContextActionItem(
                icon = Icons.Filled.OpenInBrowser,
                label = stringResource(R.string.action_open_chrome),
                testTag = "context_action_open",
                onClick = onOpenInBrowser
            )

            ContextActionItem(
                icon = Icons.Filled.PushPin,
                label = stringResource(
                    if (bookmark.isPinned) R.string.action_unpin else R.string.action_pin
                ),
                testTag = "context_action_pin",
                onClick = onTogglePin
            )

            ContextActionItem(
                icon = Icons.Filled.Edit,
                label = stringResource(R.string.action_edit),
                testTag = "context_action_edit",
                onClick = onEdit
            )

            ContextActionItem(
                icon = Icons.Filled.ContentCopy,
                label = stringResource(R.string.action_copy_url),
                testTag = "context_action_copy",
                onClick = onCopyUrl
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onMoveUp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("context_action_move_up")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_move_up))
                }
                OutlinedButton(
                    onClick = onMoveDown,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("context_action_move_down")
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDownward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_move_down))
                }
            }

            ContextActionItem(
                icon = Icons.Filled.Delete,
                label = stringResource(R.string.action_delete),
                tint = ChromeRed,
                testTag = "context_action_delete",
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ContextActionItem(
    icon: ImageVector,
    label: String,
    testTag: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = tint
            )
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag)
    )
}

@Composable
fun DeleteBookmarkConfirmDialog(
    bookmark: BookmarkEntity,
    onConfirmDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.dialog_delete_title)) },
        text = {
            Text(stringResource(R.string.dialog_delete_message, bookmark.name))
        },
        confirmButton = {
            Button(
                onClick = onConfirmDelete,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(stringResource(R.string.btn_delete))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun ImportBookmarksModeDialog(
    pendingImport: PendingImportState,
    existingCount: Int,
    onMergeSelected: () -> Unit,
    onRequestReplace: () -> Unit,
    onConfirmReplace: () -> Unit,
    onDismiss: () -> Unit
) {
    if (pendingImport.showReplaceConfirmation) {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(stringResource(R.string.dialog_confirm_replace_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.dialog_confirm_replace_msg,
                        existingCount,
                        pendingImport.validBookmarks.size
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = onConfirmReplace,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.testTag("confirm_replace_import_button")
                ) {
                    Text(stringResource(R.string.btn_replace_all))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.dialog_import_mode_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(
                            R.string.dialog_import_mode_subtitle,
                            pendingImport.validBookmarks.size
                        ),
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Option 1: Merge
                    OutlinedCard(
                        onClick = onMergeSelected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_mode_merge_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.import_mode_merge_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.import_mode_merge_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Option 2: Replace
                    OutlinedCard(
                        onClick = onRequestReplace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_mode_replace_card")
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = stringResource(R.string.import_mode_replace_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.import_mode_replace_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun WebNestSettingsDialog(
    settings: UserSettings,
    totalCount: Int,
    onThemeModeChange: (ThemeMode) -> Unit,
    onLayoutModeChange: (BookmarkLayoutMode) -> Unit,
    onPreferChromeChange: (Boolean) -> Unit,
    onRequestClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Theme Mode
                Text(
                    text = stringResource(R.string.settings_theme_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                        label = { Text(stringResource(R.string.settings_theme_system)) }
                    )
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                        label = { Text(stringResource(R.string.settings_theme_light)) }
                    )
                    FilterChip(
                        selected = settings.themeMode == ThemeMode.DARK,
                        onClick = { onThemeModeChange(ThemeMode.DARK) },
                        label = { Text(stringResource(R.string.settings_theme_dark)) }
                    )
                }

                // Layout Mode
                Text(
                    text = stringResource(R.string.settings_layout_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = settings.layoutMode == BookmarkLayoutMode.ADAPTIVE_GRID,
                        onClick = { onLayoutModeChange(BookmarkLayoutMode.ADAPTIVE_GRID) },
                        label = { Text(stringResource(R.string.settings_layout_adaptive_grid)) }
                    )
                    FilterChip(
                        selected = settings.layoutMode == BookmarkLayoutMode.COMPACT_LIST,
                        onClick = { onLayoutModeChange(BookmarkLayoutMode.COMPACT_LIST) },
                        label = { Text(stringResource(R.string.settings_layout_compact_list)) }
                    )
                }

                // Prefer Google Chrome toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_browser_label),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = stringResource(R.string.settings_browser_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = settings.preferChrome,
                        onCheckedChange = onPreferChromeChange
                    )
                }

                if (totalCount > 0) {
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = onRequestClearAll,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_clear_all))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}

@Composable
fun ClearAllBookmarksConfirmDialog(
    onConfirmClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(stringResource(R.string.dialog_clear_all_title)) },
        text = { Text(stringResource(R.string.dialog_clear_all_msg)) },
        confirmButton = {
            Button(
                onClick = onConfirmClearAll,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(stringResource(R.string.btn_delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.btn_cancel))
            }
        }
    )
}

@Composable
fun AboutWebNestDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(
                painter = painterResource(id = R.drawable.ic_webnest_logo),
                contentDescription = stringResource(R.string.cd_app_logo),
                modifier = Modifier.size(56.dp)
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.about_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.about_version),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Text(
                text = stringResource(R.string.about_description),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.btn_close))
            }
        }
    )
}
