package org.kiwix.kiwixmobile.zim_manager

import android.os.Build
import androidx.test.filters.SdkSuppress
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.KiwixApplication
import org.kiwix.kiwixmobile.KiwixMockServer
import org.kiwix.kiwixmobile.book
import org.kiwix.kiwixmobile.data.remote.KiwixService.LIBRARY_NETWORK_PATH
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.libraryNetworkEntity
import org.kiwix.kiwixmobile.metaLinkNetworkEntity
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil
import java.util.concurrent.TimeUnit.SECONDS

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR2)
class ZimManageActivityTest : BaseActivityTest<ZimManageActivity>() {

  override var activityRule = activityTestRule<ZimManageActivity> {
    KiwixApplication.setApplicationComponent(testComponent())
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
        queueMockResponseWith("0123456789")
        clickOn(book)
      }
      clickOnDownloading {
        clickStop()
        clickNegativeDialogButton()
        clickStop()
        clickPositiveDialogButton()
      }
      clickOnOnline {
        queueMockResponseWith("01234")
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

  private fun queueMockResponseWith(body: String) {
    mockServer.queueResponse(
      MockResponse()
        .setBody(body)
        .throttleBody(
          1L, 1L, SECONDS
        )
    )
  }

  private val LibraryNetworkEntity.Book.networkPath
    get() = "/${url.substringAfterLast("/")}"
}
