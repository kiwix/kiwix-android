package org.kiwix.kiwixmobile.base;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import org.kiwix.kiwixmobile.KiwixApplication;

import androidx.fragment.app.Fragment;

/**
 * All fragments should inherit from this fragment.
 */

public abstract class BaseFragment extends Fragment {

  @Override
  public void onAttach(Context context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      KiwixApplication.getApplicationComponent().inject(this);
    }
    super.onAttach(context);
  }

  @SuppressWarnings("deprecation")
  @Override
  public void onAttach(Activity activity) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      KiwixApplication.getApplicationComponent().inject(this);
    }
    super.onAttach(activity);
  }
}
