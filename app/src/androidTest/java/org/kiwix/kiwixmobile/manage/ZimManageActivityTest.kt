package org.kiwix.kiwixmobile.manage

import org.junit.Rule
import org.junit.Test
import org.kiwix.kiwixmobile.BaseActivityTest
import org.kiwix.kiwixmobile.KiwixApplication
import org.kiwix.kiwixmobile.KiwixMockServer
import org.kiwix.kiwixmobile.book
import org.kiwix.kiwixmobile.libraryNetworkEntity
import org.kiwix.kiwixmobile.metaLinkNetworkEntity
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity

class ZimManageActivityTest : BaseActivityTest<ZimManageActivity>() {
  @get:Rule
  override var activityRule = activityTestRule<ZimManageActivity> {
    KiwixApplication.setApplicationComponent(testComponent())
  }

  private val book = book()

  private val mockServer = KiwixMockServer().apply {
    enqueueForEvery(
      mapOf(
        "/library/library_zim.xml" to libraryNetworkEntity(listOf(book)),
        "/${book.url.substringAfterLast("/")}" to metaLinkNetworkEntity()
      )
    )
  }

  @Test
  fun testZimManageDataFlow() {
    zimManage() {
      clickOnOnline {
        // clickOnSearch()
        // searchFor(book(title = "zzzzz"))
        // waitForEmptyView()
        // searchFor(book)
        // pressBack()
        // pressBack()
        clickOn(book)
      }
      clickOnDownloading {
        clickStop()
        clickNegativeDialogButton()
        clickStop()
        clickPositiveDialogButton()
      }
      clickOnOnline {
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
    }
  }
}
