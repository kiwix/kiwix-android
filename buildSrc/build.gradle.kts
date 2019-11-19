plugins {
  `kotlin-dsl`
}
repositories {
  mavenCentral()
  google()
  jcenter()
  maven {
    setUrl("https://plugins.gradle.org/m2/")
  }
}

dependencies {
  implementation("com.android.tools.build:gradle:3.5.2")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.50")
  implementation("com.dicedmelon.gradle:jacoco-android:0.1.4")
  implementation("org.jlleitschuh.gradle:ktlint-gradle:8.2.0")
  implementation("com.google.apis:google-api-services-androidpublisher:v3-rev129-1.25.0")

  implementation(gradleApi())
  implementation(localGroovy())
}
