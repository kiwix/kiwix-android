#!/usr/bin/env bash

adb logcat -c
adb logcat *:E -v color &
./gradlew jacocoInstrumentationTestReport
adb shell screencap -p /mnt/sdcard/screencap.png
adb pull /mnt/sdcard/screencap.png
find screencap.png
