/*
 * Kiwix Android
 * Copyright (c) 2023 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.core.webserver

import android.util.Log
import org.kiwix.libkiwix.Book
import org.kiwix.libkiwix.JNIKiwixException
import org.kiwix.libkiwix.Library
import org.kiwix.libkiwix.Server
import org.kiwix.libzim.Archive
import javax.inject.Inject

private const val TAG = "KiwixServer"

// jniKiwixServer is a server running on a existing library.
// We must keep the library alive (keep a reference to it) to not delete the library the server
// is working on. See https://github.com/kiwix/java-libkiwix/issues/51
class KiwixServer @Inject constructor(
  private val library: Library,
  private val jniKiwixServer: Server
) {

  class Factory @Inject constructor() {
    fun createKiwixServer(selectedBooksPath: ArrayList<String>): KiwixServer {
      val kiwixLibrary = Library()
      selectedBooksPath.forEach { path ->
        try {
          val book = Book().apply {
            update(Archive(path))
          }
          kiwixLibrary.addBook(book)
        } catch (e: JNIKiwixException) {
          Log.v(TAG, "Couldn't add book with path:{ $path }")
        }
      }
      return KiwixServer(kiwixLibrary, Server(kiwixLibrary))
    }
  }

  fun startServer(port: Int): Boolean {
    jniKiwixServer.setPort(port)
    return jniKiwixServer.start()
  }

  fun stopServer() = jniKiwixServer.stop()
}
