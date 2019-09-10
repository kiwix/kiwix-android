/*
 * Copyright 2013
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.data;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.ParcelFileDescriptor.AutoCloseOutputStream;
import android.util.Log;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.kiwix.kiwixlib.JNIKiwix;
import org.kiwix.kiwixlib.JNIKiwixException;
import org.kiwix.kiwixlib.JNIKiwixInt;
import org.kiwix.kiwixlib.JNIKiwixReader;
import org.kiwix.kiwixlib.JNIKiwixSearcher;
import org.kiwix.kiwixlib.JNIKiwixString;
import org.kiwix.kiwixlib.Pair;
import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.utils.files.FileUtils;

import static org.kiwix.kiwixmobile.utils.Constants.TAG_KIWIX;

public class ZimContentProvider extends ContentProvider {

  public static final Uri CONTENT_URI =
    Uri.parse("content://" + BuildConfig.APPLICATION_ID + ".zim.base/");

  public static final Uri UI_URI = Uri.parse("content://org.kiwix.ui/");
  private static final String VIDEO_PATTERN = "([^\\s]+(\\.(?i)(3gp|mp4|m4a|webm|mkv|ogg|ogv))$)";
  private static final Pattern PATTERN = Pattern.compile(VIDEO_PATTERN, Pattern.CASE_INSENSITIVE);
  public static String originalFileName = "";
  public static Boolean canIterate = true;
  public static String zimFileName;
  public static JNIKiwixReader currentJNIReader;
  public static JNIKiwixSearcher jniSearcher;
  private static ArrayList<String> listedEntries;
  @Inject
  public JNIKiwix jniKiwix;

  private static String getFulltextIndexPath(String file) {
    String[] names = { file, file };

    /* File might be a ZIM chunk like foobar.zimaa */
    if (!names[0].substring(names[0].length() - 3).equals("zim")) {
      names[0] = names[0].substring(0, names[0].length() - 2);
    }

    /* Try to find a *.idx fulltext file/directory beside the ZIM
     * file. Returns <zimfile>.zim.idx or <zimfile>.zimaa.idx. */
    for (String name : names) {
      File f = new File(name + ".idx");
      if (f.exists() && f.isDirectory()) {
        return f.getPath();
      }
    }

    /* If no separate fulltext index file found then returns the ZIM
     * file path itself (embedded fulltext index) */
    return file;
  }

  public synchronized static String setZimFile(String fileName) {
    if (!new File(fileName).exists()) {
      Log.e(TAG_KIWIX, "Unable to find the ZIM file " + fileName);
      zimFileName = null;
      return zimFileName;
    }
    try {
      JNIKiwixReader reader = new JNIKiwixReader(fileName);
      Log.i(TAG_KIWIX, "Opening ZIM file " + fileName);
      if (!listedEntries.contains(reader.getId())) {
        listedEntries.add(reader.getId());
        jniSearcher.addKiwixReader(reader);
      }
      currentJNIReader = reader;
      zimFileName = fileName;
    } catch (JNIKiwixException e) {
      Log.e(TAG_KIWIX, "Unable to open the ZIM file " + fileName);
      zimFileName = null;
    }
    return zimFileName;
  }

  /** Returns path to the current ZIM file */
  public static String getZimFile() {
    return zimFileName;
  }

  /**
   * Returns title associated with the current ZIM file.
   *
   * Note that the value returned is NOT unique for each zim file. Versions of the same wiki
   * (complete, nopic, novid, etc) may return the same title.
   */
  public static String getZimFileTitle() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      String title = currentJNIReader.getTitle();
      if (title != null) {
        return title;
      } else {
        return "No Title Found";
      }
    }
  }

  public static String getMainPage() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      return currentJNIReader.getMainPage();
    }
  }

  public static String getId() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      return currentJNIReader.getId();
    }
  }

  public static int getFileSize() {
    if (currentJNIReader == null || zimFileName == null) {
      return 0;
    } else {
      return currentJNIReader.getFileSize();
    }
  }

  public static String getCreator() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      return currentJNIReader.getCreator();
    }
  }

  public static String getPublisher() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      return currentJNIReader.getPublisher();
    }
  }

  public static String getName() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      String name = currentJNIReader.getName();
      if (name == null || name.equals("")) {
        return getId();
      } else {
        return name;
      }
    }
  }

  public static String getDate() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      return currentJNIReader.getDate();
    }
  }

  public static String getDescription() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      return currentJNIReader.getDescription();
    }
  }

  public static String getFavicon() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      JNIKiwixString mime = new JNIKiwixString();
      mime.value = "image/x-ms-bmp";
      return currentJNIReader.getFavicon();
    }
  }

  public static String getLanguage() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      return currentJNIReader.getLanguage();
    }
  }

  public static boolean searchSuggestions(String prefix, int count) {
    if (currentJNIReader == null || zimFileName == null) {
      return false;
    } else {
      return currentJNIReader.searchSuggestions(prefix, count);
    }
  }

  public static String getNextSuggestion() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      JNIKiwixString title = new JNIKiwixString();
      if (currentJNIReader.getNextSuggestion(title)) {
        return title.value;
      } else {
        return null;
      }
    }
  }

  public static String getPageUrlFromTitle(String title) {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      JNIKiwixString url = new JNIKiwixString();
      if (currentJNIReader.getPageUrlFromTitle(title, url)) {
        return url.value;
      } else {
        return null;
      }
    }
  }

  public static String getRandomArticleUrl() {
    if (currentJNIReader == null || zimFileName == null) {
      return null;
    } else {
      JNIKiwixString url = new JNIKiwixString();
      if (currentJNIReader.getRandomPage(url)) {
        return url.value;
      } else {
        return null;
      }
    }
  }

  private static String loadICUData(Context context, File workingDir) {
    try {
      File icuDir = new File(workingDir, "icu");
      if (!icuDir.exists()) {
        icuDir.mkdirs();
      }
      String[] icuFileNames = context.getAssets().list("icu");
      for (int i = 0; i < icuFileNames.length; i++) {
        String icuFileName = icuFileNames[i];
        File icuDataFile = new File(icuDir, icuFileName);
        if (!icuDataFile.exists()) {
          InputStream in = context.getAssets().open("icu/" + icuFileName);
          OutputStream out = new FileOutputStream(icuDataFile);
          byte[] buf = new byte[1024];
          int len;
          while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
          }
          in.close();
          out.flush();
          out.close();
        }
      }
      return icuDir.getAbsolutePath();
    } catch (Exception e) {
      Log.w(TAG_KIWIX, "Error copying icu data file", e);
      //TODO: Consider surfacing to user
      return null;
    }
  }

  private static String getFilePath(Uri articleUri) {
    return getFilePath(articleUri.toString());
  }

  public void setupDagger() {
    KiwixApplication.getApplicationComponent().inject(this);
    setIcuDataDirectory();
    jniSearcher = new JNIKiwixSearcher();
    listedEntries = new ArrayList<>();
  }

  @Override
  public boolean onCreate() {
    setupDagger();
    return true;
  }

  @Override
  public String getType(Uri uri) {
    String mimeType;
    // This is the code which makes a guess based on the file extenstion
    final String uriWithoutArguments = removeArguments(uri.toString());
    String extension = MimeTypeMap.getFileExtensionFromUrl(uriWithoutArguments.toLowerCase());
    mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

    // This is the code which retrieve the mimeType from the libzim
    // "slow" and still bugyy
    if (mimeType == null || mimeType.isEmpty()) {
      mimeType = currentJNIReader.getMimeType(getFilePath(uriWithoutArguments))
        // Truncate mime-type (everything after the first space
        .replaceAll("^([^ ]+).*$", "$1");
    }

    Log.d(TAG_KIWIX, "Getting mime-type for " + uriWithoutArguments + " = " + mimeType);
    return mimeType;
  }

  @NotNull private static String getFilePath(String uriString) {
    String filePath = uriString;
    int pos = uriString.indexOf(CONTENT_URI.toString());
    if (pos != -1) {
      filePath = uriString.substring(
        CONTENT_URI.toString().length());
    }
    // Remove fragment (#...) as not supported by zimlib
    pos = filePath.indexOf("#");
    if (pos != -1) {
      filePath = filePath.substring(0, pos);
    }
    return filePath;
  }

  private static String removeArguments(String url) {
    return url.contains("?")
      ? url.substring(0, url.lastIndexOf("?"))
      : url;
  }

  public static String getRedirect(String url) {
    return Uri.parse(CONTENT_URI + currentJNIReader.checkUrl(getFilePath(Uri.parse(url))))
      .toString();
  }

  public static boolean isRedirect(String url) {
    return !url.equals(getRedirect(url));
  }

  @Override
  public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {

    Matcher matcher = PATTERN.matcher(uri.toString());
    if (matcher.matches()) {
      try {
        return loadVideo(uri);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return loadContent(uri);
  }

  private ParcelFileDescriptor loadContent(Uri uri) throws FileNotFoundException {
    ParcelFileDescriptor[] pipe;
    try {
      pipe = ParcelFileDescriptor.createPipe();
      new TransferThread(currentJNIReader, uri, new AutoCloseOutputStream(pipe[1])).start();
    } catch (IOException e) {
      //TODO: Why do we narrow the exception? We can't be sure the file isn't found
      throw new FileNotFoundException("Could not open pipe for: "
        + uri.toString());
    }
    return (pipe[0]);
  }

  // Return a file descriptor of the video within the ZIM file
  private ParcelFileDescriptor loadVideo(Uri uri) throws IOException {
    Pair pair = currentJNIReader.getDirectAccessInformation(getFilePath(uri));
    if (pair.filename == null || !new File(pair.filename).exists()) {
      return loadVideoViaCache(uri);
    }
    RandomAccessFile randomAccessFile = new RandomAccessFile(pair.filename, "r");
    randomAccessFile.seek(pair.offset);
    return ParcelFileDescriptor.dup(randomAccessFile.getFD());
  }

  // Backup mechanism to load video files between split ZIM by saving them locally
  private ParcelFileDescriptor loadVideoViaCache(Uri uri) throws IOException {
    String filePath = getFilePath(uri);
    String fileName = uri.toString();
    fileName = fileName.substring(fileName.lastIndexOf('/') + 1);
    File f = new File(FileUtils.getFileCacheDir(getContext()), fileName);
    byte[] data = currentJNIReader.getContent(new JNIKiwixString(filePath), new JNIKiwixString(),
      new JNIKiwixString(), new JNIKiwixInt());
    FileOutputStream out = new FileOutputStream(f);
    out.write(data, 0, data.length);
    out.flush();
    return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
  }

  @Override
  public Cursor query(Uri url, String[] projection, String selection,
    String[] selectionArgs, String sort) {
    throw new RuntimeException("Operation not supported");
  }

  @Override
  public Uri insert(Uri uri, ContentValues initialValues) {
    throw new RuntimeException("Operation not supported");
  }

  @Override
  public int update(Uri uri, ContentValues values, String where,
    String[] whereArgs) {
    throw new RuntimeException("Operation not supported");
  }

  @Override
  public int delete(Uri uri, String where, String[] whereArgs) {
    throw new RuntimeException("Operation not supported");
  }

  private void setIcuDataDirectory() {
    File workingDir = KiwixApplication.getInstance().getFilesDir();
    String icuDirPath = loadICUData(KiwixApplication.getInstance(), workingDir);

    if (icuDirPath != null) {
      Log.d(TAG_KIWIX, "Setting the ICU directory path to " + icuDirPath);
      jniKiwix.setDataDirectory(icuDirPath);
    }
  }

  static class TransferThread extends Thread {

    String articleZimUrl;

    OutputStream out;

    JNIKiwixReader currentJNIReader;

    TransferThread(JNIKiwixReader currentJNIReader, Uri articleUri, OutputStream out)
      throws IOException {
      this.currentJNIReader = currentJNIReader;
      Log.d(TAG_KIWIX, "Retrieving: " + articleUri.toString());

      String filePath = getFilePath(articleUri);

      this.out = out;
      this.articleZimUrl = filePath;
    }

    @Override
    public void run() {
      try {
        final JNIKiwixString mime = new JNIKiwixString();
        final JNIKiwixInt size = new JNIKiwixInt();
        final JNIKiwixString url = new JNIKiwixString(removeArguments(articleZimUrl));
        byte[] data = currentJNIReader.getContent(url, new JNIKiwixString(), mime, size);
        if (mime.value != null && mime.value.equals("text/css") && MainActivity.nightMode) {
          out.write(("img, video { \n" +
            " -webkit-filter: invert(1); \n" +
            " filter: invert(1); \n" +
            "} \n").getBytes(Charset.forName("UTF-8")));
        }
        out.write(data, 0, data.length);
        out.flush();

        Log.d(TAG_KIWIX, "reading  " + url.value
          + "(mime: " + mime.value + ", size: " + size.value + ") finished.");
      } catch (IOException | NullPointerException e) {
        Log.e(TAG_KIWIX, "Exception reading article " + articleZimUrl + " from zim file",
          e);
      } finally {
        try {
          out.close();
        } catch (IOException e) {
          Log.e(TAG_KIWIX,
            "Custom exception by closing out stream for article " + articleZimUrl,
            e);
        }
      }
    }
  }
}
