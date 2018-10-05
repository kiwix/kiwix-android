package org.kiwix.kiwixmobile.bookmark;

import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.data.local.entity.Bookmark;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.kiwix.kiwixmobile.utils.ImageUtils.createBitmapFromEncodedString;

class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.Item> {
  private final List<Bookmark> bookmarkList;
  private final OnItemClickListener itemClickListener;
  private final List<Bookmark> deleteList;

  BookmarksAdapter(List<Bookmark> bookmarkList, List<Bookmark> deleteList, OnItemClickListener itemClickListener) {
    this.bookmarkList = bookmarkList;
    this.deleteList = deleteList;
    this.itemClickListener = itemClickListener;
  }

  @NonNull
  @Override
  public Item onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark_history, parent, false);
    return new Item(view);

  }

  @Override
  public void onBindViewHolder(@NonNull Item holder, int position) {
    Bookmark bookmark = bookmarkList.get(position);
    holder.title.setText(bookmark.getBookmarkTitle());
    if (deleteList.contains(bookmark)) {
      holder.favicon.setImageDrawable(ContextCompat.getDrawable(holder.favicon.getContext(),
          R.drawable.ic_check_circle_blue_24dp));
    } else {
      holder.favicon.setImageBitmap(createBitmapFromEncodedString(holder.favicon.getContext(),
          bookmark.getFavicon()));
    }
    holder.itemView.setOnClickListener(v -> itemClickListener.onItemClick(holder.favicon, bookmark));
    holder.itemView.setOnLongClickListener(v ->
        itemClickListener.onItemLongClick(holder.favicon, bookmark));
  }

  @Override
  public int getItemCount() {
    return bookmarkList.size();
  }

  interface OnItemClickListener {
    void onItemClick(ImageView favicon, Bookmark bookmark);

    boolean onItemLongClick(ImageView favicon, Bookmark bookmark);
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
