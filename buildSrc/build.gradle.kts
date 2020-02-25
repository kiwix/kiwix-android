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
  implementation("com.android.tools.build:gradle:3.6.0")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.61")
  implementation("com.dicedmelon.gradle:jacoco-android:0.1.4")
  implementation("org.jlleitschuh.gradle:ktlint-gradle:9.2.1")
  implementation("com.google.apis:google-api-services-androidpublisher:v3-rev129-1.25.0")
  implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.5.1")

  implementation(gradleApi())
  implementation(localGroovy())
}
