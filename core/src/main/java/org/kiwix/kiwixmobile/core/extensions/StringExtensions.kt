package org.kiwix.kiwixmobile.core.extensions

import java.text.Normalizer

private val COMBINING_MARKS_REGEX = "\\p{Mn}+".toRegex()

fun String.toSlug(): String =
  lowercase()
    .replace(" ", "-")
    .replace("/", "")
    .replace(":", "")

/**
 * Returns this string without its diacritical marks, e.g. "Français" becomes "Francais".
 */
fun String.removeDiacritics(): String =
  Normalizer.normalize(this, Normalizer.Form.NFD).replace(COMBINING_MARKS_REGEX, "")

/**
 * Checks if this string contains [other] while ignoring case and diacritics,
 * so that searching for "francais" also finds "Français".
 */
fun String.containsIgnoreCaseAndDiacritics(other: String): Boolean =
  removeDiacritics().contains(other.removeDiacritics(), ignoreCase = true)
