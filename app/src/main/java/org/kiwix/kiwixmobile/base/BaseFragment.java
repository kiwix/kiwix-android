package org.kiwix.kiwixmobile.base;

import android.content.Context;
import android.support.v4.app.Fragment;

import dagger.android.support.AndroidSupportInjection;

/**
 * All fragments should inherit from this fragment.
 */

public abstract class BaseFragment extends Fragment {

  @Override
  public void onAttach(Context context) {
    AndroidSupportInjection.inject(this);
    super.onAttach(context);
  }
}
