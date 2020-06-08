package org.kiwix.kiwixmobile.core.bookmark.viewmodel

import DeleteSelectedOrAllBookmarkItems
import OpenBookmark
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.PublishProcessor
import org.kiwix.kiwixmobile.core.base.SideEffect
import org.kiwix.kiwixmobile.core.bookmark.adapter.BookmarkItem
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.DeleteBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ExitActionModeMenu
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ExitBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.Filter
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.OnItemClick
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.OnItemLongClick
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.RequestDeleteAllBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.RequestDeleteSelectedBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.ToggleShowBookmarksFromAllBooks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.Action.UpdateBookmarks
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.NoResults
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.Results
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.State.SelectionResults
import org.kiwix.kiwixmobile.core.bookmark.viewmodel.effects.ToggleShowAllBookmarksSwitchAndSaveItsStateToPrefs
import org.kiwix.kiwixmobile.core.data.Repository
import org.kiwix.kiwixmobile.core.history.viewmodel.effects.ShowDeleteBookmarkDialog
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.search.viewmodel.effects.Finish
import org.kiwix.kiwixmobile.core.utils.KiwixDialog
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import javax.inject.Inject

class BookmarkViewModel @Inject constructor(
  private val bookmarksDataSource: Repository,
  private val zimReaderContainer: ZimReaderContainer,
  private val sharedPreferenceUtil: SharedPreferenceUtil
) : ViewModel() {
  val state = MutableLiveData<State>().apply { value = NoResults(emptyList()) }
  val effects = PublishProcessor.create<SideEffect<*>>()
  val actions = PublishProcessor.create<Action>()
  private val filter = BehaviorProcessor.createDefault("")
  private var latestSearchString = ""
  private val compositeDisposable = CompositeDisposable()
  private val showAllSwitchToggle =
    BehaviorProcessor.createDefault(sharedPreferenceUtil.showBookmarksAllBooks)

  init {
    compositeDisposable.addAll(
      updateBookmarksIfSwitchIsToggled(),
      actionMapper(),
      updateBookmarks(),
      updateLatestSearchString()
    )
  }

  private fun updateBookmarksIfSwitchIsToggled() =
    showAllSwitchToggle.subscribe {
      actions.offer(UpdateBookmarks)
    }

  private fun updateBookmarks() =
    bookmarksDataSource.getBookmarks(!sharedPreferenceUtil.showBookmarksAllBooks)
      .subscribe { bookmarks ->
        state.postValue(updateResultsState(bookmarks))
      }

  private fun updateResultsState(
    bookmarkList: List<BookmarkItem>?
  ): State {
    return when {
      bookmarkList?.isEmpty() == true -> NoResults(emptyList())
      bookmarkList?.any(BookmarkItem::isSelected) == true -> SelectionResults(
        bookmarkList.filter { it.bookmarkTitle.contains(latestSearchString, true) })
      else -> Results(
        bookmarkList?.filter { it.bookmarkTitle.contains(latestSearchString, true) })
    }
  }

  private fun updateLatestSearchString() = filter.subscribe { searchString ->
    latestSearchString = searchString
    actions.offer(UpdateBookmarks)
  }

  override fun onCleared() {
    compositeDisposable.clear()
    super.onCleared()
  }

  private fun toggleSelectionOfBookmark(bookmarkItem: BookmarkItem): State =
    when (state.value) {
      is Results -> SelectionResults(toggleGivenItemAndReturnListOfResultingItems(bookmarkItem))
      is SelectionResults -> {
        if (toggleGivenItemAndReturnListOfResultingItems(bookmarkItem)
            ?.any(BookmarkItem::isSelected) == true
        ) {
          SelectionResults(toggleGivenItemAndReturnListOfResultingItems(bookmarkItem))
        } else {
          Results(toggleGivenItemAndReturnListOfResultingItems(bookmarkItem))
        }
      }
      is NoResults -> NoResults(emptyList())
      null -> NoResults(emptyList())
    }

  private fun toggleGivenItemAndReturnListOfResultingItems(bookmarkItem: BookmarkItem):
    List<BookmarkItem>? {
    return state.value
      ?.bookmarkItems
      ?.map {
        if (it.databaseId == bookmarkItem.databaseId)
          it.copy(isSelected = !it.isSelected) else it
      }
  }

  private fun updateFilter(searchTerm: String) {
    filter.offer(searchTerm)
  }

  private fun actionMapper() = actions.map {
    when (it) {
      ExitBookmarks -> effects.offer(Finish)
      is Filter -> updateFilter(it.searchTerm)
      is ToggleShowBookmarksFromAllBooks ->
        toggleShowAllHistorySwitchAndSaveItsStateToPrefs(it.isChecked)
      is OnItemLongClick -> state.postValue(toggleSelectionOfBookmark(it.bookmark))
      is OnItemClick -> appendItemToSelectionOrOpenIt(it)
      is RequestDeleteAllBookmarks ->
        effects.offer(ShowDeleteBookmarkDialog(actions, KiwixDialog.DeleteBookmarks))
      is RequestDeleteSelectedBookmarks ->
        effects.offer(ShowDeleteBookmarkDialog(actions, KiwixDialog.DeleteBookmarks))
      ExitActionModeMenu -> state.postValue(
        Results(
          state.value
            ?.bookmarkItems
            ?.map { item -> item.copy(isSelected = false) })
      )
      DeleteBookmarks -> effects.offer(
        DeleteSelectedOrAllBookmarkItems(
          state,
          bookmarksDataSource,
          actions
        )
      )
      UpdateBookmarks -> updateBookmarks()
    }
  }.subscribe({}, Throwable::printStackTrace)

  private fun toggleShowAllHistorySwitchAndSaveItsStateToPrefs(isChecked: Boolean) {
    effects.offer(
      ToggleShowAllBookmarksSwitchAndSaveItsStateToPrefs(
        showAllSwitchToggle,
        sharedPreferenceUtil,
        isChecked
      )
    )
  }

  private fun appendItemToSelectionOrOpenIt(onItemClick: OnItemClick) {
    val bookmark = onItemClick.bookmark
    if (state.value?.containsSelectedItems() == true) {
      state.postValue(toggleSelectionOfBookmark(bookmark))
    } else {
      effects.offer(OpenBookmark(bookmark, zimReaderContainer))
    }
  }
}
