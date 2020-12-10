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

package org.kiwix.kiwixmobile.webserver

import android.util.Log
import org.kiwix.kiwixlib.JNIKiwixException
import org.kiwix.kiwixlib.JNIKiwixServer
import org.kiwix.kiwixlib.Library
import org.kiwix.kiwixmobile.nav.destination.library.LibraryFactory
import javax.inject.Inject

private const val TAG = "KiwixServer"

class KiwixServer @Inject constructor(kiwixLibraryFactory: LibraryFactory) {
  private val libFactory = kiwixLibraryFactory
  private lateinit var kiwixLibrary: Library

  fun createKiwixServer(selectedBooksPath: ArrayList<String>): JNIKiwixServer? {
    kiwixLibrary = libFactory.createKiwixLibrary()
    // kiwixLibrary.addBook(selectedBooksPath)
    for (path in selectedBooksPath) {
      try {
        var isBookAdded: Boolean = kiwixLibrary.addBook(path)
      } catch (e: JNIKiwixException) {
        Log.v(TAG, "Couldn't add book $path")
      }
    }
    Log.d(TAG, "createKiwixServer: Library instance check in JNIKiwixServer: $kiwixLibrary ")
    return JNIKiwixServer(kiwixLibrary)
  }
}
