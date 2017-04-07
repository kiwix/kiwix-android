package org.kiwix.kiwixmobile.zim_manager.fileselect_view;

import android.content.Context;

import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.database.BookDao;
import org.kiwix.kiwixmobile.database.KiwixDatabase;
import org.kiwix.kiwixmobile.library.entity.LibraryNetworkEntity;

import java.util.ArrayList;

import javax.inject.Inject;

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
