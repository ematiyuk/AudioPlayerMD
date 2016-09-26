package com.github.ematiyuk.audioplayermd.fragments;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.github.ematiyuk.audioplayermd.AudioService;
import com.github.ematiyuk.audioplayermd.BaseController;
import com.github.ematiyuk.audioplayermd.PlayingState;
import com.github.ematiyuk.audioplayermd.R;
import com.github.ematiyuk.audioplayermd.service.Settings;
import com.github.ematiyuk.audioplayermd.model.Track;
import com.github.ematiyuk.audioplayermd.TrackAdapter;
import com.github.ematiyuk.audioplayermd.TrackStorage;
import com.github.ematiyuk.audioplayermd.activities.FileBrowserActivity;
import com.nononsenseapps.filepicker.FilePickerActivity;

public class PlaylistFragment extends Fragment
        implements AdapterView.OnItemClickListener, AudioService.OnPlaybackListener,
        SearchView.OnQueryTextListener {
    private static final String TAG = "PlaylistFragment";

    private static final int FILE_CODE = 0;

    private ListView mTrackView;
    private TrackAdapter mTrackAdapter;
    private static AudioService mAudioService;
    private boolean mServiceBound = false;

    private boolean mIsFirstTrackScanning;

    private ProgressDialog mProgressDialog;
    private boolean mSmoothListScroll = false;
    private boolean mSearchActivated = false;
    private boolean mPlaylistChanged = false;

    private SearchView mSearchView;
    private MenuItem mSortMenuItem;
    private MenuItem mFilterMenuItem;
    private int mSearchFilterIndex;
    private Intent mAudioServiceIntent;
    private long mLastTrackId;
    private MenuItem mSelectDirMenuItem;
    private Uri mCurrentUri;

    private Callbacks mCallbacks;

    public interface Callbacks {
        void onControllerShow();
        void onControllerHide();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mCallbacks = (Callbacks) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true); // turn on the options menu handling

        mIsFirstTrackScanning = true;
    }

    private void setActivityTitle(CharSequence title) {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        ActionBar actionBar = activity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    @Override
    public void onDestroy() {
        stopAudioService();
        TrackStorage.get(getActivity()).selfDestroy();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        super.onPause();

        hidePlaybackControls();
    }

    @Override
    public void onResume() {
        super.onResume();

        showPlaybackControls();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tracklist_layout, container, false);

        mTrackView = (ListView) rootView.findViewById(R.id.trackList);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mCurrentUri = Settings.get(getActivity()).retrieveCurrentFolderUri();
        // initial track scanning
        scanTracks(mCurrentUri);

        if (mCurrentUri == null)
            setActivityTitle(getString(R.string.all_songs_string));
        else
            setActivityTitle(mCurrentUri.getLastPathSegment());

        mLastTrackId = Settings.get(getActivity()).retrieveGlobalTrackId();
    }

    private ServiceConnection mAudioConnection = new ServiceConnection() {

        // The callback method will inform the class when the Activity instance
        // has successfully connected to the Service instance
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            AudioService.AudioBinder binder = (AudioService.AudioBinder) iBinder;
            mAudioService = binder.getService(); // get a reference to the Service instance
            mAudioService.setList(TrackStorage.get(getActivity()).getTracks()); // pass track list to service
            mAudioService.setOnPlaybackListener(PlaylistFragment.this);
            BaseController.get(getActivity()).setAudioService(mAudioService);
            BaseController.get(getActivity()).setMediaPlayer(mAudioService);
            BaseController.get(getActivity()).setPrevNextListeners(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playNext();
                }
            }, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playPrevious();
                }
            });
            final int currTrackIndex = TrackStorage.get(getActivity()).getCurrentTrackIndex();
            if (!TrackStorage.get(getActivity()).getTracks().isEmpty()) {
                Track currTrack = TrackStorage.get(getActivity()).getTracks().get(currTrackIndex);
                BaseController.get(getActivity()).updateTrackInfo(currTrack);
                mTrackAdapter.setGlobalTrackId(currTrack.getId());
            }
            mAudioService.setTrack(currTrackIndex);

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
            getActivity().unbindService(mAudioConnection);

            mAudioService.stopSelf();

            mAudioService = null;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_menu, menu);

        MenuItem searchMenuItem = menu.findItem(R.id.actionSearch);
        mSortMenuItem = menu.findItem(R.id.actionSort);
        mFilterMenuItem = menu.findItem(R.id.actionFilter);
        mSelectDirMenuItem = menu.findItem(R.id.actionSelectDirectory);
        mSortMenuItem.setVisible(true);
        mFilterMenuItem.setVisible(false);
        mSelectDirMenuItem.setVisible(true);

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                mSearchActivated = true;
                mFilterMenuItem.setVisible(true);
                return true;
            }

            // invokes after collapsing of the SearchView
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                mSearchActivated = false;
                if (!TrackStorage.get(getActivity()).getTracks().isEmpty()) {
                    mTrackAdapter.setTracks(TrackStorage.get(getActivity()).getTracks());

                    updateMainPlaylistPlayback();
                }

                mSortMenuItem.setVisible(true);
                mFilterMenuItem.setVisible(false);
                mSelectDirMenuItem.setVisible(true);

                return true;
            }
        });

        mSearchView = (SearchView) MenuItemCompat.getActionView(searchMenuItem);

        SearchManager searchManager = (SearchManager) getActivity()
                .getSystemService(Context.SEARCH_SERVICE);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getActivity().getComponentName()));

        mSearchView.setOnQueryTextListener(this);

        // show "search" icon on the left of the search field
        mSearchView.setIconifiedByDefault(false);

        mSearchView.setQueryHint(getQueryHintByPosition(Settings.get(getActivity())
                .retrieveSearchFilterPosition()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.actionSearch:
                mSortMenuItem.setVisible(false);
                mSelectDirMenuItem.setVisible(false);
                return true;
            case R.id.actionSort:
                showSortDialog();
                return true;
            case R.id.actionSelectDirectory:
                showPlaylistModeDialog();
                return true;
            case R.id.actionFilter:
                mSearchFilterIndex = Settings.get(getActivity()).retrieveSearchFilterPosition();
                showSearchFilterDialog();
                return true;
            default:
                return false;
        }
    }

    private void startFileBrowserActivity() {
        Intent intent = new Intent(getActivity(), FileBrowserActivity.class);

        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false);
        intent.putExtra(FilePickerActivity.EXTRA_ALLOW_CREATE_DIR, false);
        intent.putExtra(FilePickerActivity.EXTRA_MODE, FilePickerActivity.MODE_FILE_AND_DIR);
        intent.putExtra(FilePickerActivity.EXTRA_SINGLE_CLICK, false);

        intent.putExtra(FilePickerActivity.EXTRA_START_PATH,
                Environment.getExternalStorageDirectory().getPath());

        startActivityForResult(intent, FILE_CODE);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CODE && resultCode == Activity.RESULT_OK) {
            if (!data.getBooleanExtra(FilePickerActivity.EXTRA_ALLOW_MULTIPLE, false)) {
                mCurrentUri = data.getData();
                scanTracks(mCurrentUri);
                setActivityTitle(mCurrentUri.getLastPathSegment());
            }
        }
    }

    private void showPlaybackControls() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mCallbacks.onControllerShow();
            }
        }, 1L);
    }

    private void hidePlaybackControls() {
        mCallbacks.onControllerHide();
    }

    /* SearchView.OnQueryTextListener interface methods */

    // the method is invoked when user presses the search (submit) button
    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    // the method is invoked when user changes the text
    @Override
    public boolean onQueryTextChange(String newText) {
        mTrackAdapter.setFilterIndex(mSearchFilterIndex);
        mTrackAdapter.getFilter().filter(newText);
        return true;
    }
    /* End of SearchView.OnQueryTextListener interface methods */

    private void showSearchFilterDialog() {
        final AlertDialog searchFilterDialog;

        // retrieve strings to show in Dialog with RadioButtons
        String[] items = getResources().getStringArray(R.array.search_filter_options);

        // creating and building the SearchFilterDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.search_filter_dialog_title)
                .setIcon(R.drawable.ic_action_filter_dark)
                .setSingleChoiceItems(items, mSearchFilterIndex, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int filterPosition) {
                        // set query hint according to selected filter position
                        mSearchView.setQueryHint(getQueryHintByPosition(filterPosition));

                        // save selected option position
                        Settings.get(getActivity()).saveSearchFilterPosition(filterPosition);

                        mSearchFilterIndex = filterPosition;

                        // clear search view text field
                        mSearchView.setQuery("", false);
                        // thus set focus and soft input keyboard
                        mSearchView.setIconified(false);

                        if (!TrackStorage.get(getActivity()).getTracks().isEmpty())
                            updateMainPlaylistPlayback();

                        dialog.dismiss();
                    }
                });

        searchFilterDialog = builder.create();
        searchFilterDialog.show();
        // sets the Dialog background color
        searchFilterDialog.getWindow().setBackgroundDrawableResource(R.color.dialogBackground);
    }

    private void updateMainPlaylistPlayback() {
        mAudioService.setList(TrackStorage.get(getActivity()).getTracks());
        mPlaylistChanged = false;
        int currTrackIndex = TrackStorage.get(getActivity()).getCurrentTrackIndex();
        mTrackAdapter.setGlobalTrackId(mLastTrackId);
        mAudioService.setTrack(currTrackIndex);
        mTrackView.setSelection(currTrackIndex);
    }

    private void initAfterPlaylistReloaded() {
        mAudioService.stopPlayer();
        if (!TrackStorage.get(getActivity()).getTracks().isEmpty()) {
            Track selectedTrack = TrackStorage.get(getActivity()).getTracks().get(0);
            mLastTrackId = selectedTrack.getId();
            mTrackAdapter.setTracks(TrackStorage.get(getActivity()).getTracks());
            mTrackAdapter.setGlobalTrackId(mLastTrackId);
            Settings.get(getActivity()).saveGlobalTrackId(mLastTrackId);
            mAudioService.setList(TrackStorage.get(getActivity()).getTracks());
            mAudioService.setTrack(0);
            mTrackView.setSelection(0);
        }
    }

    private CharSequence getQueryHintByPosition(int filterPosition) {
        switch (filterPosition) {
            case 0:
                return getString(R.string.search_by_title_hint);
            case 1:
                return getString(R.string.search_by_artist_hint);
            case 2:
                return getString(R.string.search_by_album_hint);
            default:
                return getString(R.string.search_by_title_hint);
        }
    }

    private void showSortDialog() {
        final AlertDialog sortDialog;

        // retrieve strings to show in Dialog with RadioButtons
        final CharSequence[] items = getResources().getStringArray(R.array.sort_by_options);

        // get saved sort order position, if it hasn't been saved - return zero position
        int selectedIndex = Settings.get(getActivity()).retrieveSortOrderPosition();

        // creating and building the SortDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.sort_dialog_title)
                .setIcon(R.drawable.ic_action_sort_dark)
                .setSingleChoiceItems(items, selectedIndex, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, int orderPosition) {
                        // perform sorting by selected order
                        TrackStorage.get(getActivity()).sortBy(orderPosition);

                        // save selected option position
                        Settings.get(getActivity()).saveSortOrderPosition(orderPosition);

                        // update ListView by notifying the track adapter
                        mTrackAdapter.notifyDataSetChanged();

                        // retrieve current track index and set for playback
                        final int currentTrackIndex = TrackStorage.get(getActivity())
                                .getCurrentTrackIndex();
                        mAudioService.setTrack(currentTrackIndex);

                        // scroll to the current track instantly (no smooth scrolling)
                        mTrackView.setSelection(currentTrackIndex);

                        dialog.dismiss();
                    }
                });

        sortDialog = builder.create();
        sortDialog.show();
        // sets the Dialog background color
        sortDialog.getWindow().setBackgroundDrawableResource(R.color.dialogBackground);
    }

    private void showPlaylistModeDialog() {
        final AlertDialog playlistModeDialog;

        // retrieve strings to show in Dialog as items
        final CharSequence[] items = getResources().getStringArray(R.array.songs_display_mode_options);

        // creating and building the PlaylistModeDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.playlist_mode_dialog_title)
                .setIcon(R.drawable.ic_action_select_playlist_mode)
                .setItems(items, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int modePosition) {
                        switch (modePosition) {
                            // display all songs mode
                            case 0:
                                scanTracks(null);
                                setActivityTitle(getString(R.string.all_songs_string));
                                break;
                            // display all songs from a selected directory
                            // (start file browser activity)
                            case 1:
                                startFileBrowserActivity();
                                break;
                        }

                        dialog.dismiss();
                    }
                });

        playlistModeDialog = builder.create();
        playlistModeDialog.show();
        // sets the Dialog background color
        playlistModeDialog.getWindow().setBackgroundDrawableResource(R.color.dialogBackground);
    }

    private void playNext() {
        mAudioService.skipToNext();
    }

    private void playPrevious() {
        mAudioService.skipToPrevious();
    }

    /* AdapterView.OnItemClickListener method  - is called when we click upon list item */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mSearchActivated) {
            mAudioService.setList(mTrackAdapter.getTracks());
            mPlaylistChanged = true;
        }
        mAudioService.setTrack(position);
        mAudioService.preparePlayer();

        BaseController.get(getActivity()).updateProgress();

        mLastTrackId = mTrackAdapter.getItem(position).getId();
        showPlaybackControls();
    }

    @Override
    public void onStateChanged(PlayingState state) {
        mTrackAdapter.setPlayingState(state);
    }

    @Override
    public void onTrackChanged(int currPosition) {
        if (TrackStorage.get(getActivity()).getTracks().isEmpty())
            return;

        Track currTrack;
        if (mSearchActivated && mPlaylistChanged) {
            mAudioService.setList(mTrackAdapter.getTracks());
            currTrack = mTrackAdapter.getItem(currPosition);
        } else {
            currTrack = TrackStorage.get(getActivity()).getTracks().get(currPosition);
        }

        BaseController.get(getActivity()).updateTrackInfo(currTrack);

        if (!(mSearchActivated && !mPlaylistChanged)) {
            if (mSmoothListScroll) {
                doSmoothScrollToPosition(currPosition);
            } else {
                mTrackView.setSelection(currPosition);
            }
        }
        mSmoothListScroll = true;

        mTrackAdapter.setGlobalTrackId(currTrack.getId());

        // save current track id as global one
        Settings.get(getActivity()).saveGlobalTrackId(currTrack.getId());
    }

    @Override
    public void onReportError(@StringRes int resId) {
        Toast.makeText(getActivity(), resId, Toast.LENGTH_LONG).show();
    }

    /**
     * Smoothly scrolls to specified position.
     * Keeps selected list item always visible.
     *
     * @param position for scrolling to
     */
    private void doSmoothScrollToPosition(final int position) {
        mTrackView.postDelayed(new Runnable() {
            @Override
            public void run() {
                // scrolls to specified position after delay of 400 ms
                mTrackView.smoothScrollToPosition(position);
            }
        }, 400L);
    }

    private void scanTracks(Uri folderUri) {
        Settings.get(getActivity()).saveCurrentFolderUri(folderUri);
        new AsyncTrackScanner().execute(folderUri);
    }

    private class AsyncTrackScanner extends AsyncTask<Uri, Integer, String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            mProgressDialog = new ProgressDialog(getActivity());
            mProgressDialog.setMessage(getString(R.string.scanning_progress_msg));
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(Uri... params) {
            Uri folderUri = params[0];
            try {
                TrackStorage.get(getActivity()).scanTracks(folderUri);
                return getString(R.string.scanning_completed);
            } catch (Exception e) {
                Log.e(TAG, getString(R.string.error_scanning_tracks));
                e.printStackTrace();
                return getString(R.string.error_scanning_tracks);
            }
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Toast.makeText(getActivity(), result, Toast.LENGTH_SHORT).show();

            if (mIsFirstTrackScanning) {
                mTrackAdapter = new TrackAdapter(getActivity(),
                        TrackStorage.get(getActivity()).getTracks());

                mTrackView.setAdapter(mTrackAdapter);
                mTrackView.setTextFilterEnabled(false); // disables search view popup text
                mTrackView.setOnItemClickListener(PlaylistFragment.this);
                mTrackView.setFastScrollEnabled(true); // enable fast scroll thumb on the right

                startAudioService();
            } else {
                initAfterPlaylistReloaded();
            }

            mTrackView.setSelection(TrackStorage.get(getActivity()).getCurrentTrackIndex());

            if (TrackStorage.get(getActivity()).getTracks().isEmpty())
                hidePlaybackControls();
            else
                showPlaybackControls();

            mIsFirstTrackScanning = false;

            if (mProgressDialog.isShowing())
                mProgressDialog.dismiss();
        }
    }
}
