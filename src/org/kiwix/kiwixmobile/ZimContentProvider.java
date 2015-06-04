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

package org.kiwix.kiwixmobile;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ZimContentProvider extends ContentProvider {

    public static final String TAG_KIWIX = "kiwix";

    public static final Uri CONTENT_URI = Uri.parse("content://org.kiwix.zim.base/");

    public static final Uri UI_URI = Uri.parse("content://org.kiwix.ui/");

    private static final String VIDEO_PATTERN = "([^\\s]+(\\.(?i)(3gp|mp4|m4a|webm|mkv|ogg|ogv))$)";

    private static final Pattern PATTERN = Pattern.compile(VIDEO_PATTERN, Pattern.CASE_INSENSITIVE);

    private static String zimFileName;

    private static JNIKiwix jniKiwix;

    private Matcher matcher;

    public synchronized static String setZimFile(String fileName) {
        if (!jniKiwix.loadZIM(fileName)) {
            Log.e(TAG_KIWIX, "Unable to open the file " + fileName);
            zimFileName = null;
        } else {
            zimFileName = fileName;
        }
        return zimFileName;
    }

    public static String getZimFile() {
        return zimFileName;
    }

    public static String getZimFileTitle() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            JNIKiwixString title = new JNIKiwixString();
            if (jniKiwix.getTitle(title)) {
                return title.value;
            } else {
                return null;
            }
        }
    }

    public static String getMainPage() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            return jniKiwix.getMainPage();
        }
    }

    public static String getId() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            return jniKiwix.getId();
        }
    }

    public static String getLanguage() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            return jniKiwix.getLanguage();
        }
    }

    public static boolean searchSuggestions(String prefix, int count) {
        if (jniKiwix == null || zimFileName == null) {
            return false;
        } else {
            return jniKiwix.searchSuggestions(prefix, count);
        }
    }

    public static String getNextSuggestion() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            JNIKiwixString title = new JNIKiwixString();
            if (jniKiwix.getNextSuggestion(title)) {
                return title.value;
            } else {
                return null;
            }
        }
    }

    public static String getPageUrlFromTitle(String title) {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            JNIKiwixString url = new JNIKiwixString();
            if (jniKiwix.getPageUrlFromTitle(title, url)) {
                return url.value;
            } else {
                return null;
            }
        }
    }

    public static String getRandomArticleUrl() {
        if (jniKiwix == null || zimFileName == null) {
            return null;
        } else {
            JNIKiwixString url = new JNIKiwixString();
            if (jniKiwix.getRandomPage(url)) {
                return url.value;
            } else {
                return null;
            }
        }
    }

    private static String loadICUData(Context context, File workingDir) {
        String icuFileName = "icudt49l.dat";
        try {
            File icuDir = new File(workingDir, "icu");
            if (!icuDir.exists()) {
                icuDir.mkdirs();
            }
            File icuDataFile = new File(icuDir, icuFileName);
            if (!icuDataFile.exists()) {
                InputStream in = context.getAssets().open(icuFileName);
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
            return icuDir.getAbsolutePath();
        } catch (Exception e) {
            Log.e(TAG_KIWIX, "Error copying icu data file", e);
            return null;
        }
    }

    private static String getFilePath(Uri articleUri) {
        String filePath = articleUri.toString();
        int pos = articleUri.toString().indexOf(CONTENT_URI.toString());
        if (pos != -1) {
            filePath = articleUri.toString().substring(
                    CONTENT_URI.toString().length());
        }
        // Remove fragment (#...) as not supported by zimlib
        pos = filePath.indexOf("#");
        if (pos != -1) {
            filePath = filePath.substring(0, pos);
        }
        return filePath;
    }

    @Override
    public boolean onCreate() {
        jniKiwix = new JNIKiwix();
        setIcuDataDirectory();
        return true;
    }

    @Override
    public String getType(Uri uri) {
        String mimeType;

        // This is the code which makes a guess based on the file extenstion
        String extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString().toLowerCase());
        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

        // This is the code which retrieve the mimeType from the libzim
        // "slow" and still bugyy
        if (mimeType.isEmpty()) {
            String t = uri.toString();
            int pos = uri.toString().indexOf(CONTENT_URI.toString());
            if (pos != -1) {
                t = uri.toString().substring(
                        CONTENT_URI.toString().length());
            }
            // Remove fragment (#...) as not supported by zimlib
            pos = t.indexOf("#");
            if (pos != -1) {
                t = t.substring(0, pos);
            }

            mimeType = jniKiwix.getMimeType(t);

            // Truncate mime-type (everything after the first space
            mimeType = mimeType.replaceAll("^([^ ]+).*$", "$1");
        }

        Log.d(TAG_KIWIX, "Getting mime-type for " + uri.toString() + " = " + mimeType);
        return mimeType;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {

        matcher = PATTERN.matcher(uri.toString());
        if (matcher.matches()) {
            try {
                return saveVideoToCache(uri);
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
            new TransferThread(jniKiwix, uri, new AutoCloseOutputStream(pipe[1])).start();
        } catch (IOException e) {
            Log.e(TAG_KIWIX, "Exception opening pipe", e);
            throw new FileNotFoundException("Could not open pipe for: "
                    + uri.toString());
        }

        return (pipe[0]);
    }

    private ParcelFileDescriptor saveVideoToCache(Uri uri) throws IOException {
        String filePath = getFilePath(uri);

        String fileName = uri.toString();
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1, fileName.length());

        File f = new File(FileUtils.getFileCacheDir(getContext()), fileName);

        JNIKiwixString mime = new JNIKiwixString();
        JNIKiwixInt size = new JNIKiwixInt();
        byte[] data = jniKiwix.getContent(filePath, mime, size);

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
        File workingDir = this.getContext().getFilesDir();
        String icuDirPath = loadICUData(this.getContext(), workingDir);

        if (icuDirPath != null) {
            Log.d(TAG_KIWIX, "Setting the ICU directory path to " + icuDirPath);
            jniKiwix.setDataDirectory(icuDirPath);
        }
    }

    static class TransferThread extends Thread {

        Uri articleUri;

        String articleZimUrl;

        OutputStream out;

        JNIKiwix jniKiwix;

        TransferThread(JNIKiwix jniKiwix, Uri articleUri, OutputStream out) throws IOException {
            this.articleUri = articleUri;
            this.jniKiwix = jniKiwix;
            Log.d(TAG_KIWIX, "Retrieving: " + articleUri.toString());

            String filePath = getFilePath(articleUri);

            this.out = out;
            this.articleZimUrl = filePath;
        }

        @Override
        public void run() {
            try {
                JNIKiwixString mime = new JNIKiwixString();
                JNIKiwixInt size = new JNIKiwixInt();
                byte[] data = jniKiwix.getContent(articleZimUrl, mime, size);
                out.write(data, 0, data.length);
                out.flush();

                Log.d(TAG_KIWIX, "reading  " + articleZimUrl
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