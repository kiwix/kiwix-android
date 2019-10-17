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

package org.kiwix.kiwixmobile.core.settings

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File

internal class StorageCalculatorTest {

  private val storageCalculator = StorageCalculator()
  private val file: File = mockk()

  @Test
  fun `calculate available space with existing file`() {
    every { file.freeSpace } returns 1
    every { file.exists() } returns true
    assertThat(storageCalculator.calculateAvailableSpace(file)).isEqualTo("1 Bytes")
  }

  @Test
  fun `calculate total space of existing file`() {
    every { file.totalSpace } returns 1
    every { file.exists() } returns true
    assertThat(storageCalculator.calculateTotalSpace(file)).isEqualTo("1 Bytes")
  }

  @Test
  fun `calculate total space of non existing file`() {
    every { file.exists() } returns false
    assertThat(storageCalculator.calculateTotalSpace(file)).isEqualTo("0 Bytes")
  }

  @Test
  fun `available bytes of non existing file`() {
    every { file.exists() } returns false
    assertThat(storageCalculator.availableBytes(file)).isEqualTo(0L)
  }
}
