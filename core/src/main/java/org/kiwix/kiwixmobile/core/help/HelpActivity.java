/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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
 *
 */

package org.kiwix.kiwixmobile.core.help;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.OnClick;
import java.util.HashMap;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;
import org.kiwix.kiwixmobile.core.base.BaseActivity;
import org.kiwix.kiwixmobile.core.di.components.CoreComponent;
import org.kiwix.kiwixmobile.core.utils.LanguageUtils;

import static org.kiwix.kiwixmobile.core.utils.Constants.CONTACT_EMAIL_ADDRESS;

public class HelpActivity extends BaseActivity {

  private final HashMap<String, String> titleDescriptionMap = new HashMap<>();

  @BindView(R2.id.toolbar)
  Toolbar toolbar;
  @BindView(R2.id.activity_help_recycler_view)
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

    recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    recyclerView.setAdapter(new HelpAdapter(titleDescriptionMap));
  }

  @OnClick({ R2.id.activity_help_feedback_text_view, R2.id.activity_help_feedback_image_view })
  void sendFeedback() {
    Intent intent = new Intent(Intent.ACTION_SENDTO);
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

  @Override protected void injection(CoreComponent coreComponent) {
    coreComponent.inject(this);
  }
}
