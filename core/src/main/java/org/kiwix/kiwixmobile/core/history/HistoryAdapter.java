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

package org.kiwix.kiwixmobile.core.history;

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
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;
import org.kiwix.kiwixmobile.core.extensions.ImageViewExtensionsKt;
import org.threeten.bp.LocalDate;
import org.threeten.bp.format.DateTimeFormatter;

class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
  private static final int TYPE_ITEM = 1;
  private final List<HistoryListItem> historyList;
  private final OnItemClickListener itemClickListener;
  private final List<HistoryListItem> deleteList;

  HistoryAdapter(List<HistoryListItem> historyList, List<HistoryListItem> deleteList,
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
      HistoryListItem.HistoryItem history = (HistoryListItem.HistoryItem) historyList.get(position);
      Item item = (Item) holder;
      item.title.setText(history.getHistoryTitle());
      if (deleteList.contains(history)) {
        item.favicon.setImageDrawable(ContextCompat.getDrawable(item.favicon.getContext(),
          R.drawable.ic_check_circle_blue_24dp));
      } else {
        ImageViewExtensionsKt.setBitmapFromString(item.favicon, history.getFavicon());
      }
      item.itemView.setOnClickListener(v -> itemClickListener.onItemClick(item.favicon, history));
      item.itemView.setOnLongClickListener(v ->
        itemClickListener.onItemLongClick(item.favicon, history));
    } else {
      String date = ((HistoryListItem.DateItem) historyList.get(position)).getDateString();
      String todaysDate, yesterdayDate;
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy");
      todaysDate = LocalDate.now().format(formatter);
      yesterdayDate = LocalDate.now().minusDays(1).format(formatter);
      if (todaysDate.contentEquals(date)) {
        ((Category) holder).date.setText(R.string.time_today);
      } else if (yesterdayDate.contentEquals(date)) {
        ((Category) holder).date.setText(R.string.time_yesterday);
      } else {
        ((Category) holder).date.setText(date);
      }
    }
  }

  @Override
  public int getItemViewType(int position) {
    return historyList.get(position) instanceof HistoryListItem.DateItem ? 0 : TYPE_ITEM;
  }

  @Override
  public int getItemCount() {
    return historyList.size();
  }

  interface OnItemClickListener {
    void onItemClick(ImageView favicon, HistoryListItem.HistoryItem history);

    boolean onItemLongClick(ImageView favicon, HistoryListItem.HistoryItem history);
  }

  class Item extends RecyclerView.ViewHolder {
    @BindView(R2.id.favicon)
    ImageView favicon;
    @BindView(R2.id.title)
    TextView title;

    Item(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  class Category extends RecyclerView.ViewHolder {
    @BindView(R2.id.header_date)
    TextView date;

    Category(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
