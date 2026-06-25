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

package org.kiwix.kiwixmobile.nav.destination.reader

import android.app.Application
import androidx.navigation.NavOptions
import org.kiwix.kiwixmobile.core.main.reader.CoreReaderViewModel
import org.kiwix.kiwixmobile.core.main.reader.helper.ReaderWebViewManager
import org.kiwix.kiwixmobile.core.main.reader.helper.ZimFileManager
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.ExternalLinkOpener
import org.kiwix.kiwixmobile.core.utils.KiwixPermissionChecker
import org.kiwix.kiwixmobile.core.utils.datastore.KiwixDataStore
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.UnsupportedMimeTypeHandler
import org.kiwix.kiwixmobile.ui.KiwixDestination
import javax.inject.Inject

class KiwixReaderViewModel @Inject constructor(
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
  override fun shouldShowSpellCheckedSuggestions(): Boolean = false
  override fun isBrandedApp(): Boolean = false
  override fun openSearch(
    searchString: String,
    isOpenedFromTabView: Boolean,
    isVoice: Boolean
  ) {
    emitEffect(
      ReaderEffect.NavigateTo(
        KiwixDestination.Search.createRoute(
          searchString = searchString,
          isOpenedFromTabView = isOpenedFromTabView,
          isVoice = isVoice
        ),
        NavOptions.Builder().setPopUpTo(KiwixDestination.Search.route, inclusive = true).build()
      )
    )
  }

  override fun invalidZimFileFound(onInvalidZimFileFound: () -> Unit) {
    // Invoke the function so that it can show toast message to user.
    runCatching { onInvalidZimFileFound.invoke() }
  }
}
