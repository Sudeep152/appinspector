# AppInspector

A lightweight Android debug library that gives you powerful in-app inspection tools — floating button, view inspector, hierarchy viewer, ruler, design overlay, and more. **Zero overhead in release builds.**

---

## Features

| Feature | Description |
|---|---|
| Floating Button | Draggable FAB that persists across all activities |
| Shake to Open | Shake your device to open the debug menu |
| View Inspector | Tap any view to see class, id, size, margins, padding, color, text, elevation, alpha, rotation |
| Bounds Overlay | Draw boxes around every view on screen |
| Ruler Tool | Tap 2 points to measure distance in dp/px |
| Design Overlay | Load a mockup image (URL or gallery), control opacity, blend mode, and scale |
| View Hierarchy | Browse the full view tree in a bottom sheet |
| View Diff | Select 2 views and compare their properties side-by-side |
| Compose Support | Tag composables with metadata, detect long-press via callback |
| Zero-overhead noop | Release builds use empty stubs — no performance impact |
| Auto-init | Initializes automatically before `Application.onCreate()` — no setup needed |

---

## Installation

### Step 1 — Add JitPack to your project

In your root `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### Step 2 — Add the dependency

In your `app/build.gradle.kts`:

```kotlin
dependencies {
    debugImplementation("com.github.Sudeep152.appinspector:appinspector:1.0.0")
    releaseImplementation("com.github.Sudeep152.appinspector:appinspector-noop:1.0.0")
}
```

That's it. **No other setup required** — the library initializes itself automatically.

---

## Usage

### Basic — No code needed

The inspector starts automatically. Just build and run your debug app, then:
- **Shake** your device to open the debug menu, or
- Tap the **floating button** that appears on screen

### Manual Control (Optional)

```kotlin
import com.shashank.appinspector.DebugInspector

// Enable or disable the inspector at runtime
DebugInspector.setEnabled(true)
DebugInspector.setEnabled(false)

// Check state
DebugInspector.isEnabled()
DebugInspector.isInspectModeActive()

// Get current activity
DebugInspector.getCurrentActivity()
```

### Compose Support

```kotlin
import com.shashank.appinspector.compose.debugInspector
import com.shashank.appinspector.compose.DebugInspectorEffect

@Composable
fun MyCard(item: Item) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .debugInspector(
                tag = "MyCard_${item.id}",
                additionalInfo = mapOf("itemId" to item.id, "type" to item.type)
            )
    ) {
        // your content
    }

    DebugInspectorEffect(tag = "MyCard_${item.id}")
}

// Listen for long-press on tagged composables
DebugInspector.setComposeTapCallback { info ->
    Log.d("Inspector", "Tapped: ${info.tag}, size=${info.width}x${info.height}")
}
```

---

## Inspecting Views

1. Open the inspector menu → tap **Inspect**
2. Tap any view — a highlight box appears and a bottom sheet shows:
   - Class name, resource id
   - Width / height in dp and px
   - Padding and margin (all four sides)
   - Background color, text content, text size, text color
   - Alpha, elevation, visibility, rotation
3. Long-press to force-select the deepest view at that point
4. When multiple overlapping views exist, a stack picker lets you choose which to inspect

---

## Design Overlay

1. Open the inspector menu → tap **Design Overlay**
2. Choose a source: **URL** or **Gallery**
3. Adjust using the control bar:
   - **Opacity** — 0 to 100%
   - **Blend mode** — Normal, Multiply, Screen, Overlay
   - **Scale** — Fit, Fill, or Actual size

---

## How it works

- `debugImplementation` pulls in the full library with all features
- `releaseImplementation` pulls in the noop module — all methods are empty stubs, **zero runtime cost**
- Auto-initializes via a `ContentProvider` that runs before `Application.onCreate()` (same pattern as Firebase, WorkManager)
- Floating button uses `WindowManager` overlays and tracks activity lifecycle automatically
- All activity/view references use `WeakReference` to prevent memory leaks

---

## API Reference

### `DebugInspector`

| Method | Description |
|---|---|
| `init(application)` | Manual init (not needed if using auto-init) |
| `setEnabled(Boolean)` | Enable or disable the inspector |
| `isEnabled()` | Returns current enabled state |
| `isInspectModeActive()` | Returns true when tap-to-inspect is active |
| `getCurrentActivity()` | Returns the currently resumed Activity |
| `setComposeTapCallback(cb)` | Callback for Compose element long-press |
| `getRegisteredComposeElements()` | All currently tagged Compose elements |

---

## Requirements

- **Min SDK:** 24 (Android 7.0)
- **Kotlin:** 2.x
- **AGP:** 8.9+

---

## License

MIT License — Copyright (c) 2025 Shashank (Sudeep152)

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
