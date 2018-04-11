package org.kiwix.kiwixmobile;


import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class ViewPagerAdapter extends PagerAdapter {

  private LayoutInflater layoutInflater;
  private int[] layouts;
  Context context;

  public ViewPagerAdapter(int[] layouts, Context context) {
    this.layouts = layouts;
    this.context = context;
  }

  @Override
  public Object instantiateItem(ViewGroup container, int position) {
    layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
