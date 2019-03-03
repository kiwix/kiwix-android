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

package org.kiwix.kiwixmobile.help;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.OnClick;
import java.util.HashMap;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.utils.LanguageUtils;

import static org.kiwix.kiwixmobile.utils.Constants.CONTACT_EMAIL_ADDRESS;

public class HelpActivity extends BaseActivity {

  private final HashMap<String, String> titleDescriptionMap = new HashMap<>();
  private static final String LIST_STATE_KEY = "recycler_list_state";
  private LinearLayoutManager layoutManager;
  private Parcelable listState;
  @BindView(R.id.activity_help_toolbar)
  Toolbar toolbar;
  @BindView(R.id.activity_help_recycler_view)
  RecyclerView recyclerView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_help);
    setSupportActionBar(toolbar);
    toolbar.setNavigationOnClickListener(v -> onBackPressed());
    if (getSupportActionBar() != null) {
      getSupportActionBar().setDisplayHomeAsUpEnabled(true);
      getSupportActionBar().setTitle(R.string.menu_help);
    }

    populateMap(R.string.help_2, R.array.description_help_2);
    populateMap(R.string.help_5, R.array.description_help_5);
    populateMap(R.string.help_12, R.array.description_help_12);

    recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    HelpAdapter helpAdapter =
        new HelpAdapter(titleDescriptionMap, sharedPreferenceUtil.nightMode());
    recyclerView.setAdapter(helpAdapter);
    layoutManager = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (listState != null) {
      layoutManager.onRestoreInstanceState(listState);
    }
  }

  protected void onSaveInstanceState(Bundle state) {
    super.onSaveInstanceState(state);
    listState = layoutManager.onSaveInstanceState();
    state.putParcelable(LIST_STATE_KEY, listState);
  }

  protected void onRestoreInstanceState(Bundle state) {
    super.onRestoreInstanceState(state);
    if (state != null) {
      listState = state.getParcelable(LIST_STATE_KEY);
    }
  }

  @OnClick({ R.id.activity_help_feedback_text_view, R.id.activity_help_feedback_image_view })
  void sendFeedback() {
    Intent intent = new Intent(Intent.ACTION_SENDTO);
    intent.setType("plain/text");
    String uriText = "mailto:" + Uri.encode(CONTACT_EMAIL_ADDRESS) +
        "?subject=" + Uri.encode(
        "Feedback in " + LanguageUtils.getCurrentLocale(this).getDisplayLanguage());
    Uri uri = Uri.parse(uriText);
    intent.setData(uri);
    startActivity(Intent.createChooser(intent, "Send Feedback via Email"));
  }

  private void populateMap(int title, int descriptionArray) {
    StringBuilder description = new StringBuilder();
    for (String s : getResources().getStringArray(descriptionArray)) {
      description.append(s);
      description.append(System.getProperty("line.separator"));
    }
    titleDescriptionMap.put(getString(title), description.toString());
  }
}
