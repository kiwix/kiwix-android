#!/usr/bin/env bash

adb logcat -c
adb logcat *:E -v color &
./gradlew connectedDebugAndroidTest
adb exec-out screencap -p >screencap.png
find screencap.png
