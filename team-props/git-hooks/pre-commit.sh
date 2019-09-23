#!/bin/sh

echo "Running lint..."

./gradlew app:ktlintKiwixDebugCheck app:lintKiwixDebug --daemon

status=$?

if [ "$status" = 0 ] ; then
    echo "Static analysis found no problems."
    exit 0
else
    ./gradlew app:ktlintKiwixDebugFormat --daemon
    echo 1>&2 "Static analysis found violations and attempted to autofix, please commit these autoformat changes"
    echo "If the build failed for another reason please make sure JAVA_HOME is set to JDK8"
    exit 1
fi
