package org.kiwix.kiwixmobile.data.local.dao;/*
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

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.yahoo.squidb.data.AbstractModel;
import com.yahoo.squidb.data.SquidCursor;
import com.yahoo.squidb.sql.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kiwix.kiwixmobile.data.ZimContentProvider;
import org.kiwix.kiwixmobile.data.local.KiwixDatabase;
import org.kiwix.kiwixmobile.data.local.entity.RecentSearch;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(AndroidJUnit4.class)
public class RecentSearchDaoTest {

  @Mock private KiwixDatabase kiwixDatabase;
  private RecentSearchDao recentSearchDao;
  private boolean mockInitialized = false;
  @Mock private SquidCursor<AbstractModel> mockedCursor;

  @Before
  public void executeBefore() {
    if (!mockInitialized) {
      MockitoAnnotations.initMocks(this);
      mockInitialized = true;
    }
    when(kiwixDatabase.query(any(), any())).thenReturn(mockedCursor);
    recentSearchDao = new RecentSearchDao(kiwixDatabase);
  }

  // verify the correct database query was called
  @Test
  public void testGetRecentSearches() {
    recentSearchDao.getRecentSearches();

    // verify ordering is in descending order of search ID and the results are limited to 5 only
    verify(kiwixDatabase).query(any(),
        eq(Query.selectDistinct(RecentSearch.SEARCH_STRING).where(RecentSearch.ZIM_I_D.eq(
            ZimContentProvider.getId()))
            .orderBy(RecentSearch.ID.desc())
            .limit(5)));
  }
}
