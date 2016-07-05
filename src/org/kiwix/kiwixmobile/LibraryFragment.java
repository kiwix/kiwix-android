package org.kiwix.kiwixmobile;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.List;
import org.kiwix.kiwixmobile.downloader.DownloadIntent;
import org.kiwix.kiwixmobile.downloader.DownloadService;
import org.kiwix.kiwixmobile.library.LibraryAdapter;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.network.KiwixService;
import org.kiwix.kiwixmobile.utils.ShortcutUtils;

import rx.android.schedulers.AndroidSchedulers;

import static org.kiwix.kiwixmobile.utils.ShortcutUtils.stringsGetter;

public class LibraryFragment extends Fragment {

  @BindView(R.id.library_list) ListView libraryList;
  @BindView(R.id.progressBar) ProgressBar progressBar;
  @BindView(R.id.progressbar_message) TextView progressText;

  private KiwixService kiwixService;

  private LinearLayout llLayout;

  private List<LibraryNetworkEntity.Book> books;

  private boolean active;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentActivity faActivity  = (FragmentActivity)    super.getActivity();
        // Replace LinearLayout by the type of the root element of the layout you're trying to load
        llLayout    = (LinearLayout)    inflater.inflate(R.layout.activity_library, container, false);
        // Of course you will want to faActivity and llLayout in the class and not this method to access them in the rest of
        // the class, just initialize them here

        // Don't use this method, it's handled by inflater.inflate() above :
        // setContentView(R.layout.activity_layout);

      ButterKnife.bind(this, llLayout);
      kiwixService = ((KiwixApplication) super.getActivity().getApplication()).getKiwixService();
      kiwixService.getLibrary()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe(library -> {
            books = library.getBooks();
            if (active) {
              libraryList.setAdapter(new LibraryAdapter(super.getActivity(), books));
              progressBar.setVisibility(View.INVISIBLE);
              progressText.setVisibility(View.INVISIBLE);
            }
          });

      libraryList.setOnItemClickListener(
          (parent, view, position, id) -> {
            Toast.makeText(super.getActivity(), stringsGetter(R.string.download_started_library, super.getActivity()), Toast.LENGTH_LONG).show();
            Intent service = new Intent(super.getActivity(), DownloadService.class);
            service.putExtra(DownloadIntent.DOWNLOAD_URL_PARAMETER, books.get(position).getUrl());
            super.getActivity().startService(service);
          });
        active = true;
        // The FragmentActivity doesn't contain the layout directly so we must use our instance of     LinearLayout :
        //llLayout.findViewById(R.id.someGuiElement);
        // Instead of :
        // findViewById(R.id.someGuiElement);
        return llLayout; // We must return the loaded Layout
    }
  @Override
  public void onDestroyView() {
    super.onDestroyView();
    active = false;
  }

}