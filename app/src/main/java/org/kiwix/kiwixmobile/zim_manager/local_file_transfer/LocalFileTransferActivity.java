package org.kiwix.kiwixmobile.zim_manager.local_file_transfer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import org.kiwix.kiwixmobile.R;

import java.util.ArrayList;

public class LocalFileTransferActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_local_file_transfer);

    TextView fileUriListView = findViewById(R.id.text_view_file_uris);

    Intent filesIntent = getIntent();
    ArrayList<Uri> fileURIArrayList = filesIntent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);

    String uriList = "Selected File URIs:\n\n";
    if(fileURIArrayList != null && fileURIArrayList.size() > 0) {
      for(int i = 0; i < fileURIArrayList.size(); i++) {
        uriList += fileURIArrayList.get(i) + "\n\n";
      }
    }

    fileUriListView.setText(uriList);

  }
}
