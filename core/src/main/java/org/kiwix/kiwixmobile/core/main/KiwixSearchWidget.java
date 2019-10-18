/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.main;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import org.kiwix.kiwixmobile.core.Intents;
import org.kiwix.kiwixmobile.core.R;

public class KiwixSearchWidget extends AppWidgetProvider {

  public static final String TEXT_CLICKED =
    "KiwixSearchWidget.TEXT_CLICKED";
  public static final String ICON_CLICKED =
    "KiwixSearchWidget.ICON_CLICKED";
  public static final String MIC_CLICKED =
    "KiwixSearchWidget.MIC_CLICKED";
  public static final String STAR_CLICKED =
    "KiwixSearchWidget.STAR_CLICKED";

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    String appName = context.getApplicationContext().getResources().getString(R.string.app_name);

    for (int id : appWidgetIds) {
      RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.kiwix_search_widget);
      views.setTextViewText(R.id.search_widget_text, "Search " + appName);
      /** Search Kiwix intent **/
      Intent mainIntent = Intents.internal(CoreMainActivity.class);
      mainIntent.setAction(TEXT_CLICKED);
      PendingIntent searchPendingIntent =
        PendingIntent.getActivity(context, (int) (System.currentTimeMillis() % Integer.MAX_VALUE),
          mainIntent, 0);

      /** Kiwix icon intent to main app **/
      Intent kiwixIconIntent = Intents.internal(CoreMainActivity.class);
      kiwixIconIntent.setAction(ICON_CLICKED);
      PendingIntent mainAppPendingIntent =
        PendingIntent.getActivity(context, (int) (System.currentTimeMillis() % Integer.MAX_VALUE),
          kiwixIconIntent, 0);

      /** Star icon intent to bookmarks **/
      Intent starIntent = Intents.internal(CoreMainActivity.class);
      starIntent.setAction(STAR_CLICKED);
      PendingIntent starPendingIntent =
        PendingIntent.getActivity(context, (int) (System.currentTimeMillis() % Integer.MAX_VALUE),
          starIntent, 0);

      /** Microphone icon intent for voice search **/
      Intent voiceIntent = Intents.internal(CoreMainActivity.class);
      voiceIntent.setAction(MIC_CLICKED);
      PendingIntent voicePendingIntent =
        PendingIntent.getActivity(context, (int) (System.currentTimeMillis() % Integer.MAX_VALUE),
          voiceIntent, 0);

      views.setOnClickPendingIntent(R.id.search_widget_text, searchPendingIntent);
      views.setOnClickPendingIntent(R.id.search_widget_icon, mainAppPendingIntent);
      views.setOnClickPendingIntent(R.id.search_widget_star, starPendingIntent);
      views.setOnClickPendingIntent(R.id.search_widget_mic, voicePendingIntent);

      appWidgetManager.updateAppWidget(id, views);
    }
  }
}
