package com.devboi.drivelink.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp

data class DropDownItem<T> (
    val text: String,
    val value: T
)

@Composable
fun <T> Dropdown(
    modifier: Modifier,
    dropDownItems: List<DropDownItem<T>>,
    defaultItem: DropDownItem<T> = dropDownItems.last(),
    onItemClick: (DropDownItem<T>) -> Unit
) {
    var showDropdown by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf(defaultItem) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    var itemHeight by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Card(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        ),
        modifier = modifier
            .onSizeChanged {
                itemHeight = with(density) { it.height.toDp() }
            }
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth(0.5f)
                .pointerInput(true) {
                    detectTapGestures(
                        onPress = {
                            showDropdown = true
                            pressOffset = DpOffset(it.x.toDp(), it.y.toDp())
                        }
                    )
                }
                .padding(16.dp)
        ) {
            Text(selectedItem.text)
        }

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
            offset = pressOffset
        ) {
            dropDownItems.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        onItemClick(item)
                        selectedItem = item
                        showDropdown = false
                    },
                    text = {
                    Text(text = item.text)
                })
            }
        }
    }

}