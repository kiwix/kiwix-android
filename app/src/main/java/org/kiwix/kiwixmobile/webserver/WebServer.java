package org.kiwix.kiwixmobile.webserver;

import android.util.Log;
import fi.iki.elonen.NanoHTTPD;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class WebServer extends NanoHTTPD {
  private String selectedFilePath;

  public WebServer(int port, String selectedFilePath) {
    super(port);
    this.selectedFilePath = selectedFilePath;
  }

  @Override
  public Response serve(IHTTPSession session) {
    String answer = "";
    try {
      FileReader index = new FileReader(selectedFilePath);
      BufferedReader reader = new BufferedReader(index);
      String line = "";
      while ((line = reader.readLine()) != null) {
        answer += line;
      }
      reader.close();
    } catch (IOException ioe) {
      Log.w("Httpd", ioe.toString());
    }

    return newFixedLengthResponse(answer);
  }
}
