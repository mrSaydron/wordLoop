package com.mrsaydron.wordloop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.appcompat.widget.AppCompatEditText
import com.mrsaydron.wordloop.ui.keyboard.InAppEditText
import com.mrsaydron.wordloop.ui.keyboard.LocalInAppKeyboard

@Composable
fun LessonScreen(
    viewModel: WordsViewModel,
    onOpenList: () -> Unit
) {
    val state by viewModel.lessonState.collectAsState()
    val allWords by viewModel.allWords.collectAsState()
    val ttsReady by viewModel.ttsReady.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val inAppKeyboard = LocalInAppKeyboard.current
    val editTextRef = remember { mutableStateOf<AppCompatEditText?>(null) }

    LaunchedEffect(state.mode, state.currentWord?.id, ttsReady) {
        if (state.mode == LessonMode.QUESTION) {
            keyboardController?.hide()
            editTextRef.value?.post {
                editTextRef.value?.requestFocus()
                val length = editTextRef.value?.text?.length ?: 0
                editTextRef.value?.setSelection(length)
            }
            inAppKeyboard?.show()
            inAppKeyboard?.enterAction = if (ttsReady) viewModel::checkAnswer else null
            state.currentWord?.word?.let { word ->
                viewModel.speakWord(word)
            }
        } else {
            keyboardController?.hide()
            inAppKeyboard?.hide()
            inAppKeyboard?.enterAction = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onOpenList) {
                Icon(Icons.Default.List, contentDescription = "Список слов")
            }
        }

        when (state.mode) {
            LessonMode.EMPTY -> {
                Text(
                    text = "Слова на сегодня закончились",
                    style = MaterialTheme.typography.titleMedium
                )
                if (allWords.isNotEmpty()) {
                    Button(
                        onClick = viewModel::startRandomLearning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Учить случайные")
                    }
                }
            }

            LessonMode.QUESTION -> {
                QuestionContent(
                    userAnswer = state.userAnswer,
                    onAnswerChanged = viewModel::onAnswerChanged,
                    onCheck = viewModel::checkAnswer,
                    onSpeak = {
                        state.currentWord?.word?.let { word ->
                            viewModel.speakWord(word)
                        }
                    },
                    isTtsReady = ttsReady,
                    editTextRef = editTextRef
                )
            }

            LessonMode.ANSWER -> {
                AnswerContent(
                    evaluation = state.evaluation,
                    onNext = viewModel::nextWord
                )
            }
        }
    }
}

@Composable
private fun QuestionContent(
    userAnswer: String,
    onAnswerChanged: (String) -> Unit,
    onCheck: () -> Unit,
    onSpeak: () -> Unit,
    isTtsReady: Boolean,
    editTextRef: androidx.compose.runtime.MutableState<AppCompatEditText?>
) {
    if (!isTtsReady) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    }

    IconButton(
        onClick = onSpeak,
        enabled = isTtsReady,
        modifier = Modifier.size(96.dp)
    ) {
        Icon(
            imageVector = Icons.Default.VolumeUp,
            contentDescription = "Озвучить",
            modifier = Modifier.size(64.dp)
        )
    }

    NoSuggestionsTextField(
        value = userAnswer,
        onValueChange = onAnswerChanged,
        onDone = { if (isTtsReady) onCheck() },
        editTextRef = editTextRef
    )

    Button(
        onClick = onCheck,
        enabled = isTtsReady,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Проверить")
    }
}

@Composable
private fun NoSuggestionsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    editTextRef: androidx.compose.runtime.MutableState<AppCompatEditText?>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        InAppEditText(
            value = value,
            onValueChange = onValueChange,
            onDone = onDone,
            hint = "Введите слово",
            modifier = Modifier.fillMaxWidth(),
            editTextRef = editTextRef
        )
    }
}

@Composable
private fun AnswerContent(
    evaluation: AnswerEvaluation?,
    onNext: () -> Unit
) {
    if (evaluation == null) {
        Text("Нет данных ответа")
        return
    }

    Text(
        text = evaluation.correctWord,
        fontSize = 28.sp,
        style = MaterialTheme.typography.titleLarge
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        evaluation.marks.forEach { mark ->
            val color = when (mark.type) {
                LetterMarkType.CORRECT -> Color(0xFF00C853)
                LetterMarkType.MISSING -> Color(0xFFFFD600)
                LetterMarkType.INCORRECT -> Color(0xFFD50000)
            }
            Box(
                modifier = Modifier
                    .background(color, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = mark.char.toString(),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(8.dp))

    Button(
        onClick = onNext,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Дальше")
    }
}
