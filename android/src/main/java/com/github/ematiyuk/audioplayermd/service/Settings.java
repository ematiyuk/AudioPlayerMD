package com.github.ematiyuk.audioplayermd.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

public class Settings {

    private static Settings sSettings;
    private Context mAppContext;

    private Settings(Context appContext) {
        mAppContext = appContext;
    }

    public static Settings get(Context c) {
        if (sSettings == null) {
            sSettings = new Settings(c.getApplicationContext());
        }
        return sSettings;
    }

    public void saveSearchFilterPosition(int pos) {
        SharedPreferences settings = mAppContext.getSharedPreferences("search_filter_options_position", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("option_selected", pos);
        editor.commit();
    }

    public int retrieveSearchFilterPosition() {
        SharedPreferences settings = mAppContext.getSharedPreferences("search_filter_options_position", 0);
        return settings.getInt("option_selected", 0);
    }


    public void saveSortOrderPosition(int pos) {
        SharedPreferences settings = mAppContext.getSharedPreferences("sort_options_position", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("option_selected", pos);
        editor.commit();
    }

    public int retrieveSortOrderPosition() {
        SharedPreferences settings = mAppContext.getSharedPreferences("sort_options_position", 0);
        return settings.getInt("option_selected", 0);
    }

    public void saveGlobalTrackId(long id) {
        SharedPreferences settings = mAppContext.getSharedPreferences("global_track_id", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong("track_id", id);
        editor.commit();
    }

    public long retrieveGlobalTrackId() {
        SharedPreferences settings = mAppContext.getSharedPreferences("global_track_id", 0);
        return settings.getLong("track_id", 0);
    }

    public void saveCurrentFolderUri(Uri folderUri) {
        SharedPreferences settings = mAppContext.getSharedPreferences("folder_uri", 0);
        SharedPreferences.Editor editor = settings.edit();
        String folderUriString;
        folderUriString = (folderUri == null) ? "" : folderUri.toString();
        editor.putString("folder_uri", folderUriString);
        editor.commit();
    }

    public Uri retrieveCurrentFolderUri() {
        SharedPreferences settings = mAppContext.getSharedPreferences("folder_uri", 0);
        String folderUriString = settings.getString("folder_uri", "");
        return (folderUriString.isEmpty()) ? null : Uri.parse(folderUriString);
    }
}
