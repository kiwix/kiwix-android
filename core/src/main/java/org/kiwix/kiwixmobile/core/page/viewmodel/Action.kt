package org.kiwix.kiwixmobile.core.page.viewmodel

import org.kiwix.kiwixmobile.core.page.Page

sealed class Action {
  object Exit : Action()
  object ExitActionModeMenu : Action()
  object UserClickedDeleteButton : Action()
  object UserClickedDeleteSelectedPages : Action()

  data class OnItemClick(val page: Page) : Action()
  data class OnItemLongClick(val page: Page) : Action()
  data class UserClickedShowAllToggle(val isChecked: Boolean) : Action()
  data class Filter(val searchTerm: String) : Action()
  data class UpdatePages(val pages: List<Page>) : Action()
}
