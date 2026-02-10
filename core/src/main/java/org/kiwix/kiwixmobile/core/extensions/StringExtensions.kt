package org.kiwix.kiwixmobile.core.extensions

fun String.toSlug(): String =
  lowercase()
    .replace(" ", "-")
    .replace("/", "")
    .replace(":", "")
