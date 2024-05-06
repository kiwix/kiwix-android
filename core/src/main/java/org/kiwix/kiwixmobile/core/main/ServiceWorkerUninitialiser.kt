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
private const val UNINITIALISER_INTERFACE = "ServiceWorkerUninitialiser"
const val UNINITIALISE_HTML = """
  <html>
    <head>
        <script type="text/javascript">
          function do_unregister() {
            if (!navigator.serviceWorker) {
              $UNINITIALISER_INTERFACE.onUninitialised();
              return;
            }
            navigator.serviceWorker.getRegistrations().then((registrations) => {
              if (registrations.length) {
                console.debug('we do have ' + registrations.length + ' registration(s)');
                var registration = registrations[0];
                registration.unregister()
                    .then(function (success) { $UNINITIALISER_INTERFACE.onUninitialised();})
                    .catch(function (e) {alert("ERR:" + e)});
              }
              else {
                $UNINITIALISER_INTERFACE.onUninitialised();
              }
            });
          }
          do_unregister();
        </script>
    </head>
    <body>
      <div id="loading" style="width: 100%; text-align: center">
          <svg width="38" height="38" viewBox="0 0 38 38" xmlns="http://www.w3.org/2000/svg" stroke="#fff">
              <g fill="none" fill-rule="evenodd">
                  <g transform="translate(1 1)" stroke-width="2">
                      <path d="M36 18c0-9.94-8.06-18-18-18" stroke="gray">
                          <animateTransform
                              attributeName="transform"
                              type="rotate"
                              from="0 18 18"
                              to="360 18 18"
                              dur="1s"
                              repeatCount="indefinite"/>
                      </path>
                  </g>
              </g>
          </svg>
      </div>
    </body>
</html>
"""

class ServiceWorkerUninitialiser(val onUninitialisedAction: () -> Unit) {

  fun initInterface(webView: WebView) {
    webView.addJavascriptInterface(this, UNINITIALISER_INTERFACE)
  }

  @JavascriptInterface
  fun onUninitialised() {
    Handler(Looper.getMainLooper()).post { onUninitialisedAction.invoke() }
  }
}
