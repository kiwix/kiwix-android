/*
 * Kiwix Android
 * Copyright (c) 2026 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.utils

import android.app.AppOpsManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.util.Base64
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.toast
import org.kiwix.kiwixmobile.core.main.PAGE_URL_KEY
import org.kiwix.kiwixmobile.core.main.ZIM_FILE_URI_KEY
import org.kiwix.kiwixmobile.core.reader.ZimFileReader
import org.kiwix.kiwixmobile.core.utils.files.Log

/**
 * Represents the result of a shortcut creation attempt.
 * Allows callers to handle each failure case appropriately.
 */
sealed class ShortcutResult {
  object Success : ShortcutResult()
  object NotSupported : ShortcutResult()
  object ReaderNull : ShortcutResult()
  object LaunchIntentNotFound : ShortcutResult()
  object ShortcutInfoCreationFailed : ShortcutResult()
}

object ShortcutUtils {
  private const val TAG = "ShortcutUtils"
  private const val ADAPTIVE_ICON_SIZE_DP = 108
  private const val ADAPTIVE_SAFE_ZONE_SIZE_DP = 72
  private const val MIUI_INSTALL_SHORTCUT_OP_CODE = 10017

  /**
   * Attempts to add a pinned shortcut for a ZIM book to the home screen.
   * Returns a [ShortcutResult] indicating success or the specific failure reason.
   */
  fun addBookShortcut(
    context: Context,
    zimFileReader: ZimFileReader?,
    pageUrl: String?,
    customName: String? = null
  ): ShortcutResult {
    val componentName = getLauncherComponentName(context)
    return when {
      zimFileReader == null -> ShortcutResult.ReaderNull.also {
        Log.e(TAG, "Cannot create shortcut: zimFileReader is null")
      }
      !ShortcutManagerCompat.isRequestPinShortcutSupported(context) -> ShortcutResult.NotSupported.also {
        Log.e(TAG, "Pinned shortcuts are NOT supported by this launcher/device")
        context.toast(R.string.shortcut_disabled_message)
      }
      componentName == null -> ShortcutResult.LaunchIntentNotFound.also {
        Log.e(TAG, "Launcher activity not found for package: ${context.packageName}")
      }
      else -> {
        performPinning(context, zimFileReader, pageUrl, customName, componentName)
      }
    }
  }

  private fun getLauncherComponentName(context: Context): android.content.ComponentName? {
    return context.packageManager.getLaunchIntentForPackage(context.packageName)?.component
  }

  @Suppress("LongMethod")
  private fun performPinning(
    context: Context,
    zimFileReader: ZimFileReader,
    pageUrl: String?,
    customName: String?,
    componentName: android.content.ComponentName
  ): ShortcutResult {
    val displayTitle = customName?.takeIf { it.isNotBlank() }
      ?: zimFileReader.title.takeIf { it.isNotBlank() }
      ?: "Kiwix Book"
    val id = "kiwix_" + System.currentTimeMillis().toString()
    val zimFileUri = zimFileReader.zimReaderSource.toDatabase()

    val shortcutIntent = Intent(Intent.ACTION_VIEW).apply {
      setClassName(context, componentName.className)
      putExtra(ZIM_FILE_URI_KEY, zimFileUri)
      pageUrl?.let { putExtra(PAGE_URL_KEY, it) }
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

    val faviconBitmap = try {
      zimFileReader.favicon?.let { faviconBase64 ->
        val decodedString: ByteArray = Base64.decode(faviconBase64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
      }
    } catch (e: IllegalArgumentException) {
      Log.e(TAG, "Error decoding favicon for $displayTitle", e)
      null
    }

    val icon = createProfessionalIcon(context, faviconBitmap)
    val shortcutInfo = buildShortcutInfo(context, id, displayTitle, icon, shortcutIntent, componentName)

    return if (shortcutInfo != null) {
      val successCallback = PendingIntent.getBroadcast(
        context,
        id.hashCode(),
        Intent(context, ShortcutResultReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
      )
      ShortcutManagerCompat.requestPinShortcut(context, shortcutInfo, successCallback.intentSender)
      ShortcutResult.Success
    } else {
      ShortcutResult.ShortcutInfoCreationFailed
    }
  }

  private fun buildShortcutInfo(
    context: Context,
    id: String,
    displayTitle: String,
    icon: IconCompat,
    shortcutIntent: Intent,
    componentName: android.content.ComponentName
  ): ShortcutInfoCompat? {
    return try {
      ShortcutInfoCompat.Builder(context, id)
        .setActivity(componentName)
        .setShortLabel(displayTitle)
        .setLongLabel(displayTitle)
        .setIcon(icon)
        .setIntent(shortcutIntent)
        .build()
    } catch (
      @Suppress("TooGenericExceptionCaught")
      e: Exception
    ) {
      Log.e(TAG, "Failed to build ShortcutInfo for $displayTitle", e)
      null
    }
  }

  /**
   * BroadcastReceiver to handle the success callback from the launcher when a shortcut is pinned.
   */
  class ShortcutResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
      Log.d(TAG, "Launcher confirmed: Shortcut pinned successfully")
      context.toast(R.string.added_to_homescreen)
    }
  }

  /**
   * Creates a professional adaptive icon with a background and centered favicon.
   */
  private fun createProfessionalIcon(context: Context, favicon: Bitmap?): IconCompat {
    val density = context.resources.displayMetrics.density
    val sizePx = (ADAPTIVE_ICON_SIZE_DP * density).toInt()
    val safeZonePx = (ADAPTIVE_SAFE_ZONE_SIZE_DP * density).toInt()
    val insetPx = (sizePx - safeZonePx) / 2

    val bitmap = createBitmap(sizePx, sizePx)
    val canvas = Canvas(bitmap)

    // Draw background (Light gray)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFF5F5F5.toInt() }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)
    } else {
      val radius = sizePx / 2f
      canvas.drawCircle(radius, radius, radius, bgPaint)
    }

    // Draw favicon or default launcher icon in the center
    if (favicon != null) {
      val destRect = Rect(insetPx, insetPx, sizePx - insetPx, sizePx - insetPx)
      canvas.drawBitmap(favicon, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
    } else {
      // Fallback: Default launcher icon
      val defaultIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
      if (defaultIcon != null) {
        val destRect = Rect(insetPx, insetPx, sizePx - insetPx, sizePx - insetPx)
        canvas.drawBitmap(defaultIcon, null, destRect, Paint(Paint.FILTER_BITMAP_FLAG))
      }
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      IconCompat.createWithAdaptiveBitmap(bitmap)
    } else {
      IconCompat.createWithBitmap(bitmap)
    }
  }

  /**
   * Checks if the current device is a Xiaomi/MIUI device.
   */
  fun isXiaomiDevice(manufacturer: String = Build.MANUFACTURER): Boolean {
    return manufacturer.contains("Xiaomi", ignoreCase = true) ||
      manufacturer.contains("Redmi", ignoreCase = true) ||
      manufacturer.contains("POCO", ignoreCase = true) ||
      manufacturer.contains("Blackshark", ignoreCase = true)
  }

  /**
   * Attempts to open the MIUI-specific "Other permissions" editor.
   * Falls back to standard app settings if the MIUI one fails.
   */
  fun openMiuiPermissionEditor(context: Context) {
    val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
      setClassName(
        "com.miui.securitycenter",
        "com.miui.permcenter.permissions.PermissionsEditorActivity"
      )
      putExtra("extra_pkgname", context.packageName)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }

    try {
      context.startActivity(intent)
    } catch (
      @Suppress("TooGenericExceptionCaught")
      e: Exception
    ) {
      Log.e(TAG, "Failed to open MIUI permission editor, falling back to app settings", e)
      try {
        val fallbackIntent =
          Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
          }
        context.startActivity(fallbackIntent)
      } catch (
        @Suppress("TooGenericExceptionCaught")
        e2: Exception
      ) {
        Log.e(TAG, "Failed to open fallback app settings", e2)
      }
    }
  }

  /**
   * Checks if the "Home screen shortcuts" permission is granted on Xiaomi devices.
   * On non-Xiaomi devices, it returns true by default.
   */
  fun isShortcutPermissionGranted(context: Context): Boolean {
    if (!isXiaomiDevice()) return true

    return try {
      val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
      val method = appOpsManager.javaClass.getMethod(
        "checkOp",
        Int::class.javaPrimitiveType,
        Int::class.javaPrimitiveType,
        String::class.java
      )
      val result = method.invoke(
        appOpsManager,
        MIUI_INSTALL_SHORTCUT_OP_CODE,
        android.os.Process.myUid(),
        context.packageName
      ) as Int
      result == AppOpsManager.MODE_ALLOWED
    } catch (
      @Suppress("TooGenericExceptionCaught")
      e: Exception
    ) {
      // On non-MIUI ROMs (e.g. Poco with HyperOS/stock Android), op code 10017
      // doesn't exist, causing IllegalArgumentException("Bad operation #10017").
      // In that case, the device has no shortcut restriction, so return true.
      val rootCause = if (e is java.lang.reflect.InvocationTargetException) e.cause else e
      val isBadOperation = rootCause is IllegalArgumentException &&
        rootCause.message?.contains("Bad operation") == true

      if (isBadOperation) {
        Log.d(TAG, "MIUI shortcut op code not supported on this device, assuming granted")
      } else {
        Log.e(TAG, "Failed to check MIUI shortcut permission", e)
      }
      isBadOperation
    }
  }
}
