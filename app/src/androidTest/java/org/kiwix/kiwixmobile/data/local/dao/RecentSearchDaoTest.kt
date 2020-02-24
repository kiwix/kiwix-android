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
package org.kiwix.kiwixmobile.data.local.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yahoo.squidb.data.AbstractModel
import com.yahoo.squidb.data.SquidCursor
import com.yahoo.squidb.sql.Query
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.kiwix.kiwixmobile.core.data.local.KiwixDatabase
import org.kiwix.kiwixmobile.core.data.local.dao.RecentSearchDao

@RunWith(AndroidJUnit4::class)
class RecentSearchDaoTest {
  @MockK
  private lateinit var kiwixDatabase: KiwixDatabase
  private lateinit var recentSearchDao: RecentSearchDao
  @MockK(relaxed = true)
  private lateinit var mockedCursor: SquidCursor<AbstractModel>

  @Before fun executeBefore() {
    MockKAnnotations.init(this)
    every { kiwixDatabase.query(any<Class<AbstractModel>>(), any()) } returns mockedCursor
    recentSearchDao = RecentSearchDao(kiwixDatabase)
  }

  @Test fun testGetRecentSearches() {
    recentSearchDao.recentSearches
    verify { kiwixDatabase.query(any<Class<AbstractModel>>(), Query.select()) }
  }
}
