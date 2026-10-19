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
package org.kiwix.kiwixmobile.core.reader

import android.content.res.AssetFileDescriptor
import android.os.ParcelFileDescriptor
import android.util.Base64
import androidx.core.net.toUri
import eu.mhutti1.utils.storage.KB
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.kiwix.kiwixmobile.core.CoreApp
import org.kiwix.kiwixmobile.core.di.IoDispatcher
import org.kiwix.kiwixmobile.core.entity.LibkiwixBook
import org.kiwix.kiwixmobile.core.reader.ZimFileReader.Companion.CONTENT_PREFIX
import org.kiwix.kiwixmobile.core.utils.TAG_KIWIX
import org.kiwix.kiwixmobile.core.utils.files.FileUtils
import org.kiwix.kiwixmobile.core.utils.files.FileUtils.getSpellingDBDir
import org.kiwix.kiwixmobile.core.utils.files.Log
import org.kiwix.libkiwix.JNIKiwixException
import org.kiwix.libkiwix.SpellingsDB
import org.kiwix.libzim.Archive
import org.kiwix.libzim.DirectAccessInfo
import org.kiwix.libzim.Item
import org.kiwix.libzim.SuggestionSearch
import org.kiwix.libzim.SuggestionSearcher
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

private const val TAG = "ZimFileReader"

class ZimFileReader(
  val zimReaderSource: ZimReaderSource,
  val jniKiwixReader: Archive,
  private val searcher: SuggestionSearcher,
  private val ioDispatcher: CoroutineDispatcher
) {
  interface Factory {
    suspend fun create(
      zimReaderSource: ZimReaderSource,
      showSearchSuggestionsSpellChecked: Boolean
    ): ZimFileReader?

    class Impl @Inject constructor(
      @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
    ) : Factory {
      override suspend fun create(
        zimReaderSource: ZimReaderSource,
        showSearchSuggestionsSpellChecked: Boolean
      ): ZimFileReader? =
        try {
          zimReaderSource.createArchive(ioDispatcher)?.let { archive ->
            ZimFileReader(
              zimReaderSource,
              jniKiwixReader = archive,
              searcher = SuggestionSearcher(archive),
              ioDispatcher = ioDispatcher
            ).also { zimFileReader ->
              Log.e(TAG, "create: ${zimReaderSource.toDatabase()}")
              if (showSearchSuggestionsSpellChecked) {
                // Prepare the SpellingsDB asynchronously(when it configure to create) so that creating the
                // ZIM reader doesn’t block the user experience.
                CoroutineScope(ioDispatcher).launch {
                  zimFileReader.prepareSpellingsDB(archive)
                }
              }
            }
          } ?: run {
            Log.e(
              TAG,
              "Error in creating ZimFileReader," +
                " because file does not exist on path: ${zimReaderSource.toDatabase()}"
            )
            null
          }
        } catch (ignore: JNIKiwixException) {
          Log.e(
            TAG,
            "Error in creating ZimFileReader," +
              " there is an JNI exception happens: $ignore\n" +
              "For ZIM file = ${zimReaderSource.toDatabase()}"
          )
          null
        } catch (ignore: Exception) {
          // for handing the error, if any zim file is corrupted
          Log.e(
            TAG,
            "Error in creating ZimFileReader: $ignore\n" +
              "For ZIM file = ${zimReaderSource.toDatabase()}"
          )
          null
        }
    }
  }

  private var spellingsDB: SpellingsDB? = null

  /**
   * Note that the value returned is NOT unique for each zim file. Versions of the same wiki
   * (complete, nopic, novid, etc) may return the same title.
   */
  val title: String
    get() = getSafeMetaData("Title", "No Title Found")
  val mainPage: String?
    get() =
      try {
        jniKiwixReader.mainEntry.getItem(true).path
      } catch (exception: Exception) {
        Log.e(TAG, "Unable to find the main page, original exception $exception")
        null
      }
  val id: String get() = jniKiwixReader.uuid

  /*
     libzim returns file size in kib so we need to convert it into bytes.
     More information here https://github.com/kiwix/java-libkiwix/issues/41
   */
  val fileSize: Long get() = jniKiwixReader.filesize / 1024
  val creator: String get() = getSafeMetaData("Creator", "")
  val publisher: String get() = getSafeMetaData("Publisher", "")
  val name: String get() = getSafeMetaData("Name", id)
  val date: String get() = getSafeMetaData("Date", "")
  val description: String get() = getSafeMetaData("Description", "")
  val favicon: String?
    get() = runCatching {
      Base64.encodeToString(
        jniKiwixReader.getIllustrationItem(ILLUSTRATION_SIZE).data.data,
        Base64.DEFAULT
      )
    }.onFailure {
      Log.e(TAG, "Could not get the favicon for $title. Original exception: $it")
    }.getOrNull()
  val zimMainLanguage: String get() = runCatching { jniKiwixReader.mainLanguage }.getOrElse { "" }

  val languages: List<String> get() = runCatching { jniKiwixReader.languages.toList() }.getOrElse { emptyList() }

  val tags: String
    get() = getSafeMetaData("Tags", "")
  val mediaCount: Int?
    get() =
      try {
        jniKiwixReader.mediaCount
      } catch (ignore: Exception) {
        // Catch all exceptions to prevent the rendering process of other zim files from aborting.
        // If the zim file is split with zim-tool,
        // refer to https://github.com/kiwix/kiwix-android/issues/3827. catch (ignore: Exception) {
        Log.e(TAG, "Unable to find the media count $ignore")
        null
      }
  val articleCount: Int?
    get() =
      try {
        jniKiwixReader.articleCount
      } catch (ignore: Exception) {
        // Catch all exceptions to prevent the rendering process of other zim files from aborting.
        // If the zim file is split with zim-tool,
        // refer to https://github.com/kiwix/kiwix-android/issues/3827. catch (ignore: Exception) {
        Log.e(TAG, "Unable to find the article count $ignore")
        null
      }

  fun searchSuggestions(prefix: String): SuggestionSearch? =
    try {
      searcher.suggest(prefix)
    } catch (exception: Exception) {
      // to handled the exception if there is no FT Xapian index found in the current zim file
      Log.e(TAG, "Unable to search in this file as it does not have FT Xapian index. $exception")
      null
    }

  /**
   * Initializes the `SpellingsDB` instance using the currently opened ZIM archive.
   *
   * This prepares the spell correction database used by libkiwix
   * for suggesting alternative or corrected search terms.
   */
  suspend fun prepareSpellingsDB(archive: Archive) {
    spellingsDBCreationMutex.withLock {
      if (spellingsDB != null) {
        Log.d(TAG_KIWIX, "SpellingsDB already initialized, skipping.")
        return@withLock
      }
      runCatching {
        Log.d(TAG_KIWIX, "Initializing SpellingsDB")
        val cachedDir = getSpellingDBDir(CoreApp.instance)?.absolutePath
        spellingsDB = SpellingsDB(archive, cachedDir)
        Log.d(TAG_KIWIX, "SpellingsDB successfully initialized.")
      }.onFailure {
        Log.e(
          TAG_KIWIX,
          "Failed to initialize SpellingsDB: ${it.message}",
          it
        )
      }
    }
  }

  /**
   * Retrieves a list of suggested or corrected spellings for a given search term.
   *
   * @param word The search term for which to retrieve spelling suggestions.
   * @param maxCount The maximum number of suggestions to return.
   * @return A list of suggested words ordered by relevance, or an empty list if
   *         suggestions are unavailable or the SpellingsDB is not initialized.
   */
  fun getSuggestedSpelledWords(word: String, maxCount: Int): List<String> =
    runCatching {
      spellingsDB?.getSpellingCorrections(word, maxCount)?.toList().orEmpty()
    }.onFailure {
      Log.e(
        TAG,
        "Error fetching suggested spellings: ${it.message}",
        it
      )
    }.getOrDefault(emptyList())

  /**
   * Returns the entry path for given title from the ZIM archive. E.g. A/MainPage.
   */
  fun getPageEntryPathFromTitle(title: String): String? =
    try {
      jniKiwixReader.getEntryByTitle(title).path
    } catch (exception: Exception) {
      Log.e(TAG, "Could not get path for title = $title \n original exception = $exception")
      null
    }

  /**
   * Returns the random entry path from the ZIM archive. E.g. A/Random_Article.
   */
  fun getRandomEntryPath(): String? =
    try {
      jniKiwixReader.randomEntry.path
    } catch (exception: Exception) {
      Log.e(TAG, "Could not get random entry \n original exception = $exception")
      null
    }

  @Suppress("UnreachableCode")
  suspend fun load(
    uri: String
  ): InputStream? =
    withContext(ioDispatcher) {
      val extension = uri.substringAfterLast(".")
      if (assetExtensions.any { it == extension }) {
        try {
          return@withContext loadAsset(uri)
        } catch (ioException: IOException) {
          Log.e(TAG, "failed to write video for $uri", ioException)
        }
      }
      return@withContext loadContent(uri, extension)
    }

  @Suppress("UnreachableCode", "NestedBlockDepth", "ReturnCount")
  private suspend fun loadContent(uri: String, extension: String): InputStream? {
    val item = getItem(uri)
    if (compressedExtensions.any { it != extension }) {
      item?.itemSize()?.let {
        // Check if the item size exceeds 1 MB
        if (it / KB > 1024) {
          // Retrieve direct access information for the item
          val infoPair = getDirectAccessInfoOfItem(item, uri)
          val file = infoPair?.filename?.let(::File)
          // If no file found or file does not exist, return input stream from item data
          if (infoPair == null || file == null || !file.exists()) {
            return@loadContent ByteArrayInputStream(item.data?.data)
          }
          // Return the input stream from the direct access information
          return@loadContent getInputStreamFromDirectAccessInfo(item, file, infoPair)
        }
      }
    }
    return generateZimContentBytes(item, uri)
  }

  fun getMimeTypeFromUrl(uri: String): String? =
    getItem(uri)?.mimetype
      ?.truncateMimeType.also {
        Log.d(TAG, "getting mimetype for $uri = $it")
      }

  fun getRedirect(url: String) = "${toRedirect(url)}"

  fun isRedirect(url: String): Boolean {
    val redirectUrl = getRedirect(url)
    return when {
      redirectUrl.isEmpty() -> false
      else -> url.startsWith(CONTENT_PREFIX) && url != redirectUrl
    }
  }

  private fun toRedirect(url: String) = getActualUrl(url).toUri()

  private fun getActualUrl(url: String): String {
    val actualPath = url.entryPath
    val redirectPath =
      try {
        jniKiwixReader.getEntryByPath(actualPath)
          .getItem(true)
          .path
      } catch (_: Exception) {
        actualPath
      }
    // Not a redirect: keep the URL as it is, so its own encoding is not changed.
    if (redirectPath == actualPath) return url.substringBefore("#")
    var redirectUrl = redirectPath.addContentPrefix
    if (url.contains("?")) {
      redirectUrl += extractQueryParam(url)
    }
    return redirectUrl
  }

  // For fixing the issue with new created zim files,
  // see https://github.com/kiwix/kiwix-android/issues/3098#issuecomment-1642083152 to know more about the this.
  private fun extractQueryParam(url: String): String =
    "?" + url.substringAfterLast("?", "")

  private suspend fun loadAsset(
    uri: String
  ): InputStream? =
    withContext(ioDispatcher) {
      val item =
        try {
          jniKiwixReader.getEntryByPath(uri.entryPath).getItem(true)
        } catch (exception: Exception) {
          Log.e(TAG, "Could not get Item for uri = $uri \n original exception = $exception")
          null
        }
      val infoPair = getDirectAccessInfoOfItem(item, uri)
      val file = infoPair?.filename?.let(::File)
      if (infoPair == null || file == null || !file.exists()) {
        return@withContext loadAssetFromCache(uri)
      }
      return@withContext getInputStreamFromDirectAccessInfo(item, file, infoPair)
    }

  private fun getDirectAccessInfoOfItem(item: Item?, uri: String): DirectAccessInfo? =
    try {
      item?.directAccessInformation
    } catch (ignore: Exception) {
      Log.e(
        TAG,
        "Could not get directAccessInformation for uri = $uri \n" +
          "original exception = $ignore"
      )
      null
    }

  private fun getInputStreamFromDirectAccessInfo(
    item: Item?,
    file: File,
    infoPair: DirectAccessInfo
  ): InputStream? =
    item?.itemSize()?.let {
      AssetFileDescriptor(
        parcelFileDescriptor(file),
        infoPair.offset,
        it
      ).createInputStream()
    }

  @Throws(IOException::class)
  private fun loadAssetFromCache(uri: String): FileInputStream = File(
    FileUtils.getFileCacheDir(CoreApp.instance),
    uri.substringAfterLast("/")
  ).apply { getContent(uri)?.let(::writeBytes) }
    .inputStream()

  private fun getContent(url: String) =
    try {
      getItem(url)?.data?.data
    } catch (ignore: Exception) {
      Log.e(TAG, "Could not get content for url = $url original exception = $ignore")
      null
    }

  private suspend fun generateZimContentBytes(item: Item?, uri: String): ByteArrayInputStream =
    withContext(ioDispatcher) {
      runCatching {
        val output = ByteArrayOutputStream()
        if (item != null) {
          output.write(item.data.data)
        }

        ByteArrayInputStream(output.toByteArray())
      }.onFailure { Log.e(TAG, "Error generating data for $uri", it) }
        .getOrDefault(ByteArrayInputStream(ByteArray(0)))
    }

  fun getItem(url: String): Item? =
    try {
      jniKiwixReader.getEntryByPath(url.entryPath)
        .getItem(true)
    } catch (_: Exception) {
      try {
        // An entry path can contain "?" or "#" when its URL was not encoded, e.g. a URL saved
        // by an older version of the app. See #3924 for more details.
        jniKiwixReader.getEntryByPath(url.substringAfter(CONTENT_PREFIX).decodeUrl)
          .getItem(true)
      } catch (exception: Exception) {
        Log.e(TAG, "Could not get Item for url = $url \n original exception = $exception")
        null
      }
    }

  @Suppress("ExplicitThis") // this@ZimFileReader.name is required
  fun toBook() =
    LibkiwixBook().apply {
      title = this@ZimFileReader.title
      id = this@ZimFileReader.id
      size = "$fileSize"
      favicon = this@ZimFileReader.favicon.toString()
      creator = this@ZimFileReader.creator
      publisher = this@ZimFileReader.publisher
      date = this@ZimFileReader.date
      description = this@ZimFileReader.description
      language = this@ZimFileReader.zimMainLanguage
      articleCount = this@ZimFileReader.articleCount.toString()
      mediaCount = this@ZimFileReader.mediaCount.toString()
      bookName = this@ZimFileReader.name
      tags = this@ZimFileReader.tags
    }

  fun dispose() {
    jniKiwixReader.dispose()
    searcher.dispose()
    spellingsDB?.dispose()
  }

  private fun getSafeMetaData(name: String, missingDelimiterValue: String) =
    runCatching {
      jniKiwixReader.getMetadata(name)
    }.getOrElse { missingDelimiterValue }

  companion object {
    private val spellingsDBCreationMutex = Mutex()

    /*
     * these uris aren't actually nullable but unit tests fail to compile as
     * Uri.parse returns null without android dependencies loaded
     */
    const val UI_URI_STRING = "content://org.kiwix.ui/"

    const val CONTENT_PREFIX = "https://kiwix.app/"
    private val assetExtensions =
      listOf("3gp", "mp4", "m4a", "webm", "mkv", "ogg", "ogv", "svg", "warc")
    private val compressedExtensions =
      listOf("zip", "7z", "gz", "rar", "sitx")
  }
}

// The ZIM entry path of a content URL: its path without the query and the fragment, decoded.
internal val String.entryPath: String
  get() = substringAfter(CONTENT_PREFIX).substringBefore("#").substringBefore("?").decodeUrl

private val percentEncodedBytes = Regex("(%[0-9A-Fa-f]{2})+")

// Percent-decodes a URL, e.g. "C%2B%2B" to "C++" and "Caf%C3%A9" to "Café". Unlike URLDecoder,
// "+" stays a "+" (it means a space only in HTML form data), and a "%" that does not start an
// escape is kept as it is (e.g. "100%"), so URLs saved with an unencoded entry path still work.
val String.decodeUrl: String
  get() = percentEncodedBytes.replace(this) { bytes ->
    bytes.value.chunked(3) { it.substring(1).toInt(16).toByte() }
      .toByteArray()
      .toString(Charsets.UTF_8)
  }

// Truncate mime-type (everything after the first space and semi-colon(if exists)
val String.truncateMimeType: String
  get() = replace("^([^ ]+).*$", "$1").substringBefore(";")

private fun parcelFileDescriptor(file: File): ParcelFileDescriptor? =
  ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)

// Default illustration size for ZIM file favicons
const val ILLUSTRATION_SIZE = 48

// Characters that are used as they are in a URL path: RFC 3986 "pchar" and "/", except "+",
// which is also percent-encoded because decoders of form data read it as a space.
private const val URL_PATH_CHARACTERS =
  "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~!$&'()*,;=:@/"

// The content URL of a ZIM entry path. Characters that have another meaning in a URL, such as
// "?", "#", "%" and "+", and spaces and non-ASCII characters are percent-encoded (UTF-8), so
// that the URL is read back to the same entry path by entryPath.
val String.addContentPrefix: String
  get() = if (startsWith(CONTENT_PREFIX)) {
    this
  } else {
    CONTENT_PREFIX + toByteArray(Charsets.UTF_8).joinToString("") { byte ->
      val code = byte.toUByte().toInt()
      if (code.toChar() in URL_PATH_CHARACTERS) "${code.toChar()}" else "%%%02X".format(code)
    }
  }

/**
 * Handles any error thrown by this method. Developers should handle the flow if this method
 * returns null. For more details, see: https://github.com/kiwix/kiwix-android/issues/4157
 */
fun Item.itemSize(): Long? =
  try {
    size
  } catch (ignore: Exception) {
    Log.e(TAG, "Could not retrieve the item size.\n Original exception: $ignore")
    null
  }
