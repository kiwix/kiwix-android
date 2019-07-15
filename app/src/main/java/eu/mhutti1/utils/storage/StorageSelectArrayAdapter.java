/*
 * Copyright 2016 Isaac Hutt <mhutti1@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package eu.mhutti1.utils.storage;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import org.kiwix.kiwixmobile.R;

class StorageSelectArrayAdapter extends ArrayAdapter<StorageDevice> {

  private final String mInternal;

  private final String mExternal;

  public StorageSelectArrayAdapter(Context context, int resource, ArrayList<StorageDevice> devices,
      String internal, String external) {
    super(context, resource, devices);
    mInternal = internal;
    mExternal = external;
  }

  @SuppressLint("SetTextI18n") @Override
  public View getView(int position, View convertView, ViewGroup parent) {

    ViewHolder holder;
    if (convertView == null) {
      convertView = View.inflate(getContext(), R.layout.device_item, null);
      holder = new ViewHolder();
      holder.fileName = convertView.findViewById(R.id.file_name);
      holder.fileSize = convertView.findViewById(R.id.file_size);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }
    StorageDevice device = getItem(position);
    if (device.isInternal()) {
      holder.fileName.setText(mInternal);
    } else {
      holder.fileName.setText(mExternal);
    }
    holder.fileSize.setText(device.getSize() + " / " + device.getTotalSize());

    return convertView;
  }

  class ViewHolder {
    TextView fileName;
    TextView fileSize;
  }
}
