/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.page.viewmodel

import io.mockk.mockk
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.dao.PageDao
import org.kiwix.kiwixmobile.core.page.adapter.Page
import org.kiwix.kiwixmobile.core.page.adapter.PageRelated
import org.kiwix.kiwixmobile.core.page.pageState
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil

class TestablePageViewModel(
  zimReaderContainer: ZimReaderContainer,
  sharedPreferenceUtil: SharedPreferenceUtil,
  val dao: PageDao
) : PageViewModel<Page, TestablePageState>(dao, sharedPreferenceUtil, zimReaderContainer) {

  override fun initialState(): TestablePageState = pageState()

  override fun updatePagesBasedOnFilter(
    state: TestablePageState,
    action: Action.Filter
  ): TestablePageState = state

  override fun updatePages(
    state: TestablePageState,
    action: Action.UpdatePages
  ): TestablePageState = state

  override fun offerUpdateToShowAllToggle(
    action: Action.UserClickedShowAllToggle,
    state: TestablePageState
  ): TestablePageState = state

  override fun copyWithNewItems(
    state: TestablePageState,
    newItems: List<Page>
  ): TestablePageState =
    state

  override fun deselectAllPages(state: TestablePageState): TestablePageState = state
  override fun createDeletePageDialogEffect(state: TestablePageState): SideEffect<*> = mockk()
}

data class TestablePageState(
  override val pageItems: List<Page> = emptyList(),
  override val visiblePageItems: List<PageRelated> = pageItems,
  override val showAll: Boolean = true,
  override val currentZimId: String? = "currentZimId",
  override val searchTerm: String = ""
) : PageState<Page>() {
  override fun copyWithNewItems(newItems: List<Page>): PageState<Page> =
    TestablePageState(pageItems = pageItems)
}
