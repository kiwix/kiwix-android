package org.kiwix.kiwixmobile.views;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import org.kiwix.kiwixmobile.R;

import butterknife.ButterKnife;

public class PageBottomTabLayout extends TabLayout {

  public PageBottomTabLayout(Context context) {
    this(context, null);
  }

  public PageBottomTabLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public PageBottomTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    inflate(getContext(), R.layout.page_bottom_tab_layout, this);
    ButterKnife.bind(this);
  }
}
