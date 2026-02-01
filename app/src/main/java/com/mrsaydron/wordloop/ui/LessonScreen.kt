package com.mrsaydron.wordloop.ui

import android.text.InputType
import android.view.inputmethod.EditorInfo
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.widget.addTextChangedListener

@Composable
fun LessonScreen(
    viewModel: WordsViewModel,
    onOpenList: () -> Unit
) {
    val state by viewModel.lessonState.collectAsState()
    val allWords by viewModel.allWords.collectAsState()
    val ttsReady by viewModel.ttsReady.collectAsState()
    val keyboardController = LocalSoftwareKeyboardController.current
    val editTextRef = remember { mutableStateOf<AppCompatEditText?>(null) }

    LaunchedEffect(state.mode, state.currentWord?.id) {
        if (state.mode == LessonMode.QUESTION) {
            editTextRef.value?.requestFocus()
            keyboardController?.show()
            state.currentWord?.word?.let { word ->
                viewModel.speakWord(word)
            }
        } else {
            keyboardController?.hide()
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
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AppCompatEditText(context).apply {
                    editTextRef.value = this
                    setSingleLine(true)
                    setHint("Введите слово")
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                    imeOptions = EditorInfo.IME_ACTION_DONE or
                        EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
                    addTextChangedListener { editable ->
                        val newText = editable?.toString().orEmpty()
                        if (newText != value) {
                            onValueChange(newText)
                        }
                    }
                    setOnEditorActionListener { _, actionId, _ ->
                        if (actionId == EditorInfo.IME_ACTION_DONE) {
                            onDone()
                            true
                        } else {
                            false
                        }
                    }
                }
            },
            update = { editText ->
                if (editText.text?.toString() != value) {
                    editText.setText(value)
                    editText.setSelection(value.length)
                }
            }
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
