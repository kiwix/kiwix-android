#!/usr/bin/env bash

adb logcat -c
adb logcat *:E -v color &
if ./gradlew connectedDebugAndroidTest; then
  echo "connectedDebugAndroidTest succeeded" >&2
else
  adb exec-out screencap -p >screencap.png
  echo "connectedDebugAndroidTest failed" >&2
fi
