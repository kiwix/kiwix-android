package org.kiwix.kiwixmobile.core.page.bookmark

import kotlinx.coroutines.flow.Flow
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.cachedComponent
import org.kiwix.kiwixmobile.core.extensions.viewModel
import org.kiwix.kiwixmobile.core.page.PageFragment
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkViewModel

class BookmarksFragment : PageFragment() {
  override val pageViewModel by lazy { viewModel<BookmarkViewModel>(viewModelFactory) }

  override val screenTitle: Int = R.string.bookmarks
  override val noItemsString: String by lazy { getString(R.string.no_bookmarks) }
  override val switchString: String by lazy { getString(R.string.bookmarks_from_current_book) }
  override val deleteIconTitle: Int by lazy {
    R.string.pref_clear_all_bookmarks_title
  }
  override val switchIsCheckedFlow: Flow<Boolean> by lazy { kiwixDataStore.showBookmarksOfAllBooks }

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override val searchQueryHint: String by lazy { getString(R.string.search_bookmarks) }
}
