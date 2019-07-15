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

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import java.io.File;
import org.kiwix.kiwixmobile.R;

public class StorageSelectDialog extends DialogFragment implements ListView.OnItemClickListener {

  // Activities/Fragments can create instances of a StorageSelectDialog and bind a listener to get its result

  public static final String STORAGE_DIALOG_THEME = "THEME";

  public static final String STORAGE_DIALOG_INTERNAL = "INTERNAL";

  public static final String STORAGE_DIALOG_EXTERNAL = "EXTERNAL";

  private StorageSelectArrayAdapter mAdapter;

  private OnSelectListener mOnSelectListener;
  private String mTitle;

  private String mInternal = "Internal";

  private String mExternal = "External";

  @Override
  public void onCreate(Bundle savedInstanceState) {
    if (getArguments() != null) {
      // Set string values
      mInternal = getArguments().getString(STORAGE_DIALOG_INTERNAL, mInternal);
      mExternal = getArguments().getString(STORAGE_DIALOG_EXTERNAL, mExternal);
      // Set the theme to a supplied value
      if (getArguments().containsKey(STORAGE_DIALOG_THEME)) {
        setStyle(DialogFragment.STYLE_NORMAL, getArguments().getInt(STORAGE_DIALOG_THEME));
      }
    }
    super.onCreate(savedInstanceState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View rootView = inflater.inflate(R.layout.storage_select_dialog, container, false);
    TextView title = rootView.findViewById(R.id.title);
    title.setText(mTitle);
    ListView listView = rootView.findViewById(R.id.device_list);
    mAdapter = new StorageSelectArrayAdapter(getActivity(), 0,
        StorageDeviceUtils.getStorageDevices(getActivity(), true), mInternal, mExternal);
    listView.setAdapter(mAdapter);
    listView.setOnItemClickListener(this);
    Button button = rootView.findViewById(R.id.button);
    final EditText editText = rootView.findViewById(R.id.editText);
    button.setOnClickListener(view -> {
      if (editText.getText().length() != 0) {
        String path = editText.getText().toString();
        if (new File(path).exists()) {
          mAdapter.add(new StorageDevice(path, false));
        }
      }
    });
    return rootView;
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    if (mOnSelectListener != null) {
      mOnSelectListener.selectionCallback(mAdapter.getItem(position));
    }
    dismiss();
  }

  public void setOnSelectListener(OnSelectListener selectListener) {
    mOnSelectListener = selectListener;
  }

  public interface OnSelectListener {
    void selectionCallback(StorageDevice s);
  }

  @Override
  public void show(FragmentManager fm, String text) {
    mTitle = text;
    super.show(fm, text);
  }
}
