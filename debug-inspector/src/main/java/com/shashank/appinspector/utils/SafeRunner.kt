package com.shashank.appinspector.utils

import android.util.Log

internal object SafeRunner {

    private const val TAG = "DebugInspector"

    inline fun run(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Inspector error (non-fatal)", e)
        }
    }

    inline fun <T> runWithResult(fallback: T, block: () -> T): T {
        return try {
            block()
        } catch (e: Exception) {
            Log.e(TAG, "Inspector error (non-fatal)", e)
            fallback
        }
    }
}
