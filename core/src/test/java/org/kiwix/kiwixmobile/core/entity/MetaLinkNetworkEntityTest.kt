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

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.simpleframework.xml.core.Persister

@Suppress("MaxLineLength")
class MetaLinkNetworkEntityTest {
  data class DummyUrl(val location: String, val priority: Int, val value: String)

  // ======== Helper ========
  private fun makeUrl(value: String, location: String = "us", priority: Int = 1) =
    MetaLinkNetworkEntity.Url().apply {
      this.value = value
      this.location = location
      this.priority = priority
    }

  private fun entityWithUrls(urlsList: List<MetaLinkNetworkEntity.Url>) =
    MetaLinkNetworkEntity().apply {
      file = MetaLinkNetworkEntity.FileElement().apply {
        urls = urlsList
      }
    }

  @Nested
  inner class ParseXmlFiles {
    private lateinit var result: MetaLinkNetworkEntity

    @BeforeEach
    fun setup() {
      val serializer = Persister()
      val stream =
        MetaLinkNetworkEntityTest::class.java.classLoader!!.getResourceAsStream("wikipedia_af_all_nopic_2016-05.zim.meta4")
      result = serializer.read(MetaLinkNetworkEntity::class.java, stream)
    }

    @Test
    fun urls_whenParsedCorrectly_returnsExpectedList() = runTest {
      val actualUrls = result.urls?.map {
        DummyUrl(it.location.orEmpty(), it.priority, it.value.orEmpty())
      }

      val expectedUrls = listOf(
        DummyUrl(
          "us",
          1,
          "http://ftpmirror.your.org/pub/kiwix/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
        ),
        DummyUrl(
          "gb",
          2,
          "http://www.mirrorservice.org/sites/download.kiwix.org/zim/wikipedia/wikipedia_af_all_nopic_2016-05.zim"
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

      // List should have all the parameters regardless of the order
      assertThat(actualUrls).containsExactlyInAnyOrderElementsOf(expectedUrls)
    }

    @Test
    fun file_whenParsedCorrectly_returnsExpectedProperties() = runTest {
      val file = result.file
      assertThat(file?.name).isEqualTo("wikipedia_af_all_nopic_2016-05.zim")
      assertThat(file?.size).isEqualTo(63973123L)
    }

    @Test
    fun getHash_whenParsedCorrectly_returnsExpectedHash() = runTest {
      val file = result.file
      assertThat(file?.getHash("md5")).isEqualTo("6f06866b61c4a921b57f28cfd4307220")
      assertThat(file?.getHash("sha-1")).isEqualTo("8aac4c7f89e3cdd45b245695e19ecde5aac59593")
      assertThat(file?.getHash("sha-256")).isEqualTo("83126775538cf588a85edb10db04d6e012321a2025278a08a084b258849b3a5c")
    }

    @Test
    fun piece_whenParsedCorrectly_returnsExpectedPieces() = runTest {
      val file = result.file
      assertThat(file?.pieces?.hashType).isEqualTo("sha-1")
      assertThat(file?.pieces?.length).isEqualTo(1048576)
      assertThat(file?.pieceHashes?.size).isEqualTo(62)
      assertThat(file?.pieceHashes?.get(0)).isEqualTo("f36815d904d4fd563aaef4ee6ef2600fb1fd70b2")
      assertThat(file?.pieceHashes?.get(61)).isEqualTo("8055e515aa6e78f2810bbb0e0cd07330838b8920")
    }
  }

  @Nested
  inner class RelevantUrlProperty {
    @Test
    fun relevantUrl_whenFileIsNull_returnsEmptyUrl() {
      val result = MetaLinkNetworkEntity()

      assertThat(result.relevantUrl.location).isNull()
      assertThat(result.relevantUrl.priority).isEqualTo(0)
      assertThat(result.relevantUrl.value).isNull()
    }

    @Test
    fun relevantUrl_whenUrlsIsEmpty_throwsIndexOutOfBoundsException() = runTest {
      val entity = entityWithUrls(emptyList())

      assertThatThrownBy { entity.relevantUrl }.isInstanceOf(IndexOutOfBoundsException::class.java)
    }

    @Test
    fun relevantUrl_whenUrlsHasEntries_returnsFirstUrl() = runTest {
      val entity = entityWithUrls(
        listOf(
          makeUrl("http://kiwix1.example.org/file.zim", priority = 1),
          makeUrl("http://kiwix2.example.org/file.zim", priority = 2)
        )
      )

      assertThat(entity.relevantUrl.value).isEqualTo("http://kiwix1.example.org/file.zim")
    }

    @Nested
    inner class FileAttributes {
      @Test
      fun urls_whenFileIsNull_returnsNull() = runTest {
        assertThat(MetaLinkNetworkEntity().urls).isNull()
      }

      @Test
      fun urls_whenFileHasUrls_delegatesToFileUrls() = runTest {
        val entity = entityWithUrls(listOf(makeUrl("http://kiwix.org/file.zim")))
        assertThat(entity.urls).hasSize(1)
        assertThat(entity.urls?.first()?.value).isEqualTo("http://kiwix.org/file.zim")
      }

      @Test
      fun getHash_whenTypeDoesNotExist_returnsNull() = runTest {
        val file =
          MetaLinkNetworkEntity.FileElement().apply { hashes = mapOf("sha-256" to "kiwix2208") }
        assertThat(file.getHash("md5")).isNull()
      }

      @Test
      fun getHash_whenHashesMapIsNull_returnsNull() = runTest {
        val file = MetaLinkNetworkEntity.FileElement().apply { hashes = null }
        assertThat(file.getHash("sha-256")).isNull()
      }

      @Test
      fun pieceHashes_whenPiecesIsNull_returnsNull() = runTest {
        val file = MetaLinkNetworkEntity.FileElement().apply { pieces = null }
        assertThat(file.pieceHashes).isNull()
      }

      @Test
      fun pieceHashes_whenPiecesHasHashes_delegatesToPieces() = runTest {
        val pieceHashesList = listOf("aabbcc", "ddeeff")
        val file = MetaLinkNetworkEntity.FileElement().apply {
          pieces = MetaLinkNetworkEntity.Pieces().apply {
            hashType = "sha-256"
            length = 220805
            pieceHashes = pieceHashesList
          }
        }
        assertThat(file.pieceHashes).containsExactly(pieceHashesList[0], pieceHashesList[1])
      }
    }
  }
}
