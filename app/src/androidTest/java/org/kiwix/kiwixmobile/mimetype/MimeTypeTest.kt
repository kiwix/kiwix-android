/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.mimetype

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.reader.ZimReaderSource
import org.kiwix.kiwixmobile.testutils.TestUtils
import org.kiwix.kiwixmobile.testutils.TestUtils.getZimFileFromResourceFolder
import org.kiwix.libzim.SuggestionSearcher

class MimeTypeTest : BaseActivityTest() {
  @Before
  override fun waitForIdle() {
    super.waitForIdle()
    launchMainActivity()
  }

  @Test
  fun testMimeType() =
    runBlocking {
      val zimFile = getZimFileFromResourceFolder(context, "testzim.zim")
      val zimSource = ZimReaderSource(zimFile)
      val archive = zimSource.createArchive(Dispatchers.IO)
      val zimFileReader =
        ZimFileReader(
          zimSource,
          archive!!,
          SuggestionSearcher(archive),
          Dispatchers.IO
        )
      zimFileReader.getRandomArticleUrl()?.let { randomArticle ->
        val mimeType = zimFileReader.getMimeTypeFromUrl(randomArticle)
        if (mimeType?.contains("^([^ ]+).*$") == true || mimeType?.contains(";") == true) {
          Assert.fail(
            "Unable to get mime type from zim file. File = " +
              " $zimFile and url of article = $randomArticle"
          )
        }
      } ?: kotlin.run {
        Assert.fail("Unable to get article from zim file $zimFile")
      }
      // test mimetypes for some actual url
      Assert.assertEquals(
        "text/html",
        zimFileReader.getMimeTypeFromUrl("https://kiwix.app/A/index.html")
      )
      Assert.assertEquals(
        "text/css",
        zimFileReader.getMimeTypeFromUrl("https://kiwix.app/-/assets/style1.css")
      )
      // test mimetype for invalid url
      Assert.assertEquals(null, zimFileReader.getMimeTypeFromUrl("https://kiwix.app/A/test.html"))
      // dispose the ZimFileReader
      zimFileReader.dispose()
    }

  @After
  fun finish() {
    TestUtils.deleteTemporaryFilesOfTestCases(context)
  }
}
