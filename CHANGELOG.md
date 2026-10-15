# Changelog

## 0.2.6 — 2026-10-13

Bilingual naming and in-game language switching.

### Changed

- Updated the product naming:
  - Chinese name: **3D连连看**
  - English name: **linklink**.
- The start/setup menu now defaults to Chinese and provides a one-tap language switch:
  - Chinese: `切换语言 · English`
  - English: `Switch Language · 中文`.
- Added complete English resources for setup, modes, rules, tools, notices, timed state, win/fail panels, and pairing summaries.
- Localized runtime-generated text for the emulator fallback menu, combo cards, timed status, and win voice.
- Kept the existing spatial-depth icon and gameplay behavior unchanged.

### Android version

- `versionCode: 8`
- `versionName: 0.2.6-bilingual`

## 0.2.5 — 2026-09-29

Spatial-depth icon revision.

### Changed

- Reworked the icon from a flat side-by-side pair into a depth-first composition:
  - a stronger one-point ground grid and horizon glow
  - a smaller, hazier cube in the background
  - a larger, brighter cube in the foreground
  - contact shadows and atmospheric perspective
  - a tapered gold star-link traveling through depth.
- Removed the scattered star-field dots and planetary orbit cue so the first read is spatial/XR volume rather than outer space.
- Kept the four-point star as the game's match/spark symbol without covering either cube.
- Regenerated the launcher, adaptive foreground, PICO 3D layers, and SDF masks.

### Android version

- `versionCode: 7`
- `versionName: 0.2.5-spatial-depth-icon`

## 0.2.4 — 2026-09-29

Star-link icon refresh and Feel-mode hold-to-orbit.

### Changed

- Replaced the busy translucent brand mark with a cleaner icon:
  - two identical solid ice-blue cubes
  - one bold gold star link above them
  - a simpler black cosmic background
  - stronger contrast and a clearer silhouette at small launcher sizes.
- Regenerated the standard launcher icon, adaptive foreground, PICO 3D layers, and matching SDF masks from the new master.
- Feel mode now keeps a short pinch as card selection, while holding the same pinch for about 320ms enables two-axis game-body rotation.
- Board rotation is suppressed once both hands hold cards, preserving the two-card matching gesture.

### Android version

- `versionCode: 6`
- `versionName: 0.2.4-star-link-icon`

## 0.2.3 — 2026-09-29

Full-immersion and interaction stabilization.

### Changed

- Kept the validated 14-meter omnidirectional cosmos sphere with no culling and depth writing enabled.
- Expanded the Stage keep-alive ring from 6 to 12 nearly invisible anchors, covering every 30° of yaw so the Full Stage and 3D cosmos remain alive when looking in any direction.
- Replaced the unstable setup-panel position experiment with the official Spatial SDK drag gesture:
  - uses SDK-provided `dragAmount`
  - moves only in X/Y
  - deliberately suppresses Z-depth jitter that caused the panel to flash forward/backward.
- Restored stable X/Y movement for the tool rail from UI-space pointer deltas rather than raw input-device coordinates.
- Feel-mode card dragging no longer moves or rotates the whole board; held cards follow their own hand anchors.
- Feel-mode left/right assignment no longer compares incompatible `inputDevicePose` and hand-tracking coordinate spaces.
- A wrong Feel-mode card now requires sustained motion to flick-release, preventing near-stationary false releases.
- Hand-anchor updates and card attraction continue while either hand is visible, improving one-handed tracking stability.

### Android version

- `versionCode: 5`
- `versionName: 0.2.3-full-immersion-panel`

### Verification

- `./gradlew testDebugUnitTest assembleDebug` passed.
- Installed and launched on `PB325XKGL4200027B` as process 9379 with no startup fatal.
- Runtime confirmed the setup-panel gesture bound to a valid entity.
- Final real-device 360°/dragging acceptance remains with the wearer.

## 0.2.2 — 2026-09-28

Brand identity polish.

### Changed

- Application display name changed from `linklin` to **星连立方**.
- Replaced the generic blue/purple stacked-panel icon with a custom cosmic brand mark:
  - black universe badge
  - blue and mint 3D cubes
  - golden star-link arc
- Updated the standard launcher icon, adaptive foreground, and PICO 3D icon layers.
- Regenerated the foreground layer SDF mask to match the new cube/link motif.

### Android version

- `versionCode: 4`
- `versionName: 0.2.2-brand-icon`

## 0.2.1 — 2026-09-25

Position and orientation polish.

### Changed

- Real-hardware gameplay sound now uses game audio routing instead of assistance sonification.
- Replaced the card-select pop with a cleaner fixed-pitch soft tap.
- Replaced the UI click sound with a neutral short tick.
- Initial setup, in-game menu, and the right-side tool stack support direct UI dragging.
- Setup and tool panels support two-hand dragging: average movement translates the panel while the hand-to-hand line adjusts yaw and pitch.
- The game board keeps stable DirectPinch translation and ray/gaze rotation without hold-to-switch jumping.
- Restored the compact four-button tool layout after removing the temporary drag handle.

### Android version

- `versionCode: 3`
- `versionName: 0.2.1-position-polish`

## 0.2.0 — 2026-09-25

Effects polish: **core feedback, Lianliankan path cues, and ambient rhythm**.

### Stage 1 — Core Feel feedback

- Two-card proximity energy link with 18 pulsing star segments.
- Two-card contact star-dust impact.
- Successful pairs converge into a star burst.
- Invalid pairs softly repel while remaining attached for replacement.
- Card-select pop sound.
- Tool hover and click sounds.

### Stage 2 — Lianliankan feedback

- Successful matches draw the actual solver path as a glowing star-dot polyline.
- Hints draw a longer-lived blue guide path.
- Shuffle flights produce colored orbit trails.
- Shuffle plays a whoosh and staggered card-landing sounds.

### Stage 3 — Ambience and rhythm

- Cosmos background slowly rotates.
- Match-success pitch rises with combo streaks.
- Transient floating “连击 ×N” cards appear from combo 2 onward.
- Timed capsule changes from calm blue to warning gold to pulsing coral.
- Win celebration begins with a constellation animation before cannons and fireworks.

### Verification

- `./gradlew testDebugUnitTest assembleDebug` passed.
- SpatialUI design-style verifier passed with 0 errors and 0 warnings.
- Temporary forced 4×4×4 runtime created the board, all 210 gameplay particles, and all 18 energy-link entities without a fatal log.
- Forced startup was removed; the production APK launched in the emulator as process 20501 with `MainStage` visible.
- Real-device effect tuning remains pending headset reconnection.

### Release artifact

- APK: `releases/linklin-0.2.0-effects-polish.apk`

## 0.1.0 — 2026-09-24

Baseline snapshot: **Full-space 3D Lianliankan MVP with two-card-touch Feel mode**.

### Included

- Full-space `DefaultStage` with black cosmos environment.
- Casual, Feel, and Timed Challenge modes.
- 3D animal-card Lianliankan board with 6-direction, up-to-3-turn path matching.
- 3×3×3, 4×4×4, 5×5×5, unlimited custom dimensions, and next-level progression.
- Hints, shuffle, restart, win celebration, timed failure flow, match sounds, and TTS.
- Feel mode: tap-to-stick one card per hand, soft proximity magnetism, and two-card contact matching.
- Draggable initial setup sheet, draggable right-side tool stack, and DirectPinch board translation.
- Gaze/ray board rotation and controller-compatible tap interactions.

### Verification

- `./gradlew testDebugUnitTest assembleDebug` passed.
- SpatialUI design-style verifier passed with 0 errors.
- Emulator launch passed with `MainStage` visible and no fatal app log.
- Real-device validation for the final two-card-contact gesture is pending because the headset was disconnected.

### Release artifact

- APK: `releases/linklin-0.1.0-feel-two-card-touch.apk`
- Full checksummed snapshot: `versions/linklin-0.1.0-feel-two-card-touch-full.tar.gz`
- Snapshot SHA256: `d6eb53afc5977044d8d056c0bc663093132fe176fbf4170fdddd122957dccfb8`
- APK SHA256: `a2ef8377d9e0238dd668d67175f0d439e501fc7eaf019c80cb6a03864bf6a183`
