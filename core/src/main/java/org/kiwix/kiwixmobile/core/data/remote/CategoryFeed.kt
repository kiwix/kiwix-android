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

import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root

class CategoryFeed {
  @field:ElementList(name = "entry", inline = true, required = false)
  var entries: List<CategoryEntry>? = null
}

@Root(name = "entry", strict = false)
@Namespace(reference = "http://www.w3.org/2005/Atom")
class CategoryEntry {
  @field:Element(name = "title", required = false)
  var title: String = ""

  @field:Element(name = "id", required = false)
  var id: String = ""

  @field:Element(name = "updated", required = false)
  var updated: String = ""

  @field:Element(name = "content", required = false)
  var content: String = ""

  @field:Element(name = "link", required = false)
  var link: CategoryLink? = null
}

@Root(name = "link", strict = false)
@Namespace(reference = "http://www.w3.org/2005/Atom")
class CategoryLink {
  @field:Attribute(name = "rel", required = false)
  var rel: String = ""

  @field:Attribute(name = "href", required = false)
  var href: String = ""

  @field:Attribute(name = "type", required = false)
  var type: String = ""
}
