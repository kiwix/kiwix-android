/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.reader

import android.content.res.AssetFileDescriptor
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ZimReaderSourceTest {
  @Test
  fun `serializing a source with a non-null assetFileDescriptorList does not throw`() {
    // AssetFileDescriptor isn't Serializable, so this used to throw
    // NotSerializableException whenever this field was non-null (e.g. any URI-based
    // source). @Transient makes it survive serialization by coming back null instead.
    val source = ZimReaderSource(
      file = File("test.zim"),
      assetFileDescriptorList = listOf(mockk<AssetFileDescriptor>())
    )

    val bytes = ByteArrayOutputStream().apply {
      ObjectOutputStream(this).use { it.writeObject(source) }
    }.toByteArray()

    val deserialized =
      ObjectInputStream(ByteArrayInputStream(bytes)).use { it.readObject() } as ZimReaderSource

    assertNull(deserialized.assetFileDescriptorList)
  }
}
