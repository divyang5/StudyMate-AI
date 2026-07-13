package com.divyang.studymateai.data.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.divyang.studymateai.data.repository.AuthRepository
import com.divyang.studymateai.data.repository.ChapterRepository
import com.divyang.studymateai.gemini.GeminiClient
import com.divyang.studymateai.utils.TextBlocks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TextEditorUiState(
    val isLoadingChapter: Boolean = false,
    val loadFailed: Boolean = false,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val showValidationErrors: Boolean = false,
    val isGeneratingMetadata: Boolean = false
)

/**
 * In edit mode the chapter is loaded here by id — chapter text must never
 * travel through a nav route (newlines break route matching, and routes have
 * size limits).
 *
 * Content is a list of paragraph blocks rather than one string: the editor
 * renders them in a LazyColumn and only the block being edited lives in a
 * TextField. A single TextField holding a 20+ page document re-laid-out the
 * whole string on every keystroke/scroll frame. State is Compose state in the
 * ViewModel so drafts survive the "Scan More" navigation round trip.
 */
@HiltViewModel
class TextEditorViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val chapterRepository: ChapterRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TextEditorUiState())
    val uiState: StateFlow<TextEditorUiState> = _uiState.asStateFlow()

    var title by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    val contentBlocks = mutableStateListOf<String>()

    /** Index of the block currently open in a TextField, or null. */
    var editingBlockIndex by mutableStateOf<Int?>(null)
        private set

    // null = create mode, non-null = edit mode
    private var editingChapterId: String? = null
    private var initialized = false

    val isContentBlank: Boolean
        get() = contentBlocks.all { it.isBlank() }

    /** Idempotent — the screen calls this on every (re)composition entry. */
    fun initFor(chapterId: String?) {
        if (initialized) return
        initialized = true
        editingChapterId = chapterId
        if (chapterId != null) {
            loadChapter(chapterId)
        } else {
            // Create mode: start with one empty block, ready to type into.
            contentBlocks.add("")
            editingBlockIndex = 0
        }
    }

    fun retryLoad() {
        editingChapterId?.let { loadChapter(it) }
    }

    private fun loadChapter(chapterId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingChapter = true, loadFailed = false) }
            try {
                val chapter = chapterRepository.getChapter(chapterId)
                title = chapter.title
                description = chapter.description
                contentBlocks.clear()
                contentBlocks.addAll(TextBlocks.split(chapter.content).ifEmpty { listOf("") })
                _uiState.update { it.copy(isLoadingChapter = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingChapter = false, loadFailed = true) }
            }
        }
    }

    fun onTitleChange(value: String) { title = value }
    fun onDescriptionChange(value: String) { description = value }

    fun startEditingBlock(index: Int) { editingBlockIndex = index }
    fun stopEditingBlock() { editingBlockIndex = null }

    fun onBlockChange(index: Int, value: String) {
        if (index in contentBlocks.indices) contentBlocks[index] = value
    }

    fun addBlock() {
        contentBlocks.add("")
        editingBlockIndex = contentBlocks.lastIndex
    }

    fun removeBlock(index: Int) {
        if (index in contentBlocks.indices) {
            contentBlocks.removeAt(index)
            editingBlockIndex = null
            if (contentBlocks.isEmpty()) contentBlocks.add("")
        }
    }

    fun appendScannedText(text: String) {
        // Replace the single empty starter block instead of leaving it around.
        if (contentBlocks.size == 1 && contentBlocks[0].isBlank()) contentBlocks.clear()
        contentBlocks.addAll(TextBlocks.split(text))
        editingBlockIndex = null
        // Fresh import into an untitled chapter: suggest title/description in
        // the background. Never fires when the user already named it.
        if (title.isBlank() && description.isBlank()) generateMetadata()
    }

    /**
     * Suggests a title/description from the content via Gemini, off the UI
     * path. With [overwrite] false (auto-trigger) only blank fields are
     * filled, so text the user typed meanwhile is never clobbered; the
     * explicit Auto-fill button passes true. Failures are silent — the fields
     * just stay editable.
     */
    fun generateMetadata(overwrite: Boolean = false) {
        if (_uiState.value.isGeneratingMetadata) return
        val content = TextBlocks.join(contentBlocks)
        if (content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isGeneratingMetadata = true) }
            val suggestion = GeminiClient.generateChapterMetadata(content)
            if (suggestion != null) {
                if (overwrite || title.isBlank()) title = suggestion.title
                if (overwrite || description.isBlank()) description = suggestion.description
            }
            _uiState.update { it.copy(isGeneratingMetadata = false) }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearSuccess() = _uiState.update { it.copy(saveSuccess = false) }

    fun save() {
        val content = TextBlocks.join(contentBlocks)
        if (title.isBlank() || description.isBlank() || content.isBlank()) {
            _uiState.update { it.copy(showValidationErrors = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                val userId = authRepository.currentUserId
                    ?: throw Exception("User not logged in")

                val chapterId = editingChapterId
                if (chapterId != null) {
                    chapterRepository.updateChapter(chapterId, title, description, content)
                } else {
                    chapterRepository.createChapter(userId, title, description, content)
                }

                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Failed to save: ${e.message}"
                    )
                }
            }
        }
    }
}
