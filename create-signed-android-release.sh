#!/bin/bash

if [ -f "$1" ];
	then
	CERTIFICATE=$1
else
	echo "Usage:	$0 Kiwix-android.keystore"
	echo "You must specify the path of the certificate keystore."
	exit 1
fi

function die {
	echo -n "[ERROR] "
	echo -n $1
	echo -n " Aborting.
"
	exit 1
}

../src/dependencies/android-sdk/tools/android update project -p . -n Kiwix -t android-21

jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore $CERTIFICATE build/outputs/apk/android-release-unsigned.apk kiwix || die "Error signing the package."
jarsigner -verify build/outputs/apk/android-release-unsigned.apk || die "The package is not properly signed."
../src/dependencies/android-sdk/build-tools/21.1.2/zipalign -f -v 4 build/outputs/apk/android-release-unsigned.apk kiwix-android.apk || die "Could not zipalign the signed package. Please check."

echo "[SUCCESS] Your signed release package is ready:"
ls -lh kiwix-android.apk
