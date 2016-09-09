package com.github.ematiyuk.audioplayermd;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.StringRes;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.github.ematiyuk.audioplayermd.model.Track;

import java.util.ArrayList;
import java.util.List;

public class AudioService extends Service implements
        MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnCompletionListener, AudioManager.OnAudioFocusChangeListener,
        BaseController.MediaPlayerControl {
    private static final String TAG = "AudioService";

    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;
    private List<Track> mTracks;
    private int mTrackPosition;

    private final IBinder audioBinder = new AudioBinder();

    private PlaybackNotification mNotification;
    private boolean mShowNotification = true;

    // for handling incoming phone calls
    private boolean mOngoingCall = false;
    private PhoneStateListener mPhoneStateListener;
    private TelephonyManager mTelephonyManager;

    private OnPlaybackListener mPlaybackListener;

    public interface OnPlaybackListener {
        void onStateChanged(PlayingState state);
        void onTrackChanged(int currPosition);
        void onReportError(@StringRes int resId);
    }

    /**
     * Current playing state of the Service.
     */
    private PlayingState mCurrentState = PlayingState.STOPPED;

    @Override
    public void onCreate() {
        super.onCreate(); // create the service

        mTrackPosition = 0;
        initMediaPlayer();

        mNotification = new PlaybackNotification(this, this);

        // manage incoming phone calls during playback;
        // pause MediaPlayer on incoming call, resume on hang up.
        callStateListener();

        // register the BroadcastReceiver
        registerBecomingNoisyReceiver();
    }

    @Override
    public void onDestroy() {
        if (mShowNotification)
            mNotification.remove();

        releaseMediaPlayer();

        // unregister the BroadcastReceiver
        unregisterReceiver(mBecomingNoisyReceiver);
    }

    public void initMediaPlayer() {

        if (mMediaPlayer == null)
            mMediaPlayer = new MediaPlayer();

        // set up MediaPlayer event listeners
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnCompletionListener(this);

        // configure audio player by setting some of its properties
        mMediaPlayer.setWakeMode(getApplicationContext(),
                PowerManager.PARTIAL_WAKE_LOCK); // lets playback continue when the device becomes idle
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
    }

    public void setOnPlaybackListener(OnPlaybackListener listener) {
        mPlaybackListener = listener;
    }

    public void setList(List<Track> trackList) {
        mTracks = trackList;
    }

    public void preparePlayer() {
        mCurrentState = PlayingState.PREPARING;
        mPlaybackListener.onStateChanged(mCurrentState);

        mMediaPlayer.reset();
        Track currTrack = mTracks.get(mTrackPosition); // get a track

        BaseController.get(getApplicationContext()).updateTrackInfo(currTrack);

        long trackId = currTrack.getId(); // get the track Id
        Uri trackUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, trackId);

        try {
            mMediaPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e(TAG, "Error while setting data source", e);
        }

        try {
            // "For streams, you should call prepareAsync(), which returns immediately,
            // rather than blocking until enough data has been buffered."
            mMediaPlayer.prepareAsync(); // prepare player for playback, asynchronously
        } catch (Exception e) {
            Log.e(TAG, "Error while preparing playback", e);
        }
    }

    public boolean prepareSingleTrack(Uri trackUri) {
        Track track = TrackStorage.get(getApplicationContext()).retrieveTrackByUri(trackUri);
        if (track == null)
            return false;

        mTracks = new ArrayList<Track>();
        mTracks.add(track);

        mMediaPlayer.reset();
        try {
            mMediaPlayer.setDataSource(getApplicationContext(), trackUri);
        } catch (Exception e) {
            Log.e(TAG, "Error while setting data source (single track)", e);
            return false;
        }

        mShowNotification = false;

        try {
            mMediaPlayer.prepareAsync(); // prepare player for playback, asynchronously
        } catch (Exception e) {
            Log.e(TAG, "Error while preparing playback (single track)", e);
            return false;
        }

        return true;
    }

    /**
     * Sets the current track and invokes
     * {@link OnPlaybackListener#onTrackChanged(int) OnPlaybackListener.onTrackChanged(int index)}
     * @param trackIndex current track position (index)
     */
    public void setTrack(int trackIndex) {
        mTrackPosition = trackIndex;

        mPlaybackListener.onTrackChanged(mTrackPosition);
    }

    public int getTrackPosition() {
        return mTrackPosition;
    }

    public Track getCurrentTrack() {
        return mTracks.get(mTrackPosition);
    }

    /**
     * Allows interaction between the Activity and Service classes,
     * for which we also need a Binder instance
     */
    public class AudioBinder extends Binder {
        public AudioService getService() {
            return AudioService.this;
        }
    }

    public int getCurrPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    public boolean isPaused() {
        return mCurrentState == PlayingState.PAUSED;
    }

    public boolean isPreparing() {
        return mCurrentState == PlayingState.PREPARING;
    }

    public boolean isStopped() {
        return mCurrentState == PlayingState.STOPPED;
    }

    public void pausePlayer() {
        if (mCurrentState != PlayingState.PLAYING && mCurrentState != PlayingState.PAUSED)
            return;

        if (mMediaPlayer.isPlaying()) {

            mMediaPlayer.pause();

            mCurrentState = PlayingState.PAUSED;
            mPlaybackListener.onStateChanged(mCurrentState);
        }
    }

    public void stopPlayer() {

        try {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying())
                    mMediaPlayer.stop();

                mCurrentState = PlayingState.STOPPED;
                mPlaybackListener.onStateChanged(mCurrentState);
            }
        } catch (IllegalStateException ise) {
            Log.e(TAG, "Unable to stop player: ", ise);
        }
        removeAudioFocus();

        if (mShowNotification)
            mNotification.remove();

        BaseController.get(getApplicationContext()).resetProgress();
    }

    public void resumePlayer() {
        if (requestAudioFocus()) {
            try {
                mMediaPlayer.start();

                mCurrentState = PlayingState.PLAYING;
                mPlaybackListener.onStateChanged(mCurrentState);
            } catch (IllegalStateException ise) {
                Log.e(TAG, "Unable to resume playback: ", ise);
            }
        } else {
            mPlaybackListener.onReportError(R.string.error_gain_focus_msg);
        }
    }

    public void seek(int pos) {
        mMediaPlayer.seekTo(pos);
    }

    // go to the previous track
    public void skipToPrevious() {
        mTrackPosition--;
        if (mTrackPosition < 0)
            mTrackPosition = mTracks.size() - 1;

        mPlaybackListener.onTrackChanged(mTrackPosition);

        // if playback is stopped just go to the previous track keeping to stay
        // at the same playing state. If state is not STOPPED we prepare playback
        // for playing the previous track.
        if (mCurrentState != PlayingState.STOPPED)
            preparePlayer();
    }

    // go to the next track
    public void skipToNext() {

        // go to the first track (index 0) after the last one
        mTrackPosition = ++mTrackPosition % mTracks.size();

        mPlaybackListener.onTrackChanged(mTrackPosition);

        // if playback is stopped just go to the next track keeping to stay
        // at the same playing state. If state is not STOPPED we prepare playback
        // for playing the next track.
        if (mCurrentState != PlayingState.STOPPED)
            preparePlayer();
    }

    private boolean requestAudioFocus() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = mAudioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN);
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
    }

    private boolean removeAudioFocus() {
        if (mAudioManager == null)
            return false;
        return AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
                mAudioManager.abandonAudioFocus(this);
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        // invokes when the audio focus of the system is updated
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mMediaPlayer == null) initMediaPlayer();
                else if (!mMediaPlayer.isPlaying()) resumePlayer();
                mMediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // lost focus for an unbounded amount of time:
                // stop playback and release media player
                if (mMediaPlayer == null) initMediaPlayer();
                if (mMediaPlayer.isPlaying()) stopPlayer();
                releaseMediaPlayer();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                pausePlayer();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mMediaPlayer.isPlaying()) mMediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }
    }

    private BroadcastReceiver mBecomingNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // pause audio on ACTION_AUDIO_BECOMING_NOISY
            pausePlayer();
        }
    };

    private void registerBecomingNoisyReceiver() {
        // register after getting audio focus
        IntentFilter intentFilter = new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);
        registerReceiver(mBecomingNoisyReceiver, intentFilter);
    }

    // handle incoming phone calls
    private void callStateListener() {
        // get the telephony manager
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        // start listening for PhoneState changes
        mPhoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                switch (state) {
                    // if at least one call exists or the phone is ringing
                    // pause the MediaPlayer
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (mMediaPlayer != null) {
                            pausePlayer();
                            mOngoingCall = true;
                        }
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // phone idle, start playing
                        if (mMediaPlayer != null) {
                            if (mOngoingCall) {
                                mOngoingCall = false;
                                resumePlayer();
                            }
                        }
                        break;
                }
            }
        };
        // register the listener with the telephony manager
        // listen for changes to the device call state
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer == null)
            return;

        mMediaPlayer.release();
        mMediaPlayer = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return audioBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if (mTrackPosition == mTracks.size()-1) {
            stopPlayer();
        } else {
            skipToNext(); // continue playback by playing the next track
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        Log.e(TAG, "->> onError has been invoked! Type: " + what + "; extra code:" + extra);

        mPlaybackListener.onReportError(R.string.error_common_playback_error);
        mediaPlayer.reset();
        return true;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        try {
            if (requestAudioFocus()) {

                mediaPlayer.start();
                mCurrentState = PlayingState.PLAYING;
                mPlaybackListener.onStateChanged(mCurrentState);

                if (mShowNotification)
                    mNotification.set(mTracks.get(mTrackPosition));
            } else {
                mPlaybackListener.onReportError(R.string.error_gain_focus_msg);
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Cannot start playback: ", e);
        }
    }

    /* BaseController.MediaPlayerControl interface methods */
    @Override
    public void start() {
        if (isPaused())
            resumePlayer();
        else
            preparePlayer();
    }

    @Override
    public void pause() {
        pausePlayer();
    }

    @Override
    public void stop() {
        stopPlayer();
    }
    /* End of BaseController.MediaPlayerControl interface methods */
}
