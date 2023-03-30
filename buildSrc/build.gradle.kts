plugins {
  `kotlin-dsl`
}
repositories {
  mavenCentral()
  google()
  maven {
    setUrl("https://plugins.gradle.org/m2/")
  }
  maven { setUrl("https://jitpack.io") }
}

dependencies {
  implementation("com.android.tools.build:gradle:7.4.2")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.7.0")
  implementation("org.jacoco:org.jacoco.core:0.8.8")
  implementation("org.jlleitschuh.gradle:ktlint-gradle:10.3.0")
  implementation("com.google.apis:google-api-services-androidpublisher:v3-rev129-1.25.0")
  implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.20.0")
  implementation("com.googlecode.json-simple:json-simple:1.1")

  implementation(gradleApi())
  implementation(localGroovy())
}
