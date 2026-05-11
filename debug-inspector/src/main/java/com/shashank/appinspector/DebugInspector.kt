@file:Suppress("unused")

package com.shashank.appinspector

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.shashank.appinspector.compose.ComposeDebugInfo
import com.shashank.appinspector.floating.FloatingButtonManager
import com.shashank.appinspector.utils.ShakeDetector
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

object DebugInspector {

    private const val TAG = "DebugInspector"

    internal val config = DebugConfig()
    private var floatingButtonManager: FloatingButtonManager? = null
    private var applicationRef: WeakReference<Application>? = null
    private var shakeDetector: ShakeDetector? = null

    private val composeElements = ConcurrentHashMap<String, ComposeDebugInfo>()
    internal var composeTapCallback: ((ComposeDebugInfo) -> Unit)? = null

    private var currentActivityRef: WeakReference<Activity>? = null

    @JvmStatic
    fun init(application: Application) {
        try {
            synchronized(this) {
                if (config.isInitialized) return
                config.isInitialized = true
            }
            applicationRef = WeakReference(application)
            val manager = FloatingButtonManager(config)
            floatingButtonManager = manager

            val shake = ShakeDetector { manager.toggleMenuForCurrentActivity() }
            shakeDetector = shake

            application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
                override fun onActivityStarted(activity: Activity) {}

                override fun onActivityResumed(activity: Activity) {
                    try {
                        currentActivityRef = WeakReference(activity)
                        manager.attach(activity)
                        shake.register(activity)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onActivityResumed", e)
                    }
                }

                override fun onActivityPaused(activity: Activity) {
                    try {
                        shake.unregister(activity)
                    } catch (_: Exception) {}
                }

                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

                override fun onActivityDestroyed(activity: Activity) {
                    try {
                        manager.detach(activity)
                        if (currentActivityRef?.get() === activity) {
                            currentActivityRef = null
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in onActivityDestroyed", e)
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize DebugInspector", e)
        }
    }

    @JvmStatic
    fun setEnabled(enabled: Boolean) {
        config.isEnabled = enabled
        if (!enabled) config.resetAll()
    }

    @JvmStatic
    fun isEnabled(): Boolean = config.isEnabled

    fun isInspectModeActive(): Boolean = config.isInspectModeEnabled

    fun getCurrentActivity(): Activity? = currentActivityRef?.get()

    internal fun getFloatingButtonManager(): FloatingButtonManager? = floatingButtonManager

    internal fun registerComposeElement(info: ComposeDebugInfo) {
        composeElements[info.tag] = info
    }

    internal fun unregisterComposeElement(tag: String) {
        composeElements.remove(tag)
    }

    internal fun onComposeTapped(info: ComposeDebugInfo) {
        if (!config.isInspectModeEnabled) return
        composeTapCallback?.invoke(info)
    }

    fun setComposeTapCallback(callback: (ComposeDebugInfo) -> Unit) {
        composeTapCallback = callback
    }

    fun getRegisteredComposeElements(): Map<String, ComposeDebugInfo> {
        return composeElements.toMap()
    }
}
