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

package org.kiwix.kiwixmobile.core.utils.datastore

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.ThemeConfig
import org.kiwix.kiwixmobile.core.zim_manager.Category
import org.kiwix.kiwixmobile.core.zim_manager.Language
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class KiwixDataStoreTest {
  private lateinit var context: Context
  private lateinit var kiwixDataStore: KiwixDataStore

  @Before
  fun setUp() = runTest {
    context = ApplicationProvider.getApplicationContext()
    context.kiwixDataStore.edit { it.clear() }
    kiwixDataStore = KiwixDataStore(context)
  }

  private fun expectedDefaultPublicStorage(storageContext: Context): String {
    val defaultPublicStorage =
      ContextWrapper(storageContext).externalMediaDirs.firstOrNull()?.path
        ?: storageContext.filesDir.path
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      defaultPublicStorage
    } else {
      defaultPublicStorage.substringBefore("/Android")
    }
  }

  private suspend fun storageAwareDataStore(): Pair<Context, KiwixDataStore> {
    val storageContext = object : ContextWrapper(context) {
      private val externalMediaDir = File(filesDir, "test-external-media").apply { mkdirs() }
      private val externalFilesDir = File(filesDir, "test-external-files").apply { mkdirs() }

      override fun getExternalMediaDirs(): Array<File> = arrayOf(externalMediaDir)
      override fun getExternalFilesDirs(type: String?): Array<File> = arrayOf(externalFilesDir)
    }
    storageContext.kiwixDataStore.edit { it.clear() }
    return storageContext to KiwixDataStore(storageContext)
  }

  @Test
  fun `textZoom returns 100 by default`() = runTest {
    kiwixDataStore.textZoom.test {
      assertThat(awaitItem()).isEqualTo(KiwixDataStore.DEFAULT_ZOOM)
    }
  }

  @Test
  fun `setTextZoom persists the given value`() = runTest {
    kiwixDataStore.setTextZoom(150)
    kiwixDataStore.textZoom.test {
      assertThat(awaitItem()).isEqualTo(150)
    }
  }

  @Test
  fun `textZoom emits updated value when changed`() = runTest {
    kiwixDataStore.textZoom.test {
      assertThat(awaitItem()).isEqualTo(KiwixDataStore.DEFAULT_ZOOM)
      kiwixDataStore.setTextZoom(180)
      assertThat(awaitItem()).isEqualTo(180)
    }
  }

  @Test
  fun `currentZimFile returns null by default`() = runTest {
    kiwixDataStore.currentZimFile.test {
      assertThat(awaitItem()).isNull()
    }
  }

  @Test
  fun `setCurrentZimFile stores the file path`() = runTest {
    val path = "/storage/emulated/0/Android/media/org.kiwix.kiwixmobile/kiwix"

    kiwixDataStore.setCurrentZimFile(path)
    kiwixDataStore.currentZimFile.test {
      assertThat(awaitItem()).isEqualTo(path)
    }
  }

  @Test
  fun `setCurrentZimFile overwrites existing file path`() = runTest {
    val path1 = "/storage/emulated/0/Android/media/org.kiwix.kiwixmobile/kiwix1"
    val path2 = "/storage/emulated/0/Android/media/org.kiwix.kiwixmobile/kiwix2"

    kiwixDataStore.setCurrentZimFile(path1)
    kiwixDataStore.setCurrentZimFile(path2)
    kiwixDataStore.currentZimFile.test {
      assertThat(awaitItem()).isEqualTo(path2)
    }
  }

  @Test
  fun `currentTab returns null by default`() = runTest {
    kiwixDataStore.currentTab.test {
      assertThat(awaitItem()).isNull()
    }
  }

  @Test
  fun `setCurrentTab persists the tab index`() = runTest {
    kiwixDataStore.setCurrentTab(3)
    kiwixDataStore.currentTab.test {
      assertThat(awaitItem()).isEqualTo(3)
    }
  }

  @Test
  fun `backToTop returns false by default`() = runTest {
    assertThat(kiwixDataStore.backToTop.first()).isFalse()
  }

  @Test
  fun `setPrefBackToTop toggles backToTop`() = runTest {
    kiwixDataStore.setPrefBackToTop(true)
    assertThat(kiwixDataStore.backToTop.first()).isTrue()
  }

  @Test
  fun `openNewTabInBackground returns false by default`() = runTest {
    assertThat(kiwixDataStore.openNewTabInBackground.first()).isFalse()
  }

  @Test
  fun `setOpenNewInBackground toggles openNewTabInBackground`() = runTest {
    kiwixDataStore.setOpenNewInBackground(true)
    assertThat(kiwixDataStore.openNewTabInBackground.first()).isTrue()
  }

  @Test
  fun `externalLinkPopup returns true by default`() = runTest {
    assertThat(kiwixDataStore.externalLinkPopup.first()).isTrue()
  }

  @Test
  fun `setExternalLinkPopup can disable popup`() = runTest {
    kiwixDataStore.setExternalLinkPopup(false)
    assertThat(kiwixDataStore.externalLinkPopup.first()).isFalse()
  }

  @Test
  fun `wifiOnly returns true by default`() = runTest {
    assertThat(kiwixDataStore.wifiOnly.first()).isTrue()
  }

  @Test
  fun `setWifiOnly can disable wifi only`() = runTest {
    kiwixDataStore.setWifiOnly(false)
    assertThat(kiwixDataStore.wifiOnly.first()).isFalse()
  }

  @Test
  fun `showIntro returns true by default`() = runTest {
    assertThat(kiwixDataStore.showIntro.first()).isTrue()
  }

  @Test
  fun `setIntroShown marks intro as shown by default`() = runTest {
    kiwixDataStore.setIntroShown()
    assertThat(kiwixDataStore.showIntro.first()).isFalse()
  }

  @Test
  fun `setIntroShown with true re-enables intro`() = runTest {
    kiwixDataStore.setIntroShown()
    kiwixDataStore.setIntroShown(isShown = true)
    assertThat(kiwixDataStore.showIntro.first()).isTrue()
  }

  @Test
  fun `showShowCaseToUser returns true by default`() = runTest {
    assertThat(kiwixDataStore.showShowCaseToUser.first()).isTrue()
  }

  @Test
  fun `setShowCaseViewForFileTransferShown marks showcase as shown`() = runTest {
    kiwixDataStore.setShowCaseViewForFileTransferShown()
    assertThat(kiwixDataStore.showShowCaseToUser.first()).isFalse()
  }

  @Test
  fun `setShowCaseViewForFileTransferShown can enable showcase again`() = runTest {
    kiwixDataStore.setShowCaseViewForFileTransferShown()
    kiwixDataStore.setShowCaseViewForFileTransferShown(true)
    assertThat(kiwixDataStore.showShowCaseToUser.first()).isTrue()
  }

  @Test
  fun `isBookmarksMigrated returns false by default`() = runTest {
    assertThat(kiwixDataStore.isBookmarksMigrated.first()).isFalse()
  }

  @Test
  fun `setBookMarkMigrated sets migration flag`() = runTest {
    kiwixDataStore.setBookMarkMigrated(true)
    assertThat(kiwixDataStore.isBookmarksMigrated.first()).isTrue()
  }

  @Test
  fun `isRecentSearchMigrated returns false by default`() = runTest {
    assertThat(kiwixDataStore.isRecentSearchMigrated.first()).isFalse()
  }

  @Test
  fun `setRecentSearchMigrated sets migration flag`() = runTest {
    kiwixDataStore.setRecentSearchMigrated(true)
    assertThat(kiwixDataStore.isRecentSearchMigrated.first()).isTrue()
  }

  @Test
  fun `isNotesMigrated returns false by default`() = runTest {
    assertThat(kiwixDataStore.isNotesMigrated.first()).isFalse()
  }

  @Test
  fun `setNotesMigrated sets migration flag`() = runTest {
    kiwixDataStore.setNotesMigrated(true)
    assertThat(kiwixDataStore.isNotesMigrated.first()).isTrue()
  }

  @Test
  fun `isHistoryMigrated returns false by default`() = runTest {
    assertThat(kiwixDataStore.isHistoryMigrated.first()).isFalse()
  }

  @Test
  fun `setHistoryMigrated sets migration flag`() = runTest {
    kiwixDataStore.setHistoryMigrated(true)
    assertThat(kiwixDataStore.isHistoryMigrated.first()).isTrue()
  }

  @Test
  fun `isAppDirectoryMigrated returns false by default`() = runTest {
    assertThat(kiwixDataStore.isAppDirectoryMigrated.first()).isFalse()
  }

  @Test
  fun `setAppDirectoryMigrated sets migration flag`() = runTest {
    kiwixDataStore.setAppDirectoryMigrated(true)
    assertThat(kiwixDataStore.isAppDirectoryMigrated.first()).isTrue()
  }

  @Test
  fun `isBookOnDiskMigrated returns false by default`() = runTest {
    assertThat(kiwixDataStore.isBookOnDiskMigrated.first()).isFalse()
  }

  @Test
  fun `setBookOnDiskMigrated sets migration flag`() = runTest {
    kiwixDataStore.setBookOnDiskMigrated(true)
    assertThat(kiwixDataStore.isBookOnDiskMigrated.first()).isTrue()
  }

  @Test
  fun `showHistoryOfAllBooks returns true by default`() = runTest {
    assertThat(kiwixDataStore.showHistoryOfAllBooks.first()).isTrue()
  }

  @Test
  fun `setShowHistoryOfAllBooks can disable`() = runTest {
    kiwixDataStore.setShowHistoryOfAllBooks(false)
    assertThat(kiwixDataStore.showHistoryOfAllBooks.first()).isFalse()
  }

  @Test
  fun `showBookmarksOfAllBooks returns true by default`() = runTest {
    assertThat(kiwixDataStore.showBookmarksOfAllBooks.first()).isTrue()
  }

  @Test
  fun `setShowBookmarksOfAllBooks can disable`() = runTest {
    kiwixDataStore.setShowBookmarksOfAllBooks(false)
    assertThat(kiwixDataStore.showBookmarksOfAllBooks.first()).isFalse()
  }

  @Test
  fun `showNotesOfAllBooks returns true by default`() = runTest {
    assertThat(kiwixDataStore.showNotesOfAllBooks.first()).isTrue()
  }

  @Test
  fun `setShowNotesOfAllBooks can disable`() = runTest {
    kiwixDataStore.setShowNotesOfAllBooks(false)
    assertThat(kiwixDataStore.showNotesOfAllBooks.first()).isFalse()
  }

  @Test
  fun `hostedBookIds defaults to empty set`() = runTest {
    assertThat(kiwixDataStore.hostedBookIds.first()).isEmpty()
  }

  @Test
  fun `setHostedBookIds stores the given ids`() = runTest {
    val ids = setOf("book1", "book2", "book3")
    kiwixDataStore.setHostedBookIds(ids)
    assertThat(kiwixDataStore.hostedBookIds.first()).isEqualTo(ids)
  }

  @Test
  fun `laterClickedMilliSeconds returns zero by default`() = runTest {
    assertThat(kiwixDataStore.laterClickedMilliSeconds.first()).isEqualTo(0L)
  }

  @Test
  fun `setLaterClickedMilliSeconds stores the timestamp`() = runTest {
    val millis = 1L
    kiwixDataStore.setLaterClickedMilliSeconds(millis)
    assertThat(kiwixDataStore.laterClickedMilliSeconds.first()).isEqualTo(millis)
  }

  @Test
  fun `lastDonationPopupShownInMilliSeconds returns zero by default`() = runTest {
    assertThat(kiwixDataStore.lastDonationPopupShownInMilliSeconds.first()).isEqualTo(0L)
  }

  @Test
  fun `setLastDonationPopupShownInMilliSeconds stores the timestamp`() = runTest {
    val millis = 1L
    kiwixDataStore.setLastDonationPopupShownInMilliSeconds(millis)
    assertThat(kiwixDataStore.lastDonationPopupShownInMilliSeconds.first()).isEqualTo(millis)
  }

  @Test
  fun `isScanFileSystemDialogShown returns false by default`() = runTest {
    assertThat(kiwixDataStore.isScanFileSystemDialogShown.first()).isFalse()
  }

  @Test
  fun `setIsScanFileSystemDialogShown sets value`() = runTest {
    kiwixDataStore.setIsScanFileSystemDialogShown(true)
    assertThat(kiwixDataStore.isScanFileSystemDialogShown.first()).isTrue()
  }

  @Test
  fun `isScanFileSystemTest returns false by default`() = runTest {
    assertThat(kiwixDataStore.isScanFileSystemTest.first()).isFalse()
  }

  @Test
  fun `setIsScanFileSystemTest updates isScanFileSystemTest`() = runTest {
    kiwixDataStore.setIsScanFileSystemTest(true)
    assertThat(kiwixDataStore.isScanFileSystemTest.first()).isTrue()
  }

  @Test
  fun `showManageExternalFilesPermissionDialog returns true by default`() = runTest {
    assertThat(kiwixDataStore.showManageExternalFilesPermissionDialog.first()).isTrue()
  }

  @Test
  fun `setShowManageExternalFilesPermissionDialog can disable`() = runTest {
    kiwixDataStore.setShowManageExternalFilesPermissionDialog(false)
    assertThat(kiwixDataStore.showManageExternalFilesPermissionDialog.first()).isFalse()
  }

  @Test
  fun `showManageExternalFilesPermissionDialogOnRefresh returns true by default`() = runTest {
    assertThat(kiwixDataStore.showManageExternalFilesPermissionDialogOnRefresh.first()).isTrue()
  }

  @Test
  fun `setManageExternalFilesPermissionDialogOnRefresh can disable dialog on refresh`() = runTest {
    kiwixDataStore.setManageExternalFilesPermissionDialogOnRefresh(false)
    assertThat(kiwixDataStore.showManageExternalFilesPermissionDialogOnRefresh.first()).isFalse()
  }

  @Test
  fun `showStorageOption returns true by default`() = runTest {
    assertThat(kiwixDataStore.showStorageOption.first()).isTrue()
  }

  @Test
  fun `setShowStorageOption can disable`() = runTest {
    kiwixDataStore.setShowStorageOption(false)
    assertThat(kiwixDataStore.showStorageOption.first()).isFalse()
  }

  @Test
  fun `shouldShowStorageSelectionDialogOnCopyMove returns true by default`() = runTest {
    assertThat(kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove.first()).isTrue()
  }

  @Test
  fun `setShowStorageSelectionDialogOnCopyMove can disable`() = runTest {
    kiwixDataStore.setShowStorageSelectionDialogOnCopyMove(false)
    assertThat(kiwixDataStore.shouldShowStorageSelectionDialogOnCopyMove.first()).isFalse()
  }

  @Test
  fun `isFirstRun defaults to true`() = runTest {
    assertThat(kiwixDataStore.isFirstRun.first()).isTrue()
  }

  @Test
  fun `setIsFirstRun can mark as not first run`() = runTest {
    kiwixDataStore.setIsFirstRun(false)
    assertThat(kiwixDataStore.isFirstRun.first()).isFalse()
  }

  @Test
  fun `isPlayStoreBuild returns false by default`() = runTest {
    assertThat(kiwixDataStore.isPlayStoreBuild.first()).isFalse()
  }

  @Test
  fun `setIsPlayStoreBuild marks as play store build`() = runTest {
    kiwixDataStore.setIsPlayStoreBuild(true)
    assertThat(kiwixDataStore.isPlayStoreBuild.first()).isTrue()
  }

  @Test
  fun `prefIsTest returns false by default`() = runTest {
    assertThat(kiwixDataStore.prefIsTest.first()).isFalse()
  }

  @Test
  fun `setPrefIsTest marks as test`() = runTest {
    kiwixDataStore.setPrefIsTest(true)
    assertThat(kiwixDataStore.prefIsTest.first()).isTrue()
  }

  @Test
  fun `perAppLanguageMigrated returns false by default`() = runTest {
    assertThat(kiwixDataStore.perAppLanguageMigrated.first()).isFalse()
  }

  @Test
  fun `putPerAppLanguageMigration sets migration flag`() = runTest {
    kiwixDataStore.putPerAppLanguageMigration(true)
    assertThat(kiwixDataStore.perAppLanguageMigrated.first()).isTrue()
  }

  @Test
  fun `prefLanguage defaults to root locale`() = runTest {
    assertThat(kiwixDataStore.prefLanguage.first()).isEqualTo(Locale.ROOT.toString())
  }

  @Test
  fun `setPrefLanguage stores the selected language`() = runTest {
    kiwixDataStore.setPrefLanguage("fr")
    assertThat(kiwixDataStore.prefLanguage.first()).isEqualTo("fr")
  }

  @Test
  fun `selectedOnlineContentLanguage returns empty by default`() = runTest {
    assertThat(kiwixDataStore.selectedOnlineContentLanguage.first()).isEmpty()
  }

  @Test
  fun `setSelectedOnlineContentLanguage stores the value`() = runTest {
    kiwixDataStore.setSelectedOnlineContentLanguage("en")
    assertThat(kiwixDataStore.selectedOnlineContentLanguage.first()).isEqualTo("en")
  }

  @Test
  fun `selectedOnlineContentCategory returns empty by default`() = runTest {
    assertThat(kiwixDataStore.selectedOnlineContentCategory.first()).isEmpty()
  }

  @Test
  fun `setSelectedOnlineContentCategory stores the value`() = runTest {
    kiwixDataStore.setSelectedOnlineContentCategory("wikipedia")
    assertThat(kiwixDataStore.selectedOnlineContentCategory.first()).isEqualTo("wikipedia")
  }

  @Test
  fun `selectedStoragePosition returns zero by default`() = runTest {
    assertThat(kiwixDataStore.selectedStoragePosition.first()).isEqualTo(0)
  }

  @Test
  fun `setSelectedStoragePosition stores position`() = runTest {
    kiwixDataStore.setSelectedStoragePosition(2)
    assertThat(kiwixDataStore.selectedStoragePosition.first()).isEqualTo(2)
  }

  @Test
  fun `appTheme defaults to SYSTEM (follow system)`() = runTest {
    kiwixDataStore.appTheme.test {
      assertThat(awaitItem()).isEqualTo(ThemeConfig.Theme.SYSTEM)
    }
  }

  @Test
  fun `updateAppTheme stores DARK mode correctly`() = runTest {
    kiwixDataStore.updateAppTheme(AppCompatDelegate.MODE_NIGHT_YES.toString())
    kiwixDataStore.appTheme.test {
      assertThat(awaitItem()).isEqualTo(ThemeConfig.Theme.DARK)
    }
  }

  @Test
  fun `updateAppTheme stores LIGHT mode correctly`() = runTest {
    kiwixDataStore.updateAppTheme(AppCompatDelegate.MODE_NIGHT_NO.toString())
    kiwixDataStore.appTheme.test {
      assertThat(awaitItem()).isEqualTo(ThemeConfig.Theme.LIGHT)
    }
  }

  @Test
  fun `isPlayStoreBuildWithAndroid11OrAbove returns false when not play store`() = runTest {
    kiwixDataStore.setIsPlayStoreBuild(false)
    assertThat(kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()).isFalse()
  }

  @Test
  fun `isPlayStoreBuildWithAndroid11OrAbove returns true when play store on R`() = runTest {
    kiwixDataStore.setIsPlayStoreBuild(true)
    assertThat(kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()).isTrue()
  }

  @Test
  fun `isNotPlayStoreBuildWithAndroid11OrAbove returns true when not play store`() = runTest {
    kiwixDataStore.setIsPlayStoreBuild(false)
    assertThat(kiwixDataStore.isNotPlayStoreBuildWithAndroid11OrAbove()).isTrue()
  }

  @Test
  fun `isNotPlayStoreBuildWithAndroid11OrAbove returns false when play store`() = runTest {
    kiwixDataStore.setIsPlayStoreBuild(true)
    assertThat(kiwixDataStore.isNotPlayStoreBuildWithAndroid11OrAbove()).isFalse()
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.Q])
  fun `isPlayStoreBuildWithAndroid11OrAbove stays false below R`() = runTest {
    kiwixDataStore.setIsPlayStoreBuild(true)
    assertThat(kiwixDataStore.isPlayStoreBuildWithAndroid11OrAbove()).isFalse()
  }

  @Test
  fun `cachedLanguageList returns null when nothing saved`() = runTest {
    assertThat(kiwixDataStore.cachedLanguageList.first()).isNull()
  }

  @Test
  fun `saveLanguageList and cachedLanguageList round-trip correctly`() = runTest {
    kiwixDataStore.setSelectedOnlineContentLanguage("eng")

    val languages = listOf(
      Language(
        languageCode = "eng",
        active = true,
        occurrencesOfLanguage = 42,
        id = 1L
      ),
      Language(
        languageCode = "fra",
        active = false,
        occurrencesOfLanguage = 10,
        id = 2L
      )
    )
    kiwixDataStore.saveLanguageList(languages)
    val loaded = kiwixDataStore.cachedLanguageList.first()
    assertThat(loaded).isNotNull()
    assertThat(loaded).hasSize(2)
    loaded!!
    assertThat(loaded[0].languageCode).isEqualTo("eng")
    assertThat(loaded[0].occurencesOfLanguage).isEqualTo(42)
    assertThat(loaded[0].id).isEqualTo(1L)
    assertThat(loaded[0].active).isTrue()
    assertThat(loaded[1].languageCode).isEqualTo("fra")
    assertThat(loaded[1].occurencesOfLanguage).isEqualTo(10)
    assertThat(loaded[1].id).isEqualTo(2L)
    assertThat(loaded[1].active).isFalse()
  }

  @Test
  fun `cachedLanguageList follows the current selected language`() = runTest {
    kiwixDataStore.setSelectedOnlineContentLanguage("eng")
    kiwixDataStore.saveLanguageList(
      listOf(
        Language(languageCode = "eng", active = true, occurrencesOfLanguage = 42, id = 1L),
        Language(languageCode = "fra", active = false, occurrencesOfLanguage = 10, id = 2L)
      )
    )
    kiwixDataStore.setSelectedOnlineContentLanguage("fra")
    val loaded = kiwixDataStore.cachedLanguageList.first()
    assertThat(loaded).isNotNull()
    assertThat(loaded!!.first { it.languageCode == "eng" }.active).isFalse()
    assertThat(loaded.first { it.languageCode == "fra" }.active).isTrue()
  }

  @Test
  fun `cachedOnlineCategoryList returns null when nothing saved`() = runTest {
    assertThat(kiwixDataStore.cachedOnlineCategoryList.first()).isNull()
  }

  @Test
  fun `saveOnlineCategoryList and cachedOnlineCategoryList round-trip correctly`() = runTest {
    kiwixDataStore.setSelectedOnlineContentCategory("wikipedia")
    val categories = listOf(
      Category(category = "wikipedia", active = true, id = 1L),
      Category(category = "FreeCodeCamp", active = false, id = 2L)
    )
    kiwixDataStore.saveOnlineCategoryList(categories)
    val loaded = kiwixDataStore.cachedOnlineCategoryList.first()
    assertThat(loaded).isNotNull()
    assertThat(loaded).hasSize(2)
    loaded!!
    assertThat(loaded[0].category).isEqualTo("wikipedia")
    assertThat(loaded[0].id).isEqualTo(1L)
    assertThat(loaded[0].active).isTrue()
    assertThat(loaded[1].category).isEqualTo("FreeCodeCamp")
    assertThat(loaded[1].id).isEqualTo(2L)
    assertThat(loaded[1].active).isFalse()
  }

  @Test
  fun `cachedOnlineCategoryList follows the current selected category`() = runTest {
    kiwixDataStore.setSelectedOnlineContentCategory("wikipedia")
    kiwixDataStore.saveOnlineCategoryList(
      listOf(
        Category(category = "wikipedia", active = true, id = 1L),
        Category(category = "FreeCodeCamp", active = false, id = 2L)
      )
    )
    kiwixDataStore.setSelectedOnlineContentCategory("FreeCodeCamp")
    val loaded = kiwixDataStore.cachedOnlineCategoryList.first()
    assertThat(loaded).isNotNull()
    assertThat(loaded!!.first { it.category == "wikipedia" }.active).isFalse()
    assertThat(loaded.first { it.category == "FreeCodeCamp" }.active).isTrue()
  }

  @Test
  fun `setSelectedStorage persists path when directory exists`() = runTest {
    val tempDir = java.io.File.createTempFile("kiwix_test", "").apply {
      delete()
      mkdirs()
    }
    try {
      kiwixDataStore.setSelectedStorage(tempDir.absolutePath)
      val stored = kiwixDataStore.selectedStorage.first()
      assertThat(stored).isEqualTo(tempDir.absolutePath)
    } finally {
      tempDir.deleteRecursively()
    }
  }

  @Test
  fun `selectedStorage falls back to default public storage when stored path does not exist`() =
    runTest {
      val (storageContext, storageDataStore) = storageAwareDataStore()
      val missingStorage = java.io.File.createTempFile("missing_storage", "").apply {
        delete()
      }
      storageDataStore.setSelectedStorage(missingStorage.absolutePath)
      val selectedStorage = storageDataStore.selectedStorage.first()
      assertThat(selectedStorage).isEqualTo(expectedDefaultPublicStorage(storageContext))
    }

  @Test
  fun `selectedStorage resets selectedStoragePosition to zero when stored path is invalid`() =
    runTest {
      val (_, storageDataStore) = storageAwareDataStore()
      val missingStorage = java.io.File.createTempFile("missing_storage", "").apply {
        delete()
      }
      storageDataStore.setSelectedStorage(missingStorage.absolutePath)
      storageDataStore.setSelectedStoragePosition(3)
      storageDataStore.selectedStorage.first()
      assertThat(storageDataStore.selectedStoragePosition.first()).isZero()
    }

  @Test
  fun `selectedStorage keeps invalid stored path in preferences when falling back`() = runTest {
    val (storageContext, storageDataStore) = storageAwareDataStore()
    val missingStorage = java.io.File.createTempFile("missing_storage", "").apply {
      delete()
    }

    storageDataStore.setSelectedStorage(missingStorage.absolutePath)

    assertThat(storageDataStore.selectedStorage.first()).isEqualTo(
      expectedDefaultPublicStorage(storageContext)
    )
    assertThat(storageContext.kiwixDataStore.data.first()[PreferencesKeys.PREF_STORAGE])
      .isEqualTo(missingStorage.absolutePath)
  }

  @Test
  fun `selectedStorage stores default path on first read`() = runTest {
    val (storageContext, storageDataStore) = storageAwareDataStore()

    val selectedStorage = storageDataStore.selectedStorage.first()

    assertThat(selectedStorage).isEqualTo(expectedDefaultPublicStorage(storageContext))
    assertThat(storageContext.kiwixDataStore.data.first()[PreferencesKeys.PREF_STORAGE])
      .isEqualTo(expectedDefaultPublicStorage(storageContext))
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.Q])
  fun `getPublicDirectoryPath returns original path on Q and above`() = runTest {
    val path = "/storage/emulated/0/Android/media/org.kiwix.kiwixmobile"
    assertThat(kiwixDataStore.getPublicDirectoryPath(path)).isEqualTo(path)
  }

  @Test
  @Config(sdk = [Build.VERSION_CODES.P])
  fun `getPublicDirectoryPath removes Android segment below Q`() = runTest {
    val path = "/storage/emulated/0/Android/media/org.kiwix.kiwixmobile"
    assertThat(kiwixDataStore.getPublicDirectoryPath(path)).isEqualTo("/storage/emulated/0")
  }
}
