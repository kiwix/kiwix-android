/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.mockk.clearMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import java.util.Locale

class LocaleHelperTest {
  private val context: Context = mockk(relaxed = true)
  private val kiwixDataStore: KiwixDataStore = mockk(relaxed = true)
  private val resources: Resources = mockk(relaxed = true)
  private val configuration: Configuration = Configuration()

  @BeforeEach
  fun setup() {
    clearMocks(kiwixDataStore)
    configuration.setLocales(LocaleList(Locale.ENGLISH))
    every { context.resources } returns resources
    every { resources.configuration } returns configuration
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
  }

  @AfterEach
  fun tearDown() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
  }

  @Test
  fun `getAppLocale returns application locale when set in AppCompatDelegate`() = runTest {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("fr"))

    val locale = LocaleHelper.getAppLocale(context, kiwixDataStore)

    assertEquals(Locale.FRENCH.language, locale.language)
  }

  @Test
  fun `getAppLocale returns datastore language when app locales is empty`() = runTest {
    every { kiwixDataStore.prefLanguage } returns flowOf("de")

    val locale = LocaleHelper.getAppLocale(context, kiwixDataStore)

    assertEquals(Locale.GERMAN.language, locale.language)
  }

  @Test
  fun `getAppLocale returns system locale when prefLanguage is empty`() = runTest {
    every { kiwixDataStore.prefLanguage } returns flowOf("")

    val locale = LocaleHelper.getAppLocale(context, kiwixDataStore)

    assertEquals(Locale.ENGLISH.language, locale.language)
  }

  @Test
  fun `getAppLocale returns system locale when prefLanguage is ROOT`() = runTest {
    every { kiwixDataStore.prefLanguage } returns flowOf(Locale.ROOT.toString())

    val locale = LocaleHelper.getAppLocale(context, kiwixDataStore)

    assertEquals(Locale.ENGLISH.language, locale.language)
  }

  @Test
  fun `syncPrefLanguageWithAppLocales resets stale prefLanguage when app locales is empty`() =
    runTest {
      every { kiwixDataStore.prefLanguage } returns flowOf("en")

      LocaleHelper.syncPrefLanguageWithAppLocales(
        kiwixDataStore,
        LocaleListCompat.getEmptyLocaleList()
      )

      coVerify { kiwixDataStore.setPrefLanguage(Locale.ROOT.language) }
    }

  @Test
  fun `syncPrefLanguageWithAppLocales stores app locale language when it differs`() = runTest {
    every { kiwixDataStore.prefLanguage } returns flowOf("en")

    LocaleHelper.syncPrefLanguageWithAppLocales(
      kiwixDataStore,
      LocaleListCompat.forLanguageTags("fr")
    )

    coVerify { kiwixDataStore.setPrefLanguage("fr") }
  }

  @Test
  fun `syncPrefLanguageWithAppLocales does nothing when already in sync`() = runTest {
    every { kiwixDataStore.prefLanguage } returns flowOf("fr")

    LocaleHelper.syncPrefLanguageWithAppLocales(
      kiwixDataStore,
      LocaleListCompat.forLanguageTags("fr")
    )

    coVerify(exactly = 0) { kiwixDataStore.setPrefLanguage(any()) }
  }

  @Test
  fun `getSystemLocale returns non-null locale`() {
    val locale = LocaleHelper.getSystemLocale(context)
    assertNotNull(locale)
  }
}
