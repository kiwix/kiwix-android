/*
 * Kiwix Android
 * Copyright (c) 2024 Kiwix <android.kiwix.org>
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

import android.content.Context
import android.os.Looper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.extensions.ActivityExtensions.isCustomApp
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.libkiwix.Book
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

  class Factory @Inject constructor(
    private val context: Context,
    private val zimReaderContainer: ZimReaderContainer
  ) {
    @Suppress("NestedBlockDepth", "MagicNumber")
    suspend fun createKiwixServer(selectedBooksPath: ArrayList<String>): KiwixServer =
      withContext(Dispatchers.IO) {
        val kiwixLibrary = Library()
        selectedBooksPath.forEach { path ->
          try {
            val book = Book().apply {
              Log.e(
                TAG,
                " FD is valid = ${zimReaderContainer.zimReaderSource?.exists()} \n" +
                  "FD can read via the dev/fd/fdNumber = " +
                  "${zimReaderContainer.zimReaderSource?.canOpenInLibkiwix()}"
              )
              // Determine whether to create an Archive from an asset or a file path
              val archive =
                if (path == "null" && (context as CoreApp).getMainActivity().isCustomApp()) {
                  // For custom apps using create an Archive with FileDescriptor
                  zimReaderContainer.zimReaderSource?.createArchive()
                } else {
                  // For regular files, create an Archive from the file path
                  Archive(path)
                }
              update(archive)
            }
            kiwixLibrary.addBook(book)
          } catch (ignore: Exception) {
            // Catch the other exceptions as well. e.g. while hosting the split zim files.
            // we have an issue with split zim files, see #3827
            Log.v(
              TAG,
              "Couldn't add book with path:{ $path }.\n Original Exception = $ignore"
            )
          }
        }
        android.os.Handler(Looper.getMainLooper()).postDelayed({
          CoroutineScope(Dispatchers.IO).launch {
            Log.e(
              TAG,
              "After 5 second FD is valid = " +
                "${zimReaderContainer.zimReaderSource?.exists()} \n" +
                "FD can read via the dev/fd/fdNumber =" +
                " ${zimReaderContainer.zimReaderSource?.canOpenInLibkiwix()}"
            )
          }
        }, 5000)
        return@withContext KiwixServer(kiwixLibrary, Server(kiwixLibrary))
      }
  }

  fun startServer(port: Int): Boolean {
    jniKiwixServer.setPort(port)
    return jniKiwixServer.start()
  }

  fun stopServer() = jniKiwixServer.stop()
}
