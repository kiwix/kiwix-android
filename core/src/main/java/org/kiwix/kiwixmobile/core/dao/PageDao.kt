/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.dao

import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import org.kiwix.kiwixmobile.core.page.adapter.Page

interface PageDao : BasePageDao {
  override fun pages(): Flowable<List<Page>>
}

interface PageRoomDao : BasePageDao {
  override fun pages(): Flow<List<Page>>
}

interface BasePageDao {
  fun pages(): Any
  fun deletePages(pagesToDelete: List<Page>)
}
