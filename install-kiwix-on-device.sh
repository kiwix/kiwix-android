#!/bin/bash

# default value is guessed from repo (AndroidManifest)
PACKAGE=`python -c "from xml.dom.minidom import parse; d=parse('app/src/main/AndroidManifest.xml'); print([e.getAttribute('package').strip() for e in d.getElementsByTagName('manifest')][-1])"`

if [ "x$1" != "x" ];
	then
	PACKAGE=$1
fi

if [ -f app/build/outputs/apk/${PACKAGE}-debug.apk ]
then
    echo "Uninstalling old Kiwix APK..."
    ${ANDROID_HOME}/platform-tools/adb uninstall ${PACKAGE} ;
    echo "Installing new Kiwix APK..."
    ${ANDROID_HOME}/platform-tools/adb install build/outputs/apk/${PACKAGE}-debug.apk
else
    echo "No APK file available for package ${PACKAGE} !"
fi
