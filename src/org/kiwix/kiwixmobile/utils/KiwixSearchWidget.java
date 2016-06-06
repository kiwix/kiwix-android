/*
 * Copyright 2013  Elad Keyshawn <elad.keyshawn@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */


package org.kiwix.kiwixmobile.utils;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.TextView;

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
    String appName = context.getApplicationContext().getResources().getString(R.string.app_name);


    for (int i = 0; i < N; i++) {
      int appWidgetId = appWidgetIds[i];

      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.kiwix_search_widget);
      views.setTextViewText(R.id.search_widget_text, "Search " + appName);
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
      PendingIntent starPendingIntent = PendingIntent.getActivity(context, 0, starIntent, 0);


      /** Microphone icon intent for voice search **/
      Intent voiceIntent = new Intent(context, KiwixMobileActivity.class);
      voiceIntent.putExtra("isWidgetVoice", true);
      voiceIntent.setAction(MIC_CLICKED);
      PendingIntent voicePendingIntent = PendingIntent.getActivity(context, 0, voiceIntent, 0);

      views.setOnClickPendingIntent(R.id.search_widget_text, searchPendingIntent);
      views.setOnClickPendingIntent(R.id.search_widget_icon, mainAppPendingIntent);
      views.setOnClickPendingIntent(R.id.search_widget_star, starPendingIntent);
      views.setOnClickPendingIntent(R.id.search_widget_mic, voicePendingIntent);

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
