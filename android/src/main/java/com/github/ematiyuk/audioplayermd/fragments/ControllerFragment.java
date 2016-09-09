package com.github.ematiyuk.audioplayermd.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.github.ematiyuk.audioplayermd.BaseController;
import com.github.ematiyuk.audioplayermd.R;
import com.github.ematiyuk.audioplayermd.service.TimeFormatter;
import com.github.ematiyuk.audioplayermd.model.Track;

public class ControllerFragment extends Fragment implements BaseController.Callback {
    private static final String TAG = "ControllerFragment";

    private TextView mArtistTextView, mTitleTextView, mAlbumTextView;
    private ImageButton mPlayPauseButton, mStopButton;
    private ImageButton mNextButton, mPrevButton;
    private TextView mTotalTimeTextView, mPlayedTimeTextView;
    private SeekBar mSeekBar;
    private boolean mDragging;
    // handler to update UI timer, SeekBar etc.
    private Handler mSeekHandler = new Handler();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        BaseController.get(getActivity()).setCallbackListener(this);
    }

    @Override
    public void onDestroy() {
        mSeekHandler.removeCallbacks(mUpdateTimeTask);

        super.onDestroy();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.audio_controller_layout, container, false);

        mTitleTextView = (TextView) rootView.findViewById(R.id.controllerTrackTitle);
        mArtistTextView = (TextView) rootView.findViewById(R.id.controllerTrackArtist);
        mAlbumTextView = (TextView) rootView.findViewById(R.id.controllerTrackAlbum);
        mPlayPauseButton = (ImageButton) rootView.findViewById(R.id.playPauseButton);
        mStopButton = (ImageButton) rootView.findViewById(R.id.stopButton);
        mNextButton = (ImageButton) rootView.findViewById(R.id.nextButton);
        mPrevButton = (ImageButton) rootView.findViewById(R.id.prevButton);
        mSeekBar = (SeekBar) rootView.findViewById(R.id.controllerProgress);
        mTotalTimeTextView = (TextView) rootView.findViewById(R.id.controllerTotalTime);
        mPlayedTimeTextView = (TextView) rootView.findViewById(R.id.controllerPlayedTime);

        mPlayPauseButton.setOnClickListener(mPlayPauseListener);
        mStopButton.setOnClickListener(mStopListener);
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);

        // initialization section
        mPlayedTimeTextView.setText(TimeFormatter.format(0));
        mTotalTimeTextView.setText(TimeFormatter.format(0));
        mSeekBar.setProgress(0);
        mSeekBar.setMax((int) BaseController.get(getActivity()).getProgressMaxValue());

        // set focus to the textviews to enable moving text
        mTitleTextView.setSelected(true);
        mArtistTextView.setSelected(true);
        mAlbumTextView.setSelected(true);

        return rootView;
    }

    private View.OnClickListener mPlayPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            BaseController.get(getActivity()).doPlayPause();

            onUpdateSeekBar();
        }
    };

    private View.OnClickListener mStopListener = new View.OnClickListener() {
        public void onClick(View v) {
            BaseController.get(getActivity()).doStop();

            onResetProgress();
        }
    };

    private SeekBar.OnSeekBarChangeListener mSeekListener = new SeekBar.OnSeekBarChangeListener() {

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mDragging = true;

            mSeekHandler.removeCallbacks(mUpdateTimeTask);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;

            mPlayedTimeTextView.setText(TimeFormatter.format((int) BaseController.get(getActivity())
                    .getPositionOnProgressChanged(seekBar.getProgress())));

            onUpdateSeekBar();
        }
    };

    private Runnable mUpdateTimeTask = new Runnable() {
        @Override
        public void run() {
            if (BaseController.getAudioService() != null) {
                if (BaseController.getAudioService().isPlaying()) {
                    long duration = BaseController.getAudioService().getDuration();
                    long currentPosition = BaseController.getAudioService().getCurrPosition();
                    long pos = BaseController.get(getActivity()).getProgressMaxValue() * currentPosition / duration;
                    mSeekBar.setProgress((int) pos);
                    mPlayedTimeTextView.setText(TimeFormatter.format(currentPosition));
                }
                updatePlayPauseIcon(BaseController.getAudioService().isPlaying());
            }

            if (!mDragging) {
                mSeekHandler.postDelayed(this, 1000L);
            }
        }
    };

    private void updatePlayPauseIcon(boolean isPlaying) {
        if (isPlaying) {
            mPlayPauseButton.setImageResource(R.drawable.ic_control_pause);
        } else {
            mPlayPauseButton.setImageResource(R.drawable.ic_control_play);
        }
    }

    @Override
    public void onUpdateTrackInfo(Track currentTrack) {
        mTitleTextView.setText(currentTrack.getTitle());
        mArtistTextView.setText(currentTrack.getArtist());
        mAlbumTextView.setText(currentTrack.getAlbum());
        mTotalTimeTextView.setText(TimeFormatter.format(currentTrack.getDuration()));
    }

    @Override
    public void onUpdateSeekBar() {
        mSeekBar.setEnabled(true);

        mSeekHandler.postDelayed(mUpdateTimeTask, 50L);
    }

    @Override
    public void onResetProgress() {
        mSeekBar.setProgress(0);
        mSeekBar.setEnabled(false);
        mPlayedTimeTextView.setText(TimeFormatter.format(0));
        mSeekHandler.removeCallbacks(mUpdateTimeTask);
        updatePlayPauseIcon(false);
    }

    @Override
    public void onSetPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mNextButton.setOnClickListener(next);
        mPrevButton.setOnClickListener(prev);
    }
}
