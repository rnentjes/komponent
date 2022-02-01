# Table of contents

* [Home](home.md)
* [Getting started](getting-started.md)
* [How it works](how-it-works.md)

# Getting started

To get started create a new kotlin project in intellij of the type 'Browser application'

![Create 'Browser Application' project](/docs/img/create-project.png)

Add the 'sourceSets' block with the kotlin-komponent dependency so your build.gradle.kts looks like this:

```gradle
plugins {
    kotlin("js") version "1.6.10"
}

group = "com.test"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api("nl.astraeus:kotlin-komponent-js:1.0.0")

    testImplementation(kotlin("test"))
}

kotlin {
    js(IR) {
        binaries.executable()
        browser {
            commonWebpackConfig {
                cssSupport.enabled = true
            }
        }
    }
}
```

Refresh the gradle project to import the dependency.

There is now only one kotlin source file in the project called Simple.kt, it should look something like this:

```kotin
fun main() {
  console.log("Hello, ${greet()}")
}

fun greet() = "world"
```

Replace the code in the file with the following for a simple click app:

```koltin
import kotlinx.browser.document
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.hr
import kotlinx.html.js.onClickFunction
import nl.astraeus.komp.HtmlBuilder
import nl.astraeus.komp.Komponent
import nl.astraeus.komp.mutableCollectionState
import nl.astraeus.komp.state
import kotlin.js.Date

fun main() {
  console.log("Hello, ${greet()}")

  Komponent.create(document.body!!, TestKomponent())
}

fun greet() = "world"

class TestKomponent : Komponent() {
  var clicks: Int = 0
  val lines: MutableCollection<String> = mutableListOf()

  override fun HtmlBuilder.render() {
    div {
      div {
        +"Hello Komponent!"
      }

      div {
        +"Clicks $clicks"
      }

      div {
        button {
          +"Click"
          onClickFunction = {
            clicks++
            lines.add("click $clicks at ${Date()}")
            requestUpdate()
          }
        }
      }

      hr()

      for (line in lines) {
        div {
          + line
        }
      }

    }
  }
}
```

First in the main we add our TestKomponent to the document body with the following line:

```kotlin
  Komponent.create(document.body!!, TestKomponent())
```

The TestKomponent.render method will be called to render our Komponent. 
As you can see events can be attached inline with the on<event>Function methods.
The requestUpdate method will call the render method again and update the page accordingly.

After building the application you will find it in /build/distributions.

In the index.html page you will find the following line:

```html
<div id="root"></div>
```

This line is not needed for kotlin-komponent.

If you like you can use some helpers that will automatically call the requestUpdate method if
the data changes, that would look like this:

```kotlin
  var clicks: Int by state(0)
  val lines: MutableCollection<String> = mutableCollectionState(mutableListOf())
```

In that case you can remove the requestUpdate call from the onClickFunction.

You can find a working repository of this example here: [example]()
