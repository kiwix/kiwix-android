package org.kiwix.kiwixmobile.base

import android.content.Context
import androidx.fragment.app.Fragment
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
        .activity(activity!!)
        .build()
    )
  }

  abstract fun inject(activityComponent: ActivityComponent)
}
