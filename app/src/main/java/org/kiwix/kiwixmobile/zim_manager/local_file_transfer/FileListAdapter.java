package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.kiwix.kiwixmobile.R;

import java.util.ArrayList;

/**
 * Helper class, part of the local file sharing module.
 *
 * Defines the Adapter for the list of file-items displayed in {@link TransferProgressFragment}
 * */
public class FileListAdapter extends RecyclerView.Adapter<FileListAdapter.FileViewHolder> {
  private final ArrayList<FileItem> fileItems;
  private LayoutInflater layoutInflater;

  public FileListAdapter(Context context, ArrayList<FileItem> fileItems) {
    this.layoutInflater = LayoutInflater.from(context);
    this.fileItems = fileItems;
  }

  @NonNull
  @Override
  public FileListAdapter.FileViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View itemView = layoutInflater.inflate(R.layout.item_transfer_list, parent, false);
    return new FileViewHolder(itemView, this);
  }

  @Override
  public void onBindViewHolder(@NonNull FileListAdapter.FileViewHolder holder, int position) {
    FileItem fileItem = fileItems.get(position);

    String name = fileItem.getFileName();
    holder.fileName.setText(name);

    if(fileItem.getFileStatus() == FileItem.SENDING) {
      holder.statusImage.setVisibility(View.GONE);
      holder.progressBar.setVisibility(View.VISIBLE);

    } else if(fileItem.getFileStatus() != FileItem.TO_BE_SENT){ // Icon for TO_BE_SENT is assigned by default in the item layout
      holder.progressBar.setVisibility(View.GONE);

      switch (fileItem.getFileStatus()) {
        case FileItem.SENT :  holder.statusImage.setImageResource(R.drawable.ic_baseline_check_24px);
                              break;
        case FileItem.ERROR:  holder.statusImage.setImageResource(R.drawable.ic_baseline_error_24px);
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
    public final TextView fileName;
    public final ImageView statusImage;
    public final ProgressBar progressBar;
    final FileListAdapter fileListAdapter;

    public FileViewHolder(View itemView, FileListAdapter fileListAdapter) {
      super(itemView);
      this.fileName = itemView.findViewById(R.id.text_view_file_item_name);
      this.statusImage = itemView.findViewById(R.id.image_view_file_transferred);
      this.progressBar = itemView.findViewById(R.id.progress_bar_transferring_file);
      this.fileListAdapter = fileListAdapter;
    }
  }
}
