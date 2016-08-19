#include <jni.h>
#include "org_kiwix_kiwixmobile_JNIKiwix.h"

#include <stdio.h>
#include <string.h>

#include <iostream>
#include <string>

#include "unicode/putil.h"
#include <kiwix/reader.h>
#include <base64.h>

#include <android/log.h>
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO, "kiwix", __VA_ARGS__)

#include <xapian.h>

/* global variables */
kiwix::Reader *reader = NULL;

static pthread_mutex_t readerLock = PTHREAD_MUTEX_INITIALIZER;

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
  
  pthread_mutex_lock(&readerLock);
  if (reader != NULL) {
    try {
      std::string cUrl = reader->getMainPageUrl();
      url = c2jni(cUrl, env);
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }
  }
  pthread_mutex_unlock(&readerLock);
  
  return url;
}

JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getId(JNIEnv *env, jobject obj) {
  jstring id;
  
  pthread_mutex_lock(&readerLock);
  if (reader != NULL) {
    try {
      std::string cId = reader->getId();
      id = c2jni(cId, env);
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }
  }
  pthread_mutex_unlock(&readerLock);
  
  return id;
}

JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getCreator(JNIEnv *env, jobject obj) {
  jstring creator;
  
  pthread_mutex_lock(&readerLock);
  if (reader != NULL) {
    try {
      std::string cCreator = reader->getCreator();
      creator = c2jni(cCreator, env);
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }
  }
  pthread_mutex_unlock(&readerLock);
  
  return creator;
}

JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getPublisher(JNIEnv *env, jobject obj) {
  jstring publisher;
  
  pthread_mutex_lock(&readerLock);
  if (reader != NULL) {
    try {
      std::string cPublisher = reader->getPublisher();
      publisher = c2jni(cPublisher, env);
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }
  }
  pthread_mutex_unlock(&readerLock);
  
  return publisher;
}

JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getFavicon(JNIEnv *env, jobject obj) {
  jstring favicon;
  
  pthread_mutex_lock(&readerLock);
  if (reader != NULL) {
    try {
	  std::string cContent;
	  std::string cMime;
      reader->getFavicon(cContent, cMime);
      favicon = c2jni(base64_encode(reinterpret_cast<const unsigned char*>(cContent.c_str()), cContent.length()), env);
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }
  }
  pthread_mutex_unlock(&readerLock);
  
  return favicon;
}

JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getDate(JNIEnv *env, jobject obj) {
  jstring date;
  
  pthread_mutex_lock(&readerLock);
  if (reader != NULL) {
    try {
      std::string cDate = reader->getDate();
      date = c2jni(cDate, env);
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }
  }
  pthread_mutex_unlock(&readerLock);
  
  return date;
}

JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getLanguage(JNIEnv *env, jobject obj) {
  jstring language;
  
  pthread_mutex_lock(&readerLock);
  if (reader != NULL) {
    try {
      std::string cLanguage = reader->getLanguage();
      language = c2jni(cLanguage, env);
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }
  }
  pthread_mutex_unlock(&readerLock);
  
  return language;
}

JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getMimeType(JNIEnv *env, jobject obj, jstring url) {
  jstring mimeType;
  
  pthread_mutex_lock(&readerLock);
  if (reader != NULL) {
    std::string cUrl = jni2c(url, env);
    try {
      std::string cMimeType;
      reader->getMimeTypeByUrl(cUrl, cMimeType);
      mimeType = c2jni(cMimeType, env);
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }
  }
  pthread_mutex_unlock(&readerLock);
  
  return mimeType;
}

JNIEXPORT jboolean JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_loadZIM(JNIEnv *env, jobject obj, jstring path) {
  jboolean retVal = JNI_TRUE;
  std::string cPath = jni2c(path, env);

  pthread_mutex_lock(&readerLock);
  try {
    reader = new kiwix::Reader(cPath);
  } catch (exception &e) {
    std::cerr << e.what() << std::endl;
    retVal = JNI_FALSE;
  }
  pthread_mutex_unlock(&readerLock);

  return retVal;
}

JNIEXPORT jbyteArray JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getContent(JNIEnv *env, jobject obj, jstring url, jobject mimeTypeObj, jobject sizeObj) {

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

    pthread_mutex_lock(&readerLock);
    try {
      if (reader->getContentByUrl(cUrl, cData, cSize, cMimeType)) {
	data = env->NewByteArray(cSize);
	env->SetByteArrayRegion(data, 0, cSize, reinterpret_cast<const jbyte*>(cData.c_str()));
	setStringObjValue(cMimeType, mimeTypeObj, env);
	setIntObjValue(cSize, sizeObj, env);
      }
    } catch (exception &e) {
      LOGI(e.what());
      std::cerr << e.what() << std::endl;
    }
    pthread_mutex_unlock(&readerLock);
  }
  
  return data;
}

JNIEXPORT jboolean JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_searchSuggestions
(JNIEnv *env, jobject obj, jstring prefix, jint count) {
  jboolean retVal = JNI_FALSE;
  std::string cPrefix = jni2c(prefix, env);
  unsigned int cCount = jni2c(count);

  pthread_mutex_lock(&readerLock);
  try {
    if (reader != NULL) {
      if (reader->searchSuggestionsSmart(cPrefix, cCount)) {
	retVal = JNI_TRUE;
      }
    }
  } catch (exception &e) {
    std::cerr << e.what() << std::endl;
  }
  pthread_mutex_unlock(&readerLock);

  return retVal;
}

JNIEXPORT jboolean JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getNextSuggestion
(JNIEnv *env, jobject obj, jobject titleObj) {
  jboolean retVal = JNI_FALSE;
  std::string cTitle;

  pthread_mutex_lock(&readerLock);
  try {
    if (reader != NULL) {
      if (reader->getNextSuggestion(cTitle)) {
	setStringObjValue(cTitle, titleObj, env);
	retVal = JNI_TRUE;
      }
    }
  } catch (exception &e) {
    std::cerr << e.what() << std::endl;
  }
  pthread_mutex_unlock(&readerLock);

  return retVal;
}

JNIEXPORT jboolean JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getPageUrlFromTitle
(JNIEnv *env, jobject obj, jstring title, jobject urlObj) {
  jboolean retVal = JNI_FALSE;
  std::string cTitle = jni2c(title, env);
  std::string cUrl;

  pthread_mutex_lock(&readerLock);
  try {
    if (reader != NULL) {
      if (reader->getPageUrlFromTitle(cTitle, cUrl)) {
	setStringObjValue(cUrl, urlObj, env);
	retVal = JNI_TRUE;
      }
    }
  } catch (exception &e) {
    std::cerr << e.what() << std::endl;
  }
  pthread_mutex_unlock(&readerLock);
    
  return retVal;
}

JNIEXPORT jboolean JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getTitle
(JNIEnv *env , jobject obj, jobject titleObj) {
  jboolean retVal = JNI_FALSE;
  std::string cTitle;

  pthread_mutex_lock(&readerLock);
  try {
    if (reader != NULL) {
      std::string cTitle = reader->getTitle();
      setStringObjValue(cTitle, titleObj, env);
      retVal = JNI_TRUE;
    }
  } catch (exception &e) {
    std::cerr << e.what() << std::endl;
  }
  pthread_mutex_unlock(&readerLock);

  return retVal;

}

JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getDescription(JNIEnv *env, jobject obj) {
  jstring description;
  
  pthread_mutex_lock(&readerLock);
  if (reader != NULL) {
    try {
      std::string cDescription = reader->getDescription();
      description = c2jni(cDescription, env);
    } catch (exception &e) {
      std::cerr << e.what() << std::endl;
    }
  }
  pthread_mutex_unlock(&readerLock);
  
  return description;
}

JNIEXPORT jboolean JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_getRandomPage
(JNIEnv *env, jobject obj, jobject urlObj) {
  jboolean retVal = JNI_FALSE;
  std::string cUrl;

  pthread_mutex_lock(&readerLock);
  try {
    if (reader != NULL) {
      std::string cUrl = reader->getRandomPageUrl();
      setStringObjValue(cUrl, urlObj, env);
      retVal = JNI_TRUE;
    }
  } catch (exception &e) {
    std::cerr << e.what() << std::endl;
  }
  pthread_mutex_unlock(&readerLock);

  return retVal;
}

JNIEXPORT void JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_setDataDirectory
  (JNIEnv *env, jobject obj, jstring dirStr) {
  std::string cPath = jni2c(dirStr, env);

  pthread_mutex_lock(&readerLock); 
  try {
    u_setDataDirectory(cPath.c_str());
  } catch (exception &e) {
    std::cerr << e.what() << std::endl;
  }
  pthread_mutex_unlock(&readerLock);
}

const char* executeQuery(const char* dbLoc, const char* qu, bool partial) try {
    Xapian::Database db(dbLoc);

    // Start an enquire session.
    Xapian::Enquire enquire(db);

    std::string query_string(qu);
    std::string reply("");

    // Parse the query string to produce a Xapian::Query object.
    Xapian::QueryParser qp;
    Xapian::Stem stemmer("english");
    qp.set_stemmer(stemmer);
    qp.set_database(db);
    qp.set_stemming_strategy(Xapian::QueryParser::STEM_ALL);
    Xapian::Query query;

    if (partial)
        query = qp.parse_query(query_string, Xapian::QueryParser::FLAG_PARTIAL);
    else
        query = qp.parse_query(query_string);

    // Find the top 20 results for the query.
    enquire.set_query(query);
    Xapian::MSet matches = enquire.get_mset(0, 20);

    for (Xapian::MSetIterator i = matches.begin(); i != matches.end(); ++i) {
        reply += i.get_document().get_data();
        reply += "\n";
    }
    return reply.c_str();
}  catch (const Xapian::Error &e) {
    //return e.get_description().c_str();
    return "";
}

JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_indexedQuery
        (JNIEnv *env, jclass thiz, jstring db, jstring qu) {
    const char* d = env->GetStringUTFChars(db, 0);
    const char* q = env->GetStringUTFChars(qu, 0);
    const char* result = executeQuery(d, q, false);
    return env->NewStringUTF(result);
}

JNIEXPORT jstring JNICALL Java_org_kiwix_kiwixmobile_JNIKiwix_indexedQueryPartial
             (JNIEnv *env,jclass thiz, jstring db, jstring qu) {
    const char* d = env->GetStringUTFChars(db, 0);
    const char* q = env->GetStringUTFChars(qu, 0);
    const char* result = executeQuery(d, q, true);
    return env->NewStringUTF(result);
}

