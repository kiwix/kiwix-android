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

import android.R.id
import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.kiwix.kiwixmobile.BaseRobot
import java.io.File

inline fun <reified T : BaseRobot> T.applyWithViewHierarchyPrinting(
  activity: Activity,
  crossinline function: T.() -> Unit
): T =
  apply {
    try {
      function()
    } catch (runtimeException: RuntimeException) {
      uiDevice.takeScreenshot(File("${context.filesDir}${runtimeException.message}${System.currentTimeMillis()}"))
      throw RuntimeException(combineMessages(runtimeException, activity))
    }
  }

fun combineMessages(
  runtimeException: RuntimeException,
  activity: Activity
) = "${runtimeException.message}\n${getViewHierarchy(activity.findViewById(id.content))}"

fun getViewHierarchy(v: View): String {
  val desc = StringBuilder()
  getViewHierarchy(v, desc, 0)
  return desc.toString()
}

private fun getViewHierarchy(v: View, desc: StringBuilder, margin: Int) {
  desc.append(getViewMessage(v, margin))
  if (v is ViewGroup) {
    for (i in 0 until v.childCount) {
      getViewHierarchy(v.getChildAt(i), desc, margin + 1)
    }
  }
}

private fun getViewMessage(v: View, marginOffset: Int) =
  "${numSpaces(marginOffset)}[${v.javaClass.simpleName}]${resourceId(v)}${text(v)}${contentDescription(
    v
  )}\n"

fun contentDescription(view: View) =
  view.contentDescription?.let {
    if (it.isNotEmpty()) " contDesc: $it"
    else null
  } ?: ""

fun text(v: View) =
  if (TextView::class.java.isAssignableFrom(v.javaClass))
    (v as TextView).let {
      if (it.text.isNotEmpty()) " text:${v.text}"
      else ""
    }
  else ""

private fun resourceId(view: View) =
  if (view.id > 0) " id:${view.resources.getResourceName(view.id)}"
  else ""

private fun numSpaces(marginOffset: Int) = (0..marginOffset).fold("", { acc, i -> "${acc}_" })
