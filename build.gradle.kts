
plugins {
    kotlin("multiplatform") version "1.5.21"
    `maven-publish`
}

group = "nl.astraeus"
version = "0.5.7-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    js(BOTH) {
        browser {
            testTask {
                useKarma {
                    useChromiumHeadless()
                    //useChromeHeadless()
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))

                api("org.jetbrains.kotlinx:kotlinx-html-js:0.7.3")
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
        if (project.properties["nexusUsername"] != null) {
            maven {
                name = "releases"
                url = uri("https://nexus.astraeus.nl/nexus/content/repositories/releases")
                credentials {
                    val nexusUsername: String by project
                    val nexusPassword: String by project

                    username = nexusUsername
                    password = nexusPassword
                }
            }
            maven {
                name = "snapshots"
                url = uri("https://nexus.astraeus.nl/nexus/content/repositories/snapshots")
                credentials {
                    val nexusUsername: String by project
                    val nexusPassword: String by project

                    username = nexusUsername
                    password = nexusPassword
                }
            }
        } else {
            println("Publishing disabled properties not found.")
        }
    }
    publications {
        val kotlinMultiplatform by getting {}
    }
}
