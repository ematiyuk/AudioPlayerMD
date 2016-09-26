package com.github.ematiyuk.audioplayermd;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.github.ematiyuk.audioplayermd.activities.PlaylistActivity;
import com.github.ematiyuk.audioplayermd.model.Track;

public class PlaybackNotification {

    private static final int NOTIFICATION_ID = 1;

    private Context mContext;
    private Service mService;

    public PlaybackNotification(Context c, Service s) {
        this.mContext = c;
        this.mService = s;
    }

    public void set(Track track) {
        Intent activityIntent = new Intent(mContext, PlaylistActivity.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0,
                activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(mContext);

        builder.setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.ic_notification_top_bar)
                .setTicker(mContext.getString(R.string.playing_string) + ": "
                        + track.getTitle() + " - " + track.getArtist())
                .setOngoing(true)
                .setContentTitle(track.getTitle())
                .setContentText(track.getArtist())
                .setWhen(0);

        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) // if API version >= 16
            notification = builder.build();
        else  // if API version < 16
            notification = builder.getNotification();

        mService.startForeground(NOTIFICATION_ID, notification);
    }

    public void remove() {
        mService.stopForeground(true);
    }
}
