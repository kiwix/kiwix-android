pluginManagement {
  repositories {
    google()
    maven { setUrl("https://maven-central.storage-download.googleapis.com/maven2/") }
    mavenCentral()
    gradlePluginPortal()
  }
}

include(
  ":core",
  ":app",
  ":branded",
  ":install_time_asset",
  ":playstoremodule",
  ":standalonemodule",
  ":benchmark"
)
rootProject.name = "kiwix-android"
