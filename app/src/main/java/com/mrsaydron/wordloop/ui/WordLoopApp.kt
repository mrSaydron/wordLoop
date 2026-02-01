package com.mrsaydron.wordloop.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun WordLoopApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val viewModel: WordsViewModel = viewModel()

    NavHost(
        navController = navController,
        startDestination = "lesson",
        modifier = modifier
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
                }
            )
        }
    }
}
