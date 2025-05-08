// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.undercouchDownload) apply false
    // Add the dependency for the Google services Gradle plugin
    id("com.google.gms.google-services") version "4.4.2" apply false


}

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("de.undercouch:gradle-download-task:4.1.2")
    }
}
