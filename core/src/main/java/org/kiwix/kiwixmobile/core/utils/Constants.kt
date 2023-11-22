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

const val TAG_KIWIX = "kiwix"
const val CONTACT_EMAIL_ADDRESS = "android@kiwix.org"

// Request stuff
const val REQUEST_STORAGE_PERMISSION = 1
const val REQUEST_WRITE_STORAGE_PERMISSION_ADD_NOTE = 3
const val REQUEST_SELECT_FOLDER_PERMISSION = 4
const val REQUEST_POST_NOTIFICATION_PERMISSION = 4

// Tags
const val TAG_FILE_SEARCHED = "searchedarticle"
const val TAG_FILE_SEARCHED_NEW_TAB = "searchedarticlenewtab"
const val TAG_CURRENT_FILE = "currentzimfile"
const val TAG_CURRENT_ARTICLES = "currentarticles"
const val TAG_CURRENT_POSITIONS = "currentpositions"
const val TAG_CURRENT_TAB = "currenttab"
const val TAG_FROM_TAB_SWITCHER = "fromtabswitcher"

// Extras
const val EXTRA_IS_WIDGET_VOICE = "isWidgetVoice"
const val HOTSPOT_SERVICE_CHANNEL_ID = "hotspotService"
const val OLD_PROVIDER_DOMAIN = "org.kiwix.zim.base"
const val READ_ALOUD_SERVICE_CHANNEL_ID = "readAloudService"

// For Storage select dialog
const val INTERNAL_SELECT_POSITION = 0
const val EXTERNAL_SELECT_POSITION = 1

// For Read and Connect timeout on download OkHttpClient both are in minutes
const val READ_TIME_OUT = 1L
const val CONNECT_TIME_OUT = 1L

// For autoRetryMaxAttempts in download zim file
const val AUTO_RETRY_MAX_ATTEMPTS = 20

// Default port for http request
const val DEFAULT_PORT = 8080
