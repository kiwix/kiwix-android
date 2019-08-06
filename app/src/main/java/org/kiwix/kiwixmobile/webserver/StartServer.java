package org.kiwix.kiwixmobile.webserver;

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.ZimFileSelectFragment;

public class StartServer extends AppCompatActivity implements
    ZimFileSelectFragment.OnHostActionButtonClickedListener {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_start_server);
    setUpToolbar();
    FragmentManager fragmentManager = getSupportFragmentManager();
    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
    ZimFileSelectFragment fragment = new ZimFileSelectFragment();
    fragmentTransaction.add(R.id.frameLayoutServer, fragment);
    fragmentTransaction.commit();
  }

  private void setUpToolbar() {
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setTitle(getString(R.string.menu_host_books));
    getSupportActionBar().setHomeButtonEnabled(true);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    toolbar.setNavigationOnClickListener(v -> onBackPressed());
  }

  @Override public void onHostActionButtonClicked() {
    Log.v("DANG", "Action button clicked");
    Toast.makeText(this, "Host action button has been clicked", Toast.LENGTH_LONG).show();
  }
}
