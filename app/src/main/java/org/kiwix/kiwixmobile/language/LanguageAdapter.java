package org.kiwix.kiwixmobile.language;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.models.Language;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {
  private final ArrayList<Language> languages;

  LanguageAdapter(ArrayList<Language> languages) {
    this.languages = languages;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Language language = languages.get(position);
    holder.languageName.setText(language.language);
    holder.languageLocalizedName.setText(language.languageLocalized);
    holder.booksCount.setText(holder.booksCount.getContext().getResources()
        .getQuantityString(R.plurals.books_count, language.booksCount, language.booksCount));
    if (language.active == null) {
      language.active = false;
    }
    holder.checkBox.setChecked(language.active);
    View.OnClickListener onClickListener = v -> {
      language.active = holder.checkBox.isChecked();
      notifyItemChanged(position);
    };
    holder.itemView.setOnClickListener(v -> {
      holder.checkBox.toggle();
      onClickListener.onClick(v);
    });
    holder.checkBox.setOnClickListener(onClickListener);
  }

  @Override
  public int getItemCount() {
    return languages.size();
  }

  class ViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.item_language_name)
    TextView languageName;
    @BindView(R.id.item_language_localized_name)
    TextView languageLocalizedName;
    @BindView(R.id.item_language_books_count)
    TextView booksCount;
    @BindView(R.id.item_language_checkbox)
    CheckBox checkBox;

    ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
