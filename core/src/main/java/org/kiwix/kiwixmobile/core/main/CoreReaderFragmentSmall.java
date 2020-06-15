/*
 * Kiwix Android
 * Copyright (c) 2020 Kiwix <android.kiwix.org>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.kiwix.kiwixmobile.core.main;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import androidx.annotation.AnimRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.R2;
import org.kiwix.kiwixmobile.core.base.BaseActivity;
import org.kiwix.kiwixmobile.core.base.BaseFragment;
import org.kiwix.kiwixmobile.core.extensions.ViewGroupExtensions;
import org.kiwix.kiwixmobile.core.reader.ZimFileReader;
import org.kiwix.kiwixmobile.core.reader.ZimReaderContainer;
import org.kiwix.kiwixmobile.core.zim_manager.fileselect_view.adapter.BooksOnDiskListItem;

public class CoreReaderFragmentSmall extends BaseFragment implements MainContract.View {

  @BindView(R2.id.toolbar)
  Toolbar toolbar;
  private ActionBar actionBar;

  private MainMenu mainMenu;
  protected int currentWebViewIndex = 0;
  protected final List<KiwixWebView> webViewList = new ArrayList<>();

  @Inject
  protected MainContract.Presenter presenter;

  @Inject
  protected ZimReaderContainer zimReaderContainer;

  @SuppressLint("ClickableViewAccessibility")
  @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
    @Nullable ViewGroup container,
    @Nullable Bundle savedInstanceState) {
    View root = inflater.inflate(R.layout.fragment_main, container, false);
    ButterKnife.bind(this, root);
    AppCompatActivity activity = (AppCompatActivity) getActivity();
    presenter.attachView(this);
    activity.setSupportActionBar(toolbar);
    actionBar = activity.getSupportActionBar();
    toolbar.setOnTouchListener(new OnSwipeTouchListener(getActivity()) {

      @Override
      @SuppressLint("SyntheticAccessor")
      public void onSwipeBottom() {
        showTabSwitcher();
      }

      @Override
      public void onSwipeLeft() {
        if (currentWebViewIndex < webViewList.size() - 1) {
          View current = getCurrentWebView();
          startAnimation(current, R.anim.transition_left);
          selectTab(currentWebViewIndex + 1);
        }
      }

      @Override
      public void onSwipeRight() {
        if (currentWebViewIndex > 0) {
          View current = getCurrentWebView();
          startAnimation(current, R.anim.transition_right);
          selectTab(currentWebViewIndex - 1);
        }
      }

      @Override public void onTap(MotionEvent e) {
        final View titleTextView = ViewGroupExtensions.findFirstTextView(toolbar);
        if (titleTextView == null) return;
        final Rect hitRect = new Rect();
        titleTextView.getHitRect(hitRect);
        if (hitRect.contains((int) e.getX(), (int) e.getY())) {
          if (mainMenu != null) {
            mainMenu.tryExpandSearch(zimReaderContainer.getZimFileReader());
          }
        }
      }
    });

    return root;
  }

  //TODO
  private void showTabSwitcher() {

  }

  //TODO
  protected void selectTab(int position) {

  }

  //TODO
  protected KiwixWebView newTab(String url) {
    return null;
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    //((AppCompatActivity) getActivity()).setSupportActionBar(toolbar);
  }

  private void startAnimation(View view, @AnimRes int anim) {
    view.startAnimation(AnimationUtils.loadAnimation(view.getContext(), anim));
  }

  protected KiwixWebView getCurrentWebView() {
    if (webViewList.size() == 0) return newMainPageTab();
    if (currentWebViewIndex < webViewList.size()) {
      return webViewList.get(currentWebViewIndex);
    } else {
      return webViewList.get(0);
    }
  }

  protected KiwixWebView newMainPageTab() {
    return newTab(contentUrl(zimReaderContainer.getMainPage()));
  }

  @NotNull
  private String contentUrl(String articleUrl) {
    return Uri.parse(ZimFileReader.CONTENT_PREFIX + articleUrl).toString();
  }

  @Override public void addBooks(
    List<BooksOnDiskListItem> books) {

  }

  @Override public void inject(@NotNull BaseActivity baseActivity) {

  }
}
