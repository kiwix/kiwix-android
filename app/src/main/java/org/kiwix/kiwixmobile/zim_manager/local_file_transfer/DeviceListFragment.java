package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.app.Dialog;
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
import androidx.fragment.app.FragmentTransaction;
import androidx.fragment.app.ListFragment;

import org.kiwix.kiwixmobile.BuildConfig;
import org.kiwix.kiwixmobile.R;

import java.io.BufferedReader;
import java.io.File;
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
import java.util.ArrayList;
import java.util.List;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.DeviceListFragment.TAG;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.filePath;

public class DeviceListFragment extends ListFragment implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener, PeerGroupHandshakeAsyncTask.ClientAddressReady {

  public static String TAG = "DeviceListFragment";

  private View fragRootView = null;
  private List<WifiP2pDevice> peerDevices = new ArrayList<WifiP2pDevice>();
  private WifiP2pDevice userDevice;
  private WifiP2pDevice selectedPeerDevice = null;
  private InetAddress selectedPeerDeviceInetAddress;
  private WifiP2pInfo groupInfo;

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

        /*if(groupInfo.groupFormed && groupInfo.isGroupOwner) {

            try {
                ServerSocket serverSocket = new ServerSocket(8008);
                serverSocket.setReuseAddress(true);
                Socket client = serverSocket.accept();
                ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
                Object object = objectInputStream.readObject();
                if (object.getClass().equals(String.class) && ((String) object).equals("Request Kiwix File Sharing")) {
                    Log.d(TAG, "Client IP address: "+ client.getInetAddress());
                    selectedPeerDeviceInetAddress = client.getInetAddress();
                }
            } catch (Exception e) {
                //Log.d(TAG, e.getMessage());
                e.printStackTrace();
            }

        } else if(groupInfo.groupFormed && !groupInfo.isGroupOwner) {

            try {
                Socket socket = new Socket();
                socket.setReuseAddress(true);
                socket.connect((new InetSocketAddress(groupInfo.groupOwnerAddress.getHostAddress(), 8008)), 15000);
                OutputStream os = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(os);
                oos.writeObject(new String("Request Kiwix File Sharing"));
                oos.close();
                os.close();
                socket.close();
            } catch (Exception e) {
                //Log.d(TAG, e.getMessage());
                e.printStackTrace();
            }

            *//*Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, Environment.getExternalStorageDirectory() + "/MainPage.txt");
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, groupInfo.groupOwnerAddress.getHostAddress());
            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8008);
            getActivity().startService(serviceIntent);*//*
        }

        if(groupInfo.groupFormed && ((MainActivity) getActivity()).isFileSender()) {
            Toast.makeText(getActivity(), "Sending file to "+selectedPeerDevice.deviceAddress+"\nSelf: "+userDevice.deviceAddress, Toast.LENGTH_SHORT).show();
            Log.d(MainActivity.TAG, "Starting file transfer");

            new AlertDialog.Builder(getActivity())
                    .setMessage("Transfer file?")
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
                            serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
                            serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_PATH, FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID+".fileprovider", new File(filePath)).toString());

                            InetAddress fileReceiverAddress;
                            if(groupInfo.isGroupOwner)  fileReceiverAddress = selectedPeerDeviceInetAddress;
                            else                        fileReceiverAddress = groupInfo.groupOwnerAddress;

                            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, fileReceiverAddress.getHostAddress());//getIPFromMac(selectedPeerDevice.deviceAddress));
                            serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8008);
                            getActivity().startService(serviceIntent);
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();


        } else {
            Toast.makeText(getActivity(), "File receiving device", Toast.LENGTH_SHORT).show();
            new FileServerAsyncTask(getActivity()).execute();
        }*/

    // TODO: Disable onclick listener (of list) for connecting to devices
  }

  @Override
  public void setClientAddress(InetAddress clientAddress) {
    if(clientAddress != null) selectedPeerDeviceInetAddress = clientAddress;

    startFileTransfer();
  }

  private void startFileTransfer() {
    if(groupInfo.groupFormed && !((LocalFileTransferActivity) getActivity()).isFileSender()) {
      Toast.makeText(getActivity(), "File receiving device", Toast.LENGTH_SHORT).show();
      new FileServerAsyncTask(getActivity()).execute();

    } else if(groupInfo.groupFormed) {
      {
        Toast.makeText(getActivity(), "Sending file to "+selectedPeerDevice.deviceAddress+"\nSelf: "+userDevice.deviceAddress, Toast.LENGTH_SHORT).show();
        Log.d(LocalFileTransferActivity.TAG, "Starting file transfer");

        new AlertDialog.Builder(getActivity())
            .setMessage("Transfer file?")
            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialog, int which) {
                Intent serviceIntent = new Intent(getActivity(), FileTransferService.class);
                serviceIntent.setAction(FileTransferService.ACTION_SEND_FILE);
                serviceIntent.putExtra(FileTransferService.EXTRAS_FILE_URI, filePath.toString());//FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID+".fileprovider", new File(filePath)).toString());

                InetAddress fileReceiverAddress;
                if(groupInfo.isGroupOwner)  fileReceiverAddress = selectedPeerDeviceInetAddress;
                else                        fileReceiverAddress = groupInfo.groupOwnerAddress;

                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_ADDRESS, fileReceiverAddress.getHostAddress());//getIPFromMac(selectedPeerDevice.deviceAddress));
                serviceIntent.putExtra(FileTransferService.EXTRAS_GROUP_OWNER_PORT, 8008);
                getActivity().startService(serviceIntent);
              }
            })
            .setNegativeButton("No", null)
            .show();
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

  //private class EditNameDialog e
  /*private void requestEnableLocationServices() {

   *//*Toast.makeText(MainActivity.this, "Enable location to allow detection of peers", Toast.LENGTH_LONG);
        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1);*//*
   *//*new AlertDialog.Builder(this)
                .setMessage("Enable location to allow detection of peers")
                .setPositiveButton("Open Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        paramDialogInterface.cancel();
                        startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();*//*

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment prev = getSupportFragmentManager().findFragmentByTag("LocationDialog");
        if(prev != null) {
            fragmentTransaction.remove(prev); // To prevent multiple instances of the DialogFragment
        }
        fragmentTransaction.addToBackStack(null);

        RequestEnableLocationServicesDialog dialogFragment = new RequestEnableLocationServicesDialog();
        // For DialogFragments, show() handles the fragment commit and display
        dialogFragment.show(fragmentTransaction, "LocationDialog");

    }

    public static class RequestEnableLocationServicesDialog extends DialogFragment {
        @NonNull
        @Override
        public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage("Enable location to allow detection of peers")
                    .setPositiveButton("Open Location Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                            paramDialogInterface.cancel();
                            startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), 1);
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            //TODO: Close activity

                        }
                    });

            return builder.create();
        }
    }*/

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

  public static class FileServerAsyncTask extends AsyncTask<Void, Void, String> {

    private Context context;
    //private View statusView

    public FileServerAsyncTask(Context context) {
      this.context = context;
    }

    @Override
    protected String doInBackground(Void... voids) {
      try {
        ServerSocket serverSocket = new ServerSocket(8008);
        Log.d(LocalFileTransferActivity.TAG, "Server: Socket opened at 8008");
        Socket client = serverSocket.accept();
        Log.d(LocalFileTransferActivity.TAG, "Server: Client connected");

        // File selector, file not exists,
        //TODO: Change to appropriate file-path
        final File clientNoteFileLocation = new File(Environment.getExternalStorageDirectory() + "/KiwixWifi/temp.zim");
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
        serverSocket.close();
        return clientNoteFileLocation.getAbsolutePath();

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
      while ((len = inputStream.read(buf)) != -1) {
        out.write(buf, 0, len);

      }
      out.close();
      inputStream.close();
    } catch (IOException e) {
      Log.d(LocalFileTransferActivity.TAG, e.toString());
      return false;
    }
    return true;
  }
}

class PeerGroupHandshakeAsyncTask extends AsyncTask<Void, Void, InetAddress> {

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
        ServerSocket serverSocket = new ServerSocket(8009);
        serverSocket.setReuseAddress(true);
        Socket client = serverSocket.accept();
        ObjectInputStream objectInputStream = new ObjectInputStream(client.getInputStream());
        Object object = objectInputStream.readObject();
        if (object.getClass().equals(String.class) && ((String) object).equals("Request Kiwix File Sharing")) {
          Log.d(TAG, "Client IP address: "+ client.getInetAddress());
          //selectedPeerDeviceInetAddress = client.getInetAddress();
        }
        return client.getInetAddress();
      } catch (Exception e) {
        //Log.d(TAG, e.getMessage());
        e.printStackTrace();
        return null;
      }

    } else if(groupInfo.groupFormed && !groupInfo.isGroupOwner) {

      try {
        Socket socket = new Socket();
        socket.setReuseAddress(true);
        for(int i = 0; i < 100000000; i++);
        socket.connect((new InetSocketAddress(groupInfo.groupOwnerAddress.getHostAddress(), 8009)), 15000);
        OutputStream os = socket.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(new String("Request Kiwix File Sharing"));
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

  public interface ClientAddressReady {
    void setClientAddress(InetAddress clientAddress);
  }
}
