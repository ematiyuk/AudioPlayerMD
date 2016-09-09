package com.github.ematiyuk.audioplayermd.activities;

import android.support.v4.app.Fragment;

import com.github.ematiyuk.audioplayermd.fragments.ControllerFragment;

public class ControllerActivity extends SingleFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return new ControllerFragment();
    }
}
