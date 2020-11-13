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

package org.kiwix.kiwixmobile.core.main

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView

const val UNINITIALISER_ADDRESS = "A/remove_service_workers.html"
const val UNINITIALISE_HTML = """
  <html>
    <head>
        <title>...</title>
        <script type="text/javascript">console.log("** INSIDE BLANK **");
          function do_unregister() {
            if (!navigator.serviceWorker) {
              return;
            }
            navigator.serviceWorker.getRegistrations().then(async function (registrations) {
              if (registrations.length) {
                console.debug('we do have ' + registrations.length + ' registration(s)');
                var registration = registrations[0];
                registration.unregister()
                    .then(function (success) { ServiceWorkerUninitialiser.onUninitialised();})
                    .catch(function (e) {alert("ERR:" + e)});
              }
              else {
                ServiceWorkerUninitialiser.onUninitialised();
              }
            });
          }
        do_unregister();
        </script>
    </head>
    <h1>---</h1>
</html>
"""

class ServiceWorkerUninitialiser(val onUninitialisedAction: () -> Unit) {

  @JavascriptInterface
  fun onUninitialised() {
    Handler(Looper.getMainLooper()).post {
      onUninitialisedAction()
    }
  }

  fun initInterface(webView: WebView) {
    webView.addJavascriptInterface(this, "ServiceWorkerUninitialiser")
  }
}
