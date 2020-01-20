/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package eu.mhutti1.utils.storage.adapter

import eu.mhutti1.utils.storage.StorageDevice
import org.kiwix.kiwixmobile.core.base.adapter.BaseDelegateAdapter

internal class StorageAdapter(storageDelegate: StorageDelegate) :
  BaseDelegateAdapter<StorageDevice>(storageDelegate) {
  override fun getIdFor(item: StorageDevice) = item.hashCode().toLong()
}
