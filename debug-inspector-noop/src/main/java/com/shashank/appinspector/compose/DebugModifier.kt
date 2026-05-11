@file:Suppress("unused", "UNUSED_PARAMETER")

package com.shashank.appinspector.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

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

@Composable
fun DebugInspectorEffect(tag: String) = Unit
