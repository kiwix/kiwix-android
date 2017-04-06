package org.kiwix.kiwixmobile.zim_manager.library_view;

import org.kiwix.kiwixmobile.BasePresenter;
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
          getMvpView().displayNoNetworkConnection();
        });
  }

  @Override
  public void attachView(LibraryViewCallback view) {
    super.attachView(view);
  }

  @Override
  public void detachView() {
    super.detachView();
  }


}
