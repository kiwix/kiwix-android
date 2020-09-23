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

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebViewFeature
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import javax.inject.Inject

const val UNREGISTER_SERVICE_WORKER_JS =
  """
    console.log('Commencing deregistration');
    navigator.serviceWorker.getRegistrations().then(function (registrations) {
      if (!registrations.length) {
        console.log('No serviceWorker registrations found.')
        return
      }
      for(let registration of registrations) {
      alert('Attempting to unregister Service Worker' + registration.index);
        registration.unregister().then(function (boolean) {
          console.log(
            (boolean ? 'Successfully unregistered' : 'Failed to unregister'), 'ServiceWorkerRegistration\n' +
            (registration.installing ? '  .installing.scriptURL = ' + registration.installing.scriptURL + '\n' : '') +
            (registration.waiting ? '  .waiting.scriptURL = ' + registration.waiting.scriptURL + '\n' : '') +
            (registration.active ? '  .active.scriptURL = ' + registration.active.scriptURL + '\n' : '') +
            '  .scope: ' + registration.scope + '\n'
          )
        })
      }
    }).catch(function(err) { console.error(err); });
  """

class ServiceWorkerInitialiser @Inject constructor(zimReaderContainer: ZimReaderContainer) {
  init {
    if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
      ServiceWorkerControllerCompat.getInstance()
        .setServiceWorkerClient(object : ServiceWorkerClientCompat() {
          override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? =
            zimReaderContainer.load(request.url.toString())
        })
    }
  }
}
