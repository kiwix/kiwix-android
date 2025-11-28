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
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.CoreApp.Companion.coreComponent
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getPackageInformation
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.getVersionCode
import org.kiwix.kiwixmobile.core.compat.CompatHelper.Companion.queryIntentActivitiesCompat
import org.kiwix.kiwixmobile.core.compat.ResolveInfoFlagsCompat
import org.kiwix.kiwixmobile.core.dao.LibkiwixBookOnDisk
import org.kiwix.kiwixmobile.core.downloader.downloadManager.APP_NAME_KEY
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isCustomApp
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.viewModel
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel.ValidationStatus
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel.ValidationStatus.Failed
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel.ValidationStatus.InProgress
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel.ValidationStatus.Pending
import org.kiwix.kiwixmobile.core.reader.integrity.ValidateZimViewModel.ValidationStatus.Success
import org.kiwix.kiwixmobile.core.utils.CRASH_AND_FEEDBACK_EMAIL_ADDRESS
import org.kiwix.kiwixmobile.core.utils.LanguageUtils.Companion.getCurrentLocale
import org.kiwix.kiwixmobile.core.utils.dialog.DialogConfirmButton
import org.kiwix.kiwixmobile.core.utils.dialog.KiwixBasicDialogFrame
import org.kiwix.kiwixmobile.core.utils.dialog.ValidateZimDialog
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
  lateinit var libkiwixBookOnDisk: LibkiwixBookOnDisk

  @Inject
  lateinit var zimReaderContainer: ZimReaderContainer

  @Inject
  lateinit var mountPointProducer: MountPointProducer

  @Inject
  lateinit var fileLogger: FileLogger

  private var exception: Throwable? = null
  var appName: String? = null

  open val crashTitle: Int = R.string.crash_title
  open val crashDescription: Int = R.string.crash_description
  private val showValidationDialog = MutableStateFlow(false)

  @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
  private val validateZimViewModel by lazy {
    viewModel<ValidateZimViewModel>(viewModelFactory)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    coreComponent.inject(this)
    super.onCreate(savedInstanceState)
    val extras = intent.extras
    exception =
      if (extras != null && safeContains(extras, EXCEPTION_KEY)) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
          extras.getSerializable(EXCEPTION_KEY, Throwable::class.java)
        } else {
          @Suppress("DEPRECATION")
          extras.getSerializable(EXCEPTION_KEY) as Throwable
        }
      } else {
        null
      }
    appName = if (extras != null && safeContains(extras, APP_NAME_KEY)) {
      extras.getString(APP_NAME_KEY)
    } else {
      null
    }
    setContent {
      val showDialog by showValidationDialog.collectAsState()
      ErrorActivityScreen(
        crashTitle,
        crashDescription,
        getDiagnosticDetailsItems(),
        { restartApp() },
        { sendDetailsOnMail() }
      )
      ShowValidatingZimFilesDialog(showDialog)
    }
  }

  @Composable
  private fun ShowValidatingZimFilesDialog(showDialog: Boolean) {
    if (!showDialog) return
    val items by validateZimViewModel.items.collectAsState()
    KiwixBasicDialogFrame(
      onDismissRequest = { dismissVerificationDialog() },
      cancelable = false,
    ) {
      ValidateZimDialog(items = items)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        DialogConfirmButton(
          stringResource(R.string.cancel),
          { dismissVerificationDialog() },
          null
        )
      }
    }
  }

  private fun dismissVerificationDialog() {
    showValidationDialog.value = false
  }

  /**
   * This list will create the "Details included" items in ErrorActivity.
   * Subclasses like DiagnosticReportActivity override this method to customize
   * the behavior, such as hiding the crashLogs item.
   *
   * WARNING: If modifying this method, ensure thorough testing with DiagnosticReportActivity
   *    to verify proper functionality.
   */
  open fun getDiagnosticDetailsItems(): List<Int> =
    listOf(
      R.string.crash_checkbox_language,
      R.string.crash_checkbox_logs,
      R.string.crash_checkbox_exception,
      R.string.crash_checkbox_zimfiles,
      R.string.crash_checkbox_device,
      R.string.crash_checkbox_file_system
    )

  private fun sendDetailsOnMail() {
    lifecycleScope.launch {
      startValidatingZIMFiles()
      validateZimViewModel.allZIMValidated
        .filter { it }
        .collect {
          dismissVerificationDialog()
          // Generate and send send report when all ZIM files are validated.
          val emailIntent = emailIntent()
          val activities = getSupportedEmailApps(emailIntent, supportedEmailPackages)
          val targetedIntents = createEmailIntents(emailIntent, activities)
          if (activities.isNotEmpty() && targetedIntents.isNotEmpty()) {
            val chooserIntent =
              Intent.createChooser(targetedIntents.removeAt(0), "Send email...")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedIntents.toTypedArray())
            sendEmailLauncher.launch(chooserIntent)
          } else {
            toast(getString(R.string.no_email_application_installed))
          }
        }
    }
  }

  private suspend fun startValidatingZIMFiles(dispatcher: CoroutineDispatcher = Dispatchers.IO) {
    val zimBooks = withContext(dispatcher) { libkiwixBookOnDisk.getBooks() }
    showValidationDialog.value = true
    CoroutineScope(dispatcher).launch {
      validateZimViewModel.startValidation(zimBooks, isCustomApp())
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
  private val supportedEmailPackages =
    listOf(
      "com.google.android.gm",
      "com.zoho.mail",
      "com.microsoft.office.outlook",
      "com.yahoo.mobile.client.android.mail",
      "me.bluemail.mail",
      "ch.protonmail.android",
      "com.fsck.k9",
      "com.maildroid",
      "org.kman.AquaMail",
      "com.edison.android.mail",
      "com.readdle.spark",
      "com.gmx.mobile.android.mail",
      "com.fastmail",
      "ru.mail.mailapp",
      "ru.yandex.mail",
      "de.tutao.tutanota"
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
      val file = fileLogger.writeLogFile(this@ErrorActivity)
      file.appendText(emailBody)
      val path =
        FileProvider.getUriForFile(
          this@ErrorActivity,
          applicationContext.packageName + ".fileprovider",
          file
        )
      addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      putExtra(Intent.EXTRA_STREAM, path)
      putExtra(Intent.EXTRA_TEXT, emailBody)
    }
  }

  /**
   * Builds a detailed crash report body.
   *
   * This function dynamically constructs a report. It gathers relevant
   * details such as exception information, ZIM file details, language settings,
   * device specifications, and file system details.
   *
   * @return A formatted string containing the selected crash report details.
   */
  private suspend fun buildBody(): String =
    """ 
    $initialBody
      
    ${if (getDiagnosticDetailsItems().contains(R.string.crash_checkbox_exception) && exception != null) exceptionDetails() else ""}
    ${zimFiles()}
    ${languageLocale()}
    ${deviceDetails()}
    ${systemDetails()}  
    
    """.trimIndent()

  private fun exceptionDetails(): String =
    """
    Exception Details:
    ${exception?.let(::toStackTraceString)}
    """.trimIndent()

  private suspend fun zimFiles(): String {
    val allZimFiles = validateZimViewModel.items.value.joinToString(separator = ",\n") {
      """
        ${it.book.book.title}:
        Articles: [${it.book.book.articleCount}]
        Creator: [${it.book.book.creator}]
        Validation status: [${it.status.getValidationStatus()}]
      """.trimIndent()
    }
    return """
      Current Zim File:
      ${zimReaderContainer.zimReaderSource?.toDatabase()}
      All Zim Files in DB:
      $allZimFiles
      
      """.trimIndent()
  }

  /**
   * Returns the ZIM file validation status.
   *
   * These messages are intentionally hardcoded in English because they must always be shown in English,
   * so we are not using string resources here.
   */
  private fun ValidationStatus.getValidationStatus() =
    when (this) {
      Pending -> "Pending"
      InProgress -> "In Progress"
      Success -> "Valid ZIM file"
      is Failed -> "Invalid ZIM file; Error $error"
    }

  private fun languageLocale(): String =
    """
    Current Locale:
    ${getCurrentLocale(applicationContext)}
    
    """.trimIndent()

  private fun deviceDetails(): String =
    """
    BluetoothClass.Device Details:
    Device:[${Build.DEVICE}]
    Model:[${Build.MODEL}]
    Manufacturer:[${Build.MANUFACTURER}]
    Time:[${Build.TIME}]
    Android Version:[${Build.VERSION.RELEASE}]
    App Version:[$versionName $versionCode]
    
    """.trimIndent()

  private fun systemDetails(): String =
    """
    Mount Points
    ${mountPointProducer.produce().joinToString { "$it\n" }}
    External Directories
    ${externalFileDetails()}
    """.trimIndent()

  private fun externalFileDetails(): String =
    getExternalFilesDirs(null).joinToString("\n") { it?.path ?: "null" }

  private fun safeContains(extras: Bundle, key: String): Boolean {
    return try {
      extras.containsKey(key)
    } catch (_: RuntimeException) {
      false
    }
  }

  protected open val subject: String
    get() = "Someone has reported a crash"

  protected open val initialBody: String by lazy {
    """
      Hi Kiwix Developers!
      The "$appName" app crashed, here are some details to help fix it:
    """.trimIndent()
  }

  private val versionCode: Int
    @SuppressLint("WrongConstant")
    get() =
      packageManager
        .getPackageInformation(packageName, ZERO).getVersionCode()

  private val versionName: String
    @SuppressLint("WrongConstant")
    get() =
      packageManager
        .getPackageInformation(packageName, ZERO).versionName.toString()

  private fun toStackTraceString(exception: Throwable): String =
    try {
      StringWriter().apply {
        exception.printStackTrace(PrintWriter(this))
      }.toString()
    } catch (_: Exception) {
      // Some exceptions thrown by coroutines do not have a stack trace.
      // These exceptions contain the full error message in the exception object itself.
      // To handle these cases, log the full exception message as it contains the
      // main cause of the error.
      StringWriter().append("$exception").toString()
    }

  open fun restartApp() {
    val restartAppIntent =
      packageManager.getLaunchIntentForPackage(packageName)?.apply {
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
