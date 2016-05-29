package org.kiwix.kiwixmobile.utils;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import org.kiwix.kiwixmobile.KiwixMobileActivity;
import org.kiwix.kiwixmobile.R;

public class KiwixSearchWidget extends AppWidgetProvider {

  private static final String TEXT_CLICKED = "SearchKiwixActionClicked";
  private static final String MIC_CLICKED = "MicSearchActionClicked";


  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    final int N = appWidgetIds.length;

    for (int i=0; i<N; i++) {
      int appWidgetId = appWidgetIds[i];

      Intent intent = new Intent(context, KiwixMobileActivity.class);
      intent.putExtra("isWidget", true);
      PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

      // Get the layout for the App Widget and attach an on-click listener
      // to the button
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.kiwix_search_widget);
      views.setOnClickPendingIntent(R.id.search_widget_text, pendingIntent);

      // Tell the AppWidgetManager to perform an update on the current app widget
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }

  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);


  }

  protected PendingIntent getPendingSelfIntent(Context context, String action) {
    Intent intent = new Intent(context, getClass());
    intent.setAction(action);
    return PendingIntent.getBroadcast(context, 0, intent, 0);
  }


}
