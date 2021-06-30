buildscript {
  repositories {
    google()
    mavenCentral()
    jcenter()
  }
  dependencies {
    classpath(Libs.com_android_tools_build_gradle)
    classpath(Libs.kotlin_gradle_plugin)
    classpath(Libs.navigation_safe_args_gradle_plugin)

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
    mavenCentral()
    jcenter()
  }
}

tasks.create<Delete>("clean") {
  delete(rootProject.buildDir)
}
