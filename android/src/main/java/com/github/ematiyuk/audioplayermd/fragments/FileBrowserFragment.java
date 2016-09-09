package com.github.ematiyuk.audioplayermd.fragments;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import com.github.ematiyuk.audioplayermd.R;
import com.nononsenseapps.filepicker.FilePickerFragment;
import com.nononsenseapps.filepicker.LogicHandler;

import java.io.File;

public class FileBrowserFragment extends FilePickerFragment {

    // file extensions to filter on
    private static final String EXTENSION_MP3 = ".mp3";
    private static final String EXTENSION_M4A = ".m4a";
    private static final String EXTENSION_AAC = ".aac";
    private static final String EXTENSION_OGG = ".ogg";
    private static final String EXTENSION_WAV = ".wav";

    /**
     *
     * @param file
     * @return The file extension. If file has no extension, it returns null.
     */
    private String getExtension(@NonNull File file) {
        String path = file.getPath();
        int i = path.lastIndexOf(".");
        if (i < 0) {
            return null;
        } else {
            return path.substring(i);
        }
    }

    @Override
    protected boolean isItemVisible(final File file) {
        boolean ret = super.isItemVisible(file);
        if (ret && !isDir(file) && (mode == MODE_FILE || mode == MODE_FILE_AND_DIR)) {
            String ext = getExtension(file);
            return ext != null && (EXTENSION_MP3.equalsIgnoreCase(ext) ||
                    EXTENSION_M4A.equalsIgnoreCase(ext) ||
                    EXTENSION_AAC.equalsIgnoreCase(ext) ||
                    EXTENSION_OGG.equalsIgnoreCase(ext) ||
                    EXTENSION_WAV.equalsIgnoreCase(ext));
        }
        return ret;
    }

    /**
     * @param parent Containing view
     * @param viewType which the ViewHolder will contain. Will be one of:
     * [VIEWTYPE_HEADER, VIEWTYPE_CHECKABLE, VIEWTYPE_DIR]. It is OK, and even expected, to use the same
     * layout for VIEWTYPE_HEADER and VIEWTYPE_DIR.
     * @return a view holder for a file or directory (the difference is presence of checkbox).
     */
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v;
        switch (viewType) {
            case LogicHandler.VIEWTYPE_HEADER:
                v = LayoutInflater.from(getActivity()).inflate(R.layout.longer_listitem_dir,
                        parent, false);
                return new HeaderViewHolder(v);
            case LogicHandler.VIEWTYPE_CHECKABLE:
                v = LayoutInflater.from(getActivity()).inflate(R.layout.longer_listitem_checkable,
                        parent, false);
                CheckBox chb = (CheckBox) v.findViewById(R.id.checkbox);
                chb.setEnabled(false); // disable checkbox
                chb.setAlpha(0.0f); // hide checkbox having set it transparent
                return new CheckableViewHolder(v);
            case LogicHandler.VIEWTYPE_DIR:
            default:
                v = LayoutInflater.from(getActivity()).inflate(R.layout.longer_listitem_dir,
                        parent, false);
                return new DirViewHolder(v);
        }
    }

    /**
     * For consistency, the top level the back button checks against should be the start path.
     * But it will fall back on /.
     */
    public File getBackTop() {
        return getPath(getArguments().getString(KEY_START_PATH, "/"));
    }

    /**
     * @return true if the current path is the startpath or /
     */
    public boolean isBackTop() {
        return 0 == compareFiles(mCurrentPath, getBackTop()) ||
                0 == compareFiles(mCurrentPath, new File("/"));
    }

    /**
     * Go up on level, same as pressing on "..".
     */
    public void goUp() {
        mCurrentPath = getParent(mCurrentPath);
        mCheckedItems.clear();
        mCheckedVisibleViewHolders.clear();
        refresh(mCurrentPath);
    }
}
