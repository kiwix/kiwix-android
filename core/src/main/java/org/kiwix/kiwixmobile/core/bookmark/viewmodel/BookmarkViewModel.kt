package org.kiwix.kiwixmobile.core.bookmark.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
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
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.DeleteBookmarkItems
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.OpenBookmark
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.ShowDeleteBookmarksDialog
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.UpdateAllBookmarksPreference
import org.kiwix.kiwixmobile.core.dao.NewBookmarksDao
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class BookmarkViewModel @Inject constructor(
  private val bookmarksDao: NewBookmarksDao,
  private val zimReaderContainer: ZimReaderContainer,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : ViewModel() {
  val state = MutableLiveData<State>().apply {
    value =
      Results(emptyList(), sharedPreferenceUtil.showBookmarksAllBooks, zimReaderContainer.id, "")
  }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val compositeDisposable = CompositeDisposable()
  val showAllSwitchToggle =
    BehaviorProcessor.createDefault(sharedPreferenceUtil.showBookmarksAllBooks)

  init {
    compositeDisposable.addAll(
      bookmarksDao.bookmarks().subscribe { actions.offer(UpdateBookmarks(it)) },
      viewStateReducer()
    )
  }

  private fun viewStateReducer() =
    actions.map { reduce(it, state.value!!) }.subscribe(state::postValue)

  private fun reduce(action: Action, state: State): State =
    when (action) {
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

  private fun setSearchTerm(state: State, action: Filter) =
    when (state) {
      is Results -> state.copy(searchTerm = action.searchTerm)
      is SelectionResults -> state.copy(searchTerm = action.searchTerm)
    }

  private fun changeShowBookmarksToggle(
    state: State,
    action: AllBookmarkPreferenceChanged
  ): State {
    return when (state) {
      is SelectionResults -> state
      is Results -> state.copy(showAll = action.showAll)
    }
  }

  private fun updateBookmarksList(
    state: State,
    action: UpdateBookmarks
  ): State = when (state) {
    is Results -> state.copy(bookmarks = action.bookmarks)
    is SelectionResults -> Results(
      action.bookmarks,
      state.showAll,
      zimReaderContainer.id,
      state.searchTerm
    )
  }

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
    return when (state) {
      is Results -> state.copy(showAll = action.isChecked)
      else -> state
    }
  }

  private fun handleItemLongClick(
    state: State,
    action: OnItemLongClick
  ): State {
    return when (state) {
      is Results -> state.toggleSelectionOfItem(action.bookmark)
      else -> state
    }
  }

  private fun handleItemClick(
    state: State,
    action: OnItemClick
  ): State {
    return when (state) {
      is Results -> {
        effects.offer(OpenBookmark(action.bookmark, zimReaderContainer))
        state
      }
      is SelectionResults -> state.toggleSelectionOfItem(action.bookmark)
    }
  }

  private fun offerShowDeleteDialog(state: State): State {
    effects.offer(ShowDeleteBookmarksDialog(actions))
    return state
  }

  private fun offerDeletionOfItems(state: State): State {
    return when (state) {
      is Results -> {
        effects.offer(DeleteBookmarkItems(state.bookmarks, bookmarksDao))
        state
      }
      is SelectionResults -> {
        effects.offer(DeleteBookmarkItems(state.selectedItems, bookmarksDao))
        state
      }
    }
  }

  private fun deselectAllBookmarkItems(state: State): State {
    return when (state) {
      is SelectionResults -> {
        Results(
          state.bookmarks.map { it.copy(isSelected = false) },
          state.showAll,
          state.currentZimId,
          state.searchTerm
        )
      }
      else -> state
    }
  }

  private fun finishBookmarksActivity(state: State): State {
    effects.offer(Finish)
    return state
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }
}
