package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import org.kiwix.kiwixmobile.R;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.REQUEST_ENABLE_WIFI_P2P;
import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.LocalFileTransferActivity.showToast;

public class RequestEnableWifiP2pServicesDialog extends DialogFragment {

  public static final String TAG = "WifiP2pDialog";

  @NonNull
  @Override
  public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    builder.setMessage(R.string.request_enable_wifi)
        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface paramDialogInterface, int paramInt) {
            startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS),
                REQUEST_ENABLE_WIFI_P2P);
          }
        })
        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            showToast(getActivity(), R.string.discovery_needs_wifi, Toast.LENGTH_SHORT);
          }
        });

    return builder.create();
  }
}
