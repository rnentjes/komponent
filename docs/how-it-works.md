# Table of contents

* [Home](home.md)
* [Getting started](getting-started.md)
* [How it works](how-it-works.md)

# How it works

When the requestUpdate call is made to the [Komponent](src/jsMain/kotlin/nl/astraeus/komp/Komponent.kt)
the update is queued in a callback. The callback will be called after the current event is handled.

If there are multiple updates requested, these are sorted so that the top Komponents get executed first. 
This way there will not be double updates of the same komponent.

The render call will be invoked and every html builder function (div, span etc.) will call the 
different HtmlBuilder functions like onTagStart, onTagAttributeChange etc.

In these functions the HtmlBuilder will compare the dom against the call being made, and it will update the DOM
as needed.




