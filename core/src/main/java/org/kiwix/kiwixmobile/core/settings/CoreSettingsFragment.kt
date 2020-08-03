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
package org.kiwix.kiwixmobile.core.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.layout_toolbar.toolbar
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseFragment

abstract class CoreSettingsFragment : BaseFragment() {

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    requireActivity().supportFragmentManager
      .beginTransaction().replace(R.id.content_frame, createPreferenceFragment()!!)
      .commit()
    setUpToolbar()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View = inflater.inflate(R.layout.settings, container, false)

  protected abstract fun createPreferenceFragment(): Fragment

  private fun setUpToolbar() {
    val activity = requireActivity() as AppCompatActivity
    activity.setSupportActionBar(toolbar)
    activity.supportActionBar!!.title = getString(R.string.menu_settings)
    activity.supportActionBar!!.setHomeButtonEnabled(true)
    activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
  }
}
