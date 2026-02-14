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

package org.kiwix.kiwixmobile.core.page.history

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.page.PageScreenRoute
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryViewModel

@Composable
fun HistoryScreenRoute(
  navigateBack: () -> Unit,
  viewModel: HistoryViewModel,
  alertDialogShower: AlertDialogShower
) {
  PageScreenRoute(
    navigateBack = navigateBack,
    viewModel = viewModel,
    screenTitle = stringResource(R.string.history),
    noItemsString = stringResource(R.string.no_history),
    switchString = stringResource(R.string.history_from_current_book),
    searchQueryHint = stringResource(R.string.search_history),
    deleteIconTitle = R.string.pref_clear_all_history_title,
    alertDialogShower = alertDialogShower,
    pageViewModelClickListener = null
  )
}

