/*
 * Kiwix Android
 * Copyright (C) 2018  Kiwix <android.kiwix.org>
 *
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
 */
package org.kiwix.kiwixmobile.settings;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import org.kiwix.kiwixmobile.R;

import static org.kiwix.kiwixmobile.utils.Constants.PREF_NIGHTMODE;
import static org.kiwix.kiwixmobile.utils.Constants.PREF_ZOOM_ENABLED;

public class SliderPreference extends DialogPreference {

  protected final static int SEEKBAR_MAX = 500;

  protected int mSeekBarValue;

  protected int initialSeekBarValue;

  protected CharSequence[] mSummaries;

  private TextView mMessage;

  /**
   * @param context
   * @param attrs
   */
  public SliderPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    setup(context, attrs);
  }

  /**
   * @param context
   * @param attrs
   * @param defStyle
   */
  public SliderPreference(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    setup(context, attrs);
  }

  private void setup(Context context, AttributeSet attrs) {
    setDialogLayoutResource(R.layout.slider_dialog);
    updateSummaryText(context, attrs);
  }

  public void updateSummaryText(Context context, AttributeSet attrs) {
    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SliderPreference);
    try {
      setSummary(a.getTextArray(R.styleable.SliderPreference_android_summary));
    } catch (Exception e) {
      // Do nothing
    }
    a.recycle();
  }

  @Override
  protected Object onGetDefaultValue(TypedArray a, int index) {
    return a.getFloat(index, 0);
  }

  @Override
  protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
    setValue(restoreValue ? getPersistedFloat(mSeekBarValue) : (Float) defaultValue);
  }

  @Override
  public CharSequence getSummary() {
    if (mSummaries != null && mSummaries.length > 0) {
      double piece = SEEKBAR_MAX / mSummaries.length;
      int index = (int) (mSeekBarValue / piece);
      if (index == mSummaries.length) {
        --index;
      }
      return mSummaries[index];
    } else {
      return super.getSummary();
    }
  }

  @Override
  public void setSummary(int summaryResId) {
    try {
      //noinspection ResourceType
      setSummary(getContext().getResources().getStringArray(summaryResId));
    } catch (Exception e) {
      super.setSummary(summaryResId);
    }
  }

  public void setSummary(CharSequence[] summaries) {
    mSummaries = summaries;
  }

  public void setValue(float value) {
    if (shouldPersist()) {
      persistFloat(value);
    }
    if (value != mSeekBarValue) {
      mSeekBarValue = (int) value;
      notifyChanged();
    }
  }

  @Override
  protected View onCreateDialogView() {
    View view = super.onCreateDialogView();
    mMessage = view.findViewById(R.id.message);
    mMessage.setText(String.valueOf(mSeekBarValue));
    SeekBar seekbar = view.findViewById(R.id.slider_preference_seekbar);
    seekbar.setMax(SEEKBAR_MAX);
    seekbar.setProgress(mSeekBarValue);
    initialSeekBarValue = mSeekBarValue;
    seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (fromUser) {
          mSeekBarValue = progress;
          mMessage.setText(String.valueOf(mSeekBarValue));
        }
      }
    });
    return view;
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    if (positiveResult && callChangeListener(mSeekBarValue)) {
      setValue(mSeekBarValue);
    } else {
      mSeekBarValue = initialSeekBarValue;
    }
    super.onDialogClosed(positiveResult);
  }

  @Override
  protected void onBindView(View view) {
    super.onBindView(view);
    setGrayState(view);
  }

  private void setGrayState(View view) {
    boolean enabled = getPreferenceManager().getSharedPreferences().getBoolean(PREF_ZOOM_ENABLED, false);
    boolean Nightmode = getPreferenceManager().getSharedPreferences().getBoolean(PREF_NIGHTMODE, false);
    TextView titleView = view.findViewById(android.R.id.title);
    TextView summaryTV = view.findViewById(android.R.id.summary);
    if (!enabled) {
      titleView.setTextColor(Color.GRAY);
      summaryTV.setTextColor(Color.GRAY);
    } else {
      if (Nightmode) {
        titleView.setTextColor(Color.WHITE);
        summaryTV.setTextColor(Color.WHITE);
      } else {
        titleView.setTextColor(Color.BLACK);
        summaryTV.setTextColor(Color.BLACK);
      }
    }
  }
}
