pluginManagement {
    repositories {

        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-dev") }

        maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }

        mavenCentral()

        maven { setUrl("https://plugins.gradle.org/m2/") }
    }
}

rootProject.name = "komp"

enableFeaturePreview("GRADLE_METADATA")
