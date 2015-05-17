#!/bin/bash
if [ -f build/outputs/apk/android-debug-unaligned.apk ]
then
    ../src/dependencies/android-sdk/platform-tools/adb uninstall org.kiwix.kiwixmobile ;
    ../src/dependencies/android-sdk/platform-tools/adb install build/outputs/apk/android-debug-unaligned.apk
else
    echo "No APK file available!"
fi