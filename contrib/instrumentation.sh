#!/usr/bin/env bash

adb logcat -c
adb logcat *:E -v color &
./gradlew jacocoInstrumentationTestReport; gradlew_return_code=$?
adb exec-out screencap -p > screencap.png
gradlew_return_code
