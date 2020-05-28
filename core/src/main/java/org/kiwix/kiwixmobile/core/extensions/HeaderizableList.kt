package org.kiwix.kiwixmobile.core.extensions

inline class HeaderizableList<SUPERTYPE, ITEM : SUPERTYPE, HEADER : SUPERTYPE>(
  val list: List<ITEM>
) {
  fun foldOverAddingHeaders(
    headerConstructor: (ITEM) -> HEADER,
    criteriaToAddHeader: (ITEM, ITEM) -> Boolean
  ): MutableList<SUPERTYPE> =
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
