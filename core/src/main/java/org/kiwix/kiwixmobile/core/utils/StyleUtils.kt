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
package org.kiwix.kiwixmobile.core.utils

import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Html
import android.text.Spanned
import android.util.AttributeSet
import android.util.Xml
import androidx.annotation.XmlRes
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

object StyleUtils {
  @JvmStatic fun Context.getAttributes(@XmlRes xml: Int): AttributeSet {
    val parser: XmlPullParser = resources.getXml(xml)
    try {
      parser.next()
      parser.nextTag()
    } catch (e: XmlPullParserException) {
      e.printStackTrace()
    } catch (e: IOException) {
      e.printStackTrace()
    }
    return Xml.asAttributeSet(parser)
  }

  @Suppress("DEPRECATION")
  @JvmStatic fun String.fromHtml(): Spanned =
    if (VERSION.SDK_INT >= VERSION_CODES.N) Html.fromHtml(
      this,
      Html.FROM_HTML_MODE_LEGACY
    ) else Html.fromHtml(this)
}
