package com.github.ematiyuk.audioplayermd.model;

public class Track {
    private long id;
    private String title;
    private String artist;
    private String album;
    private long duration;

    public Track(long trackId, String trackTitle, String trackArtist,
                 String trackAlbum, long trackDuration) {
        this.id = trackId;
        this.title = trackTitle;
        this.artist = trackArtist;
        this.album = trackAlbum;
        this.duration = trackDuration;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getAlbum() {
        return album;
    }

    public long getDuration() {
        return duration;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
