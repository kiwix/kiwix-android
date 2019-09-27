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

package org.kiwix.kiwixmobile.utils.files

import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import java.io.File

class FileUtilsTest {

  private val mockFile: File = mockk()
  private val testBook = Book().apply { file = mockFile }
  private val testId = "8ce5775a-10a9-bbf3-178a-9df69f23263c"
  private val fileName = "/data/user/0/org.kiwix.kiwixmobile/files${File.separator}$testId"

  @BeforeEach
  fun init() {
    clearMocks(mockFile)
  }

  @Test
  fun `Filename ends with zim and file does not exist at the location`() {
    testWith(".zim", false)
  }

  @Test
  fun `Filename ends with zim and file exists at the location`() {
    testWith(".zim", true)
  }

  @Test
  fun `Filename ends with zim part and file does not exist at the location`() {
    testWith(".zim.part", false)
  }

  @Test
  fun `Filename ends with zim part and file exists at the location`() {
    testWith(".zim.part", true)
  }

  @Test
  fun `Filename ends with zimXX and no such file exists at any such location`() {
    expect("zimab", false)
    assertThat(FileUtils.getAllZimParts(testBook).size).isEqualTo(0)
      .withFailMessage("Nothing is returned in this case")
  }

  private fun testWith(
    extension: String,
    fileExists: Boolean
  ) {
    expect(extension, fileExists)
    val files = FileUtils.getAllZimParts(testBook)
    assertThat(files.size).isEqualTo(1)
      .withFailMessage("Only a single book is returned in case the file has extension $extension")
    if (fileExists) {
      assertThat(testBook.file).isEqualTo(files[0])
        .withFailMessage("The filename retained as such")
    } else {
      assertThat(testBook.file.toString() + ".part").isEqualTo(files[0].path)
        .withFailMessage("The filename is appended with .part")
    }
  }

  private fun expect(
    extension: String,
    fileExists: Boolean
  ) {
    every { mockFile.path } returns "$fileName$extension"
    every { mockFile.exists() } returns fileExists
  }
}
