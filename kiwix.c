#include <jni.h>
#include "JNIKiwix.h"

#include <iostream>
#include <string>

/* c2jni type conversion functions */
jboolean c2jni(const bool &val) {
  return val ? JNI_TRUE : JNI_FALSE;
}

jstring c2jni(const std::string &val, JNIEnv *env) {
  return env->NewStringUTF(val.c_str());
}

/* jni2c type conversion functions */
bool jni2c(const jboolean &val) {
  return val == JNI_TRUE;
}

std::string jni2c(const jstring &val, JNIEnv *env) {
  return std::string(env->GetStringUTFChars(val, 0));
}

/* Kiwix library functions */
JNIEXPORT jboolean JNICALL Java_JNIKiwix_nativeLoadZIM(JNIEnv *env, jobject obj, jstring path) {
  return c2jni(true);
}

JNIEXPORT jstring JNICALL Java_JNIKiwix_nativeGetContent(JNIEnv *env, jobject obj, jstring url) {
}

