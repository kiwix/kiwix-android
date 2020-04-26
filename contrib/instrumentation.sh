#!/usr/bin/env bash

adb shell screencap -p /sdcard/screencap.png
adb pull /sdcard/screencap.png
find screencap.png
adb logcat -c
adb logcat *:E -v color &
./gradlew jacocoInstrumentationTestReport
