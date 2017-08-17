package org.kiwix.kiwixmobile.zim_manager.library_view;

import android.content.Context;
import android.util.Log;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.downloader.DownloadFragment;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.network.KiwixService;

import javax.inject.Inject;

import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

public class LibraryPresenter extends BasePresenter<LibraryViewCallback> {

  @Inject
  KiwixService kiwixService;

  @Inject
  public LibraryPresenter() {
  }

  void loadBooks() {
    getMvpView().displayScanningContent();
    kiwixService.getLibrary()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(library -> {
          getMvpView().showBooks(library.getBooks());
        }, error -> {
          String msg = error.getLocalizedMessage();
          Log.w("kiwixLibrary", "Error:" + (msg != null ? msg : "(null)"));
          getMvpView().displayNoNetworkConnection();
        });
  }

  void loadRunningDownloadsFromDb(Context context) {
    BookDao bookDao = new BookDao(KiwixDatabase.getInstance(context));
    for (LibraryNetworkEntity.Book book : bookDao.getDownloadingBooks()) {
      if (!DownloadFragment.mDownloads.containsValue(book)) {
        book.url = book.remoteUrl;
        getMvpView().downloadFile(book);
      }
    }
  }

  @Override
  public void attachView(LibraryViewCallback view) {
    super.attachView(view);
  }

}
