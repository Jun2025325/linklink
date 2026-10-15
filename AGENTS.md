## Plugin Context

Also read `./PICO-SPATIAL-AGENTIC-TOOLS.AGENTS.md` for PICO Spatial plugin guidance.

# linklin — PICO Spatial SDK (Full-space Stage)

## What this project is
A PICO Spatial Android/Kotlin app (SDK 6.1.9) using a **Full-space DefaultStage** (`MainStage`,
StageStyle.Full, immersion 100%). The original 960dp Volumetric Window Container was replaced so a
large inward-facing cosmos cube can fill the complete view instead of being framed by the system
window background. A full-size opaque Compose starfield safety layer also sits beneath HomeVolume;
unlike the 3D enclosure it is independent of viewport zoom/dolly, preventing PICO's environment from
appearing when the emulator view is magnified or reduced. It runs a
**3D 连连看 (Lianliankan) MVP**: a product mode layer is now started
(`CASUAL` / `FEEL` / `TIMED`, with Casual, Feel, and Timed Challenge implemented).
Casual defaults to gaze targeting plus `InteractionKind.GazePinch` confirmation. Feel mode keeps
pinch/drag assigned to board orbiting. A short DirectPinch/GazePinch/RayBasedPinch/Poke tap assigns
the hit card to the nearest tracked left or right hand; `HandTrackingProvider` continuously converts
that hand's pinch/palm anchor into rotated board-local space and springs the card to it. Tapping
another card with the same hand replaces the wrong card. When both sticky cards are held and the
tweaked card positions enter the card-contact radius, soft magnetism first brings them together;
`LianliankanGame.match(first, second)` then judges the explicit pair. A legal pair converges and
disappears, while an invalid touch plays failure feedback and keeps the cards attached for
replacement. Games start from a setup panel offering 3×3×3, 4×4×4, 5×5×5,
or custom dimensions with no upper limit; odd boards reserve one empty cell so all remaining
cards pair.
That reserved odd-board cell is placed at the innermost median cell (for example Cell(1,1,1) in
3×3×3), so every exterior face remains visually complete while the empty cell stays usable as
path space.
Timed Challenge uses the Casual tap interaction, pauses its countdown while the setup sheet is open,
and computes duration as `90s + 16s per pair`, rounded to 30 seconds with a three-minute floor
(3×3×3=5:00, 4×4×4=10:00, 5×5×5=18:00). A cosmic status capsule shows remaining time and pairs;
when time expires, the board/tools hide and a matching failure card offers retry or menu.
Cards use 64 animal/color identities, safely reusing identities in complete pairs on larger boards.
Each card is one cube with one complete opaque texture that
bakes the background color and animal emoji together, so all six faces stay stable while
the board rotates. A large inward-facing, star-textured black cosmos cube fills the Full Stage; front-face culling
lets its six interior faces act as a skybox without occluding the board or tools. Stage gameplay content is placed at a
comfortable viewing offset in front of the user. The game keeps casual untimed mode, relaxed 6-direction <=3-turn 3D path matching,
ray-targeted tap selection, whole-card selection glow/scale, scale-down removal
animation, two-axis drag-to-rotate (yaw and pitch), six-face stickers for viewing from
any side, and a Full-Space large-board layout that preserves a 0.058m playable card floor. Preset
cubes keep the authored spacing; edges above five use tighter spacing and grow outward rather than
shrinking cards into the retired 960dp window. Three per-game hints that highlight a connectable pair,
a remaining-tile shuffle with staggered 3D flight/spin animation, and give-up/restart.
The setup/rules panel is foreground on first launch and is collapsed after starting. Real hardware
uses the full SpatialUI AttachmentPanel setup sheet; the emulator (`Build.HARDWARE=ranchu`) keeps the
resilient Direct 3D fallback menu. Hardware uses the opposite board/tool depth from the emulator so
a new board starts in front of the player rather than behind them. During play, all persistent
controls form one readable right-side cosmos stack: Menu, Hint, Shuffle, and Give Up. The tool
surface keeps its authored front face, includes a persistent drag handle, and uses enlarged
112×48dp capsule actions. Dragging that handle moves the complete tool stack. A DirectPinch board
drag moves the board in clamped stage space; GazePinch/ray board drag orbits it.
There is deliberately no outer panel/backplate; each action is an independent dark
glass capsule with colored icon accents, so the control area has no empty lower strip. The
expanded setup sheet stays open while changing
difficulty or mode and applies the selection as a new game. Both the first-launch sheet and the
in-game expanded sheet default to a comfortable centered offset that keeps the exit button inside the
view, can be dragged and clamped within a comfortable stage volume, retain one shared position during the session, and expose a coral
“退出游戏” action that closes MainStage through SpatialUI `closeStage()`. The panel position is shared for the
initial sheet and reopened in-game menu. The setup/rules sheet is a compact, nearly opaque cosmic panel with a dark depth plate,
starlight border, dark option cards, colored selected borders, emoji mode icons, one-line rules,
an orange primary action, and a dark coral exit action. Opening the in-game menu brings the sheet
to z=-0.56, hides the board and control capsules, and disables board/control recognizers until it
is closed, preventing click-through and system-environment visibility. Winning triggers a detached 3D celebration with party cannons,
confetti, multiple firework bursts, a compact opaque cosmic win panel, SoundPool effects, and Chinese
text-to-speech “恭喜成功！”. While the win card is active, the board root is hidden so cleared cubes
cannot cover the next-level/menu actions. Correct and incorrect matches play dedicated low-latency SoundPool effects from
`res/raw/match_success.wav` and `res/raw/match_failure.wav`.
Gameplay now has a three-layer effects system: Feel-mode cards show a proximity energy link, contact
star-dust, success convergence, and invalid soft repulsion; matches and hints draw reusable star-dot
paths; shuffle flights leave orbit trails and play whoosh/landing cues. The cosmos slowly rotates,
combos raise match pitch and show a transient “连击” card, timed state changes from calm blue to
warning gold to pulsing coral, and wins begin with a constellation before fireworks.
Clearing every pair shows a centered “恭喜通关” card, plays the finite 3D celebration, hides the
in-game controls, and after the celebration reveals two actions: `下一关 <N+1>×<N+1>×<N+1>` and
`返回菜单`. Level progression increments all three dimensions without an upper bound.
The UI uses macaron rounded-square
buttons (cream yellow / sky blue / mint green /
coral pink), unified vector icons, deep-grey text, soft radial shadows, and a bottom-aligned row.
Card mesh/material/texture resources are persisted with `toGlobal()` and explicitly closed
in `GameSceneState.destroy()` because selection updates reuse them across many components.

## Key files
- `app/src/main/java/com/linklink/myspatial/Main.kt` — app entry; `DefaultStage { LinklinTheme { HomeVolume() } }`
- `app/src/main/java/com/linklink/myspatial/content/HomeVolume.kt` — 3D scene: dynamic single-cube textured boards, setup/rules menu, collider+interactable+hover effect, gaze-pinch selection, hand-tracked tap-to-stick/two-card-touch feel mode, board translation/orbit drag, hint/match star paths, Feel energy links and star dust, shuffle trails, combo/timed visuals, compact menu button, persistent tool stack, celebration/constellation panels, measurement-driven `BoardLayout`
- `app/src/main/java/com/linklink/myspatial/game/LianliankanGame.kt` — pure game logic: dynamic board generation with odd-board blank handling, tap state machine, explicit two-card matching for Feel mode, timed-duration formula, 6-direction <=3-turn path solver with 3D outer-shell walking, hint quota, remaining-tile shuffle, win detection
- `app/src/test/java/com/linklink/myspatial/game/LianliankanGameTest.kt` — 3D solver/rule unit tests (straight/diagonal/outer-shell paths, turn rejection, tap/explicit matching/timed durations, board generation for 4×4×4/odd/non-cubic boards, hint quota, shuffle preservation/playability)
- `app/src/main/java/com/linklink/myspatial/platform/LaunchActivity.kt` — launcher activity (`SpatialLaunchActivity`)
- `app/src/main/java/com/linklink/myspatial/platform/SpatialApplication.kt` — application; `launch(::mainApp)`
- `app/src/main/AndroidManifest.xml` — Full-space DefaultStage meta-data configuration (`MainStage`, StageStyle.Full)
- `gradle/libs.versions.toml` — versions (Spatial BOM 6.1.9, AGP 8.13.2, Kotlin 2.1.20)

## Hard UI rule
All 2D UI must use **SpatialUI** only: components from `com.pico.spatial.ui.design.*`,
root wrapped in `PicoTheme`, colors/typography via `PicoTheme.colorScheme` / `PicoTheme.typography`.
**No `androidx.compose.material`, Material3, `MaterialTheme`, or `Scaffold`.**

## Build / install / run
```bash
# Build (JDK 21 is bundled with pico-cli)
JAVA_HOME=/Users/bytedance/.pico/primer-cli/jdk/jdk-21.0.12.1+1/Contents/Home ./gradlew assembleDebug

# Emulator with auto-reconnect (restarts on exit / adb loss, checks every 5s)
pico-cli emulator start --watch

# Install & launch
pico-cli app install app/build/outputs/apk/debug/app-debug.apk
pico-cli app launch com.linklink.myspatial --activity com.linklink.myspatial.platform.LaunchActivity
```

## Verification notes
- `./gradlew testDebugUnitTest assembleDebug` passes (solver + rules tested on JVM).
- Emulator: AVD `Pico_Emulator`, bundle 6.1.0, adb target `emulator-5554`.
- Runtime evidence: `outputs/linklin-single-cube-final.png` shows the full 4x4x4 board with
  complete single-cube textures, readable animals, and no ANR, missing textures, or clipping.
- Click-stability evidence: a temporary startup stress loop toggled one card's highlight 20
  times without closing the material or crashing; the production build omits that test loop.
- Casual-mode evidence: `outputs/linklin-casual-gaze-final.png` shows the mode label
  "休闲模式 · 眼神捏合", remaining pairs, rotation hint, and the complete board. Runtime
  interaction decisions are logged under the `LinklinInteraction` tag.
- Floating-control evidence: `outputs/linklin-3d-controls-final.png` shows four detached,
  colored 3D rounded-cube controls ("提示 3" gold, "洗牌" sky blue, "放弃" coral red, and
  "休闲/手感" mint green) around the board without clipping.
- Draggable-control evidence: `outputs/linklin-draggable-3d-controls.png` shows the production
  controls after adding spatial drag, volume clamping, and SharedPreferences placement
  persistence. `controlDragStart` / `controlDragEnd` logs identify real-device rearrangement.
- Status-glass evidence: `outputs/linklin-status-glass-final.png` shows the translucent rounded
  panel, readable white shadowed text, and visible back-plate thickness without clipping.
- UI-polish evidence: `outputs/linklin-ui-polish-final.png` shows the refined dark status glass,
  unified vector icon controls, larger labels, and muted board/control palette.
- Cartoon-UI evidence: `outputs/linklin-cartoon-ui-final.png` shows the final macaron rounded-square
  bottom control row, hint-count badge, white glass status panel, soft shadows, and unified cartoon
  style without candy highlights.
- Gameplay-UI fix evidence: `outputs/linklin-gameplay-ui-fix.png` shows the Casual default and
  readable control row. Runtime logs distinguish `controlTap`, `controlDragEnd`, and `spatialTap`;
  hint cards use a dedicated outlined texture and lift strongly out of the board.
- Control/audio fix: floating controls have a dedicated `controlSpatialTap` recognizer, so short
  presses invoke actions even when the drag recognizer sees no movement. The two match WAVs are
  packaged and played through SoundPool.
- Feel-mode evidence: `outputs/linklin-feel-mode-active.png` shows the active label
  "手感模式 · 伸手抓取", instruction "伸手捏住卡片 · 拖到另一张", and a floating control to
  switch back to Casual. Direct grab feel requires a real PICO device.
- Feel-polish evidence: `outputs/linklin-feel-mode-magnet-haptics.png` shows the same active
  mode after adding target magnetism, target highlighting, haptics, and two-hand logging.
- Two-hand evidence: a temporary startup smoke test exercised both grab states and the converge/
  remove animation without crashing; `outputs/linklin-two-hand-mode.png` shows the production
  build after the smoke test was removed. Real two-hand collision requires a PICO device.
- Feel-clap v2 evidence: real-device logs showed short interactions surfacing as `GazePinch`, so
  Feel taps now accept GazePinch/RayBasedPinch/DirectPinch/Poke while pinch drags rotate the board.
  The build adds `HandTrackingProvider`, left/right sticky-card anchors, tap replacement, and
  explicit clap matching. `testDebugUnitTest assembleDebug` passes 19 tests; the APK was installed
  and relaunched on `PB325XKGL4200027B`, where process 17110 stayed alive with no fatal app log.
- Feel-hand assignment fix: later real-device logs showed `handTrackingStart SUCCESS`, but ambiguous
  GazePinch taps repeatedly resolved to RIGHT and replaced the same hand's card. Feel-mode hand
  selection now scores actual left/right pinch strength and aim data, prefers the single visible
  hand, and assigns an ambiguous second card to the free hand. `feelHandSignal` logs the raw
  left/right pinch, aim, visibility, and occupancy for continued device diagnosis. Final
  `testDebugUnitTest assembleDebug` passes and the APK was installed/launched on
  `PB325XKGL4200027B`.
- Feel two-card-touch evidence: the explicit hand-clap requirement was replaced by a card-to-card
  confirmation gesture. Both held cards now use their spring-follow current positions; below
  `cardSize × 1.15` they contact and judge the pair, while within `cardSize × 2.10` a soft magnet,
  proximity scale-up, and target pull make the action readable without forcing palms together.
  Replacement re-arms contact when the held pair changes or the cards separate again. Chinese setup
  and active-mode strings now say “左右各拿一张 · 双卡相碰”. `testDebugUnitTest assembleDebug` passes;
  the APK was installed and launched in the emulator as process 12708 with MainStage visible and no
  fatal log. Real-hand validation is pending headset reconnection.
- Setup-panel v2 evidence: the setup sheet defaults to an upper-right, farther offset; has a visible
  “⠿ 可拖动” hint; supports spatial drag in both initial and in-game states; and includes
  “退出游戏”. The first screenshot iteration clipped the exit button, so the default and drag clamp
  were tuned to x=0.18/y=0.96/z=-0.95. OCR inspection of
  `outputs/linklin-draggable-setup-exit-adjusted-2.png` confirms the complete panel, title, drag
  hint, start action, and exit action with no viewport clipping. The SpatialUI verifier passes with
  0 errors. `testDebugUnitTest assembleDebug` passes. The final APK is installed/running in the
  emulator; the real headset later disappeared from both ADB and the macOS USB registry, so final
  real-device reinstall is pending USB reconnection.
- Unlimited-custom/level evidence: the custom stepper now has a lower bound of 2 but no upper bound,
  and its setup card says “边长不设上限”. Unit tests cover 6×6×6 (108 pairs) and the next odd
  7×7×7 level. A temporary production-equivalent 6×6×6 runtime build created the full textured
  board in the emulator (`outputs/linklin-6x6x6-runtime-smoke-2.png`) without crashing; temporary
  forced-startup tests were removed. Win actions were also exercised with a temporary forced-win
  smoke build, then restored so wins require `game.isCleared`. Final `testDebugUnitTest
  assembleDebug` passes 21 tests and the SpatialUI verifier reports 0 errors.
- Large-custom-layout evidence: after enabling unlimited dimensions, the old fixed-volume fit made
  cards too small to point at. `BoardLayout` now enforces `MIN_PLAYABLE_CARD_SIZE = 0.058m`, uses a
  tighter 0.030m gap for edges above five, and stops shrinking the Full-Space root. A temporary
  6×6×6 startup build produced clearly readable animal cubes in
  `outputs/linklin-large-layout-6x6x6-5.png`; the forced startup board was then removed. Final
  `testDebugUnitTest assembleDebug` passes and the production APK was installed/launched in the
  emulator as process 5370 with no fatal app log.
- Cosmos-control-rail evidence: the first dark-glass panel still left a visible empty lower strip, so
  the outer rail material/border was removed entirely. A temporary 4×4×4 startup build produced
  `outputs/linklin-cosmos-control-capsules-2.png`; OCR confirmed four independent complete capsules
  (“菜单 / 提示 3 / 洗牌 / 放弃”) with no outer rectangle, no bottom blank strip, no overlap, and no
  clipping. Because the rotating board could still cover the original right-side position, the stack
  was moved to x=0.300/y=0.820/z=-0.620: closer to the player and above the rotated-board ray while
  retaining a visible right-edge margin. `outputs/linklin-control-position-4x4x4-3.png` confirms a
  clear starfield gap between the board and controls. Temporary startup games were removed.
  `testDebugUnitTest assembleDebug` and the SpatialUI verifier pass; the production APK launched in
  the emulator as process 12277 with no fatal app log.
- Cosmic-menu foreground evidence: clicking Menu previously exposed the board through a translucent
  white sheet. The sheet is now dark/opaque and themed like the cosmos controls. A temporary
  startup build with a 4×4×4 board and `menuOpen=true` produced
  `outputs/linklin-cosmic-menu-foreground-14.png`; OCR confirmed the complete title, difficulty and
  mode sections, start/exit actions, “收起面板”, all four panel rounded borders, no board or system
  interior scene behind it, and no visible control capsules. The remaining old cartoon bottom
  strips were then removed from every setup/action card; visual inspection of
  `outputs/linklin-cosmic-menu-no-button-lines.png` confirms clean rounded cosmic cards and pills
  with no horizontal decoration lines. Temporary forced menu state was removed.
  `testDebugUnitTest assembleDebug` and the SpatialUI verifier pass; the latest production APK
  launched in the emulator as process 22783 without a fatal app log.
- Stage-reentry fix evidence: after `finishAndRemoveTask()`, a re-entered emulator task could keep system
  scenario content or only the screen-space starfield while `SpatialView` AttachmentPanel entities stayed
  null. SDK guidance and sample code prescribe `closeStage()` for Full Stage cleanup. `exitGame` now calls
  `LocalSpatialNavigator.closeStage()` instead of finishing/removing the Android Activity, and initial/game
  SpatialViews retry late AttachmentPanel registration. Debug attachment logs and the temporary TEST panel
  were removed, and the original AttachmentPanel mount flow was restored. Tests/build and the SpatialUI
  verifier pass. The AVD retained a corrupt Stage/window-compositor state across a normal restart:
  UIAutomator could see the complete setup hierarchy while screencap showed only the cosmos. A
  `pico-cli emulator start --wipe-data --watch` restart is still required for visual re-verification.
- Timed-challenge evidence: the former locked HUNT slot is now selectable “限时挑战”. The setup sheet
  shows the dynamic duration (`outputs/linklin-timed-challenge-menu.png`, OCR confirmed 4×4×4 · 32
  对 · 10:00), and a temporary short-duration runtime build exercised timeout. OCR confirmed the
  failure card “时间到 / 挑战失败 · 还有 32 对未消除 / 重新挑战 / 返回菜单” with the board and tools
  hidden (`outputs/linklin-timed-challenge-fail-3.png`). A combined status/fail AttachmentPanel now
  shows remaining time/pairs during play and switches to failure on timeout. Temporary forced mode,
  short duration, and debug state logs were removed. Final unit/build verification passes 22 tests
  and the SpatialUI verifier reports 0 errors; production launched as process 8643 without a fatal
  app log.
- Cosmic-win-panel evidence: the old bright/golden win card was redesigned to match the cosmos UI:
  near-opaque `#050A16`, starlight border, gold star markers, clean white title, muted subtitle, an
  orange next-level action, and a dark return-menu capsule. A temporary forced-win smoke build
  produced `outputs/linklin-cosmic-win-panel-centered.png`; OCR confirmed complete “恭喜通关 /
  全部消除 · 干得漂亮 / 下一关 5×5×5 / 返回菜单” text with no clipping or cube occlusion. The
  forced-win state was removed afterward. Final `testDebugUnitTest assembleDebug` and the SpatialUI
  verifier pass; the production APK launched as process 26033 without a fatal app log.
- Real-hardware setup/control path: the initial Direct 3D menu was visible in the emulator but the
  player reported no game on `PB325XKGL4200027B` even though MainStage rendered at 90 FPS. Initial
  setup now branches by hardware: real devices use the full SpatialUI AttachmentPanel sheet at a
  clamped comfortable offset, while `ranchu` keeps Direct 3D. The real-device control rail was also
  enlarged from an 0.086×0.184m panel with 72×33dp actions to 0.140×0.230m with 112×48dp actions,
  16sp labels, and 20dp icons. A later real-device session showed that positive-Z gameplay content
  was behind the player; hardware now mirrors board/tool depth to negative Z, keeps the tool panel's
  front face, and supports DirectPinch board translation plus draggable board/tool/setup surfaces.
  Tests/build and the SpatialUI verifier pass, and the move-mode APK was installed and launched on
  `PB325XKGL4200027B` as process 10001.
- Effects-polish evidence: all three requested effect stages are implemented. Core Feel feedback
  adds an 18-segment energy link, contact star-dust, successful pair convergence into a star burst,
  invalid-card soft repulsion, card-select pops, and UI hover/click sounds. Lianliankan feedback
  draws solver paths, hint guides, shuffle trails, and plays whoosh/landing sounds. Ambience slowly
  rotates the cosmos, raises success pitch and shows transient combo cards with streak tracking,
  colors/pulses the timed capsule by remaining ratio, and precedes fireworks with a constellation.
  A temporary forced 4×4×4 build created all 210 gameplay particles and 18 energy-link entities
  without a fatal log; forced startup was removed. Final `testDebugUnitTest assembleDebug` and the
  SpatialUI verifier pass, and production launched in the emulator as process 20501.
- Rotation evidence: a temporary 60-degree pitch build stayed fully inside the view and
  container; the production initial pitch is 0 degrees.
- Tap interaction requires controller/gaze input; 2D adb taps are NOT valid for volumetric
  containers — verify interaction manually in the emulator.

## Natural next steps
- Tune effect particle counts and colors from real-device feedback
- Shuffle-when-stuck and score/streak UI
- Add a persistent combo/score panel if transient combo cards feel too subtle
