package org.kiwix.kiwixmobile.webserver;

import android.util.Log;
import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;
import java.util.List;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.base.BasePresenter;
import org.kiwix.kiwixmobile.data.DataSource;
import org.kiwix.kiwixmobile.di.PerActivity;
import org.kiwix.kiwixmobile.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

@PerActivity
class ZimHostPresenter extends BasePresenter<ZimHostContract.View>
  implements ZimHostContract.Presenter {

  private static final String TAG = "ZimHostPresenter";
  private final DataSource dataSource;

  @Inject ZimHostPresenter(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void loadBooks() {
    dataSource.getLanguageCategorizedBooks()
      .subscribe(new SingleObserver<List<BooksOnDiskListItem>>() {
        @Override
        public void onSubscribe(Disposable d) {
          compositeDisposable.add(d);
        }

        @Override
        public void onSuccess(List<BooksOnDiskListItem> books) {
          view.addBooks(books);
        }

        @Override
        public void onError(Throwable e) {
          Log.e(TAG, "Unable to load books", e);
        }
      });
  }
}
