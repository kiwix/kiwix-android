package org.kiwix.kiwixmobile.intro;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;

class IntroPagerAdapter extends PagerAdapter {
  private final View[] views;

  IntroPagerAdapter(View[] views) {
    this.views = views;
  }

  @Override
  public int getCount() {
    return views.length;
  }

  @NonNull
  @Override
  public Object instantiateItem(@NonNull ViewGroup container, int position) {
    container.addView(views[position]);
    return views[position];
  }

  @Override
  public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
    container.removeView((View) object);
  }

  @Override
  public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
    return view == object;
  }
}
