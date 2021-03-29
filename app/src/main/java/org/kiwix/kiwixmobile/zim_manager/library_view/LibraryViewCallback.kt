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
package org.kiwix.kiwixmobile.zim_manager.library_view

import org.kiwix.kiwixmobile.core.base.BaseContract
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import java.util.LinkedList

/**
 * Created by EladKeyshawn on 06/04/2017.
 */
interface LibraryViewCallback : BaseContract.View<Any?> {
  fun showBooks(books: LinkedList<LibraryNetworkEntity.Book?>?)
  fun displayNoNetworkConnection()
  fun displayNoItemsFound()
  fun displayNoItemsAvailable()
  fun displayScanningContent()
  fun stopScanningContent()
  fun downloadFile(book: LibraryNetworkEntity.Book?)
}
