package org.kiwix.kiwixmobile.common.utils;

import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import org.kiwix.kiwixmobile.common.views.table.TableDrawerAdapter;

import java.util.ArrayList;
import java.util.List;

import static org.kiwix.kiwixmobile.common.views.table.TableDrawerAdapter.DocumentSection;

public class DocumentParser {

  private String title;
  private SectionsListener listener;
  private List<TableDrawerAdapter.DocumentSection> sections;

  public DocumentParser(SectionsListener listener) {
    this.listener = listener;
  }

  public void initInterface(WebView webView) {
    webView.addJavascriptInterface(new ParserCallback(), "DocumentParser");
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
      sections = new ArrayList<>();
      new Handler(Looper.getMainLooper()).post(() -> listener.clearSections());
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void stop() {
      new Handler(Looper.getMainLooper()).post(() -> listener.sectionsLoaded(title, sections));
    }
  }

  public interface SectionsListener {
    void sectionsLoaded(String title, List<DocumentSection> sections);

    void clearSections();
  }
}


