package org.kiwix.kiwixmobile.help;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.utils.LanguageUtils;

import java.util.HashMap;

import butterknife.BindView;
import butterknife.OnClick;

import static org.kiwix.kiwixmobile.utils.Constants.CONTACT_EMAIL_ADDRESS;

public class HelpActivity extends BaseActivity {

  private final HashMap<String, String> titleDescriptionMap = new HashMap<>();

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
    HelpAdapter helpAdapter = new HelpAdapter(titleDescriptionMap, sharedPreferenceUtil.nightMode());
    recyclerView.setAdapter(helpAdapter);
  }

  @OnClick({R.id.activity_help_feedback_text_view, R.id.activity_help_feedback_image_view})
  void sendFeedback() {
    Intent intent = new Intent(Intent.ACTION_SEND);
    intent.setType("plain/text");
    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{CONTACT_EMAIL_ADDRESS});
    intent.putExtra(Intent.EXTRA_SUBJECT, "Feedback in " +
        LanguageUtils.getCurrentLocale(this).getDisplayLanguage());
    startActivity(Intent.createChooser(intent, ""));
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
