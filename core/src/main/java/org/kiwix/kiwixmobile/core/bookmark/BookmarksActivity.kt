/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
package org.kiwix.kiwixmobile.core.bookmark

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_bookmarks.bookmarks_switch
import kotlinx.android.synthetic.main.activity_bookmarks.no_bookmarks
import kotlinx.android.synthetic.main.activity_bookmarks.recycler_view
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.Intents.internal
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.coreActivityComponent
import org.kiwix.kiwixmobile.core.extensions.setBitmapFromString
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.DialogShower
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_FILE
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_TITLE
import org.kiwix.kiwixmobile.core.utils.EXTRA_CHOSE_X_URL
import org.kiwix.kiwixmobile.core.utils.KiwixDialog
import java.util.ArrayList
import javax.inject.Inject

class BookmarksActivity : BaseActivity(),
  BookmarksContract.View, BookmarksAdapter.OnItemClickListener {
  private val bookmarksList: MutableList<BookmarkItem> = ArrayList()
  private val allBookmarks: MutableList<BookmarkItem> = ArrayList()
  private val deleteList: MutableList<BookmarkItem> = ArrayList()
  private val activityComponent by lazy { coreActivityComponent }

  @Inject
  lateinit var presenter: BookmarksContract.Presenter

  @Inject
  lateinit var zimReaderContainer: ZimReaderContainer

  @Inject
  lateinit var dialogShower: DialogShower
  private var refreshAdapter = true
  private var bookmarksAdapter: BookmarksAdapter? = null
  private var actionMode: ActionMode? = null
  private val actionModeCallback: ActionMode.Callback =
    object : ActionMode.Callback {
      override fun onCreateActionMode(
        mode: ActionMode,
        menu: Menu
      ): Boolean {
        mode.menuInflater.inflate(R.menu.menu_context_delete, menu)
        bookmarks_switch!!.isEnabled = false
        return true
      }

      override fun onPrepareActionMode(
        mode: ActionMode,
        menu: Menu
      ): Boolean = false

      override fun onActionItemClicked(
        mode: ActionMode,
        item: MenuItem
      ): Boolean {
        refreshAdapter = false
        if (item.itemId == R.id.menu_context_delete) {
          dialogShower.show(KiwixDialog.DeleteBookmarks, {
            allBookmarks.removeAll(deleteList)
            for (bookmark in deleteList) {
              val position = bookmarksList.indexOf(bookmark)
              bookmarksList.remove(bookmark)
              bookmarksAdapter!!.notifyItemRemoved(position)
              bookmarksAdapter!!.notifyItemRangeChanged(position, bookmarksAdapter!!.itemCount)
            }
            presenter.deleteBookmarks(ArrayList(deleteList))
            mode.finish()
          })
          return true
        }
        return false
      }

      override fun onDestroyActionMode(mode: ActionMode) {
        if (deleteList.size != 0) {
          deleteList.clear()
        }
        actionMode = null
        if (refreshAdapter) {
          bookmarksAdapter!!.notifyDataSetChanged()
        }
        bookmarks_switch!!.isEnabled = true
      }
    }

  override fun injection(coreComponent: CoreComponent) {
    activityComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    presenter.attachView(this)
    setContentView(R.layout.activity_bookmarks)
    setSupportActionBar(toolbar)
    val actionBar = supportActionBar
    if (actionBar != null) {
      actionBar.setDisplayHomeAsUpEnabled(true)
      actionBar.setTitle(R.string.bookmarks)
    }
    setupBookmarksAdapter()
    recycler_view!!.adapter = bookmarksAdapter
    bookmarks_switch!!.setOnCheckedChangeListener { _: CompoundButton?, isChecked: Boolean ->
      sharedPreferenceUtil.showBookmarksCurrentBook = !isChecked
      presenter.loadBookmarks(sharedPreferenceUtil.showBookmarksCurrentBook)
    }
    bookmarks_switch!!.isChecked = !sharedPreferenceUtil.showBookmarksCurrentBook
  }

  private fun setupBookmarksAdapter() {
    bookmarksAdapter = BookmarksAdapter(bookmarksList, deleteList, this)
    bookmarksAdapter!!.registerAdapterDataObserver(object : AdapterDataObserver() {
      override fun onChanged() {
        super.onChanged()
        no_bookmarks!!.visibility = if (bookmarksList.size == 0) View.VISIBLE else View.GONE
      }
    })
  }

  override fun onResume() {
    super.onResume()
    presenter.loadBookmarks(sharedPreferenceUtil.showBookmarksCurrentBook)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.menu_bookmarks, menu)
    val search = menu.findItem(R.id.menu_bookmarks_search)
      .actionView as SearchView
    search.queryHint = getString(R.string.search_bookmarks)
    search.setOnQueryTextListener(object :
      SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String): Boolean = false

      override fun onQueryTextChange(newText: String): Boolean {
        bookmarksList.clear()
        bookmarksList.addAll(allBookmarks)
        if ("" == newText) {
          bookmarksAdapter!!.notifyDataSetChanged()
          return true
        }
        presenter.filterBookmarks(bookmarksList, newText)
        return true
      }
    })
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      android.R.id.home -> {
        onBackPressed()
      }
      R.id.menu_bookmarks_clear -> {
        dialogShower.show(KiwixDialog.DeleteBookmarks, {
          presenter.deleteBookmarks(ArrayList(allBookmarks))
          allBookmarks.clear()
          bookmarksList.clear()
          bookmarksAdapter!!.notifyDataSetChanged()
          Snackbar.make(no_bookmarks!!, R.string.all_bookmarks_cleared, Snackbar.LENGTH_SHORT)
            .show()
        })
      }
      else -> return super.onOptionsItemSelected(item)
    }
    return true
  }

  override fun onDestroy() {
    presenter.detachView()
    super.onDestroy()
  }

  override fun updateBookmarksList(bookmarksList: List<BookmarkItem>) {
    allBookmarks.clear()
    allBookmarks.addAll(bookmarksList)
    notifyBookmarksListFiltered(bookmarksList)
  }

  override fun notifyBookmarksListFiltered(bookmarksList: List<BookmarkItem>) {
    this.bookmarksList.clear()
    this.bookmarksList.addAll(bookmarksList)
    bookmarksAdapter!!.notifyDataSetChanged()
  }

  override fun onItemClick(
    favicon: ImageView,
    bookmark: BookmarkItem
  ) {
    if (actionMode == null) {
      val intent = internal(
        CoreMainActivity::class.java
      )
      if ("null" == bookmark.bookmarkUrl) {
        intent.putExtra(EXTRA_CHOSE_X_TITLE, bookmark.bookmarkTitle)
      } else {
        intent.putExtra(EXTRA_CHOSE_X_URL, bookmark.bookmarkUrl)
      }
      if (bookmark.zimFilePath != null &&
        bookmark.zimFilePath != zimReaderContainer.zimCanonicalPath
      ) {
        intent.putExtra(EXTRA_CHOSE_X_FILE, bookmark.zimFilePath)
      }
      setResult(Activity.RESULT_OK, intent)
      finish()
    } else {
      toggleSelection(favicon, bookmark)
    }
  }

  override fun onItemLongClick(
    favicon: ImageView,
    bookmark: BookmarkItem
  ): Boolean {
    if (actionMode != null) {
      return false
    }
    actionMode = startSupportActionMode(actionModeCallback)
    refreshAdapter = true
    toggleSelection(favicon, bookmark)
    return true
  }

  private fun toggleSelection(
    favicon: ImageView,
    bookmark: BookmarkItem
  ) {
    if (deleteList.remove(bookmark)) {
      favicon.setBitmapFromString(bookmark.favicon)
    } else {
      favicon.setImageDrawable(
        ContextCompat.getDrawable(this, R.drawable.ic_check_circle_blue_24dp)
      )
      deleteList.add(bookmark)
    }
    actionMode!!.title = getString(R.string.selected_items, deleteList.size)
    if (deleteList.size == 0) {
      actionMode!!.finish()
    }
  }
}
