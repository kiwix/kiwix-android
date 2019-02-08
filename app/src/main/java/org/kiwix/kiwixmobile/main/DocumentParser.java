/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.main;

import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import java.util.ArrayList;
import java.util.List;

import static org.kiwix.kiwixmobile.main.TableDrawerAdapter.DocumentSection;

public class DocumentParser {

  private String title;
  private SectionsListener listener;
  private List<TableDrawerAdapter.DocumentSection> sections;

  DocumentParser(SectionsListener listener) {
    this.listener = listener;
  }

  public void initInterface(WebView webView) {
    webView.addJavascriptInterface(new ParserCallback(), "DocumentParser");
  }

  public interface SectionsListener {
    void sectionsLoaded(String title, List<DocumentSection> sections);

    void clearSections();
  }

  class ParserCallback {

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void parse(final String sectionTitle, final String element, final String id) {
      if (element.equals("H1")) {
        title = sectionTitle.trim();
        return;
      }
      DocumentSection section = new DocumentSection();
      section.title = sectionTitle.trim();
      section.id = id;
      int level;
      try {
        String character = element.substring(element.length() - 1);
        level = Integer.parseInt(character);
      } catch (NumberFormatException e) {
        level = 0;
      }
      section.level = level;
      sections.add(section);
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void start() {
      title = "";
      sections = new ArrayList<>();
      new Handler(Looper.getMainLooper()).post(() -> listener.clearSections());
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void stop() {
      new Handler(Looper.getMainLooper()).post(() -> listener.sectionsLoaded(title, sections));
    }
  }
}


