package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.net.Uri;
import android.net.wifi.p2p.WifiP2pInfo;
import android.os.AsyncTask;
import android.util.Log;

import org.kiwix.kiwixmobile.BuildConfig;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.FileStatus.TO_BE_SENT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.PEER_HANDSHAKE_PORT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.getFileName;

/**
 * Helper class for the local file sharing module.
 *
 * Once two peer devices are connected through wifi direct, this task is executed to perform a
 * handshake between the two devices. The purpose of the handshake is to allow the file sending
 * device to obtain the IP address of the file receiving device (When the file sending device
 * is the wifi direct group owner, it doesn't have the IP address of the peer device by default).
 *
 * After obtaining the IP address, the sender also shares metadata regarding the file transfer
 * (no. of files & their names) with the receiver. Finally, the onPostExecute() of the sender
 * initiates the file transfer through {@link SenderDeviceAsyncTask} on the sender and using
 * {@link ReceiverDeviceAsyncTask} on the receiver.
 */
class PeerGroupHandshakeAsyncTask extends AsyncTask<Void, Void, InetAddress> {

  private static final String TAG = "PeerGrpHndshakeAsyncTsk";
  private final String HANDSHAKE_MESSAGE = "Request Kiwix File Sharing";

  private LocalFileTransferActivity localFileTransferActivity;
  private WifiP2pInfo groupInfo;

  public PeerGroupHandshakeAsyncTask(LocalFileTransferActivity localFileTransferActivity, WifiP2pInfo groupInfo) {
    this.localFileTransferActivity = localFileTransferActivity;
    this.groupInfo = groupInfo;
  }

  @Override
  protected InetAddress doInBackground(Void... voids) {
    if(BuildConfig.DEBUG)  {
      Log.d(TAG, "Handshake in progress");
    }

    if (groupInfo.groupFormed && groupInfo.isGroupOwner && !isCancelled()) {
      try (ServerSocket serverSocket = new ServerSocket(PEER_HANDSHAKE_PORT)) {
        serverSocket.setReuseAddress(true);
        Socket server = serverSocket.accept();

        ObjectInputStream objectInputStream = new ObjectInputStream(server.getInputStream());
        Object object = objectInputStream.readObject();

        // Verify that the peer trying to communicate is a kiwix app intending to transfer files
        if (isKiwixHandshake(object) && !isCancelled()) {
          if (BuildConfig.DEBUG) {
            Log.d(TAG, "Client IP address: " + server.getInetAddress());
          }
          exchangeFileTransferMetadata(server.getOutputStream(), server.getInputStream());

          return server.getInetAddress();
        } else { // Selected device is not accepting wifi direct connections through the kiwix app
          return null;
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        return null;
      }
    } else if (groupInfo.groupFormed && !isCancelled()) { //&& !groupInfo.isGroupOwner
      try (Socket client = new Socket()) {
        client.setReuseAddress(true);
        client.connect((new InetSocketAddress(groupInfo.groupOwnerAddress.getHostAddress(),
            PEER_HANDSHAKE_PORT)), 15000);

        ObjectOutputStream objectOutputStream = new ObjectOutputStream(client.getOutputStream());
        objectOutputStream.writeObject(
            HANDSHAKE_MESSAGE); // Send message for the peer device to verify

        exchangeFileTransferMetadata(client.getOutputStream(), client.getInputStream());

        return groupInfo.groupOwnerAddress;
      } catch (Exception ex) {
        ex.printStackTrace();
        return null;
      }
    } else {
      return null;
    }
  }

  private boolean isKiwixHandshake(Object object) {
    return (object.getClass().equals(String.class) && object.equals(HANDSHAKE_MESSAGE));
  }

  private void exchangeFileTransferMetadata(OutputStream outputStream, InputStream inputStream) {
    if (localFileTransferActivity.isFileSender()) {
      try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)) {
        // Send total number of files which will be transferred
        objectOutputStream.writeObject("" + localFileTransferActivity.getTotalFilesForTransfer());

        ArrayList<Uri> fileUriList = localFileTransferActivity.getFileUriArrayList();
        for (
            Uri fileUri : fileUriList) { // Send the names of each of those files, in order
          objectOutputStream.writeObject(getFileName(fileUri));
          Log.d(TAG, "Sending " + fileUri.toString());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else { // Device is not the file sender
      try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
        // Read the number of files
        Object totalFilesObject = objectInputStream.readObject();

        if (totalFilesObject.getClass().equals(String.class)) {
          int total = Integer.parseInt((String) totalFilesObject);
          localFileTransferActivity.setTotalFilesForTransfer(total);

          ArrayList<FileItem> fileItems = new ArrayList<>();
          for (int i = 0; i < total; i++) { // Read names of each of those files, in order
            Object fileNameObject = objectInputStream.readObject();

            if (fileNameObject.getClass().equals(String.class)) {
              fileItems.add(new FileItem((String) fileNameObject, TO_BE_SENT));
            }
          }

          localFileTransferActivity.setFileItems(fileItems);
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override protected void onCancelled() {
    Log.d(TAG, "PeerGroupHandshakeAsyncTask cancelled");
  }

  @Override
  protected void onPostExecute(InetAddress inetAddress) {
    localFileTransferActivity.setClientAddress(inetAddress);
  }
}
