package org.kiwix.kiwixmobile.core.extensions

typealias HeaderizableList<T> = List<T>

fun <SUPERTYPE, ITEM : SUPERTYPE, HEADER : SUPERTYPE> HeaderizableList<ITEM>.foldOverAddingHeaders(
  headerConstructor: (ITEM) -> HEADER,
  criteriaToAddHeader: (ITEM, ITEM) -> Boolean
): MutableList<SUPERTYPE> =
  foldIndexed(mutableListOf(), { index, acc, currentItem ->
    if (index == 0) {
      acc.add(headerConstructor.invoke(currentItem))
    }
    acc.add(currentItem)
    if (index < size - 1) {
      val nextItem = get(index + 1)
      if (criteriaToAddHeader.invoke(currentItem, nextItem)) {
        acc.add(headerConstructor.invoke(nextItem))
      }
    }
    acc
  })
