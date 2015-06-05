#!/bin/bash

# default value is guessed from repo (AndroidManifest)
PACKAGE=`python -c "from xml.dom.minidom import parse; d=parse('AndroidManifest.xml'); print([e.getAttribute('package').strip() for e in d.getElementsByTagName('manifest')][-1])"`

if [ "x$1" != "x" ];
	then
	PACKAGE=$1
fi

if [ -f build/outputs/apk/${PACKAGE}-debug-unaligned.apk ]
then
    echo "Uninstalling old Kiwix APK..."
    ../src/dependencies/android-sdk/platform-tools/adb uninstall ${PACKAGE} ;
    echo "Installing new Kiwix APK..."
    ../src/dependencies/android-sdk/platform-tools/adb install build/outputs/apk/${PACKAGE}-debug-unaligned.apk
else
    echo "No APK file available for package ${PACKAGE} !"
fi
