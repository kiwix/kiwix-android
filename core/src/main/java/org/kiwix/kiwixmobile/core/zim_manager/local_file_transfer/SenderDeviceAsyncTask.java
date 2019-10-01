/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.zim_manager.local_file_transfer;

import android.app.Activity;
import android.content.ContentResolver;
import android.os.AsyncTask;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import org.kiwix.kiwixmobile.core.BuildConfig;

import static org.kiwix.kiwixmobile.core.zim_manager.local_file_transfer.FileItem.FileStatus.ERROR;
import static org.kiwix.kiwixmobile.core.zim_manager.local_file_transfer.FileItem.FileStatus.SENDING;
import static org.kiwix.kiwixmobile.core.zim_manager.local_file_transfer.FileItem.FileStatus.SENT;

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
        socket.connect((new InetSocketAddress(hostAddress, WifiDirectManager.FILE_TRANSFER_PORT)), 15000);

        Log.d(TAG, "Sender socket connected to server - " + socket.isConnected());

        publishProgress(fileIndex, SENDING);
        OutputStream socketOutputStream = socket.getOutputStream();

        WifiDirectManager.copyToOutputStream(fileInputStream, socketOutputStream);
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
