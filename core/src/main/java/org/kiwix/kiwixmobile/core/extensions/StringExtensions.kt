package org.kiwix.kiwixmobile.core.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import java.text.Normalizer
import java.util.Locale

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

@Composable
fun String.uppercaseWithAppLocale(): String =
  uppercase(ConfigurationCompat.getLocales(LocalConfiguration.current)[0] ?: Locale.getDefault())
