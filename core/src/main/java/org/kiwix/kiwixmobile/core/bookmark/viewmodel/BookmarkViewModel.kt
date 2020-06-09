package org.kiwix.kiwixmobile.core.bookmark.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.schedulers.Schedulers
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.AllBookmarkPreferenceChanged
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ExitBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UpdateBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UserClickedConfirmDelete
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UserClickedDeleteButton
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UserClickedDeleteSelectedBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UserClickedShowAllToggle
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.DeleteBookmarkItems
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

  val state = MutableLiveData<State>().apply {
    value =
      State(emptyList(), sharedPreferenceUtil.showBookmarksAllBooks, zimReaderContainer.id, "")
  }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val compositeDisposable = CompositeDisposable()
  val showAllSwitchToggle =
    BehaviorProcessor.createDefault(sharedPreferenceUtil.showBookmarksAllBooks)

  init {
    compositeDisposable.addAll(
      viewStateReducer(),
      bookmarksDao.bookmarks().subscribeOn(Schedulers.io())
        .subscribe { actions.offer(UpdateBookmarks(it)) }
    )
  }

  private fun viewStateReducer() =
    actions.map { reduce(it, state.value!!) }.subscribe(state::postValue)

  private fun reduce(action: Action, state: State): State {
    val newState = when (action) {
      ExitBookmarks -> finishBookmarksActivity(state)
      ExitActionModeMenu -> deselectAllBookmarkItems(state)
      UserClickedConfirmDelete -> offerDeletionOfItems(state)
      UserClickedDeleteButton -> offerShowDeleteDialog(state)
      UserClickedDeleteSelectedBookmarks -> offerShowDeleteDialog(state)
      is OnItemClick -> handleItemClick(state, action)
      is OnItemLongClick -> handleItemLongClick(state, action)
      is UserClickedShowAllToggle -> offerUpdateToShowAllToggle(action, state)
      is Filter -> setSearchTerm(state, action)
      is UpdateBookmarks -> updateBookmarksList(state, action)
      is AllBookmarkPreferenceChanged -> changeShowBookmarksToggle(state, action)
    }
    Log.d(TAG, "New bookmark state: $newState")
    return newState
  }

  private fun setSearchTerm(state: State, action: Filter) =
    state.copy(searchTerm = action.searchTerm)

  private fun changeShowBookmarksToggle(
    state: State,
    action: AllBookmarkPreferenceChanged
  ): State = state.copy(showAll = action.showAll)

  private fun updateBookmarksList(
    state: State,
    action: UpdateBookmarks
  ): State = state.copy(bookmarks = action.bookmarks)

  private fun offerUpdateToShowAllToggle(
    action: UserClickedShowAllToggle,
    state: State
  ): State {
    effects.offer(
      UpdateAllBookmarksPreference(
        sharedPreferenceUtil,
        action.isChecked
      )
    )
    return state.copy(showAll = action.isChecked)
  }

  private fun handleItemLongClick(
    state: State,
    action: OnItemLongClick
  ): State {
    if (state.isInSelectionState) {
      return state
    }
    return state.toggleSelectionOfItem(action.bookmark)
  }

  private fun handleItemClick(
    state: State,
    action: OnItemClick
  ): State {
    if (state.isInSelectionState) {
      return state.toggleSelectionOfItem(action.bookmark)
    }
    effects.offer(OpenBookmark(action.bookmark, zimReaderContainer))
    return state
  }

  private fun offerShowDeleteDialog(state: State): State {
    effects.offer(ShowDeleteBookmarksDialog(actions))
    return state
  }

  private fun offerDeletionOfItems(state: State): State {
    effects.offer(DeleteBookmarkItems(state, bookmarksDao))
    return state
  }

  private fun deselectAllBookmarkItems(state: State): State =
    state.copy(bookmarks = state.bookmarks.map { it.copy(isSelected = false) })

  private fun finishBookmarksActivity(state: State): State {
    effects.offer(Finish)
    return state
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }
}
