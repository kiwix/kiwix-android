#!/bin/sh

echo "Running lint..."

./gradlew ktlintCheck app:lintDebug core:lintDebug custom:lintCustomexampleDebug --daemon

status=$?

if [ "$status" = 0 ] ; then
    echo "Static analysis found no problems."
    exit 0
else
    ./gradlew ktlintFormat --daemon
    echo 1>&2 "Static analysis found violations and attempted to autofix, please commit these autoformat changes"
    echo "
    ---------------------------IMPORTANT FOR KIWIX DEVELOPERS----------------------------------------------
    If the build failed with '.../.gradle/daemon/5.6.1/custom/scr/customexample/info.json No File Found' then you do not have JAVA_HOME set to JDK8
    Please make sure JAVA_HOME is set to JDK8
    ---------------------------IMPORTANT FOR KIWIX DEVELOPERS----------------------------------------------"

    exit 1
fi
