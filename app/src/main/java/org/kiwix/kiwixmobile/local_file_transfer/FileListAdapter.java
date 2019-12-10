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

package org.kiwix.kiwixmobile.local_file_transfer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.ArrayList;
import org.kiwix.kiwixmobile.R;

import static org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.ERROR;
import static org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.SENDING;
import static org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.SENT;
import static org.kiwix.kiwixmobile.local_file_transfer.FileItem.FileStatus.TO_BE_SENT;

/**
 * Helper class, part of the local file sharing module.
 *
 * Defines the Adapter for the list of file-items displayed in {TransferProgressFragment}
 */
public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {
  private final ArrayList<FileItem> fileItems;

  public FileListAdapter(@NonNull ArrayList<FileItem> fileItems) {
    this.fileItems = fileItems;
  }

  @NonNull
  @Override
  public FileListAdapter.FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent,
    int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
      .inflate(R.layout.item_transfer_list, parent, false);
    return new FileViewHolder(itemView, this);
  }

  @Override
  public void onBindViewHolder(@NonNull FileListAdapter.FileViewHolder holder, int position) {
    FileItem fileItem = fileItems.get(position);

    String name = fileItem.getFileName();
    holder.fileName.setText(name);

    if (fileItem.getFileStatus() == SENDING) {
      holder.statusImage.setVisibility(View.GONE);
      holder.progressBar.setVisibility(View.VISIBLE);
    } else if (fileItem.getFileStatus() != TO_BE_SENT) {
      // Icon for TO_BE_SENT is assigned by default in the item layout
      holder.progressBar.setVisibility(View.GONE);

      switch (fileItem.getFileStatus()) {
        case SENT:
          holder.statusImage.setImageResource(R.drawable.ic_baseline_check_24px);
          break;
        case ERROR:
          holder.statusImage.setImageResource(R.drawable.ic_baseline_error_24px);
          break;
        case TO_BE_SENT:
        case SENDING:
        default:
          break;
      }

      holder.statusImage.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public int getItemCount() {
    return fileItems.size();
  }

  class FileViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.text_view_file_item_name) TextView fileName;
    @BindView(R.id.image_view_file_transferred) ImageView statusImage;
    @BindView(R.id.progress_bar_transferring_file) ProgressBar progressBar;
    final FileListAdapter fileListAdapter;

    public FileViewHolder(View itemView, FileListAdapter fileListAdapter) {
      super(itemView);
      this.fileListAdapter = fileListAdapter;
      ButterKnife.bind(this, itemView);
    }
  }
}
