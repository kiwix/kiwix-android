/*
 * Kiwix Android
 * Copyright (c) 2022 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.settings

object Constants {
  const val IS_CUSTOM_APP = true
  const val CUSTOM_APP_ID = "~package~"
  const val CUSTOM_APP_HAS_EMBEDDED_ZIM = embed_zim.inv().toBoolean()
  const val CUSTOM_APP_ZIM_FILE_NAME = "~zim_name~"
  const val CUSTOM_APP_ZIM_FILE_SIZE = zim_size.inv().toLong()
  const val CUSTOM_APP_VERSION_NAME = "~version_name~"
  const val CUSTOM_APP_VERSION_CODE: Int = version_code.inv()
  const val CUSTOM_APP_CONTENT_VERSION_CODE: Int = content_version_code.inv()
  const val CUSTOM_APP_WEBSITE = "~website~"
  const val CUSTOM_APP_EMAIL = "~support_email~"
  const val CUSTOM_APP_SUPPORT_EMAIL = "~support_email~"
  const val CUSTOM_APP_ENFORCED_LANG = "~enforced_lang~"
}
