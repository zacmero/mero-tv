package dev.mero.stream;
import android.app.Instrumentation;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.TextView;
import androidx.media3.ui.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
    runOnMainSync(() -> app.play(new Provider.Stream(android.net.Uri.fromFile(clip).toString(),
      "", null, "sub", null), 0, true));
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
  if ("live-remote".equals(check) || "episode-features".equals(check) || "genocidal-organ".equals(check)) {
   checkWithoutSavingHistory(result);
   finish(Activity.RESULT_OK,result);return;
  }
  runOnMainSync(()->{app.query.setText("sousou no frieren");app.search();});
  waitFor(()->app.rows.getChildCount()>0,20000,"Search results");
  waitFor(()->app.poster.getDrawable()!=null,10000,"Focused poster preview");
  runOnMainSync(()->{android.graphics.drawable.BitmapDrawable poster=(android.graphics.drawable.BitmapDrawable)app.poster.getDrawable();if(poster.getBitmap().getWidth()>768||poster.getBitmap().getHeight()>1152)throw new AssertionError("Poster decode exceeded bounds");if(poster.getBitmap().getWidth()<300||poster.getBitmap().getHeight()<450)throw new AssertionError("Expected higher-resolution detail poster");if(app.poster.getWidth()<app.root.getWidth()*.3||app.poster.getHeight()<app.root.getHeight()*.65)throw new AssertionError("Poster panel is too small");});
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
   String episodeId = app.playing.selection().episode().id();
   app.onBackPressed();if(app.video!=null)throw new AssertionError("Second Back must exit playback");
   if(app.getPreferences(0).getLong("position",0)<10000)throw new AssertionError("Progress not saved");
   org.json.JSONArray history=new WatchHistory(app).entries();
   if(history.length()==0||!episodeId.equals(history.optJSONObject(0).optString("episode")))throw new AssertionError("Last watched missing current episode");
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

 private void checkWithoutSavingHistory(Bundle result) throws Exception {
  SharedPreferences[] prefs = { app.getPreferences(0), new WatchHistory(app).prefs, new SearchHistory(app).prefs };
  List<Map<String, ?>> snapshots = new ArrayList<>();
  for (SharedPreferences p : prefs) snapshots.add(p.getAll());
  try {
   if ("genocidal-organ".equals(check)) {
    selectTitle("Genocidal Organ");
    selectEpisode(0);
    waitFor(() -> app.video != null && app.video.player.isPlaying()
      && app.video.player.getCurrentPosition() >= 5000 && app.video.player.getVideoSize().height > 0,
      35000, "Genocidal Organ advances");
    runOnMainSync(() -> {
     if (!"sub".equals(app.playing.selection().preference()) || !"dub".equals(app.playing.mode())
       || app.playing.episodes().size() != 1 || app.playing.hasEpisode(-1) || app.playing.hasEpisode(1)) {
      throw new AssertionError("Expected the DUB fallback and single-episode boundaries");
     }
     if (findButton(app.video.bar, "Skip opening") != null) throw new AssertionError("Unmarked opening button");
    });
    result.putString("film", "PASS: film advances with the available DUB source selected automatically");
   } else if ("episode-features".equals(check)) {
    checkEpisodeFeatures(result);
   } else {
    selectTitle("sousou no frieren");
    selectEpisode(0);
    checkPlaybackRemote(result);
   }
  } finally {
   runOnMainSync(() -> {
    app.playing = null;
    app.finish();
    for (int i = 0; i < prefs.length; i++) {
     SharedPreferences.Editor update = prefs[i].edit().clear();
     for (Map.Entry<String, ?> entry : snapshots.get(i).entrySet()) {
      if (entry.getValue() instanceof String text) update.putString(entry.getKey(), text);
      else if (entry.getValue() instanceof Long time) update.putLong(entry.getKey(), time);
     }
     update.apply();
    }
   });
  }
 }

 private void selectTitle(String term) throws Exception {
  runOnMainSync(() -> {
   app.query.setText(term);
   app.search();
  });
  waitFor(() -> app.rows.getChildCount() > 0, 20000, "Search results for live test");
  runOnMainSync(() -> app.rows.getChildAt(0).performClick());
  waitFor(() -> app.episodes && app.rows.getChildCount() > 0, 20000, "Title's episode list");
 }

 private void selectEpisode(int index) {
  runOnMainSync(() -> app.rows.getChildAt(index).performClick());
 }

 private void waitForEpisode(int index) throws Exception {
  waitFor(() -> app.playing != null && app.playing.index() == index && app.video != null
    && app.video.player.isPlaying() && app.video.player.getCurrentPosition() > 0,
    35000, "Playing expected episode");
 }

 private void openControls() throws Exception {
  boolean[] hidden = { false };
  runOnMainSync(() -> hidden[0] = !app.video.surface.isControllerFullyVisible());
  if (hidden[0]) remoteKey(KeyEvent.KEYCODE_DPAD_UP);
  waitFor(() -> app.video.surface.isControllerFullyVisible(), 2000, "Playback controls shown");
 }

 private void selectEpisodeControl(int id, int direction) throws Exception {
  openControls();
  runOnMainSync(() -> {
   if (!control(id).isEnabled()) throw new AssertionError("Episode control is disabled");
   control(R.id.exo_play_pause).requestFocus();
  });
  for (int i = 0; i < 3; i++) {
   boolean[] focused = { false };
   runOnMainSync(() -> focused[0] = control(id).hasFocus());
   if (focused[0]) {
    remoteKey(KeyEvent.KEYCODE_DPAD_CENTER);
    return;
   }
   remoteKey(direction);
  }
  throw new AssertionError("Episode control cannot be reached with the D-pad");
 }

 private void checkEpisodeFeatures(Bundle result) throws Exception {
  // The real search submits to the provider. Keep the menu and saved-state checks independent of playback.
  selectTitle("sousou no frieren");
  runOnMainSync(() -> {
   if (!new SearchHistory(app).terms().get(0).equals("sousou no frieren")) {
    throw new AssertionError("Search did not persist");
   }
   findButton(app.root, "Search history").performClick();
  });
  long deadline = SystemClock.elapsedRealtime() + 3000;
  boolean found = false;
  do {
   AccessibilityNodeInfo window = getUiAutomation().getRootInActiveWindow();
   found = window != null && !window.findAccessibilityNodeInfosByText("sousou no frieren").isEmpty()
     && !window.findAccessibilityNodeInfosByText("Search history").isEmpty();
   if (!found) Thread.sleep(100);
  } while (!found && SystemClock.elapsedRealtime() < deadline);
  if (!found) throw new AssertionError("Search history menu did not show the saved query");
  remoteKey(KeyEvent.KEYCODE_BACK);
  waitFor(() -> app.hasWindowFocus(), 2500, "Catalogue restored after Search History");

  selectEpisode(0);
  waitForEpisode(0);
  openControls();
  runOnMainSync(() -> {
   if (control(R.id.exo_prev).isEnabled()) throw new AssertionError("Previous enabled on first episode");
  });
  selectEpisodeControl(R.id.exo_next, KeyEvent.KEYCODE_DPAD_RIGHT);
  waitForEpisode(1);
  openControls();
  waitFor(() -> {
   View skip = findButton(app.video.bar, "Skip opening");
   return skip != null && skip.isShown();
  }, 2200, "Marked opening is available in the toolbar");
  runOnMainSync(() -> {
   control(R.id.exo_play_pause).requestFocus();
   if (!"Skip opening".contentEquals(((TextView) app.video.bar.getChildAt(1)).getText())) {
    throw new AssertionError("Skip Opening must sit beside Subtitles");
   }
  });
  remoteKey(KeyEvent.KEYCODE_DPAD_DOWN);
  remoteKey(KeyEvent.KEYCODE_DPAD_DOWN);
  remoteKey(KeyEvent.KEYCODE_DPAD_RIGHT);
  runOnMainSync(() -> {
   if (!findButton(app.video.bar, "Skip opening").hasFocus()) {
    throw new AssertionError("D-pad cannot reach Skip Opening");
   }
  });
  remoteKey(KeyEvent.KEYCODE_DPAD_CENTER);
  waitFor(() -> app.video.player.getCurrentPosition() >= 89000, 10000, "Skip Opening seeks to the marked end");
  openControls();
  runOnMainSync(() -> {
   if (findButton(app.video.bar, "Skip opening").isShown()) {
    throw new AssertionError("Skip Opening remained outside the opening");
   }
  });
  selectEpisodeControl(R.id.exo_prev, KeyEvent.KEYCODE_DPAD_LEFT);
  waitForEpisode(0);
  runOnMainSync(() -> {
   long duration = app.video.player.getDuration();
   if (duration <= 0) throw new AssertionError("Expected finite video duration");
   app.video.player.seekTo(duration - 1000);
  });
  waitForEpisode(1);
  runOnMainSync(() -> {
   if (app.video.surface.isControllerFullyVisible()) throw new AssertionError("Autoplay opened the controls");
   if (app.video.player.getCurrentPosition() > 15000) throw new AssertionError("Autoplay reused the old position");
  });
  result.putString("episodes", "PASS: search history, D-pad Next/Previous, opening skip and automatic next");
 }

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
  if ("playback-remote".equals(check)) checkBlackBars();

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
 private void checkBlackBars() {
  int[] bounds = new int[4];
  runOnMainSync(() -> {
   View videoSurface = app.video.surface.getVideoSurfaceView();
   videoSurface.getLocationOnScreen(bounds);
   bounds[2] = videoSurface.getWidth();
   bounds[3] = videoSurface.getHeight();
  });
  Bitmap screenshot = getUiAutomation().takeScreenshot();
  if (screenshot == null || bounds[0] < 16
    || bounds[0] + bounds[2] + 8 >= screenshot.getWidth()) {
   throw new AssertionError("Expected both side bars in a 4:3 playback screenshot");
  }
  int y = bounds[1] + bounds[3] / 2;
  if (screenshot.getPixel(bounds[0] - 8, y) != Color.BLACK
    || screenshot.getPixel(bounds[0] + bounds[2] + 8, y) != Color.BLACK
    || screenshot.getPixel(bounds[0] + bounds[2] / 2, y) == Color.BLACK) {
   throw new AssertionError("Displayed side bars must be black with video visible between them");
  }
  screenshot.recycle();
 }
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
