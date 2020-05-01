#!/usr/bin/env bash

adb logcat -c
adb logcat *:E -v color &
./gradlew jacocoInstrumentationTestReport
adb shell screencap -p /sdcard/screencap.png
adb shell
run-as org.kiwix.kiwixmobile
cp '/sdcard/screencap.png' '/mnt/sdcard/'
exit
exit
adb pull /sdcard/screencap.png
find screencap.png
