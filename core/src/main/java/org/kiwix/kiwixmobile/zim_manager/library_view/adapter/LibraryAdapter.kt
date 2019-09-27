/*
 * Copyright 2013  Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.zim_manager.library_view.adapter

import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base.AdapterDelegate
import org.kiwix.kiwixmobile.zim_manager.library_view.adapter.base.BaseDelegateAdapter

class LibraryAdapter(
  vararg delegates: AdapterDelegate<LibraryListItem>
) : BaseDelegateAdapter<LibraryListItem>(
  *delegates
) {
  override fun getIdFor(item: LibraryListItem) = item.id
}
