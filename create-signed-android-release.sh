#!/bin/bash

function usage {
	echo "Usage:	$0 kiwix-android.keystore [PACKAGE] [APP_NAME] [APP_VERSION]"
	echo "You must specify the path of the certificate keystore."
	exit 1
}

if [ -f "$1" ];
	then
	CERTIFICATE=$1
else
	usage
fi

function die {
	echo -n "[ERROR] "
	echo -n $1
	echo -n " Aborting.
"
	exit 1
}

# default values are guessed from repo (AndroidManifest and res/values/branding)
APP_NAME=`python -c "from xml.dom.minidom import parse; d=parse('res/values/branding.xml'); print([e.childNodes[0].data.strip() for e in d.getElementsByTagName('string') if e.getAttribute('name') == 'app_name'][-1])"`
PACKAGE=`python -c "from xml.dom.minidom import parse; d=parse('AndroidManifest.xml'); print([e.getAttribute('package').strip() for e in d.getElementsByTagName('manifest')][-1])"`
APP_VERSION=`python -c "from xml.dom.minidom import parse; d=parse('AndroidManifest.xml'); print([e.getAttribute('android:versionName').strip() for e in d.getElementsByTagName('manifest')][-1])"`

if [ "x$2" != "x" ];
	then
	PACKAGE=$2
fi

if [ "x$3" != "x" ];
	then
	APP_NAME=$3
fi

if [ "x$4" != "x" ];
	then
	APP_VERSION=$4
fi

../src/dependencies/android-sdk/tools/android update project -p . -n Kiwix -t android-21

jarsigner -verbose -sigalg MD5withRSA -digestalg SHA1 -keystore $CERTIFICATE build/outputs/apk/${PACKAGE}-release-unsigned.apk kiwix || die "Error signing the package."
jarsigner -verify build/outputs/apk/${PACKAGE}-release-unsigned.apk || die "The package is not properly signed."
../src/dependencies/android-sdk/build-tools/21.1.2/zipalign -f -v 4 build/outputs/apk/${PACKAGE}-release-unsigned.apk "${APP_NAME}-${APP_VERSION}.apk" || die "Could not zipalign the signed package. Please check."

echo "[SUCCESS] Your signed release package is ready:"
ls -lh "${APP_NAME}-${APP_VERSION}.apk"
