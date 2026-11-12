package dev.mero.stream;
import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.view.KeyEvent;
import android.widget.*;
import androidx.media3.common.Player;

/** Local gain fallback for fixed-volume HDMI devices; never boosts above source gain. */
final class PlayerVolume {
 final Activity activity;final Player player;final SharedPreferences prefs;
 PlayerVolume(Activity activity,Player player){this.activity=activity;this.player=player;prefs=activity.getSharedPreferences("player-volume",0);apply();}
 void apply(){player.setVolume(prefs.getBoolean("muted",false)?0:Math.max(0,Math.min(1,prefs.getFloat("gain",1))));}
 void change(float amount){float gain=Math.max(0,Math.min(1,prefs.getFloat("gain",1)+amount));prefs.edit().putFloat("gain",gain).putBoolean("muted",false).apply();apply();}
 void mute(){prefs.edit().putBoolean("muted",!prefs.getBoolean("muted",false)).apply();apply();}
 boolean key(KeyEvent event){
  int key=event.getKeyCode();if(key!=KeyEvent.KEYCODE_VOLUME_UP&&key!=KeyEvent.KEYCODE_VOLUME_DOWN&&key!=KeyEvent.KEYCODE_VOLUME_MUTE&&key!=KeyEvent.KEYCODE_MUTE)return false;
  AudioManager audio=(AudioManager)activity.getSystemService(Context.AUDIO_SERVICE);
  if(audio==null||!audio.isVolumeFixed())return false;
  if(event.getAction()==KeyEvent.ACTION_DOWN){if(key==KeyEvent.KEYCODE_VOLUME_UP)change(.05f);else if(key==KeyEvent.KEYCODE_VOLUME_DOWN)change(-.05f);else if(event.getRepeatCount()==0)mute();Toast.makeText(activity,"Player volume: "+Math.round(player.getVolume()*100)+"%",Toast.LENGTH_SHORT).show();}return true;
 }
 AlertDialog show(){
  LinearLayout layout=new LinearLayout(activity);layout.setOrientation(LinearLayout.VERTICAL);
  TextView level=new TextView(activity);level.setTextSize(22);level.setPadding(24,16,24,16);layout.addView(level);
  Runnable refresh=()->level.setText("Player volume: "+Math.round(player.getVolume()*100)+"%");refresh.run();
  for(String label:new String[]{"Quieter −","Louder +","Mute / unmute"}){Button b=new Button(activity);FocusStyle.apply(b);b.setText(label);b.setOnClickListener(v->{if(label.startsWith("Quieter"))change(-.05f);else if(label.startsWith("Louder"))change(.05f);else mute();refresh.run();});layout.addView(b);}
  AlertDialog dialog=new AlertDialog.Builder(activity).setTitle("Player volume").setView(layout).setNegativeButton("Done",null).create();dialog.show();layout.getChildAt(1).requestFocus();return dialog;
 }
}
