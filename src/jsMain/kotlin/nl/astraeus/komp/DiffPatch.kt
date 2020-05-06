package nl.astraeus.komp

import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.w3c.dom.events.Event
import org.w3c.dom.get

const val HASH_VALUE = "komp-hash-value"
//const val HASH_ATTRIBUTE = "data-komp-hash"
const val EVENT_ATTRIBUTE = "data-komp-events"

fun Node.getKompHash(): Int = this.asDynamic()[HASH_VALUE] as? Int? ?: -1

fun Node.setKompHash(hash: Int) {
  this.asDynamic()[HASH_VALUE] = hash
}

private fun NodeList.findNodeHashIndex(hash: Int): Int {
  for (index in 0..this.length) {
    val node = this[index]
    if (node is HTMLElement && node.getKompHash() == hash) {
      return index
    }
  }

  return -1
}

object DiffPatch {

  fun hashesMatch(oldNode: Node, newNode: Node): Boolean {
    return (
        oldNode is HTMLElement &&
        newNode is HTMLElement &&
        oldNode.nodeName == newNode.nodeName &&
        oldNode.getKompHash() == newNode.getKompHash()
           )
  }

  fun updateNode(oldNode: Node, newNode: Node): Node {
    if (hashesMatch(oldNode, newNode)) {
      if (Komponent.logReplaceEvent) {
        console.log("Hashes match", oldNode, newNode, oldNode.getKompHash(), newNode.getKompHash())
      }
      return oldNode
    }

    if (oldNode.nodeType == newNode.nodeType && oldNode.nodeType == 3.toShort()) {
      if (oldNode.textContent != newNode.textContent) {
        if (Komponent.logReplaceEvent) {
          console.log("Updating text content", oldNode, newNode)
        }
        oldNode.textContent = newNode.textContent
      }
      return oldNode
    }

    if (oldNode is HTMLElement && newNode is HTMLElement) {
      if (oldNode.nodeName == newNode.nodeName) {
        if (Komponent.logReplaceEvent) {
          console.log("Update attributes", oldNode.innerHTML, newNode.innerHTML)
        }
        updateAttributes(oldNode, newNode);
        if (Komponent.logReplaceEvent) {
          console.log("Update children", oldNode.innerHTML, newNode.innerHTML)
        }
        updateChildren(oldNode, newNode)
        updateEvents(oldNode, newNode)
        oldNode.setKompHash(newNode.getKompHash())
        return oldNode
      }
    }

    if (Komponent.logReplaceEvent) {
      console.log("Replace node (type)", oldNode.nodeType, oldNode, newNode)
    }
    replaceNode(oldNode, newNode)
    return newNode
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
    var oldIndex = 0
    var newIndex = 0

    if (Komponent.logReplaceEvent) {
      console.log("updateChildren HTML old/new", oldNode.innerHTML, newNode.innerHTML)
    }

    while (newIndex < newNode.childNodes.length) {
      if (Komponent.logReplaceEvent) {
        console.log("Update Old/new", oldIndex, newIndex)
      }
      val newChildNode = newNode.childNodes[newIndex]

      if (oldIndex < oldNode.childNodes.length) {
        val oldChildNode = oldNode.childNodes[oldIndex]

        if (oldChildNode != null && newChildNode != null) {
/*
          if (Komponent.logReplaceEvent) {
            console.log(">>> updateChildren old/new", oldChildNode, newChildNode)
          }
*/

          if (Komponent.logReplaceEvent) {
            console.log("Update node Old/new", oldChildNode, newChildNode)
          }

          if (!hashesMatch(oldChildNode, newChildNode) && newChildNode is HTMLElement && oldChildNode is HTMLElement) {
            if (Komponent.logReplaceEvent) {
              console.log("Hashes don't match")
            }

            val oldHash = oldChildNode.getKompHash()
            val newHash = newChildNode.getKompHash()

            if (newHash != null) {
              val oldNodeWithNewHashIndex = oldNode.childNodes.findNodeHashIndex(newHash)

              if (Komponent.logReplaceEvent) {
                console.log("oldNodeWithNewHashIndex", newHash, oldNodeWithNewHashIndex)
              }

              if (oldNodeWithNewHashIndex > oldIndex) {
                if (oldHash != null) {
                  val newNodeWithOldHashIndex = newNode.childNodes.findNodeHashIndex(oldHash)

                  // remove i.o. swap
                  if (newNodeWithOldHashIndex == -1) {
                    if (Komponent.logReplaceEvent) {
                      console.log("Old node missing in new tree, remove node", oldChildNode)
                    }
                    oldNode.removeChild(oldChildNode)
                    continue
                  }
                }
                val nodeWithHash = oldNode.childNodes[oldNodeWithNewHashIndex]

                if (Komponent.logReplaceEvent) {
                  console.log("nodeWithHash", nodeWithHash)
                }
                if (nodeWithHash != null) {
                  if (Komponent.logReplaceEvent) {
                    console.log(">-> swap nodes", oldNode)
                  }

                  oldNode.insertBefore(nodeWithHash, oldNode.childNodes[oldIndex])

                  if (Komponent.logReplaceEvent) {
                    console.log(">-> swapped nodes", oldNode)
                  }
                  newIndex++
                  oldIndex++
                  continue
                }
              } else if (oldHash != null && newNode.childNodes.findNodeHashIndex(oldHash) > newIndex) {
                if (Komponent.logReplaceEvent) {
                  console.log("newNodeWithOldHashIndex", oldHash, newNode.childNodes.findNodeHashIndex(oldHash))
                }

                oldNode.insertBefore(newChildNode, oldChildNode)
                oldIndex++
                continue
              }
            }
          }

          updateNode(oldChildNode, newChildNode)
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

/*
      if (Komponent.logReplaceEvent) {
        console.log("<<< Updated Old/new", oldNode.innerHTML, newNode.innerHTML)
      }
*/

      oldIndex++
      newIndex++
    }

    while (oldIndex < oldNode.childNodes.length) {
      oldNode.childNodes[oldIndex]?.also {
        if (Komponent.logReplaceEvent) {
          console.log("Remove old node", it)
        }

        oldNode.removeChild(it)
      }
    }
  }

  private fun updateEvents(oldNode: HTMLElement, newNode: HTMLElement) {
    val oldEvents = mutableListOf<String>()
    oldEvents.addAll((oldNode.getAttribute(EVENT_ATTRIBUTE) ?: "").split(","))

    val newEvents = (newNode.getAttribute(EVENT_ATTRIBUTE) ?: "").split(",")

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

    newNode.getAttribute(EVENT_ATTRIBUTE)?.also {
      oldNode.setAttribute(EVENT_ATTRIBUTE, it)
    }
  }

  private fun replaceNode(oldNode: Node, newNode: Node) {
    oldNode.parentNode?.also { parent ->
      val clone = newNode.cloneNode(true)
      if (newNode is HTMLElement) {
        val events = (newNode.getAttribute(EVENT_ATTRIBUTE) ?: "").split(",")
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
