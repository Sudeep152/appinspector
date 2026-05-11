package com.shashank.appinspector

internal class DebugConfig {

    @Volatile var isInspectModeEnabled: Boolean = false
    @Volatile var isDesignOverlayActive: Boolean = false
    @Volatile var isBoundsOverlayActive: Boolean = false
    @Volatile var isRulerActive: Boolean = false
    @Volatile var isCompareActive: Boolean = false
    @Volatile var isInitialized: Boolean = false
    @Volatile var isEnabled: Boolean = true

    val hasActiveFeature: Boolean
        get() = isInspectModeEnabled || isDesignOverlayActive || isBoundsOverlayActive || isRulerActive || isCompareActive

    fun resetAll() {
        isInspectModeEnabled = false
        isDesignOverlayActive = false
        isBoundsOverlayActive = false
        isRulerActive = false
        isCompareActive = false
    }

    companion object {
        const val INSPECTOR_VIEW_TAG = "__debug_inspector__"
    }
}
