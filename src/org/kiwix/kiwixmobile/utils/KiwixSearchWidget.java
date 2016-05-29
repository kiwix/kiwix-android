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
  private static final String ICON_CLICKED = "KiwixIconActionClicked";
  private static final String MIC_CLICKED = "MicSearchActionClicked";
  private static final String STAR_CLICKED = "StarActionClicked";


  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    final int N = appWidgetIds.length;

    for (int i = 0; i < N; i++) {
      int appWidgetId = appWidgetIds[i];

      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.kiwix_search_widget);

      /** Search Kiwix intent **/
      Intent mainIntent = new Intent(context, KiwixMobileActivity.class);
      mainIntent.putExtra("isWidgetSearch", true);
      mainIntent.setAction(TEXT_CLICKED);
      PendingIntent searchPendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);

      /** Kiwix icon intent to main app **/
      Intent kiwixIconIntent = new Intent(context, KiwixMobileActivity.class);
      kiwixIconIntent.setAction(ICON_CLICKED);
      PendingIntent mainAppPendingIntent = PendingIntent.getActivity(context, 0, kiwixIconIntent, 0);

      /** Star icon intent to bookmarks **/
      Intent starIntent = new Intent(context, KiwixMobileActivity.class);
      starIntent.putExtra("isWidgetStar", true);
      starIntent.setAction(STAR_CLICKED);
      PendingIntent starPendingIntent = PendingIntent.getActivity(context,0,starIntent, 0);


      /** Microphone icon intent for voice search **/

      views.setOnClickPendingIntent(R.id.search_widget_text, searchPendingIntent);
      views.setOnClickPendingIntent(R.id.search_widget_icon, mainAppPendingIntent);
      views.setOnClickPendingIntent(R.id.search_widget_star, starPendingIntent);
      appWidgetManager.updateAppWidget(appWidgetId, views);
    }
  }

  public void onReceive(Context context, Intent intent) {
    super.onReceive(context, intent);

  }

//  protected PendingIntent getPendingSelfIntent(Context context, String action) {
//    Intent intent = new Intent(context, getClass());
//    intent.setAction(action);
//    return PendingIntent.getBroadcast(context, 0, intent, 0);
//  }


}
