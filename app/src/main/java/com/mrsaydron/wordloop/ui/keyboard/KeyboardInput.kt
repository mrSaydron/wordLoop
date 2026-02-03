package com.mrsaydron.wordloop.ui.keyboard

import androidx.appcompat.widget.AppCompatEditText
import kotlin.math.max
import kotlin.math.min

fun insertText(editText: AppCompatEditText, text: String) {
    val editable = editText.editableText ?: return
    val start = editText.selectionStart.coerceAtLeast(0)
    val end = editText.selectionEnd.coerceAtLeast(0)
    val minPos = min(start, end)
    val maxPos = max(start, end)
    editable.replace(minPos, maxPos, text)
    editText.setSelection(minPos + text.length)
}

fun deleteBeforeCursor(editText: AppCompatEditText) {
    val editable = editText.editableText ?: return
    val start = editText.selectionStart.coerceAtLeast(0)
    val end = editText.selectionEnd.coerceAtLeast(0)
    val minPos = min(start, end)
    val maxPos = max(start, end)
    if (minPos != maxPos) {
        editable.delete(minPos, maxPos)
        editText.setSelection(minPos)
        return
    }
    if (minPos > 0) {
        editable.delete(minPos - 1, minPos)
        editText.setSelection(minPos - 1)
    }
}
