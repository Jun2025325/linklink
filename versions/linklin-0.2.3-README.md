# 星连立方 version 0.2.3

Version name: `full-immersion-panel`

Saved on 2026-09-29.

## Files

- `linklin-0.2.3-full-immersion-panel-full.tar.gz`
  - Full source/config snapshot and this release APK.
- `../releases/linklin-0.2.3-full-immersion-panel.apk`
  - Directly installable debug APK.
- `linklin-0.2.3.SHA256`
  - SHA256 checksums for the full snapshot and APK.

## Android version

- `versionCode: 5`
- `versionName: 0.2.3-full-immersion-panel`

## Version focus

- 14-meter omnidirectional cosmos sphere.
- Twelve-point, 360° Full Stage keep-alive ring.
- Official Spatial SDK drag handling for the setup panel.
- Setup-panel X/Y movement with Z-depth jitter suppressed.
- Stable right-side tool-rail dragging.
- Feel-mode board/card interaction separation and sustained flick release.

## Restore

From the project root:

```bash
mkdir -p restore-0.2.3
tar -xzf versions/linklin-0.2.3-full-immersion-panel-full.tar.gz \
  -C restore-0.2.3
```

Install the saved APK directly:

```bash
pico-cli app install --device <device-id> \
  releases/linklin-0.2.3-full-immersion-panel.apk
```
