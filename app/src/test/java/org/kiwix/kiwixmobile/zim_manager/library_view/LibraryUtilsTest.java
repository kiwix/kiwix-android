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

package org.kiwix.kiwixmobile.zim_manager.library_view;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class LibraryUtilsTest {

  /*
   * Test rounding off to 3 significant figures
   */
  @Test
  public void testRoundingOff(){
    assertEquals("Test round down (standard case)","3.46",LibraryUtils.round3SF(3.462999));
    assertEquals("Test round up (standard case)","3.47",LibraryUtils.round3SF(3.46701));
    assertEquals("Test no decimal (and number having less than 3 SF)","7.0",LibraryUtils.round3SF(7));
    assertEquals("Test single decimal (and number having less than 3 SF)","80.4",LibraryUtils.round3SF(80.4));
    assertEquals("Test no decimal (and number having more than 3 SF)","7410.0",LibraryUtils.round3SF(7405));
    assertEquals("Test single decimal (and number having more than 3 SF)","8060.0",LibraryUtils.round3SF(8055.4));
    assertEquals("Test round down edge case","3.46",LibraryUtils.round3SF(3.464999));
    assertEquals("Test round up edge case","3.47",LibraryUtils.round3SF(3.465001));
    assertEquals("Test round off middle case","3.46",LibraryUtils.round3SF(3.46500));
  }
}
