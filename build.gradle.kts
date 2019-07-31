buildscript {
  repositories {
    google()
    jcenter()
  }
  dependencies {
    classpath("com.android.tools.build:gradle:3.4.1")
    classpath("com.dicedmelon.gradle:jacoco-android:0.1.4")
    // NOTE: Do not place your application dependencies here; they belong
    // in the individual module build.gradle files
  }
}

ext {
  set("androidGradlePluginVersion", "3.4.1")
  set("testdroidGradlePluginVersion", "2.63.0")
  set("appCompatVersion", "1.0.2")
  set("materialVersion", "1.0.0")
  set("annotationVersion", "1.0.0")
  set("cardViewVersion", "1.0.0")
  set("rxJavaVersion", "2.2.5")
  set("rxAndroidVersion", "2.1.0")
  set("okHttpVersion", "3.12.1")
  set("retrofitVersion", "2.5.0")
  set("javaxAnnotationVersion", "1.3.2")
  set("daggerVersion", "2.21")
  set("inkPageIndicatorVersion", "1.3.0")
  set("constraintLayoutVersion", "1.1.3")
  set("butterKnifeVersion", "10.1.0")
  set("espressoVersion", "3.1.1")
  set("apacheCommonsVersion", "2.6")
  set("multidexVersion", "2.0.1")
  set("jUnitVersion", "4.12")
  set("mockitoVersion", "2.24.5")
  set("powerMockVersion", "1.6.6")
  set("powerMockJUnitVersion", "1.7.4")
  set("baristaVersion", "2.7.1")
  set("kotlinVersion", "1.3.41")
  set("objectboxVersion", "2.3.4")
}

allprojects {
  repositories {
    google()
    jcenter()
  }
}

tasks.create<Delete>("clean") {
  delete(rootProject.buildDir)
}

