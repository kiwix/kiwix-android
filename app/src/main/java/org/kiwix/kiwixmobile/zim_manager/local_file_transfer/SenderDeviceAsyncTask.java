package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.app.Activity;
import android.content.ContentResolver;
import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.kiwix.kiwixmobile.BuildConfig;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.ERROR;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.SENDING;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.SENT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.WifiDirectManager.FILE_TRANSFER_PORT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.WifiDirectManager.copyToOutputStream;

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
class SenderDeviceAsyncTask extends AsyncTask<FileItem, Integer, Boolean> {

  private static final String TAG = "SenderDeviceAsyncTask";

  private WifiDirectManager wifiDirectManager;
  private ContentResolver contentResolver;

  public SenderDeviceAsyncTask(WifiDirectManager wifiDirectManager, Activity activity) {
    this.wifiDirectManager = wifiDirectManager;
    this.contentResolver = activity.getContentResolver();
  }

  @Override
  protected Boolean doInBackground(FileItem... fileItems) {

    if (delayForSlowReceiverDevicesToSetupServer() == false) {
      return false;
    }

    String hostAddress = wifiDirectManager.getFileReceiverDeviceAddress().getHostAddress();
    boolean isTransferErrorFree = true;

    for (int fileIndex = 0; fileIndex < fileItems.length && !isCancelled(); fileIndex++) {
      FileItem fileItem = fileItems[fileIndex];

      try (Socket socket = new Socket(); // Represents the sender device
           InputStream fileInputStream = contentResolver.openInputStream(fileItem.getFileUri())) {

        socket.bind(null);
        socket.connect((new InetSocketAddress(hostAddress, FILE_TRANSFER_PORT)), 15000);

        Log.d(TAG, "Sender socket connected to server - " + socket.isConnected());

        publishProgress(fileIndex, SENDING);
        OutputStream socketOutputStream = socket.getOutputStream();

        copyToOutputStream(fileInputStream, socketOutputStream);
        if (BuildConfig.DEBUG) Log.d(TAG, "Sender: Data written");
        publishProgress(fileIndex, SENT);

      } catch (IOException e) {
        Log.e(TAG, e.getMessage());
        e.printStackTrace();

        isTransferErrorFree = false;
        publishProgress(fileIndex, ERROR);
      }
    }

    return (!isCancelled() && isTransferErrorFree);
  }

  private boolean delayForSlowReceiverDevicesToSetupServer() {
    try { // Delay trying to connect with receiver, to allow slow receiver devices to setup server
      Thread.sleep(3000);
    } catch (InterruptedException e) {
      Log.e(TAG, e.getMessage());
      return false;
    }
    return true;
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    int fileIndex = values[0];
    int fileStatus = values[1];
    wifiDirectManager.changeStatus(fileIndex, fileStatus);
  }

  @Override protected void onCancelled() {
    Log.d(TAG, "SenderDeviceAsyncTask cancelled");
  }

  @Override
  protected void onPostExecute(Boolean wereAllFilesTransferred) {
    if (BuildConfig.DEBUG) Log.d(TAG, "SenderDeviceAsyncTask complete");
    wifiDirectManager.onFileTransferAsyncTaskComplete(wereAllFilesTransferred);
  }
}
