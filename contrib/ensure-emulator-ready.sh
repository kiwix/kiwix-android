#!/bin/bash
set -e

adb devices

# Check if device is offline
if adb get-state 2>&1 | grep -q "offline"; then
  echo "ADB is offline. Restarting ADB..."
  adb kill-server || true
  adb start-server || true
fi

adb wait-for-device

for i in {1..60}; do
  BOOT=$(adb shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')
  if [ "$BOOT" = "1" ]; then
    echo "Emulator boot completed"
    break
  fi
  echo "Waiting for boot... ($i)"
  sleep 5
done

adb shell input keyevent 82 || true
adb devices
bash "$1"
