/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.utils

import android.app.Activity
import android.webkit.WebResourceResponse
import android.widget.Toast
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil
import org.kiwix.kiwixmobile.core.utils.dialog.AlertDialogShower
import org.kiwix.kiwixmobile.core.utils.dialog.DownloadOrOpenEpubAndPdfHandler
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixDialog
import java.io.File
import java.io.InputStream

class DownloadOrOpenEpubAndPdfHandlerTest {
  private val demoUrl = "content://demoPdf.pdf"
  private val demoFileName = "demoPdf.pdf"
  private val sharedPreferenceUtil: SharedPreferenceUtil = mockk()
  private val zimReaderContainer: ZimReaderContainer = mockk()
  private val alertDialogShower: AlertDialogShower = mockk(relaxed = true)
  private val savedFile: File = mockk(relaxed = true)
  private val activity: Activity = mockk()
  private val webResourceResponse: WebResourceResponse = mockk()
  private val inputStream: InputStream = mockk()
  private val downloadOrOpenEpubAndPdfHandler = DownloadOrOpenEpubAndPdfHandler(
    activity,
    sharedPreferenceUtil,
    alertDialogShower,
    zimReaderContainer
  )

  @Before
  fun before() {
    every { savedFile.name } returns demoFileName
    every { sharedPreferenceUtil.isPlayStoreBuildWithAndroid11OrAbove() } returns true
    every { inputStream.read(array()) } returns 1024
    every { webResourceResponse.data } returns inputStream
    every {
      zimReaderContainer.load(
        demoUrl,
        emptyMap()
      )
    } returns webResourceResponse
    every { activity.packageManager } returns mockk()
    every { activity.packageName } returns "org.kiwix.kiwixmobile"
    every {
      activity.getString(
        R.string.save_media_saved,
        demoFileName
      )
    } returns "Saved media as $demoFileName to Downloads/org.kiwix…/"
    every { savedFile.path } returns "Emulated/0/Downloads/$demoFileName"
    every { savedFile.exists() } returns true
    downloadOrOpenEpubAndPdfHandler.intent = mockk()
    every { downloadOrOpenEpubAndPdfHandler.intent.setDataAndType(any(), any()) } returns mockk()
    every { downloadOrOpenEpubAndPdfHandler.intent.setFlags(any()) } returns mockk()
    every { downloadOrOpenEpubAndPdfHandler.intent.addFlags(any()) } returns mockk()
  }

  @Test
  fun testOpeningFileInExternalReaderApplication() {
    every {
      downloadOrOpenEpubAndPdfHandler.intent.resolveActivity(activity.packageManager)
    } returns mockk()
    every { activity.startActivity(downloadOrOpenEpubAndPdfHandler.intent) } returns mockk()
    val lambdaSlot = slot<() -> Unit>()
    downloadOrOpenEpubAndPdfHandler.showDownloadOrOpenEpubAndPdfDialog(demoUrl, "application/pdf")
    verify {
      alertDialogShower.show(
        KiwixDialog.DownloadOrOpenEpubAndPdf,
        capture(lambdaSlot),
        any(),
        any()
      )
    }
    lambdaSlot.captured.invoke()
    verify {
      activity.startActivity(downloadOrOpenEpubAndPdfHandler.intent)
    }
  }

  @Test
  fun testOpeningFileWhenNoReaderApplicationInstalled() {
    every {
      downloadOrOpenEpubAndPdfHandler.intent.resolveActivity(activity.packageManager)
    } returns null
    mockkStatic(Toast::class)
    justRun {
      Toast.makeText(activity, R.string.no_reader_application_installed, Toast.LENGTH_LONG).show()
    }
    val lambdaSlot = slot<() -> Unit>()
    downloadOrOpenEpubAndPdfHandler.showDownloadOrOpenEpubAndPdfDialog(demoUrl, "application/pdf")
    verify {
      alertDialogShower.show(
        KiwixDialog.DownloadOrOpenEpubAndPdf,
        capture(lambdaSlot),
        any(),
        any()
      )
    }
    lambdaSlot.captured.invoke()
    verify { activity.toast(R.string.no_reader_application_installed) }
  }

  @Test
  fun testFileDownloadingSuccessfull() {
    val toastMessage = activity.getString(R.string.save_media_saved, savedFile.name)
    mockkStatic(Toast::class)
    justRun {
      Toast.makeText(
        activity,
        toastMessage,
        Toast.LENGTH_LONG
      ).show()
    }
    val lambdaSlot = slot<() -> Unit>()
    downloadOrOpenEpubAndPdfHandler.showDownloadOrOpenEpubAndPdfDialog(
      demoUrl,
      "application/pdf"
    )
    verify {
      alertDialogShower.show(
        KiwixDialog.DownloadOrOpenEpubAndPdf,
        any(),
        capture(lambdaSlot),
        any()
      )
    }
    lambdaSlot.captured.invoke()
    verify { activity.toast(toastMessage) }
  }

  @Test
  fun testUserClicksOnNoThanksButton() {
    val lambdaSlot = slot<() -> Unit>()
    downloadOrOpenEpubAndPdfHandler.showDownloadOrOpenEpubAndPdfDialog(demoUrl, "application/pdf")
    verify {
      alertDialogShower.show(
        KiwixDialog.DownloadOrOpenEpubAndPdf,
        any(),
        any(),
        capture(lambdaSlot)
      )
    }
    lambdaSlot.captured.invoke()
    verify(exactly = 0) { activity.startActivity(any()) }
  }

  @Test
  fun testIfDownloadFailed() {
    val downloadOrOpenEpubAndPdfHandler = DownloadOrOpenEpubAndPdfHandler(
      activity,
      sharedPreferenceUtil,
      alertDialogShower,
      zimReaderContainer
    )
    mockkStatic(Toast::class)
    justRun {
      Toast.makeText(activity, R.string.save_media_error, Toast.LENGTH_LONG).show()
    }
    val lambdaSlot = slot<() -> Unit>()
    downloadOrOpenEpubAndPdfHandler.showDownloadOrOpenEpubAndPdfDialog(null, "application/pdf")
    verify {
      alertDialogShower.show(
        KiwixDialog.DownloadOrOpenEpubAndPdf,
        any(),
        capture(lambdaSlot),
        any()
      )
    }
    lambdaSlot.captured.invoke()
    verify { activity.toast(R.string.save_media_error) }
  }
}
