package com.mrsaydron.wordloop.ui

import androidx.activity.compose.BackHandler
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.mrsaydron.wordloop.ui.keyboard.InAppEditText
import com.mrsaydron.wordloop.ui.keyboard.LocalInAppKeyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class WordInputMode {
    ADD,
    EDIT;

    companion object {
        fun fromRoute(value: String): WordInputMode {
            return if (value.equals("edit", ignoreCase = true)) EDIT else ADD
        }
    }
}

@Composable
fun WordInputScreen(
    viewModel: WordsViewModel,
    mode: WordInputMode,
    wordId: Long,
    onBack: () -> Unit
) {
    val allWords by viewModel.allWords.collectAsState()
    val targetWord = if (mode == WordInputMode.EDIT) {
        allWords.firstOrNull { it.id == wordId }
    } else {
        null
    }
    val canConfirm = mode == WordInputMode.ADD || targetWord != null
    val initialValue = if (mode == WordInputMode.EDIT) {
        targetWord?.word.orEmpty()
    } else {
        ""
    }

    var value by remember(mode, wordId) { mutableStateOf("") }
    var errorMessage by remember(mode, wordId) { mutableStateOf<String?>(null) }
    var showSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val currentValue by rememberUpdatedState(value)
    val keyboardController = LocalSoftwareKeyboardController.current
    val inAppKeyboard = LocalInAppKeyboard.current
    val editTextRef = remember { mutableStateOf<AppCompatEditText?>(null) }

    LaunchedEffect(mode, wordId, initialValue) {
        if (mode == WordInputMode.ADD) {
            value = ""
            errorMessage = null
        } else if (targetWord != null) {
            value = initialValue
            errorMessage = null
        }
    }

    LaunchedEffect(showSuccess) {
        if (showSuccess) {
            delay(1000)
            showSuccess = false
        }
    }

    LaunchedEffect(editTextRef.value) {
        val editText = editTextRef.value ?: return@LaunchedEffect
        keyboardController?.hide()
        editText.post {
            editText.requestFocus()
            val length = editText.text?.length ?: 0
            editText.setSelection(length)
        }
        inAppKeyboard?.show()
    }

    DisposableEffect(inAppKeyboard) {
        inAppKeyboard?.enterAction = {
            if (canConfirm) {
                submitWord(
                    scope = scope,
                    mode = mode,
                    wordId = wordId,
                    value = currentValue,
                    viewModel = viewModel,
                    onError = { errorMessage = it },
                    onSuccess = {
                        if (mode == WordInputMode.ADD) {
                            value = ""
                        }
                        showSuccess = true
                    }
                )
            }
        }
        onDispose {
            inAppKeyboard?.enterAction = null
            inAppKeyboard?.hide()
        }
    }

    BackHandler { onBack() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
                Text(
                    text = if (mode == WordInputMode.EDIT) "Редактирование" else "Новое слово",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        if (showSuccess) {
            Box(
                modifier = Modifier
                    .background(Color(0xFFE3F2FD), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (mode == WordInputMode.EDIT) "Сохранено" else "Добавлено",
                    color = Color(0xFF1E3A8A),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            InAppEditText(
                value = value,
                onValueChange = {
                    value = it
                    errorMessage = null
                },
                onDone = {
                    if (canConfirm) {
                        submitWord(
                            scope = scope,
                            mode = mode,
                            wordId = wordId,
                            value = value,
                            viewModel = viewModel,
                            onError = { errorMessage = it },
                            onSuccess = {
                                if (mode == WordInputMode.ADD) {
                                    value = ""
                                }
                                showSuccess = true
                            }
                        )
                    }
                },
                hint = "Введите слово",
                modifier = Modifier.fillMaxWidth(),
                editTextRef = editTextRef
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else if (mode == WordInputMode.EDIT && targetWord == null) {
            Text(
                text = "Слово не найдено",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        } else {
            Spacer(modifier = Modifier.height(0.dp))
        }

        Button(
            onClick = {
                if (canConfirm) {
                    submitWord(
                        scope = scope,
                        mode = mode,
                        wordId = wordId,
                        value = value,
                        viewModel = viewModel,
                        onError = { errorMessage = it },
                        onSuccess = {
                            if (mode == WordInputMode.ADD) {
                                value = ""
                            }
                            showSuccess = true
                        }
                    )
                }
            },
            enabled = canConfirm,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (mode == WordInputMode.EDIT) "Сохранить" else "Добавить")
        }
    }
}

private fun submitWord(
    scope: CoroutineScope,
    mode: WordInputMode,
    wordId: Long,
    value: String,
    viewModel: WordsViewModel,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    scope.launch {
        when (
            if (mode == WordInputMode.EDIT) {
                viewModel.updateWord(wordId, value)
            } else {
                viewModel.addWord(value)
            }
        ) {
            WordInputResult.SUCCESS -> onSuccess()
            WordInputResult.EMPTY -> onError("Введите слово")
            WordInputResult.DUPLICATE -> onError("Такое слово уже есть")
        }
    }
}
