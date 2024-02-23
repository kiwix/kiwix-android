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

package org.kiwix.kiwixmobile.core.base

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.extensions.getToolbarNavigationIcon
import org.kiwix.kiwixmobile.core.extensions.setToolTipWithContentDescription

/**
 * All fragments should inherit from this fragment.
 */

abstract class BaseFragment : Fragment() {

  open val fragmentToolbar: Toolbar? = null
  open val fragmentTitle: String? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    inject(activity as BaseActivity)
  }

  abstract fun inject(baseActivity: BaseActivity)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    setupToolbar()
  }

  // Setup toolbar to handle common back pressed event
  private fun setupToolbar() {
    val activity = activity as AppCompatActivity?
    fragmentToolbar?.apply {
      activity?.let {
        it.setSupportActionBar(this)
        it.supportActionBar?.let { actionBar ->
          actionBar.setDisplayHomeAsUpEnabled(true)
          actionBar.title = fragmentTitle
          // set the navigation back button contentDescription
          getToolbarNavigationIcon()?.setToolTipWithContentDescription(
            getString(R.string.toolbar_back_button_content_description)
          )
        }
      }
      setNavigationOnClickListener {
        activity?.onBackPressedDispatcher?.onBackPressed()
      }
    }
  }
}
