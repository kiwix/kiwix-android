package org.kiwix.kiwixmobile.zim_manager.fileselect_view;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.kiwix.kiwixmobile.BasePresenter;
import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.utils.files.FileSearch;
import org.kiwix.kiwixmobile.zim_manager.ZimManageActivity;
import org.kiwix.kiwixmobile.zim_manager.library_view.LibraryFragment;

import java.util.ArrayList;

import javax.inject.Inject;

import static org.kiwix.kiwixmobile.R.id.progressBar;
import static org.kiwix.kiwixmobile.downloader.DownloadService.bookDao;

/**
 * Created by EladKeyshawn on 06/04/2017.
 */

public class ZimFileSelectPresenter extends BasePresenter<ZimFileSelectViewCallback> {

  @Inject
  public ZimFileSelectPresenter() {
  }

  @Override
  public void attachView(ZimFileSelectViewCallback mvpView) {
    super.attachView(mvpView);
  }

  @Override
  public void detachView() {
    super.detachView();
  }

  public void loadLocalZimFileFromDb(Context context){
    BookDao bookDao = new BookDao(KiwixDatabase.getInstance(context));
    ArrayList<LibraryNetworkEntity.Book> books = bookDao.getBooks();
    getMvpView().showFiles(books);
  }


}
