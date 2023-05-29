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
package org.kiwix.kiwixmobile.utils.files

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getAllZimParts
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.hasPart
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import java.io.File
import java.io.IOException
import java.util.Random

class FileUtilsInstrumentationTest {
  private var context: Context? = null
  private var testDir: File? = null

  @Before
  fun executeBefore() {
    context = InstrumentationRegistry.getInstrumentation().targetContext

    // Create a temporary directory where all the test files will be saved
    testDir = context?.getDir("testDir", Context.MODE_PRIVATE)
  }

  @Test
  @Throws(IOException::class)
  fun testGetAllZimParts() {

    // Filename ends with .zimXX and the files up till "FileName.zimer" exist
    // i.e. 26 * 4 + 18 = 122 files exist
    val testId = "2rs5475f-51h7-vbz6-331b-7rr25r58251s"
    val fileName = testDir?.path + "/" + testId + "testfile.zim"
    var fileNameWithExtension: String
    val r = Random()
    val bool = BooleanArray(122)

    // Creating the files for the test
    var index = 0
    var char1 = 'a'
    while (char1 <= 'z') {
      var char2 = 'a'
      while (char2 <= 'z') {
        bool[index] = r.nextBoolean()
        fileNameWithExtension = fileName + char1 + char2
        fileNameWithExtension =
          if (bool[index]) fileNameWithExtension else "$fileNameWithExtension.part"
        val file = File(fileNameWithExtension)
        file.createNewFile()
        if (char1 == 'e' && char2 == 'r') {
          break
        }
        index++
        char2++
      }
      if (char1 == 'e') {
        break
      }
      char1++
    }
    val book = LibraryNetworkEntity.Book()
    book.file = File(fileName + "bg")
    val files = getAllZimParts(book)

    // Testing the data returned
    Assert.assertEquals(
      "26 * 4 + 18 = 122 files should be returned",
      122,
      files.size.toLong()
    )
    index = 0
    while (index < 122) {
      if (bool[index]) {
        Assert.assertEquals(
          "if the file fileName.zimXX exists, then no need to add the .part extension at the end",
          false,
          files[index].path.endsWith(
            ".part"
          )
        )
      } else {
        Assert.assertEquals(
          "if the file fileName.zimXX.part exists, then the file" +
            " returned should also have the same ending .zimXX.part",
          true,
          files[index].path.endsWith(
            ".part"
          )
        )
      }
      index++
    }
  }

  @Test
  @Throws(IOException::class)
  fun testHasPart() {
    val testId = "3yd5474g-55d1-aqw0-108z-1xp69x25260d"
    val baseName = testDir?.path + "/" + testId + "testFile"

    // FileName ends with .zim
    val file1 = File(baseName + "1" + ".zim")
    file1.createNewFile()
    Assert.assertEquals(
      "if the fileName ends with .zim and exists in memory, return false",
      false, hasPart(file1)
    )

    // FileName ends with .part
    val file2 = File(baseName + "2" + ".zim")
    file2.createNewFile()
    Assert.assertEquals(
      "if the fileName ends with .part and exists in memory, return true",
      false, hasPart(file2)
    )

    // FileName ends with .zim, however, only the FileName.zim.part file exists in memory
    val file3 = File(baseName + "3" + ".zim" + ".part")
    file3.createNewFile()
    val file4 = File(baseName + "3" + ".zim")
    Assert.assertEquals(
      "if the fileName ends with .zim, but instead the .zim.part file exists in memory",
      true, hasPart(file4)
    )

    // FileName ends with .zimXX
    val testCall = File("$baseName.zimcj")
    testCall.createNewFile()

    // Case : FileName.zimXX.part does not exist for any value of "XX" from "aa"
    // till "bl", but FileName.zimXX exists for all "XX" from "aa',
    // till "bk", then it does not exist
    var char1 = 'a'
    while (char1 <= 'z') {
      var char2 = 'a'
      while (char2 <= 'z') {
        val file = File("$baseName.zim$char1$char2")
        file.createNewFile()
        if (char1 == 'b' && char2 == 'k') {
          break
        }
        char2++
      }
      if (char1 == 'b') {
        break
      }
      char1++
    }
    Assert.assertEquals(false, hasPart(testCall))

    // Case : FileName.zim is the calling file, but neither FileName.zim,
    //        nor FileName.zim.part exist
    // In this case the answer will be the same as that
    //        in the previous (FileName.zimXX) case
    val testCall2 = File("$baseName.zim")
    Assert.assertEquals(false, hasPart(testCall2))

    // Case : FileName.zimXX.part exists for some "XX" between "aa" till "bl"
    // And FileName.zimXX exists for all "XX" from "aa', till "bk",
    //        and then it does not exist
    val t = File("$baseName.zimaj.part")
    t.createNewFile()
    Assert.assertEquals(true, hasPart(testCall))

    // Case : FileName.zim is the calling file, but neither FileName.zim,
    //        nor FileName.zim.part exist
    // In this case the answer will be the same as that in the
    //        previous (FileName.zimXX) case
    Assert.assertEquals(true, hasPart(testCall2))
  }

  @After
  fun removeTestDirectory() {
    testDir?.let {
      it.listFiles()?.let { files ->
        for (child in files) {
          child.delete()
        }
      }
      it.delete()
    }
  }

  @Test
  fun testDecodeFileName() {
    val dummyUrlArray = listOf(
      DummyUrlData(
        "https://kiwix.org/contributors/contributors_list.pdf",
        "contributors_list.pdf"
      ),
      DummyUrlData(
        "https://kiwix.org/contributors/",
        null
      ),
      DummyUrlData(
        "android_tutorials.pdf",
        null
      ),
      DummyUrlData(
        null,
        null
      ),
      DummyUrlData(
        "/html/images/test.png",
        "test.png"
      ),
      DummyUrlData(
        "/html/images/",
        null
      ),
      DummyUrlData(
        "https://kiwix.org/contributors/images/wikipedia.png",
        "wikipedia.png"
      ),
      DummyUrlData(
        "https://kiwix.org/contributors/images/wikipedia",
        null
      )
    )
    dummyUrlArray.forEach {
      Assertions.assertEquals(
        FileUtils.getDecodedFileName(it.url),
        it.expectedFileName
      )
    }
  }

  data class DummyUrlData(val url: String?, val expectedFileName: String?)
}
