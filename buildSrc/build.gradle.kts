buildscript {
  repositories {
    google()
    maven { setUrl("https://maven-central.storage-download.googleapis.com/maven2/") }
    mavenCentral()
    gradlePluginPortal()
  }
  dependencies {
    classpath("org.gradle.kotlin:gradle-kotlin-dsl-plugins:6.7.13")
  }
}
apply(plugin = "org.gradle.kotlin.kotlin-dsl")

repositories {
  google()
  maven { setUrl("https://maven-central.storage-download.googleapis.com/maven2/") }
  mavenCentral()
  maven {
    setUrl("https://plugins.gradle.org/m2/")
  }
  maven { setUrl("https://jitpack.io") }
}

dependencies {
  implementation("com.android.tools.build:gradle:9.4.0")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.10")
  // Provides AGP's kapt plugin (`com.android.legacy-kapt`), used by `:playstoremodule` (ObjectBox).
  implementation("com.android.tools.build:gradle-kotlin:9.4.0")
  implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.3.13")
  implementation("com.google.dagger:hilt-android-gradle-plugin:2.60.1")
  implementation("org.jacoco:org.jacoco.core:0.8.15")
  implementation("org.jlleitschuh.gradle:ktlint-gradle:14.2.0")
  implementation("com.google.apis:google-api-services-androidpublisher:v3-rev20230406-2.0.0") {
    exclude(group = "com.google.guava", module = "guava")
  }
  implementation("com.google.http-client:google-http-client-jackson2:1.40.0") {
    exclude(group = "com.google.guava", module = "guava")
  }
  implementation("dev.detekt:detekt-gradle-plugin:2.0.0-alpha.6")
  implementation("com.googlecode.json-simple:json-simple:1.1.1")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")

  implementation(gradleApi())
  implementation(localGroovy())
}
