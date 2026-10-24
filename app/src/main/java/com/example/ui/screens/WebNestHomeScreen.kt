package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.local.BookmarkEntity
import com.example.data.preferences.BookmarkLayoutMode
import com.example.ui.components.AboutWebNestDialog
import com.example.ui.components.AddEditBookmarkDialog
import com.example.ui.components.BookmarkContextMenuSheet
import com.example.ui.components.BookmarkGridCard
import com.example.ui.components.BookmarkListTile
import com.example.ui.components.ClearAllBookmarksConfirmDialog
import com.example.ui.components.DeleteBookmarkConfirmDialog
import com.example.ui.components.ImportBookmarksModeDialog
import com.example.ui.components.WebNestDrawerContent
import com.example.ui.components.WebNestSettingsDialog
import com.example.ui.theme.ChromeBlue
import com.example.ui.theme.ChromeGreen
import com.example.ui.theme.ChromeRed
import com.example.ui.theme.ChromeYellow
import com.example.ui.viewmodel.BookmarkFilterMode
import com.example.ui.viewmodel.BookmarkViewModel
import com.example.ui.viewmodel.UiEvent
import com.example.util.BookmarkBackupManager
import com.example.util.BrowserLauncher
import com.example.util.LaunchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebNestHomeScreen(
    viewModel: BookmarkViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Hold pending export JSON string while the user chooses a file destination in SAF
    var pendingExportJson by remember { mutableStateOf<String?>(null) }

    // SAF Launcher for Exporting Bookmarks (CreateDocument)
    val exportDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val jsonToWrite = pendingExportJson
        pendingExportJson = null
        if (uri != null && jsonToWrite != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(jsonToWrite.toByteArray(Charsets.UTF_8))
                            outputStream.flush()
                        } ?: throw IllegalStateException("Unable to open output stream")
                    }
                    viewModel.notifyMessage(
                        context.getString(R.string.msg_export_success, uiState.totalCount)
                    )
                } catch (e: Exception) {
                    viewModel.notifyMessage(
                        context.getString(
                            R.string.msg_export_failed,
                            e.localizedMessage ?: "write error"
                        )
                    )
                }
            }
        }
    }

    // SAF Launcher for Importing Bookmarks (OpenDocument)
    val importDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val rawJson = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)
                            ?.use { it.readText() }
                            ?: throw IllegalStateException("Cannot read selected file")
                    }
                    viewModel.onImportFileSelected(rawJson)
                } catch (e: Exception) {
                    viewModel.notifyMessage(
                        context.getString(
                            R.string.msg_import_failed,
                            e.localizedMessage ?: "read error"
                        )
                    )
                }
            }
        }
    }

    // Collect one-off UI events from ViewModel
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.TriggerExportDocument -> {
                    pendingExportJson = event.jsonContent
                    exportDocumentLauncher.launch(BookmarkBackupManager.DEFAULT_BACKUP_FILENAME)
                }
            }
        }
    }

    // Handle system Back press when drawer is open or filter/search is active
    BackHandler(
        enabled = drawerState.isOpen ||
            uiState.filterMode == BookmarkFilterMode.PINNED_ONLY ||
            uiState.searchQuery.isNotEmpty()
    ) {
        when {
            drawerState.isOpen -> scope.launch { drawerState.close() }
            uiState.searchQuery.isNotEmpty() -> viewModel.clearSearchQuery()
            uiState.filterMode == BookmarkFilterMode.PINNED_ONLY ->
                viewModel.setFilterMode(BookmarkFilterMode.ALL)
        }
    }

    val emptyExportMsg = stringResource(R.string.msg_export_empty)
    val pinnedMsg = stringResource(R.string.msg_bookmark_pinned)
    val unpinnedMsg = stringResource(R.string.msg_bookmark_unpinned)
    val deletedMsg = stringResource(R.string.msg_bookmark_deleted)
    val urlCopiedMsg = stringResource(R.string.msg_url_copied)

    val launchBookmark: (BookmarkEntity) -> Unit = { bookmark ->
        when (
            val result = BrowserLauncher.openWebsite(
                context = context,
                rawUrl = bookmark.url,
                preferChrome = uiState.settings.preferChrome
            )
        ) {
            is LaunchResult.InvalidUrl -> {
                viewModel.notifyMessage(
                    context.getString(R.string.msg_invalid_launch_url, result.rawUrl)
                )
            }
            is LaunchResult.NoBrowserAvailable -> {
                viewModel.notifyMessage(context.getString(R.string.msg_no_browser_found))
            }
            else -> Unit
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            WebNestDrawerContent(
                currentFilterMode = uiState.filterMode,
                totalCount = uiState.totalCount,
                pinnedCount = uiState.pinnedCount,
                onSelectHome = {
                    viewModel.setFilterMode(BookmarkFilterMode.ALL)
                    scope.launch { drawerState.close() }
                },
                onSelectAddWebsite = {
                    scope.launch { drawerState.close() }
                    viewModel.openAddBookmarkDialog()
                },
                onSelectImport = {
                    scope.launch { drawerState.close() }
                    importDocumentLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                },
                onSelectExport = {
                    scope.launch { drawerState.close() }
                    viewModel.requestExportBookmarks(emptyExportMsg)
                },
                onSelectPinnedWebsites = {
                    viewModel.setFilterMode(BookmarkFilterMode.PINNED_ONLY)
                    scope.launch { drawerState.close() }
                },
                onSelectSettings = {
                    scope.launch { drawerState.close() }
                    viewModel.setShowSettingsDialog(true)
                },
                onSelectAbout = {
                    scope.launch { drawerState.close() }
                    viewModel.setShowAboutDialog(true)
                }
            )
        }
    ) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                WebNestTopBar(
                    layoutMode = uiState.settings.layoutMode,
                    onToggleLayoutMode = {
                        val next = if (uiState.settings.layoutMode == BookmarkLayoutMode.ADAPTIVE_GRID) {
                            BookmarkLayoutMode.COMPACT_LIST
                        } else {
                            BookmarkLayoutMode.ADAPTIVE_GRID
                        }
                        viewModel.setLayoutMode(next)
                    },
                    onOpenDrawer = {
                        scope.launch {
                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                        }
                    }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openAddBookmarkDialog() },
                    icon = {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.cd_add_bookmark_fab)
                        )
                    },
                    text = { Text(stringResource(R.string.nav_add_website)) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_website_fab")
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                // Search & Filter Controls Header
                SearchAndFilterBar(
                    searchQuery = uiState.searchQuery,
                    onSearchQueryChange = viewModel::onSearchQueryChange,
                    onClearSearch = viewModel::clearSearchQuery,
                    filterMode = uiState.filterMode,
                    totalCount = uiState.totalCount,
                    pinnedCount = uiState.pinnedCount,
                    onFilterModeChange = viewModel::setFilterMode
                )

                // Main Content Area
                if (uiState.filteredBookmarks.isEmpty() && !uiState.isLoading) {
                    EmptyBookmarksView(
                        hasAnyBookmarks = uiState.totalCount > 0,
                        isPinnedFilter = uiState.filterMode == BookmarkFilterMode.PINNED_ONLY,
                        isSearching = uiState.searchQuery.isNotBlank(),
                        onAddBookmark = viewModel::openAddBookmarkDialog,
                        onLoadStarterPack = viewModel::loadStarterBookmarks,
                        onShowAllBookmarks = {
                            viewModel.clearSearchQuery()
                            viewModel.setFilterMode(BookmarkFilterMode.ALL)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                    )
                } else {
                    val pinnedItems = remember(uiState.filteredBookmarks) {
                        uiState.filteredBookmarks.filter { it.isPinned }
                    }
                    val unpinnedItems = remember(uiState.filteredBookmarks) {
                        uiState.filteredBookmarks.filter { !it.isPinned }
                    }

                    val gridColumns = if (uiState.settings.layoutMode == BookmarkLayoutMode.ADAPTIVE_GRID) {
                        GridCells.Adaptive(minSize = 164.dp)
                    } else {
                        GridCells.Fixed(1)
                    }

                    LazyVerticalGrid(
                        columns = gridColumns,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 6.dp,
                            bottom = 96.dp
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .testTag("bookmarks_grid")
                    ) {
                        // Pinned Websites Section
                        if (pinnedItems.isNotEmpty()) {
                            item(
                                key = "header_pinned",
                                span = { GridItemSpan(maxLineSpan) }
                            ) {
                                SectionHeaderRow(
                                    title = stringResource(R.string.section_pinned),
                                    count = pinnedItems.size,
                                    accentColor = ChromeYellow
                                )
                            }

                            items(
                                items = pinnedItems,
                                key = { "pinned_${it.id}" }
                            ) { bookmark ->
                                if (uiState.settings.layoutMode == BookmarkLayoutMode.ADAPTIVE_GRID) {
                                    BookmarkGridCard(
                                        bookmark = bookmark,
                                        onOpenBookmark = launchBookmark,
                                        onLongPressBookmark = viewModel::openContextMenu,
                                        onTogglePin = {
                                            viewModel.togglePinStatus(it, pinnedMsg, unpinnedMsg)
                                        }
                                    )
                                } else {
                                    BookmarkListTile(
                                        bookmark = bookmark,
                                        onOpenBookmark = launchBookmark,
                                        onLongPressBookmark = viewModel::openContextMenu,
                                        onTogglePin = {
                                            viewModel.togglePinStatus(it, pinnedMsg, unpinnedMsg)
                                        }
                                    )
                                }
                            }
                        }

                        // Saved / Unpinned Websites Section
                        if (unpinnedItems.isNotEmpty()) {
                            if (pinnedItems.isNotEmpty()) {
                                item(
                                    key = "header_all",
                                    span = { GridItemSpan(maxLineSpan) }
                                ) {
                                    SectionHeaderRow(
                                        title = stringResource(R.string.section_all),
                                        count = unpinnedItems.size,
                                        accentColor = ChromeBlue,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }

                            items(
                                items = unpinnedItems,
                                key = { "unpinned_${it.id}" }
                            ) { bookmark ->
                                if (uiState.settings.layoutMode == BookmarkLayoutMode.ADAPTIVE_GRID) {
                                    BookmarkGridCard(
                                        bookmark = bookmark,
                                        onOpenBookmark = launchBookmark,
                                        onLongPressBookmark = viewModel::openContextMenu,
                                        onTogglePin = {
                                            viewModel.togglePinStatus(it, pinnedMsg, unpinnedMsg)
                                        }
                                    )
                                } else {
                                    BookmarkListTile(
                                        bookmark = bookmark,
                                        onOpenBookmark = launchBookmark,
                                        onLongPressBookmark = viewModel::openContextMenu,
                                        onTogglePin = {
                                            viewModel.togglePinStatus(it, pinnedMsg, unpinnedMsg)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Bookmark Dialog
    if (uiState.formState.isVisible) {
        val emptyNameErr = stringResource(R.string.error_empty_name)
        val emptyUrlErr = stringResource(R.string.error_empty_url)
        val invalidUrlErr = stringResource(R.string.error_invalid_url)
        val duplicateUrlErr = stringResource(R.string.error_duplicate_url)
        val addedMsg = stringResource(R.string.msg_bookmark_added)
        val updatedMsg = stringResource(R.string.msg_bookmark_updated)

        AddEditBookmarkDialog(
            formState = uiState.formState,
            onNameChange = viewModel::updateFormName,
            onUrlChange = viewModel::updateFormUrl,
            onCustomFaviconChange = viewModel::updateFormCustomFavicon,
            onAccentColorChange = viewModel::updateFormAccentColor,
            onPinChange = viewModel::updateFormPinned,
            onSave = {
                viewModel.saveBookmark(
                    emptyNameErrorMsg = emptyNameErr,
                    emptyUrlErrorMsg = emptyUrlErr,
                    invalidUrlErrorMsg = invalidUrlErr,
                    duplicateUrlErrorMsg = duplicateUrlErr,
                    addedSuccessMsg = addedMsg,
                    updatedSuccessMsg = updatedMsg
                )
            },
            onDismiss = viewModel::dismissAddEditDialog
        )
    }

    // Long-press Contextual Menu Sheet
    uiState.activeContextMenuBookmark?.let { targetBookmark ->
        BookmarkContextMenuSheet(
            bookmark = targetBookmark,
            onOpenInBrowser = {
                viewModel.dismissContextMenu()
                launchBookmark(targetBookmark)
            },
            onTogglePin = {
                viewModel.togglePinStatus(targetBookmark, pinnedMsg, unpinnedMsg)
            },
            onEdit = {
                viewModel.openEditBookmarkDialog(targetBookmark)
            },
            onCopyUrl = {
                viewModel.dismissContextMenu()
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                clipboard?.setPrimaryClip(ClipData.newPlainText(targetBookmark.name, targetBookmark.url))
                viewModel.notifyMessage(urlCopiedMsg)
            },
            onMoveUp = {
                viewModel.moveBookmarkRelative(targetBookmark, moveUp = true)
            },
            onMoveDown = {
                viewModel.moveBookmarkRelative(targetBookmark, moveUp = false)
            },
            onDelete = {
                viewModel.requestDeleteBookmark(targetBookmark)
            },
            onDismiss = viewModel::dismissContextMenu
        )
    }

    // Delete Confirmation Dialog
    uiState.bookmarkPendingDelete?.let { targetBookmark ->
        DeleteBookmarkConfirmDialog(
            bookmark = targetBookmark,
            onConfirmDelete = { viewModel.confirmDeleteBookmark(deletedMsg) },
            onDismiss = viewModel::dismissDeleteDialog
        )
    }

    // Import Mode & Confirmation Dialog
    uiState.pendingImportState?.let { pendingImport ->
        ImportBookmarksModeDialog(
            pendingImport = pendingImport,
            existingCount = uiState.totalCount,
            onMergeSelected = viewModel::executeMergeImport,
            onRequestReplace = viewModel::promptReplaceConfirmation,
            onConfirmReplace = viewModel::executeReplaceImport,
            onDismiss = viewModel::dismissImportDialog
        )
    }

    // Settings Dialog
    if (uiState.showSettingsDialog) {
        WebNestSettingsDialog(
            settings = uiState.settings,
            totalCount = uiState.totalCount,
            onThemeModeChange = viewModel::setThemeMode,
            onLayoutModeChange = viewModel::setLayoutMode,
            onPreferChromeChange = viewModel::setPreferChrome,
            onRequestClearAll = { viewModel.setShowClearAllConfirmDialog(true) },
            onDismiss = { viewModel.setShowSettingsDialog(false) }
        )
    }

    // Clear All Confirmation Dialog
    if (uiState.showClearAllConfirmDialog) {
        ClearAllBookmarksConfirmDialog(
            onConfirmClearAll = viewModel::confirmClearAllBookmarks,
            onDismiss = { viewModel.setShowClearAllConfirmDialog(false) }
        )
    }

    // About WebNest Dialog
    if (uiState.showAboutDialog) {
        AboutWebNestDialog(
            onDismiss = { viewModel.setShowAboutDialog(false) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WebNestTopBar(
    layoutMode: BookmarkLayoutMode,
    onToggleLayoutMode: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Column {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_webnest_logo),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.app_subtitle),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    // Toggle Grid / List layout icon button
                    IconButton(
                        onClick = onToggleLayoutMode,
                        modifier = Modifier.testTag("toggle_layout_button")
                    ) {
                        Icon(
                            imageVector = if (layoutMode == BookmarkLayoutMode.ADAPTIVE_GRID) {
                                Icons.Filled.ViewAgenda
                            } else {
                                Icons.Filled.GridView
                            },
                            contentDescription = stringResource(R.string.settings_layout_label),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Navigation Drawer Menu Icon on the right as requested
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("open_drawer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = stringResource(R.string.cd_open_drawer),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Chrome-inspired 4-color bottom border line
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
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
        }
    }
}

@Composable
private fun SearchAndFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onClearSearch: () -> Unit,
    filterMode: BookmarkFilterMode,
    totalCount: Int,
    pinnedCount: Int,
    onFilterModeChange: (BookmarkFilterMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Chrome-inspired Omnibox Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    text = stringResource(R.string.search_placeholder),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                AnimatedVisibility(
                    visible = searchQuery.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    IconButton(
                        onClick = onClearSearch,
                        modifier = Modifier.testTag("clear_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Clear,
                            contentDescription = stringResource(R.string.cd_clear_search)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(50),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_bookmarks_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips Row (All Bookmarks vs Pinned Websites)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            FilterChip(
                selected = filterMode == BookmarkFilterMode.ALL,
                onClick = { onFilterModeChange(BookmarkFilterMode.ALL) },
                label = {
                    Text("${stringResource(R.string.filter_all)} ($totalCount)")
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("filter_chip_all")
            )

            FilterChip(
                selected = filterMode == BookmarkFilterMode.PINNED_ONLY,
                onClick = { onFilterModeChange(BookmarkFilterMode.PINNED_ONLY) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.PushPin,
                        contentDescription = null,
                        tint = ChromeYellow,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = {
                    Text("${stringResource(R.string.filter_pinned)} ($pinnedCount)")
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                modifier = Modifier.testTag("filter_chip_pinned")
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    count: Int,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .background(accentColor, RoundedCornerShape(50))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.width(6.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(50)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun EmptyBookmarksView(
    hasAnyBookmarks: Boolean,
    isPinnedFilter: Boolean,
    isSearching: Boolean,
    onAddBookmark: () -> Unit,
    onLoadStarterPack: () -> Unit,
    onShowAllBookmarks: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 420.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_empty_bookmarks),
                contentDescription = null,
                modifier = Modifier.size(168.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            val titleText = when {
                isSearching -> stringResource(R.string.empty_search_title)
                isPinnedFilter -> stringResource(R.string.empty_pinned_title)
                else -> stringResource(R.string.empty_bookmarks_title)
            }

            val subtitleText = when {
                isSearching -> stringResource(R.string.empty_search_subtitle)
                isPinnedFilter -> stringResource(R.string.empty_pinned_subtitle)
                else -> stringResource(R.string.empty_bookmarks_subtitle)
            }

            Text(
                text = titleText,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            if (!hasAnyBookmarks) {
                Button(
                    onClick = onAddBookmark,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .testTag("empty_state_add_button")
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.nav_add_website))
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onLoadStarterPack,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .testTag("empty_state_starter_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = stringResource(R.string.cd_load_sample_bookmarks),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_load_starter_pack))
                }
            } else {
                OutlinedButton(
                    onClick = onShowAllBookmarks,
                    modifier = Modifier.testTag("empty_state_reset_filter_button")
                ) {
                    Text(stringResource(R.string.filter_all))
                }
            }
        }
    }
}
