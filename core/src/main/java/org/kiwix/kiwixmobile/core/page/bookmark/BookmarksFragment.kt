package org.kiwix.kiwixmobile.core.page.bookmark

import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.coreActivityComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.page.PageFragment
import org.kiwix.kiwixmobile.core.page.adapter.PageAdapter
import org.kiwix.kiwixmobile.core.page.adapter.PageDelegate.PageItemDelegate
import org.kiwix.kiwixmobile.core.page.bookmark.viewmodel.BookmarkViewModel

class BookmarksFragment : PageFragment() {
  override val pageViewModel by lazy {
    requireActivity().viewModel<BookmarkViewModel>(
      viewModelFactory
    )
  }

  override val pageAdapter: PageAdapter by lazy {
    PageAdapter(PageItemDelegate(this))
  }

  override val title: String by lazy { getString(R.string.bookmarks) }
  override val noItemsString: String by lazy { getString(R.string.no_bookmarks) }
  override val switchString: String by lazy { getString(R.string.bookmarks_from_current_book) }
  override val switchIsChecked: Boolean by lazy { sharedPreferenceUtil.showBookmarksAllBooks }
  override fun inject(baseActivity: BaseActivity) {
    requireActivity().coreActivityComponent.inject(this)
  }

  override val searchQueryHint: String by lazy { getString(R.string.search_bookmarks) }
}
