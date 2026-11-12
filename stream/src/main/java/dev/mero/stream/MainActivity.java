package dev.mero.stream;

import android.app.Activity;
import static dev.mero.stream.Provider.*;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import android.net.Uri;
import android.text.Html;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/** Provider extraction adapted from pystardust/ani-cli, GPL-3.0. */
@androidx.media3.common.util.UnstableApi
public class MainActivity extends Activity {
    final ExecutorService worker = Executors.newSingleThreadExecutor();
    final String base = "https://hianime.at";
    LinearLayout root, rows; EditText query; TextView status; StreamPlayer video;
    int generation; String mode="sub"; String slug; boolean episodes; String episodeId="",episodeLabel=""; boolean historyOpen; long resumePosition; Button resumeButton;
    final android.os.Handler progressHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    final Runnable progressTick=new Runnable(){public void run(){saveProgress();progressHandler.postDelayed(this,5000);}};
    interface Job { void run() throws Exception; }
    @Override public void onCreate(Bundle b) { super.onCreate(b);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC); home(); progressHandler.postDelayed(progressTick,5000); }
    TextView text(String s, int size) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xff71f7bc); t.setTypeface(Typeface.MONOSPACE); t.setPadding(12,10,12,10); return t; }
    void home() {
        video=null; episodes=false;historyOpen=false;
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(36,20,36,20); root.setBackgroundColor(0xff080e15);
        LinearLayout brand=new LinearLayout(this);brand.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);logo.setImageResource(dev.mero.stream.R.drawable.stream_logo);logo.setContentDescription("mero-stream logo");brand.addView(logo,new LinearLayout.LayoutParams(dp(52),dp(52)));brand.addView(text("mero-stream // anime terminal",26));root.addView(brand);
        query=new EditText(this); query.setSingleLine(true); query.setTextColor(Color.WHITE); query.setHintTextColor(0xff95a5b0); query.setHint("Search anime_ "); query.setTypeface(Typeface.MONOSPACE); root.addView(query);
        Button search=new Button(this);FocusStyle.apply(search); search.setText("> SEARCH"); search.setOnClickListener(v->search());root.addView(search);
        query.setOnEditorActionListener((v,a,e)->{search();return true;});
        Button language=new Button(this);FocusStyle.apply(language);language.setText("SOURCE AUDIO: SUB");language.setOnClickListener(v->{mode=mode.equals("sub")?"dub":"sub";language.setText("SOURCE AUDIO: "+mode.toUpperCase(java.util.Locale.ROOT));});root.addView(language);
        resumeButton=new Button(this);FocusStyle.apply(resumeButton);resumeButton.setText("Continue watching");
        resumeButton.setVisibility(getPreferences(0).contains("episode")?View.VISIBLE:View.GONE);
        resumeButton.setOnClickListener(v->{android.content.SharedPreferences p=getPreferences(0);mode=p.getString("mode","sub");slug=p.getString("slug","");query.setText(p.getString("query",""));episodeLabel=p.getString("label","");resumePosition=p.getLong("position",0);resolve(p.getString("episode",""));});root.addView(resumeButton);
        Button watched=new Button(this);FocusStyle.apply(watched);watched.setText("Last watched");watched.setOnClickListener(v->showHistory());root.addView(watched);
        status=text("READY // enter a title",16);root.addView(status);
        ScrollView scroll=new ScrollView(this); rows=new LinearLayout(this); rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    void row(String label, Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false);
        b.setTypeface(Typeface.MONOSPACE); b.setTextSize(18); b.setTextColor(0xffd4eeff);
        b.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.LEFT);
        b.setPadding(dp(18),dp(14),dp(18),dp(14)); b.setMinHeight(dp(64));
        FocusStyle.apply(b); b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);rows.addView(b,lp);
    }
    void task(String message,Job job) {
        int g=++generation;status.setText(message);
        worker.submit(()->{try{job.run();}catch(Exception e){runOnUiThread(()->{if(g==generation){status.setText("ERROR // "+e.getClass().getSimpleName()+" — retry or search again");}});}});
    }
    void search() {
        String q=query.getText().toString().trim();if(q.isEmpty())return;episodes=false;rows.removeAllViews();
        task("SEARCHING...",()->{
            String html=get(base+"/search?keyword="+URLEncoder.encode(q,"UTF-8")).split("id=\"main-sidebar\"")[0];
            Matcher m=Pattern.compile("<h3 class=\"film-name\">\\s*<a href=\"[^\"]*/([^\"/]+)\"\\s*title=\"([^\"]*)\"",Pattern.DOTALL).matcher(html);
            List<String[]> found=new ArrayList<>();while(m.find()&&found.size()<50)found.add(new String[]{m.group(1),clean(m.group(2))});
            runOnUiThread(()->{rows.removeAllViews();for(String[] x:found)row(x[1],()->episodes(x[0]));status.setText(found.isEmpty()?"NO RESULTS // provider may be unavailable":found.size()+" RESULTS");if(rows.getChildCount()>0)rows.getChildAt(0).requestFocus();});
        });
    }
    void episodes(String id) {
        slug=id;episodes=true;rows.removeAllViews();
        task("LOADING EPISODES...",()->{
            JSONObject json=new JSONObject(get(base+"/api/theme/episode/list/"+id.substring(id.lastIndexOf('-')+1)));
            String html=json.optString("html");Matcher m=Pattern.compile("data-number=\"([^\"]*)\"[^>]*data-id=\"([0-9]+)\"").matcher(html);
            List<String[]> found=new ArrayList<>();while(m.find())found.add(new String[]{m.group(1),m.group(2)});
            runOnUiThread(()->{rows.removeAllViews();for(String[] x:found)row("EP "+x[0]+" > PLAY",()->{episodeLabel=x[0];resolve(x[1]);});status.setText(found.size()+" EPISODES // "+mode.toUpperCase(java.util.Locale.ROOT));if(rows.getChildCount()>0)rows.getChildAt(0).requestFocus();});
        });
    }
    void resolve(String id) {
        episodeId=id;
        task("RESOLVING STREAM...",()->{
            Provider.Stream stream=Provider.resolve(id,mode);
            runOnUiThread(()->play(stream.url,stream.referer,stream.subtitles));

        });
    }
    void play(String url,String referer) { play(url,referer,new org.json.JSONArray()); }
    void play(String url,String referer,org.json.JSONArray subtitles) {
        video=new StreamPlayer(this,url,referer,subtitles,()->{
            if(video!=null){video.release();video=null;}setContentView(root);
            status.setText("PLAYBACK ERROR // retry or choose another episode");
        });video.player.seekTo(resumePosition);resumePosition=0;setContentView(video.view);video.surface.requestFocus();
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }
    void showHistory(){
        historyOpen=true;episodes=false;rows.removeAllViews();org.json.JSONArray entries=new WatchHistory(this).entries();
        for(int i=0;i<entries.length();i++){JSONObject item=entries.optJSONObject(i);if(item==null)continue;
            String title=item.optString("slug").replaceFirst("-[0-9]+$", "").replace('-', ' ');
            row(title+" // EP "+item.optString("label","?")+" // "+(item.optBoolean("completed")?"Completed":item.optLong("position")/60000+" min")+" // "+item.optString("mode"),()->{
                slug=item.optString("slug");mode=item.optString("mode","sub");query.setText(item.optString("query"));episodeLabel=item.optString("label");resumePosition=item.optLong("position");resolve(item.optString("episode"));
            });
        }
        status.setText(entries.length()==0?"NO WATCH HISTORY YET":"LAST WATCHED // latest 20 episodes");
        if(rows.getChildCount()>0)rows.getChildAt(0).requestFocus();
    }
    void saveProgress(){if(video==null||episodeId.isEmpty())return;long pos=video.player.getCurrentPosition(),duration=video.player.getDuration();
        if(pos>=1000)new WatchHistory(this).save(episodeId,slug==null?"":slug,query.getText().toString(),mode,episodeLabel,pos,duration>0&&pos>=duration-5000);
        if(duration>0&&pos>=duration-5000){getPreferences(0).edit().clear().apply();resumeButton.setVisibility(View.GONE);return;}
        if(pos<1000)return;
        getPreferences(0).edit().putString("label",episodeLabel).putString("episode",episodeId).putString("slug",slug).putString("mode",mode).putString("query",query.getText().toString()).putLong("position",pos).apply();resumeButton.setVisibility(View.VISIBLE);
    }
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){
        int key=event.getKeyCode();
        if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0&&event.getAction()==android.view.KeyEvent.ACTION_DOWN&&((key>=19&&key<=23)||key==24||key==25||key==164)){
            android.content.SharedPreferences diagnostic=getSharedPreferences("remote-diagnostics",0);String name="key"+key;diagnostic.edit().putInt(name,diagnostic.getInt(name,0)+1).apply();
        }
        if(video!=null&&video.volume.key(event))return true;if(video!=null&&event.getAction()==android.view.KeyEvent.ACTION_DOWN&&event.getKeyCode()!=android.view.KeyEvent.KEYCODE_BACK)video.interact();return super.dispatchKeyEvent(event);}
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){if(video!=null&&event.getAction()==android.view.MotionEvent.ACTION_DOWN)video.interact();return super.dispatchTouchEvent(event);}
    @Override public void onBackPressed(){generation++;if(video!=null){if(video.dismissControls())return;saveProgress();video.release();video=null;getWindow().getDecorView().setSystemUiVisibility(0);getWindow().clearFlags(128);setContentView(root);}else if(historyOpen){home();}else if(episodes){search();}else super.onBackPressed();}
    @Override protected void onPause(){if(video!=null){saveProgress();video.pause();}super.onPause();}
    @Override protected void onDestroy(){generation++;progressHandler.removeCallbacks(progressTick);saveProgress();if(video!=null)video.release();worker.shutdownNow();super.onDestroy();}
}
