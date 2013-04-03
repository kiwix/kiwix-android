#include <jni.h>
#include "JNIKiwix.h"

#include <stdio.h>
#include <string.h>

#include <iostream>
#include <string>

/* c2jni type conversion functions */
jboolean c2jni(const bool &val) {
  return val ? JNI_TRUE : JNI_FALSE;
}

jstring c2jni(const std::string &val, JNIEnv *env) {
  return env->NewStringUTF(val.c_str());
}

jint c2jni(const int val) {
  return (jint)val;
}

jint c2jni(const unsigned val) {
  return (unsigned)val;
}

/* jni2c type conversion functions */
bool jni2c(const jboolean &val) {
  return val == JNI_TRUE;
}

std::string jni2c(const jstring &val, JNIEnv *env) {
  return std::string(env->GetStringUTFChars(val, 0));
}

int jni2c(const jint val) {
  return (int)val;
}

/* Method to deal with variable passed by reference */
void setStringObjValue(const std::string &value, const jobject obj, JNIEnv *env) {
  jclass objClass = env->GetObjectClass(obj);
  jfieldID objFid = env->GetFieldID(objClass, "value", "Ljava/lang/String;");
  env->SetObjectField(obj, objFid, c2jni(value, env));
}

void setIntObjValue(const int value, const jobject obj, JNIEnv *env) {
  jclass objClass = env->GetObjectClass(obj);
  jfieldID objFid = env->GetFieldID(objClass, "value", "I");
  env->SetIntField(obj, objFid, value);
}

void setBoolObjValue(const bool value, const jobject obj, JNIEnv *env) {
  jclass objClass = env->GetObjectClass(obj);
  jfieldID objFid = env->GetFieldID(objClass, "value", "Z");
  env->SetIntField(obj, objFid, c2jni(value));
}

/* Kiwix library functions */
JNIEXPORT jboolean JNICALL Java_JNIKiwix_nativeLoadZIM(JNIEnv *env, jobject obj, jstring path) {
  return c2jni(true);
}

JNIEXPORT jbyteArray JNICALL Java_JNIKiwix_nativeGetContent(JNIEnv *env, jobject obj, jstring url, 
							    jobject mimeTypeObj, jobject sizeObj) {
  setStringObjValue("42", mimeTypeObj, env);
  setIntObjValue(42, sizeObj, env);

  std::string cData = "This is great!";
  jbyteArray data = env->NewByteArray(6);
  jbyte *dataPointer = env->GetByteArrayElements(data, 0);
  memcpy(dataPointer, cData.c_str(), c2jni(cData.size()));
  env->ReleaseByteArrayElements(data, dataPointer, 0);

  return data;
}

