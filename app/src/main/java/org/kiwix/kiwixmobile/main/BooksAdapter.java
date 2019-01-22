package org.kiwix.kiwixmobile.main;

import android.content.Context;
import android.graphics.ColorMatrixColorFilter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.kiwix.kiwixmobile.KiwixApplication;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

import static org.kiwix.kiwixmobile.library.LibraryAdapter.createGbString;

/**
 * Adapter class for book-items list displayed in the home page WebView
 * Use LibraryAdapter for list items in the downloads library
 */
public class BooksAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  private static int TYPE_ITEM = 1;
  private final SharedPreferenceUtil sharedPreferenceUtil = new SharedPreferenceUtil(KiwixApplication.getInstance());
  private List<LibraryNetworkEntity.Book> books;
  private OnItemClickListener itemClickListener;

  BooksAdapter(List<LibraryNetworkEntity.Book> books, OnItemClickListener itemClickListener) {
    this.books = books;
    this.itemClickListener = itemClickListener;
  }

  private static String getArticleCountString(Context context, String articleCount) {
    if (articleCount == null || articleCount.equals("")) {
      return "";
    }
    try {
      int size = Integer.parseInt(articleCount);
      if (size <= 0) {
        return "";
      }

      final String[] units = new String[]{"", "K", "M", "B", "T"};
      int conversion = (int) (Math.log10(size) / 3);
      return context.getString(R.string.articleCount, new DecimalFormat("#,##0.#")
          .format(size / Math.pow(1000, conversion)) + units[conversion]);
    } catch (NumberFormatException e) {
      Log.d("BooksAdapter", e.toString());
      return "";
    }
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_ITEM) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_book, parent, false);
      return new Item(view);
    } else {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.header_language, parent, false);
      return new Category(view);
    }
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
    if (holder instanceof Item) {
      LibraryNetworkEntity.Book book = books.get(position);
      Item item = (Item) holder;
      item.title.setText(book.getTitle());
      item.date.setText(book.getDate());
      item.description.setText(book.getDescription());
      item.size.setText(createGbString(book.getSize()));
      item.articleCount.setText(getArticleCountString(item.articleCount.getContext(), book.getArticleCount()));
      item.icon.setImageBitmap(LibraryAdapter.createBitmapFromEncodedString(book.getFavicon(), item.icon.getContext()));

      if (sharedPreferenceUtil.nightMode()) {  ////Fix Bug #905: Launch activity (night-mode) inverts colour of icons
        item.icon.getDrawable().mutate().setColorFilter(new ColorMatrixColorFilter(KiwixWebView.getNightModeColors()));
      }

      item.itemView.setOnClickListener(v -> itemClickListener.openFile(book.file.getPath()));
      if (book.file.getPath().contains("nopic")) {
        item.pictureLabel.setVisibility(View.GONE);
        item.videoLabel.setVisibility(View.GONE);
      } else if (book.file.getPath().contains("novid")) {
        item.videoLabel.setVisibility(View.GONE);
      }
    } else {
      Locale locale = new Locale(books.get(position + 1).getLanguage());
      ((Category) holder).language.setText(locale.getDisplayLanguage(locale));
    }
  }

  @Override
  public int getItemViewType(int position) {
    return books.get(position) == null ? 0 : TYPE_ITEM;
  }

  @Override
  public int getItemCount() {
    return books.size();
  }

  interface OnItemClickListener {
    void openFile(String url);
  }

  class Item extends RecyclerView.ViewHolder {
    @BindView(R.id.item_book_icon)
    ImageView icon;
    @BindView(R.id.item_book_title)
    TextView title;
    @BindView(R.id.item_book_description)
    TextView description;
    @BindView(R.id.item_book_date)
    TextView date;
    @BindView(R.id.item_book_size)
    TextView size;
    @BindView(R.id.item_book_article_count)
    TextView articleCount;
    @BindView(R.id.item_book_label_picture)
    TextView pictureLabel;
    @BindView(R.id.item_book_label_video)
    TextView videoLabel;

    Item(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  class Category extends RecyclerView.ViewHolder {
    @BindView(R.id.header_language)
    TextView language;

    public Category(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
