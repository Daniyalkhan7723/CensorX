package edu.metrostate.setexoplayer;

import android.net.Uri;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.util.Util;

public class SetExoPlayer {

    TrackSelector trackSelector;
    DefaultBandwidthMeter defaultBandwidthMeter;
    MediaSource mediaSource;
    SimpleExoPlayer player;

    void setExoPlayer() {
        trackSelector = new DefaultTrackSelector();
        // Measures bandwidth during playback. Can be null if not required.
        defaultBandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(context, Util.getUserAgent(context, "yourApplicationName"), defaultBandwidthMeter);
        // This is the MediaSource representing the media to be played.
        mediaSource=new ProgressiveMediaSource.Factory(new DefaultHttpDataSource.Factory()).createMediaSource(MediaItem.fromUri(Uri.parse("url or path")));

        // SimpleExoPlayer
        player =SimpleExoPlayer.Builder(context).build();
        // Prepare the player with the source.
        player.prepare(videoSource);
        player.setPlayWhenReady(true);
    }

}
