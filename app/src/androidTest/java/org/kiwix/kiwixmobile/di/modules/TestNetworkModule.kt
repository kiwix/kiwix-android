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
package org.kiwix.kiwixmobile.di.modules

import dagger.Module
import okhttp3.OkHttpClient
import org.kiwix.kiwixmobile.MOCK_BASE_URL
import org.kiwix.kiwixmobile.data.remote.KiwixService

/**
 * Created by mhutti1 on 14/04/17.
 */

@Module
class TestNetworkModule : NetworkModule() {

  internal override fun provideKiwixService(okHttpClient: OkHttpClient): KiwixService =
    KiwixService.ServiceCreator.newHacklistService(
      okHttpClient,
      MOCK_BASE_URL
    )

}
