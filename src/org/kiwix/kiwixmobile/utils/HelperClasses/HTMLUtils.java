package org.kiwix.kiwixmobile.utils.HelperClasses;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.HeaderViewListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import java.util.List;
import org.kiwix.kiwixmobile.KiwixMobileActivity;

public class HTMLUtils {

  private List<KiwixMobileActivity.SectionProperties> sectionProperties;
  private List<TextView> textViews;
  private ArrayAdapter arrayAdapter;
  private KiwixMobileActivity context;
  private Handler mHandler;

  public HTMLUtils(List<KiwixMobileActivity.SectionProperties> sectionProperties, List<TextView> textViews, ListView listView, KiwixMobileActivity context, Handler handler) {
    this.sectionProperties = sectionProperties;
    this.textViews = textViews;
    this.arrayAdapter = ((ArrayAdapter)((HeaderViewListAdapter)listView.getAdapter()).getWrappedAdapter());
    this.context = context;
    this.mHandler = handler;
  }

  public void initInterface(WebView webView) {
    webView.addJavascriptInterface(new HTMLinterface(), "HTMLUtils");
  }


  class HTMLinterface {
    int i = 0;

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void parse(final String sectionTitle, final String element, final String id) {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          if (element.equals("H1")) {
              KiwixMobileActivity.headerView.setText(sectionTitle);
          } else {
            textViews.add(i, new TextView(context));
            sectionProperties.add(i, new KiwixMobileActivity.SectionProperties());
            KiwixMobileActivity.SectionProperties section = sectionProperties.get(i);
            section.sectionTitle = sectionTitle;
            section.sectionId = id;
            switch (element) {
              case "H2":
                section.leftPadding = (int) (16 * context.getResources().getDisplayMetrics().density);
                section.typeface = Typeface.DEFAULT;
                section.color = Color.BLACK;
                break;
              case "H3":
                section.leftPadding = (int) (36 * context.getResources().getDisplayMetrics().density);
                section.typeface = Typeface.DEFAULT;
                section.color = Color.GRAY;
                break;
              default:
                section.leftPadding = (int) (16 * context.getResources().getDisplayMetrics().density);
                section.typeface = Typeface.DEFAULT;
                section.color = Color.BLACK;
                break;
            }
            i++;
          }
        }
      });
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void start() {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          i = 0;
          textViews.clear();
          sectionProperties.clear();
          arrayAdapter.clear();
          arrayAdapter.notifyDataSetChanged();
        }
      });
    }

    @JavascriptInterface
    @SuppressWarnings("unused")
    public void stop() {
      mHandler.post(new Runnable() {
        @Override
        public void run() {
          arrayAdapter.notifyDataSetChanged();
        }
      });
    }
  }
}


