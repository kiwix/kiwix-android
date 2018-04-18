package org.kiwix.kiwixmobile.zim_manager;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.utils.LanguageUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * An adapter that provides a language list to be shown when language filter option is selected in
 * {@link ZimManageActivity} menu.
 */

class LanguageSelectAdapter extends RecyclerView.Adapter<LanguageSelectAdapter.ViewHolder> {

  private List<LibraryAdapter.Language> data;
  private Map<String, Integer> languageBooksCount;

  LanguageSelectAdapter(List<LibraryAdapter.Language> data, Map<String, Integer> languageBooksCount) {
    this.data = data;
    this.languageBooksCount = languageBooksCount;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    return new ViewHolder(LayoutInflater.from(parent.getContext())
        .inflate(R.layout.language_check_item, parent, false));
  }

  @Override
  public void onBindViewHolder(ViewHolder holder, int position) {
    LibraryAdapter.Language language = data.get(position);
    Context context = holder.row.getContext();
    holder.row.setOnClickListener(view -> holder.checkBox.toggle());
    holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> language.active = isChecked);
    holder.checkBox.setChecked(language.active);
    holder.language.setText(language.language);
    holder.languageLocalized.setText(context.getString(R.string.language_localized,
        language.languageLocalized));
    holder.languageLocalized.setTypeface(Typeface.createFromAsset(context.getAssets(),
        LanguageUtils.getTypeface(language.languageCode)));
    holder.languageEntriesCount.setText(context.getString(R.string.language_count,
        languageBooksCount.get(language.languageCode)));
  }

  @Override
  public int getItemCount() {
    return data.size();
  }

  List<LibraryAdapter.Language> getFilteredLanguages() {
    List<LibraryAdapter.Language> list = new ArrayList<>();
    for (LibraryAdapter.Language language : data) {
      if (language.active) list.add(language);
    }
    return list;
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.language_row)
    ViewGroup row;
    @BindView(R.id.language_checkbox)
    CheckBox checkBox;
    @BindView(R.id.language_name)
    TextView language;
    @BindView(R.id.language_name_localized)
    TextView languageLocalized;
    @BindView(R.id.language_entries_count)
    TextView languageEntriesCount;

    ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
