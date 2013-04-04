#include <jni.h>
#include "org_kiwix_kiwixmobile_JNIKiwix.h"

#include <stdio.h>
#include <string.h>

#include <iostream>
#include <string>

#include <kiwix/reader.h>

/* global variables */
kiwix::Reader *reader = NULL;

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
JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getMainPage(JNIEnv *env, jobject obj) {
  jstring url;

  if (reader != NULL) {
    try {
      std::string cUrl = reader->getMainPageUrl();
      url = c2jni(cUrl, env);
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }
  }
  
  return url;
}

JNIEXPORT jboolean JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_nativeLoadZIM(JNIEnv *env, jobject obj, jstring path) {
  std::string cPath = jni2c(path, env);
  jboolean retVal = JNI_TRUE;

  try {
    reader = new kiwix::Reader(cPath);
  } catch (exception &e) {
    std::cerr << e.what() << std::endl;
    retVal = JNI_FALSE;
  }

  return retVal;
}

JNIEXPORT jbyteArray JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_nativeGetContent(JNIEnv *env, jobject obj, jstring url, jobject mimeTypeObj, jobject sizeObj) {

  /* Default values */
  setStringObjValue("", mimeTypeObj, env);
  setIntObjValue(0, sizeObj, env);
  jbyteArray data = env->NewByteArray(0);

  /* Retrieve the content */
  if (reader != NULL) {

    std::string cUrl = jni2c(url, env);
    std::string cData;
    std::string cMimeType;
    unsigned int cSize = 0;

    try {
      if (reader->getContentByUrl(cUrl, cData, cSize, cMimeType)) {
	data = env->NewByteArray(cSize);
	jbyte *dataPointer = env->GetByteArrayElements(data, 0);
	memcpy(dataPointer, cData.c_str(), cSize);
	env->ReleaseByteArrayElements(data, dataPointer, 0);

	setStringObjValue(cMimeType, mimeTypeObj, env);
	setIntObjValue(cSize, sizeObj, env);
      }
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }

  }
  
  return data;
}
