# linklin version 0.2.0

Version name: `effects-polish`

Saved on 2026-09-25.

## Files

- `linklin-0.2.0-effects-polish-full.tar.gz`
  - Full source/config snapshot and release APK.
- `../releases/linklin-0.2.0-effects-polish.apk`
  - Directly installable debug APK.
- `linklin-0.2.0.SHA256`
  - SHA256 checksums.

## Restore

```bash
mkdir -p restore-0.2.0
tar -xzf versions/linklin-0.2.0-effects-polish-full.tar.gz -C restore-0.2.0
```

Install the saved APK directly:

```bash
pico-cli app install --device <device-id> \
  releases/linklin-0.2.0-effects-polish.apk
```
