@file:OptIn(ExperimentalWasmDsl::class)

import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
  kotlin("multiplatform") version "2.1.10"
  signing
  id("org.jetbrains.dokka") version "2.0.0"
  id("com.vanniktech.maven.publish") version "0.31.0"
}

group = "nl.astraeus"
version = "1.2.9"

repositories {
  mavenCentral()
  maven {
    name = "Sonatype Releases"
    url = uri("https://central.sonatype.com/api/v1/publisher/deployments/download/")
  }
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

publishing {
  repositories {
    mavenLocal()
    maven {
      name = "gitea"
      setUrl("https://gitea.astraeus.nl/api/packages/rnentjes/maven")

      credentials {
        val giteaUsername: String? by project
        val giteaPassword: String? by project

        username = giteaUsername
        password = giteaPassword
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

mavenPublishing {
  publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)

  signAllPublications()

  coordinates(group.toString(), name, version.toString())

  pom {
    name = "kotlin-komponent"
    description = "Kotlin komponent"
    inceptionYear = "2017"
    url = "https://github.com/rnentjes/komponent"
    licenses {
      license {
        name = "MIT"
        url = "https://opensource.org/licenses/MIT"
      }
    }
    developers {
      developer {
        id = "rnentjes"
        name = "Rien Nentjes"
        email = "info@nentjes.com"
      }
    }
    scm {
      url = "https://github.com/rnentjes/komponent"
    }
  }
}
