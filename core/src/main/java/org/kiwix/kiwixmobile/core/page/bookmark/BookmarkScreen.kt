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

package org.kiwix.kiwixmobile.core.page.bookmark

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.PageScreenRoute
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkViewModel

@Composable
fun BookmarkScreenRoute(
  navigateBack: () -> Unit,
  viewModel: BookmarkViewModel,
  alertDialogShower: AlertDialogShower
) {
  PageScreenRoute(
    navigateBack = navigateBack,
    viewModel = viewModel,
    screenTitle = stringResource(R.string.bookmarks),
    noItemsString = stringResource(R.string.no_bookmarks),
    switchString = stringResource(R.string.bookmarks_from_current_book),
    searchQueryHint = stringResource(R.string.search_bookmarks),
    deleteIconTitle = R.string.pref_clear_all_bookmarks_title,
    alertDialogShower = alertDialogShower,
    pageViewModelClickListener = null
  )
}
