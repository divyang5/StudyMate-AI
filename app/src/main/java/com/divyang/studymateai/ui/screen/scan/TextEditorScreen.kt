package com.divyang.studymateai.ui.screen.scan


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import com.divyang.studymateai.ads.findActivity
import com.divyang.studymateai.ads.rememberAdManager
import com.divyang.studymateai.data.viewmodel.TextEditorViewModel
import com.divyang.studymateai.navigation.Routes
import com.divyang.studymateai.ui.components.AppColors
import com.divyang.studymateai.ui.components.AppTopBar
import com.divyang.studymateai.ui.components.verticalScrollbar

/**
 * Block-based editor: content is a LazyColumn of paragraph cards and only the
 * tapped block lives in a TextField. One TextField holding a whole 20+ page
 * document re-laid-out the entire string per keystroke/scroll frame, which
 * froze this screen for large chapters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(
    chapterId: String? = null,
    navController: NavController,
    savedStateHandle: SavedStateHandle? = null,
    viewModel: TextEditorViewModel = hiltViewModel()
) {
    val adManager = rememberAdManager()
    val activity = LocalContext.current.let { ctx -> remember(ctx) { ctx.findActivity() } }

    LaunchedEffect(Unit) {
        viewModel.initFor(chapterId)
        adManager.loadInterstitialAd()   // shown after a successful save
    }

    // "Scan More" (and the import flow) hand extracted text back here as a
    // result — chapter text must never travel through nav routes.
    LaunchedEffect(savedStateHandle) {
        savedStateHandle?.getStateFlow(Routes.KEY_SCANNED_TEXT, "")?.collect { scanned ->
            if (scanned.isNotBlank()) {
                viewModel.appendScannedText(scanned)
                savedStateHandle[Routes.KEY_SCANNED_TEXT] = ""
            }
        }
    }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSuccess()
            // Signal earlier entries to refetch instead of re-navigating to a
            // fresh Home (which recreated HomeViewModel and reloaded everything).
            runCatching {
                navController.getBackStackEntry(Routes.Home.route)
                    .savedStateHandle[Routes.KEY_CHAPTERS_CHANGED] = true
            }
            navController.previousBackStackEntry
                ?.savedStateHandle?.set(Routes.KEY_CHAPTER_CHANGED, true)
            // Save-point interstitial (frequency-capped); navigate back once
            // it's dismissed — or immediately when no ad is shown.
            adManager.showInterstitialAd(activity) {
                navController.popBackStack()
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = if (chapterId != null) "Edit Chapter" else "New Chapter",
                onBack = { navController.popBackStack() }
            )
        },
        bottomBar = {
            if (!uiState.isLoadingChapter && !uiState.loadFailed) {
                EditorActionsBar(
                    isSaving = uiState.isSaving,
                    onScanMore = {
                        navController.navigate(
                            Routes.Scan.createRoute(fromCamera = false, returnResult = true)
                        )
                    },
                    onSave = { viewModel.save() }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        when {
            uiState.isLoadingChapter -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppColors.Purple)
                }
            }

            uiState.loadFailed -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Couldn't load chapter",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.retryLoad() },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.Purple)
                    ) { Text("Retry") }
                }
            }

            else -> EditorContent(viewModel = viewModel, padding = padding)
        }
    }

    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text  = { Text(msg) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearError() },
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.Purple)
                ) { Text("OK") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun EditorContent(
    viewModel: TextEditorViewModel,
    padding: PaddingValues
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .imePadding()
            .verticalScrollbar(listState),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item(key = "title") {
            EditorSectionLabel("Title")
            OutlinedTextField(
                value = viewModel.title,
                onValueChange = viewModel::onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Chapter title", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) },
                isError = uiState.showValidationErrors && viewModel.title.isBlank(),
                supportingText = if (uiState.showValidationErrors && viewModel.title.isBlank()) {
                    { Text("Title is required") }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = outlinedFieldColors()
            )
        }

        item(key = "description") {
            EditorSectionLabel("Description")
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = viewModel::onDescriptionChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Short description", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) },
                isError = uiState.showValidationErrors && viewModel.description.isBlank(),
                supportingText = if (uiState.showValidationErrors && viewModel.description.isBlank()) {
                    { Text("Description is required") }
                } else null,
                shape = RoundedCornerShape(14.dp),
                maxLines = 3,
                colors = outlinedFieldColors()
            )
        }

        item(key = "content-header") {
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorSectionLabel("Content")
                Text(
                    text = "Tap a paragraph to edit",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                )
            }
            if (uiState.showValidationErrors && viewModel.isContentBlank) {
                Text(
                    text = "Content is required",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
                )
            }
        }

        itemsIndexed(viewModel.contentBlocks) { index, block ->
            if (index == viewModel.editingBlockIndex) {
                EditingBlock(
                    text = block,
                    onTextChange = { viewModel.onBlockChange(index, it) },
                    onDone = { viewModel.stopEditingBlock() },
                    onRemove = { viewModel.removeBlock(index) }
                )
            } else {
                ReadBlock(
                    text = block,
                    onClick = { viewModel.startEditingBlock(index) }
                )
            }
        }

        item(key = "add-block") {
            TextButton(
                onClick = { viewModel.addBlock() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("+ Add paragraph", color = AppColors.Purple)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ReadBlock(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text.ifBlank { "Empty paragraph — tap to write" },
            style = MaterialTheme.typography.bodyMedium,
            lineHeight = 22.sp,
            color = if (text.isBlank())
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun EditingBlock(
    text: String,
    onTextChange: (String) -> Unit,
    onDone: () -> Unit,
    onRemove: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            placeholder = { Text("Write here…", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)) },
            shape = RoundedCornerShape(14.dp),
            colors = outlinedFieldColors()
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onRemove) {
                Text("Remove", color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
            }
            TextButton(onClick = onDone) {
                Text("Done", color = AppColors.Purple, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun EditorActionsBar(
    isSaving: Boolean,
    onScanMore: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onScanMore,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(0.5.dp, AppColors.Purple),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text("Scan More", color = AppColors.Purple)
            }

            Button(
                onClick = onSave,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.Purple,
                    contentColor   = Color.White
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                enabled = !isSaving,
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                } else {
                    Text("Save", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun EditorSectionLabel(text: String) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.6.sp),
        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        modifier = Modifier.padding(start = 2.dp, bottom = 4.dp)
    )
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AppColors.Purple,
    unfocusedBorderColor = AppColors.Purple.copy(alpha = 0.25f),
    focusedLabelColor    = AppColors.Purple,
    cursorColor          = AppColors.Purple
)
