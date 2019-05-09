package org.kiwix.kiwixmobile.zim_manager

import android.support.v4.view.ViewPager.OnPageChangeListener

class SimplePageChangeListener(val onPageSelectedAction: (Int) -> Unit) : OnPageChangeListener {
  override fun onPageScrolled(
    position: Int,
    positionOffset: Float,
    positionOffsetPixels: Int
  ) {

  }

  override fun onPageSelected(position: Int) {
    onPageSelectedAction.invoke(position)
  }

  override fun onPageScrollStateChanged(state: Int) {

  }
}