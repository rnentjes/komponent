@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  kotlin("multiplatform") version "2.1.10"
  `maven-publish`
  signing
  id("org.jetbrains.dokka") version "1.5.31"
}

group = "nl.astraeus"
version = "1.2.5"

repositories {
  mavenCentral()
}

/*
tasks.withType<Test>(Test::class.java) {
  useJUnitPlatform()
}
*/

kotlin {
  js {
    browser {
      testTask {
        useKarma {
          useChromeHeadless()
        }
      }
    }
  }
  /*  wasmJs {
      //moduleName = project.name
      browser()

      mavenPublication {
        groupId = group as String
        pom { name = "${project.name}-wasm-js" }
      }
    }*/

  /*
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
      common {
        group("jsCommon") {
          withJs()
          // TODO: switch to `withWasmJs()` after upgrade to Kotlin 2.0
          withWasm()
        }
      }
    }
  */

  sourceSets {
    val commonMain by getting {
      dependencies {
        api("org.jetbrains.kotlinx:kotlinx-html:0.12.0")
      }
    }
    val commonTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
    val jsMain by getting
    val jsTest by getting {
      dependencies {
        implementation(kotlin("test"))
      }
    }
    //val wasmJsMain by getting
  }
}

extra["PUBLISH_GROUP_ID"] = group
extra["PUBLISH_VERSION"] = version
extra["PUBLISH_ARTIFACT_ID"] = name

// Stub secrets to let the project sync and build without the publication values set up
val signingKeyId: String? by project
val signingPassword: String? by project
val signingSecretKeyRingFile: String? by project
val ossrhUsername: String? by project
val ossrhPassword: String? by project

extra["signing.keyId"] = signingKeyId
extra["signing.password"] = signingPassword
extra["signing.secretKeyRingFile"] = signingSecretKeyRingFile
extra["ossrhUsername"] = ossrhUsername
extra["ossrhPassword"] = ossrhPassword

val javadocJar by tasks.registering(Jar::class) {
  archiveClassifier.set("javadoc")
}

publishing {
  repositories {
    mavenLocal()
    maven {
      name = "releases"
      // change to point to your repo, e.g. http://my.org/repo
      setUrl("https://reposilite.astraeus.nl/releases")
      credentials {
        val reposiliteUsername: String? by project
        val reposilitePassword: String? by project

        username = reposiliteUsername
        password = reposilitePassword
      }
    }
    maven {
      name = "snapshots"
      // change to point to your repo, e.g. http://my.org/repo
      setUrl("https://reposilite.astraeus.nl/snapshots")
      credentials {
        val reposiliteUsername: String? by project
        val reposilitePassword: String? by project

        username = reposiliteUsername
        password = reposilitePassword
      }
    }
    maven {
      name = "sonatype"
      setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
      credentials {
        username = ossrhUsername
        password = ossrhPassword
      }
    }
    maven {
      name = "gitea"
      setUrl("https://gitea.astraeus.nl/api/packages/rnentjes/maven")

      credentials() {
        val giteaUsername: String? by project
        val giteaPassword: String? by project

        username = giteaUsername
        password = giteaPassword
      }
    }
  }

  // Configure all publications
  publications.withType<MavenPublication> {
    // Stub javadoc.jar artifact
    artifact(javadocJar.get())

    // Provide artifacts information requited by Maven Central
    pom {
      name.set("kotlin-komponent")
      description.set("Kotlin komponent")
      url.set("https://github.com/rnentjes/komponent")

      licenses {
        license {
          name.set("MIT")
          url.set("https://opensource.org/licenses/MIT")
        }
      }
      developers {
        developer {
          id.set("rnentjes")
          name.set("Rien Nentjes")
          email.set("info@nentjes.com")
        }
      }
      scm {
        url.set("https://github.com/rnentjes/komponent")
      }
    }
  }
}

tasks.withType<AbstractPublishToMaven> {
  dependsOn(tasks.withType<Sign>())
}

signing {
  sign(publishing.publications)
}

tasks.withType<PublishToMavenRepository> {
  dependsOn(tasks.withType<Sign>())
}
