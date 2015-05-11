package org.kiwix.kiwixmobile;

import android.content.Context;
import android.os.Environment;

import java.io.File;

public class FileUtils {

    public static File getFileCacheDir(Context context) {
        boolean external = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());

        if (external) {
            return context.getExternalCacheDir();

        } else {
            return context.getCacheDir();
        }
    }


    public static void deleteCachedFiles(Context context) {
        for (File file : getFileCacheDir(context).listFiles()) {
            file.delete();
        }

    }
}
