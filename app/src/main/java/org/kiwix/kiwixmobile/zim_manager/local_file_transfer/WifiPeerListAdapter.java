package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.app.Activity;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pDevice;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.List;
import org.kiwix.kiwixmobile.R;

import static org.kiwix.kiwixmobile.zim_manager.local_file_transfer.WifiDirectManager.getDeviceStatus;

/**
 * Helper class, part of the local file sharing module.
 *
 * Defines the Adapter for the list of wifi peer-device items displayed.
 */
public class WifiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {

  private static final String TAG = "WifiPeerListAdapter";

  private Context context;
  private List<WifiP2pDevice> listItems;

  public WifiPeerListAdapter(@NonNull Context context, int resource,
    @NonNull List<WifiP2pDevice> objects) {
    super(context, resource, objects);
    this.context = context;
    this.listItems = objects;
  }

  @NonNull
  @Override
  public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
    View rowView = convertView;
    ViewHolder viewHolder;

    if (rowView == null) {
      LayoutInflater layoutInflater = ((Activity) context).getLayoutInflater();
      rowView = layoutInflater.inflate(R.layout.row_peer_device, parent, false);
      viewHolder = new ViewHolder(rowView);
      rowView.setTag(viewHolder);
    } else {
      viewHolder = (ViewHolder) rowView.getTag();
    }

    WifiP2pDevice device = listItems.get(position);

    if (device != null) {
      viewHolder.deviceName.setText(device.deviceName);
      Log.d(TAG, getDeviceStatus(device.status));
    }

    return rowView;
  }

  static class ViewHolder {
    @BindView(R.id.row_device_name) TextView deviceName;

    public ViewHolder(View view) {
      ButterKnife.bind(this, view);
    }
  }
}
