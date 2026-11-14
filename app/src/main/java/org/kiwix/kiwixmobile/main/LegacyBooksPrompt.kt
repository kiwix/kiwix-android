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

package org.kiwix.kiwixmobile.main

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.mhutti1.utils.storage.Bytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.utils.HUNDERED
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.files.LegacyBooksMover
import org.kiwix.kiwixmobile.nav.destination.library.local.CopyMoveProgressBarController
import javax.inject.Inject
import javax.inject.Singleton

/** Offers once per process to move books out of app storage, which uninstall deletes. */
@Singleton
class LegacyBooksPrompt @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val legacyBooksMover: LegacyBooksMover,
  private val progressBarController: CopyMoveProgressBarController
) {
  private var promptedThisSession = false

  suspend fun show(alertDialogShower: AlertDialogShower, scope: CoroutineScope) {
    if (promptedThisSession) return
    promptedThisSession = true
    legacyBooksMover.useDocumentsIfNothingToMove()
    val offer = legacyBooksMover.offer() ?: return
    alertDialogShower.show(
      KiwixDialog.MoveLegacyBooks(offer.books.size, Bytes(offer.totalBytes).humanReadable),
      { scope.launch { move(offer, alertDialogShower) } },
      { scope.launch { legacyBooksMover.decline() } }
    )
  }

  private suspend fun move(offer: LegacyBooksMover.Offer, alertDialogShower: AlertDialogShower) {
    progressBarController.setAlertDialogShower(alertDialogShower)
    progressBarController.showProgress(context.getString(R.string.moving_books_progress))
    val result = legacyBooksMover.move(offer) { done, total ->
      progressBarController.updateProgress(done * HUNDERED / total)
    }
    progressBarController.dismissCopyMoveProgressDialog()
    context.toast(context.getString(R.string.move_books_done, result.moved))
    if (result.failed > 0) {
      context.toast(context.getString(R.string.move_books_failed, result.failed))
    }
  }
}
