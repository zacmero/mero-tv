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
    PosterPreview posters;ImageView poster;
    LinearLayout root, rows; EditText query; TextView status; StreamPlayer video;
    int generation; String mode="sub"; String slug; boolean episodes; String episodeId="",episodeLabel=""; boolean historyOpen; long resumePosition; Button resumeButton;
    final android.os.Handler progressHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    final Runnable progressTick=new Runnable(){public void run(){saveProgress();progressHandler.postDelayed(this,5000);}};
    interface Job { void run() throws Exception; }
    @Override public void onCreate(Bundle b) { super.onCreate(b);setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC); home(); progressHandler.postDelayed(progressTick,5000); }
    TextView text(String s, int size) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xff71f7bc); t.setTypeface(Typeface.MONOSPACE); t.setPadding(12,10,12,10); return t; }
    void home() {
        if(posters!=null)posters.close();
        video=null; episodes=false;historyOpen=false;
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(10),dp(18),dp(10));root.setBackgroundColor(0xff080e15);
        LinearLayout brand=new LinearLayout(this);brand.setGravity(android.view.Gravity.CENTER_VERTICAL);
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.icon);logo.setContentDescription("mero-stream logo");brand.addView(logo,new LinearLayout.LayoutParams(dp(40),dp(40)));brand.addView(text("mero-stream // anime terminal",22));root.addView(brand);
        LinearLayout body=new LinearLayout(this),left=new LinearLayout(this);left.setOrientation(LinearLayout.VERTICAL);left.setPadding(0,0,dp(18),0);body.addView(left,new LinearLayout.LayoutParams(0,-1,3));
        query=new EditText(this);query.setSingleLine(true);query.setTextSize(17);query.setTextColor(Color.WHITE);query.setHintTextColor(0xff95a5b0);query.setHint("Search anime_ ");query.setTypeface(Typeface.MONOSPACE);left.addView(query);
        LinearLayout actions=new LinearLayout(this);left.addView(actions);
        Button search=menuButton("> SEARCH");search.setOnClickListener(v->search());actions.addView(search,new LinearLayout.LayoutParams(0,-2,1));
        Button language=menuButton("Audio: "+mode.toUpperCase(Locale.ROOT));language.setOnClickListener(v->{mode=mode.equals("sub")?"dub":"sub";language.setText("Audio: "+mode.toUpperCase(Locale.ROOT));});LinearLayout.LayoutParams actionParams=new LinearLayout.LayoutParams(0,-2,1);actionParams.leftMargin=dp(6);actions.addView(language,actionParams);
        query.setOnEditorActionListener((v,action,event)->{search();return true;});
        LinearLayout shortcuts=new LinearLayout(this);LinearLayout.LayoutParams shortcutParams=new LinearLayout.LayoutParams(-1,-2);shortcutParams.topMargin=dp(6);left.addView(shortcuts,shortcutParams);
        resumeButton=menuButton("Continue watching");resumeButton.setVisibility(getPreferences(0).contains("episode")?View.VISIBLE:View.GONE);
        resumeButton.setOnClickListener(v->{android.content.SharedPreferences prefs=getPreferences(0);mode=prefs.getString("mode","sub");slug=prefs.getString("slug","");query.setText(prefs.getString("query",""));episodeLabel=prefs.getString("label","");resumePosition=prefs.getLong("position",0);resolve(prefs.getString("episode",""));});shortcuts.addView(resumeButton,new LinearLayout.LayoutParams(0,-2,1));
        Button watched=menuButton("Last watched");watched.setOnClickListener(v->showHistory());LinearLayout.LayoutParams watchedParams=new LinearLayout.LayoutParams(0,-2,1);watchedParams.leftMargin=dp(6);shortcuts.addView(watched,watchedParams);
        status=text("READY // enter a title",13);left.addView(status);
        ScrollView scroll=new ScrollView(this);rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);left.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.FIT_CENTER);poster.setVisibility(View.INVISIBLE);poster.setFocusable(false);body.addView(poster,new LinearLayout.LayoutParams(0,-1,2));posters=new PosterPreview(this,poster);
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    Button menuButton(String label){Button button=new Button(this);FocusStyle.apply(button);button.setText(label);button.setTextSize(14);button.setTypeface(Typeface.MONOSPACE);button.setPadding(dp(8),dp(6),dp(8),dp(6));button.setMinimumHeight(dp(40));button.setMinHeight(dp(40));return button;}

    int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    Button row(String label, Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false);
        b.setTypeface(Typeface.MONOSPACE); b.setTextSize(16); b.setTextColor(0xffd4eeff);
        b.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.LEFT);
        b.setPadding(dp(12),dp(8),dp(12),dp(8)); b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));
        FocusStyle.apply(b); b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(4);rows.addView(b,lp);return b;
    }
    void task(String message,Job job) {
        int g=++generation;status.setText(message);
        worker.submit(()->{try{job.run();}catch(Exception e){runOnUiThread(()->{if(g==generation){status.setText("ERROR // "+e.getClass().getSimpleName()+" — retry or search again");}});}});
    }
    void search() {
        String q=query.getText().toString().trim();if(q.isEmpty())return;episodes=false;posters.show(null);poster.setVisibility(View.INVISIBLE);rows.removeAllViews();
        task("SEARCHING...",()->{
            String html=get(base+"/search?keyword="+URLEncoder.encode(q,"UTF-8")).split("id=\"main-sidebar\"")[0];
            List<Provider.SearchResult> found=Provider.searchResults(html);
            runOnUiThread(()->{rows.removeAllViews();for(Provider.SearchResult x:found){Button entry=row(x.title,()->episodes(x.id));entry.setOnFocusChangeListener((v,focused)->{if(focused){poster.setVisibility(x.poster.startsWith("https://")?View.VISIBLE:View.INVISIBLE);poster.setContentDescription(x.title+" poster");posters.show(x.poster,Provider.BASE+"/"+x.id);}});}status.setText(found.isEmpty()?"NO RESULTS // provider may be unavailable":found.size()+" RESULTS");if(rows.getChildCount()>0)rows.getChildAt(0).requestFocus();});
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
        historyOpen=true;episodes=false;posters.show(null);poster.setVisibility(View.INVISIBLE);rows.removeAllViews();org.json.JSONArray entries=new WatchHistory(this).entries();
        for(int i=0;i<entries.length();i++){JSONObject item=entries.optJSONObject(i);if(item==null)continue;
            String title=item.optString("slug").replaceFirst("-[0-9]+$", "").replace('-', ' ');
            Button entry=row(title+" // EP "+item.optString("label","?")+" // "+(item.optBoolean("completed")?"Completed":item.optLong("position")/60000+" min")+" // "+item.optString("mode"),()->{
                slug=item.optString("slug");mode=item.optString("mode","sub");query.setText(item.optString("query"));episodeLabel=item.optString("label");resumePosition=item.optLong("position");resolve(item.optString("episode"));
            });
            entry.setOnFocusChangeListener((v,focused)->{if(focused){String id=item.optString("slug");poster.setVisibility(id.isEmpty()?View.INVISIBLE:View.VISIBLE);poster.setContentDescription(title+" poster");posters.show(null,id.isEmpty()?null:Provider.BASE+"/"+id);}});
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
        if(video!=null&&video.volume.key(event))return true;if(video!=null&&video.seekKey(event))return true;if(video!=null&&video.timelineKey(event))return true;if(video!=null&&event.getAction()==android.view.KeyEvent.ACTION_DOWN&&event.getKeyCode()!=android.view.KeyEvent.KEYCODE_BACK)video.interact();return super.dispatchKeyEvent(event);}
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event){if(video!=null&&event.getAction()==android.view.MotionEvent.ACTION_DOWN)video.interact();return super.dispatchTouchEvent(event);}
    @Override public void onBackPressed(){generation++;if(video!=null){if(video.volume.dismissShortcut()||video.dismissControls())return;saveProgress();video.release();video=null;getWindow().getDecorView().setSystemUiVisibility(0);getWindow().clearFlags(128);setContentView(root);}else if(historyOpen){home();}else if(episodes){search();}else super.onBackPressed();}
    @Override protected void onPause(){if(video!=null){saveProgress();video.pause();}super.onPause();}
    @Override protected void onDestroy(){generation++;progressHandler.removeCallbacks(progressTick);if(posters!=null)posters.close();saveProgress();if(video!=null)video.release();worker.shutdownNow();super.onDestroy();}
}
