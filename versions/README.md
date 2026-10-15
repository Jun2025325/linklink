# linklin version 0.1.0

Version name: `feel-two-card-touch`

Saved on 2026-09-24.

## Files

- `linklin-0.1.0-feel-two-card-touch-full.tar.gz`
  - Full source/config snapshot and the release APK.
- `../releases/linklin-0.1.0-feel-two-card-touch.apk`
  - Directly installable debug APK.
- `linklin-0.1.0.SHA256`
  - SHA256 checksums.

## Restore

From the project root:

```bash
mkdir -p restore-test
tar -xzf versions/linklin-0.1.0-feel-two-card-touch-full.tar.gz -C restore-test
```

Or install the saved APK directly:

```bash
pico-cli app install --device <device-id> \
  releases/linklin-0.1.0-feel-two-card-touch.apk
```

## Why this is not a Git tag

This workspace is under a host sandbox that denied creating `.git`. The version is therefore preserved as a checksummed source-plus-APK snapshot.
