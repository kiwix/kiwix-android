package org.kiwix.kiwixmobile.new_bookmarks;

/**
 * Created by EladKeyshawn on 03/04/2017.
 */

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialogFragment;
import android.view.ViewGroup;

import org.kiwix.kiwixmobile.R;

/**
 * Descendant of BottomSheetDialogFragment that adds a few features and conveniences.
 */
public class ExtendedBottomSheetDialogFragment extends BottomSheetDialogFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        setWindowLayout();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setWindowLayout();
    }

    protected void disableBackgroundDim() {
        getDialog().getWindow().setDimAmount(0f);
    }

    private void setWindowLayout() {
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(dialogWidthPx(), ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private int dialogWidthPx() {
        return Math.min(getResources().getDisplayMetrics().widthPixels,
                (int) getResources().getDimension(R.dimen.bottomSheetMaxWidth));
    }
}