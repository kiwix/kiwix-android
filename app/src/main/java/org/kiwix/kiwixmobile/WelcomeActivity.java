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
  private Button next;
  private Button skip;
  private Button prev;
  private ViewPagerAdapter viewPagerAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.welcome_main);
    viewPager = (ViewPager) findViewById(R.id.view_pager);
    skip = (Button) findViewById(R.id.btn_skip);
    next = (Button) findViewById(R.id.btn_next);
    prev = (Button) findViewById(R.id.btn_prev);
    prev.setVisibility(View.GONE);

    layouts = new int[]{R.layout.welcome_detail1, R.layout.welcome_detail2, R.layout.welcome_detail3, R.layout.welcome_detail4};

    viewPagerAdapter = new ViewPagerAdapter(layouts,this);
    viewPager.setAdapter(viewPagerAdapter);
    viewPager.addOnPageChangeListener(viewListener);

    skip.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        skipClicked();
      }
    });

    next.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        nextClicked();
      }
    });

    prev.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        prevClicked();
      }
    });
  }

  public void skipClicked(){
    Intent i = new Intent(WelcomeActivity.this, KiwixMobileActivity.class);
    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
    startActivity(i);
    finish();
  }

  public void nextClicked(){
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

  public void prevClicked(){
    int current = getItem(-1);
    if (current >= 0) {
      viewPager.setCurrentItem(current);
    }
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
      //No implementation
    }
  };
}
