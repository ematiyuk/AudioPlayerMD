package com.github.ematiyuk.audioplayermd.activities;

import android.os.Environment;

import com.github.ematiyuk.audioplayermd.fragments.FileBrowserFragment;
import com.nononsenseapps.filepicker.AbstractFilePickerFragment;
import com.nononsenseapps.filepicker.FilePickerActivity;

import java.io.File;

public class FileBrowserActivity extends FilePickerActivity {

    public FileBrowserActivity() {
        super();
    }

    /**
     * Need access to the fragment
     */
    FileBrowserFragment currentFragment;

    /**
     * Return a copy of the new fragment and set the variable above.
     */
    @Override
    protected AbstractFilePickerFragment<File> getFragment(
            final String startPath, final int mode, final boolean allowMultiple,
            final boolean allowDirCreate, final boolean allowExistingFile,
            final boolean singleClick) {

        // startPath is allowed to be null.
        // In that case, default folder should be SD-card and not "/"
        String path = (startPath != null ? startPath
                : Environment.getExternalStorageDirectory().getPath());

        currentFragment = new FileBrowserFragment();
        currentFragment.setArgs(path, mode, allowMultiple, allowDirCreate,
                allowExistingFile, singleClick);
        return currentFragment;
    }

    /**
     * Override the back-button.
     */
    @Override
    public void onBackPressed() {
        // If at top most level, normal behaviour
        if (currentFragment.isBackTop()) {
            super.onBackPressed();
        } else {
            // Else go up
            currentFragment.goUp();
        }
    }

//    @Override
//    protected AbstractFilePickerFragment<File> getFragment(@Nullable String startPath, int mode, boolean allowMultiple, boolean allowCreateDir, boolean allowExistingFile, boolean singleClick) {
//        // load our custom fragment here
//        AbstractFilePickerFragment<File> fragment = new FileBrowserFragment();
//        // startPath is allowed to be null. In that case, default folder should be SD-card and not "/"
//        fragment.setArgs(startPath != null ? startPath : Environment.getExternalStorageDirectory().getPath(),
//                mode, allowMultiple, allowCreateDir, allowExistingFile, singleClick);
//        return fragment;
//    }
}
