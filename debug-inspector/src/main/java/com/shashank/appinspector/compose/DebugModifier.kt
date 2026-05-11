package com.shashank.appinspector.compose

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import com.shashank.appinspector.DebugInspector

data class ComposeDebugInfo(
    val tag: String,
    val width: Int = 0,
    val height: Int = 0,
    val positionX: Float = 0f,
    val positionY: Float = 0f,
    val additionalInfo: Map<String, String> = emptyMap()
)

fun Modifier.debugInspector(
    tag: String,
    additionalInfo: Map<String, String> = emptyMap()
): Modifier = this
    .onGloballyPositioned { coordinates ->
        val size = coordinates.size
        val position = coordinates.positionInRoot()
        DebugInspector.registerComposeElement(
            ComposeDebugInfo(
                tag = tag,
                width = size.width,
                height = size.height,
                positionX = position.x,
                positionY = position.y,
                additionalInfo = additionalInfo
            )
        )
    }
    .pointerInput(tag) {
        detectTapGestures(
            onLongPress = {
                val info = ComposeDebugInfo(
                    tag = tag,
                    additionalInfo = additionalInfo
                )
                DebugInspector.onComposeTapped(info)
            }
        )
    }

@Composable
fun DebugInspectorEffect(tag: String) {
    DisposableEffect(tag) {
        onDispose {
            DebugInspector.unregisterComposeElement(tag)
        }
    }
}
