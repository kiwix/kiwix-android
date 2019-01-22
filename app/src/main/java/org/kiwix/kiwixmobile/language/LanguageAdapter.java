package org.kiwix.kiwixmobile.language;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.models.Language;

import java.util.ArrayList;
import java.util.Collections;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

class LanguageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
  private static final int TYPE_HEADER = 0;
  private static final int TYPE_ITEM = 1;
  private final ArrayList<Language> languages;
  private final ArrayList<Language> selectedLanguages = new ArrayList<>();
  private final ArrayList<Language> unselectedLanguages = new ArrayList<>();

  LanguageAdapter(ArrayList<Language> languages) {
    this.languages = languages;
    categorizeLanguages();
  }

  void categorizeLanguages() {
    selectedLanguages.clear();
    unselectedLanguages.clear();
    for (Language language : languages) {
      if (language.active != null && language.active.equals(true)) {
        selectedLanguages.add(language);
      } else {
        language.active = false;
        unselectedLanguages.add(language);
      }
    }
    Collections.sort(selectedLanguages);
    Collections.sort(unselectedLanguages);
  }

  @NonNull
  @Override
  public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    if (viewType == TYPE_ITEM) {
      View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language, parent, false);
      return new ViewHolder(view);
    }
    return new Header(LayoutInflater.from(parent.getContext()).inflate(R.layout.header_date, parent, false));
  }

  @Override
  public void onBindViewHolder(@NonNull RecyclerView.ViewHolder item, int position) {
    if (item instanceof Header) {
      Header header = (Header) item;
      if (position == 0) {
        header.header.setText(R.string.your_languages);
      } else {
        header.header.setText(R.string.other_languages);
      }
      return;
    }
    Language language;
    if (position - 1 < selectedLanguages.size()) {
      language = selectedLanguages.get(position - 1);
    } else {
      language = unselectedLanguages.get(position - selectedLanguages.size() - 2);
    }
    ViewHolder holder = (ViewHolder) item;
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
      if (language.active) {
        unselectedLanguages.remove(language);
        selectedLanguages.add(language);
      } else {
        unselectedLanguages.add(language);
        selectedLanguages.remove(language);
      }
      Collections.sort(selectedLanguages);
      Collections.sort(unselectedLanguages);
      notifyDataSetChanged();
    };
    holder.itemView.setOnClickListener(v -> {
      holder.checkBox.toggle();
      onClickListener.onClick(v);
    });
    holder.checkBox.setOnClickListener(onClickListener);
  }

  @Override
  public int getItemViewType(int position) {
    if (position == 0 || position == selectedLanguages.size() + 1) {
      return TYPE_HEADER;
    }
    return TYPE_ITEM;
  }

  @Override
  public int getItemCount() {
    return selectedLanguages.size() + unselectedLanguages.size() + 2;
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

  class Header extends RecyclerView.ViewHolder {
    @BindView(R.id.header_date)
    TextView header;

    Header(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
