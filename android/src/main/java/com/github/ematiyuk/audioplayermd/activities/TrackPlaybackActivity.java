package com.github.ematiyuk.audioplayermd.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.github.ematiyuk.audioplayermd.fragments.TrackPlaybackDialog;

public class TrackPlaybackActivity extends AppCompatActivity {
    private static final String TAG = "TrackPlaybackActivity";

    private static final String DIALOG_AUDIO_PLAYBACK = "audio_playback_dialog";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // do not close the activity on touch outside it
        setFinishOnTouchOutside(false);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        String scheme = intent.getScheme();

        if (Intent.ACTION_VIEW.equals(action) && type != null && scheme != null) {
            if (type.startsWith("audio/") && "file".equals(scheme)) {
                handleSentAudio(intent);
            }
        }
    }

    private void handleSentAudio(Intent intent) {
        Uri audioUri = intent.getData();
        if (audioUri != null) {
            // show dialog fragment
            FragmentManager fm = getSupportFragmentManager();
            TrackPlaybackDialog.newInstance(audioUri).show(fm, DIALOG_AUDIO_PLAYBACK);
        }
    }
}
