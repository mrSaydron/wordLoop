package com.mrsaydron.wordloop.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mrsaydron.wordloop.data.WordDatabase
import com.mrsaydron.wordloop.data.WordEntity
import com.mrsaydron.wordloop.data.WordsRepository
import com.mrsaydron.wordloop.domain.CardStatus
import com.mrsaydron.wordloop.domain.Sm2Scheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class LessonMode {
    QUESTION,
    ANSWER,
    EMPTY
}

enum class LetterMarkType {
    CORRECT,
    INCORRECT,
    MISSING
}

data class LetterMark(
    val char: Char,
    val type: LetterMarkType
)

data class AnswerEvaluation(
    val correctWord: String,
    val userAnswer: String,
    val marks: List<LetterMark>
)

data class LessonUiState(
    val mode: LessonMode,
    val currentWord: WordEntity?,
    val userAnswer: String,
    val evaluation: AnswerEvaluation?,
    val isRandomMode: Boolean
) {
    companion object {
        fun empty(): LessonUiState = LessonUiState(
            mode = LessonMode.EMPTY,
            currentWord = null,
            userAnswer = "",
            evaluation = null,
            isRandomMode = false
        )
    }
}

enum class WordInputResult {
    SUCCESS,
    EMPTY,
    DUPLICATE
}

class WordsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = WordsRepository(
        WordDatabase.getInstance(application).wordDao()
    )

    val allWords: StateFlow<List<WordEntity>> = repository.observeAllWords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _lessonState = MutableStateFlow(LessonUiState.empty())
    val lessonState: StateFlow<LessonUiState> = _lessonState.asStateFlow()

    private var currentWordId: Long? = null
    private var dueWordsSnapshot: List<WordEntity> = emptyList()
    private val _ttsReady = MutableStateFlow(false)
    val ttsReady: StateFlow<Boolean> = _ttsReady.asStateFlow()
    private var pendingSpeak: String? = null
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(application.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.getDefault()
                _ttsReady.value = true
                pendingSpeak?.let { word ->
                    tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "lesson_word")
                    pendingSpeak = null
                }
            }
        }
        viewModelScope.launch {
            repository.observeDueWordsForToday().collect { dueWords ->
                handleDueWordsChanged(dueWords)
            }
        }
    }

    fun onAnswerChanged(value: String) {
        if (_lessonState.value.mode != LessonMode.QUESTION) return
        _lessonState.value = _lessonState.value.copy(userAnswer = value)
    }

    fun checkAnswer() {
        val state = _lessonState.value
        val word = state.currentWord ?: return
        if (state.mode != LessonMode.QUESTION) return

        val userAnswer = state.userAnswer.trim()
        val correctWord = word.word.trim()
        val isCorrect = userAnswer == correctWord
        val evaluation = AnswerEvaluation(
            correctWord = correctWord,
            userAnswer = userAnswer,
            marks = buildMarks(correctWord, userAnswer)
        )

        if (!state.isRandomMode) {
            val updatedWord = Sm2Scheduler.updateCard(
                word = word,
                isCorrect = isCorrect,
                nowMillis = System.currentTimeMillis()
            )
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateWord(updatedWord)
            }
        }

        _lessonState.value = state.copy(
            mode = LessonMode.ANSWER,
            userAnswer = userAnswer,
            evaluation = evaluation
        )
    }

    fun nextWord() {
        val state = _lessonState.value
        if (state.isRandomMode) {
            viewModelScope.launch {
                val words = withContext(Dispatchers.IO) { repository.getAllWordsOnce() }
                if (words.isEmpty()) {
                    _lessonState.value = LessonUiState.empty().copy(isRandomMode = true)
                    return@launch
                }
                val nextWord = words.random()
                currentWordId = nextWord.id
                _lessonState.value = LessonUiState(
                    mode = LessonMode.QUESTION,
                    currentWord = nextWord,
                    userAnswer = "",
                    evaluation = null,
                    isRandomMode = true
                )
            }
            return
        }

        val nextWords = dueWordsSnapshot
        if (nextWords.isEmpty()) {
            _lessonState.value = LessonUiState.empty()
            return
        }
        val nextWord = resolveNextWord(nextWords)
        currentWordId = nextWord.id
        _lessonState.value = LessonUiState(
            mode = LessonMode.QUESTION,
            currentWord = nextWord,
            userAnswer = "",
            evaluation = null,
            isRandomMode = false
        )
    }

    fun startRandomLearning() {
        viewModelScope.launch {
            val words = withContext(Dispatchers.IO) { repository.getAllWordsOnce() }
            if (words.isEmpty()) {
                _lessonState.value = LessonUiState.empty()
                return@launch
            }
            val nextWord = words.random()
            currentWordId = nextWord.id
            _lessonState.value = LessonUiState(
                mode = LessonMode.QUESTION,
                currentWord = nextWord,
                userAnswer = "",
                evaluation = null,
                isRandomMode = true
            )
        }
    }

    fun speakWord(word: String) {
        if (ttsReady.value) {
            tts.speak(word, TextToSpeech.QUEUE_FLUSH, null, "lesson_word")
        } else {
            pendingSpeak = word
        }
    }

    fun stopRandomLearning() {
        val currentState = _lessonState.value
        if (!currentState.isRandomMode) return
        currentWordId = null
        _lessonState.value = currentState.copy(
            isRandomMode = false,
            mode = LessonMode.EMPTY,
            userAnswer = "",
            evaluation = null
        )
        handleDueWordsChanged(dueWordsSnapshot)
    }

    fun deleteWord(word: WordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWord(word)
        }
    }

    suspend fun addWord(rawWord: String): WordInputResult {
        val cleaned = rawWord.trim()
        if (cleaned.isEmpty()) return WordInputResult.EMPTY
        val existing = repository.getByWord(cleaned)
        if (existing != null) return WordInputResult.DUPLICATE

        val now = System.currentTimeMillis()
        val entity = WordEntity(
            word = cleaned,
            nextReviewDate = now,
            intervalMinutes = DEFAULT_INTERVAL_MINUTES,
            ef = DEFAULT_EF,
            status = CardStatus.NEW
        )
        repository.insertWord(entity)
        return WordInputResult.SUCCESS
    }

    suspend fun updateWord(id: Long, rawWord: String): WordInputResult {
        val cleaned = rawWord.trim()
        if (cleaned.isEmpty()) return WordInputResult.EMPTY
        val existing = repository.getByWord(cleaned)
        if (existing != null && existing.id != id) return WordInputResult.DUPLICATE
        val current = repository.getById(id) ?: return WordInputResult.EMPTY
        repository.updateWord(current.copy(word = cleaned))
        return WordInputResult.SUCCESS
    }

    private fun handleDueWordsChanged(dueWords: List<WordEntity>) {
        if (_lessonState.value.isRandomMode) {
            return
        }
        dueWordsSnapshot = dueWords
        val currentState = _lessonState.value
        if (currentState.mode == LessonMode.ANSWER) {
            return
        }
        if (dueWords.isEmpty()) {
            if (currentState.mode != LessonMode.ANSWER) {
                _lessonState.value = LessonUiState.empty()
            }
            return
        }

        val resolvedWord = dueWords.firstOrNull { it.id == currentWordId } ?: dueWords.first()
        currentWordId = resolvedWord.id

        val newMode = if (currentState.mode == LessonMode.EMPTY) {
            LessonMode.QUESTION
        } else {
            currentState.mode
        }

        val newEvaluation = if (newMode == LessonMode.ANSWER &&
            currentState.currentWord?.id == resolvedWord.id
        ) {
            currentState.evaluation
        } else {
            null
        }

        _lessonState.value = currentState.copy(
            mode = newMode,
            currentWord = resolvedWord,
            evaluation = newEvaluation
        )
    }

    private fun resolveNextWord(words: List<WordEntity>): WordEntity {
        val index = words.indexOfFirst { it.id == currentWordId }
        return if (index == -1 || index == words.lastIndex) {
            words.first()
        } else {
            words[index + 1]
        }
    }

    private fun buildMarks(correctWord: String, userAnswer: String): List<LetterMark> {
        if (correctWord.isEmpty() && userAnswer.isEmpty()) return emptyList()
        return alignAndMark(userAnswer, correctWord, ignoreCase = true)
    }

    private fun alignAndMark(
        user: String,
        correct: String,
        ignoreCase: Boolean
    ): List<LetterMark> {
        val m = user.length
        val n = correct.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (user[i - 1].equals(correct[j - 1], ignoreCase = ignoreCase)) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        val aligned = ArrayList<Pair<Char?, Char?>>()
        var i = m
        var j = n
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                val cost = if (user[i - 1].equals(correct[j - 1], ignoreCase = ignoreCase)) 0 else 1
                if (dp[i][j] == dp[i - 1][j - 1] + cost) {
                    aligned.add(user[i - 1] to correct[j - 1])
                    i--
                    j--
                    continue
                }
            }
            if (i > 0 && dp[i][j] == dp[i - 1][j] + 1) {
                aligned.add(user[i - 1] to null)
                i--
                continue
            }
            if (j > 0 && dp[i][j] == dp[i][j - 1] + 1) {
                aligned.add(null to correct[j - 1])
                j--
                continue
            }
        }
        aligned.reverse()

        val marks = ArrayList<LetterMark>(aligned.size)
        for ((u, c) in aligned) {
            if (u != null) {
                val isMatch = c != null && u.equals(c, ignoreCase = ignoreCase)
                marks.add(
                    LetterMark(
                        char = u,
                        type = if (isMatch) LetterMarkType.CORRECT else LetterMarkType.INCORRECT
                    )
                )
            } else if (c != null) {
                marks.add(LetterMark(char = c, type = LetterMarkType.MISSING))
            }
        }
        return marks
    }

    companion object {
        private const val DEFAULT_INTERVAL_MINUTES = 1440L
        private const val DEFAULT_EF = 2.5
    }

    override fun onCleared() {
        super.onCleared()
        tts.stop()
        tts.shutdown()
    }
}
