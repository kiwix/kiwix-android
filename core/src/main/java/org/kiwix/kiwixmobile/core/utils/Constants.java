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
package org.kiwix.kiwixmobile.core.utils;

public final class Constants {

  public static final String TAG_KIWIX = "kiwix";

  public static final String CONTACT_EMAIL_ADDRESS = "android@kiwix.org";

  // Request stuff
  public static final int REQUEST_FILE_SEARCH = 1236;

  public static final int REQUEST_STORAGE_PERMISSION = 1;

  public static final int REQUEST_READ_STORAGE_PERMISSION = 2;

  public static final int REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE = 3;

  public static final int REQUEST_HISTORY_ITEM_CHOSEN = 99;

  public static final int REQUEST_FILE_SELECT = 1234;

  public static final int REQUEST_PREFERENCES = 1235;

  public static final int BOOKMARK_CHOSEN_REQUEST = 1;

  // Result stuff
  public static final int RESULT_RESTART = 1236;

  public static final int RESULT_HISTORY_CLEARED = 1239;

  // Tags
  public static final String TAG_FILE_SEARCHED = "searchedarticle";

  public static final String TAG_CURRENT_FILE = "currentzimfile";

  public static final String TAG_CURRENT_ARTICLES = "currentarticles";

  public static final String TAG_CURRENT_POSITIONS = "currentpositions";

  public static final String TAG_CURRENT_TAB = "currenttab";

  // Extras
  public static final String EXTRA_ZIM_FILE_2 = "zim_file";

  public static final String EXTRA_ZIM_FILE = "zimFile";

  public static final String EXTRA_CHOSE_X_URL = "choseXURL";

  public static final String EXTRA_CHOSE_X_TITLE = "choseXTitle";

  public static final String EXTRA_EXTERNAL_LINK = "external_link";

  public static final String EXTRA_LIBRARY = "library";

  public static final String EXTRA_SEARCH = "search";

  public static final String EXTRA_BOOK = "Book";

  public static final String EXTRA_IS_WIDGET_VOICE = "isWidgetVoice";

  public static final String EXTRA_IS_WIDGET_SEARCH = "isWidgetSearch";

  public static final String EXTRA_IS_WIDGET_STAR = "isWidgetStar";

  public static final String EXTRA_NOTIFICATION_ID = "notificationID";

  public static final String HOTSPOT_SERVICE_CHANNEL_ID = "hotspotService";

  public static final String EXTRA_WEBVIEWS_LIST = "webviewsList";

  public static final String EXTRA_SEARCH_TEXT = "searchText";

  // Notification Channel Constants
  public static final String ONGOING_DOWNLOAD_CHANNEL_ID = "ongoing_downloads_channel_id";

  public static final String OLD_PROVIDER_DOMAIN = "org.kiwix.zim.base";
}
