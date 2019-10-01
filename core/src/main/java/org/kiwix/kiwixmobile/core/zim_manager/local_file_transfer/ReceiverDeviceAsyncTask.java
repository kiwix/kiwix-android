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

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import org.kiwix.kiwixmobile.core.BuildConfig;
import org.kiwix.kiwixmobile.core.R;

import static org.kiwix.kiwixmobile.core.zim_manager.local_file_transfer.FileItem.FileStatus.ERROR;
import static org.kiwix.kiwixmobile.core.zim_manager.local_file_transfer.FileItem.FileStatus.SENDING;
import static org.kiwix.kiwixmobile.core.zim_manager.local_file_transfer.FileItem.FileStatus.SENT;

/**
 * Helper class for the local file sharing module.
 *
 * Once the handshake has successfully taken place, this async-task is used to receive files from
 * the sender device on the FILE_TRANSFER_PORT port. No. of files to be received (and their names)
 * are learnt beforehand during the handshake.
 *
 * A single Task is used for the entire file transfer (the server socket accepts connections as
 * many times as the no. of files).
 */
class ReceiverDeviceAsyncTask extends AsyncTask<Void, Integer, Boolean> {

  private static final String TAG = "ReceiverDeviceAsyncTask";

  private WifiDirectManager wifiDirectManager;
  private String incomingFileName;

  public ReceiverDeviceAsyncTask(WifiDirectManager wifiDirectManager) {
    this.wifiDirectManager = wifiDirectManager;
  }

  @Override
  protected Boolean doInBackground(Void... voids) {
    try (ServerSocket serverSocket = new ServerSocket(WifiDirectManager.FILE_TRANSFER_PORT)) {
      Log.d(TAG, "Server: Socket opened at " + WifiDirectManager.FILE_TRANSFER_PORT);

      final String zimStorageRootPath = wifiDirectManager.getZimStorageRootPath();
      ArrayList<FileItem> fileItems = wifiDirectManager.getFilesForTransfer();
      boolean isTransferErrorFree = true;

      if (BuildConfig.DEBUG) Log.d(TAG, "Expecting " + fileItems.size() + " files");

      for (int fileItemIndex = 0; fileItemIndex < fileItems.size() && !isCancelled();
        fileItemIndex++) {
        incomingFileName = fileItems.get(fileItemIndex).getFileName();

        try (Socket client = serverSocket.accept()) {
          if (BuildConfig.DEBUG) {
            Log.d(TAG, "Sender device connected for " + fileItems.get(fileItemIndex).getFileName());
          }
          publishProgress(fileItemIndex, SENDING);

          final File clientNoteFileLocation = new File(zimStorageRootPath + incomingFileName);
          File dirs = new File(clientNoteFileLocation.getParent());
          if (!dirs.exists() && !dirs.mkdirs()) {
            Log.d(TAG, "ERROR: Required parent directories couldn't be created");
            isTransferErrorFree = false;
            continue;
          }

          boolean fileCreated = clientNoteFileLocation.createNewFile();
          if (BuildConfig.DEBUG) Log.d(TAG, "File creation: " + fileCreated);

          WifiDirectManager.copyToOutputStream(client.getInputStream(), new FileOutputStream(clientNoteFileLocation));
          publishProgress(fileItemIndex, SENT);
        } catch (IOException e) {
          Log.e(TAG, e.getMessage());
          isTransferErrorFree = false;
          publishProgress(fileItemIndex, ERROR);
        }
      }
      return (!isCancelled() && isTransferErrorFree);
    } catch (IOException e) {
      Log.e(TAG, e.getMessage());
      return false; // Returned when an error was encountered during transfer
    }
  }

  @Override
  protected void onProgressUpdate(Integer... values) {
    int fileIndex = values[0];
    int fileStatus = values[1];
    wifiDirectManager.changeStatus(fileIndex, fileStatus);

    if (fileStatus == ERROR) {
      wifiDirectManager.displayToast(R.string.error_transferring, incomingFileName,
        Toast.LENGTH_SHORT);
    }
  }

  @Override protected void onCancelled() {
    Log.d(TAG, "ReceiverDeviceAsyncTask cancelled");
  }

  @Override
  protected void onPostExecute(Boolean wereAllFilesTransferred) {
    if (BuildConfig.DEBUG) Log.d(TAG, "ReceiverDeviceAsyncTask complete");
    wifiDirectManager.onFileTransferAsyncTaskComplete(wereAllFilesTransferred);
  }
}
