@file:Suppress("unused", "UNUSED_PARAMETER")

package com.shashank.appinspector

import android.app.Activity
import android.app.Application
import com.shashank.appinspector.compose.ComposeDebugInfo

object DebugInspector {

    @JvmStatic
    fun init(application: Application) = Unit

    @JvmStatic
    fun setEnabled(enabled: Boolean) = Unit

    @JvmStatic
    fun isEnabled(): Boolean = false

    fun isInspectModeActive(): Boolean = false

    fun getCurrentActivity(): Activity? = null

    fun setComposeTapCallback(callback: (ComposeDebugInfo) -> Unit) = Unit

    fun getRegisteredComposeElements(): Map<String, ComposeDebugInfo> = emptyMap()
}
