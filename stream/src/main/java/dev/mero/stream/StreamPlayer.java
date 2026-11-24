package dev.mero.stream;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.common.util.Util;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.DefaultTimeBar;
import androidx.media3.ui.PlayerControlView;
import androidx.media3.ui.PlayerView;
import androidx.media3.ui.TimeBar;
import org.json.*;
import java.util.*;

/** Owns playback and track selection; no provider or catalogue responsibilities. */
@androidx.media3.common.util.UnstableApi
final class StreamPlayer {
    final FrameLayout view;
    final PlayerView surface;
    final LinearLayout bar;
    final ExoPlayer player;
    final Activity activity;
    final SubtitleAppearance subtitleAppearance;
    final PlayerVolume volume;
    private final PlayerControlView controls;
    private final DefaultTimeBar timeline;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable finishScrub = this::finishSeek;
    private boolean scrubbing;
    private int consumedNavigationKey = KeyEvent.KEYCODE_UNKNOWN;

    boolean key(KeyEvent event) {
        if (volume.key(event)) return true;
        int key = event.getKeyCode();
        boolean down = event.getAction() == KeyEvent.ACTION_DOWN;
        if (key == KeyEvent.KEYCODE_MEDIA_REWIND || key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD) {
            if (down) {
                cancelScrub();
                if (key == KeyEvent.KEYCODE_MEDIA_REWIND) player.seekBack();
                else player.seekForward();
                finishSeek();
            }
            return true;
        }
        if (controls.dispatchMediaKeyEvent(event)) return true;

        boolean select = key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
            || key == KeyEvent.KEYCODE_NUMPAD_ENTER;
        if (!select && (key < KeyEvent.KEYCODE_DPAD_UP || key > KeyEvent.KEYCODE_DPAD_RIGHT)) {
            return false;
        }
        // Consume the entire shortcut or wake press, including repeats and release.
        if (key == consumedNavigationKey) {
            if (event.getAction() == KeyEvent.ACTION_UP) {
                consumedNavigationKey = KeyEvent.KEYCODE_UNKNOWN;
                return true;
            }
            if (event.getRepeatCount() > 0) return true;
            consumedNavigationKey = KeyEvent.KEYCODE_UNKNOWN;
        }
        if (!down) return false;
        if (!controls.isFullyVisible()) {
            consumedNavigationKey = key;
            if (!select) interact();
            else if (event.getRepeatCount() == 0) Util.handlePlayPauseButtonAction(player, false);
            return true;
        }
        if (select && timeline.hasFocus()) {
            consumedNavigationKey = key;
            if (scrubbing) {
                timeline.onKeyDown(KeyEvent.KEYCODE_DPAD_CENTER, event);
            } else {
                player.play();
                finishSeek();
            }
            return true;
        }
        return false;
    }

    private void finishSeek() {
        handler.removeCallbacks(finishScrub);
        surface.hideController();
        volume.hideLevel();
    }

    private void cancelScrub() {
        if (!scrubbing) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent cancel = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0, 0, 0);
        timeline.onTouchEvent(cancel);
        cancel.recycle();
    }

    void interact() { surface.showController(); }

    StreamPlayer(Activity a,String url,String referer,JSONArray subtitles,Runnable failure) {
        activity=a;subtitleAppearance=new SubtitleAppearance(a);
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0")
            .setDefaultRequestProperties(Collections.singletonMap("Referer",referer));
        player=new ExoPlayer.Builder(a).setSeekBackIncrementMs(2000).setSeekForwardIncrementMs(2000)
            .setMediaSourceFactory(new DefaultMediaSourceFactory(new DefaultDataSource.Factory(a, http))).build();
        view=new FrameLayout(a);view.setBackgroundColor(0xff080e15);
        bar=new LinearLayout(a);bar.setBackgroundColor(0xdd080e15);
        for(int type:new int[]{C.TRACK_TYPE_TEXT,C.TRACK_TYPE_AUDIO,C.TRACK_TYPE_VIDEO}) {
            Button b=new Button(a);FocusStyle.apply(b);b.setTextSize(13);b.setMinWidth(0);b.setMinHeight(0);b.setPadding(12,4,12,4);b.setText(type==C.TRACK_TYPE_TEXT?"Subtitles":type==C.TRACK_TYPE_AUDIO?"Audio":"Resolution");
            b.setOnClickListener(v->tracks(type));bar.addView(b,new LinearLayout.LayoutParams(-2,Math.round(40*a.getResources().getDisplayMetrics().density)));
        }
        surface=new PlayerView(a);surface.setPlayer(player);surface.setKeepScreenOn(true);
        controls = surface.findViewById(androidx.media3.ui.R.id.exo_controller);
        controls.setAnimationEnabled(false);
        surface.setFocusable(true);
        surface.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) visibility -> {
            if (visibility == View.GONE) surface.requestFocus();
        });
        timeline = surface.findViewById(androidx.media3.ui.R.id.exo_progress);
        timeline.addListener(new TimeBar.OnScrubListener() {
            @Override public void onScrubStart(TimeBar bar, long position) { scrubbing = true; }
            @Override public void onScrubMove(TimeBar bar, long position) {}
            @Override public void onScrubStop(TimeBar bar, long position, boolean canceled) {
                scrubbing = false;
                if (!canceled) {
                    // Media3 commits the seek. Wait until key dispatch ends before hiding the overlay.
                    player.play();
                    handler.post(finishScrub);
                }
            }
        });
        subtitleAppearance.bind(surface.getSubtitleView());
        surface.setControllerShowTimeoutMs(4000);surface.setControllerAutoShow(false);

        view.addView(surface,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout bottom=surface.findViewById(androidx.media3.ui.R.id.exo_basic_controls);
        bottom.addView(bar,0);
        View playPause = surface.findViewById(androidx.media3.ui.R.id.exo_play_pause);
        playPause.setNextFocusDownId(timeline.getId());
        timeline.setNextFocusUpId(playPause.getId());
        View firstTrack = bar.getChildAt(0);
        firstTrack.setId(View.generateViewId());
        timeline.setNextFocusDownId(firstTrack.getId());
        for (int i = 0; i < bar.getChildCount(); i++) {
            bar.getChildAt(i).setNextFocusUpId(timeline.getId());
        }
        volume=new PlayerVolume(a,player,view);
        surface.findViewById(androidx.media3.ui.R.id.exo_settings).setVisibility(android.view.View.GONE);
        List<MediaItem.SubtitleConfiguration> subs=new ArrayList<>();
        if(subtitles!=null)for(int i=0;i<subtitles.length();i++){
            JSONObject s=subtitles.optJSONObject(i);if(s==null)continue;
            String src=s.optString("src");if(!src.startsWith("https://"))continue;
            subs.add(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(src)).setMimeType(MimeTypes.TEXT_VTT)
                .setLanguage(s.optString("srclang","und")).setLabel(s.optString("label","Subtitles"))
                .setSelectionFlags(s.optBoolean("default")?C.SELECTION_FLAG_DEFAULT:0).build());
        }
        player.addListener(new Player.Listener(){@Override public void onCues(androidx.media3.common.text.CueGroup cues){subtitleAppearance.onCues(cues.cues);} @Override public void onPlayerError(PlaybackException error){failure.run();}});
        player.setMediaItem(new MediaItem.Builder().setUri(url).setSubtitleConfigurations(subs).build());
        player.prepare();player.play();
    }
    void tracks(int type){
        List<String> labels=new ArrayList<>();List<Runnable> actions=new ArrayList<>();
        labels.add(type==C.TRACK_TYPE_TEXT?"Off":"Auto");actions.add(()->player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(type).setTrackTypeDisabled(type,type==C.TRACK_TYPE_TEXT).build()));
        for(Tracks.Group group:player.getCurrentTracks().getGroups())if(group.getType()==type)
            for(int i=0;i<group.length;i++)if(group.isTrackSupported(i)){
                final int index=i;Format f=group.getTrackFormat(i);
                labels.add(type==C.TRACK_TYPE_VIDEO?(f.height>0?f.height+"p":"Video"):(f.label!=null?f.label:(f.language!=null?f.language:"Track "+(i+1))));
                actions.add(()->player.setTrackSelectionParameters(player.getTrackSelectionParameters().buildUpon().setTrackTypeDisabled(type,false).setOverrideForType(new TrackSelectionOverride(group.getMediaTrackGroup(),index)).build()));
            }
        if(type==C.TRACK_TYPE_AUDIO){labels.add("Player volume");actions.add(()->volume.show());}
        if(type==C.TRACK_TYPE_TEXT){labels.add("Appearance / font size");actions.add(()->subtitleAppearance.show(surface.getSubtitleView()));}
        LinearLayout choices=new LinearLayout(activity);choices.setOrientation(LinearLayout.VERTICAL);
        ScrollView scroll=new ScrollView(activity);scroll.addView(choices);
        AlertDialog dialog=new AlertDialog.Builder(activity).setTitle(type==C.TRACK_TYPE_TEXT?"Subtitles":type==C.TRACK_TYPE_AUDIO?"Audio":"Resolution")
            .setView(scroll).setNegativeButton("Back",null).create();
        for(int i=0;i<labels.size();i++){final int index=i;Button b=new Button(activity);FocusStyle.apply(b);b.setText(labels.get(i));b.setOnClickListener(v->{dialog.dismiss();actions.get(index).run();});choices.addView(b);}
        dialog.setOnDismissListener(d -> {
            if (surface.isAttachedToWindow() && surface.getPlayer() != null) interact();
        });
        dialog.show();if(choices.getChildCount()>0)choices.getChildAt(0).requestFocus();
    }
    boolean dismissControls() {
        if (!controls.isFullyVisible()) return false;
        cancelScrub();
        finishSeek();
        return true;
    }
    void pause(){player.pause();}
    void release() {
        cancelScrub();
        handler.removeCallbacks(finishScrub);
        surface.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener) null);
        surface.hideController();
        surface.setPlayer(null);
        volume.release();
        player.release();
    }
}
