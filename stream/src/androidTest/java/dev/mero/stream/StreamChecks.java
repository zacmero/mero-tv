package dev.mero.stream;
import android.app.Instrumentation;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import androidx.media3.ui.R;
public class StreamChecks extends Instrumentation {
 MainActivity app;String check;
 @Override public void runOnMainSync(Runnable action){Throwable[] error={null};super.runOnMainSync(()->{try{action.run();}catch(Throwable e){error[0]=e;}});if(error[0]!=null)throw new RuntimeException(error[0]);}

 @Override public void onCreate(Bundle args){super.onCreate(args);check=args==null?null:args.getString("check");start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  app=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  if ("playback-remote".equals(check)) {
   java.io.File clip = new java.io.File(app.getCacheDir(), "playback-clock.mp4");
   try (java.io.InputStream source = getContext().getAssets().open(clip.getName());
        java.io.OutputStream output = new java.io.FileOutputStream(clip)) {
    byte[] buffer = new byte[4096];
    for (int count; (count = source.read(buffer)) != -1; ) output.write(buffer, 0, count);
   }
   try {
    runOnMainSync(() -> app.play(android.net.Uri.fromFile(clip).toString(), ""));
    checkPlaybackRemote(result);
   } finally {
    runOnMainSync(() -> app.finish());
    clip.delete();
   }
   finish(Activity.RESULT_OK,result);return;
  }
  if("history-posters".equals(check)){
   runOnMainSync(()->{app.posters.cache.evictAll();app.showHistory();if(app.rows.getChildCount()==0)throw new AssertionError("Existing history required");});
   waitFor(()->app.poster.getDrawable()!=null,12000,"Uncached history detail artwork");
   runOnMainSync(()->{if(!app.poster.isShown())throw new AssertionError("History poster not visible");android.graphics.Bitmap first=((android.graphics.drawable.BitmapDrawable)app.poster.getDrawable()).getBitmap();if(first.getWidth()>768||first.getHeight()>1152)throw new AssertionError("History poster exceeded decode bounds");app.showHistory();if(app.poster.getDrawable()==null||((android.graphics.drawable.BitmapDrawable)app.poster.getDrawable()).getBitmap()!=first)throw new AssertionError("History did not reuse artwork cache");});
   result.putString("stream","PASS: uncached Last Watched artwork and cache reuse");finish(Activity.RESULT_OK,result);return;
  }

  if (!"live-remote".equals(check)) checkLauncherIcon();
  if ("launcher-icon".equals(check)) {
   result.putString("launcher","PASS: bitmap icon and banner through application, MAIN/LAUNCHER and MAIN/LEANBACK_LAUNCHER");
   finish(Activity.RESULT_OK,result);return;
  }
  runOnMainSync(()->{app.query.setText("sousou no frieren");app.search();});
  waitFor(()->app.rows.getChildCount()>0,20000,"Search results");
  if (!"live-remote".equals(check)) {
   waitFor(()->app.poster.getDrawable()!=null,10000,"Focused poster preview");
   runOnMainSync(()->{android.graphics.drawable.BitmapDrawable poster=(android.graphics.drawable.BitmapDrawable)app.poster.getDrawable();if(poster.getBitmap().getWidth()>768||poster.getBitmap().getHeight()>1152)throw new AssertionError("Poster decode exceeded bounds");if(poster.getBitmap().getWidth()<300||poster.getBitmap().getHeight()<450)throw new AssertionError("Expected higher-resolution detail poster");if(app.poster.getWidth()<app.root.getWidth()*.3||app.poster.getHeight()<app.root.getHeight()*.65)throw new AssertionError("Poster panel is too small");});
  }
  runOnMainSync(()->app.rows.getChildAt(0).performClick());
  waitFor(()->app.episodes&&app.rows.getChildCount()>0,20000,"Episodes");
  runOnMainSync(()->app.rows.getChildAt(0).performClick());
  waitFor(()->app.video!=null,25000,"Stream resolution");
  if ("live-remote".equals(check)) {
   runOnMainSync(() -> app.episodeId = "");
   checkPlaybackRemote(result);
   runOnMainSync(() -> app.finish());
   finish(Activity.RESULT_OK,result);return;
  }
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

   if(app.video.player.getCurrentPosition()-start<=2000)throw new AssertionError("D-pad timeline jump must remain larger than 2 seconds");
   if(!app.video.player.getPlayWhenReady())throw new AssertionError("Timeline Center did not resume playback");
  });
  waitFor(()->app.video!=null&&!app.video.surface.isControllerFullyVisible(),1500,"Timeline confirmation hides controls");
  Thread.sleep(200);
  runOnMainSync(()->{
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_UP,android.view.KeyEvent.KEYCODE_DPAD_CENTER));
   if(app.video.surface.findViewById(androidx.media3.ui.R.id.exo_controller).getVisibility()!=android.view.View.GONE||app.video.bar.isShown()||app.video.volume.hud.isShown())throw new AssertionError("Center release reopened playback chrome");
  });
  runOnMainSync(()->{
   app.video.interact();android.view.View timeline=app.video.surface.findViewById(androidx.media3.ui.R.id.exo_progress);timeline.requestFocus();
   app.dispatchKeyEvent(new android.view.KeyEvent(android.view.KeyEvent.ACTION_DOWN,android.view.KeyEvent.KEYCODE_DPAD_RIGHT));
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
  result.putString("stream","PASS: widescreen playback, controls, history resume and subtitle placement, two-second seeks, clean Center down/up confirmation and large bounded poster panel");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){android.util.Log.e("StreamChecks","Acceptance failure",e);result.putString("stream","FAIL: "+e.getMessage());finish(Activity.RESULT_CANCELED,result);}}

 void checkLauncherIcon() {
  android.content.pm.PackageManager pm = app.getPackageManager();
  int expectedIcon = app.getApplicationInfo().icon;
  int expectedBanner = app.getApplicationInfo().banner;
  expectBitmap(app.getApplicationInfo().loadIcon(pm));
  expectBitmap(app.getApplicationInfo().loadBanner(pm));
  for (String category : new String[]{Intent.CATEGORY_LAUNCHER, Intent.CATEGORY_LEANBACK_LAUNCHER}) {
   Intent launch = new Intent(Intent.ACTION_MAIN).addCategory(category).setPackage(app.getPackageName());
   java.util.List<android.content.pm.ResolveInfo> entries = pm.queryIntentActivities(launch, 0);
   if (entries.size() != 1 || entries.get(0).activityInfo.icon != expectedIcon) {
    throw new AssertionError("Explicit icon missing for " + category);
   }
   expectBitmap(entries.get(0).loadIcon(pm));
   if (entries.get(0).activityInfo.banner != expectedBanner) {
    throw new AssertionError("Explicit banner missing for " + category);
   }
   expectBitmap(entries.get(0).activityInfo.loadBanner(pm));
  }
 }

 private void expectBitmap(android.graphics.drawable.Drawable icon) {
  if (!(icon instanceof android.graphics.drawable.BitmapDrawable)) {
   throw new AssertionError("Expected launcher bitmap");
  }
  android.graphics.Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) icon).getBitmap();
  if (bitmap.getPixel(0, 0) != 0xff0a1420) throw new AssertionError("Expected blue launcher artwork");
 }

 void checkPlaybackRemote(Bundle result) throws Exception {
  waitFor(() -> app.video != null && app.video.player.isPlaying()
    && app.video.player.getVideoSize().height > 0, 30000, "Advancing video");
  Thread.sleep(5500);
  runOnMainSync(() -> {
   if (app.video.surface.isControllerFullyVisible()) throw new AssertionError("Overlay failed to hide");
  });

  long centerDown = SystemClock.uptimeMillis();
  remoteKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.ACTION_DOWN, centerDown, 0);
  waitFor(() -> !app.video.player.getPlayWhenReady(), 1500, "Center pauses on press");
  runOnMainSync(this::requireHiddenChrome);
  remoteKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.ACTION_DOWN, centerDown, 1);
  remoteKeyEvent(KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.ACTION_UP, centerDown, 0);
  runOnMainSync(() -> {
   if (app.video.player.getPlayWhenReady()) throw new AssertionError("Center repeated or toggled on release");
   requireHiddenChrome();
  });
  remoteKey(KeyEvent.KEYCODE_DPAD_CENTER);
  waitFor(() -> app.video.player.isPlaying(), 6000, "Center resumes while hidden");
  runOnMainSync(this::requireHiddenChrome);

  remoteKey(KeyEvent.KEYCODE_DPAD_UP);
  waitFor(() -> app.video.surface.isControllerFullyVisible(), 1500, "Up opens idle overlay");
  runOnMainSync(() -> {
   if (!app.video.player.isPlaying()) throw new AssertionError("Up activated play/pause");
   requireCurrentClock();
  });
  final long[] initial = {0};
  runOnMainSync(() -> initial[0] = readClock());
  waitFor(() -> readClock() > initial[0], 2200, "Visible elapsed time advances");

  remoteKey(KeyEvent.KEYCODE_DPAD_DOWN);
  runOnMainSync(() -> {
   if (!control(R.id.exo_progress).hasFocus()) throw new AssertionError("Down did not focus timeline");
  });
  remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT);
  remoteKey(KeyEvent.KEYCODE_DPAD_CENTER);
  waitFor(() -> !app.video.surface.isControllerFullyVisible() && !app.video.bar.isShown(),
    1500, "Timeline confirmation closes whole overlay");

  Thread.sleep(5500);
  remoteKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
  waitFor(() -> !app.video.player.getPlayWhenReady(), 1500, "Play/Pause works from hidden overlay");
  final long[] pausedAt = {0};
  runOnMainSync(() -> pausedAt[0] = app.video.player.getCurrentPosition());
  remoteKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD);
  runOnMainSync(() -> {
   if (app.video.player.getCurrentPosition() != pausedAt[0] + 2000) {
    throw new AssertionError("Fast Forward did not seek by two seconds while hidden");
   }
  });
  remoteKey(KeyEvent.KEYCODE_MEDIA_REWIND);
  runOnMainSync(() -> {
   if (app.video.player.getCurrentPosition() != pausedAt[0]) {
    throw new AssertionError("Rewind did not seek by two seconds while hidden");
   }
  });
  remoteKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE);
  waitFor(() -> app.video.player.isPlaying(), 6000, "Play/Pause resumes while hidden");
  remoteKey(KeyEvent.KEYCODE_DPAD_UP);
  waitFor(() -> app.video.surface.isControllerFullyVisible(), 1500, "Up reopens overlay");
  runOnMainSync(this::requireCurrentClock);
  remoteKey(KeyEvent.KEYCODE_DPAD_DOWN);
  remoteKey(KeyEvent.KEYCODE_DPAD_DOWN);
  remoteKey(KeyEvent.KEYCODE_DPAD_CENTER);
  long dialogDeadline = SystemClock.elapsedRealtime() + 2000;
  boolean subtitleMenu = false;
  do {
   android.view.accessibility.AccessibilityNodeInfo window = getUiAutomation().getRootInActiveWindow();
   subtitleMenu = window != null
     && !window.findAccessibilityNodeInfosByText("Appearance / font size").isEmpty();
   if (!subtitleMenu) Thread.sleep(100);
  } while (!subtitleMenu && SystemClock.elapsedRealtime() < dialogDeadline);
  if (!subtitleMenu) throw new AssertionError("D-pad and Center did not reach Subtitles");
  waitFor(() -> !app.video.surface.isControllerFullyVisible(), 5500, "Overlay hides behind track menu");
  remoteKey(KeyEvent.KEYCODE_BACK);
  waitFor(() -> app.hasWindowFocus() && app.video.surface.isControllerFullyVisible(), 2000,
    "Back from track menu restores playback window and overlay");
  remoteKey(KeyEvent.KEYCODE_BACK);
  waitFor(() -> app.video != null && !app.video.surface.isControllerFullyVisible(),
    1500, "First playback Back closes overlay");
  remoteKey(KeyEvent.KEYCODE_BACK);
  waitFor(() -> app.video == null, 1500, "Second playback Back exits to library");
  result.putString("remote","PASS: hidden Center pause/resume, Up opens controls, live clock, media keys, timeline, track menu and Back");
 }

 private View control(int id) { return app.video.surface.findViewById(id); }
 private void requireHiddenChrome() {
  if (control(R.id.exo_controller).getVisibility() != View.GONE
    || app.video.bar.isShown() || app.video.volume.hud.isShown()) {
   throw new AssertionError("Center showed playback controls");
  }
 }
 private long readClock() {
  String[] fields = ((TextView) control(R.id.exo_position)).getText().toString().trim().split(":");
  long seconds = 0;
  for (String field : fields) seconds = seconds * 60 + Integer.parseInt(field);
  return seconds * 1000;
 }
 private void requireCurrentClock() {
  long playerTime = app.video.player.getCurrentPosition();
  long clock = readClock();
  String position = ((TextView) control(R.id.exo_position)).getText().toString();
  if (Math.abs(playerTime - clock) > 1500
      || !position.equals(String.valueOf(control(R.id.exo_progress).getContentDescription()))) {
   throw new AssertionError("Stale time bar: displayed " + clock + ", player " + playerTime);
  }
 }
 private void remoteKey(int code) {
  long now = SystemClock.uptimeMillis();
  remoteKeyEvent(code, KeyEvent.ACTION_DOWN, now, 0);
  remoteKeyEvent(code, KeyEvent.ACTION_UP, now, 0);
 }
 private void remoteKeyEvent(int code, int action, long downTime, int repeat) {
  KeyEvent event = new KeyEvent(downTime, SystemClock.uptimeMillis(), action, code, repeat, 0,
    android.view.KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0, InputDevice.SOURCE_DPAD);
  if (!getUiAutomation().injectInputEvent(event, true)) {
   throw new AssertionError("Android rejected key " + code);
  }
 }
 android.widget.Button findButton(android.view.View view,String prefix){if(view instanceof android.widget.Button&&((android.widget.Button)view).getText().toString().startsWith(prefix))return (android.widget.Button)view;if(view instanceof android.view.ViewGroup){android.view.ViewGroup group=(android.view.ViewGroup)view;for(int i=0;i<group.getChildCount();i++){android.widget.Button found=findButton(group.getChildAt(i),prefix);if(found!=null)return found;}}return null;}
 interface Check {boolean ok();}
 void waitFor(Check c,long timeout,String step)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){boolean[] yes={false};runOnMainSync(()->yes[0]=c.ok());if(yes[0])return;Thread.sleep(300);}throw new Exception(step+" timed out");}
}
