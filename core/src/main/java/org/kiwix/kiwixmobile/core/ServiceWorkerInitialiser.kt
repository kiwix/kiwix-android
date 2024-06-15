/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core

import android.app.ActivityManager
import android.content.Context
import android.os.Process
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebViewFeature
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import javax.inject.Inject

class ServiceWorkerInitialiser @Inject constructor(val zimReaderContainer: ZimReaderContainer) {
  fun init(context: Context) {
    if (isMainProcess(context) &&
      WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)
    ) {
      ServiceWorkerControllerCompat.getInstance()
        .setServiceWorkerClient(object : ServiceWorkerClientCompat() {
          override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? =
            zimReaderContainer.load(request.url.toString(), request.requestHeaders)
        })
    }
  }

  private fun isMainProcess(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    return activityManager.runningAppProcesses.find {
      it.pid == Process.myPid()
    }?.processName == context.packageName
  }
}
