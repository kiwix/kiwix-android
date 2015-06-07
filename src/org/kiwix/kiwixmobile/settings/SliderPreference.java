package org.kiwix.kiwixmobile.settings;

import org.kiwix.kiwixmobile.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

public class SliderPreference extends DialogPreference {

    protected final static int SEEKBAR_MAX = 500;

    protected int mSeekBarValue;

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

    public void setSummary(CharSequence[] summaries) {
        mSummaries = summaries;
    }

    @Override
    public void setSummary(CharSequence summary) {
        super.setSummary(summary);
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

    public float getValue() {
        return mSeekBarValue;
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
        mMessage = (TextView) view.findViewById(R.id.message);
        mMessage.setText(String.valueOf(mSeekBarValue));
        SeekBar seekbar = (SeekBar) view.findViewById(R.id.slider_preference_seekbar);
        seekbar.setMax(SEEKBAR_MAX);
        seekbar.setProgress(mSeekBarValue);
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
        }
        super.onDialogClosed(positiveResult);
    }
}