buildscript {
  repositories {
    google()
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
  }
  dependencies {
    classpath(Libs.com_android_tools_build_gradle)
    classpath(Libs.kotlin_gradle_plugin)
    classpath(Libs.kotlin_ksp)
    classpath(Libs.navigation_safe_args_gradle_plugin)
    classpath(Libs.keeper)

    // NOTE: Do not place your application dependencies here; they belong
    // in the individual module build.gradle files
  }
}
plugins {
  buildSrcVersions
  id("org.jetbrains.kotlin.plugin.compose") version Versions.ORG_JETBRAINS_KOTLIN_PLUGIN_COMPOSE apply false
}

allprojects {
  repositories {
    google()
    mavenCentral()
    maven { setUrl("https://jitpack.io") }
  }
}

tasks.create<Delete>("clean") {
  delete(rootProject.layout.buildDirectory)
}
