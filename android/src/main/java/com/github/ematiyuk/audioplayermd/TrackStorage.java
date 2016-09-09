package com.github.ematiyuk.audioplayermd;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import com.github.ematiyuk.audioplayermd.model.Track;
import com.github.ematiyuk.audioplayermd.service.Settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TrackStorage {
    private static final String TAG = "TrackStorage";

    private List<Track> mTracks;

    private static TrackStorage sTrackStorage;
    private Context mAppContext;

    private TrackStorage(Context appContext) {
        mAppContext = appContext;
        mTracks = new ArrayList<Track>();
    }

    public static TrackStorage get(Context c) {
        if (sTrackStorage == null) {
            sTrackStorage = new TrackStorage(c.getApplicationContext());
        }
        return sTrackStorage;
    }

    public void selfDestroy() {
        sTrackStorage = null;
    }

    public List<Track> getTracks() {
        return mTracks;
    }

    public void scanTracks(Uri uri) {
        mTracks.clear();

        if (uri == null) {
            scanAllTracks();
        } else {
            scanTracksByUri(uri);
        }

        sortBy(Settings.get(mAppContext).retrieveSortOrderPosition());
    }

    private void scanAllTracks() {
        ContentResolver audioResolver = mAppContext.getContentResolver();
        Uri audioUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor audioCursor = audioResolver.query(audioUri, null, null, null, null);

        if(audioCursor != null && audioCursor.moveToFirst()) {
            // get columns
            int titleColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int artistColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            int durationColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.DURATION);

            // add tracks to list
            do {
                long thisId = audioCursor.getLong(idColumn);
                String thisTitle = audioCursor.getString(titleColumn);
                String thisArtist = audioCursor.getString(artistColumn);
                String thisAlbum = audioCursor.getString(albumColumn);
                long thisDuration = audioCursor.getLong(durationColumn);
                mTracks.add(new Track(thisId, thisTitle, thisArtist, thisAlbum, thisDuration));
            }
            while (audioCursor.moveToNext());

            audioCursor.close();
        }
    }

    private void scanTracksByUri(Uri folderUri) {
        ContentResolver audioResolver = mAppContext.getContentResolver();
        Uri audioContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor audioCursor = audioResolver.query(audioContentUri, null,
                MediaStore.Audio.Media.DATA + " LIKE ?",
                new String[] {"%" + folderUri.getPath() + "%"}, null);

        if(audioCursor != null && audioCursor.moveToFirst()) {
            // get columns
            int titleColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int artistColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            int durationColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.DURATION);

            // add tracks to list
            do {
                long thisId = audioCursor.getLong(idColumn);
                String thisTitle = audioCursor.getString(titleColumn);
                String thisArtist = audioCursor.getString(artistColumn);
                String thisAlbum = audioCursor.getString(albumColumn);
                long thisDuration = audioCursor.getLong(durationColumn);
                mTracks.add(new Track(thisId, thisTitle, thisArtist, thisAlbum, thisDuration));
            }
            while (audioCursor.moveToNext());

            audioCursor.close();
        }
    }

    public Track retrieveTrackByUri(Uri audioFileUri) {
        Track returnTrack = null;
        ContentResolver audioResolver = mAppContext.getContentResolver();
        Uri audioContentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor audioCursor = audioResolver.query(audioContentUri, null,
                MediaStore.Audio.Media.DATA + " = ?", new String[] {audioFileUri.getPath()}, null);

        // if cursor is empty moveToFirst() returns false
        if(audioCursor != null && audioCursor.moveToFirst()) {
            // get columns
            int titleColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.TITLE);
            int idColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media._ID);
            int artistColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.ARTIST);
            int albumColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            int durationColumn = audioCursor.getColumnIndex
                    (MediaStore.Audio.Media.DURATION);

            long thisId = audioCursor.getLong(idColumn);
            String thisTitle = audioCursor.getString(titleColumn);
            String thisArtist = audioCursor.getString(artistColumn);
            String thisAlbum = audioCursor.getString(albumColumn);
            long thisDuration = audioCursor.getLong(durationColumn);

            returnTrack = new Track(thisId, thisTitle, thisArtist, thisAlbum, thisDuration);

            audioCursor.close();
        }

        return returnTrack;
    }

    public void sortBy(int orderPosition) {
        switch (orderPosition) {
            // sort by title
            case 0:
                // sort tracks alphabetically based on track title
                Collections.sort(mTracks, new Comparator<Track>() {
                    @Override
                    public int compare(Track track1, Track track2) {
                        return track1.getTitle().compareTo(track2.getTitle());
                    }
                });
                break;
            // sort by artist
            case 1:
                // sort tracks alphabetically based on track artist
                Collections.sort(mTracks, new Comparator<Track>() {
                    @Override
                    public int compare(Track track1, Track track2) {
                        return track1.getArtist().compareTo(track2.getArtist());
                    }
                });
                break;
            // sort by album
            case 2:
                // sort tracks alphabetically based on track album
                Collections.sort(mTracks, new Comparator<Track>() {
                    @Override
                    public int compare(Track track1, Track track2) {
                        return track1.getAlbum().compareTo(track2.getAlbum());
                    }
                });
                break;
            // sort by running time
            case 3:
                // sort tracks in descending mode based on track running time
                Collections.sort(mTracks, new Comparator<Track>() {
                    @Override
                    public int compare(Track track1, Track track2) {
                        return (int) (track2.getDuration() - track1.getDuration());
                    }
                });
                break;
        }
    }

    /**
     * Retrieves current track index by its global identifier.
     *
     * @return current track index
     */
    public int getCurrentTrackIndex() {
        int returnIndex = 0;
        long globalTrackId = Settings.get(mAppContext).retrieveGlobalTrackId();
        int trackListSize = mTracks.size();
        for (int i = 0; i < trackListSize; i++) {
            if (globalTrackId == mTracks.get(i).getId()) {
                returnIndex = i;
                break;
            }
        }
        return returnIndex;
    }
}
