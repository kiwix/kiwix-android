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

android update project -p . -n Kiwix -t android-14
ant release || die "ant release error."
jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore $CERTIFICATE bin/Kiwix-release-unsigned.apk kiwix || die "Error signing the package."
jarsigner -verify bin/Kiwix-release-unsigned.apk || die "The package is not properly signed."
zipalign -f -v 4 bin/Kiwix-release-unsigned.apk bin/kiwix-android.apk || die "Could not zipalign the signed package. Please check."

echo "[SUCCESS] Your signed release package is ready:"
ls -lh bin/kiwix-android.apk
