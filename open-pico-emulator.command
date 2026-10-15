#!/bin/bash
cd /Users/bytedance/Documents/Codex/2026-09-16/planar-spatial-sdk-x20-myspatialapp-com || exit 1
export JAVA_HOME=/Users/bytedance/.pico/primer-cli/jdk/jdk-21.0.12.1+1/Contents/Home
export PATH="$JAVA_HOME/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
echo "Starting PICO emulator with --wipe-data --watch..."
pico-cli emulator start --wipe-data --watch
status=$?
echo "pico-cli exited with status $status"
if [ "$status" -ne 0 ]; then
  echo "Press any key to close this window..."
  read -n 1 -s
fi
exit $status
