#!/usr/bin/env bash

adb logcat -c
adb logcat *:E -v color &
if ./gradlew jacocoInstrumentationTestReport; then
  echo "jacocoInstrumentationTestReport succeeded" >&2
else
  adb exec-out screencap -p >screencap.png
  echo "jacocoInstrumentationTestReport failed" >&2
fi
