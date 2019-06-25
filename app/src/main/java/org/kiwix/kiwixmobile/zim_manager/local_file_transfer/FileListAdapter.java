package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.kiwix.kiwixmobile.R;

import java.util.ArrayList;

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
    String name = fileItems.get(position).getFileName();
    holder.fileItemView.setText(name);
  }

  @Override
  public int getItemCount() {
    return fileItems.size();
  }

  class FileViewHolder extends RecyclerView.ViewHolder {
    public final TextView fileItemView;
    final FileListAdapter fileListAdapter;

    public FileViewHolder(View itemView, FileListAdapter fileListAdapter) {
      super(itemView);
      this.fileItemView = itemView.findViewById(R.id.text_view_file_item_name);
      this.fileListAdapter = fileListAdapter;
    }
  }
}
