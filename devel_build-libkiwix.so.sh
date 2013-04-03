#!/bin/bash

# this script is just for testing purposes
# it helps generate a libkiwix.so on a dev linux machine to assist
# with JNI debugging.

# change the following to point to absolute of kiwix repo.
KIWIX_ROOT=`pwd`/../

cd $KIWIX_ROOT

# compilation de liblzma.a
cd src/dependencies/xz-5.0.4
CFLAGS=" -fPIC " ./configure --disable-assembler --enable-shared --enable-static
make
cp src/liblzma/.libs/liblzma.a $KIWIX_ROOT/android/
make clean

# compile libzim.a
cd $KIWIX_ROOT/src/zimlib/src
g++ -fPIC -c -D_FILE_OFFSET_BITS=64 -D_LARGEFILE64_SOURCE article.cpp articlesearch.cpp cluster.cpp dirent.cpp file.cpp fileheader.cpp fileimpl.cpp indexarticle.cpp ptrstream.cpp search.cpp template.cpp unicode.cpp uuid.cpp zintstream.cpp envvalue.cpp lzmastream.cpp unlzmastream.cpp fstream.cpp md5.cpp md5stream.cpp -I. -I../include/ -I../../dependencies/xz-5.0.4/src/liblzma/api/
ar rvs libzim.a article.o articlesearch.o cluster.o dirent.o file.o fileheader.o fileimpl.o indexarticle.o ptrstream.o search.o template.o unicode.o uuid.o zintstream.o envvalue.o lzmastream.o unlzmastream.o fstream.o md5.o md5stream.o
cp libzim.a $KIWIX_ROOT/android/
rm *.o

# compile libkiwix
cd $KIWIX_ROOT/android/
rm *.o
g++ -fPIC -c kiwix.c $KIWIX_ROOT/src/common/kiwix/reader.cpp $KIWIX_ROOT/src/common/stringTools.cpp -I$KIWIX_ROOT/src/zimlib/include -I../../dependencies/xz-5.0.4/src/liblzma/api/ -I$KIWIX_ROOT/src/common -I/usr/lib/jvm/java-7-openjdk-amd64/include/
g++ -fPIC -shared kiwix.o reader.o stringTools.o libzim.a liblzma.a /usr/lib/gcc/x86_64-linux-gnu/4.7/libgcc.a -o libkiwix.so

ls -lh libkiwix.so