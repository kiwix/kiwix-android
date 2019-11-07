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

package org.kiwix.kiwixmobile.core.main;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import org.kiwix.kiwixmobile.core.R;
import org.kiwix.kiwixmobile.core.utils.SharedPreferenceUtil;

/**
 * Created by @Aditya-Sood as a part of GSoC 2019
 *
 * This is a generic helper class for displaying a 2-button (positive & negative) confirmation
 * dialog fragment on top of an existing dialog fragment
 * - Only for confirmation dialogs with a Positive & Negative button
 * - If you also need a Neutral button, add it selectively (if-else) for the required use case (Take
 * care of the callback interface as well)
 *
 * Currently used as:
 * - Helper class to show the alert dialog in case the user tries to exit the {@link AddNoteDialog}
 * with unsaved file changes
 **/

public class ConfirmationAlertDialogFragment extends DialogFragment {

  public static String TAG = "ConfirmationAlertDialog";

  private SharedPreferenceUtil sharedPreferenceUtil;
  private int stringResourceId;
  private String parentDialogFragmentTAG;

  public ConfirmationAlertDialogFragment(SharedPreferenceUtil sharedPreferenceUtil,
    String parentDialogFragmentTAG, int stringResourceId) {
    this.sharedPreferenceUtil = sharedPreferenceUtil;
    this.parentDialogFragmentTAG = parentDialogFragmentTAG;
    this.stringResourceId = stringResourceId;
  }

  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    Fragment parentDialogFragment = getFragmentManager().findFragmentByTag(parentDialogFragmentTAG);

    AlertDialog.Builder builder;

    if (sharedPreferenceUtil != null && sharedPreferenceUtil.nightMode()) { // Night Mode support
      builder = new AlertDialog.Builder(getActivity(), R.style.AppTheme_Dialog_Night);
    } else {
      builder = new AlertDialog.Builder(getActivity());
    }

    builder.setMessage(stringResourceId)
      .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {

          if (parentDialogFragment != null) {
            ((UserClickListener) parentDialogFragment).onPositiveClick();
          }
        }
      })
      .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {

          if (parentDialogFragment != null) {
            ((UserClickListener) parentDialogFragment).onNegativeClick();
          }
        }
      });

    return builder.create();
  }

  /**
   * Callback interface for responding to user clicks to a {@link ConfirmationAlertDialogFragment}
   * dialog
   */
  public interface UserClickListener {
    void onPositiveClick();

    void onNegativeClick();
  }
}
