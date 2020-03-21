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
package org.kiwix.kiwixmobile.core.utils

object Constants {
  const val TAG_KIWIX = "kiwix"
  const val CONTACT_EMAIL_ADDRESS = "android@kiwix.org";

  // Request stuff
  const val REQUEST_STORAGE_PERMISSION = 1
  const val REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE = 3
  const val REQUEST_HISTORY_ITEM_CHOSEN = 99
  const val REQUEST_FILE_SELECT = 1234
  const val REQUEST_PREFERENCES = 1235
  const val BOOKMARK_CHOSEN_REQUEST = 1

  // Result stuff
  const val RESULT_RESTART = 1236
  const val RESULT_HISTORY_CLEARED = 1239

  // Tags
  const val TAG_FILE_SEARCHED = "searchedarticle"
  const val TAG_CURRENT_FILE = "currentzimfile"
  const val TAG_CURRENT_ARTICLES = "currentarticles"
  const val TAG_CURRENT_POSITIONS = "currentpositions"
  const val TAG_CURRENT_TAB = "currenttab"

  // Extras
  const val EXTRA_ZIM_FILE = "zimFile"
  const val EXTRA_CHOSE_X_URL = "choseXURL"
  const val EXTRA_CHOSE_X_TITLE = "choseXTitle"
  const val EXTRA_CHOSE_X_FILE = "choseXFile"
  const val EXTRA_EXTERNAL_LINK = "external_link"
  const val EXTRA_SEARCH = "search"
  const val EXTRA_IS_WIDGET_VOICE = "isWidgetVoice"
  const val HOTSPOT_SERVICE_CHANNEL_ID = "hotspotService"
  const val EXTRA_WEBVIEWS_LIST = "webviewsList"
  const val OLD_PROVIDER_DOMAIN = "org.kiwix.zim.base"
}
