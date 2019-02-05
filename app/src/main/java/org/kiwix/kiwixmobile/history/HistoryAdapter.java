package org.kiwix.kiwixmobile.history;

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
import org.kiwix.kiwixmobile.data.local.entity.History;

import static org.kiwix.kiwixmobile.library.LibraryAdapter.createBitmapFromEncodedString;

class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
  private static final int TYPE_ITEM = 1;
  private final List<History> historyList;
  private final OnItemClickListener itemClickListener;
  private final List<History> deleteList;

  HistoryAdapter(List<History> historyList, List<History> deleteList,
      OnItemClickListener itemClickListener) {
    this.historyList = historyList;
    this.deleteList = deleteList;
    this.itemClickListener = itemClickListener;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_ITEM) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.item_bookmark_history, parent, false);
      return new Item(view);
    } else {
      View view =
          LayoutInflater.from(parent.getContext()).inflate(R.layout.header_date, parent, false);
      return new Category(view);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof Item) {
      History history = historyList.get(position);
      Item item = (Item) holder;
      item.title.setText(history.getHistoryTitle());
      if (deleteList.contains(history)) {
        item.favicon.setImageDrawable(ContextCompat.getDrawable(item.favicon.getContext(),
            R.drawable.ic_check_circle_blue_24dp));
      } else {
        item.favicon.setImageBitmap(createBitmapFromEncodedString(history.getFavicon(),
            item.favicon.getContext()));
      }
      item.itemView.setOnClickListener(v -> itemClickListener.onItemClick(item.favicon, history));
      item.itemView.setOnLongClickListener(v ->
          itemClickListener.onItemLongClick(item.favicon, history));
    } else {
      ((Category) holder).date.setText(historyList.get(position + 1).getDate());
    }
  }

  @Override
  public int getItemViewType(int position) {
    return historyList.get(position) == null ? 0 : TYPE_ITEM;
  }

  @Override
  public int getItemCount() {
    return historyList.size();
  }

  interface OnItemClickListener {
    void onItemClick(ImageView favicon, History history);

    boolean onItemLongClick(ImageView favicon, History history);
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

  class Category extends RecyclerView.ViewHolder {
    @BindView(R.id.header_date)
    TextView date;

    Category(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
