package org.kiwix.kiwixmobile.utils.files;

import android.content.Context;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class FileReader {

  public String readFile(String filePath, Context context) {
    try {
      StringBuilder buf = new StringBuilder();
      InputStream json = context.getAssets().open(filePath);
      BufferedReader in =
          new BufferedReader(new InputStreamReader(json, "UTF-8"));
      String str;

      while ((str = in.readLine()) != null) {
        buf.append(str);
      }

      in.close();
      return buf.toString();
    } catch (Exception e) {
      return "";
    }
  }
}
