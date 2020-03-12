#!/usr/bin/env bash

adb logcat -c
adb logcat *:E -v color &
./gradlew jacocoInstrumentationTestReport
