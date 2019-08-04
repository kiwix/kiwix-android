package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.R;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.*;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.WifiDirectManager.FILE_TRANSFER_PORT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.WifiDirectManager.copyToOutputStream;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.WifiDirectManager.getFileName;

/**
 * Helper class for the local file sharing module.
 *
 * Once the handshake between the two connected devices has taked place, this async-task is used
 * on the sender device to transfer the file to the receiver device at the FILE_TRANSFER_PORT port.
 *
 * It takes in the uri of all the files to be shared. For each file uri, creates a new connection &
 * copies all the bytes from input stream of the file to the output stream of the receiver device.
 * Also changes the status of the corresponding FileItem on the list of files for transfer.
 *
 * A single task is used by the sender for the entire transfer
 */
class SenderDeviceAsyncTask extends AsyncTask<Uri, Integer, Boolean> {

  private static final String TAG = "SenderDeviceAsyncTask";

  private WifiDirectManager wifiDirectManager;
  private ContentResolver contentResolver;

  public SenderDeviceAsyncTask(WifiDirectManager wifiDirectManager, LocalFileTransferActivity localFileTransferActivity) {
    this.wifiDirectManager = wifiDirectManager;
    this.contentResolver = localFileTransferActivity.getContentResolver();
  }

  @Override
  protected Boolean doInBackground(Uri... fileUris) {

    /*try {
      Thread.sleep(5000);
    } catch (InterruptedException e) {
      Log.e(TAG, e.getMessage());
      return false;
    }*/

    for(int i = 0; i < 2000000000; i++);

    boolean result = true;
    int fileItemIndex = -1;

    for(Uri fileUri : fileUris) { // Uri of file to be transferred
      fileItemIndex++;

      try (Socket socket = new Socket(); // Represents the sender device
           InputStream fileInputStream = contentResolver.openInputStream(fileUri)) {

        if (isCancelled()) {
          result = false;
          return result;
        }
        socket.bind(null);

        String hostAddress = wifiDirectManager.getFileReceiverDeviceAddress().getHostAddress();
        socket.connect((new InetSocketAddress(hostAddress, FILE_TRANSFER_PORT)), 15000);

        if (BuildConfig.DEBUG) Log.d(TAG, "Sender socket - " + socket.isConnected());
        publishProgress(fileItemIndex, SENDING);

        OutputStream socketOutputStream = socket.getOutputStream();

        copyToOutputStream(fileInputStream, socketOutputStream);
        if (BuildConfig.DEBUG) Log.d(TAG, "Sender: Data written");

        publishProgress(fileItemIndex, SENT);

      } catch (IOException e) {
        Log.e(TAG, e.getMessage());
        e.printStackTrace();
        result = false;
        publishProgress(fileItemIndex, ERROR);
      }

      wifiDirectManager.incrementTotalFilesSent();
    }

    return result;
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    int fileIndex = values[0];
    int fileStatus = values[1];
    wifiDirectManager.changeStatus(fileIndex, fileStatus);

    if(fileStatus == ERROR) {
      wifiDirectManager.displayToast(R.string.error_transferring, getFileName(wifiDirectManager.getFileUriArrayList().get(fileIndex)), Toast.LENGTH_SHORT);
    }
  }

  @Override protected void onCancelled() {
    Log.d(TAG, "SenderDeviceAsyncTask cancelled");
  }

  @Override
  protected void onPostExecute(Boolean fileSendSuccessful) {
    if (wifiDirectManager.allFilesSent()) {
      wifiDirectManager.displayToast(R.string.file_transfer_complete, Toast.LENGTH_SHORT);
    }
    wifiDirectManager.onFileTransferAsyncTaskComplete();
  }
}
