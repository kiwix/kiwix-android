package org.kiwix.kiwixmobile.intro;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.constraint.ConstraintLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

import com.pixelcan.inkpageindicator.InkPageIndicator;

import org.kiwix.kiwixmobile.R;
import org.kiwix.kiwixmobile.base.BaseActivity;
import org.kiwix.kiwixmobile.main.MainActivity;
import org.kiwix.kiwixmobile.utils.SharedPreferenceUtil;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;

public class IntroActivity extends BaseActivity {

  @BindView(R.id.view_pager)
  ViewPager viewPager;
  @BindView(R.id.tab_indicator)
  InkPageIndicator tabIndicator;
  @Inject
  SharedPreferenceUtil preferences;

  private ValueAnimator tedAnimator;
  private ValueAnimator stackExchangeAnimator;
  private ValueAnimator wikivoyageAnimator;
  private ImageView airPlane;
  private Handler handler = new Handler();
  private Timer timer = new Timer();
  private int currentPage = 0;
  private ViewPager.OnPageChangeListener pageChangeListener = new ViewPager.OnPageChangeListener() {
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
      if (position == 2) {
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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_intro);
    LayoutInflater layoutInflater = getLayoutInflater();
    views = new View[]{
        layoutInflater.inflate(R.layout.item_intro_1, viewPager, false),
        layoutInflater.inflate(R.layout.item_intro_2, viewPager, false),
        layoutInflater.inflate(R.layout.item_intro_3, viewPager, false)
    };
    IntroPagerAdapter introPagerAdapter = new IntroPagerAdapter(views);
    viewPager.setAdapter(introPagerAdapter);
    tabIndicator.setViewPager(viewPager);

    stackExchangeAnimator = animateImage(views[1].findViewById(R.id.ic_stack_exchange),
        TimeUnit.SECONDS.toMillis(6));
    wikivoyageAnimator = animateImage(views[1].findViewById(R.id.ic_wikivoyage),
        TimeUnit.SECONDS.toMillis(9));
    tedAnimator = animateImage(views[1].findViewById(R.id.ic_ted),
        TimeUnit.SECONDS.toMillis(12));

    tedAnimator.start();
    stackExchangeAnimator.start();
    wikivoyageAnimator.start();

    airPlane = views[2].findViewById(R.id.airplane);
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
    tedAnimator.end();
    wikivoyageAnimator.end();
    stackExchangeAnimator.end();
    handler.removeCallbacksAndMessages(null);
    timer.cancel();
    for (View view : views) {
      view.findViewById(R.id.root).setOnClickListener(null);
    }
  }

  @OnClick(R.id.get_started)
  void startMainActivity() {
    dismissAutoRotate();
    startActivity(new Intent(this, MainActivity.class));
    preferences.setIntroShown();
    finish();
  }

  private void dismissAutoRotate() {
    handler.removeCallbacksAndMessages(null);
    timer.cancel();
  }

  private ValueAnimator animateImage(ImageView imageView, long orbitDuration) {
    ValueAnimator anim = ValueAnimator.ofInt(0, 359);
    anim.addUpdateListener(valueAnimator -> {
      int val = (Integer) valueAnimator.getAnimatedValue();
      ConstraintLayout.LayoutParams layoutParams = (ConstraintLayout.LayoutParams) imageView.getLayoutParams();
      layoutParams.circleAngle = val;
      imageView.setLayoutParams(layoutParams);
    });
    anim.setDuration(orbitDuration);
    anim.setInterpolator(new LinearInterpolator());
    anim.setRepeatMode(ValueAnimator.RESTART);
    anim.setRepeatCount(ValueAnimator.INFINITE);

    return anim;
  }
}
