#!/bin/sh

#export ANDROID_ROOT=/home/kelson/android/android-ndk-r6/ 
#export PATH=$PATH:$ANDROID_ROOT/toolchains/arm-linux-androideabi-4.4.3/prebuilt/linux-x86/bin/
#export CPPFLAGS="-I$ANDROID_ROOT/platforms/android-9/arch-arm/usr/include/ -I$ANDROID_ROOT/sources/cxx-stl/gnu-libstdc++/include/ -I$ANDROID_ROOT/sources/cxx-stl/gnu-libstdc++/libs/armeabi-v7a/include/"
#export CFLAGS="-nostdlib"
#export LDFLAGS="-L$ANDROID_ROOT/platforms/android-9/arch-arm/usr/lib/ -Wl,-rpath-link=$ANDROID_ROOT/platforms/android-9/arch-arm/usr/lib/"
#export LIBS="-lc"

export ANDROID_ROOT=/tmp/ndk-kelson/arm-linux-androideabi-4.4.3/
export PATH=$PATH:$ANDROID_ROOT/bin/
export CPPFLAGS=
export CFLAGS="-DMOZ_NO_MOZALLOC"
export LDFLAGS="-nostdlib"
export LIBS=
export PYTHONPATH=/home/kelson/android/mozilla-central/objdir-droid/dist/sdk/bin/

make
