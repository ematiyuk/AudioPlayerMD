package com.github.ematiyuk.audioplayermd.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ematiyuk.audioplayermd.AudioService;
import com.github.ematiyuk.audioplayermd.BaseController;
import com.github.ematiyuk.audioplayermd.PlayingState;
import com.github.ematiyuk.audioplayermd.R;
import com.github.ematiyuk.audioplayermd.model.Track;
import com.github.ematiyuk.audioplayermd.service.TimeFormatter;

public class TrackPlaybackDialog extends DialogFragment
        implements BaseController.Callback, AudioService.OnPlaybackListener {
    private static final String TAG = "TrackPlaybackDialog";

    public static final String EXTRA_TRACK_URI = "com.github.ematiyuk.audioplayermd.track_uri";

    private TextView mArtistTextView, mTitleTextView;
    private ImageButton mPlayPauseButton;
    private TextView mTotalTimeTextView, mPlayedTimeTextView;
    private SeekBar mSeekBar;
    private ProgressBar mLoadingProgressBar;

    private boolean mDragging;

    private AudioService mAudioService;
    private boolean mServiceBound;
    private Intent mAudioServiceIntent;
    private Uri mTrackUri;

    // handler to update UI timer, SeekBar etc.
    private Handler mSeekHandler = new Handler();

    public static TrackPlaybackDialog newInstance(Uri trackUri) {
        TrackPlaybackDialog dialog = new TrackPlaybackDialog();

        // supply trackUri input as an argument
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_TRACK_URI, trackUri);
        dialog.setArguments(args);

        dialog.setStyle(DialogFragment.STYLE_NO_TITLE, 0);

        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTrackUri = getArguments().getParcelable(EXTRA_TRACK_URI);

        BaseController.get(getActivity()).setCallbackListener(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.track_playback_layout, container, false);

        mTitleTextView = (TextView) rootView.findViewById(R.id.controllerTrackTitle);
        mArtistTextView = (TextView) rootView.findViewById(R.id.controllerTrackArtist);
        mPlayPauseButton = (ImageButton) rootView.findViewById(R.id.playPauseButton);
        mSeekBar = (SeekBar) rootView.findViewById(R.id.controllerProgress);
        mTotalTimeTextView = (TextView) rootView.findViewById(R.id.controllerTotalTime);
        mPlayedTimeTextView = (TextView) rootView.findViewById(R.id.controllerPlayedTime);
        mLoadingProgressBar = (ProgressBar) rootView.findViewById(R.id.loadingProgressBar);

        mPlayPauseButton.setOnClickListener(mPlayPauseListener);
        mSeekBar.setOnSeekBarChangeListener(mSeekListener);

        // initialization section
        mPlayedTimeTextView.setText(TimeFormatter.format(0));
        mTotalTimeTextView.setText(TimeFormatter.format(0));
        mSeekBar.setProgress(0);
        mSeekBar.setMax((int) BaseController.get(getActivity()).getProgressMaxValue());
        mLoadingProgressBar.setVisibility(View.GONE);

        // set focus to the textviews to enable moving text
        mTitleTextView.setSelected(true);
        mArtistTextView.setSelected(true);

        return rootView;
    }

    @Override
    public void onPause() {
        super.onPause();
        mSeekHandler.removeCallbacks(mUpdateTimeTask);
        stopAudioService();
        getActivity().finish();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // set the dialog width to match_parent of its host activity
        WindowManager.LayoutParams params = getDialog().getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.MATCH_PARENT;
        getDialog().getWindow().setAttributes(params);

        // disable closing the dialog on touch outside it
        getDialog().setCanceledOnTouchOutside(false);

        startAudioService();
    }

    @Override
    public void onResume() {
        super.onResume();

        // show loading progress bar
        mLoadingProgressBar.setVisibility(View.VISIBLE);

        // run playing with 1000 ms delay to avoid NPE of audioService,
        // 'cause binding process takes some time
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mAudioService.prepareSingleTrack(mTrackUri)) {
                    onUpdateTrackInfo(mAudioService.getCurrentTrack());
                    onUpdateSeekBar();

                    // hide loading progress bar
                    mLoadingProgressBar.setVisibility(View.GONE);
                } else { // if it failed to play the track then show a toast msg and close the app
                    Toast.makeText(getActivity(), R.string.error_playing_track_toast_msg,
                            Toast.LENGTH_LONG).show();
                    dismiss(); // close the dialog
                }
            }
        }, 1000L);
    }

    @Override
    public void onDestroy() {
        mSeekHandler.removeCallbacks(mUpdateTimeTask);
        stopAudioService();
        getActivity().finish();
        super.onDestroy();
    }

    private ServiceConnection mAudioConnection = new ServiceConnection() {

        // The callback method will inform the class when the Activity instance
        // has successfully connected to the Service instance
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            AudioService.AudioBinder binder = (AudioService.AudioBinder) iBinder;
            mAudioService = binder.getService(); // get a reference to the Service instance
//            mAudioService.setOnPlaybackListener(TrackPlaybackDialog.this);
            mAudioService.setOnPlaybackListener(TrackPlaybackDialog.this);
            BaseController.get(getActivity()).setAudioService(mAudioService);
            BaseController.get(getActivity()).setMediaPlayer(mAudioService);

            mServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mServiceBound = false;
        }
    };

    private void startAudioService() {
        if (!mServiceBound) {
            // if the Intent object already exists - return
            if (mAudioServiceIntent != null)
                return;

            if (mAudioService != null)
                return;

            // create an intent to bind AudioConnection with AudioService
            mAudioServiceIntent = new Intent(getActivity(), AudioService.class);
            getActivity().bindService(mAudioServiceIntent, mAudioConnection, Context.BIND_AUTO_CREATE);
            getActivity().startService(mAudioServiceIntent);
        }
    }

    private void stopAudioService() {
        if (mServiceBound) {
            if (mAudioServiceIntent == null)
                return;

            getActivity().stopService(mAudioServiceIntent);
            getActivity().unbindService(mAudioConnection);

//        BaseController.get(getActivity()).destroyController();

            mAudioServiceIntent = null;

            mAudioService = null;
        }
    }

    private Runnable mUpdateTimeTask = new Runnable() {
        @Override
        public void run() {
            if (BaseController.getAudioService() != null) {
                long duration = BaseController.getAudioService().getDuration();
                long currentPosition = BaseController.getAudioService().getCurrPosition();
                if (BaseController.getAudioService().isPlaying()) {
                    long pos = BaseController.get(getActivity()).getProgressMaxValue() * currentPosition / duration;
                    mSeekBar.setProgress((int) pos);
                    mPlayedTimeTextView.setText(TimeFormatter.format(currentPosition));
                }
            }

            updatePlayPauseIcon(BaseController.getAudioService().isPlaying());

            if (!mDragging) {
                mSeekHandler.postDelayed(this, 500L);
            }
        }
    };

    private View.OnClickListener mPlayPauseListener = new View.OnClickListener() {
        public void onClick(View v) {
            BaseController.get(getActivity()).doPlayPause();

            onUpdateSeekBar();
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
            if (!fromUser) {
                // We're not interested in programmatically initiated changes
                // to the SeekBar's position.
                return;
            }

            mPlayedTimeTextView.setText(TimeFormatter.format((int) BaseController.get(getActivity())
                    .getPositionOnProgressChanged(progress)));
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            mDragging = false;

            onUpdateSeekBar();
        }
    };

    private void updatePlayPauseIcon(boolean isPlaying) {
        if (isPlaying) {
            mPlayPauseButton.setImageResource(R.drawable.ic_control_pause);
        } else {
            mPlayPauseButton.setImageResource(R.drawable.ic_control_play);
        }
    }

    /* BaseController.Callback interface methods */
    @Override
    public void onUpdateTrackInfo(Track currentTrack) {
        mTitleTextView.setText(currentTrack.getTitle());
        mArtistTextView.setText(currentTrack.getArtist());
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
    }

    @Override
    public void onSetPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
    }
    /* End of BaseController.Callback interface methods */

    /* AudioService.OnPlaybackListener interface methods */
    @Override
    public void onStateChanged(PlayingState state) {

    }

    @Override
    public void onTrackChanged(int currPosition) {

    }

    @Override
    public void onReportError(@StringRes int resId) {

    }
    /* End of AudioService.OnPlaybackListener interface methods */
}
