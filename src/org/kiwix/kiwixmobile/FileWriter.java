package org.kiwix.kiwixmobile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class FileWriter {

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

        ArrayList<String> list = new ArrayList<String>();

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

    // Add items to the MediaStore list, that are not in the MediaStore database.
    // These will be loaded from a previously saved CSV file.
    // We are checking, if these file still exist as well.
    public ArrayList<DataModel> getDataModelList() {

        for (String file : readCsv()) {
            if (!mDataList.contains(new DataModel(getTitleFromFilePath(file), file))) {
                Log.i("kiwix", "Added file: " + file);
                mDataList.add(new DataModel(getTitleFromFilePath(file), file));
            }
        }

        return mDataList;
    }

    // Split the CSV by the comma and return an ArrayList with the file paths
    private ArrayList<String> readCsv() {

        String csv = getCsvFromPrefrences();

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


