#!/bin/sh

#export ANDROID_ROOT=/home/kelson/android/android-ndk-r6/
#export PATH=$PATH:$ANDROID_ROOT/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/
#export CPPFLAGS="-I$ANDROID_ROOT/platforms/android-9/arch-arm/usr/include/"
#export CFLAGS="-nostdlib"
#export LDFLAGS="-L$ANDROID_ROOT/platforms/android-9/arch-arm/usr/lib/ -Wl,-rpath-link=$ANDROID_ROOT/platforms/android-9/arch-arm/usr/lib/ -lgcc "
#export LIBS="-lc"

export ANDROID_ROOT=/tmp/ndk-kelson/arm-linux-androideabi-4.4.3/
export PATH=$PATH:$ANDROID_ROOT/bin/
export CPPFLAGS=
export CFLAGS=
export LDFLAGS=
export LIBS=

rm -f build-aux/config.sub
./autogen.sh
./configure -host=arm-linux-androideabi
make
