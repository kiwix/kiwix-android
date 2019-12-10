/*
 * Kiwix Android
 * Copyright (c) 2019 Kiwix <android.kiwix.org>
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

package org.kiwix.kiwixmobile.intro;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import androidx.viewpager.widget.ViewPager;
import butterknife.BindView;
import butterknife.OnClick;
import com.pixelcan.inkpageindicator.InkPageIndicator;
import java.util.Timer;
import java.util.TimerTask;
import javax.inject.Inject;
import org.kiwix.kiwixmobile.ActivityExtensionsKt;
import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.core.Intents;
import org.kiwix.kiwixmobile.core.base.BaseActivity;
import org.kiwix.kiwixmobile.core.main.CoreMainActivity;

public class IntroActivity extends BaseActivity implements IntroContract.View {

  private final Handler handler = new Handler();
  private final Timer timer = new Timer();
  @BindView(R.id.view_pager)
  ViewPager viewPager;
  @BindView(R.id.tab_indicator)
  InkPageIndicator tabIndicator;
  @Inject
  IntroContract.Presenter presenter;
  private ImageView airPlane;
  private int currentPage = 0;
  private final ViewPager.OnPageChangeListener pageChangeListener =
    new ViewPager.OnPageChangeListener() {
      @Override
      public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

      }

      @Override
      public void onPageSelected(int position) {
        if (position == 1) {
          airPlane.setVisibility(View.VISIBLE);
          airPlane.animate()
            .translationX(airPlane.getWidth())
            .setDuration(800);
        } else {
          airPlane.setVisibility(View.INVISIBLE);
          airPlane.animate()
            .translationX(-airPlane.getWidth());
        }
        currentPage = position;
      }

      @Override
      public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_DRAGGING) {
          dismissAutoRotate();
        }
      }
    };
  private View[] views;

  @Override protected void injection() {
    ActivityExtensionsKt.getKiwixActivityComponent(this).inject(this);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_intro);
    LayoutInflater layoutInflater = getLayoutInflater();
    views = new View[] {
      layoutInflater.inflate(R.layout.item_intro_1, viewPager, false),
      layoutInflater.inflate(R.layout.item_intro_2, viewPager, false)
    };
    IntroPagerAdapter introPagerAdapter = new IntroPagerAdapter(views);
    viewPager.setAdapter(introPagerAdapter);
    tabIndicator.setViewPager(viewPager);

    airPlane = views[1].findViewById(R.id.airplane);
    viewPager.addOnPageChangeListener(pageChangeListener);

    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        handler.post(() -> {
          if (currentPage == views.length) {
            currentPage = 0;
          }
          viewPager.setCurrentItem(currentPage++, true);
        });
      }
    }, 0, 2000);

    for (View view : views) {
      view.findViewById(R.id.root).setOnClickListener(v -> dismissAutoRotate());
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    handler.removeCallbacksAndMessages(null);
    timer.cancel();
    for (View view : views) {
      view.findViewById(R.id.root).setOnClickListener(null);
    }
  }

  @OnClick(R.id.get_started)
  void startMainActivity() {
    dismissAutoRotate();
    startActivity(Intents.internal(CoreMainActivity.class));
    presenter.setIntroShown();
    finish();
  }

  private void dismissAutoRotate() {
    handler.removeCallbacksAndMessages(null);
    timer.cancel();
  }
}
