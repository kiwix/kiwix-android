#!/bin/bash
if [ -f bin/Kiwix-debug.apk ]
then
    ../src/dependencies/android-sdk/sdk/platform-tools/adb uninstall org.kiwix.kiwixmobile ; ../src/dependencies/android-sdk/sdk/platform-tools/adb install bin/Kiwix-debug.apk
else
    echo "No APK file available!"
fi