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

package org.kiwix.kiwixmobile.library;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.library.contract.ILibraryItemClickListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LibraryDividerRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

  @BindView(R.id.divider_text) TextView title;
  private final ILibraryItemClickListener libraryItemClickListener;

  void setupDagger() {
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }

  public LibraryDividerRecyclerViewHolder(View view, ILibraryItemClickListener libraryItemClickListener) {
    super(view);
    this.libraryItemClickListener = libraryItemClickListener;
    setupDagger();
    ButterKnife.bind(this, view);
    view.setOnClickListener(this);
  }

  public void bind(LibraryRecyclerViewAdapter.ListItem item) {
    setDividerDataToViews((String) item.data);
  }

  @Override
  public void onClick(View view) {
    libraryItemClickListener.onLibraryItemClick(getAdapterPosition());
  }

  //Utility methods

  private void setDividerDataToViews(String dividerText) {
    title.setText(dividerText);
  }
}
