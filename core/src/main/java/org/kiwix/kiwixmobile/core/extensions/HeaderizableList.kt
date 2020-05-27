package org.kiwix.kiwixmobile.core.extensions

inline class HeaderizableList<T>(val list: List<T>) {
  fun <HEADER : T> foldOverAddingHeaders(
    headerConstructor: (T) -> HEADER,
    criteriaToAddHeader: (T, T) -> Boolean
  ): MutableList<T> =
    list.foldIndexed(mutableListOf(), { index, acc, currentItem ->
      if (index == 0) {
        acc.add(headerConstructor.invoke(currentItem))
      }
      acc.add(currentItem)
      if (index < list.size - 1) {
        val nextItem = list.get(index + 1)
        if (criteriaToAddHeader.invoke(currentItem, nextItem)) {
          acc.add(headerConstructor.invoke(nextItem))
        }
      }
      acc
    })
}
