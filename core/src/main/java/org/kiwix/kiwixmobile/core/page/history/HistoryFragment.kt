package org.kiwix.kiwixmobile.core.page.history

import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.cachedComponent
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.page.PageFragment
import org.kiwix.kiwixmobile.core.page.history.viewmodel.HistoryViewModel

class HistoryFragment : PageFragment() {
  override val pageViewModel by lazy { viewModel<HistoryViewModel>(viewModelFactory) }

  override val noItemsString: String by lazy { getString(R.string.no_history) }
  override val switchString: String by lazy { getString(R.string.history_from_current_book) }
  override val screenTitle: Int = R.string.history
  override val deleteIconTitle: Int by lazy {
    R.string.pref_clear_all_history_title
  }
  override val switchIsChecked: Boolean by lazy { sharedPreferenceUtil.showHistoryAllBooks }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override val searchQueryHint: String by lazy { getString(R.string.search_history) }
}
