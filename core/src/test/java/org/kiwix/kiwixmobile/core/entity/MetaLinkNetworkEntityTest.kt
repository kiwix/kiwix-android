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

import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.simpleframework.xml.core.Persister

class MetaLinkNetworkEntityTest {
  @Test
  @Throws(Exception::class)
  fun testDeserialize() {
    val serializer = Persister()
    val result = serializer.read(
      MetaLinkNetworkEntity::class.java,
      MetaLinkNetworkEntityTest::class.java.classLoader!!.getResourceAsStream(
        "wikipedia_af_all_nopic_2016-05.zim.meta4"
      )
    )

    MetaLinkNetworkEntityUrlAssert(result.urls).hasItems(
      listOf(
        DummyUrl(
          "us",
          1,
          "http://ftpmirror.your.org/pub/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
        ),
        DummyUrl(
          "gb",
          2,
          "http://www.mirrorservice.org/sites/download.kiwix.org/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim" // ktlint-disable
        ),
        DummyUrl(
          "us",
          3,
          "http://download.wikimedia.org/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
        ),
        DummyUrl(
          "de",
          4,
          "http://mirror.netcologne.de/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
        ),
        DummyUrl(
          "fr",
          5,
          "http://mirror3.kiwix.org/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
        )
      )
    )

    // Basic file attributes
    assertThat(result.getFile().getName()).isEqualTo("wikipedia_af_all_nopic_2016-05.zim")

    assertThat(result.getFile().size).isEqualTo(63973123L)

    // File hashes
    assertThat(result.getFile().getHash("md5")).isEqualTo("6f06866b61c4a921b57f28cfd4307220")
    assertThat(
      result.getFile().getHash("sha-1")
    ).isEqualTo("8aac4c7f89e3cdd45b245695e19ecde5aac59593")
    assertThat(
      result.getFile().getHash("sha-256")
    ).isEqualTo("83126775538cf588a85edb10db04d6e012321a2025278a08a084b258849b3a5c")

    // Pieces
    assertThat(result.getFile().pieceHashType).isEqualTo("sha-1")
    assertThat(result.getFile().pieceLength).isEqualTo(1048576)

    // Check only the first and the last elements of the piece hashes
    assertThat(result.getFile().pieceHashes.size).isEqualTo(62)
    assertThat(
      result.getFile().pieceHashes[0]
    )
      .isEqualTo("f36815d904d4fd563aaef4ee6ef2600fb1fd70b2")
    assertThat(
      result.getFile().pieceHashes[61]
    )
      .isEqualTo("8055e515aa6e78f2810bbb0e0cd07330838b8920")
  }

  data class DummyUrl(val location: String, val priority: Int, val value: String)

  /**
   * Implemented as a matcher only to avoid putting extra code into {@code MetaLinkNetworkEntity}.
   * However in case {@code equals} and {@code hashCode} methods are added to
   * {@code MetaLinkNetworkEntity.Url} class itself, this Matcher should be deleted.
   */
  class MetaLinkNetworkEntityUrlAssert(
    actual: List<MetaLinkNetworkEntity.Url>
  ) :
    AbstractAssert<MetaLinkNetworkEntityUrlAssert, List<MetaLinkNetworkEntity.Url>>(
      actual,
      MetaLinkNetworkEntityUrlAssert::class.java
    ) {
    private fun <S, T> intersectionWith(
      first: List<S>,
      second: List<T>,
      function: (S, T) -> Boolean
    ): Boolean {
      val filtered = first.filter { a -> second.any { b -> function(a, b) } }
      if (filtered.isNotEmpty())
        return true
      return false
    }

    fun hasItems(items: List<DummyUrl>): Boolean {
      return intersectionWith(actual, items) { a, b ->
        a.location == b.location && a.priority == b.priority && a.value == b.value
      }
    }
  }

  companion object {
    fun assertThat(actual: List<MetaLinkNetworkEntity.Url>): MetaLinkNetworkEntityUrlAssert =
      MetaLinkNetworkEntityUrlAssert(actual)
  }
}
