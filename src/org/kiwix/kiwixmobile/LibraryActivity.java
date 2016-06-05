package org.kiwix.kiwixmobile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ListView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.List;
import org.kiwix.kiwixmobile.downloader.DownloadIntent;
import org.kiwix.kiwixmobile.downloader.DownloadService;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.network.KiwixService;
import rx.android.schedulers.AndroidSchedulers;

public class LibraryActivity extends AppCompatActivity {

  @BindView(R.id.toolbar) Toolbar toolbar;
  @BindView(R.id.library_list) ListView libraryList;

  private KiwixService kiwixService;
  private List<LibraryNetworkEntity.Book> books;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_library);
    ButterKnife.bind(this);
    setSupportActionBar(toolbar);
    kiwixService = ((KiwixApplication) getApplication()).getKiwixService();
    kiwixService.getLibrary()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(library -> {
          books = library.getBooks();
          libraryList.setAdapter(new LibraryAdapter(LibraryActivity.this, books));
        });

    libraryList.setOnItemClickListener(
        (parent, view, position, id) -> {
          Intent service = new Intent(this, DownloadService.class);
          service.putExtra(DownloadIntent.DOWNLOAD_URL_PARAMETER, books.get(position).getUrl());
          startService(service);
        });
  }
}