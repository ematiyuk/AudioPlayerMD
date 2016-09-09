package com.github.ematiyuk.audioplayermd;

import android.content.Context;
import android.view.View;

import com.github.ematiyuk.audioplayermd.model.Track;

public class BaseController {
    private static final String TAG = "BaseController";

    private static BaseController sBaseController;
    private Context mAppContext;

    private static AudioService mAudioService;
    private MediaPlayerControl mPlayer;
    private long mTotalTime;
    private final long mProgressMax = 1000L;

    private Callback mCallback;

    private BaseController(Context appContext) {
        mAppContext = appContext;
    }

    public static BaseController get(Context c) {
        if (sBaseController == null) {
            sBaseController = new BaseController(c.getApplicationContext());
        }
        return sBaseController;
    }

    public void setAudioService(AudioService audioService) {
        mAudioService = audioService;
    }

    public static AudioService getAudioService() {
        return mAudioService;
    }

    public void setMediaPlayer(MediaPlayerControl player) {
        mPlayer = player;
    }

    public void setCallbackListener(Callback listener) {
        mCallback = listener;
    }

    public long getTotalTime() {
        return mTotalTime;
    }

    public long getProgressMaxValue() {
        return mProgressMax;
    }

    public long getPositionOnProgressChanged(int progress) {
        long duration;
        duration = mAudioService.getDuration();
        long newPosition = (duration * progress) / mProgressMax;
        mAudioService.seek((int) newPosition);
        return newPosition;
    }

    public void doPlayPause() {
        if (mPlayer == null) {
            return;
        }

        if (mAudioService.isPlaying()) {
            mTotalTime = mAudioService.getDuration();
            mPlayer.pause();
        } else {
            mPlayer.start();
        }

        mCallback.onUpdateSeekBar();
    }

    public void doStop() {
        if (mPlayer == null) {
            return;
        }

        mPlayer.stop();

        mCallback.onResetProgress();
    }

    public void resetProgress() {
        mCallback.onResetProgress();
    }

    public void updateProgress() {
        mCallback.onUpdateSeekBar();
    }

    public void setPrevNextListeners(View.OnClickListener next, View.OnClickListener prev) {
        mCallback.onSetPrevNextListeners(next, prev);
    }

    public void updateTrackInfo(Track track) {
        mCallback.onUpdateTrackInfo(track);
    }

    public interface Callback {
        void onUpdateTrackInfo(Track currentTrack);
        void onUpdateSeekBar();
        void onResetProgress();
        void onSetPrevNextListeners(View.OnClickListener next,
                                    View.OnClickListener prev);
    }

    public void destroySelf() {
        mAudioService = null;
        sBaseController = null;
    }

    public interface MediaPlayerControl {
        void start();

        void pause();

        void stop();
    }
}
