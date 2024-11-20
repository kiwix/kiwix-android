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
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.kiwix.kiwixmobile.core.CoreApp.Companion.coreComponent
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getVersionCode
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.queryIntentActivitiesCompat
import org.kiwix.kiwixmobile.core.compat.ResolveInfoFlagsCompat
import org.kiwix.kiwixmobile.core.dao.NewBookDao
import org.kiwix.kiwixmobile.core.databinding.ActivityKiwixErrorBinding
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.CRASH_AND_FEEDBACK_EMAIL_ADDRESS
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
    coreComponent.inject(this)
    super.onCreate(savedInstanceState)
    activityKiwixErrorBinding = ActivityKiwixErrorBinding.inflate(layoutInflater)
    setContentView(activityKiwixErrorBinding?.root)
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
      lifecycleScope.launch {
        val emailIntent = emailIntent()
        val activities = getSupportedEmailApps(emailIntent, supportedEmailPackages)
        val targetedIntents = createEmailIntents(emailIntent, activities)
        if (activities.isNotEmpty() && targetedIntents.isNotEmpty()) {
          val chooserIntent =
            Intent.createChooser(targetedIntents.removeFirst(), "Send email...")
          chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
          sendEmailLauncher.launch(chooserIntent)
        } else {
          toast(getString(R.string.no_email_application_installed))
        }
      }
    }
  }

  /**
   * Get a list of supported email apps.
   */
  private fun getSupportedEmailApps(
    emailIntent: Intent,
    supportedPackages: List<String>
  ): List<ResolveInfo> {
    return packageManager.queryIntentActivitiesCompat(emailIntent, ResolveInfoFlagsCompat.EMPTY)
      .filter {
        supportedPackages.any(it.activityInfo.packageName::contains)
      }
  }

  /**
   * Create a list of intents for supported email apps.
   */
  private fun createEmailIntents(
    emailIntent: Intent,
    activities: List<ResolveInfo>
  ): MutableList<Intent> {
    return activities.map { resolveInfo ->
      Intent(emailIntent).apply {
        setPackage(resolveInfo.activityInfo.packageName)
      }
    }.toMutableList()
  }

  // List of supported email apps
  private val supportedEmailPackages = listOf(
    "com.google.android.gm", // Gmail
    "com.zoho.mail", // Zoho Mail
    "com.microsoft.office.outlook", // Outlook
    "com.yahoo.mobile.client.android.mail", // Yahoo Mail
    "me.bluemail.mail", // BlueMail
    "ch.protonmail.android", // ProtonMail
    "com.fsck.k9", // K-9 Mail
    "com.maildroid", // Maildroid
    "org.kman.AquaMail", // Aqua Mail
    "com.edison.android.mail", // Edison Mail
    "com.readdle.spark", // Spark
    "com.gmx.mobile.android.mail", // GMX Mail
    "com.fastmail", // FastMail
    "ru.mail.mailapp", // Mail.ru
    "ru.yandex.mail", // Yandex.Mail
    "de.tutao.tutanota" // Tutanota
  )

  private val sendEmailLauncher =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      restartApp()
    }

  private suspend fun emailIntent(): Intent {
    val emailBody = buildBody()
    return Intent(Intent.ACTION_SEND).apply {
      type = "text/plain"
      putExtra(Intent.EXTRA_EMAIL, arrayOf(CRASH_AND_FEEDBACK_EMAIL_ADDRESS))
      putExtra(Intent.EXTRA_SUBJECT, subject)
      val file = fileLogger.writeLogFile(
        this@ErrorActivity,
        activityKiwixErrorBinding?.allowLogs?.isChecked == true
      )
      file.appendText(emailBody)
      val path =
        FileProvider.getUriForFile(
          this@ErrorActivity,
          applicationContext.packageName + ".fileprovider",
          file
        )
      addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
      putExtra(android.content.Intent.EXTRA_STREAM, path)
      putExtra(Intent.EXTRA_TEXT, emailBody)
    }
  }

  private suspend fun buildBody(): String = """ 
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
    ${exception?.let(::toStackTraceString)}
    """.trimIndent()

  private suspend fun zimFiles(): String {
    val allZimFiles = bookDao.getBooks().joinToString {
      """
      ${it.book.title}:
      Articles: [${it.book.articleCount}]
      Creator: [${it.book.creator}]
      """.trimIndent()
    }
    return """
        Current Zim File:
        ${zimReaderContainer.zimReaderSource?.toDatabase()}
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
    try {
      StringWriter().apply {
        exception.printStackTrace(PrintWriter(this))
      }.toString()
    } catch (ignore: Exception) {
      // Some exceptions thrown by coroutines do not have a stack trace.
      // These exceptions contain the full error message in the exception object itself.
      // To handle these cases, log the full exception message as it contains the
      // main cause of the error.
      StringWriter().append("$exception").toString()
    }

  open fun restartApp() {
    val restartAppIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
    startActivity(restartAppIntent)
    finish()
    killCurrentProcess()
  }

  companion object {
    const val EXCEPTION_KEY = "exception"
    private fun killCurrentProcess() {
      Process.killProcess(Process.myPid())
      exitProcess(STATUS)
    }
  }
}
