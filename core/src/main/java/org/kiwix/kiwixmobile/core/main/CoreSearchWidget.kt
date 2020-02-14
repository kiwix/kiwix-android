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
package org.kiwix.kiwixmobile.core.main

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import kotlin.reflect.KClass
import org.kiwix.kiwixmobile.core.R

abstract class CoreSearchWidget : AppWidgetProvider() {

  abstract val activityKClass: KClass<*>

  override fun onUpdate(
    context: Context,
    appWidgetManager: AppWidgetManager,
    appWidgetIds: IntArray
  ) {
    val appName = context.getString(R.string.app_name)
    appWidgetIds.forEach { appWidgetId ->
      val views = RemoteViews(context.packageName, R.layout.kiwix_search_widget)
      views.setTextViewText(R.id.search_widget_text, "Search $appName")
      idsToActions.forEach { (id, action) ->
        views.setOnClickPendingIntent(id, pendingIntent(context, action))
      }
      appWidgetManager.updateAppWidget(appWidgetId, views)
    }
  }

  private fun pendingIntent(context: Context, action: String) = PendingIntent.getActivity(
    context,
    (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
    Intent(context, activityKClass.java).setAction(action),
    0
  )

  companion object {
    const val TEXT_CLICKED = "KiwixSearchWidget.TEXT_CLICKED"
    const val MIC_CLICKED = "KiwixSearchWidget.MIC_CLICKED"
    const val STAR_CLICKED = "KiwixSearchWidget.STAR_CLICKED"
    private const val ICON_CLICKED = "KiwixSearchWidget.ICON_CLICKED"

    private val idsToActions = mapOf(
      R.id.search_widget_text to TEXT_CLICKED,
      R.id.search_widget_icon to ICON_CLICKED,
      R.id.search_widget_star to STAR_CLICKED,
      R.id.search_widget_mic to MIC_CLICKED
    )
  }
}
