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

import kotlinx.serialization.Serializable
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("feed", namespace = "http://www.w3.org/2005/Atom", prefix = "")
data class CategoryFeed(
  @XmlSerialName("entry", namespace = "http://www.w3.org/2005/Atom", prefix = "")
  var entries: List<CategoryEntry>? = null
)

@Serializable
@XmlSerialName("entry", namespace = "http://www.w3.org/2005/Atom", prefix = "")
data class CategoryEntry(
  @XmlElement(true)
  @XmlSerialName("title", namespace = "http://www.w3.org/2005/Atom", prefix = "")
  var title: String = "",
  @XmlElement(true)
  @XmlSerialName("id", namespace = "http://www.w3.org/2005/Atom", prefix = "")
  var id: String = "",
  @XmlElement(true)
  @XmlSerialName("updated", namespace = "http://www.w3.org/2005/Atom", prefix = "")
  var updated: String = "",
  @XmlElement(true)
  @XmlSerialName("content", namespace = "http://www.w3.org/2005/Atom", prefix = "")
  var content: String = "",
  @XmlElement(true)
  @XmlSerialName("link", namespace = "http://www.w3.org/2005/Atom", prefix = "")
  var link: CategoryLink? = null
)

@Serializable
@XmlSerialName("link", namespace = "http://www.w3.org/2005/Atom", prefix = "")
data class CategoryLink(
  @XmlElement(false)
  var rel: String = "",
  @XmlElement(false)
  var href: String = "",
  @XmlElement(false)
  var type: String = ""
)
