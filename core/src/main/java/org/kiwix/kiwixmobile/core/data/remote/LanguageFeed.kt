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
import org.kiwix.kiwixmobile.core.utils.ZERO
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName

@Serializable
@XmlSerialName("feed", namespace = "http://www.w3.org/2005/Atom", prefix = "")
data class LanguageFeed(
  @XmlSerialName("entry", namespace = "http://www.w3.org/2005/Atom", prefix = "")
  var entries: List<LanguageEntry>? = null
)

@Serializable
@XmlSerialName("entry", namespace = "http://www.w3.org/2005/Atom", prefix = "")
data class LanguageEntry(
  @XmlElement(true)
  @XmlSerialName("title", namespace = "http://www.w3.org/2005/Atom", prefix = "")
  var title: String = "",
  @XmlElement(true)
  @XmlSerialName("language", namespace = "http://purl.org/dc/terms/", prefix = "dc")
  var languageCode: String = "",
  @XmlElement(true)
  @XmlSerialName("count", namespace = "http://purl.org/syndication/thread/1.0", prefix = "thr")
  var count: Int = ZERO
)
