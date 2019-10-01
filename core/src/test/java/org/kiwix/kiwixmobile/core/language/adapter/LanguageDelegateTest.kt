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

package org.kiwix.kiwixmobile.core.language.adapter

import android.view.ViewGroup
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions.inflate
import org.kiwix.kiwixmobile.core.language.adapter.LanguageDelegate.HeaderDelegate
import org.kiwix.kiwixmobile.core.language.adapter.LanguageDelegate.LanguageItemDelegate
import org.kiwix.kiwixmobile.core.language.adapter.LanguageListItem.HeaderItem
import org.kiwix.kiwixmobile.core.language.adapter.LanguageListItem.LanguageItem
import org.kiwix.kiwixmobile.core.language.adapter.LanguageListViewHolder.HeaderViewHolder
import org.kiwix.kiwixmobile.core.language.adapter.LanguageListViewHolder.LanguageViewHolder

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

class LanguageDelegateTest {
  @Nested
  inner class HeaderDelegateTests {
    @Test
    fun `class is header item`() {
      assertThat(HeaderDelegate().itemClass).isEqualTo(HeaderItem::class.java)
    }

    @Test
    fun `creates HeaderViewHolder`() {
      val parent = mockk<ViewGroup>()
      mockkObject(ViewGroupExtensions)
      every { parent.inflate(R.layout.header_date, false) } returns mockk(relaxed = true)
      assertThat(HeaderDelegate().createViewHolder(parent))
        .isInstanceOf(HeaderViewHolder::class.java)
    }
  }

  @Nested
  inner class LanguageItemDelegateTests {
    @Test
    fun `class is lanuguage item`() {
      assertThat(LanguageItemDelegate {}.itemClass).isEqualTo(LanguageItem::class.java)
    }

    @Test
    fun `creates LanguageViewHolder`() {
      val parent = mockk<ViewGroup>()
      mockkObject(ViewGroupExtensions)
      every { parent.inflate(R.layout.item_language, false) } returns mockk(relaxed = true)
      val clickAction = mockk<(LanguageItem) -> Unit>()
      assertThat(LanguageItemDelegate(clickAction).createViewHolder(parent))
        .isInstanceOf(LanguageViewHolder::class.java)
    }
  }
}
