package dev.mero.stream;
import android.app.Instrumentation;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
public class StreamChecks extends Instrumentation {
 MainActivity app;
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){Bundle result=new Bundle();try{
  app=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
  runOnMainSync(()->{app.query.setText("naruto");app.search();});
  waitFor(()->app.rows.getChildCount()>0,20000,"Search results");
  runOnMainSync(()->app.rows.getChildAt(0).performClick());
  waitFor(()->app.episodes&&app.rows.getChildCount()>0,20000,"Episodes");
  runOnMainSync(()->app.rows.getChildAt(0).performClick());
  waitFor(()->app.video!=null,25000,"Stream resolution");
  waitFor(()->app.video!=null&&app.video.player.getCurrentPosition()>15000,40000,"Advancing playback");
  runOnMainSync(()->{result.putLong("positionMs",app.video.player.getCurrentPosition());result.putInt("trackGroups",app.video.player.getCurrentTracks().getGroups().size());result.putInt("height",app.video.player.getVideoSize().height);});
  result.putString("stream","PASS: search -> episode -> advancing native playback");finish(Activity.RESULT_OK,result);
 }catch(Throwable e){result.putString("stream","FAIL: "+e.getMessage());finish(Activity.RESULT_CANCELED,result);}}
 interface Check {boolean ok();}
 void waitFor(Check c,long timeout,String step)throws Exception{long end=SystemClock.elapsedRealtime()+timeout;while(SystemClock.elapsedRealtime()<end){boolean[] yes={false};runOnMainSync(()->yes[0]=c.ok());if(yes[0])return;Thread.sleep(300);}throw new Exception(step+" timed out");}
}
