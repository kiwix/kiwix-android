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
package org.kiwix.kiwixmobile.core.main

import android.app.Activity
import android.content.res.Configuration
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.core.view.isVisible
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.reader.ZimFileReader

const val REQUEST_FILE_SEARCH = 1236

class MainMenu(
  private val activity: Activity,
  zimFileReader: ZimFileReader?,
  menu: Menu,
  webViews: MutableList<KiwixWebView>,
  urlIsValid: Boolean,
  disableReadAloud: Boolean = false,
  disableTabs: Boolean = false,
  private val menuClickListener: MenuClickListener
) {

  interface Factory {
    fun create(
      menu: Menu,
      webViews: MutableList<KiwixWebView>,
      urlIsValid: Boolean,
      menuClickListener: MenuClickListener,
      disableReadAloud: Boolean,
      disableTabs: Boolean
    ): MainMenu
  }

  interface MenuClickListener {
    fun onTabMenuClicked()
    fun onHomeMenuClicked()
    fun onAddNoteMenuClicked()
    fun onRandomArticleMenuClicked()
    fun onReadAloudMenuClicked()
    fun onFullscreenMenuClicked()
    fun onDesktopModeMenuClicked(isEnable: Boolean)
  }

  init {
    activity.menuInflater.inflate(R.menu.menu_main, menu)
  }

  private val search = menu.findItem(R.id.menu_search)
  private var tabSwitcher: MenuItem? = menu.findItem(R.id.menu_tab_switcher)
  private var tabSwitcherTextView: TextView? =
    tabSwitcher?.actionView?.findViewById(R.id.ic_tab_switcher_text)
  private val addNote = menu.findItem(R.id.menu_add_note)
  private val randomArticle = menu.findItem(R.id.menu_random_article)
  private val fullscreen = menu.findItem(R.id.menu_fullscreen)
  private var readAloud: MenuItem? = menu.findItem(R.id.menu_read_aloud)
  private var desktopMode: MenuItem? = menu.findItem(R.id.menu_desktop_mode)
  private var isInTabSwitcher: Boolean = false

  init {
    if (disableReadAloud) {
      readAloud?.isVisible = false
      readAloud = null
    }
    if (disableTabs) {
      tabSwitcher?.isVisible = false
      tabSwitcherTextView?.isVisible = false
      tabSwitcher = null
      tabSwitcherTextView = null
    }

    randomArticle.setShowAsAction(
      if (activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        MenuItem.SHOW_AS_ACTION_IF_ROOM
      else
        MenuItem.SHOW_AS_ACTION_NEVER
    )
    tabSwitcher?.actionView?.setOnClickListener { menuClickListener.onTabMenuClicked() }
    addNote.menuItemClickListener { menuClickListener.onAddNoteMenuClicked() }
    randomArticle.menuItemClickListener { menuClickListener.onRandomArticleMenuClicked() }
    readAloud.menuItemClickListener { menuClickListener.onReadAloudMenuClicked() }
    fullscreen.menuItemClickListener { menuClickListener.onFullscreenMenuClicked() }
    desktopMode.menuItemClickListener { menuClickListener.onDesktopModeMenuClicked(it.isChecked) }

    showWebViewOptions(urlIsValid)
    zimFileReader?.let {
      onFileOpened(urlIsValid)
    }
    updateTabIcon(webViews.size)
  }

  fun onOptionsItemSelected(item: MenuItem) =
    when (item.itemId) {
      android.R.id.home -> {
        menuClickListener.onHomeMenuClicked()
        true
      }
      else -> false
    }

  fun onFileOpened(urlIsValid: Boolean) {
    setVisibility(urlIsValid, randomArticle, search, readAloud, addNote, fullscreen, desktopMode)
    search.setOnMenuItemClickListener { navigateToSearch() }
  }

  fun hideBookSpecificMenuItems() {
    setVisibility(false, search, tabSwitcher, randomArticle, addNote, desktopMode)
  }

  fun showBookSpecificMenuItems() {
    setVisibility(true, search, tabSwitcher, randomArticle, addNote, desktopMode)
  }

  fun showTabSwitcherOptions() {
    isInTabSwitcher = true
    setVisibility(false, randomArticle, readAloud, addNote, fullscreen, desktopMode)
  }

  fun showWebViewOptions(urlIsValid: Boolean) {
    isInTabSwitcher = false
    fullscreen.isVisible = true
    setVisibility(urlIsValid, randomArticle, search, readAloud, addNote, desktopMode)
  }

  fun updateTabIcon(tabs: Int) {
    tabSwitcherTextView?.text = if (tabs > 99) ":D" else "$tabs"
  }

  private fun navigateToSearch(): Boolean {
    (activity as CoreMainActivity).openSearch(isOpenedFromTabView = isInTabSwitcher)
    return true
  }

  fun onTextToSpeechStartedTalking() {
    readAloud?.setTitle(R.string.menu_read_aloud_stop)
  }

  fun onTextToSpeechStoppedTalking() {
    readAloud?.setTitle(R.string.menu_read_aloud)
  }

  fun setDesktopModeEnable(isEnable: Boolean) {
    desktopMode?.isChecked = isEnable
  }

  private fun setVisibility(visibility: Boolean, vararg menuItems: MenuItem?) {
    menuItems.forEach { it?.isVisible = visibility }
  }

  fun tryExpandSearch(zimFileReader: ZimFileReader?) {
    if (search.isVisible && zimFileReader != null) {
      navigateToSearch()
    }
  }

  fun isInTabSwitcher(): Boolean = isInTabSwitcher
}

private fun MenuItem?.menuItemClickListener(function: (MenuItem) -> Unit) {
  this?.setOnMenuItemClickListener {
    function.invoke(it)
    true
  }
}
