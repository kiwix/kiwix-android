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
package org.kiwix.kiwixmobile.core.error

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import kotlinx.android.synthetic.main.activity_kiwix_error.allowCrash
import kotlinx.android.synthetic.main.activity_kiwix_error.allowDeviceDetails
import kotlinx.android.synthetic.main.activity_kiwix_error.allowFileSystemDetails
import kotlinx.android.synthetic.main.activity_kiwix_error.allowLanguage
import kotlinx.android.synthetic.main.activity_kiwix_error.allowLogs
import kotlinx.android.synthetic.main.activity_kiwix_error.allowZims
import kotlinx.android.synthetic.main.activity_kiwix_error.reportButton
import kotlinx.android.synthetic.main.activity_kiwix_error.restartButton
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.getCurrentLocale
import org.kiwix.kiwixmobile.core.utils.files.FileLogger
import org.kiwix.kiwixmobile.zim_manager.MountPointProducer
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject
import kotlin.system.exitProcess

open class ErrorActivity : BaseActivity() {
  @Inject
  lateinit var bookDao: NewBookDao

  @Inject
  lateinit var zimReaderContainer: ZimReaderContainer

  @Inject
  lateinit var mountPointProducer: MountPointProducer

  @Inject
  lateinit var fileLogger: FileLogger

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_kiwix_error)
    val callingIntent = intent
    val extras = callingIntent.extras
    val exception: Throwable?
    exception = if (extras != null && safeContains(extras)) {
      extras.getSerializable(EXCEPTION_KEY) as Throwable
    } else {
      null
    }
    reportButton!!.setOnClickListener {
      val emailIntent = Intent(Intent.ACTION_SEND)
      emailIntent.type = "vnd.android.cursor.dir/email"
      emailIntent.putExtra(
        Intent.EXTRA_EMAIL,
        arrayOf("android-crash-feedback@kiwix.org")
      )
      emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject)
      var body = body
      if (allowLogs.isChecked) {
        val file = fileLogger.writeLogFile(this)
        val path = FileProvider.getUriForFile(
          this,
          applicationContext.packageName + ".fileprovider",
          file
        )
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        emailIntent.putExtra(Intent.EXTRA_STREAM, path)
      }
      if (allowCrash.isChecked && exception != null) {
        body += """
        Exception Details:

        ${toStackTraceString(exception)}


        """.trimIndent()
      }
      if (allowZims.isChecked) {
        val sb = StringBuilder()
        bookDao.getBooks().forEach {
          val bookString = """
          ${it.book.getTitle()}:
          Articles: [${it.book.getArticleCount()}]
          Creator: [${it.book.getCreator()}]

          """.trimIndent()
          sb.append(bookString)
        }
        val allZimFiles = "$sb"
        val currentZimFile = zimReaderContainer.zimCanonicalPath
        body += """
        Curent Zim File:
        $currentZimFile

        All Zim Files in DB:
        $allZimFiles


        """.trimIndent()
      }
      if (allowLanguage.isChecked) {
        body += """
        Current Locale:
        ${getCurrentLocale(applicationContext)}


        """.trimIndent()
      }
      if (allowDeviceDetails.isChecked) {
        body += """Device Details:
Device:[${Build.DEVICE}]
Model:[${Build.MODEL}]
Manufacturer:[${Build.MANUFACTURER}]
Time:[${Build.TIME}]
Android Version:[${Build.VERSION.RELEASE}]
App Version:[$versionName $versionCode]

"""
      }
      if (allowFileSystemDetails.isChecked) {
        body += "Mount Points\n"
        mountPointProducer.produce().forEach {
          body += """
          $it

          """.trimIndent()
        }
        body += "\nExternal Directories\n"
        for (externalFilesDir in ContextCompat.getExternalFilesDirs(this, null)) body += """
        ${if (externalFilesDir != null) externalFilesDir.path else "null"}

        """.trimIndent()
      }
      emailIntent.putExtra(Intent.EXTRA_TEXT, body)
      startActivityForResult(Intent.createChooser(emailIntent, "Send email..."), 1)
    }
    restartButton!!.setOnClickListener { onRestartClicked() }
  }

  private fun safeContains(extras: Bundle): Boolean {
    return try {
      extras.containsKey(EXCEPTION_KEY)
    } catch (ignore: RuntimeException) {
      false
    }
  }

  private fun onRestartClicked() {
    restartApp()
  }

  protected open val subject: String
    get() = "Someone has reported a crash"

  protected open val body: String
    get() = """
      Hi Kiwix Developers!
      The Android app crashed, here are some details to help fix it:


      """.trimIndent()

  private val versionCode: Int
    get() = try {
      packageManager
        .getPackageInfo(packageName, 0).versionCode
    } catch (e: PackageManager.NameNotFoundException) {
      throw RuntimeException(e)
    }

  private val versionName: String
    get() = try {
      packageManager
        .getPackageInfo(packageName, 0).versionName
    } catch (e: PackageManager.NameNotFoundException) {
      throw RuntimeException(e)
    }

  private fun toStackTraceString(exception: Throwable): String {
    val stringWriter = StringWriter()
    exception.printStackTrace(PrintWriter(stringWriter))
    return "$stringWriter"
  }

  open fun restartApp() {
    startActivity(packageManager.getLaunchIntentForPackage(packageName))
    finish()
    killCurrentProcess()
  }

  public override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, data)
    restartApp()
  }

  override fun injection(coreComponent: CoreComponent) {
    coreComponent.inject(this)
  }

  companion object {
    const val EXCEPTION_KEY = "exception"
    private fun killCurrentProcess() {
      Process.killProcess(Process.myPid())
      exitProcess(10)
    }
  }
}
