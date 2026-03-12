package com.mrsaydron.wordloop.ui

import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mrsaydron.wordloop.ui.keyboard.InAppKeyboardController
import com.mrsaydron.wordloop.ui.keyboard.LocalInAppKeyboard
import com.mrsaydron.wordloop.ui.keyboard.RussianKeyboard
import com.mrsaydron.wordloop.ui.keyboard.deleteBeforeCursor
import com.mrsaydron.wordloop.ui.keyboard.insertText

@Composable
fun WordLoopApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val viewModel: WordsViewModel = viewModel()
    val keyboardTarget = remember { mutableStateOf<AppCompatEditText?>(null) }
    val keyboardVisible = remember { mutableStateOf(false) }
    val keyboardController = remember {
        InAppKeyboardController(
            targetState = keyboardTarget,
            visibleState = keyboardVisible
        )
    }

    CompositionLocalProvider(LocalInAppKeyboard provides keyboardController) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                NavHost(
                    navController = navController,
                    startDestination = "lesson",
                    modifier = Modifier.weight(1f)
                ) {
                    composable("lesson") {
                        LessonScreen(
                            viewModel = viewModel,
                            onOpenList = { navController.navigate("list") }
                        )
                    }
                    composable("list") {
                        WordListScreen(
                            viewModel = viewModel,
                            onBack = {
                                viewModel.stopRandomLearning()
                                navController.popBackStack()
                            },
                            onAddWord = { navController.navigate("word_input/add/0") },
                            onEditWord = { word ->
                                navController.navigate("word_input/edit/${word.id}")
                            }
                        )
                    }
                    composable("word_input/{mode}/{wordId}") { backStackEntry ->
                        val modeArg = backStackEntry.arguments?.getString("mode").orEmpty()
                        val wordIdArg = backStackEntry.arguments
                            ?.getString("wordId")
                            ?.toLongOrNull()
                            ?: 0L
                        WordInputScreen(
                            viewModel = viewModel,
                            mode = WordInputMode.fromRoute(modeArg),
                            wordId = wordIdArg,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }

            if (keyboardVisible.value) {
                RussianKeyboard(
                    onKey = { key ->
                        keyboardTarget.value?.let { insertText(it, key) }
                    },
                    onBackspace = {
                        keyboardTarget.value?.let { deleteBeforeCursor(it) }
                    },
                    onSpace = {
                        keyboardTarget.value?.let { insertText(it, " ") }
                    },
                    onEnter = {
                        val customEnter = keyboardController.enterAction
                        if (customEnter != null) {
                            customEnter()
                        } else {
                            val target = keyboardTarget.value ?: return@RussianKeyboard
                            target.dispatchKeyEvent(
                                android.view.KeyEvent(
                                    android.view.KeyEvent.ACTION_DOWN,
                                    android.view.KeyEvent.KEYCODE_ENTER
                                )
                            )
                            target.dispatchKeyEvent(
                                android.view.KeyEvent(
                                    android.view.KeyEvent.ACTION_UP,
                                    android.view.KeyEvent.KEYCODE_ENTER
                                )
                            )
                        }
                    },
                    modifier = Modifier.align(androidx.compose.ui.Alignment.BottomCenter)
                )
            }
        }
    }
}
