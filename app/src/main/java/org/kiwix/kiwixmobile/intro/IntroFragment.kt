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
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.viewpager.widget.ViewPager
import org.kiwix.kiwixmobile.R
import org.kiwix.kiwixmobile.cachedComponent
import org.kiwix.kiwixmobile.core.base.BaseActivity
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.base.FragmentActivityExtensions
import org.kiwix.kiwixmobile.databinding.FragmentIntroBinding
import org.kiwix.kiwixmobile.zimManager.SimplePageChangeListener
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

class IntroFragment : BaseFragment(), IntroContract.View, FragmentActivityExtensions {

  companion object {
    private const val timerDelay: Long = 0
    private const val timerPeriod: Long = 2000
    private const val animationDuration: Long = 800
  }

  private val handler = Handler(Looper.getMainLooper())
  private var timer: Timer? = Timer()
  private var fragmentIntroBinding: FragmentIntroBinding? = null

  @Inject
  internal lateinit var presenter: IntroContract.Presenter
  private var currentPage = 0
  private lateinit var views: Array<View>

  override fun inject(baseActivity: BaseActivity) {
    baseActivity.cachedComponent.inject(this)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    fragmentIntroBinding?.getStarted?.setOnClickListener { navigateToLibrary() }
    fragmentIntroBinding?.viewPager?.let { viewPager ->
      views = arrayOf(
        layoutInflater.inflate(R.layout.item_intro_1, viewPager, false),
        layoutInflater.inflate(R.layout.item_intro_2, viewPager, false)
      )
      viewPager.run {
        adapter = IntroPagerAdapter(views)
        simplePageChangeListener?.let(::addOnPageChangeListener)
      }
      fragmentIntroBinding?.tabIndicator?.setViewPager(viewPager)
    }
    timer?.schedule(
      object : TimerTask() {
        override fun run() {
          handler.post {
            if (currentPage == views.size) currentPage = 0
            fragmentIntroBinding?.viewPager?.setCurrentItem(currentPage++, true)
          }
        }
      },
      timerDelay,
      timerPeriod
    )
    views.forEach {
      it.setOnClickListener { dismissAutoRotate() }
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    fragmentIntroBinding = FragmentIntroBinding.inflate(inflater, container, false)
    return fragmentIntroBinding?.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    handler.removeCallbacksAndMessages(null)
    timer?.cancel()
    timer = null
    views.forEach {
      it.setOnClickListener(null)
    }
    views = emptyArray()
    simplePageChangeListener = null
    fragmentIntroBinding = null
  }

  private fun navigateToLibrary() {
    dismissAutoRotate()
    presenter.setIntroShown()
    findNavController().navigate(IntroFragmentDirections.actionIntrofragmentToLibraryFragment())
  }

  private fun updateView(position: Int) {
    val airplane = views[1].findViewById<ImageView>(R.id.airplane) ?: return
    airplane.isVisible = position == 1
    if (position == 1) {
      airplane.animate().translationX(airplane.width.toFloat()).duration = animationDuration
    } else {
      airplane.animate().translationX(-airplane.width.toFloat())
    }
    currentPage = position
  }

  private fun handleDraggingState(state: Int) {
    if (state == ViewPager.SCROLL_STATE_DRAGGING) {
      dismissAutoRotate()
    }
  }

  private fun dismissAutoRotate() {
    handler.removeCallbacksAndMessages(null)
    timer?.cancel()
  }

  private var simplePageChangeListener: SimplePageChangeListener? =
    SimplePageChangeListener(::updateView, ::handleDraggingState)
}
