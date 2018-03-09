/*
 * Copyright 2013  Rashiq Ahmad <rashiq.z@gmail.com>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU  General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.kiwix.kiwixmobile.library;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.library.contract.ILibraryItemClickListener;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity.Book;
import org.kiwix.kiwixmobile.utils.BookUtils;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;

import static org.kiwix.kiwixmobile.library.LibraryRecyclerViewAdapter.createBitmapFromEncodedString;
import static org.kiwix.kiwixmobile.library.LibraryRecyclerViewAdapter.createGbString;
import static org.kiwix.kiwixmobile.utils.NetworkUtils.parseURL;

public class LibraryBookRecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

  @BindView(R.id.title) TextView title;
  @BindView(R.id.description) TextView description;
  @BindView(R.id.language) TextView language;
  @BindView(R.id.creator) TextView creator;
  @BindView(R.id.publisher) TextView publisher;
  @BindView(R.id.date) TextView date;
  @BindView(R.id.size) TextView size;
  @BindView(R.id.fileName) TextView fileName;
  @BindView(R.id.favicon) ImageView favicon;
  @Inject BookUtils bookUtils;
  private final ILibraryItemClickListener libraryItemClickListener;

  void setupDagger() {
    KiwixApplication.getInstance().getApplicationComponent().inject(this);
  }

  public LibraryBookRecyclerViewHolder(View view, ILibraryItemClickListener libraryItemClickListener) {
    super(view);
    this.libraryItemClickListener = libraryItemClickListener;
    setupDagger();
    ButterKnife.bind(this, view);
    view.setOnClickListener(this);
  }

  public void bind(LibraryRecyclerViewAdapter.ListItem item) {
    setBookDataToViews((Book) item.data);
  }

  @Override
  public void onClick(View view) {
    libraryItemClickListener.onLibraryItemClick(getAdapterPosition());
  }

  // Utility methods


  private void setBookDataToViews(Book book) {
    title.setText(book.getTitle());
    description.setText(book.getDescription());
    language.setText(bookUtils.getLanguage(book.getLanguage()));
    creator.setText(book.getCreator());
    publisher.setText(book.getPublisher());
    date.setText(book.getDate());
    size.setText(createGbString(book.getSize()));
    fileName.setText(parseURL(itemView.getContext(), book.getUrl()));
    favicon.setImageBitmap(createBitmapFromEncodedString(book.getFavicon(), itemView.getContext()));

    hideUnknownBookDataViews(book);
  }

  // Check if no value is empty. Set the view to View.GONE, if it is.
  private void hideUnknownBookDataViews(Book book) {

    if (book.getTitle() == null || book.getTitle().isEmpty()) {
      title.setVisibility(View.GONE);
    }

    if (book.getDescription() == null || book.getDescription().isEmpty()) {
      description.setVisibility(View.GONE);
    }

    if (book.getCreator() == null || book.getCreator().isEmpty()) {
      creator.setVisibility(View.GONE);
    }

    if (book.getPublisher() == null || book.getPublisher().isEmpty()) {
      publisher.setVisibility(View.GONE);
    }

    if (book.getDate() == null || book.getDate().isEmpty()) {
      date.setVisibility(View.GONE);
    }
    if (book.getSize() == null || book.getSize().isEmpty()) {
      size.setVisibility(View.GONE);
    }
  }
}
