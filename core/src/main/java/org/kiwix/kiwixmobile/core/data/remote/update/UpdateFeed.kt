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

package org.kiwix.kiwixmobile.core.data.remote.update

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root

@Root(name = "rss", strict = false)
@Namespace(reference = "http://www.w3.org/2005/Atom", prefix = "atom")
class UpdateFeed {
  @field:Element(name = "channel", required = false)
  var channel: Channel? = null
}

@Root(name = "channel", strict = false)
@Namespace(reference = "http://www.w3.org/2005/Atom", prefix = "atom")
class Channel {
  @field:ElementList(name = "item", inline = true, required = false)
  var items: List<Items>? = null // Also renamed from 'channels' to 'items' for clarity
}

@Root(name = "item", strict = false)
@Namespace(reference = "http://www.w3.org/2005/Atom", prefix = "atom")
class Items {
  @field:Element(name = "title", required = false)
  var title: String = ""

  @field:Element(name = "link", required = false)
  var link: String = ""

  @field:Element(name = "pubDate", required = false)
  var pubDate: String = "" // Optional: if you want to capture the publish date
}
