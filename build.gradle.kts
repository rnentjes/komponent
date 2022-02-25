plugins {
  kotlin("multiplatform") version "1.6.20-M1"
  `maven-publish`
  signing
  id("org.jetbrains.dokka") version "1.5.31"
}

group = "nl.astraeus"
version = "1.0.5-SNAPSHOT"

repositories {
  mavenCentral()
}

kotlin {
  js(BOTH) {
    browser {
      testTask {
        useKarma {
          useChromiumHeadless()
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
      setUrl("https://nexus.astraeus.nl/nexus/content/repositories/releases")
      credentials {
        val nexusUsername: String? by project
        val nexusPassword: String? by project

        username = nexusUsername
        password = nexusPassword
      }
    }
    maven {
      name = "snapshots"
      // change to point to your repo, e.g. http://my.org/repo
      setUrl("https://nexus.astraeus.nl/nexus/content/repositories/snapshots")
      credentials {
        val nexusUsername: String? by project
        val nexusPassword: String? by project

        username = nexusUsername
        password = nexusPassword
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

signing {
  sign(publishing.publications)
}
