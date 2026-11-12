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
    final LinearLayout view;
    final ExoPlayer player;
    final Activity activity;
    StreamPlayer(Activity a,String url,String referer,JSONArray subtitles,Runnable failure) {
        activity=a;
        DefaultHttpDataSource.Factory http=new DefaultHttpDataSource.Factory().setUserAgent("Mozilla/5.0")
            .setDefaultRequestProperties(Collections.singletonMap("Referer",referer));
        player=new ExoPlayer.Builder(a).setMediaSourceFactory(new DefaultMediaSourceFactory(http)).build();
        view=new LinearLayout(a);view.setOrientation(LinearLayout.VERTICAL);view.setBackgroundColor(0xff080e15);
        LinearLayout bar=new LinearLayout(a);
        for(int type:new int[]{C.TRACK_TYPE_TEXT,C.TRACK_TYPE_AUDIO,C.TRACK_TYPE_VIDEO}) {
            Button b=new Button(a);b.setText(type==C.TRACK_TYPE_TEXT?"Subtitles":type==C.TRACK_TYPE_AUDIO?"Audio":"Resolution");
            b.setOnClickListener(v->tracks(type));bar.addView(b,new LinearLayout.LayoutParams(0,-2,1));
        }
        view.addView(bar);
        PlayerView surface=new PlayerView(a);surface.setPlayer(player);surface.setKeepScreenOn(true);
        surface.setShowSubtitleButton(true);view.addView(surface,new LinearLayout.LayoutParams(-1,0,1));
        List<MediaItem.SubtitleConfiguration> subs=new ArrayList<>();
        if(subtitles!=null)for(int i=0;i<subtitles.length();i++){
            JSONObject s=subtitles.optJSONObject(i);if(s==null)continue;
            String src=s.optString("src");if(!src.startsWith("https://"))continue;
            subs.add(new MediaItem.SubtitleConfiguration.Builder(Uri.parse(src)).setMimeType(MimeTypes.TEXT_VTT)
                .setLanguage(s.optString("srclang","und")).setLabel(s.optString("label","Subtitles"))
                .setSelectionFlags(s.optBoolean("default")?C.SELECTION_FLAG_DEFAULT:0).build());
        }
        player.addListener(new Player.Listener(){@Override public void onPlayerError(PlaybackException error){failure.run();}});
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
        new AlertDialog.Builder(activity).setTitle(type==C.TRACK_TYPE_TEXT?"Subtitles":type==C.TRACK_TYPE_AUDIO?"Audio":"Resolution")
            .setItems(labels.toArray(new String[0]),(d,n)->actions.get(n).run()).setNegativeButton("Back",null).show();
    }
    void pause(){player.pause();}
    void release(){player.release();}
}
