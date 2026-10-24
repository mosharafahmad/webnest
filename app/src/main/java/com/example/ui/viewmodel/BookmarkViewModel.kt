package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.BookmarkEntity
import com.example.data.local.WebNestDatabase
import com.example.data.preferences.BookmarkLayoutMode
import com.example.data.preferences.ThemeMode
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.repository.BookmarkRepository
import com.example.util.BookmarkBackupManager
import com.example.util.ImportParseResult
import com.example.util.UrlAndFaviconUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class BookmarkFilterMode {
    ALL,
    PINNED_ONLY
}

data class AddEditFormState(
    val isVisible: Boolean = false,
    val editingBookmark: BookmarkEntity? = null,
    val name: String = "",
    val url: String = "",
    val customFaviconUrl: String = "",
    val accentColorIndex: Int = 0,
    val isPinned: Boolean = false,
    val nameError: String? = null,
    val urlError: String? = null,
    val isSaving: Boolean = false
)

data class PendingImportState(
    val validBookmarks: List<BookmarkEntity>,
    val skippedCount: Int,
    val showReplaceConfirmation: Boolean = false
)

data class BookmarkUiState(
    val allBookmarks: List<BookmarkEntity> = emptyList(),
    val filteredBookmarks: List<BookmarkEntity> = emptyList(),
    val pinnedCount: Int = 0,
    val totalCount: Int = 0,
    val searchQuery: String = "",
    val filterMode: BookmarkFilterMode = BookmarkFilterMode.ALL,
    val settings: UserSettings = UserSettings(),
    val formState: AddEditFormState = AddEditFormState(),
    val activeContextMenuBookmark: BookmarkEntity? = null,
    val bookmarkPendingDelete: BookmarkEntity? = null,
    val pendingImportState: PendingImportState? = null,
    val showSettingsDialog: Boolean = false,
    val showAboutDialog: Boolean = false,
    val showClearAllConfirmDialog: Boolean = false,
    val isLoading: Boolean = true
)

sealed class UiEvent {
    data class ShowSnackbar(val message: String) : UiEvent()
    data class TriggerExportDocument(val jsonContent: String) : UiEvent()
}

class BookmarkViewModel(
    private val repository: BookmarkRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val searchQueryFlow = MutableStateFlow("")
    private val filterModeFlow = MutableStateFlow(BookmarkFilterMode.ALL)
    private val formStateFlow = MutableStateFlow(AddEditFormState())
    private val contextMenuBookmarkFlow = MutableStateFlow<BookmarkEntity?>(null)
    private val pendingDeleteFlow = MutableStateFlow<BookmarkEntity?>(null)
    private val pendingImportFlow = MutableStateFlow<PendingImportState?>(null)
    private val dialogFlagsFlow = MutableStateFlow(DialogFlags())

    private data class DialogFlags(
        val showSettings: Boolean = false,
        val showAbout: Boolean = false,
        val showClearAllConfirm: Boolean = false
    )

    private val _events = MutableSharedFlow<UiEvent>()
    val events = _events.asSharedFlow()

    val uiState: StateFlow<BookmarkUiState> = combine(
        repository.allBookmarks,
         preferencesRepository.userSettingsFlow,
        searchQueryFlow,
        filterModeFlow,
        combine(
            formStateFlow,
            contextMenuBookmarkFlow,
            pendingDeleteFlow,
            pendingImportFlow,
            dialogFlagsFlow
        ) { form, contextBookmark, pendingDel, pendingImp, flags ->
             OverlayBundle(form, contextBookmark, pendingDel, pendingImp, flags)
        }
    ) { allBookmarks, settings, query, filterMode, overlays ->
        val trimmedQuery = query.trim().lowercase()
        val filtered = allBookmarks.filter { bookmark ->
            val matchesFilter = when (filterMode) {
                BookmarkFilterMode.ALL -> true
                BookmarkFilterMode.PINNED_ONLY -> bookmark.isPinned
            }
            val matchesSearch = if (trimmedQuery.isEmpty()) {
                true
            } else {
                bookmark.name.lowercase().contains(trimmedQuery) ||
                    bookmark.url.lowercase().contains(trimmedQuery)
            }
            matchesFilter && matchesSearch
        }

        BookmarkUiState(
            allBookmarks = allBookmarks,
            filteredBookmarks = filtered,
            pinnedCount = allBookmarks.count { it.isPinned },
            totalCount = allBookmarks.size,
            searchQuery = query,
            filterMode = filterMode,
            settings = settings,
            formState = overlays.form,
            activeContextMenuBookmark = overlays.contextBookmark,
            bookmarkPendingDelete = overlays.pendingDel,
            pendingImportState = overlays.pendingImp,
            showSettingsDialog = overlays.flags.showSettings,
            showAboutDialog = overlays.flags.showAbout,
            showClearAllConfirmDialog = overlays.flags.showClearAllConfirm,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = BookmarkUiState()
    )

    private data class OverlayBundle(
        val form: AddEditFormState,
        val contextBookmark: BookmarkEntity?,
        val pendingDel: BookmarkEntity?,
        val pendingImp: PendingImportState?,
        val flags: DialogFlags
    )

    fun onSearchQueryChange(newQuery: String) {
        searchQueryFlow.value = newQuery
    }

    fun clearSearchQuery() {
        searchQueryFlow.value = ""
    }

    fun setFilterMode(mode: BookmarkFilterMode) {
        filterModeFlow.value = mode
    }

    // Add / Edit Bookmark Form Actions
    fun openAddBookmarkDialog() {
        contextMenuBookmarkFlow.value = null
        formStateFlow.value = AddEditFormState(
            isVisible = true,
            editingBookmark = null,
            name = "",
            url = "",
            customFaviconUrl = "",
            accentColorIndex = 0,
            isPinned = filterModeFlow.value == BookmarkFilterMode.PINNED_ONLY
        )
    }

    fun openEditBookmarkDialog(bookmark: BookmarkEntity) {
        contextMenuBookmarkFlow.value = null
        val autoFavicon = UrlAndFaviconUtils.resolveFaviconUrl(bookmark.url, "")
        val customIcon = if (bookmark.faviconUrl != autoFavicon) bookmark.faviconUrl else ""
        formStateFlow.value = AddEditFormState(
            isVisible = true,
            editingBookmark = bookmark,
            name = bookmark.name,
            url = bookmark.url,
            customFaviconUrl = customIcon,
            accentColorIndex = bookmark.accentColorIndex.coerceIn(0, 3),
            isPinned = bookmark.isPinned
        )
    }

    fun dismissAddEditDialog() {
        formStateFlow.value = AddEditFormState(isVisible = false)
    }

    fun updateFormName(name: String) {
        formStateFlow.update {
            it.copy(
                name = name,
                nameError = null,
                accentColorIndex = if (it.editingBookmark == null && it.name.isEmpty() && name.isNotEmpty()) {
                    UrlAndFaviconUtils.defaultAccentIndexFor(name)
                } else {
                    it.accentColorIndex
                }
            )
        }
    }

    fun updateFormUrl(url: String) {
        formStateFlow.update { it.copy(url = url, urlError = null) }
    }

    fun updateFormCustomFavicon(faviconUrl: String) {
        formStateFlow.update { it.copy(customFaviconUrl = faviconUrl) }
    }

    fun updateFormAccentColor(index: Int) {
        formStateFlow.update { it.copy(accentColorIndex = index.coerceIn(0, 3)) }
    }

    fun updateFormPinned(pinned: Boolean) {
        formStateFlow.update { it.copy(isPinned = pinned) }
    }

    fun saveBookmark(
        emptyNameErrorMsg: String,
        emptyUrlErrorMsg: String,
        invalidUrlErrorMsg: String,
        duplicateUrlErrorMsg: String,
        addedSuccessMsg: String,
        updatedSuccessMsg: String
    ) {
        val current = formStateFlow.value
        if (current.isSaving) return

        val trimmedName = current.name.trim()
        val trimmedUrl = current.url.trim()

        var hasError = false
        var nameErr: String? = null
        var urlErr: String? = null

        if (trimmedName.isEmpty()) {
            nameErr = emptyNameErrorMsg
            hasError = true
        }

        if (trimmedUrl.isEmpty()) {
            urlErr = emptyUrlErrorMsg
            hasError = true
        }

        val normalizedUrl = if (trimmedUrl.isNotEmpty()) {
            UrlAndFaviconUtils.normalizeUrl(trimmedUrl)
        } else {
            null
        }

        if (trimmedUrl.isNotEmpty() && normalizedUrl == null) {
            urlErr = invalidUrlErrorMsg
            hasError = true
        }

        if (hasError || normalizedUrl == null) {
            formStateFlow.update { it.copy(nameError = nameErr, urlError = urlErr) }
            return
        }

        formStateFlow.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            try {
                val existingWithUrl = repository.findByUrl(normalizedUrl)
                val editing = current.editingBookmark

                if (existingWithUrl != null && (editing == null || existingWithUrl.id != editing.id)) {
                    formStateFlow.update {
                        it.copy(isSaving = false, urlError = duplicateUrlErrorMsg)
                    }
                    return@launch
                }

                val resolvedFavicon = UrlAndFaviconUtils.resolveFaviconUrl(
                    normalizedWebsiteUrl = normalizedUrl,
                    customIconInput = current.customFaviconUrl
                )

                if (editing == null) {
                    repository.addBookmark(
                        name = trimmedName,
                        normalizedUrl = normalizedUrl,
                        faviconUrl = resolvedFavicon,
                        accentColorIndex = current.accentColorIndex,
                        isPinned = current.isPinned
                    )
                    formStateFlow.value = AddEditFormState(isVisible = false)
                    _events.emit(UiEvent.ShowSnackbar(addedSuccessMsg))
                } else {
                    val updated = editing.copy(
                        name = trimmedName,
                        url = normalizedUrl,
                        faviconUrl = resolvedFavicon,
                        accentColorIndex = current.accentColorIndex,
                        isPinned = current.isPinned
                    )
                    repository.updateBookmark(updated)
                    formStateFlow.value = AddEditFormState(isVisible = false)
                    _events.emit(UiEvent.ShowSnackbar(updatedSuccessMsg))
                }
            } catch (e: Exception) {
                formStateFlow.update { it.copy(isSaving = false) }
                _events.emit(
                    UiEvent.ShowSnackbar("Database error: ${e.localizedMessage ?: "Failed to save"}")
                )
            }
        }
    }

    // Contextual Menu & Pinning Actions
    fun openContextMenu(bookmark: BookmarkEntity) {
        contextMenuBookmarkFlow.value = bookmark
    }

    fun dismissContextMenu() {
        contextMenuBookmarkFlow.value = null
    }

    fun togglePinStatus(bookmark: BookmarkEntity, pinnedMsg: String, unpinnedMsg: String) {
        contextMenuBookmarkFlow.value = null
        viewModelScope.launch {
            val updated = repository.togglePin(bookmark)
            _events.emit(
                UiEvent.ShowSnackbar(if (updated.isPinned) pinnedMsg else unpinnedMsg)
            )
        }
    }

    fun moveBookmarkRelative(bookmark: BookmarkEntity, moveUp: Boolean) {
        contextMenuBookmarkFlow.value = null
        viewModelScope.launch {
            val currentList = uiState.value.allBookmarks.filter { it.isPinned == bookmark.isPinned }
            val index = currentList.indexOfFirst { it.id == bookmark.id }
            if (index == -1) return@launch
            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex in currentList.indices) {
                repository.swapBookmarkOrder(bookmark, currentList[targetIndex])
            }
        }
    }

    fun requestDeleteBookmark(bookmark: BookmarkEntity) {
        contextMenuBookmarkFlow.value = null
        pendingDeleteFlow.value = bookmark
    }

    fun dismissDeleteDialog() {
        pendingDeleteFlow.value = null
    }

    fun confirmDeleteBookmark(deletedMsg: String) {
        val target = pendingDeleteFlow.value ?: return
        pendingDeleteFlow.value = null
        viewModelScope.launch {
            repository.deleteBookmark(target)
            _events.emit(UiEvent.ShowSnackbar(deletedMsg))
        }
    }

    // Starter Bookmarks Helper
    fun loadStarterBookmarks() {
        viewModelScope.launch {
            val starters = listOf(
                Triple("Google Search", "https://www.google.com", true),
                Triple("GitHub", "https://github.com", true),
                Triple("Wikipedia", "https://www.wikipedia.org", false),
                Triple("Android Developers", "https://developer.android.com", false),
                Triple("YouTube", "https://www.youtube.com", false),
                Triple("Stack Overflow", "https://stackoverflow.com", false)
            )
            starters.forEachIndexed { idx, (title, url, pinned) ->
                if (repository.findByUrl(url) == null) {
                    repository.addBookmark(
                        name = title,
                        normalizedUrl = url,
                        faviconUrl = UrlAndFaviconUtils.resolveFaviconUrl(url),
                        accentColorIndex = idx % 4,
                        isPinned = pinned
                    )
                }
            }
            _events.emit(UiEvent.ShowSnackbar("Added 6 popular websites to your WebNest"))
        }
    }

    // Import & Export Operations
    fun requestExportBookmarks(emptyExportMsg: String) {
        viewModelScope.launch {
            val all = withContext(Dispatchers.IO) {
                repository.getAllBookmarksSnapshot()
            }
            if (all.isEmpty()) {
                _events.emit(UiEvent.ShowSnackbar(emptyExportMsg))
                return@launch
            }
            val json = withContext(Dispatchers.Default) {
                BookmarkBackupManager.serializeToJson(all)
            }
            _events.emit(UiEvent.TriggerExportDocument(json))
        }
    }

    fun onImportFileSelected(rawJson: String) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.Default) {
                BookmarkBackupManager.parseAndValidateJson(rawJson)
            }
            when (result) {
                is ImportParseResult.Success -> {
                    pendingImportFlow.value = PendingImportState(
                        validBookmarks = result.validBookmarks,
                        skippedCount = result.skippedInvalidCount,
                        showReplaceConfirmation = false
                    )
                }
                is ImportParseResult.Error -> {
                    _events.emit(UiEvent.ShowSnackbar("Import failed: ${result.reason}"))
                }
            }
        }
    }

    fun dismissImportDialog() {
        pendingImportFlow.value = null
    }

    fun executeMergeImport() {
        val pending = pendingImportFlow.value ?: return
        pendingImportFlow.value = null
        viewModelScope.launch {
            try {
                val summary = withContext(Dispatchers.IO) {
                    repository.mergeImportedBookmarks(pending.validBookmarks)
                }
                _events.emit(
                    UiEvent.ShowSnackbar(
                        "Merged ${summary.importedOrUpdatedCount} bookmarks (${summary.duplicatesUpdatedCount} updated)"
                    )
                )
            } catch (e: Exception) {
                _events.emit(
                    UiEvent.ShowSnackbar("Failed to merge bookmarks: ${e.localizedMessage ?: "error"}")
                )
            }
        }
    }

    fun promptReplaceConfirmation() {
        val pending = pendingImportFlow.value ?: return
        if (uiState.value.totalCount == 0) {
            executeReplaceImport()
        } else {
            pendingImportFlow.value = pending.copy(showReplaceConfirmation = true)
        }
    }

    fun executeReplaceImport() {
        val pending = pendingImportFlow.value ?: return
        pendingImportFlow.value = null
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    repository.replaceAllWithImported(pending.validBookmarks)
                }
                _events.emit(
                    UiEvent.ShowSnackbar("Replaced collection with $count imported bookmarks")
                )
            } catch (e: Exception) {
                _events.emit(
                    UiEvent.ShowSnackbar("Failed to replace bookmarks: ${e.localizedMessage ?: "error"}")
                )
            }
        }
    }

    fun notifyMessage(message: String) {
        viewModelScope.launch {
            _events.emit(UiEvent.ShowSnackbar(message))
        }
    }

    // Settings & About Dialogs
    fun setShowSettingsDialog(show: Boolean) {
        dialogFlagsFlow.update { it.copy(showSettings = show) }
    }

    fun setShowAboutDialog(show: Boolean) {
        dialogFlagsFlow.update { it.copy(showAbout = show) }
    }

    fun setShowClearAllConfirmDialog(show: Boolean) {
        dialogFlagsFlow.update { it.copy(showClearAllConfirm = show, showSettings = false) }
    }

    fun confirmClearAllBookmarks() {
        dialogFlagsFlow.update { it.copy(showClearAllConfirm = false) }
        viewModelScope.launch {
            repository.deleteAllBookmarks()
            _events.emit(UiEvent.ShowSnackbar("All bookmarks cleared from local storage"))
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setLayoutMode(mode: BookmarkLayoutMode) {
        viewModelScope.launch {
            preferencesRepository.setLayoutMode(mode)
        }
    }

    fun setPreferChrome(prefer: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setPreferChrome(prefer)
        }
    }

    companion object {
        fun provideFactory(context: Context): ViewModelProvider.Factory {
            val appContext = context.applicationContext
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = WebNestDatabase.getInstance(appContext)
                    val repository = BookmarkRepository(database.bookmarkDao())
                    val prefs = UserPreferencesRepository(appContext)
                    return BookmarkViewModel(repository, prefs) as T
                }
            }
        }
    }
}
