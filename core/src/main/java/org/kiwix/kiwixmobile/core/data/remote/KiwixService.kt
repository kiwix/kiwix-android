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
@file:Suppress("DEPRECATION")

package org.kiwix.kiwixmobile.core.data.remote

import okhttp3.OkHttpClient
import org.kiwix.kiwixmobile.core.data.remote.update.UpdateFeed
import org.kiwix.kiwixmobile.core.entity.MetaLinkNetworkEntity
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url

interface KiwixService {
  @GET
  suspend fun getLibraryPage(
    @Url url: String
  ): Response<String>

  @GET
  suspend fun getMetaLinks(
    @Url url: String
  ): MetaLinkNetworkEntity?

  @GET("catalog/v2/languages")
  suspend fun getLanguages(): LanguageFeed

  @GET("kiwix/release/kiwix-android/feed.xml")
  suspend fun getUpdates(): UpdateFeed

  /******** Helper class that sets up new services  */
  object ServiceCreator {
    @Suppress("DEPRECATION")
    fun newHackListService(okHttpClient: OkHttpClient, baseUrl: String): KiwixService {
      val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addConverterFactory(SimpleXmlConverterFactory.create())
        .build()
      return retrofit.create(KiwixService::class.java)
    }
  }

  companion object {
    const val OPDS_LIBRARY_ENDPOINT = "v2/entries"
    const val ITEMS_PER_PAGE = 25
  }
}
