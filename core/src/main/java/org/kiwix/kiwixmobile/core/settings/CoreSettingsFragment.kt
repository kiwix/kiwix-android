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
import org.kiwix.kiwixmobile.core.R
import org.kiwix.kiwixmobile.core.base.BaseFragment
import org.kiwix.kiwixmobile.core.databinding.SettingsBinding

abstract class CoreSettingsFragment : BaseFragment() {
  private lateinit var prefsFragment: Fragment
  private var settingsBinding: SettingsBinding? = null
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    prefsFragment = createPreferenceFragment()
    requireActivity().supportFragmentManager.beginTransaction()
      .replace(R.id.content_frame, prefsFragment).commit()
    setUpToolbar()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    settingsBinding = SettingsBinding.inflate(inflater, container, false)
    return settingsBinding?.root
  }

  protected abstract fun createPreferenceFragment(): Fragment

  private fun setUpToolbar() {
    val activity = requireActivity() as AppCompatActivity
    activity.setSupportActionBar(settingsBinding?.root?.findViewById(R.id.toolbar))
    activity.supportActionBar!!.title = getString(R.string.menu_settings)
    activity.supportActionBar!!.setHomeButtonEnabled(true)
    activity.supportActionBar!!.setDisplayHomeAsUpEnabled(true)
  }

  override fun onDestroyView() {
    requireActivity().supportFragmentManager.beginTransaction().remove(prefsFragment)
      .commitNowAllowingStateLoss()
    super.onDestroyView()
    settingsBinding = null
  }
}
