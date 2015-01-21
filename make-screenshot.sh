#!/bin/bash

../src/dependencies/android-sdk/platform-tools/adb shell screencap -p /sdcard/screenshot.png
../src/dependencies/android-sdk/platform-tools/adb pull /sdcard/screenshot.png
../src/dependencies/android-sdk/platform-tools/adb shell rm /sdcard/screenshot.png
