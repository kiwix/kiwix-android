package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.R;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.ERROR;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.SENDING;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.SENT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.WifiDirectManager.FILE_TRANSFER_PORT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.WifiDirectManager.copyToOutputStream;

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
    try (ServerSocket serverSocket = new ServerSocket(FILE_TRANSFER_PORT)) {
      Log.d(TAG, "Server: Socket opened at " + FILE_TRANSFER_PORT);

      final String zimStorageRootPath = wifiDirectManager.getZimStorageRootPath();
      ArrayList<FileItem> fileItems = wifiDirectManager.getFilesForTransfer();
      boolean isTransferErrorFree = true;

      if (BuildConfig.DEBUG) Log.d(TAG, "Expecting " + fileItems.size() + " files");

      for (int fileItemIndex = 0; fileItemIndex < fileItems.size() && !isCancelled(); fileItemIndex++) {
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

          copyToOutputStream(client.getInputStream(), new FileOutputStream(clientNoteFileLocation));
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
