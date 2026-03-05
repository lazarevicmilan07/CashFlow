package com.moneytracker.simplebudget.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

class DragDropListState(val lazyListState: LazyListState) {
    var draggedKey by mutableStateOf<Any?>(null)
        private set

    var draggedOffset by mutableFloatStateOf(0f)
        private set

    private var draggedGroup: String? = null

    var groupKeyOf: (Any) -> String = { "" }
    var onSwap: (Any, Any) -> Unit = { _, _ -> }
    var onDragEnd: () -> Unit = {}

    val isDragging: Boolean get() = draggedKey != null

    fun startDrag(key: Any) {
        draggedKey = key
        draggedGroup = groupKeyOf(key)
        draggedOffset = 0f
    }

    fun onDrag(delta: Float) {
        val key = draggedKey ?: return
        draggedOffset += delta

        val draggedInfo = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key == key } ?: return

        val draggedCenter = draggedInfo.offset + draggedInfo.size / 2 + draggedOffset.toInt()

        val target = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                item.key != key &&
                    groupKeyOf(item.key) == draggedGroup &&
                    draggedCenter in item.offset..(item.offset + item.size)
            }

        if (target != null) {
            draggedOffset += draggedInfo.offset - target.offset
            onSwap(key, target.key)
        }
    }

    fun endDrag() {
        if (isDragging) onDragEnd()
        draggedKey = null
        draggedOffset = 0f
        draggedGroup = null
    }
}

@Composable
fun rememberDragDropListState(lazyListState: LazyListState): DragDropListState {
    return remember(lazyListState) { DragDropListState(lazyListState) }
}

fun Modifier.dragHandle(state: DragDropListState, key: Any): Modifier {
    return this.pointerInput(key) {
        detectDragGestures(
            onDragStart = { state.startDrag(key) },
            onDrag = { change, offset ->
                change.consume()
                state.onDrag(offset.y)
            },
            onDragEnd = { state.endDrag() },
            onDragCancel = { state.endDrag() }
        )
    }
}

fun Modifier.draggableItem(state: DragDropListState, key: Any): Modifier {
    val isDragging = state.draggedKey == key
    return this
        .zIndex(if (isDragging) 1f else 0f)
        .graphicsLayer {
            translationY = if (isDragging) state.draggedOffset else 0f
            shadowElevation = if (isDragging) 8f else 0f
        }
}
