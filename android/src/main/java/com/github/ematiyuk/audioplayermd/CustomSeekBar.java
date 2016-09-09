package com.github.ematiyuk.audioplayermd;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

/**
 * Represents custom SeekBar element that overrides standard disabling functionality.
 *
 * It does not "grey out" the seek bar, if it is disabled.
 */
public class CustomSeekBar extends SeekBar {
    private boolean mEnabled;

    public CustomSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        mEnabled = false;
    }

    /**
     * Disables standard {@link SeekBar#onTouchEvent(MotionEvent)} behaviour.
     *
     * @return <code>false</code> if {@link #setEnabled(boolean)} accepts <code>false</code>,
     * else returns standard {@link SeekBar#onTouchEvent(MotionEvent)} result.
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mEnabled && super.onTouchEvent(event);
    }

    @Override
    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }
}
