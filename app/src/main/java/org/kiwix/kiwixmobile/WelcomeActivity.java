package org.kiwix.kiwixmobile;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WelcomeActivity extends AppCompatActivity {

  private ViewPager viewPager;
  private int[] layouts;
  private TextView[] dots;
  private LinearLayout dotsLayout;
  private Button next;
  private Button skip;
  private Button prev;
  private ViewPagerAdapter viewPagerAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.welcome_main);
    viewPager = (ViewPager) findViewById(R.id.view_pager);
    dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);
    skip = (Button) findViewById(R.id.btn_skip);
    next = (Button) findViewById(R.id.btn_next);
    prev = (Button) findViewById(R.id.btn_prev);
    prev.setVisibility(View.GONE);


    layouts = new int[]{R.layout.welcome_detail1, R.layout.welcome_detail2, R.layout.welcome_detail3, R.layout.welcome_detail4};

    addBottomDots(0);
    viewPagerAdapter = new ViewPagerAdapter();
    viewPager.setAdapter(viewPagerAdapter);
    viewPager.addOnPageChangeListener(viewListener);

    skip.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        Intent i = new Intent(WelcomeActivity.this, KiwixMobileActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
      }
    });

    next.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        int current = getItem(+1);
        if (current < layouts.length) {
          viewPager.setCurrentItem(current);
        } else {
          Intent i = new Intent(WelcomeActivity.this, KiwixMobileActivity.class);
          i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
          startActivity(i);
          finish();
        }
      }
    });

    prev.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        int current = getItem(-1);
        if (current >= 0) {
          viewPager.setCurrentItem(current);
        }
      }
    });

  }

  private void addBottomDots(int position) {

    dots = new TextView[layouts.length];
    dotsLayout.removeAllViews();
    for (int i = 0; i < layouts.length; i++) {
      dots[i] = new TextView(this);
      dots[i].setText(Html.fromHtml("&#8226;"));
      dots[i].setTextSize(35);
      dots[i].setTextColor(getResources().getColor(R.color.primary));
    }
    if (dots.length > 0)
      dots[position].setTextColor(getResources().getColor(R.color.primary));

  }

  private int getItem(int i) {
    return viewPager.getCurrentItem() + i;
  }

  ViewPager.OnPageChangeListener viewListener = new ViewPager.OnPageChangeListener() {
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {

      addBottomDots(position);
      if (position == layouts.length - 1) {
        next.setText(R.string.proceed_button);
        skip.setVisibility(View.GONE);
        prev.setVisibility(View.VISIBLE);
      } else if (position == 0) {
        prev.setVisibility(View.GONE);
      } else {
        next.setText(R.string.next);
        skip.setVisibility(View.VISIBLE);
        prev.setVisibility(View.VISIBLE);
      }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }
  };


  public class ViewPagerAdapter extends PagerAdapter {

    private LayoutInflater layoutInflater;

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
      layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View v = layoutInflater.inflate(layouts[position], container, false);
      container.addView(v);
      return v;
    }

    @Override
    public int getCount() {
      return layouts.length;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
      return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
      View v = (View) object;
      container.removeView(v);
    }
  }

}
