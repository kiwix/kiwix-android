package org.kiwix.kiwixmobile.core.bookmark.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ExitBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UpdateBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UserClickedDeleteSelectedBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.OpenBookmark
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.ShowDeleteBookmarksDialog
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.UpdateAllBookmarksPreference
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

const val TAG: String = "BookmarkViewModel"

class BookmarkViewModel @Inject constructor(
  private val bookmarksDao: NewBookmarksDao,
  private val zimReaderContainer: ZimReaderContainer,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : ViewModel() {

  val state = MutableLiveData<BookmarkState>().apply {
    value =
      BookmarkState(emptyList(), sharedPreferenceUtil.showBookmarksAllBooks, zimReaderContainer.id)
  }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val compositeDisposable = CompositeDisposable()

  init {
    compositeDisposable.addAll(
      viewStateReducer(),
      bookmarksDao.bookmarks().subscribeOn(Schedulers.io())
        .subscribe({ actions.offer(UpdateBookmarks(it)) }, Throwable::printStackTrace)
    )
  }

  private fun viewStateReducer() =
    actions.map { reduce(it, state.value!!) }
      .subscribe(state::postValue, Throwable::printStackTrace)

  private fun reduce(action: Action, state: BookmarkState): BookmarkState = when (action) {
    ExitBookmarks -> finishBookmarkActivity(state)
    ExitActionModeMenu -> deselectAllBookmarks(state)
    UserClickedDeleteButton, UserClickedDeleteSelectedBookmarks -> offerShowDeleteDialog(state)
    is UserClickedShowAllToggle -> offerUpdateToShowAllToggle(action, state)
    is OnItemClick -> handleItemClick(state, action)
    is OnItemLongClick -> handleItemLongClick(state, action)
    is Filter -> updateBookmarksBasedOnFilter(state, action)
    is UpdateBookmarks -> updateBookmarks(state, action)
  }

  private fun updateBookmarksBasedOnFilter(state: BookmarkState, action: Filter) =
    state.copy(searchTerm = action.searchTerm)

  private fun updateBookmarks(state: BookmarkState, action: UpdateBookmarks): BookmarkState =
    state.copy(bookmarks = action.bookmarks)

  private fun offerUpdateToShowAllToggle(
    action: UserClickedShowAllToggle,
    state: BookmarkState
  ): BookmarkState {
    effects.offer(UpdateAllBookmarksPreference(sharedPreferenceUtil, action.isChecked))
    return state.copy(showAll = action.isChecked)
  }

  private fun handleItemLongClick(state: BookmarkState, action: OnItemLongClick): BookmarkState =
    state.toggleSelectionOfItem(action.bookmark)

  private fun handleItemClick(state: BookmarkState, action: OnItemClick): BookmarkState {
    if (state.isInSelectionState) {
      return state.toggleSelectionOfItem(action.bookmark)
    }
    effects.offer(OpenBookmark(action.bookmark, zimReaderContainer))
    return state
  }

  private fun offerShowDeleteDialog(state: BookmarkState): BookmarkState {
    effects.offer(ShowDeleteBookmarksDialog(effects, state, bookmarksDao))
    return state
  }

  private fun deselectAllBookmarks(state: BookmarkState): BookmarkState =
    state.copy(bookmarks = state.bookmarks.map { it.copy(isSelected = false) })

  private fun finishBookmarkActivity(state: BookmarkState): BookmarkState {
    effects.offer(Finish)
    return state
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }
}
