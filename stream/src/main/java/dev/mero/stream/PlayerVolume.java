package dev.mero.stream;
import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.widget.*;
import androidx.media3.common.Player;

/** Local gain for received volume keys, with a brief non-interactive level HUD. */
final class PlayerVolume {
 final Activity activity;final Player player;final SharedPreferences prefs;
 final LinearLayout hud;final TextView hudText;final ProgressBar hudBar;
 final android.os.Handler handler=new android.os.Handler(android.os.Looper.getMainLooper());
 boolean remoteMode;
 final Runnable hide=()->hideLevel();
 void hideLevel(){remoteMode=false;hud.setVisibility(android.view.View.GONE);}
 void showLevel(){showLevel(900);}
 void showLevel(long timeout){hudText.setText("Volume "+Math.round(player.getVolume()*100)+"%"+(remoteMode?" · ↑ / ↓":""));hudBar.setProgress(Math.round(player.getVolume()*100));hud.setVisibility(android.view.View.VISIBLE);handler.removeCallbacks(hide);handler.postDelayed(hide,timeout);}
 boolean dismissShortcut(){if(!remoteMode)return false;handler.removeCallbacks(hide);hideLevel();return true;}
 void release(){handler.removeCallbacks(hide);}
 PlayerVolume(Activity activity,Player player,FrameLayout container){this.activity=activity;this.player=player;prefs=activity.getSharedPreferences("player-volume",0);
 hud=new LinearLayout(activity);hud.setOrientation(LinearLayout.VERTICAL);hud.setBackgroundColor(0xe6101b27);hud.setPadding(20,12,20,12);
 hudText=new TextView(activity);hudText.setTextColor(0xff8cceff);hudText.setTextSize(17);hud.addView(hudText);
 hudBar=new ProgressBar(activity,null,android.R.attr.progressBarStyleHorizontal);hudBar.setMax(100);hudBar.setProgressTintList(android.content.res.ColorStateList.valueOf(0xff6abfff));hud.addView(hudBar);
 FrameLayout.LayoutParams layout=new FrameLayout.LayoutParams(Math.round(210*activity.getResources().getDisplayMetrics().density),-2,android.view.Gravity.RIGHT|android.view.Gravity.CENTER_VERTICAL);layout.rightMargin=24;container.addView(hud,layout);hud.setVisibility(android.view.View.GONE);apply();}
 void apply(){player.setVolume(prefs.getBoolean("muted",false)?0:Math.max(0,Math.min(1,prefs.getFloat("gain",1))));}
 void change(float amount){float gain=Math.max(0,Math.min(1,prefs.getFloat("gain",1)+amount));prefs.edit().putFloat("gain",gain).putBoolean("muted",false).apply();apply();showLevel();}
 void mute(){prefs.edit().putBoolean("muted",!prefs.getBoolean("muted",false)).apply();apply();showLevel();}
 boolean key(KeyEvent event){
  int key=event.getKeyCode();
  if(key==KeyEvent.KEYCODE_MENU){if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){if(remoteMode)hideLevel();else{remoteMode=true;showLevel(3000);}}return true;}
  if(remoteMode&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)){if(event.getAction()==KeyEvent.ACTION_DOWN)change(key==KeyEvent.KEYCODE_DPAD_UP?.05f:-.05f);return true;}
  if(key!=KeyEvent.KEYCODE_VOLUME_UP&&key!=KeyEvent.KEYCODE_VOLUME_DOWN&&key!=KeyEvent.KEYCODE_VOLUME_MUTE&&key!=KeyEvent.KEYCODE_MUTE)return false;
  if(event.getAction()==KeyEvent.ACTION_DOWN){if(key==KeyEvent.KEYCODE_VOLUME_UP)change(.05f);else if(key==KeyEvent.KEYCODE_VOLUME_DOWN)change(-.05f);else if(event.getRepeatCount()==0)mute();}return true;
 }
 AlertDialog show(){
  LinearLayout layout=new LinearLayout(activity);layout.setOrientation(LinearLayout.VERTICAL);
  TextView level=new TextView(activity);level.setTextSize(22);level.setPadding(24,16,24,16);layout.addView(level);
  Runnable refresh=()->level.setText("Player volume: "+Math.round(player.getVolume()*100)+"%");refresh.run();
  for(String label:new String[]{"Quieter −","Louder +","Mute / unmute"}){Button b=new Button(activity);FocusStyle.apply(b);b.setText(label);b.setOnClickListener(v->{if(label.startsWith("Quieter"))change(-.05f);else if(label.startsWith("Louder"))change(.05f);else mute();refresh.run();});layout.addView(b);}
  AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Player volume").setView(layout).setNegativeButton("Done",null).create();dialog.show();layout.getChildAt(1).requestFocus();return dialog;
 }
}
