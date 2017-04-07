package org.kiwix.kiwixmobile.base;

/**
 * Created by EladKeyshawn on 05/04/2017.
 */

public class BasePresenter<T extends ViewCallback> implements Presenter<T> {

  private T mMvpView;

  @Override
  public void attachView(T mvpView) {
    this.mMvpView = mvpView;
  }

  @Override
  public void detachView() {
    mMvpView = null;
  }

  public boolean isViewAttached() {
    return mMvpView != null;
  }

  public T getMvpView() {
    return mMvpView;
  }


  public void checkViewAttached() {
    if (!isViewAttached()) throw new MvpViewNotAttachedException();
  }

  public static class MvpViewNotAttachedException extends RuntimeException {
    public MvpViewNotAttachedException() {
      super("Please call Presenter.attachView(MvpView) before" +
          " requesting data to the Presenter");
    }
  }
}
