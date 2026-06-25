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

package org.kiwix.kiwixmobile.custom.main

import android.app.Application
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.extensions.update
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.custom.BuildConfig
import javax.inject.Inject

class BrandedReaderViewModel @Inject constructor(
  context: Application,
  kiwixDataStore: KiwixDataStore,
  externalLinkOpener: ExternalLinkOpener,
  unsupportedMimeTypeHandler: UnsupportedMimeTypeHandler,
  readerWebViewManager: ReaderWebViewManager,
  alertDialogShower: AlertDialogShower,
  zimReaderContainer: ZimReaderContainer,
  zimFileManager: ZimFileManager,
  kiwixPermissionChecker: KiwixPermissionChecker
) : CoreReaderViewModel(
  context,
  kiwixDataStore,
  externalLinkOpener,
  unsupportedMimeTypeHandler,
  readerWebViewManager,
  alertDialogShower,
  zimReaderContainer,
  zimFileManager,
  kiwixPermissionChecker
) {
  override fun shouldShowSpellCheckedSuggestions(): Boolean =
    BuildConfig.SHOW_SEARCH_SUGGESTIONS_SPELLCHECKED

  override fun isBrandedApp(): Boolean = true

  override fun invalidZimFileFound(onInvalidZimFileFound: () -> Unit) {
    openDownloadScreen()
  }

  private fun openDownloadScreen() {
    viewModelScope.launch {
      delay(OPENING_DOWNLOAD_SCREEN_DELAY)
      val navOptions = NavOptions.Builder()
        .setPopUpTo(CustomDestination.Reader.route, true)
        .build()
      emitEffect(ReaderEffect.NavigateTo(CustomDestination.Downloads.route, navOptions))
    }
  }

  /**
   * Overrides the method to configure the title of toolbar. When the "setting title" is disabled
   * in a custom app, this function set the empty toolbar title.
   */
  override suspend fun updateTitle() {
    if (BuildConfig.DISABLE_TITLE) {
      // Since we have increased the zone for triggering search suggestions (see https://github.com/kiwix/kiwix-android/pull/3566),
      // we need to set this title for handling the toolbar click,
      // even if it is empty. If we do not set up this title,
      // the search screen will open if the user clicks on the toolbar from the tabs screen.
      updateToolbarSearchPlaceholderVisibility(true)
    } else {
      updateToolbarSearchPlaceholderVisibility(false)
      super.updateTitle()
    }
  }

  private fun updateToolbarSearchPlaceholderVisibility(show: Boolean) {
    updateState {
      copy(searchPlaceHolderItemForBrandedApps = show)
    }
  }

  override fun openSearch(searchString: String, isOpenedFromTabView: Boolean, isVoice: Boolean) {
    emitEffect(
      ReaderEffect.NavigateTo(
        CustomDestination.Search.createRoute(
          searchString = searchString,
          isOpenedFromTabView = isOpenedFromTabView,
          isVoice = isVoice
        ),
        NavOptions.Builder().setPopUpTo(CustomDestination.Search.route, inclusive = true).build()
      )
    )
  }

  override fun showNoBookOpenViews() {
    updateState { copy(showNoBookOpenInReader = false) }
  }
}
