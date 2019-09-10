/*
 * Copyright (c) 2019 Kiwix
 * All rights reserved.
 */

package org.kiwix.kiwixmobile.bookmark;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.List;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.extensions.ImageViewExtensionsKt;

class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.Item> {
  private final List<BookmarkItem> bookmarkList;
  private final OnItemClickListener itemClickListener;
  private final List<BookmarkItem> deleteList;

  BookmarksAdapter(List<BookmarkItem> bookmarkList, List<BookmarkItem> deleteList,
      OnItemClickListener itemClickListener) {
    this.bookmarkList = bookmarkList;
    this.deleteList = deleteList;
    this.itemClickListener = itemClickListener;
  }

  @NonNull
  @Override
  public Item onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_bookmark_history, parent, false);
    return new Item(view);
  }

  @Override
  public void onBindViewHolder(@NonNull Item holder, int position) {
    BookmarkItem bookmark = bookmarkList.get(position);
    holder.title.setText(bookmark.getBookmarkTitle());
    if (deleteList.contains(bookmark)) {
      holder.favicon.setImageDrawable(ContextCompat.getDrawable(holder.favicon.getContext(),
          R.drawable.ic_check_circle_blue_24dp));
    } else {
      ImageViewExtensionsKt.setBitmapFromString(holder.favicon, bookmark.getFavicon());
    }
    holder.itemView.setOnClickListener(
        v -> itemClickListener.onItemClick(holder.favicon, bookmark));
    holder.itemView.setOnLongClickListener(v ->
        itemClickListener.onItemLongClick(holder.favicon, bookmark));
  }

  @Override
  public int getItemCount() {
    return bookmarkList.size();
  }

  interface OnItemClickListener {
    void onItemClick(ImageView favicon, BookmarkItem bookmark);

    boolean onItemLongClick(ImageView favicon, BookmarkItem bookmark);
  }

  class Item extends RecyclerView.ViewHolder {
    @BindView(R.id.favicon)
    ImageView favicon;
    @BindView(R.id.title)
    TextView title;

    Item(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
