package org.kiwix.kiwixmobile.core.page.history

import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.page.PageActivity
import org.kiwix.kiwixmobile.core.page.adapter.PageAdapter
import org.kiwix.kiwixmobile.core.page.adapter.PageDelegate.HistoryDateDelegate
import org.kiwix.kiwixmobile.core.page.adapter.PageDelegate.PageItemDelegate
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryViewModel

const val USER_CLEARED_HISTORY: String = "user_cleared_history"

class HistoryActivity : PageActivity() {
  override val pageViewModel by lazy { viewModel<HistoryViewModel>(viewModelFactory) }

  override val pageAdapter by lazy {
    PageAdapter(PageItemDelegate(this), HistoryDateDelegate())
  }

  override fun injection(coreComponent: CoreComponent) {
    activityComponent.inject(this)
  }

  override val noItemsString: String by lazy { getString(R.string.no_history) }
  override val switchString: String by lazy { getString(R.string.history_from_current_book) }
  override val title: String by lazy { getString(R.string.history) }
  override val switchIsChecked: Boolean by lazy { sharedPreferenceUtil.showHistoryAllBooks }
  override val searchQueryHint: String by lazy { getString(R.string.search_history) }
}
