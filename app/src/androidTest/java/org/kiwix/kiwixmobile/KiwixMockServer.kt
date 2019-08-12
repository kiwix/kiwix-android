package org.kiwix.kiwixmobile

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.kiwix.kiwixmobile.di.modules.TestNetworkModule
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity.FileElement
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity.Pieces
import org.kiwix.kiwixmobile.library.entity.MetaLinkNetworkEntity.Url
import org.simpleframework.xml.core.Persister
import java.io.StringWriter
import java.util.LinkedList
import java.util.Stack

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
class KiwixMockServer {

  val queuedResponses: Stack<MockResponse> = Stack()

  private val mockWebServer = MockWebServer().apply {
    start(PORT)
  }

  fun stop() {
    mockWebServer.shutdown()
  }

  fun map(
    vararg pathsToResponses: Pair<String, Any>
  ) {
    val mapOfPathsToResponses = mapOf(*pathsToResponses)
    mockWebServer.setDispatcher(object : Dispatcher() {
      override fun dispatch(request: RecordedRequest) =
        mapOfPathsToResponses[request.path]?.let(::successfulResponse)
          ?: queuedResponses.pop()?.let { return@let it }
          ?: throw RuntimeException("No response mapped for ${request.path}")
    })
  }

  private fun successfulResponse(bodyObject: Any) = MockResponse().apply {
    setResponseCode(200)
    setBody(bodyObject.asXmlString())
  }

  fun queueResponse(mockResponse: MockResponse) {
  }

  companion object {
    const val PORT = 8080
  }
}

fun Any.asXmlString() = StringWriter().let {
  Persister().write(this, it)
  "$it"
}

fun metaLinkNetworkEntity() = MetaLinkNetworkEntity().apply {
  file = fileElement()
}

fun fileElement(
  urls: List<Url> = listOf(
    url()
  ),
  name: String = "name",
  hashes: Map<String, String> = mapOf("hash" to "hash"),
  pieces: Pieces = pieces()
) = FileElement().apply {
  this.name = name
  this.urls = urls
  this.hashes = hashes
  this.pieces = pieces
}

fun pieces(
  hashType: String = "hashType",
  pieceHashes: List<String> = listOf("hash")
) = Pieces().apply {
  this.hashType = hashType
  this.pieceHashes = pieceHashes
}

fun url(
  value: String = "${TestNetworkModule.MOCK_BASE_URL}/relevantUrl.zim.meta4",
  location: String = "location"
) = Url().apply {
  this.location = location
  this.value = value
}

fun book(
  id: String = "id",
  title: String = "title",
  description: String = "description",
  language: String = "eng",
  creator: String = "creator",
  publisher: String = "publisher",
  date: String = "date",
  url: String = "${TestNetworkModule.MOCK_BASE_URL}/url",
  articleCount: String = "mediaCount",
  mediaCount: String = "mediaCount",
  size: String = "1024",
  name: String = "name",
  favIcon: String = "favIcon"
) =
  Book().apply {
    this.id = id
    this.title = title
    this.description = description
    this.language = language
    this.creator = creator
    this.publisher = publisher
    this.date = date
    this.url = url
    this.articleCount = articleCount
    this.mediaCount = mediaCount
    this.size = size
    bookName = name
    favicon = favIcon
  }

fun libraryNetworkEntity(books: List<Book> = emptyList()) = LibraryNetworkEntity().apply {
  book = LinkedList(books)
}
