buildscript {
  repositories {
    google()
    jcenter()
  }
  dependencies {
    classpath(Libs.com_android_tools_build_gradle)
    classpath(Libs.kotlin_gradle_plugin)

    val navVersion = "2.3.0-rc01"
    classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")

    // NOTE: Do not place your application dependencies here; they belong
    // in the individual module build.gradle files
  }
}
plugins {
  buildSrcVersions
}

allprojects {
  repositories {
    google()
    jcenter()
    mavenCentral()
  }
}

tasks.create<Delete>("clean") {
  delete(rootProject.buildDir)
}
