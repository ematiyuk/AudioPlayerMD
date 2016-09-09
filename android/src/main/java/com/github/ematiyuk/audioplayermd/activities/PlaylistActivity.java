package com.github.ematiyuk.audioplayermd.activities;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.github.ematiyuk.audioplayermd.fragments.PlaylistFragment;
import com.github.ematiyuk.audioplayermd.R;

public class PlaylistActivity extends SingleFragmentActivity
        implements PlaylistFragment.Callbacks {

    @Override
    protected Fragment createFragment() {
        return new PlaylistFragment();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_with_controller;
    }

    @Override
    public void onControllerShow() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        ft.setCustomAnimations(
                R.anim.slide_in_from_bottom, R.anim.slide_out_to_bottom,
                R.anim.slide_in_from_bottom, R.anim.slide_out_to_bottom);

        Fragment controllerFragment = fm.findFragmentById(R.id.fragment_playback_controls);
        ft.show(controllerFragment);
        ft.commit();
    }

    @Override
    public void onControllerHide() {
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        Fragment controllerFragment = fm.findFragmentById(R.id.fragment_playback_controls);

        ft.hide(controllerFragment);
        ft.commit();
    }
}
