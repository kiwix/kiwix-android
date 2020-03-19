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
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program. If not, see <http://www.gnu.org/licenses/>.
*
*/
package org.kiwix.kiwixmobile.core.entity

import org.hamcrest.CoreMatchers
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Assert
import org.junit.jupiter.api.Test
import org.simpleframework.xml.core.Persister

class MetaLinkNetworkEntityTest {
  private fun url(
    location: String, priority: Int, value: String
  ): Matcher<MetaLinkNetworkEntity.Url> = UrlMatcher(location, priority, value)

  @Test
  @Throws(Exception::class)
  fun testDeserialize() {
    val serializer = Persister()
    val result = serializer.read(
      MetaLinkNetworkEntity::class.java,
      MetaLinkNetworkEntityTest::class.java!!.classLoader.getResourceAsStream(
        "wikipedia_af_all_nopic_2016-05.zim.meta4"
      )
    )
    Assert.assertThat(result.urls.size, CoreMatchers.`is`(5))
    Assert.assertThat(
      result.urls, CoreMatchers.hasItems(
        url(
          "us", 1,
          "http://ftpmirror.your.org/pub/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
        ),
        url(
          "gb", 2,
          "http://www.mirrorservice.org/sites/download.kiwix.org/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
        ),
        url(
          "us", 3,
          "http://download.wikimedia.org/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
        ),
        url(
          "de", 4,
          "http://mirror.netcologne.de/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
        ),
        url("fr", 5, "http://mirror3.kiwix.org/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim")
      )
    )
    // Basic file attributes
    Assert.assertThat(
      result.getFile().getName(),
      CoreMatchers.`is`("wikipedia_af_all_nopic_2016-05.zim")
    )
    Assert.assertThat(result.getFile().size, CoreMatchers.`is`(63973123L))
    // File hashes
    Assert.assertThat(
      result.getFile().getHash("md5"),
      CoreMatchers.`is`("6f06866b61c4a921b57f28cfd4307220")
    )
    Assert.assertThat(
      result.getFile().getHash("sha-1"),
      CoreMatchers.`is`("8aac4c7f89e3cdd45b245695e19ecde5aac59593")
    )
    Assert.assertThat(
      result.getFile().getHash("sha-256"),
      CoreMatchers.`is`("83126775538cf588a85edb10db04d6e012321a2025278a08a084b258849b3a5c")
    )
    // Pieces
    Assert.assertThat(result.getFile().pieceHashType, CoreMatchers.`is`("sha-1"))
    Assert.assertThat(result.getFile().pieceLength, CoreMatchers.`is`(1048576))
    // Check only the first and the last elements of the piece hashes
    Assert.assertThat(result.getFile().pieceHashes.size, CoreMatchers.`is`(62))
    Assert.assertThat(
      result.getFile().pieceHashes[0],
      CoreMatchers.`is`("f36815d904d4fd563aaef4ee6ef2600fb1fd70b2")
    )
    Assert.assertThat(
      result.getFile().pieceHashes[61],
      CoreMatchers.`is`("8055e515aa6e78f2810bbb0e0cd07330838b8920")
    )
  }

  /**
   * Implemented as a matcher only to avoid putting extra code into {@code MetaLinkNetworkEntity}.
   * However in case {@code equals} and {@code hashCode} methods are added to
   * {@code MetaLinkNetworkEntity.Url} class itself, this Matcher should be deleted.
   */
  private class UrlMatcher(
    val location: String, val priority: Int,
    val value: String
  ) :

    TypeSafeMatcher<MetaLinkNetworkEntity.Url>() {

    override fun matchesSafely(item: MetaLinkNetworkEntity.Url): Boolean {
      return (location == item.location && priority == item.priority
        && value == item.value)
    }

    override fun describeTo(description: Description) {
      description.appendText(
        String.format(
          "Url (location=%s, priority=%d, value=%s", location, priority, value
        )
      )
    }
  }
}
