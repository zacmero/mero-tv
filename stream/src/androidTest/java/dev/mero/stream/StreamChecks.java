package dev.mero.stream;
import android.app.Instrumentation;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
public class StreamChecks extends Instrumentation {
 MainActivity app;
 @Override public void runOnMainSync(Runnable action){Throwable[] error={null};super.runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new RuntimeException(error[0]);}

 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  app=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  runOnMainSync(()->{
   try{android.content.pm.ActivityInfo info=app.getPackageManager().getActivityInfo(new android.content.ComponentName(app,"dev.mero.stream.MainActivity"),0);
    if(!(info.loadIcon(app.getPackageManager()) instanceof android.graphics.drawable.VectorDrawable)||!(info.loadBanner(app.getPackageManager()) instanceof android.graphics.drawable.VectorDrawable))throw new AssertionError("Launcher vector assets missing");
   }catch(android.content.pm.PackageManager.NameNotFoundException e){throw new RuntimeException(e);}
  });
  runOnMainSync(()->{app.query.setText("sousou no frieren");app.search();});
  waitFor(()->app.rows.getChildCount()>0,20000,"Search results");
  waitFor(()->app.poster.getDrawable()!=null,10000,"Focused poster preview");
  runOnMainSync(()->{android.graphics.drawable.BitmapDrawable poster=(android.graphics.drawable.BitmapDrawable)app.poster.getDrawable();if(poster.getBitmap().getWidth()>256||poster.getBitmap().getHeight()>384)throw new AssertionError("Poster decode exceeded bounds");});
  runOnMainSync(()->app.rows.getChildAt(0).performClick());
  waitFor(()->app.episodes&&app.rows.getChildCount()>0,20000,"Episodes");
  runOnMainSync(()->app.rows.getChildAt(0).performClick());
  waitFor(()->app.video!=null,25000,"Stream resolution");
  waitFor(()->app.video!=null&&app.video.player.getCurrentPosition()>15000,40000,"Advancing playback");
  runOnMainSync(()->{
   app.video.player.pause();long start=app.video.player.getCurrentPosition();
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_MEDIA_FAST_FORWARD));
   if(app.video.player.getCurrentPosition()-start!=2000)throw new AssertionError("Forward must seek 2 seconds");
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_MEDIA_REWIND));
   if(app.video.player.getCurrentPosition()!=start)throw new AssertionError("Rewind must seek 2 seconds");
   if(app.video.surface.isControllerFullyVisible())throw new AssertionError("Seek controls did not hide");
   app.video.interact();android.view.View timeline=app.video.surface.findViewById(androidx.media3.ui.R.id.exo_progress);timeline.requestFocus();
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_RIGHT));
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_DPAD_RIGHT));
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_CENTER));
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_DPAD_CENTER));
   if(app.video.player.getCurrentPosition()-start<=2000)throw new AssertionError("D-pad timeline jump must remain larger than 2 seconds");
   if(!app.video.player.getPlayWhenReady())throw new AssertionError("Timeline Center did not resume playback");
  });
  waitFor(()->app.video!=null&&!app.video.surface.isControllerFullyVisible(),1500,"Timeline confirmation hides controls");
  runOnMainSync(()->{
   app.video.interact();android.view.View timeline=app.video.surface.findViewById(androidx.media3.ui.R.id.exo_progress);timeline.requestFocus();
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_LEFT));
   app.onBackPressed();if(app.video==null||app.video.surface.isControllerFullyVisible())throw new AssertionError("Canceling timeline scrub must hide controls without exiting");
   app.video.interact();
  });
  waitFor(()->!app.video.surface.isControllerFullyVisible(),8000,"Controller auto-hide");
  runOnMainSync(()->{if(app.video.player.getVideoSize().height<=0 || (double)app.video.player.getVideoSize().width/app.video.player.getVideoSize().height<1.7)throw new AssertionError("Expected native widescreen source");
   if(app.video.surface.getWidth()!=app.video.view.getWidth()||app.video.surface.getHeight()!=app.video.view.getHeight())throw new AssertionError("Video surface must fill playback window");
   if(app.video.surface.isControllerFullyVisible())throw new AssertionError("Track controls must auto-hide");});
  runOnMainSync(()->{result.putLong("positionMs",app.video.player.getCurrentPosition());result.putInt("trackGroups",app.video.player.getCurrentTracks().getGroups().size());result.putInt("height",app.video.player.getVideoSize().height);});
  runOnMainSync(()->{
   app.video.surface.showController();app.onBackPressed();
   if(app.video==null||app.video.surface.isControllerFullyVisible())throw new AssertionError("First Back must hide controls");
   app.onBackPressed();if(app.video!=null)throw new AssertionError("Second Back must exit playback");
   if(app.getPreferences(0).getLong("position",0)<10000)throw new AssertionError("Progress not saved");
   org.json.JSONArray history=new WatchHistory(app).entries();
   if(history.length()==0||!app.episodeId.equals(history.optJSONObject(0).optString("episode")))throw new AssertionError("Last watched missing current episode");
   app.showHistory();if(app.rows.getChildCount()==0)throw new AssertionError("History rows missing");app.rows.getChildAt(0).performClick();
  });
  waitFor(()->app.video!=null&&app.video.player.isPlaying()&&app.video.player.getCurrentPosition()>10000,30000,"Continue watching");
  runOnMainSync(()->{
   PlayerVolume volume=app.video.volume;float gain=volume.prefs.getFloat("gain",1);boolean muted=volume.prefs.getBoolean("muted",false);
   volume.prefs.edit().putFloat("gain",1).putBoolean("muted",false).apply();volume.apply();
   android.app.AlertDialog volumeDialog=volume.show();android.widget.Button quieter=findButton(volumeDialog.getWindow().getDecorView(),"Quieter");
   if(quieter==null)throw new AssertionError("Volume control missing");quieter.performClick();
   float previous=app.video.player.getVolume();app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_VOLUME_DOWN));
   if(app.video.player.getVolume()>=previous||volume.hud.getVisibility()!=android.view.View.VISIBLE)throw new AssertionError("Volume key or HUD failed");
   float beforeShortcut=app.video.player.getVolume();
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_MENU));
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_DOWN));
   if(app.video.player.getVolume()>=beforeShortcut)throw new AssertionError("Menu/D-pad shortcut failed");
   volume.dismissShortcut();
   if(app.video.player.getVolume()>=1)throw new AssertionError("Player volume did not decrease");volume.mute();if(app.video.player.getVolume()!=0)throw new AssertionError("Mute failed");volume.mute();if(app.video.player.getVolume()<=0)throw new AssertionError("Unmute failed");
   volumeDialog.dismiss();volume.prefs.edit().putFloat("gain",gain).putBoolean("muted",muted).apply();volume.apply();
   SubtitleAppearance style=app.video.subtitleAppearance;
   java.util.Map<String,?> before=style.prefs.getAll();int original=style.choice("size",4);
   android.app.AlertDialog dialog=style.show(app.video.surface.getSubtitleView());
   android.widget.Button size=findButton(dialog.getWindow().getDecorView(),"Font size:");
   if(size==null)throw new AssertionError("Subtitle size control missing");size.performClick();
   if(new SubtitleAppearance(app).choice("size",4)!=(original+1)%4)throw new AssertionError("Subtitle size not persisted");
   if(findButton(dialog.getWindow().getDecorView(),"Background:")==null)throw new AssertionError("Subtitle background control missing");
   android.widget.Button placement=findButton(dialog.getWindow().getDecorView(),"Vertical position:");
   if(placement==null)throw new AssertionError("Subtitle placement control missing");
   int oldPlacement=style.choice("position",5);placement.performClick();
   if(new SubtitleAppearance(app).choice("position",5)!=(oldPlacement+1)%5)throw new AssertionError("Placement not persisted");
   dialog.dismiss();android.content.SharedPreferences.Editor restore=style.prefs.edit().clear();for(java.util.Map.Entry<String,?> entry:before.entrySet())restore.putInt(entry.getKey(),(Integer)entry.getValue());restore.apply();style.apply(app.video.surface.getSubtitleView());
  });
  waitFor(()->app.video.volume.hud.getVisibility()==android.view.View.GONE,2500,"Volume HUD auto-hide");
  result.putString("controls","PASS: Back hides then exits; Continue restores position");
  result.putString("stream","PASS: widescreen playback, controls, history resume and subtitle placement, two-second seeks, timeline resume/hide and bounded poster preview");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){android.util.Log.e("StreamChecks","Acceptance failure",e);result.putString("stream","FAIL: "+e.getMessage());finish(Activity.RESULT_CANCELED,result);}}
 android.widget.Button findButton(android.view.View view,String prefix){if(view instanceof android.widget.Button&&((android.widget.Button)view).getText().toString().startsWith(prefix))return (android.widget.Button)view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){android.widget.Button found=findButton(group.getChildAt(i),prefix);if(found!=null)return found;}}return null;}
 interface Check {boolean ok();}
 void waitFor(Check c,long timeout,String step)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){boolean[] yes={false};runOnMainSync(()->yes[0]=c.ok());if(yes[0])return;Thread.sleep(300);}throw new Exception(step+" timed out");}
}
