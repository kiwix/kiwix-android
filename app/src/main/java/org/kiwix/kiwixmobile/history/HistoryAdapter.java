package org.kiwix.kiwixmobile.history;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.data.local.entity.History;
import org.kiwix.kiwixmobile.library.LibraryAdapter;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
  private static int TYPE_ITEM = 1;
  private List<History> historyList = new ArrayList<>();
  private OnItemClickListener itemClickListener;

  HistoryAdapter(OnItemClickListener itemClickListener) {
    this.itemClickListener = itemClickListener;
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_ITEM) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
      return new Item(view);
    } else {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_date, parent, false);
      return new Category(view);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof Item) {
      History history = historyList.get(position);
      Item item = (Item) holder;
      item.title.setText(history.getHistoryTitle());
      item.favicon.setImageBitmap(LibraryAdapter.createBitmapFromEncodedString(history.getFavicon(),
          item.favicon.getContext()));
      item.itemView.setOnClickListener(v -> itemClickListener
          .openHistoryUrl(history.getHistoryUrl(), history.getZimFile()));
    } else {
      DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault());
      ((Category) holder).date.setText(dateFormat.format(new Date(historyList.get(position + 1)
          .getTimeStamp())));
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

  public void setHistoryList(List<History> historyList) {
    this.historyList = historyList;
    notifyDataSetChanged();
  }

  interface OnItemClickListener {
    void openHistoryUrl(String historyUrl, String zimFile);
  }

  class Item extends RecyclerView.ViewHolder {
    @BindView(R.id.item_history_favicon)
    ImageView favicon;
    @BindView(R.id.item_history_title)
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
