#!/bin/sh

export ANDROID_ROOT=/home/kelson/android/android-ndk-r6/
export PATH=$PATH:$ANDROID_ROOT/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/
export CPPFLAGS="-I$ANDROID_ROOT/platforms/android-9/arch-arm/usr/include/ -I$ANDROID_ROOT/sources/cxx-stl/gnu-libstdc++/include/ -I$ANDROID_ROOT/sources/cxx-stl/gnu-libstdc++/libs/armeabi-v7a/include/"
export CFLAGS="-nostdlib"
export LDFLAGS="-Wl,-rpath-link=$ANDROID_ROOT/platforms/android-9/arch-arm/usr/lib/ -L$ANDROID_ROOT/platforms/android-9/arch-arm/usr/lib/"
export LIBS=-lc

./configure -host=arm-linux-androideabi --with-gecko-sdk=/home/kelson/android/mozilla-central/objdir-droid/dist/ --without-dependences
