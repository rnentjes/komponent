package nl.astraeus.komp

import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.get

object DiffPatch {

    fun updateNode(oldNode: Node, newNode: Node): Node {
        if (oldNode is HTMLElement && newNode is HTMLElement) {
            if (oldNode.nodeName == newNode.nodeName) {
                if (oldNode.getAttribute("data-komp-hash") != null &&
                    oldNode.getAttribute("data-komp-hash") == newNode.getAttribute("data-komp-hash")) {

                    if (Komponent.logReplaceEvent) {
                        console.log("Skip node, hash equals", oldNode, newNode)
                    }

                    return oldNode
                } else {
                    if (Komponent.logReplaceEvent) {
                        console.log("Update attributes", oldNode.innerHTML, newNode.innerHTML)
                    }
                    updateAttributes(oldNode, newNode);
                    if (Komponent.logReplaceEvent) {
                        console.log("Update children", oldNode.innerHTML, newNode.innerHTML)
                    }
                    updateChildren(oldNode, newNode)
                    updateEvents(oldNode, newNode)
                    return oldNode
                }
            } else {
                if (Komponent.logReplaceEvent) {
                    console.log("Replace node ee", oldNode.innerHTML, newNode.innerHTML)
                }
                replaceNode(oldNode, newNode)
                return newNode
            }
        } else {
            if (oldNode.nodeType == newNode.nodeType && oldNode.nodeType == 3.toShort()) {
                if (oldNode.textContent != newNode.textContent) {
                    if (Komponent.logReplaceEvent) {
                        console.log("Updating text content", oldNode, newNode)
                    }
                    oldNode.textContent = newNode.textContent
                    return oldNode
                }
            }

            if (Komponent.logReplaceEvent) {
                console.log("Replace node", oldNode, newNode)
            }
            replaceNode(oldNode, newNode)
            return newNode

        }
    }

    private fun updateAttributes(oldNode: HTMLElement, newNode: HTMLElement) {
        // removed attributes
        for (index in 0 until oldNode.attributes.length) {
            val attr = oldNode.attributes[index]

            if (attr != null && newNode.attributes[attr.name] == null) {
                oldNode.removeAttribute(attr.name)
            }
        }

        for (index in 0 until newNode.attributes.length) {
            val attr = newNode.attributes[index]

            if (attr != null) {
                val oldAttr = oldNode.attributes[attr.name]

                if (oldAttr == null || oldAttr.value != attr.value) {
                    oldNode.setAttribute(attr.name, attr.value)
                }
            }
        }
    }

    private fun updateChildren(oldNode: HTMLElement, newNode: HTMLElement) {
        // todo: add 1 look ahead/back
        var oldIndex = 0
        var newIndex = 0

        if (Komponent.logReplaceEvent) {
            console.log("updateChildren old/new count", oldNode.childNodes.length, newNode.childNodes.length)
        }

        while(newIndex < newNode.childNodes.length) {
            if (Komponent.logReplaceEvent) {
                console.log(">>> updateChildren old/new count", oldNode.childNodes, newNode.childNodes)
                console.log("Update Old/new", oldIndex, newIndex)
            }
            val newChildNode = newNode.childNodes[newIndex]

            if (oldIndex < oldNode.childNodes.length) {
                val oldChildNode = oldNode.childNodes[oldIndex]

                if (oldChildNode != null && newChildNode != null) {
                    if (Komponent.logReplaceEvent) {
                        console.log("Update node Old/new", oldChildNode, newChildNode)
                    }

                    updateNode(oldChildNode, newChildNode)

                    if (Komponent.logReplaceEvent) {
                        console.log("--- Updated Old/new", oldNode.children, newNode.children)
                    }
                } else {
                    if (Komponent.logReplaceEvent) {
                        console.log("Null node", oldChildNode, newChildNode)
                    }
                }
            } else {
                if (Komponent.logReplaceEvent) {
                    console.log("Append Old/new/node", oldIndex, newIndex, newChildNode)
                }
                oldNode.append(newChildNode)
            }

            if (Komponent.logReplaceEvent) {
                console.log("<<< Updated Old/new", oldNode.children, newNode.children)
            }

            oldIndex++
            newIndex++
        }

        while(oldIndex < oldNode.childNodes.length) {
            oldNode.childNodes[oldIndex]?.also {
                if (Komponent.logReplaceEvent) {
                    console.log("Remove old node", it)
                }

                oldNode.removeChild(it)
            }
            oldIndex++
        }
    }

    private fun updateEvents(oldNode: HTMLElement, newNode: HTMLElement) {
        val oldEvents = mutableListOf<String>()
        oldEvents.addAll((oldNode.getAttribute("data-komp-events") ?: "").split(","))

        val newEvents = (newNode.getAttribute("data-komp-events") ?: "").split(",")

        for (event in newEvents) {
            if (event.isNotBlank()) {
                val oldNodeEvent = oldNode.asDynamic()["event-$event"]
                val newNodeEvent = newNode.asDynamic()["event-$event"]
                if (oldNodeEvent != null) {
                    oldNode.removeEventListener(event, oldNodeEvent as ((Event) -> Unit), null)
                }
                if (newNodeEvent != null) {
                    oldNode.addEventListener(event, newNodeEvent as ((Event) -> Unit), null)
                    oldNode.asDynamic()["event-$event"] = newNodeEvent
                }
                oldEvents.remove(event)
            }
        }

        for (event in oldEvents) {
            if (event.isNotBlank()) {
                val oldNodeEvent = oldNode.asDynamic()["event-$event"]
                if (oldNodeEvent != null) {
                    oldNode.removeEventListener(event, oldNodeEvent as ((Event) -> Unit), null)
                }
            }
        }

        newNode.getAttribute("data-komp-events")?.also {
            oldNode.setAttribute("data-komp-events", it)
        }
    }

    private fun replaceNode(oldNode: Node, newNode: Node) {
        oldNode.parentNode?.also { parent ->
            val clone = newNode.cloneNode(true)
            if (newNode is HTMLElement) {
                val events = (newNode.getAttribute("data-komp-events") ?: "").split(",")
                for (event in events) {
                    val foundEvent = newNode.asDynamic()["event-$event"]
                    if (foundEvent != null) {
                        clone.addEventListener(event, foundEvent as ((Event) -> Unit), null)
                    }
                }
            }
            parent.replaceChild(clone, oldNode)
        }
    }

}
