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
import android.view.View
import androidx.core.view.isVisible
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.activity_intro.*
import kotlinx.android.synthetic.main.item_intro_2.airplane
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.core.Intents.internal
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.di.components.CoreComponent
import org.kiwix.kiwixmobile.core.main.CoreMainActivity
import org.kiwix.kiwixmobile.kiwixActivityComponent
import org.kiwix.kiwixmobile.zim_manager.SimplePageChangeListener
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

class IntroActivity : BaseActivity(), IntroContract.View {

  companion object {
    private const val timerDelay: Long = 0
    private const val timerPeriod: Long = 2000
    private const val animationDuration: Long = 800
  }

  private val handler = Handler()
  private val timer = Timer()

  @Inject
  internal lateinit var presenter: IntroContract.Presenter
  private var currentPage = 0
  private lateinit var views: Array<View>

  override fun injection(coreComponent: CoreComponent) {
    this.kiwixActivityComponent.inject(this)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_intro)
    get_started.setOnClickListener { startMainActivity() }
    views = arrayOf(
      layoutInflater.inflate(R.layout.item_intro_1, view_pager, false),
      layoutInflater.inflate(R.layout.item_intro_2, view_pager, false)
    )
    view_pager.run {
      adapter = IntroPagerAdapter(views)
      addOnPageChangeListener(SimplePageChangeListener(::updateView, ::handleDraggingState))
    }
    tab_indicator.setViewPager(view_pager)
    timer.schedule(object : TimerTask() {
      override fun run() {
        handler.post {
          if (currentPage == views.size) currentPage = 0
          view_pager.setCurrentItem(currentPage++, true)
        }
      }
    }, timerDelay, timerPeriod)
    views.forEach {
      it.setOnClickListener { dismissAutoRotate() }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    handler.removeCallbacksAndMessages(null)
    timer.cancel()
    views.forEach {
      it.setOnClickListener(null)
    }
  }

  private fun startMainActivity() {
    dismissAutoRotate()
    startActivity(internal(CoreMainActivity::class.java))
    presenter.setIntroShown()
    finish()
  }

  private fun updateView(position: Int) {
    airplane.isVisible = position == 1
    if (position == 1)
      airplane.animate().translationX(airplane.width.toFloat()).duration = animationDuration
    else
      airplane.animate().translationX(-airplane.width.toFloat())
    currentPage = position
  }

  private fun handleDraggingState(state: Int) {
    if (state == ViewPager.SCROLL_STATE_DRAGGING) dismissAutoRotate()
  }

  private fun dismissAutoRotate() {
    handler.removeCallbacksAndMessages(null)
    timer.cancel()
  }
}
