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
package org.kiwix.kiwixmobile.intro

import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ImageView
import androidx.annotation.Nullable
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import butterknife.BindView
import butterknife.OnClick
import com.pixelcan.inkpageindicator.InkPageIndicator
import org.kiwix.kiwixmobile.R.id
import org.kiwix.kiwixmobile.R.layout
import org.kiwix.kiwixmobile.core.Intents.internal
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.intro.IntroContract.Presenter
import org.kiwix.kiwixmobile.intro.IntroContract.View
import org.kiwix.kiwixmobile.kiwixActivityComponent
import java.util.Timer
import java.util.TimerTask

const val DURATION_WAITER = 800
const val PERIOD_TIME = 2000
const val DELAY_TIME = 0

class IntroActivity : BaseActivity(), View {
  private val handler = Handler()
  private val timer = Timer()
  @BindView(id.view_pager)
  @Nullable
  @JvmField
  var viewPager: ViewPager? = null
  @BindView(id.tab_indicator)
  @Nullable
  @JvmField
  var tabIndicator: InkPageIndicator? = null
  var presenter: Presenter? = null
  private var airPlane: ImageView? = null
  private var currentPage = 0
  private val pageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
    override fun onPageScrolled(
      position: Int,
      positionOffset: Float,
      positionOffsetPixels: Int
    ) {
      Log.d("CustomViewPager", "pageChangeListener")
    }

    override fun onPageSelected(position: Int) {
      if (position == 1) {
        airPlane!!.visibility = android.view.View.VISIBLE
        airPlane!!.animate()
          .translationX(airPlane!!.width.toFloat()).duration = DURATION_WAITER.toLong()
      } else {
        airPlane!!.visibility = android.view.View.INVISIBLE
        airPlane!!.animate()
          .translationX(-airPlane!!.width.toFloat())
      }
      currentPage = position
    }

    override fun onPageScrollStateChanged(state: Int) {
      if (state == ViewPager.SCROLL_STATE_DRAGGING) {
        dismissAutoRotate()
      }
    }
  }
  private lateinit var views: Array<android.view.View>
  override fun injection(coreComponent: CoreComponent) {
    this.kiwixActivityComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.activity_intro)
    val layoutInflater = layoutInflater
    views = arrayOf(
      layoutInflater.inflate(layout.item_intro_1, viewPager, false),
      layoutInflater.inflate(layout.item_intro_2, viewPager, false)
    )
    val introPagerAdapter = IntroPagerAdapter(views)
    viewPager!!.adapter = introPagerAdapter
    tabIndicator!!.setViewPager(viewPager)
    airPlane = views[1].findViewById(id.airplane)
    viewPager!!.addOnPageChangeListener(pageChangeListener)
    timer.schedule(object : TimerTask() {
      override fun run() {
        handler.post {
          if (currentPage == views.size) {
            currentPage = 0
          }
          viewPager!!.setCurrentItem(currentPage++, true)
        }
      }
    }, DELAY_TIME.toLong(), PERIOD_TIME.toLong())
    for (view in views) {
      view.findViewById<android.view.View>(id.root)
        .setOnClickListener { v: android.view.View? -> dismissAutoRotate() }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.removeCallbacksAndMessages(null)
    timer.cancel()
    for (view in views) {
      view.findViewById<android.view.View>(id.root).setOnClickListener(null)
    }
  }

  @OnClick(id.get_started) fun startMainActivity() {
    dismissAutoRotate()
    startActivity(internal(CoreMainActivity::class.java))
    presenter!!.setIntroShown()
    finish()
  }

  private fun dismissAutoRotate() {
    handler.removeCallbacksAndMessages(null)
    timer.cancel()
  }
}
