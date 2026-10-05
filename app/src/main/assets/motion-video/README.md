# Zenith Studio — 16:9 Motion Graphics Launch Video

A cinematic, interactive 16:9 motion graphics video and launch reel generator for **Zenith Studio**, modeled after the **`latent-spaces/brag`** methodology for developer-first project launches.

---

## 🎬 Video Overview

- **Aspect Ratio**: 16:9 (1920 × 1080 Full HD)
- **Duration**: 20 Seconds (1,200 Frames at 60 FPS)
- **Style**: Sleek minimalist modern tech product launch (Apple / Linear keynote aesthetic: deep obsidian canvas `#06070a`, glowing cyan and ultraviolet laser paths, 3D exploded isometric layer cards, and kinetic typography)
- **Audio**: Built-in parametric Web Audio API sci-fi synthesizer (no external MP3 downloads required; togglable via `🔊` / `M` key)

---

## ⏱ Scene Storyboard

| Timestamp | Frame Range | Scene Title | Key Motion Graphics Elements |
|---|---|---|---|
| **00:00 – 00:04** | `0 – 240` | **Scene 1: Monolith Reveal** | Atmospheric starfield, receding perspective grid horizon, pulsing chromatic diamond shield, glowing "Z" emblem assembly, and typography reveal. |
| **00:04 – 00:08** | `240 – 480` | **Scene 2: Sub-Pixel Bezier Math** | Dynamic CAD grid plane, animated cubic Bezier spline with moving tangent handles (P0, C0, C1, P1), traveling laser pulse, and real-time SDF telemetry HUD. |
| **00:08 – 00:13** | `480 – 780` | **Scene 3: 3D GPU Layer Explosion** | 3D isometric tilt of the Zenith stack separating into 4 floating translucent glass panels (Vector, Camera RAW LUT, Kawase Blur, 16-bit Float Artboard) with live parameter dials. |
| **00:13 – 00:17** | `780 – 1020` | **Scene 4: Kinetic Typography & Presets** | Rhythmic typographic cuts (*"EVERY STROKE."* ➔ *"EVERY PIXEL."* ➔ *"INFINITE DEPTH."*), gradient text spans, and dynamic font engine comparisons. |
| **00:17 – 00:20** | `1020 – 1200` | **Scene 5: Finale Lockup & CTA** | Layers collapse into a floating mobile phone frame with radiant solar corona, feature checklist, and Product Hunt / GitHub launch badges. |

---

## 🚀 How to Run the Interactive Showcase

You can open `index.html` directly in any web browser:

```bash
# Open directly in your browser:
open motion-video/index.html
# or with any local server:
npx serve motion-video
```

### ⌨️ Keyboard & Scrubber Shortcuts

- **Space**: Play / Pause playback
- **Left Arrow (←)**: Step backward 1 frame (or 1 second with `Shift`)
- **Right Arrow (→)**: Step forward 1 frame (or 1 second with `Shift`)
- **M**: Mute / Unmute built-in synthesizer soundscape
- **S**: Capture instant high-resolution PNG snapshot of the active frame
- **F**: Toggle Fullscreen cinema mode
- **1x / 1.5x / 2x / 0.5x**: Adjust timeline playback speed

---

## 🎥 Rendering to MP4 with latent-spaces/brag / Puppeteer

To export all 1,200 frames frame-by-frame and compile them into a broadcast-quality MP4:

```bash
cd motion-video

# 1. Install Puppeteer
npm install puppeteer

# 2. Run the deterministic frame recorder
node export.js
```

The script will launch a headless browser at 1920×1080, advance frame-by-frame via `engine.seek(f)` without dropped frames, capture each frame, and execute `ffmpeg` to produce `zenith-launch.mp4`.
