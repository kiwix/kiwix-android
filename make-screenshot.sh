#!/bin/bash

if hash adb 2>/dev/null
then
    ADB=`which adb`
    TMP=/data/local/tmp/
    FILE=${1:-screenshot.png}
    PATH=$TMP$FILE

    $ADB shell screencap -p $PATH
    $ADB pull $PATH
    $ADB shell rm $PATH

    echo "Screenshot saved to '$FILE'"
    exit 0
else
    echo "Unable to find 'adb'"
    exit 1
fi
