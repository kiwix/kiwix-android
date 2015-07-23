package org.kiwix.kiwixmobile;

import org.kiwix.kiwixmobile.settings.Constants;

import java.io.File;

import android.content.Context;
import android.os.Environment;

public class FileUtils {

    public static File getFileCacheDir(Context context) {
        boolean external = Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());

        if (external) {
            return context.getExternalCacheDir();

        } else {
            return context.getCacheDir();
        }
    }


    public static synchronized void deleteCachedFiles(Context context) {
        try {
            for (File file : getFileCacheDir(context).listFiles()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the file name (without full path) for an Expansion APK file from the given context.
     *
     * @param mainFile true for menu_main file, false for patch file
     * @return String the file name of the expansion file
     */
    public static String getExpansionAPKFileName(boolean mainFile) {
        return (mainFile ? "main." : "patch.") + Constants.CUSTOM_APP_VERSION_CODE + "."
                + Constants.CUSTOM_APP_ID + ".obb";
    }

    /**
     * Returns the filename (where the file should be saved) from info about a download
     */
    static public String generateSaveFileName(String fileName) {
        return getSaveFilePath() + File.separator + fileName;
    }

    static public String getSaveFilePath() {
        String obbFolder = File.separator + "Android" + File.separator + "obb" + File.separator;
        File root = Environment.getExternalStorageDirectory();
        return root.toString() + obbFolder + Constants.CUSTOM_APP_ID;
    }

    /**
     * Helper function to ascertain the existence of a file and return true/false appropriately
     *
     * @param fileName             the name (sans path) of the file to query
     * @param fileSize             the size that the file must match
     * @param deleteFileOnMismatch if the file sizes do not match, delete the file
     * @return true if it does exist, false otherwise
     */
    static public boolean doesFileExist(String fileName, long fileSize,
                                        boolean deleteFileOnMismatch) {
        // the file may have been delivered by Market --- let's make sure
        // it's the size we expect
        File fileForNewFile = new File(fileName);
        if (fileForNewFile.exists()) {
            if (fileForNewFile.length() == fileSize) {
                return true;
            }
            if (deleteFileOnMismatch) {
                // delete the file --- we won't be able to resume
                // because we cannot confirm the integrity of the file
                fileForNewFile.delete();
            }
        }
        return false;
    }
}
