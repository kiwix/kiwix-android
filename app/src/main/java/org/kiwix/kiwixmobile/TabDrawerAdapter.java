/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kiwix.kiwixmobile.views.web.KiwixWebView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class TabDrawerAdapter extends RecyclerView.Adapter<TabDrawerAdapter.ViewHolder> {
  private TabClickListener listener;
  private List<KiwixWebView> webViews;

  private int selectedPosition = 0;

  public TabDrawerAdapter(List<KiwixWebView> webViews) {
    this.webViews = webViews;
  }

  @Override
  public TabDrawerAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.tabs_list, parent, false);
    return new ViewHolder(v);
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    KiwixWebView webView = webViews.get(position);
    holder.title.setText(webView.getTitle());
    holder.exit.setOnClickListener(v -> listener.onCloseTab(v, position));
    holder.itemView.setOnClickListener(v -> {
      listener.onSelectTab(v, position);
      selectedPosition = holder.getAdapterPosition();
      notifyDataSetChanged();
      holder.itemView.setActivated(true);
    });
    holder.itemView.setActivated(holder.getAdapterPosition() == selectedPosition);
  }

  @Override
  public int getItemCount() {
    return webViews.size();
  }

  public void setSelected(int position) {
    this.selectedPosition = position;
  }

  public int getSelectedPosition() {
    return selectedPosition;
  }

  public void setTabClickListener(TabClickListener listener) {
    this.listener = listener;
  }

  public interface TabClickListener {
    void onSelectTab(View view, int position);

    void onCloseTab(View view, int position);
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {
    public @BindView(R.id.titleText) TextView title;
    public @BindView(R.id.exitButton) ImageView exit;

    public ViewHolder(View v) {
      super(v);
      ButterKnife.bind(this, v);
    }
  }
}