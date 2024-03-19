#!/usr/bin/env bash

adb logcat -c
# shellcheck disable=SC2035
adb logcat *:E -v color &
retry=0
adb shell pm clear "org.kiwix.kiwixmobile"

while [ $retry -le 3 ]; do
  if ./gradlew jacocoInstrumentationTestReport; then
    echo "jacocoInstrumentationTestReport succeeded" >&2
    break
  else
    adb kill-server
    adb start-server
    adb logcat -c
    # shellcheck disable=SC2035
    adb logcat *:E -v color &

    adb shell pm clear "org.kiwix.kiwixmobile"
    ./gradlew clean
    retry=$(( retry + 1 ))
    if [ $retry -eq 3 ]; then
      adb exec-out screencap -p >screencap.png
      exit 1
    fi
  fi
done
