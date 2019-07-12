package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.app.Activity;
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

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.DeviceListFragment.FILE_TRANSFER_PORT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.DeviceListFragment.getFileName;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.*;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.showToast;

/**
 * Helper class for the local file sharing module, used in {@link DeviceListFragment}.
 *
 * Once the handshake between the two connected devices has taked place, this async-task is used
 * on the sender device to transfer the file to the receiver device at the FILE_TRANSFER_PORT port.
 *
 * It takes in the uri of a single file, and copies all the bytes from input stream of the file to
 * the output stream of the receiver device. Also changes the status of the corresponding FileItem
 * on the list of files for transfer in {@link TransferProgressFragment}.
 *
 * A new task is created by the sender for every file to be transferred
 * */
class SenderDeviceAsyncTask extends AsyncTask<Uri, Void, Boolean> {

  private static final String TAG = "SenderDeviceAsyncTask";

  private DeviceListFragment deviceListFragment;
  private TransferProgressFragment transferProgressFragment;
  private int fileItemIndex;

  public SenderDeviceAsyncTask(DeviceListFragment deviceListFragment, TransferProgressFragment transferProgressFragment, int fileItemIndex) {
    this.deviceListFragment = deviceListFragment;
    this.transferProgressFragment = transferProgressFragment;
    this.fileItemIndex = fileItemIndex;
  }

  @Override
  protected void onPreExecute() {
    transferProgressFragment.changeStatus(fileItemIndex, SENDING);
  }

  @Override
  protected Boolean doInBackground(Uri... fileUris) {
    Uri fileUri = fileUris[0];    // Uri of file to be transferred
    Socket socket = new Socket(); // Represents the sender device
    boolean fileSendSuccessful;   // Whether this task was successful in sending the file

    try {
      socket.bind(null);

      String hostAddress = deviceListFragment.getFileReceiverDeviceAddress().getHostAddress();
      socket.connect((new InetSocketAddress(hostAddress, FILE_TRANSFER_PORT)), 15000);

      if(BuildConfig.DEBUG) Log.d(TAG, "Sender socket - " + socket.isConnected());

      OutputStream socketOutputStream = socket.getOutputStream();

      ContentResolver contentResolver = deviceListFragment.getActivity().getContentResolver();
      InputStream fileInputStream = contentResolver.openInputStream(fileUri);

      DeviceListFragment.copyToOutputStream(fileInputStream, socketOutputStream);
      if(BuildConfig.DEBUG) Log.d(TAG, "Sender: Data written");

      fileSendSuccessful = true;

    } catch (IOException e) {
      Log.e(TAG, e.getMessage());
      fileSendSuccessful = false;

    } finally {
      if (socket.isConnected()) {
        try {
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return fileSendSuccessful;
  }

  @Override
  protected void onPostExecute(Boolean fileSendSuccessful) {
    deviceListFragment.incrementTotalFilesSent();

    if(fileSendSuccessful) {
      transferProgressFragment.changeStatus(fileItemIndex, SENT);
    } else {
      Activity activity = deviceListFragment.getActivity();
      showToast(activity, activity.getString(R.string.error_sending, getFileName(deviceListFragment.getFileUriList().get(fileItemIndex))), Toast.LENGTH_SHORT);
      transferProgressFragment.changeStatus(fileItemIndex, ERROR);
    }

    if(deviceListFragment.allFilesSent()) {
      showToast(deviceListFragment.getActivity(), R.string.all_files_transferred, Toast.LENGTH_SHORT);
      deviceListFragment.getActivity().finish();
    }
  }
}
