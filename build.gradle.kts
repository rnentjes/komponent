
plugins {
    kotlin("multiplatform") version "1.4.30"
    `maven-publish`
}

group = "nl.astraeus"
version = "0.2.5-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
}

kotlin {
    /* Targets configuration omitted. 
    *  To find out how to configure the targets, please follow the link:
    *  https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#setting-up-targets */
    js(BOTH) {
        browser {
            //produceKotlinLibrary()
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))

                api("org.jetbrains.kotlinx:kotlinx-html-js:0.7.2")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

publishing {
    repositories {
        maven {
            name = "releases"
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
        val kotlinMultiplatform by getting {}
    }
}

tasks.getByName<Task>("publish").enabled = project.properties["nexusUsername"] != null
