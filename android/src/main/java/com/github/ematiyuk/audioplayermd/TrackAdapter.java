package com.github.ematiyuk.audioplayermd;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.ematiyuk.audioplayermd.model.Track;
import com.github.ematiyuk.audioplayermd.service.TimeFormatter;

import java.util.ArrayList;
import java.util.List;

public class TrackAdapter extends BaseAdapter implements Filterable {
    private static final String TAG = "TrackAdapter";

    private List<Track> mTracks;
    private List<Track> mFilteredTrackList;
    private LayoutInflater mTrackInflater;
    private long mGlobalTrackId;
    private PlayingState mPlayingState;
    private TrackFilter mTrackFilter;

    public TrackAdapter(Context c, List<Track> tracks) {
        this.mTracks = tracks;
        this.mFilteredTrackList = tracks;
        this.mTrackInflater = LayoutInflater.from(c);
        mPlayingState = PlayingState.STOPPED;

        getFilter();
    }

    public void setGlobalTrackId(long globalTrackId) {
        this.mGlobalTrackId = globalTrackId;

        notifyDataSetChanged();
    }

    public void setPlayingState(PlayingState state) {
        mPlayingState = state;
        notifyDataSetChanged();
    }

    public void setTracks(List<Track> tracks) {
        this.mTracks = tracks;
        this.mFilteredTrackList = tracks;
        notifyDataSetChanged();
    }

    public List<Track> getTracks() {
        return mFilteredTrackList;
    }

    @Override
    public int getCount() {
        return mFilteredTrackList.size();
    }

    @Override
    public Track getItem(int position) {
        return mFilteredTrackList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // create a ViewHolder reference
        ViewHolder holder;

        // check to see if the reused view is null or not, if is not null then reuse it
        if(convertView == null) {
            convertView = mTrackInflater.inflate(R.layout.track_list_item, parent, false);

            holder = new ViewHolder();

            // get all views and save them in the view holder
            holder.titleView = (TextView) convertView.findViewById(R.id.trackTitle);
            holder.artistView = (TextView) convertView.findViewById(R.id.trackArtist);
            holder.albumView = (TextView) convertView.findViewById(R.id.trackAlbum);
            holder.durationView = (TextView) convertView.findViewById(R.id.trackDuration);
            holder.iconView = (ImageView) convertView.findViewById(R.id.audioTrackIcon);

            // save the view holder on the cell view to get it back latter
            convertView.setTag(holder);
        } else {
            // the getTag() returns the viewHolder object set as a tag to the view
            holder = (ViewHolder)convertView.getTag();
        }

        Track currTrack = getItem(position);

        // set track parameters to the views
        holder.titleView.setText(currTrack.getTitle());
        holder.artistView.setText(currTrack.getArtist());
        holder.albumView.setText(currTrack.getAlbum());
        holder.durationView.setText(TimeFormatter.format(currTrack.getDuration()));

        if (currTrack.getId() == mGlobalTrackId) {
            switch (mPlayingState) {
                case PLAYING:
                case PREPARING:
                    holder.iconView.setImageResource(R.drawable.ic_control_play_dark);
                    break;
                case PAUSED:
                    holder.iconView.setImageResource(R.drawable.ic_control_pause_dark);
                    break;
                default:
                    holder.iconView.setImageResource(R.drawable.ic_audio_track_dark);
                    break;
            }
            convertView.setActivated(true);
        } else {
            holder.iconView.setImageResource(R.drawable.ic_audio_track_dark);
            convertView.setActivated(false);
        }
        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (mTrackFilter == null) {
            mTrackFilter = new TrackFilter();
        }

        return mTrackFilter;
    }

    public void setFilterIndex(int index) {
        mTrackFilter.setFilterIndex(index);
    }

    /**
     * Used to avoid calling "findViewById" every time the getView() method is called,
     * because this can impact to application performance when the list is large
     */
    private class ViewHolder {
        TextView titleView;
        TextView artistView;
        TextView albumView;
        TextView durationView;
        ImageView iconView;
    }

    private class TrackFilter extends Filter {
        private int mSearchFilterIndex;

        public void setFilterIndex(int index) {
            this.mSearchFilterIndex = index;
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults filterResults = new FilterResults();
            if (constraint != null && constraint.length() > 0) {
                List<Track> tempTrackList = new ArrayList<Track>();
                String filterString = constraint.toString().toLowerCase();

                switch (mSearchFilterIndex) {
                    // filter by title
                    case 0:
                        for (Track track : mTracks) {
                            if (track.getTitle().toLowerCase().contains(filterString)) {
                                tempTrackList.add(track);
                            }
                        }
                        break;
                    // filter by artist
                    case 1:
                        for (Track track : mTracks) {
                            if (track.getArtist().toLowerCase().contains(filterString)) {
                                tempTrackList.add(track);
                            }
                        }
                        break;
                    // filter by album
                    case 2:
                        for (Track track : mTracks) {
                            if (track.getAlbum().toLowerCase().contains(filterString)) {
                                tempTrackList.add(track);
                            }
                        }
                        break;
                }

                filterResults.count = tempTrackList.size();
                filterResults.values = tempTrackList;
            } else {
                filterResults.count = mTracks.size();
                filterResults.values = mTracks;
            }

            return filterResults;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults filterResults) {
            mFilteredTrackList = (List<Track>) filterResults.values;
            notifyDataSetChanged();
        }
    }
}