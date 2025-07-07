/*
 * Kiwix Android
 * Copyright (c) 2025 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.data.remote

import org.kiwix.kiwixmobile.core.downloader.downloadManager.ZERO
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root

@Root(name = "feed", strict = false)
@Namespace(reference = "http://www.w3.org/2005/Atom")
class LanguageFeed {
  @field:ElementList(name = "entry", inline = true, required = false)
  var entries: List<LanguageEntry>? = null
}

@Root(name = "entry", strict = false)
@Namespace(reference = "http://www.w3.org/2005/Atom")
class LanguageEntry {
  @field:Element(name = "title", required = false)
  var title: String = ""

  @field:Element(name = "language", required = false)
  @Namespace(prefix = "dc", reference = "http://purl.org/dc/terms/")
  var languageCode: String = ""

  @field:Element(name = "count", required = false)
  @Namespace(prefix = "thr", reference = "http://purl.org/syndication/thread/1.0")
  var count: Int = ZERO
}
