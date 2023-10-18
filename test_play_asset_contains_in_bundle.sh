#!/bin/bash

# Download zim file and put that file in asset folder

./gradlew downloadDwdsZimAndPutInAssetFolder

# Check if file is exist in asset folder or not.

file="install_time_asset_for_dwds/src/main/assets/dwds.zim"

if [ -e "$file" ]; then
    echo "The file exists."
else
    echo "The file does not exist."
    exit 0
fi

# Generate the bundle file

./gradlew bundleDwdsRelease

# Check the bundle size it should be around 358961703 bytes

ls -l custom/build/outputs/bundle/dwdsRelease/custom-dwds-release.aab
