plugins {
    kotlin("multiplatform") version "1.4-M2-eap-68"
    `maven-publish`
}

group = "nl.astraeus"
version = "0.1.20-SNAPSHOT"

repositories {
    maven { setUrl("https://dl.bintray.com/kotlin/kotlin-eap") }
    mavenCentral()
    maven {
        url = uri("https://dl.bintray.com/kotlin/kotlin-dev")
    }
}

kotlin {
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */
    js {
        browser {
            //produceKotlinLibrary()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))

                //implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2-build-1711")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))

                api("org.jetbrains.kotlinx:kotlinx-html-js:0.7.2-build-1716")
            }
        }
    }
}

publishing {
    repositories {
        maven {
            name = "releases"
            // change to point to your repo, e.g. http://my.org/repo
            url = uri("http://nexus.astraeus.nl/nexus/content/repositories/releases")
            credentials {
                val nexusUsername: String by project
                val nexusPassword: String by project

                username = nexusUsername
                password = nexusPassword
            }
        }
        maven {
            name = "snapshots"
            // change to point to your repo, e.g. http://my.org/repo
            url = uri("http://nexus.astraeus.nl/nexus/content/repositories/snapshots")
            credentials {
                val nexusUsername: String by project
                val nexusPassword: String by project

                username = nexusUsername
                password = nexusPassword
            }
        }
    }
    publications {
        val kotlinMultiplatform by getting {
            //artifactId = "kotlin-css-generator"
        }
    }
}
