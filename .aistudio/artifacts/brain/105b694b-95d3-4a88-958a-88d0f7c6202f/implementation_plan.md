# Galaxy Pro Zoom & AI Super-Resolution Studio

An advanced computational photography and AI zoom enhancement Android application inspired by flagship Samsung Galaxy Pro Camera systems. It pairs a real-time CameraX Pro Viewfinder—complete with live luminance histograms, focus peaking, and optical vs. digital zoom telemetry—with a dual-engine AI Super-Resolution Studio that restores clarity to zoomed photographs (2x, 5x, 10x, and higher) while preserving natural identity and offering non-destructive history.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX decisions incorporate your confirmed selections from Phase 1:

- **Confirmed Decision 1 — Dual Enhancement Engine Architecture**:
  - **Detail Mode (On-Device DSP Pipeline)**: Uses a dedicated multi-stage on-device super-resolution & clarity pipeline (tiled bicubic/Lanczos-inspired resampling for 2x and 4x output, bilateral luminance/chrominance noise reduction, Sobel edge-guided adaptive unsharp masking, and local micro-contrast recovery) that preserves 100% of original facial identity, text geometry, and object shapes without inventing features.
  - **Generative AI Mode (Gemini Multimodal Image API)**: Integrates `gemini-2.5-flash-image` / `gemini-nano-banana-2.1` via REST for heavily degraded high-zoom captures, guided by strict system prompts to reconstruct degraded textures while preserving original composition and identity, paired with a mandatory **"AI-Enhanced (Generative)"** badge and metadata disclosure.
- **Confirmed Decision 2 — Pro Manual Camera Viewfinder Experience**:
  - Implements a real-time **CameraX** pro viewfinder displaying actual hardware `ZoomState` (`zoomRatio`, `minZoomRatio`, `maxZoomRatio`), quick zoom presets (`1x`, `2x`, `5x`, `10x`, `20x`), optical vs. sensor/digital zoom range indicators, a live luminance/RGB histogram overlay, real-time edge-based **Focus Peaking** highlights, and a **Picture-in-Picture (PiP) Zoom Lock Target Guide** at high magnification.
- **Confirmed Decision 3 — Non-Destructive Studio Gallery & MediaStore Export**:
  - Includes a built-in **Room-backed Studio Gallery** that stores original captures and imported zoomed photos alongside their non-destructive enhancement history, diagnostic telemetry (blur/noise/detail loss scores), and one-tap **Undo / Revert to Original** and **Export to Device Gallery (`MediaStore`)**.

---

## 1. Overview & Core Concept

- **What It Does**: Allows users to capture high-zoom photos using a pro-grade CameraX viewfinder or import existing zoomed-in photos via the Android Photo Picker, analyze them for blur, sensor noise, and compression loss, and enhance them at **1x (Clarity Restore)**, **2x**, or **4x Super-Resolution** using either deterministic **Detail Mode** or **Generative AI Mode**.
- **Target Audience / Persona**: Mobile photographers, travelers, and enthusiasts on Android 10–17 devices who want flagship-class zoom clarity, transparent optical vs. digital zoom telemetry, and honest, non-destructive photo enhancement.
- **Key Value**: Bridges the gap between hardware camera optics and post-capture enhancement while clearly educating users that digital zoom cannot recover physical optical information that the sensor never captured—and distinguishing deterministic detail sharpening from generative AI reconstruction.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Pro Camera Capture Flow**:
   - Launch into the **Pro Zoom Viewfinder** (or switch seamlessly to the Editor/Studio).
   - View real-time hardware zoom ratio (e.g., `3.8x`), lens mode badge (`OPTICAL / SENSOR` vs. `DIGITAL ZOOM`), live luminance histogram, and toggleable **Focus Peaking** overlay.
   - Tap quick zoom pills (`1x`, `2x`, `5x`, `10x`) or scrub the precision zoom arc. At `>= 4x` zoom, a floating **PiP Framing Guide** stabilizes framing and displays an educational banner when crossing into digital zoom territory.
   - Capture a photo directly into the Studio Workspace with its exact capture zoom ratio recorded.
2. **Import & Diagnostic Analysis Flow**:
   - Import any zoomed photo from the device using the zero-permission Android Photo Picker (`PickVisualMedia`) or select a built-in high-zoom test photograph (e.g., architectural text at 10x, portrait at 5x, distant landmark at 20x) for immediate testing on emulators or devices.
   - The **Zoom Clarity Analyzer** automatically inspects the image and displays real-time diagnostic metrics: **Blur Index (Laplacian Variance)**, **Noise Estimate**, **Compression Artifact Score**, and **Detected/Tagged Zoom Level** (`2x`, `5x`, `10x+`).
   - An interactive **Optical Physics Notice** explains that digital zoom interpolates sensor pixels and cannot recover physical details never captured by the lens.
3. **AI Zoom Clarity & Super-Resolution Flow**:
   - Tap **Enhance Zoom** and configure:
     - **Enhancement Mode**: **Detail Mode** (On-device deterministic edge/texture recovery, face-safe) or **Generative AI Mode** (Cloud Gemini AI texture reconstruction for heavily degraded shots, labeled as AI-enhanced).
     - **Output Scale**: **1x Clarity**, **2x Upscale**, or **4x Upscale** (memory-guarded tiled processing to support large photos safely without OOM crashes).
   - Observe asynchronous step-by-step progress (`Analyzing blur & noise` -> `Denoising & artifact removal` -> `2x/4x Super-Resolution pass` -> `Face-safe micro-contrast refinement`).
4. **Before / After Split Comparison & Non-Destructive History**:
   - Inspect results using an interactive **Before / After Draggable Split Slider** with synchronized pinch-to-zoom/pan inspection and a **Magnifier Loupe**.
   - Revert to the untouched original image at any time with **Undo Enhancement**, or save/export the enhanced photo to the device gallery.

### Visual Identity & Theme

- **Aesthetic Direction**: **Samsung Galaxy Pro Studio Dark** — precision optical instrumentation with carbon-obsidian surfaces, machined titanium borders, and laser-amber (`#FFB800`) and cyan-optic (`#00E5FF`) telemetry accents.
- **Color Palette & Mood**:
  - `--background`: `#0B0D10` (Deep Optical Obsidian)
  - `--surface`: `#14181F` (Anodized Camera Body Charcoal)
  - `--surface-variant`: `#1E242E` (Tactile Control Pod)
  - `--primary`: `#FFB800` (Galaxy Pro Laser Amber for active zoom & shutter controls)
  - `--secondary`: `#00E5FF` (Computational AI Cyan for Super-Resolution & Focus Peaking telemetry)
  - `--tertiary`: `#B388FF` (Generative AI Mode indicator & provenance badge)
- **Typography & Hierarchy**:
  - **Display / Headers**: `Space Grotesk` (bundled locally in `res/font/`) for crisp technical headers and zoom readouts.
  - **Telemetry & EXIF Readouts**: `JetBrains Mono` / `Roboto Mono` (bundled locally) for tabular numeric zoom ratios (`10.0x`), resolution badges (`3840 × 2160 • 4x SR`), and histogram metrics.
- **Interactive Feedback & Motion**:
  - Spring-animated zoom arc and preset pill transitions.
  - Real-time canvas-drawn split-screen comparison handle with tactile badge overlays (`ORIGINAL 5x DIGITAL` vs. `DETAIL MODE 2x UPSCALE` or `GENERATIVE AI RECONSTRUCTED`).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Memory-Safe Tiled Super-Resolution on Android 10–17**
  - *Chosen Approach*: Tiled asynchronous bitmap processing on `Dispatchers.Default` with hardware-accelerated rendering and automatic max-dimension capping (`BitmapFactory.Options.inSampleSize` + tile buffers) when performing 2x and 4x upscaling.
  - *Why*: Naive 4x upscaling of a 12MP photo creates a 192MP bitmap (~768MB RAM), which crashes Android phones with `OutOfMemoryError`. Tiled region processing and intelligent dimension budgeting guarantee smooth, crash-free 2x and 4x upscaling across all phones running Android 10 (API 29) through Android 17 (API 36+).
- **Decision 2: Honest Optical vs. Digital Telemetry & Face-Safe Detail Mode**
  - *Chosen Approach*: Skin-tone and facial-region luminance protection in Detail Mode + explicit provenance badges in Generative AI Mode.
  - *Why*: Prevents harsh oversharpening halos on faces or plastic skin textures in Detail Mode, and ensures users never mistake generatively synthesized pixels for real optical capture.
- **Decision 3: Real-Time CameraX Integration with Emulator & Device Compatibility**
  - *Chosen Approach*: Queries `CameraInfo.zoomState` dynamically from CameraX `ProcessCameraProvider` to report actual hardware zoom bounds, while providing real-time frame analysis (`ImageAnalysis`) for the live histogram and focus peaking overlay.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                        UI LAYER (Jetpack Compose M3)                       │
├──────────────────────────┬───────────────────────────┬─────────────────────┤
│   Pro Camera Viewfinder  │   AI Zoom Clarity Editor  │   Studio Gallery    │
│  • CameraX PreviewView   │  • Before/After Slider    │  • Non-Destructive  │
│  • Zoom Preset Pills     │  • 1x / 2x / 4x Selector  │    Version Cards    │
│  • Live RGB/Y Histogram  │  • Detail vs Generative   │  • EXIF & Zoom Tag  │
│  • Focus Peaking Canvas  │  • Diagnostic Telemetry   │  • MediaStore Save  │
│  • PiP Zoom Lock Guide   │  • Undo / Reset Original  │  • Sample Test Kits │
└────────────┬─────────────┴─────────────┬─────────────┴──────────┬──────────┘
             │                           │                        │
             ▼                           ▼                        ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   VIEWMODEL & STATE LAYER (StateFlow)                      │
│  • CameraZoomState (actualZoomRatio, min/maxZoom, isOpticalRange, peaking) │
│  • EditorEnhancementState (originalBitmap, enhancedBitmap, progress,       │
│    diagnostics: Blur/Noise/DetailLoss, mode: DETAIL | GENERATIVE_AI,       │
│    scaleFactor: 1x | 2x | 4x, splitFraction, isAiReconstructed)            │
└────────────┬───────────────────────────┬────────────────────────┬──────────┘
             │                           │                        │
             ▼                           ▼                        ▼
┌─────────────────────────┐ ┌─────────────────────────┐ ┌────────────────────┐
│  CameraX & Frame Engine │ │ Super-Resolution Engine │ │ Persistence Layer  │
│  • ProcessCameraProvider│ │ • ImageDiagnostics      │ │ • Room Database    │
│  • ZoomControl.setZoom  │ │   (Laplacian / Noise)   │ │   (PhotoSessionDao)│
│  • ImageAnalysis        │ │ • DetailModeProcessor   │ │ • Internal File    │
│    (Histogram & Sobel   │ │   (2x/4x Tiled Lanczos, │ │   Storage (Orig +  │
│     Focus Peaking)      │ │    Bilateral Denoise,   │ │   Enhanced PNG/JPG)│
│  • ImageCapture         │ │    Face-Safe Unsharp)   │ │ • MediaStore       │
│                         │ │ • GeminiGenerativeApi   │ │   Exporter         │
└─────────────────────────┘ └─────────────────────────┘ └────────────────────┘
```

### Data Model & State

- **`PhotoEnhancementRecord` (Room Entity)**:
  - Stores `id`, `title`, `originalFilePath`, `enhancedFilePath` (nullable until enhanced), `capturedZoomRatio` (`Float`, e.g., `1.0f`, `2.0f`, `5.0f`, `10.0f`), `isOpticalCapture` (`Boolean`), `enhancementMode` (`NONE`, `DETAIL_MODE`, `GENERATIVE_AI_MODE`), `upscaleFactor` (`1`, `2`, `4`), `originalWidth`, `originalHeight`, `enhancedWidth`, `enhancedHeight`, `blurScore`, `noiseScore`, `detailLossScore`, `isGenerativeAiLabeled`, and `timestamp`.
- **`ImageDiagnostics` (Domain Model)**:
  - Computed asynchronously from the source bitmap:
    - `blurSeverity` (0–100, derived from 3x3 Laplacian variance)
    - `noiseLevel` (0–100, derived from high-frequency local standard deviation in flat patches)
    - `compressionArtifactScore` (0–100, derived from 8x8 block boundary discontinuities)
    - `recommendedMode` (`DETAIL_MODE` for moderate blur/noise; suggests optional `GENERATIVE_AI_MODE` when severe degradation is detected)

### Interactive Component & State Mapping

- **Pro Viewfinder Controls**:
  - Pinch gesture and zoom preset buttons (`1x`, `2x`, `5x`, `10x`, `Max`) call `cameraControl.setZoomRatio(targetRatio)` bounded by `cameraInfo.zoomState.value`.
  - Focus Peaking toggle enables real-time Sobel edge magnitude thresholding in `ImageAnalysis.Analyzer`, rendered as a crisp cyan highlight overlay aligned with the viewfinder.
  - Live Histogram toggle computes 64-bin luminance distribution per frame and renders a real-time waveform in the top HUD.
- **Photo Editor & Enhance Zoom Workflow**:
  - Selecting **Enhance Zoom** runs the chosen pipeline on a background coroutine with granular stage updates (`0% -> 25% -> 60% -> 90% -> 100%`).
  - **Detail Mode** executes on-device:
    1. Bilateral chroma/luma denoise & JPEG block de-artifacting.
    2. Memory-safe 2x or 4x bicubic/Lanczos-kernel upscaling with edge-directed interpolation.
    3. Skin-tone-aware (YCbCr mask) adaptive unsharp masking so faces and skin textures stay natural while text, foliage, and architectural edges gain crisp definition.
  - **Generative AI Mode** calls Gemini (`gemini-2.5-flash-image` / `gemini-nano-banana-2.1`) via Retrofit with `BuildConfig.GEMINI_API_KEY` to reconstruct severely degraded high-zoom crops while preserving subject identity, and automatically attaches a persistent **"AI-Enhanced (Generative)"** badge in the UI and exported image option. If no API key is configured, it provides a clear diagnostic message and offers seamless fallback to high-recovery Detail Mode.
  - **Before / After Slider**: Interactive horizontal drag handle (`0.0f..1.0f`) clipping the Original (scaled to match viewport) on the left and Enhanced output on the right, plus a toggleable 3x **Detail Loupe** to inspect fine pixel recovery.
