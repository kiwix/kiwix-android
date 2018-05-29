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
package org.kiwix.kiwixmobile.zim_manager;

import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.utils.LanguageUtils;

import java.util.List;
import java.util.Map;

/**
 * Created by judebrauer on 12/6/17
 */

public class LanguageSelectDialog extends AlertDialog {
  protected LanguageSelectDialog(@NonNull Context context) {
    super(context);
  }

  public static class Builder extends AlertDialog.Builder {
    private List<LibraryAdapter.Language> languages;
    private Map<String, Integer> languageCounts;
    private boolean singleSelect = false;
    private String selectedLanguage;
    private OnLanguageSelectedListener languageSelectedListener;

    public Builder(@NonNull Context context) {
      super(context);
    }

    public Builder(@NonNull Context context, int themeResId) {
      super(context, themeResId);
    }

    public Builder setLanguages(List<LibraryAdapter.Language> languages) {
      this.languages = languages;
      return this;
    }

    public Builder setLanguageCounts(Map<String, Integer> languageCounts) {
      this.languageCounts = languageCounts;
      return this;
    }

    public Builder setSingleSelect(boolean singleSelect) {
      this.singleSelect = singleSelect;
      return this;
    }

    // Should only be called if setSingleSelect has previously been called with a value of true
    public Builder setSelectedLanguage(String languageCode) {
      this.selectedLanguage = languageCode;
      return this;
    }

    // Should only be called if setSingleSelect has previously been called with a value of true
    public Builder setOnLanguageSelectedListener(OnLanguageSelectedListener listener) {
      languageSelectedListener = listener;
      return this;
    }

    @Override
    public AlertDialog create() {
      LinearLayout view = (LinearLayout) View
          .inflate(getContext(), R.layout.language_selection, null);
      ListView listView = view.findViewById(R.id.language_check_view);
      int size = 0;
      try {
        size = languages.size();
      } catch (NullPointerException e) {
        e.printStackTrace();
      }

      LanguageArrayAdapter languageArrayAdapter = new LanguageArrayAdapter(getContext(), 0,
          languages, languageCounts, singleSelect, selectedLanguage);
      listView.setAdapter(languageArrayAdapter);
      setView(view);

      if (languageSelectedListener != null) {
        setPositiveButton(android.R.string.ok, ((dialog, which) -> {
          languageSelectedListener.onLanguageSelected(languageArrayAdapter.getSelectedLanguage());
        }));
      }

      return super.create();
    }
  }

  private static class LanguageArrayAdapter extends ArrayAdapter<LibraryAdapter.Language> {
    private Map<String, Integer> languageCounts;
    private Context context;
    private boolean singleSelect;
    private String selectedLanguage;

    public LanguageArrayAdapter(Context context, int textViewResourceId, List<LibraryAdapter.Language> languages,
                                Map<String, Integer> languageCounts, boolean singleSelect, String selectedLanguage) {
      super(context, textViewResourceId, languages);
      this.languageCounts = languageCounts;
      this.context = context;
      this.singleSelect = singleSelect;
      this.selectedLanguage = selectedLanguage;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
      LanguageArrayAdapter.ViewHolder holder;

      if (convertView == null) {
        convertView = View.inflate(getContext(), R.layout.language_check_item, null);
        holder = new LanguageArrayAdapter.ViewHolder();
        holder.row = convertView.findViewById(R.id.language_row);
        holder.checkBox = convertView.findViewById(R.id.language_checkbox);
        holder.language = convertView.findViewById(R.id.language_name);
        holder.languageLocalized = convertView.findViewById(R.id.language_name_localized);
        holder.languageEntriesCount = convertView.findViewById(R.id.language_entries_count);
        convertView.setTag(holder);
      } else {
        holder = (LanguageArrayAdapter.ViewHolder) convertView.getTag();
      }

      // Set event listeners first, since updating the default values can trigger them.
      holder.row.setOnClickListener((view) -> holder.checkBox.toggle());
      holder.checkBox
          .setOnCheckedChangeListener((compoundButton, b) -> getItem(position).active = b);

      LibraryAdapter.Language language = getItem(position);
      holder.language.setText(language.language);
      holder.languageLocalized.setText(context.getString(R.string.language_localized,
              language.languageLocalized));
      holder.languageLocalized.setTypeface(Typeface.createFromAsset(context.getAssets(),
              LanguageUtils.getTypeface(language.languageCode)));

      if (languageCounts != null) {
        holder.languageEntriesCount.setText(context.getString(R.string.language_count,
            languageCounts.get(language.languageCode)));
      } else {
        holder.languageEntriesCount.setVisibility(View.GONE);
      }

      if (!singleSelect) {
        holder.checkBox.setChecked(language.active);
      } else {
        holder.checkBox.setClickable(false);
        holder.checkBox.setFocusable(false);

        if (getSelectedLanguage().equalsIgnoreCase(language.languageCodeISO2)) {
          holder.checkBox.setChecked(true);
        } else {
          holder.checkBox.setChecked(false);
        }

        convertView.setOnClickListener((v -> {
          setSelectedLanguage(language.languageCodeISO2);
          notifyDataSetChanged();
        }));
      }

      return convertView;
    }

    public String getSelectedLanguage() {
      return selectedLanguage;
    }

    public void setSelectedLanguage(String selectedLanguage) {
      this.selectedLanguage = selectedLanguage;
    }

    // We are using the ViewHolder pattern in order to optimize the ListView by reusing
    // Views and saving them to this mLibrary class, and not inflating the layout every time
    // we need to create a row.
    private class ViewHolder {
      ViewGroup row;
      CheckBox checkBox;
      TextView language;
      TextView languageLocalized;
      TextView languageEntriesCount;
    }
  }

  public interface OnLanguageSelectedListener {
    void onLanguageSelected(String languageCode);
  }
}
