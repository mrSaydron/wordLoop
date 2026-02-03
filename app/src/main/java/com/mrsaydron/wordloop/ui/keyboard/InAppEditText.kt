package com.mrsaydron.wordloop.ui.keyboard

import android.os.Build
import android.text.InputType
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.widget.addTextChangedListener

@Composable
fun InAppEditText(
    value: String,
    onValueChange: (String) -> Unit,
    onDone: () -> Unit,
    hint: String,
    modifier: Modifier = Modifier,
    editTextRef: MutableState<AppCompatEditText?>? = null
) {
    val keyboardController = LocalInAppKeyboard.current

    AndroidView(
        modifier = modifier,
        factory = { context ->
            AppCompatEditText(context).apply {
                editTextRef?.value = this
                setSingleLine(true)
                setHint(hint)
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
                imeOptions = EditorInfo.IME_ACTION_DONE
                setShowSoftInputOnFocus(false)
                importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setAutofillHints(*emptyArray())
                }
                addTextChangedListener { editable ->
                    val newText = editable?.toString().orEmpty()
                    if (newText != value) {
                        onValueChange(newText)
                    }
                }
                setOnKeyListener { _, keyCode, event ->
                    if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                        onDone()
                        true
                    } else {
                        false
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
                setOnFocusChangeListener { _, hasFocus ->
                    if (hasFocus) {
                        keyboardController?.attach(this)
                    } else {
                        keyboardController?.detach(this)
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
