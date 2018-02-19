package org.kiwix.kiwixmobile.modules.zim_manager.fileselect_view;

import android.content.Context;

import org.kiwix.kiwixmobile.common.base.BasePresenter;
import org.kiwix.kiwixmobile.common.data.database.BookDao;
import org.kiwix.kiwixmobile.common.data.database.KiwixDatabase;
import org.kiwix.kiwixmobile.modules.library.entity.LibraryNetworkEntity;
import org.kiwix.kiwixmobile.modules.zim_manager.fileselect_view.contract.ZimFileSelectViewCallback;

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

  public void loadLocalZimFileFromDb(Context context){
    BookDao bookDao = new BookDao(KiwixDatabase.getInstance(context));
    ArrayList<LibraryNetworkEntity.Book> books = bookDao.getBooks();
    getMvpView().showFiles(books);
  }

}
