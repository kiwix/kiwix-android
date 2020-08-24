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
import kotlin.collections.List
import kotlin.collections.ArrayList

import org.kiwix.kiwixmobile.core.main.TableDrawerAdapter.DocumentSection

public class DocumentParser(private var listener: DocumentParser.SectionsListener) {

  private var title: String = ""
  private var sections = ArrayList<TableDrawerAdapter.DocumentSection>()

  public fun initInterface(webView: WebView) {
    webView.addJavascriptInterface(ParserCallback(), "Documentparser")
  }

  public interface SectionsListener {
    fun sectionsLoaded(title: String, sections: List<DocumentSection>)

    fun clearSections()
  }

  inner class ParserCallback() {

    @JavascriptInterface
    public fun parse(sectionTitle: String, element: String, id: String) {

      if (element == "H1") {
        title = sectionTitle.trim()
        return
      }
      sections.add(DocumentSection().apply {
        this.id = id
        title = sectionTitle.trim()
        level = element.takeLast(element.length - 1).toIntOrNull() ?: 0
      })
    }

    @JavascriptInterface
    public fun start() {
      title = ""
      sections = ArrayList()
      Handler(Looper.getMainLooper()).post(Runnable(listener::clearSections))
    }

    @JavascriptInterface
    public fun stop() {
      val listToBeSentToMainThread: List<DocumentSection> = ArrayList(sections)
      Handler(Looper.getMainLooper()).post {
        listener.sectionsLoaded(
          title,
          listToBeSentToMainThread
        )
      }
    }
  }
}

