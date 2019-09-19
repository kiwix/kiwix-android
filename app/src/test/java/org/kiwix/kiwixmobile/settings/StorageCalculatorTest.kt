package org.kiwix.kiwixmobile.settings

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

import android.os.storage.StorageManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.KiwixBuildConfig
import java.io.File
import java.util.UUID

internal class StorageCalculatorTest {

  private val storageManager: StorageManager = mockk()
  private val storageCalculator = StorageCalculator(storageManager)
  private val file: File = mockk()

  init {
    mockkObject(KiwixBuildConfig)
  }

  @Test
  fun calculateAvailableSpace() {
    every { KiwixBuildConfig.SDK_INT } returns 25
    every { file.freeSpace } returns 1
    assertThat(storageCalculator.calculateAvailableSpace(file)).isEqualTo("1 Bytes")
  }

  @Test
  fun calculateTotalSpace() {
    every { file.totalSpace } returns 1
    assertThat(storageCalculator.calculateTotalSpace(file)).isEqualTo("1 Bytes")
  }

  @Test
  fun availableBytes() {
    val uuid: UUID = mockk()
    every { KiwixBuildConfig.SDK_INT } returns 26
    every { storageManager.getUuidForPath(file) } returns uuid
    every { storageManager.getAllocatableBytes(uuid) } returns 1
    assertThat(storageCalculator.availableBytes(file)).isEqualTo(1L)
  }
}
