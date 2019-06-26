package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import static android.view.View.GONE;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.DeviceListFragment.TAG;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.FileItem.TO_BE_SENT;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.filePath;

public class DeviceListFragment extends ListFragment implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

  public static String TAG = "DeviceListFragment";

  private SharedPreferenceUtil sharedPreferenceUtil;

  private View fragRootView = null;
  private List<WifiP2pDevice> peerDevices = new ArrayList<WifiP2pDevice>();
  private WifiP2pDevice userDevice;
  private WifiP2pDevice selectedPeerDevice = null;
  private InetAddress selectedPeerDeviceInetAddress;
  private WifiP2pInfo groupInfo;

  private InetAddress fileReceiverAddress;
  public static int PEER_HANDSHAKE_PORT = 8009;
  public static int FILE_TRANSFER_PORT = 8008;

  private boolean fileSender = false;
  private ArrayList<Uri> fileUriList;
  private int totalFiles = -1;
  private int totalFilesSent = 0;
  private ArrayList<FileItem> fileItems = new ArrayList<>();
  private TransferProgressFragment transferProgressFragment;

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    this.setListAdapter(new WifiPeerListAdapter(getActivity(), R.layout.row_peer_device, peerDevices));

    ImageButton editUserDeviceName = getActivity().findViewById(R.id.btn_edit_device_name);
    editUserDeviceName.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if(userDevice != null) {
          //TODO: Dialog to take input & call this method:
          //((DeviceActionListener) getActivity()).changeDeviceName("Sood");

          if(((LocalFileTransferActivity) getActivity()).isWifiP2pEnabled()) {
            FragmentTransaction fragmentTransaction = getActivity().getSupportFragmentManager().beginTransaction();
            Fragment prev = getActivity().getSupportFragmentManager().findFragmentByTag("EditDeviceNameDialog");
            if (prev != null) {
              fragmentTransaction.remove(prev); // To prevent multiple instances of the DialogFragment
            }
            fragmentTransaction.addToBackStack(null);

            EditDeviceNameDialog dialogFragment = new EditDeviceNameDialog();
            // For DialogFragments, show() handles the fragment commit and display
            dialogFragment.show(fragmentTransaction, "EditDeviceNameDialog");
          } else {
            Toast.makeText(getActivity(), "Enable WiFi P2P to change device name", Toast.LENGTH_SHORT).show();
          }
        }
      }
    });

    if(((LocalFileTransferActivity) getActivity()).isFileSender()) {
      fileSender = true;
      fileUriList = ((LocalFileTransferActivity) getActivity()).getFileURIArrayList();
      totalFiles = fileUriList.size();

      for(int i = 0; i < fileUriList.size(); i++)
        fileItems.add(new FileItem(getFileName(fileUriList.get(i)), TO_BE_SENT));

      displayTransferProgressFragment();
    }
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    fragRootView = inflater.inflate(R.layout.fragment_device_list, null);
    return fragRootView;
  }

  public WifiP2pDevice getUserDevice() { return userDevice; }

  private static String getDeviceStatus(int status) {

    Log.d(LocalFileTransferActivity.TAG, "Peer Status: " + status);
    switch (status) {
      case WifiP2pDevice.AVAILABLE : return "Available";
      case WifiP2pDevice.INVITED   : return "Invited";
      case WifiP2pDevice.CONNECTED : return "Connected";
      case WifiP2pDevice.FAILED    : return "Failed";
      case WifiP2pDevice.UNAVAILABLE:return "Unavailable";

      default: return "Unknown";
    }
  }

  @Override
  public void onListItemClick(ListView l, View v, int position, long id) {
    if(!isFileSender())
      return;

    selectedPeerDevice = (WifiP2pDevice) getListAdapter().getItem(position);
    Toast.makeText(getActivity(), selectedPeerDevice.deviceName, Toast.LENGTH_SHORT).show();

    // TODO: Set sender depending upon receiving file URI in activity opening intent
    //((LocalFileTransferActivity) getActivity()).setFileSender();
    ((DeviceActionListener) getActivity()).connect(selectedPeerDevice);
  }

  public void updateUserDevice(WifiP2pDevice device) {
    this.userDevice = device;

    if(userDevice != null) {
      TextView deviceName = fragRootView.findViewById(R.id.text_view_device_name);
      TextView deviceStatus = fragRootView.findViewById(R.id.text_view_device_status);

      if(deviceName != null)  deviceName.setText(userDevice.deviceName);
      if(deviceStatus != null)    deviceStatus.setText(getDeviceStatus(userDevice.status));
    }
  }

  @Override
  public void onPeersAvailable(WifiP2pDeviceList peers) {

    ProgressBar searchingPeersProgressBar = fragRootView.findViewById(R.id.progress_bar_searching_peers);
    searchingPeersProgressBar.setVisibility(View.GONE);
    FrameLayout frameLayoutPeerDevices = fragRootView.findViewById(R.id.frame_layout_peer_devices);
    frameLayoutPeerDevices.setVisibility(View.VISIBLE);

    peerDevices.clear();
    peerDevices.addAll(peers.getDeviceList());
    ((WifiPeerListAdapter) getListAdapter()).notifyDataSetChanged();

    if(peerDevices.size() == 0) {
      Log.d(LocalFileTransferActivity.TAG, "No devices found");
    }
  }

  public void clearPeers() {
    peerDevices.clear();
    ((WifiPeerListAdapter) getListAdapter()).notifyDataSetChanged();
  }

  public void onInitiateDiscovery() {
    ProgressBar searchingPeersProgressBar = fragRootView.findViewById(R.id.progress_bar_searching_peers);
    searchingPeersProgressBar.setVisibility(View.VISIBLE);
    FrameLayout frameLayoutPeerDevices = fragRootView.findViewById(R.id.frame_layout_peer_devices);
    frameLayoutPeerDevices.setVisibility(View.INVISIBLE);
  }

  @Override
  public void onConnectionInfoAvailable(WifiP2pInfo info) {
    groupInfo = info;

    //TODO: Show progress of file transfer process

    new PeerGroupHandshakeAsyncTask(this, groupInfo).execute();

    // TODO: Disable onclick listener (of list) for connecting to devices
  }

  public void setClientAddress(InetAddress clientAddress) {
    if(clientAddress != null) selectedPeerDeviceInetAddress = clientAddress;

    startFileTransfer();
  }

  public boolean isFileSender() {
    return fileSender;
  }

  public int getTotalFiles() {
    return totalFiles;
  }

  public void setTotalFiles(int totalFiles) {
    this.totalFiles = totalFiles;
  }

  public ArrayList<FileItem> getFileItems() {
    return fileItems;
  }

  public void setFileItems(ArrayList<FileItem> fileItems) {
    this.fileItems = fileItems;
  }

  public ArrayList<Uri> getFileUriList() {
    return fileUriList;
  }


  public void incrementTotalFilesSent() {
    this.totalFilesSent++;
  }

  public int getTotalFilesSent() {
    return totalFilesSent;
  }

  public boolean allFilesSent() {
    return (totalFilesSent == totalFiles);
  }

  public void setSharedPreferenceUtil(SharedPreferenceUtil sharedPreferenceUtil) {
    this.sharedPreferenceUtil = sharedPreferenceUtil;
  }

  public String getZimStorageRootPath() {
    return (sharedPreferenceUtil.getPrefStorage() + "/Kiwix/");
  }

  private void displayTransferProgressFragment() {
    transferProgressFragment = new TransferProgressFragment(fileItems);
    FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    fragmentTransaction.add(R.id.container_fragment_transfer_progress, transferProgressFragment)
                       .commit();
  }

  private void startFileTransfer() {

    if(groupInfo.groupFormed && !fileSender) {
      displayTransferProgressFragment();

      new FileServerAsyncTask(getActivity(), this, transferProgressFragment).execute();
      Toast.makeText(getActivity(), "File receiving device", Toast.LENGTH_SHORT).show();

    } else if(groupInfo.groupFormed) {
      {
        //Toast.makeText(getActivity(), "Sending file to "+selectedPeerDevice.deviceAddress+"\nSelf: "+userDevice.deviceAddress, Toast.LENGTH_SHORT).show();
        Log.d(LocalFileTransferActivity.TAG, "Starting file transfer");

        new AlertDialog.Builder(getActivity())
            .setMessage("Transferring file")
            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                /*Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
                serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
                serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_URI, filePath.toString());//FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID+".fileprovider", new File(filePath)).toString());

                if(groupInfo.isGroupOwner)  fileReceiverAddress = selectedPeerDeviceInetAddress;
                else                        fileReceiverAddress = groupInfo.groupOwnerAddress;

                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, fileReceiverAddress.getHostAddress());//getIPFromMac(selectedPeerDevice.deviceAddress));
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8008);
                getActivity().startService(serviceIntent);*/
                /*DeviceListFragment parent = ((DeviceListFragment) getActivity().getSupportFragmentManager().findFragmentByTag(TAG));
                if(parent != null)
                  new FileSenderAsyncTask(getContext(), parent, parent.groupInfo).execute(((LocalFileTransferActivity) getActivity()).getFileURIArrayList());*/
              }
            })
            //.setNegativeButton("No", null)
            .show();

        if(groupInfo.isGroupOwner)  fileReceiverAddress = selectedPeerDeviceInetAddress;
        else                        fileReceiverAddress = groupInfo.groupOwnerAddress;
        for(int i = 0; i < 10000000; i++);

        for(int i = 0; i < totalFiles; i++) {
          new FileSenderAsyncTask(getContext(), this, groupInfo, transferProgressFragment, i).execute(fileUriList.get(i));
          transferProgressFragment.changeStatus(i, FileItem.SENDING);
        }
      }
    }
  }

  private class WifiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

    private List<WifiP2pDevice> listItems;

    public WifiPeerListAdapter(@NonNull Context context, int resource, List<WifiP2pDevice> objects) {
      super(context, resource, objects);
      this.listItems = objects;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

      View rowView = convertView;
      if(rowView == null) {
        LayoutInflater layoutInflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        rowView = layoutInflater.inflate(R.layout.row_peer_device, parent, false);
      }

      WifiP2pDevice device = listItems.get(position);

      if(device != null) {
        TextView deviceName = rowView.findViewById(R.id.row_device_name);
        TextView deviceStatus = rowView.findViewById(R.id.row_device_status);

        if(deviceName != null)  deviceName.setText(device.deviceName);
        if(deviceStatus != null)    deviceStatus.setText(getDeviceStatus(device.status));
      }

      return rowView;
    }
  }

  public interface DeviceActionListener {

    void changeDeviceName(String deviceNewName);

    /*void showDetails(WifiP2pDevice device);*/

    void cancelDisconnect();

    void connect(WifiP2pDevice peerDevice);

    void disconnect();
  }

  public static class EditDeviceNameDialog extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

      builder.setView(R.layout.dialog_edit_device_name)
          .setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
              EditText changeDeviceName = EditDeviceNameDialog.this.getDialog().findViewById(R.id.edit_text_change_device_name);
              String deviceNewName = changeDeviceName.getText().toString();
              //Toast.makeText(getActivity(), "Changing name to: " + deviceNewName.getText().toString(), Toast.LENGTH_SHORT).show();

              if(deviceNewName != null && !deviceNewName.equals("")) {
                ((DeviceActionListener) getActivity()).changeDeviceName(deviceNewName);
              } else {
                Toast.makeText(getActivity(), "Error: Empty name field", Toast.LENGTH_SHORT).show();
              }

              //dialog.cancel();
            }
          })
          .setNeutralButton("Dismiss", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
          });

      return builder.create();
    }
  }

  public static class PeerGroupHandshakeAsyncTask extends AsyncTask<Void, Void, InetAddress> {

    private DeviceListFragment deviceListFragment;
    private WifiP2pInfo groupInfo;

    public PeerGroupHandshakeAsyncTask(DeviceListFragment deviceListFragment, WifiP2pInfo groupInfo) {
      this.deviceListFragment = deviceListFragment;
      this.groupInfo = groupInfo;
    }

    @Override
    protected InetAddress doInBackground(Void... voids) {
      if(groupInfo.groupFormed && groupInfo.isGroupOwner) {

        try {
          ServerSocket serverSocket = new ServerSocket(PEER_HANDSHAKE_PORT);
          serverSocket.setReuseAddress(true);
          Socket client = serverSocket.accept();
          ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
          Object object = objectInputStream.readObject();
          if (object.getClass().equals(String.class) && ((String) object).equals("Request Kiwix File Sharing")) {
            Log.d(TAG, "Client IP address: "+ client.getInetAddress());
            //selectedPeerDeviceInetAddress = client.getInetAddress();

            if(deviceListFragment.isFileSender()) {
              ObjectOutputStream objectOutputStream = new ObjectOutputStream(client.getOutputStream());
              objectOutputStream.writeObject(""+deviceListFragment.getTotalFiles());
              Log.d(TAG, "### SENDER DEVICE");
              ArrayList<Uri> fileUriList = deviceListFragment.getFileUriList();
              for (int i = 0; i < fileUriList.size(); i++) {
                objectOutputStream.writeObject(getFileName(fileUriList.get(i)));
              }

            } else {
              Object totalFilesObject = objectInputStream.readObject();
              if(totalFilesObject.getClass().equals(String.class)) {
                int total = Integer.parseInt((String) totalFilesObject);
                deviceListFragment.setTotalFiles(total);
                Log.d(TAG, "### Transfer of "+ total + " files");
                ArrayList<FileItem> fileItems = new ArrayList<>();
                for (int i = 0; i < total; i++) {
                  Object fileNameObject = objectInputStream.readObject();
                  if(fileNameObject.getClass().equals(String.class)){
                    Log.d(TAG, "File - "+ (String) fileNameObject);
                    fileItems.add(new FileItem((String) fileNameObject, TO_BE_SENT));
                  }
                }
                deviceListFragment.setFileItems(fileItems);
              }
            }
          }
          return client.getInetAddress();

        } catch (Exception e) {
          //Log.d(TAG, e.getMessage());
          e.printStackTrace();
          return null;
        }

      } else if(groupInfo.groupFormed && !groupInfo.isGroupOwner) {

        String hostAddress = groupInfo.groupOwnerAddress.getHostAddress();
        try {
          Socket socket = new Socket();
          socket.setReuseAddress(true);
          socket.connect((new InetSocketAddress(hostAddress, PEER_HANDSHAKE_PORT)), 15000);
          OutputStream os = socket.getOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(os);
          oos.writeObject(new String("Request Kiwix File Sharing"));
          if(deviceListFragment.isFileSender()) {
            oos.writeObject(""+deviceListFragment.getTotalFiles());
            Log.d(TAG, "### SENDER DEVICE");
            ArrayList<Uri> fileUriList = deviceListFragment.getFileUriList();
            for (int i = 0; i < fileUriList.size(); i++) {
              oos.writeObject(getFileName(fileUriList.get(i)));
            }
          } else {
            ObjectInputStream objectInputStream = new ObjectInputStream(socket.getInputStream());
            Object totalFilesObject = objectInputStream.readObject();
            if(totalFilesObject.getClass().equals(String.class)) {
              int total = Integer.parseInt((String) totalFilesObject);
              deviceListFragment.setTotalFiles(total);
              Log.d(TAG, "### Transfer of "+ total + " files");
              ArrayList<FileItem> fileItems = new ArrayList<>();
              for (int i = 0; i < total; i++) {
                Object fileNameObject = objectInputStream.readObject();
                if(fileNameObject.getClass().equals(String.class)) {
                  Log.d(TAG, "File - "+ (String) fileNameObject);
                  fileItems.add(new FileItem((String) fileNameObject, TO_BE_SENT));
                }
              }
              deviceListFragment.setFileItems(fileItems);
            }
          }
          oos.close();
          os.close();
          socket.close();

          return null;
        } catch (Exception e) {
          //Log.d(TAG, e.getMessage());
          e.printStackTrace();
          return null;
        }

            /*Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, Environment.getExternalStorageDirectory() + "/MainPage.txt");
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, groupInfo.groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8008);
            getActivity().startService(serviceIntent);*/
      }

      return null;
    }

    @Override
    protected void onPostExecute(InetAddress inetAddress) {
      (deviceListFragment).setClientAddress(inetAddress);
    }
  }

  public static String getFileName(Uri fileUri) {
    String fileName = "";
    String fileUriString = fileUri.toString();

    // Searches for location of last slash in the file path
    for(int loc = fileUriString.length()-1; loc >= 0; loc--) {
      if(fileUriString.charAt(loc) == '/') {
        return fileUriString.substring(loc+1);
      }
    }

    return null;
  }

  public InetAddress getFileReceiverAddress() {
    return fileReceiverAddress;
  }

  public static class FileSenderAsyncTask extends AsyncTask<Uri, Void, String> {

    private Context context;
    private DeviceListFragment deviceListFragment;
    private WifiP2pInfo groupInfo;
    private TransferProgressFragment transferProgressFragment;
    private ArrayList<FileItem> fileItems;
    private int fileItemIndex;

    public FileSenderAsyncTask(Context context, DeviceListFragment deviceListFragment, WifiP2pInfo groupInfo, TransferProgressFragment transferProgressFragment, int fileItemIndex) {
      this.context = context;
      this.deviceListFragment = deviceListFragment;
      this.groupInfo = groupInfo;
      this.transferProgressFragment = transferProgressFragment;
      this.fileItems = deviceListFragment.getFileItems();
      this.fileItemIndex = fileItemIndex;
    }

    @Override
    protected void onPreExecute() {
      //fileItemIndex = deviceListFragment.getTotalFilesSent();

      //TODO: Remove runnable for onPreExecute, onPostExecute, onProgressUpdate
      deviceListFragment.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          transferProgressFragment.changeStatus(fileItemIndex, FileItem.SENDING);
        }
      });

    }

    @Override
    protected String doInBackground(Uri... fileUris) {

      Uri fileUri = fileUris[0];

      Socket socket = new Socket();
      try {
        Log.d(LocalFileTransferActivity.TAG, "Opening client socket - ");
        socket.bind(null);
        for(int i = 0; i < 10000000; i++);
        String hostAddress = deviceListFragment.getFileReceiverAddress().getHostAddress();
        socket.connect((new InetSocketAddress(hostAddress, FILE_TRANSFER_PORT)), 15000);

        Log.d(LocalFileTransferActivity.TAG, "Client socket - " + socket.isConnected());
        OutputStream stream = socket.getOutputStream();
        Log.d(LocalFileTransferActivity.TAG, "OutputStream found");
        ContentResolver cr = context.getContentResolver();
        Log.d(LocalFileTransferActivity.TAG, "ContentResolver obtained");
        InputStream is = null;
        try {
          is = cr.openInputStream(fileUri);
          Log.d(LocalFileTransferActivity.TAG, "Opened InputStream");
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

        return "";
      }
    }

    @Override
    protected void onPostExecute(String s) {
      //
      deviceListFragment.incrementTotalFilesSent();

      deviceListFragment.getActivity().runOnUiThread(new Runnable() {
        @Override
        public void run() {
          transferProgressFragment.changeStatus(fileItemIndex, FileItem.SENT);
        }
      });

      if(deviceListFragment.allFilesSent()) {
        Toast.makeText(context, "All files transferred", Toast.LENGTH_SHORT).show();
        deviceListFragment.getActivity().finish();
      }
    }
  }

  public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

    private Context context;
    private DeviceListFragment deviceListFragment;
    private TransferProgressFragment transferProgressFragment;
    private int fileItemIndex;
    //private View statusView

    public FileServerAsyncTask(Context context, DeviceListFragment deviceListFragment, TransferProgressFragment transferProgressFragment) {
      this.context = context;
      this.deviceListFragment = deviceListFragment;
      this.transferProgressFragment = transferProgressFragment;
    }

    @Override
    protected String doInBackground(Void... voids) {
      try {
        ServerSocket serverSocket = new ServerSocket(FILE_TRANSFER_PORT);
        Log.d(LocalFileTransferActivity.TAG, "Server: Socket opened at " + FILE_TRANSFER_PORT);

        final String KIWIX_ROOT = deviceListFragment.getZimStorageRootPath();

        int totalFileCount = deviceListFragment.getTotalFiles();
        for(int currentFile = 1; currentFile <= totalFileCount; currentFile++) {
          Socket client = serverSocket.accept();
          Log.d(LocalFileTransferActivity.TAG, "Server: Client connected");
          fileItemIndex = currentFile-1;
          deviceListFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              transferProgressFragment.changeStatus(fileItemIndex, FileItem.SENDING);
            }
          });

          ArrayList<FileItem> fileItems = deviceListFragment.getFileItems();
          String incomingFileName = fileItems.get(currentFile-1).getFileName();

          // TODO:? File selector, file not exists,
          final File clientNoteFileLocation = new File(KIWIX_ROOT + incomingFileName);
          File dirs = new File(clientNoteFileLocation.getParent());
          if(!dirs.exists()) {
            Log.d(LocalFileTransferActivity.TAG, "Parent creation result: "+dirs.mkdirs());
          } else {
            Log.d(LocalFileTransferActivity.TAG, "Parent directories exist");
          }

          Log.d(LocalFileTransferActivity.TAG, "File creation: "+clientNoteFileLocation.createNewFile());

          Log.d(LocalFileTransferActivity.TAG, "Copying files");
          InputStream inputStream = client.getInputStream();
          copyFile(inputStream, new FileOutputStream(clientNoteFileLocation));

          deviceListFragment.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
              transferProgressFragment.changeStatus(fileItemIndex, FileItem.SENT);
            }
          });

          deviceListFragment.incrementTotalFilesSent();
        }
        serverSocket.close();

        return "";

      } catch (IOException e) {
        Log.e(LocalFileTransferActivity.TAG, e.getMessage());
        return null;
      }
    }

    @Override
    protected void onPostExecute(String s) {
      super.onPostExecute(s);
      Toast.makeText(context, "File transfer complete", Toast.LENGTH_LONG).show();
      Log.d(LocalFileTransferActivity.TAG, "File transfer complete");
      ((LocalFileTransferActivity) deviceListFragment.getActivity()).disconnect();

      /*File recvFile = new File(filePath);
      Uri fileUri = FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID+".fileprovider",recvFile);
      Intent intent = new Intent();
      intent.setAction(Intent.ACTION_VIEW);
      intent.setDataAndType(fileUri, "text/plain");
      intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      context.startActivity(intent);*/
    }
  }

  public static boolean copyFile(InputStream inputStream, OutputStream out) {
    byte buf[] = new byte[1024];
    int len;
    try {
      Log.d(LocalFileTransferActivity.TAG, "Copying to OutputStream...");
      while ((len = inputStream.read(buf)) != -1) {
        out.write(buf, 0, len);

      }
      out.close();
      inputStream.close();
      Log.d(LocalFileTransferActivity.TAG, "Both streams closed");
    } catch (IOException e) {
      Log.d(LocalFileTransferActivity.TAG, e.toString());
      return false;
    }
    return true;
  }

  /*public interface UpdateProgressCallback {
    public void changeStatus(int itemIndex, short status);
  }*/
}
