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
    exit 1
fi
