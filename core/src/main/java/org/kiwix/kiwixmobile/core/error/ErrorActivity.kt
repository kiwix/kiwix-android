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

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getVersionCode
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.databinding.ActivityKiwixErrorBinding
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.getCurrentLocale
import org.kiwix.kiwixmobile.core.utils.files.FileLogger
import org.kiwix.kiwixmobile.core.zim_manager.MountPointProducer
import java.io.PrintWriter
import java.io.StringWriter
import javax.inject.Inject
import kotlin.system.exitProcess

private const val STATUS = 10
private const val ZERO = 0

open class ErrorActivity : BaseActivity() {
  @Inject
  lateinit var bookDao: NewBookDao

  @Inject
  lateinit var zimReaderContainer: ZimReaderContainer

  @Inject
  lateinit var mountPointProducer: MountPointProducer

  @Inject
  lateinit var fileLogger: FileLogger

  private var exception: Throwable? = null

  var activityKiwixErrorBinding: ActivityKiwixErrorBinding? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    activityKiwixErrorBinding = ActivityKiwixErrorBinding.inflate(layoutInflater)
    setContentView(activityKiwixErrorBinding!!.root)
    val extras = intent.extras
    exception = if (extras != null && safeContains(extras)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        extras.getSerializable(EXCEPTION_KEY, Throwable::class.java)
      } else {
        @Suppress("DEPRECATION")
        extras.getSerializable(EXCEPTION_KEY) as Throwable
      }
    } else {
      null
    }
    setupReportButton()
    activityKiwixErrorBinding?.restartButton?.setOnClickListener { restartApp() }
  }

  override fun onDestroy() {
    super.onDestroy()
    activityKiwixErrorBinding = null
  }

  private fun setupReportButton() {
    activityKiwixErrorBinding?.reportButton?.setOnClickListener {
      sendEmailLauncher.launch(Intent.createChooser(emailIntent(), "Send email..."))
    }
  }

  private val sendEmailLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      restartApp()
    }

  private fun emailIntent(): Intent {
    val emailBody = buildBody()
    return Intent(Intent.ACTION_SEND).apply {
      type = "vnd.android.cursor.dir/email"
      putExtra(
        Intent.EXTRA_EMAIL,
        arrayOf("android-crash-feedback@kiwix.org")
      )
      putExtra(Intent.EXTRA_SUBJECT, subject)
      putExtra(Intent.EXTRA_TEXT, emailBody)
      if (activityKiwixErrorBinding?.allowLogs?.isChecked == true) {
        val file = fileLogger.writeLogFile(this@ErrorActivity)
        file.appendText(emailBody)
        val path =
          FileProvider.getUriForFile(
            this@ErrorActivity,
            applicationContext.packageName + ".fileprovider",
            file
          )
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(android.content.Intent.EXTRA_STREAM, path)
      }
    }
  }

  private fun buildBody(): String = """ 
  $initialBody
    
  ${if (activityKiwixErrorBinding?.allowCrash?.isChecked == true && exception != null) exceptionDetails() else ""}
  ${if (activityKiwixErrorBinding?.allowZims?.isChecked == true) zimFiles() else ""}
  ${if (activityKiwixErrorBinding?.allowLanguage?.isChecked == true) languageLocale() else ""}
  ${if (activityKiwixErrorBinding?.allowDeviceDetails?.isChecked == true) deviceDetails() else ""}
  ${if (activityKiwixErrorBinding?.allowFileSystemDetails?.isChecked == true) systemDetails() else ""} 
  
  """.trimIndent()

  private fun exceptionDetails(): String =
    """
    Exception Details:
    ${toStackTraceString(exception!!)}
    """.trimIndent()

  private fun zimFiles(): String {
    val allZimFiles = bookDao.getBooks().joinToString {
      """
      ${it.book.title}:
      Articles: [${it.book.articleCount}]
      Creator: [${it.book.creator}]
      """.trimIndent()
    }
    return """
        Current Zim File:
        ${zimReaderContainer.zimCanonicalPath}
        All Zim Files in DB:
        $allZimFiles
        
    """.trimIndent()
  }

  private fun languageLocale(): String = """
    Current Locale:
    ${getCurrentLocale(applicationContext)}
    
  """.trimIndent()

  private fun deviceDetails(): String = """
    BluetoothClass.Device Details:
    Device:[${Build.DEVICE}]
    Model:[${Build.MODEL}]
    Manufacturer:[${Build.MANUFACTURER}]
    Time:[${Build.TIME}]
    Android Version:[${Build.VERSION.RELEASE}]
    App Version:[$versionName $versionCode]
    
  """.trimIndent()

  private fun systemDetails(): String = """
    Mount Points
    ${mountPointProducer.produce().joinToString { "$it\n" }}
    External Directories
    ${externalFileDetails()}
  """.trimIndent()

  private fun externalFileDetails(): String =
    ContextCompat.getExternalFilesDirs(this, null).joinToString("\n") { it?.path ?: "null" }

  private fun safeContains(extras: Bundle): Boolean {
    return try {
      extras.containsKey(EXCEPTION_KEY)
    } catch (ignore: RuntimeException) {
      false
    }
  }

  protected open val subject: String
    get() = "Someone has reported a crash"

  protected open val initialBody: String
    get() = """
      Hi Kiwix Developers!
      The Android app crashed, here are some details to help fix it:
    """.trimIndent()

  private val versionCode: Int
    @SuppressLint("WrongConstant")
    get() = packageManager
      .getPackageInformation(packageName, ZERO).getVersionCode()

  private val versionName: String
    @SuppressLint("WrongConstant")
    get() = packageManager
      .getPackageInformation(packageName, ZERO).versionName

  private fun toStackTraceString(exception: Throwable): String =
    StringWriter().apply {
      exception.printStackTrace(PrintWriter(this))
    }.toString()

  open fun restartApp() {
    startActivity(packageManager.getLaunchIntentForPackage(packageName))
    finish()
    killCurrentProcess()
  }

  override fun injection(coreComponent: CoreComponent) {
    coreComponent.inject(this)
  }

  companion object {
    const val EXCEPTION_KEY = "exception"
    private fun killCurrentProcess() {
      Process.killProcess(Process.myPid())
      exitProcess(STATUS)
    }
  }
}
