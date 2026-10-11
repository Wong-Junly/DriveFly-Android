package com.devboi.drivelink.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction

@Composable
fun TextBox(
    modifier: Modifier =  Modifier,
    value: String,
    onSubmit: (String) -> Unit,
    label: String
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val text = remember(value) { mutableStateOf(value) }

    TextField(
        modifier = modifier,
        value = text.value,
        onValueChange = { text.value = it },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                onSubmit(text.value)
                keyboardController?.hide()
            }
        ),
        label = { Text(label) }
    )
}