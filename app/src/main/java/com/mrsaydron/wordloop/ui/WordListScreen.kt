package com.mrsaydron.wordloop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.mrsaydron.wordloop.data.WordEntity
import com.mrsaydron.wordloop.domain.CardStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max

@Composable
fun WordListScreen(
    viewModel: WordsViewModel,
    onBack: () -> Unit
) {
    val words by viewModel.allWords.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var editWord by remember { mutableStateOf<WordEntity?>(null) }
    var sortMode by remember { mutableStateOf(SortMode.ALPHABETICAL) }
    var showAddSuccess by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(showAddSuccess) {
        if (showAddSuccess) {
            delay(1000)
            showAddSuccess = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        sortMode = if (sortMode == SortMode.ALPHABETICAL) {
                            SortMode.NEXT_REVIEW
                        } else {
                            SortMode.ALPHABETICAL
                        }
                    }) {
                        if (sortMode == SortMode.ALPHABETICAL) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text("А")
                                Icon(Icons.Default.ArrowUpward, contentDescription = null)
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null)
                                Icon(Icons.Default.ArrowUpward, contentDescription = null)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.padding(start = 8.dp))
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить слово")
                    }
                }
            }

            val sortedWords = when (sortMode) {
                SortMode.ALPHABETICAL -> words.sortedBy { it.word.lowercase() }
                SortMode.NEXT_REVIEW -> words.sortedBy {
                    daysUntilForSort(System.currentTimeMillis(), it)
                }
            }

            if (sortedWords.isEmpty()) {
                Text(
                    text = "Список пуст",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(sortedWords, key = { it.id }) { word ->
                        val nowMillis = System.currentTimeMillis()
                        val rowColor = when {
                            word.status == CardStatus.PROGRESS_RESET -> Color(0xFFFFEBEE)
                            word.nextReviewDate <= nowMillis -> Color(0xFFE8F5E9)
                            else -> Color(0xFFE3F2FD)
                        }
                        val daysUntil = if (word.status == CardStatus.PROGRESS_RESET) {
                            0L
                        } else {
                            daysUntilReview(nowMillis, word.nextReviewDate)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(rowColor, RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${word.word} (${daysUntil} дн.)",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { editWord = word }
                            )
                            IconButton(onClick = { viewModel.deleteWord(word) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Удалить")
                            }
                        }
                    }
                }
            }
        }

        if (showAddSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFE3F2FD), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color(0xFF1E3A8A), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(text = "W", color = Color.White)
                        }
                        Text(text = "+", color = Color(0xFF1E3A8A))
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        WordInputDialog(
            title = "Новое слово",
            confirmText = "Добавить",
            initialValue = "",
            onDismiss = { showAddDialog = false },
            onConfirm = { value, setError, clearValue ->
                scope.launch {
                    when (viewModel.addWord(value)) {
                        WordInputResult.SUCCESS -> {
                            clearValue()
                            showAddSuccess = true
                        }
                        WordInputResult.EMPTY ->
                            setError("Введите слово")
                        WordInputResult.DUPLICATE ->
                            setError("Такое слово уже есть")
                    }
                }
            }
        )
    }

    editWord?.let { editing ->
        WordInputDialog(
            title = "Редактирование",
            confirmText = "Сохранить",
            initialValue = editing.word,
            onDismiss = { editWord = null },
            onConfirm = { value, setError, _ ->
                scope.launch {
                    when (viewModel.updateWord(editing.id, value)) {
                        WordInputResult.SUCCESS -> editWord = null
                        WordInputResult.EMPTY -> setError("Введите слово")
                        WordInputResult.DUPLICATE -> setError("Такое слово уже есть")
                    }
                }
            }
        )
    }
}

private enum class SortMode {
    ALPHABETICAL,
    NEXT_REVIEW
}

private fun daysUntilReview(nowMillis: Long, nextReviewDate: Long): Long {
    val diffMillis = nextReviewDate - nowMillis
    if (diffMillis <= 0L) return 0L
    val days = ceil(diffMillis / 86_400_000.0)
    return max(0L, days.toLong())
}

private fun daysUntilForSort(nowMillis: Long, word: WordEntity): Long {
    return if (word.status == CardStatus.PROGRESS_RESET) {
        0L
    } else {
        daysUntilReview(nowMillis, word.nextReviewDate)
    }
}

@Composable
private fun WordInputDialog(
    title: String,
    confirmText: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String, (String) -> Unit, () -> Unit) -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialValue) {
        value = initialValue
        errorMessage = null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = value,
                    onValueChange = {
                        value = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(value, { errorMessage = it }) {
                        value = ""
                        errorMessage = null
                    }
                }
            ) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
