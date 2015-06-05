#!/bin/bash
if [ -f build/outputs/apk/android-debug-unaligned.apk ]
then
    echo "Uninstalling old Kiwix APK..."
    ../src/dependencies/android-sdk/platform-tools/adb uninstall org.kiwix.kiwixmobile ;
    echo "Installing new Kiwix APK..."
    ../src/dependencies/android-sdk/platform-tools/adb install build/outputs/apk/android-debug-unaligned.apk
else
    echo "No APK file available!"
fi
