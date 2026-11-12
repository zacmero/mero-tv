package dev.mero.stream;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.view.KeyEvent;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
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
    final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
    final Runnable hide=()->surfaceHide();
    void surfaceHide(){surface.hideController();}
    boolean confirmingTimeline;
    boolean timelineKey(KeyEvent event){int key=event.getKeyCode();if(key!=KeyEvent.KEYCODE_DPAD_CENTER&&key!=KeyEvent.KEYCODE_ENTER)return false;
        android.view.View timeline=surface.findViewById(androidx.media3.ui.R.id.exo_progress);
        if(!confirmingTimeline&&!timeline.hasFocus())return false;
        if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){confirmingTimeline=true;timeline.dispatchKeyEvent(event);player.play();finishSeek();}
        else if(event.getAction()==KeyEvent.ACTION_UP){confirmingTimeline=false;timeline.setPressed(false);finishSeek();}
        return true;
    }
    void finishSeek(){handler.removeCallbacks(hide);surface.hideController();surface.findViewById(androidx.media3.ui.R.id.exo_controller).setVisibility(android.view.View.GONE);volume.hideLevel();surface.requestFocus();}
    boolean seekKey(KeyEvent event){int key=event.getKeyCode();if(key!=KeyEvent.KEYCODE_MEDIA_REWIND&&key!=KeyEvent.KEYCODE_MEDIA_FAST_FORWARD)return false;
        if(event.getAction()==KeyEvent.ACTION_DOWN){if(key==KeyEvent.KEYCODE_MEDIA_REWIND)player.seekBack();else player.seekForward();finishSeek();}return true;}

    void interact(){surface.findViewById(androidx.media3.ui.R.id.exo_controller).setVisibility(android.view.View.VISIBLE);surface.showController();handler.removeCallbacks(hide);handler.postDelayed(hide,4000);}

    StreamPlayer(Activity a,String url,String referer,JSONArray subtitles,Runnable failure) {
        activity=a;subtitleAppearance=new SubtitleAppearance(a);
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0")
            .setDefaultRequestProperties(Collections.singletonMap("Referer",referer));
        player=new ExoPlayer.Builder(a).setSeekBackIncrementMs(2000).setSeekForwardIncrementMs(2000).setMediaSourceFactory(new DefaultMediaSourceFactory(http)).build();
        view=new FrameLayout(a);view.setBackgroundColor(0xff080e15);
        bar=new LinearLayout(a);bar.setBackgroundColor(0xdd080e15);
        for(int type:new int[]{C.TRACK_TYPE_TEXT,C.TRACK_TYPE_AUDIO,C.TRACK_TYPE_VIDEO}) {
            Button b=new Button(a);FocusStyle.apply(b);b.setTextSize(13);b.setMinWidth(0);b.setMinHeight(0);b.setPadding(12,4,12,4);b.setText(type==C.TRACK_TYPE_TEXT?"Subtitles":type==C.TRACK_TYPE_AUDIO?"Audio":"Resolution");
            b.setOnClickListener(v->tracks(type));bar.addView(b,new LinearLayout.LayoutParams(-2,Math.round(40*a.getResources().getDisplayMetrics().density)));
        }
        surface=new PlayerView(a);surface.setPlayer(player);surface.setKeepScreenOn(true);
        ((androidx.media3.ui.PlayerControlView)surface.findViewById(androidx.media3.ui.R.id.exo_controller)).setAnimationEnabled(false);
        androidx.media3.ui.TimeBar timeline=surface.findViewById(androidx.media3.ui.R.id.exo_progress);
        ((android.view.View)timeline).setOnClickListener(v->{player.play();handler.post(()->finishSeek());});
        timeline.addListener(new androidx.media3.ui.TimeBar.OnScrubListener(){
            public void onScrubStart(androidx.media3.ui.TimeBar bar,long position){handler.removeCallbacks(hide);}
            public void onScrubMove(androidx.media3.ui.TimeBar bar,long position){handler.removeCallbacks(hide);}
            public void onScrubStop(androidx.media3.ui.TimeBar bar,long position,boolean canceled){if(!canceled){player.seekTo(position);player.play();handler.post(()->finishSeek());}}
        });
        subtitleAppearance.bind(surface.getSubtitleView());
        surface.setControllerShowTimeoutMs(4000);surface.setControllerAutoShow(false);

        view.addView(surface,new FrameLayout.LayoutParams(-1,-1));
        LinearLayout bottom=surface.findViewById(androidx.media3.ui.R.id.exo_basic_controls);
        bottom.addView(bar,0);
        volume=new PlayerVolume(a,player,view);
        surface.findViewById(androidx.media3.ui.R.id.exo_settings).setVisibility(android.view.View.GONE);
        view.setFocusable(true);
        view.setOnKeyListener((v,key,event)->{if(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_CENTER){interact();return false;}return false;});
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
        player.prepare();player.play();interact();
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
        for(int i=0;i<labels.size();i++){final int index=i;Button b=new Button(activity);FocusStyle.apply(b);b.setText(labels.get(i));b.setOnClickListener(v->{actions.get(index).run();dialog.dismiss();interact();});choices.addView(b);}
        dialog.show();if(choices.getChildCount()>0)choices.getChildAt(0).requestFocus();
    }
    boolean dismissControls(){if(surface.isControllerFullyVisible()){surface.hideController();surface.requestFocus();return true;}return false;}
    void pause(){player.pause();}
    void release(){handler.removeCallbacks(hide);volume.release();player.release();}
}
