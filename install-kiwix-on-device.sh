#!/bin/bash
if [ -f build/apk/android-debug-unaligned.apk ]
then
    ../src/dependencies/android-sdk/sdk/platform-tools/adb uninstall org.kiwix.kiwixmobile ;
    ../src/dependencies/android-sdk/sdk/platform-tools/adb install build/apk/android-debug-unaligned.apk
else
    echo "No APK file available!"
fi