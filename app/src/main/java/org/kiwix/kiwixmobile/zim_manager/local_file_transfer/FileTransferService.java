
package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * A service that process each file transfer request i.e Intent by opening a
 * socket connection with the WiFi Direct Group Owner and writing the file
 */
public class FileTransferService extends IntentService {

  private static final int SOCKET_TIMEOUT = 15000;
  public static final String ACTION_SEND_FILE = "dev.sood.hermes.SEND_FILE";
  public static final String EXTRAS_FILE_URI = "file_url";
  public static final String EXTRAS_GROUP_OWNER_ADDRESS = "go_host";
  public static final String EXTRAS_GROUP_OWNER_PORT = "go_port";

  public FileTransferService(String name) {
    super(name);
    Log.d(LocalFileTransferActivity.TAG, "In FileTransferService constructor");
  }

  public FileTransferService() {
    super("FileTransferService");
    Log.d(LocalFileTransferActivity.TAG, "In FileTransferService constructor");
  }

  /*
   * (non-Javadoc)
   * @see android.app.IntentService#onHandleIntent(android.content.Intent)
   */
  @Override
  protected void onHandleIntent(Intent intent) {

    Log.d(LocalFileTransferActivity.TAG, "In onHandleIntent");

    Context context = getApplicationContext();
    if (intent.getAction().equals(ACTION_SEND_FILE)) {

      Log.d(LocalFileTransferActivity.TAG, "In main if-else");

      String fileUriString = intent.getExtras().getString(EXTRAS_FILE_URI);
      String host = intent.getExtras().getString(EXTRAS_GROUP_OWNER_ADDRESS);
      Socket socket = new Socket();
      int port = intent.getExtras().getInt(EXTRAS_GROUP_OWNER_PORT);

      try {
        Log.d(LocalFileTransferActivity.TAG, "Opening client socket - ");
        socket.bind(null);
        socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);

        Log.d(LocalFileTransferActivity.TAG, "Client socket - " + socket.isConnected());
        OutputStream stream = socket.getOutputStream();
        ContentResolver cr = context.getContentResolver();
        InputStream is = null;
        try {
          is = cr.openInputStream(Uri.parse(fileUriString));
        } catch (FileNotFoundException e) {
          Log.d(LocalFileTransferActivity.TAG, e.toString());
        }
        DeviceListFragment.copyFile(is, stream);
        Log.d(LocalFileTransferActivity.TAG, "Client: Data written");
      } catch (IOException e) {
        Log.e(LocalFileTransferActivity.TAG, e.getMessage());
      } finally {
        if (socket != null) {
          if (socket.isConnected()) {
            try {
              socket.close();
            } catch (IOException e) {
              // Give up
              e.printStackTrace();
            }
          }
        }
      }

    }
  }
}

