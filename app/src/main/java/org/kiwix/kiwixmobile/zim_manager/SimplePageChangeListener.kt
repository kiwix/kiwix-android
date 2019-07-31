package org.kiwix.kiwixmobile.zim_manager

import androidx.viewpager.widget.ViewPager.OnPageChangeListener

class SimplePageChangeListener(private val onPageSelectedAction: (Int) -> Unit) :
  OnPageChangeListener {
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
