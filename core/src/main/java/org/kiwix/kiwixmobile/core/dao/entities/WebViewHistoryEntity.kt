/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.dao.entities

import android.os.Bundle
import android.os.Parcel
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import org.kiwix.kiwixmobile.core.page.history.models.WebViewHistoryItem

@Entity
data class WebViewHistoryEntity(
  @PrimaryKey(autoGenerate = true) var id: Long = 0L,
  val zimId: String,
  val webViewIndex: Int,
  val webViewCurrentPosition: Int,
  @TypeConverters(BundleRoomConverter::class)
  val webViewBackForwardListBundle: Bundle?
) {
  constructor(webViewHistoryItem: WebViewHistoryItem) : this(
    webViewHistoryItem.databaseId,
    webViewHistoryItem.zimId,
    webViewHistoryItem.webViewIndex,
    webViewHistoryItem.webViewCurrentPosition,
    webViewHistoryItem.webViewBackForwardListBundle,
  )
}

class BundleRoomConverter {
  @TypeConverter
  fun convertToDatabaseValue(bundle: Bundle?): ByteArray? {
    if (bundle == null) return null
    val parcel = Parcel.obtain()
    parcel.writeBundle(bundle)
    val bytes = parcel.marshall()
    parcel.recycle()
    return bytes
  }

  @TypeConverter
  fun convertToEntityProperty(byteArray: ByteArray?): Bundle? {
    if (byteArray == null) return null
    val parcel = Parcel.obtain()
    parcel.unmarshall(byteArray, 0, byteArray.size)
    parcel.setDataPosition(0)
    val bundle = parcel.readBundle(Bundle::class.java.classLoader)
    parcel.recycle()
    return bundle
  }
}
