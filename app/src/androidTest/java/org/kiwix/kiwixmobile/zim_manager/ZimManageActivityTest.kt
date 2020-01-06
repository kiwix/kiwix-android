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

package org.kiwix.kiwixmobile.zim_manager

import android.os.Build
import androidx.test.filters.SdkSuppress
import attempt
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.KiwixMockServer
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.LIBRARY_NETWORK_PATH
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.sharedFunctions.book
import org.kiwix.sharedFunctions.libraryNetworkEntity
import org.kiwix.sharedFunctions.metaLinkNetworkEntity
import java.util.concurrent.TimeUnit.SECONDS

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR2)
class ZimManageActivityTest : BaseActivityTest<ZimManageActivity>() {

  override var activityRule = activityTestRule<ZimManageActivity> {
    CoreApp.setCoreComponent(testComponent())
  }
  private val book = book()

  private val mockServer = KiwixMockServer().apply {
    map(
      LIBRARY_NETWORK_PATH to libraryNetworkEntity(listOf(book)),
      book.networkPath to metaLinkNetworkEntity()
    )
  }

  @Test
  fun testZimManageDataFlow() {
    SharedPreferenceUtil(activityRule.activity).putPrefWifiOnly(false)
    zimManage {
      clickOnOnline {
        clickOnSearch()
        searchFor(book(title = "zzzzz"))
        waitForEmptyView()
        searchFor(book)
        pressBack()
        pressBack()
        forceResponse("012345678901234567890123456789012345678901234567890123456789012345678")
        attempt(10) {
          clickOn(book)
          waitForEmptyView()
        }
      }
      clickOnDownloading {
        clickStop()
        clickNegativeDialogButton()
        clickStop()
        clickPositiveDialogButton()
      }
      clickOnOnline {
        forceResponse("01234")
        clickOn(book)
      }
      clickOnDownloading {
        waitForEmptyView()
      }
      clickOnDevice {
        longClickOn(book)
        clickCloseActionMode()
        longClickOn(book)
        clickDelete()
        clickNegativeDialogButton()
        longClickOn(book)
        clickDelete()
        clickPositiveDialogButton()
        waitForEmptyView()
      }
      clickOnOnline { }
    } clickOnLanguageIcon { }
  }

  private fun forceResponse(body: String) {
    mockServer.forceResponse(
      MockResponse()
        .setBody(body)
        .throttleBody(1L, 1L, SECONDS)
    )
  }

  private val LibraryNetworkEntity.Book.networkPath
    get() = "/${url.substringAfterLast("/")}"
}
