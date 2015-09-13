/*
 * Copyright 2013 Rashiq Ahmad <rashiq.z@gmail.com>
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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public class FileWriter {

    public static final String TAG_KIWIX = "kiwix";

    private static final String PREF_NAME = "csv_file";

    private static final String CSV_PREF_NAME = "csv_string";

    private Context mContext;

    private ArrayList<DataModel> mDataList;

    public FileWriter(Context context) {
        mContext = context;
    }

    public FileWriter(Context context, ArrayList<DataModel> dataList) {
        mDataList = dataList;
        mContext = context;
    }

    // Build a CSV list from the file paths
    public void saveArray(ArrayList<DataModel> files) {

        ArrayList<String> list = new ArrayList<>();

        for (DataModel file : files) {
            list.add(file.getPath());
        }

        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s);
            sb.append(",");
        }

        saveCsvToPrefrences(sb.toString());
    }

    // Read the locales.txt file in the assets folder, that has been created at compile time by the
    // build script
    public ArrayList<String> readFileFromAssets() {

        String content = "";

        try {
            InputStream stream = mContext.getAssets().open("locales.txt");

            int size = stream.available();
            byte[] buffer = new byte[size];
            stream.read(buffer);
            stream.close();
            content = new String(buffer);

        } catch (IOException e) {

        }

        return readCsv(content);
    }

    // Add items to the MediaStore list, that are not in the MediaStore database.
    // These will be loaded from a previously saved CSV file.
    // We are checking, if these file still exist as well.
    public ArrayList<DataModel> getDataModelList() {

        for (String file : readCsv()) {
            if (!mDataList.contains(new DataModel(getTitleFromFilePath(file), file))) {
                Log.i(TAG_KIWIX, "Added file: " + file);
                mDataList.add(new DataModel(getTitleFromFilePath(file), file));
            }
        }

        return mDataList;
    }

    // Split the CSV by the comma and return an ArrayList with the file paths
    private ArrayList<String> readCsv() {

        String csv = getCsvFromPrefrences();

        return readCsv(csv);
    }

    private ArrayList<String> readCsv(String csv) {

        String[] csvArray = csv.split(",");

        return new ArrayList<String>(Arrays.asList(csvArray));
    }

    // Save a CSV file to the prefrences
    private void saveCsvToPrefrences(String csv) {

        SharedPreferences preferences = mContext.getSharedPreferences(PREF_NAME, 0);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(CSV_PREF_NAME, csv);

        editor.commit();
    }

    // Load the CSV from the prefrences
    private String getCsvFromPrefrences() {
        SharedPreferences preferences = mContext.getSharedPreferences(PREF_NAME, 0);

        return preferences.getString(CSV_PREF_NAME, "");
    }

    // Remove the file path and the extension and return a file name for the given file path
    private String getTitleFromFilePath(String path) {
        return new File(path).getName().replaceFirst("[.][^.]+$", "");
    }
}


