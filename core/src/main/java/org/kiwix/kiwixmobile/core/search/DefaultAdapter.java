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

package org.kiwix.kiwixmobile.core.search;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import javax.annotation.Nonnull;

public class DefaultAdapter extends RecyclerView.Adapter<DefaultAdapter.ViewHolder> {

  interface ClickListener {
    void onItemClick(String text);

    void onItemLongClick(String text);
  }

  private final ClickListener clickListener;
  private Context context;
  List<String> searchList;

  public DefaultAdapter(Context context, ClickListener clickListener) {
    this.context = context;
    this.clickListener = clickListener;
  }

  @NonNull @Override
  public DefaultAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = (View) LayoutInflater.from(parent.getContext())
      .inflate(android.R.layout.simple_list_item_1, parent, false);
    ViewHolder holder = new ViewHolder(view);
    return holder;
  }

  @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    holder.recentSearch.setText(
      Html.fromHtml(getItem(position)).toString());///need to get rid of this error
  }

  @Override public int getItemCount() {
    return searchList.size();
  }

  @NonNull public String getItem(int position) {
    return searchList.get(position);
  }

  public class ViewHolder extends RecyclerView.ViewHolder {
    TextView recentSearch;

    public ViewHolder(@NonNull View itemView) {
      super(itemView);
      recentSearch = itemView.findViewById(android.R.id.text1);

      itemView.setOnClickListener(v -> {
        if (context instanceof SearchActivity) {
          String title = recentSearch.getText().toString();
          ((SearchActivity) context).searchPresenter.saveSearch(title);
        }
      });
      itemView.setOnLongClickListener(v -> {
        if (context instanceof SearchActivity) {
          String searched = recentSearch.getText().toString();
          ((SearchActivity) context).deleteSpecificSearchDialog(searched);
          return true;
        }
        return false;
      });
    }
  }
}

