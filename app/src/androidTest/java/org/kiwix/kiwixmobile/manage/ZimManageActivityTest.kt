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

  private val libraryNetworkEntity = libraryNetworkEntity(listOf(book()))
  private val mockServer = KiwixMockServer().apply {
    enqueueForEvery(
      mapOf(
        "/library/library_zim.xml" to libraryNetworkEntity,
        "/${libraryNetworkEntity.book[0].url.substringAfterLast("/")}" to metaLinkNetworkEntity()
      )
    )
  }

  @Test
  fun testZimManageDataFlow() {
    zimManage {
      clickOnOnline {
        clickOnSearch()
        searchFor(libraryNetworkEntity(listOf(book(title = "zzzzz"))))
        waitForEmptyView()
        searchFor(libraryNetworkEntity)
        pressBack()
        pressBack()
        clickOn(libraryNetworkEntity)
      }
      clickOnDownloading {
        clickStop()
        clickNegativeDialogButton()
        clickStop()
        clickPositiveDialogButton()
      }
      clickOnOnline {
        clickOn(libraryNetworkEntity)
      }
      clickOnDownloading {
        waitForEmptyView()
      }
      clickOnDevice {
        longClickOn(libraryNetworkEntity)
        clickCloseActionMode()
        longClickOn(libraryNetworkEntity)
        clickDelete()
        clickNegativeDialogButton()
        longClickOn(libraryNetworkEntity)
        clickDelete()
        clickPositiveDialogButton()
        waitForEmptyView()
      }
    }
  }
}
