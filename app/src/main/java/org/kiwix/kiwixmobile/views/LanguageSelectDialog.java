package org.kiwix.kiwixmobile.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.library.LibraryAdapter;

import java.util.List;
import java.util.Map;

/**
 * Created by judebrauer on 12/6/17
 */

public class LanguageSelectDialog extends AlertDialog {
	protected LanguageSelectDialog(@NonNull Context context) {
		super(context);
	}

	public LanguageSelectDialog(@NonNull Context context, int themeResId) {
		super(context, themeResId);
	}

	public static class Builder extends AlertDialog.Builder {
		private List<LibraryAdapter.Language> languages;
		private Map<String, Integer> languageCounts;

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

		@Override
		public AlertDialog create() {
			LinearLayout view = (LinearLayout) View.inflate(getContext(), R.layout.language_selection, null);
			ListView listView = view.findViewById(R.id.language_check_view);
			int size = 0;
			try {
				size = languages.size();
			} catch (NullPointerException e) {}

			if (size == 0) {
				Toast.makeText(getContext(), getContext().getResources().getString(R.string.wait_for_load), Toast.LENGTH_LONG).show();
			}
			LanguageArrayAdapter languageArrayAdapter = new LanguageArrayAdapter(getContext(), 0, languages, languageCounts);
			listView.setAdapter(languageArrayAdapter);
			setView(view);

			return super.create();
		}
	}

	private static class LanguageArrayAdapter extends ArrayAdapter<LibraryAdapter.Language> {
		private Map<String, Integer> languageCounts;
		private Context context;

		public LanguageArrayAdapter(Context context, int textViewResourceId, List<LibraryAdapter.Language> languages, Map<String, Integer> languageCounts) {
			super(context, textViewResourceId, languages);
			this.languageCounts = languageCounts;
			this.context = context;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
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
			holder.checkBox.setOnCheckedChangeListener((compoundButton, b) -> getItem(position).active = b);

			LibraryAdapter.Language language = getItem(position);
			holder.language.setText(language.language);
			holder.languageLocalized.setText(language.languageLocalized);

			if (languageCounts != null) {
        holder.languageEntriesCount.setText(
                context.getString(R.string.language_count,
                                  languageCounts.get(language.languageCode)));
      } else {
			  holder.languageEntriesCount.setVisibility(View.GONE);
      }
			holder.checkBox.setChecked(language.active);

			return convertView;
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
}
