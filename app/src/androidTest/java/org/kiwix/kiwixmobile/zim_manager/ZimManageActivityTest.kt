package org.kiwix.kiwixmobile.zim_manager

import android.os.Build
import androidx.test.filters.SdkSuppress
import okhttp3.mockwebserver.MockResponse
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.core.KiwixApplication
import org.kiwix.kiwixmobile.KiwixMockServer
import org.kiwix.kiwixmobile.core.book
import org.kiwix.kiwixmobile.core.data.remote.KiwixService.LIBRARY_NETWORK_PATH
import org.kiwix.kiwixmobile.core.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.core.libraryNetworkEntity
import org.kiwix.kiwixmobile.core.metaLinkNetworkEntity
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.zim_manager.ZimManageActivity
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
        forceResponse("0123456789")
        clickOn(book)
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
        .throttleBody(
          1L, 1L, SECONDS
        )
    )
  }

  private val LibraryNetworkEntity.Book.networkPath
    get() = "/${url.substringAfterLast("/")}"
}
