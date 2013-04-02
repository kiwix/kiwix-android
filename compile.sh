#!/bin/bash

# Compile the JNI class, this will create JNIKiwix.class
javac JNIKiwix.java

# Create the JNI header file, this will create JNIKiwix.h
javah -jni JNIKiwix

# Compile the native lib libkiwix.so
g++ -shared -fpic -o libkiwix.so -I/usr/lib/jvm/java-7-openjdk-i386/include/ kiwix.c

# Run the code
java -Djava.library.path=./ JNIKiwix