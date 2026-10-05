# Anchor Handle Touch Registration Fix & Interactive Anchor Mode Menu

A comprehensive solution to fix touch hit detection for all 8 shape resize handles and edge zones, and add an interactive dropdown menu to the transform HUD badge allowing users to switch between Center Symmetrical, Opposite Corner, and Fixed Origin anchor modes.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following parameters were confirmed during clarification and guide this implementation:

- **Anchor Modes in Dropdown Menu**: **Center Symmetrical, Opposite Corner, and Fixed Origin**. Tapping the transform HUD badge opens a dropdown menu letting the user select their preferred anchor behavior on the fly, with an integrated Aspect Ratio Lock toggle.
- **Touch Hit Detection Priority**: **Prioritize resizing with generous corner and edge touch zones**. Instead of falling back to moving the shape, touch events on or near the bounding box perimeter (minimum 48dp touch target) immediately lock into resize mode.
- **Layer-Local Precision Math**: Convert touches directly to layer-local coordinates (`canvasToLayerLocal`) for hit detection and live scaling, eliminating coordinate drift and scale mismatch on high-density screens.

---

## 1. Overview & Core Concept

- **What It Does**:
  1. Fixes the shape resize touch registration bug so dragging *any* corner, handle, or edge reliably resizes the shape instead of accidentally moving it.
  2. Replaces the static "Symmetric: Center" badge in the Floating Shape Transform HUD with an interactive dropdown menu button.
  3. Provides three fully functional anchor modes:
     - **Center Symmetrical**: Resizes outward/inward from the center pivot.
     - **Opposite Corner (Freeform)**: The corner or edge opposite the dragged handle stays fixed in place while the shape scales toward the touch point.
     - **Fixed Origin (Top-Left)**: The top-left corner remains stationary while width/height scale.
- **Target Audience / Persona**: Designers and digital artists who need fast, tactile, and dependable vector shape transformations on mobile touchscreens.
- **Key Value**: Guarantees that shape resizing works from every handle and edge with zero missed touches, while giving users full control over anchor interaction physics.

---

## 2. User Experience & Visual Design

### Interactive Flow
1. **Selecting a Shape**:
   - The user selects a shape or spawns one from the "Shapes" dialog.
   - The shape displays the amber dashed bounding box with 8 illuminated anchor handles.
   - The Floating Shape Transform HUD appears at the bottom with live dimensions, lock icon, and the new **Anchor Mode Badge** (e.g. `Anchor: Center ▾`).

2. **Changing Anchor Modes via Dropdown**:
   - Tapping the badge opens a sleek dark-slate Material 3 `DropdownMenu`.
   - The menu displays 3 selectable anchor behaviors with icons and descriptive subtitles:
     - 🎯 **Center Symmetrical**: Scale equally around center point.
     - 📐 **Opposite Corner**: Anchor opposite corner in place.
     - 📌 **Fixed Origin (Top-Left)**: Keep top-left stationary.
     - 🔒 **Aspect Ratio Lock**: Quick toggle for proportional scaling.
   - Selecting a mode updates the badge label and applies immediately to all handle drags.

3. **Resizing from Any Handle or Edge**:
   - When the user places their finger anywhere within the generous 48dp touch radius of any corner handle or anywhere along the 4 edges of the shape, resizing immediately activates.
   - Moving the finger scales the shape smoothly in real time according to the active anchor mode.
   - Center mode keeps the center stationary and adjusts `positionX/Y` and `width/height`.
   - Opposite Corner mode anchors the opposite corner while tracking the finger.
   - Releasing the gesture commits the transformation cleanly to the undo/redo history stack.

---

## 3. Key Technical Decisions & Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│               Floating Shape Transform HUD                      │
│                                                                 │
│  [ W: 240px ] [ H: 180px ] [ 🔓 Lock ] [ 🎯 Anchor: Center ▾ ]  │
└───────────────────────────────────────────────┬─────────────────┘
                                                │ Tap Badge
                                                ▼
┌─────────────────────────────────────────────────────────────────┐
│                   Anchor Mode Dropdown Menu                     │
│  ├── 🎯 Center Symmetrical (Outward from center)                │
│  ├── 📐 Opposite Corner (Standard Photoshop/Figma anchor)       │
│  ├── 📌 Fixed Origin (Top-Left corner stationary)               │
│  └── ── Divider ──                                              │
│  └── 🔒 Proportional Scaling (Aspect Lock)                      │
└─────────────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────────────┐
│              Layer-Local Hit Detection Pipeline                 │
│                                                                 │
│   Touch Down (down.position)                                    │
│      │                                                          │
│      ▼                                                          │
│   screenToCanvas() -> Canvas Artboard Space                     │
│      │                                                          │
│      ▼                                                          │
│   canvasToLayerLocal() -> Layer Coordinate Space (0..W, 0..H)   │
│      │                                                          │
│      ├── Distance to TL (0, 0) < 48dp?          -> resize_tl    │
│      ├── Distance to TR (W, 0) < 48dp?          -> resize_tr    │
│      ├── Distance to BL (0, H) < 48dp?          -> resize_bl    │
│      ├── Distance to BR (W, H) < 48dp?          -> resize_br    │
│      ├── Proximity to Top / Bottom / Left /     -> resize_t/b/  │
│      │   Right edges within 36dp band?             l/r          │
│      └── Touch strictly inside interior?        -> "move"       │
└─────────────────────────────────────────────────────────────────┘
```

### Technical Details & Implementation Plan
1. **Layer-Local Hit Detection**:
   - In `WorkspaceScreen.kt` pointer input gesture loop: convert the start touch directly into layer-local space using `canvasToLayerLocal(artboardRelativeTouch, currentSelected)`.
   - Define handle touch hit radii in screen dp (e.g. 48dp converted via `(48f * density) / totalScale`), making touch zones physically generous regardless of canvas zoom level.
   - Check the 4 corner handles first, then the 4 edge centers and border segments.
   - Prioritize resizing so that any touch near the perimeter triggers `resize_*` instead of `move`.

2. **Anchor Mode State & Drag Transformations**:
   - Add state `shapeAnchorMode` (`"CENTER"`, `"OPPOSITE"`, `"ORIGIN"`).
   - In the resize drag loop (`dragMode.startsWith("resize")`):
     - **"CENTER"**: Compute target width and height as `2 * |localTouch - pivot|`, adjusting both size and top-left origin symmetrically.
     - **"OPPOSITE"**: Anchor the opposite corner (e.g. for `resize_br`, anchor `(0, 0)` and expand `(width, height)` to `(localTouch.x, localTouch.y)`; for `resize_tl`, anchor `(startW, startH)` and adjust position and size).
     - **"ORIGIN"**: Anchor `(0, 0)` so `positionX/Y` remain fixed while `width` and `height` adjust.
     - Honor `isAspectLocked` across all modes.

3. **Interactive Dropdown Badge UI**:
   - Replace the static badge in the Floating Shape Transform HUD with an interactive `Box` + `DropdownMenu`.
   - Display a dropdown trigger button with icon, current mode title, and dropdown arrow.
   - Provide clear menu items with icons and check indicators for the selected mode.

---

## 4. Verification & Testing Plan

1. **Compilation Check**:
   - Run `compile_applet` to verify syntax and type correctness.
2. **Touch Registration Verification**:
   - Spawn a Square, Circle, or Oval vector shape.
   - Verify that touching any of the 4 corner handles immediately engages corner resize.
   - Verify that touching the top, bottom, left, or right border edges immediately engages edge resize without whole-shape moving.
   - Verify that touching the inner center moves the shape as expected.
3. **Dropdown & Anchor Mode Verification**:
   - Tap the Anchor badge on the HUD; verify that the dropdown menu appears smoothly.
   - Select "Opposite Corner": drag the bottom-right handle and confirm the top-left corner stays pinned in place.
   - Select "Fixed Origin": verify top-left remains stationary.
   - Select "Center Symmetrical": verify shape scales symmetrically from the center.
   - Toggle Aspect Ratio Lock and confirm proportional scaling works in all anchor modes.
