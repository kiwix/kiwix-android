package org.kiwix.kiwixmobile.core.base

import android.content.Context
import androidx.fragment.app.Fragment
import org.kiwix.kiwixmobile.core.KiwixApplication
import org.kiwix.kiwixmobile.core.di.components.ActivityComponent

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
