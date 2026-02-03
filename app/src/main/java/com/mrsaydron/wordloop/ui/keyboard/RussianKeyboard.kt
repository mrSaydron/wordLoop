package com.mrsaydron.wordloop.ui.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardCapslock
import androidx.compose.material.icons.filled.SpaceBar

private val row1 = listOf("й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ")
private val row2 = listOf("ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э")
private val row3 = listOf("я", "ч", "с", "м", "и", "т", "ь", "б", "ю")

@Composable
fun RussianKeyboard(
    onKey: (String) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (isShifted, setShifted) = remember { mutableStateOf(false) }

    fun emitKey(key: String) {
        val text = if (isShifted) key.uppercase() else key
        onKey(text)
        if (isShifted) {
            setShifted(false)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            KeyRow(keys = row1, onKey = ::emitKey, modifier = Modifier.fillMaxWidth())
            KeyRow(keys = row2, onKey = ::emitKey, modifier = Modifier.fillMaxWidth())
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KeyButton(
                    onClick = { setShifted(!isShifted) },
                    modifier = Modifier.weight(1.2f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardCapslock,
                        contentDescription = "Shift"
                    )
                }
                Box(modifier = Modifier.weight(6f)) {
                    KeyRow(
                        keys = row3,
                        onKey = ::emitKey,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                KeyButton(
                    onClick = onBackspace,
                    modifier = Modifier.weight(1.4f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace"
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KeyButton(
                    onClick = onSpace,
                    modifier = Modifier.weight(6f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SpaceBar,
                        contentDescription = "Пробел"
                    )
                }
                KeyButton(
                    onClick = onEnter,
                    modifier = Modifier.weight(2f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Проверить"
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<String>,
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        keys.forEach { key ->
            KeyButton(
                onClick = { onKey(key) },
                modifier = Modifier.weight(1f)
            ) {
                Text(text = key, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick)
            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
