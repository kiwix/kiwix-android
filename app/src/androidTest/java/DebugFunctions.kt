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

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage.RESUMED
import org.kiwix.kiwixmobile.BaseRobot
import java.io.File

inline fun <reified T : BaseRobot> T.applyWithViewHierarchyPrinting(
  crossinline function: T.() -> Unit
): T =
  apply {
    try {
      function()
    } catch (runtimeException: RuntimeException) {
      uiDevice.takeScreenshot(File(context.filesDir, "${System.currentTimeMillis()}.png"))
      InstrumentationRegistry.getInstrumentation().runOnMainSync {
        throw RuntimeException(
          combineMessages(
            runtimeException,
            ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(RESUMED).last()
          ),
          runtimeException
        )
      }
    }
  }

fun combineMessages(
  runtimeException: RuntimeException,
  activity: Activity
) = "${runtimeException.message}\n${getViewHierarchy(activity.window.decorView)}"

fun getViewHierarchy(v: View) =
  StringBuilder().apply { getViewHierarchy(v, this, 0) }.toString()

private fun getViewHierarchy(v: View, desc: StringBuilder, margin: Int) {
  desc.append(getViewMessage(v, margin))
  if (v is ViewGroup) {
    for (i in 0 until v.childCount) {
      getViewHierarchy(v.getChildAt(i), desc, margin + 1)
    }
  }
}

private fun getViewMessage(v: View, marginOffset: Int) =
  "${numSpaces(marginOffset)}[${v.javaClass.simpleName}]${resourceId(v)}${text(v)}" +
    "${contentDescription(v)}${visibility(v)}\n"

fun visibility(v: View) = " visibility:" +
  when (v.visibility) {
    View.VISIBLE -> "visible"
    View.INVISIBLE -> "invisible"
    else -> "gone"
  }

fun contentDescription(view: View) =
  view.contentDescription?.let {
    if (it.isNotEmpty()) " contDesc:$it"
    else null
  } ?: ""

fun text(v: View) =
  if (v is TextView)
    if (v.text.isNotEmpty()) " text:${v.text}"
    else ""
  else ""

private fun resourceId(view: View) =
  if (view.id > 0 && view.resources != null) " id:${view.resources.getResourceName(view.id)}"
  else ""

private fun numSpaces(marginOffset: Int) = (0..marginOffset).fold("", { acc, _ -> "$acc-" })
