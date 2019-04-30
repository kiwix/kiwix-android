package org.kiwix.kiwixmobile.base

import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity

import org.kiwix.kiwixmobile.KiwixApplication
import org.kiwix.kiwixmobile.di.components.ActivityComponent

/**
 * All fragments should inherit from this fragment.
 */

abstract class BaseFragment : Fragment() {

  override fun onAttach(context: Context?) {
    super.onAttach(context)
    inject(
        KiwixApplication.getApplicationComponent().activityComponent()
            .activity(activity as FragmentActivity)
            .build()
    )
  }

  abstract fun inject(activityComponent: ActivityComponent)
}
