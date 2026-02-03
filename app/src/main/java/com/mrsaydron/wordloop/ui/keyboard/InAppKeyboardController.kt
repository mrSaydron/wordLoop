package com.mrsaydron.wordloop.ui.keyboard

import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.runtime.MutableState

class InAppKeyboardController(
    private val targetState: MutableState<AppCompatEditText?>,
    private val visibleState: MutableState<Boolean>
) {
    val target: AppCompatEditText?
        get() = targetState.value
    var enterAction: (() -> Unit)? = null

    fun attach(editText: AppCompatEditText) {
        targetState.value = editText
        visibleState.value = true
    }

    fun detach(editText: AppCompatEditText) {
        if (targetState.value == editText) {
            targetState.value = null
            visibleState.value = false
        }
    }

    fun show() {
        visibleState.value = true
    }

    fun hide() {
        visibleState.value = false
    }
}
