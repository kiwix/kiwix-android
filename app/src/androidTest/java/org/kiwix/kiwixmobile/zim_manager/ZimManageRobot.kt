/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.zim_manager

import applyWithViewHierarchyPrinting
import org.kiwix.kiwixmobile.BaseRobot
import org.kiwix.kiwixmobile.Findable.StringId.TextId
import org.kiwix.kiwixmobile.Findable.Text
import org.kiwix.kiwixmobile.Findable.ViewId
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.language.LanguageRobot
import org.kiwix.kiwixmobile.language.language
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book

fun zimManage(func: ZimManageRobot.() -> Unit) =
  ZimManageRobot().applyWithViewHierarchyPrinting(func)

class ZimManageRobot : BaseRobot() {
  init {
    isVisible(ViewId(R.id.manageViewPager))
  }

  fun clickOnOnline(func: LibraryRobot.() -> Unit): LibraryRobot {
    clickOnTab(R.string.remote_zims)
    return library(func)
  }

  fun clickOnDownloading(func: DownloadRobot.() -> Unit): DownloadRobot {
    clickOnTab(R.string.zim_downloads)
    return download(func)
  }

  fun clickOnDevice(func: DeviceRobot.() -> Unit): DeviceRobot {
    clickOnTab(R.string.local_zims)
    return device(func)
  }

  infix fun clickOnLanguageIcon(function: LanguageRobot.() -> Unit): LanguageRobot {
    TextId(R.string.remote_zims)
    clickOn(ViewId(R.id.select_language))
    return language(function)
  }

  private fun library(func: LibraryRobot.() -> Unit) = LibraryRobot().apply(func)
  inner class LibraryRobot : BaseRobot() {
    init {
      isVisible(ViewId(R.id.libraryList))
    }

    fun clickOn(book: Book) {
      clickOn(Text(book.title))
    }

    fun clickOnSearch() {
      clickOn(ViewId(R.id.action_search))
    }

    fun searchFor(book: Book) {
      isVisible(ViewId(R.id.search_src_text)).text = book.title
    }

    fun waitForEmptyView() {
      isVisible(ViewId(R.id.libraryErrorText))
    }
  }

  private fun download(func: DownloadRobot.() -> Unit) = DownloadRobot().apply(func)
  inner class DownloadRobot : BaseRobot() {
    init {
      isVisible(ViewId(R.id.zim_download_root), 20000L)
    }

    fun clickStop() {
      clickOn(ViewId(R.id.stop))
    }

    fun waitForEmptyView() {
      isVisible(ViewId(R.id.download_management_no_downloads), 11000L)
    }
  }

  private fun device(func: DeviceRobot.() -> Unit) = DeviceRobot().apply(func)
  inner class DeviceRobot : BaseRobot() {
    init {
      isVisible(ViewId(R.id.zimfilelist))
    }

    fun longClickOn(book: Book) {
      longClickOn(Text(book.title))
    }

    fun clickCloseActionMode() {
      clickOn(ViewId(R.id.action_mode_close_button))
    }

    fun clickDelete() {
      clickOn(ViewId(R.id.zim_file_delete_item))
    }

    fun waitForEmptyView() {
      isVisible(ViewId(R.id.file_management_no_files))
    }
  }
}
