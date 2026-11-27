package dev.mero.stream;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.widget.*;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.HttpDataSource;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Provider extraction adapted from pystardust/ani-cli, GPL-3.0. */
@androidx.media3.common.util.UnstableApi
public class MainActivity extends Activity {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    PosterPreview posters;ImageView poster;
    LinearLayout root, rows; EditText query; TextView status; StreamPlayer video;
    private int generation;
    private Future<?> pending;
    private boolean loadingEpisode;
    private boolean foreground;
    private String mode = "sub";
    boolean episodes;
    private boolean historyOpen;
    private Button resumeButton;
    private SearchHistory searches;
    private View returnFocus;

    record Selection(String series, String query, String preference, Provider.Episode episode, long startMs) {}
    record Resolved(Provider.Stream stream, List<Provider.Episode> episodes) {}
    record Playback(Selection selection, List<Provider.Episode> episodes, int index, String mode) {
        boolean hasEpisode(int offset) {
            return index >= 0 && index + offset >= 0 && index + offset < episodes.size();
        }
    }
    Playback playing;

    final android.os.Handler progressHandler=new android.os.Handler(android.os.Looper.getMainLooper());
    final Runnable progressTick=new Runnable(){public void run(){saveProgress();progressHandler.postDelayed(this,5000);}};
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
        searches = new SearchHistory(this);
        searches.seed(new WatchHistory(this), getPreferences(0).getString("query", ""));
        home();
        progressHandler.postDelayed(progressTick, 5000);
    }
    TextView text(String s, int size) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(0xff71f7bc); t.setTypeface(Typeface.MONOSPACE); t.setPadding(12,10,12,10); return t; }
    void home() {
        if(posters!=null)posters.close();
        episodes=false;historyOpen=false;
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
        resumeButton.setOnClickListener(v -> {
            SharedPreferences saved = getPreferences(0);
            playSaved(saved.getString("slug", ""), saved.getString("query", ""),
                saved.getString("preference", saved.getString("mode", "sub")),
                saved.getString("episode", ""), saved.getString("label", ""), saved.getLong("position", 0));
        });
        shortcuts.addView(resumeButton,new LinearLayout.LayoutParams(0,-2,1));
        Button watched = menuButton("Last watched");
        watched.setOnClickListener(v -> showHistory());
        addShortcut(shortcuts, watched);
        Button searchHistory = menuButton("Search history");
        searchHistory.setOnClickListener(v -> showSearchHistory());
        addShortcut(shortcuts, searchHistory);
        status=text("READY // enter a title",13);left.addView(status);
        ScrollView scroll=new ScrollView(this);rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);scroll.addView(rows);left.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        poster=new ImageView(this);poster.setScaleType(ImageView.ScaleType.FIT_CENTER);poster.setVisibility(View.INVISIBLE);poster.setFocusable(false);body.addView(poster,new LinearLayout.LayoutParams(0,-1,2));posters=new PosterPreview(this,poster);
        root.addView(body,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    Button menuButton(String label){Button button=new Button(this);FocusStyle.apply(button);button.setText(label);button.setTextSize(14);button.setTypeface(Typeface.MONOSPACE);button.setPadding(dp(8),dp(6),dp(8),dp(6));button.setMinimumHeight(dp(40));button.setMinHeight(dp(40));return button;}

    private void addShortcut(LinearLayout row, Button button) {
        LinearLayout.LayoutParams layout = new LinearLayout.LayoutParams(0, -2, 1);
        layout.leftMargin = dp(6);
        row.addView(button, layout);
    }

    int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    Button row(String label, Runnable action) {
        Button b=new Button(this); b.setText(label); b.setAllCaps(false);
        b.setTypeface(Typeface.MONOSPACE); b.setTextSize(16); b.setTextColor(0xffd4eeff);
        b.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.LEFT);
        b.setPadding(dp(12),dp(8),dp(12),dp(8)); b.setMinHeight(dp(48));b.setMinimumHeight(dp(48));
        FocusStyle.apply(b); b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(4);rows.addView(b,lp);return b;
    }
    private void cancelRequest() {
        generation++;
        if (pending != null) pending.cancel(true);
        pending = null;
        loadingEpisode = false;
    }

    private <T> void task(Callable<T> request, Consumer<T> result, Consumer<Exception> failure) {
        int ticket = ++generation;
        if (pending != null) pending.cancel(true);
        pending = worker.submit(() -> {
            try {
                T value = request.call();
                runOnUiThread(() -> {
                    if (ticket == generation) result.accept(value);
                });
            } catch (Exception error) {
                runOnUiThread(() -> {
                    if (ticket == generation) failure.accept(error);
                });
            }
        });
    }

    private String providerError(Exception error) {
        if (error instanceof Provider.Unavailable) return error.getMessage();
        if (error instanceof SocketTimeoutException) return "Provider timed out. Try again.";
        if (error instanceof java.io.IOException) return "Could not reach the provider. Try again.";
        return "Provider returned an unreadable response. Try again.";
    }

    void search() { search(true); }

    private void search(boolean record) {
        String submitted = query.getText().toString().trim();
        if (submitted.isEmpty()) return;
        cancelRequest();
        if (record) searches.record(submitted);
        episodes = false;
        historyOpen = false;
        posters.show(null);
        poster.setVisibility(View.INVISIBLE);
        rows.removeAllViews();
        status.setText("SEARCHING...");
        task(() -> Provider.search(submitted), found -> {
            for (Provider.SearchResult result : found) {
                Button entry = row(result.title, () -> episodes(result.id, submitted));
                entry.setOnFocusChangeListener((v, focused) -> {
                    if (!focused) return;
                    poster.setVisibility(result.poster.startsWith("https://") ? View.VISIBLE : View.INVISIBLE);
                    poster.setContentDescription(result.title + " poster");
                    posters.show(result.poster, Provider.BASE + "/" + result.id);
                });
            }
            status.setText(found.isEmpty() ? "NO RESULTS" : found.size() + " RESULTS");
            if (rows.getChildCount() > 0) rows.getChildAt(0).requestFocus();
        }, error -> status.setText("SEARCH FAILED // " + providerError(error)));
    }

    void showSearchHistory() {
        List<String> terms = searches.terms();
        AlertDialog.Builder menu = new AlertDialog.Builder(this)
            .setTitle("Search history").setNegativeButton("Back", null);
        if (terms.isEmpty()) menu.setMessage("No searches yet.");
        else menu.setItems(terms.toArray(new String[0]), (dialog, index) -> {
            query.setText(terms.get(index));
            search();
        });
        menu.show();
    }

    private void episodes(String series, String submitted) {
        cancelRequest();
        episodes = true;
        rows.removeAllViews();
        status.setText("LOADING EPISODES...");
        task(() -> Provider.episodes(series), found -> {
            for (Provider.Episode episode : found) {
                row("EP " + episode.label() + " > PLAY", () ->
                    loadEpisode(new Selection(series, submitted, mode, episode, 0), found, false));
            }
            status.setText(found.isEmpty() ? "NO EPISODES AVAILABLE" : found.size() + " EPISODES");
            if (rows.getChildCount() > 0) rows.getChildAt(0).requestFocus();
        }, error -> status.setText("EPISODES UNAVAILABLE // " + providerError(error)));
    }

    private void playSaved(String series, String submitted, String preference, String id,
                           String label, long position) {
        loadEpisode(new Selection(series, submitted, preference, new Provider.Episode(id, label), position),
            Collections.emptyList(), false);
    }

    private void loadEpisode(Selection selection, List<Provider.Episode> knownEpisodes, boolean automatic) {
        if (video != null && loadingEpisode) return;
        String message = "Loading episode " + selection.episode().label() + "…";
        if (video == null) {
            status.setText(message);
        } else {
            saveProgress();
            video.pause();
            if (!automatic) video.showMessage(message);
        }
        loadingEpisode = true;
        task(() -> {
            Provider.Stream stream = Provider.resolve(selection.episode().id(), selection.preference());
            List<Provider.Episode> list = knownEpisodes;
            if (list.isEmpty() && !selection.series().isEmpty()) {
                try {
                    list = Provider.episodes(selection.series());
                } catch (java.io.IOException | org.json.JSONException ignored) {
                    // A saved episode can still play if the catalogue is unavailable.
                }
            }
            return new Resolved(stream, list);
        }, resolved -> {
            loadingEpisode = false;
            int index = -1;
            for (int i = 0; i < resolved.episodes().size(); i++) {
                if (selection.episode().id().equals(resolved.episodes().get(i).id())) {
                    index = i;
                    break;
                }
            }
            playing = new Playback(selection, resolved.episodes(), index, resolved.stream().mode());
            status.setText(playbackLabel(playing));
            play(resolved.stream(), selection.startMs(), !automatic);
        }, error -> {
            loadingEpisode = false;
            String failure = "CAN'T PLAY EPISODE " + selection.episode().label() + " // " + providerError(error);
            if (video == null) status.setText(failure);
            else video.showMessage(failure);
        });
    }

    private String playbackLabel(Playback playback) {
        return "Episode " + playback.selection().episode().label() + " · "
            + playback.mode().toUpperCase(Locale.ROOT) + " source";
    }

    private void moveEpisode(int offset, boolean automatic) {
        if (playing == null || !playing.hasEpisode(offset)) return;
        Selection previous = playing.selection();
        Provider.Episode episode = playing.episodes().get(playing.index() + offset);
        loadEpisode(new Selection(previous.series(), previous.query(), previous.preference(), episode, 0),
            playing.episodes(), automatic);
    }

    void play(Provider.Stream stream, long position, boolean showControls) {
        if (video == null) returnFocus = getCurrentFocus();
        else video.release();
        Playback playback = playing;
        Runnable previous = playback != null && playback.hasEpisode(-1) ? () -> moveEpisode(-1, false) : null;
        Runnable next = playback != null && playback.hasEpisode(1) ? () -> moveEpisode(1, false) : null;
        StreamPlayer opened = new StreamPlayer(this, stream, playback == null ? "" : playbackLabel(playback),
            previous, next);
        video = opened;
        opened.player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (video != opened || state != Player.STATE_ENDED) return;
                if (foreground && playback != null && playback.hasEpisode(1)) moveEpisode(1, true);
                else saveProgress();
            }

            @Override public void onPlayerError(PlaybackException error) {
                if (video != opened) return;
                saveProgress();
                cancelRequest();
                closePlayback();
                String detail = error.getCause() instanceof HttpDataSource.InvalidResponseCodeException http
                    ? "Server returned HTTP " + http.responseCode + "."
                    : "Could not load the video. Try again or choose another episode.";
                status.setText("PLAYBACK FAILED // " + detail);
            }
        });
        setContentView(opened.view);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        opened.surface.requestFocus();
        if (showControls) opened.interact();
        if (position > 0) opened.player.seekTo(position);
        opened.player.prepare();
        opened.player.play();
    }

    private void closePlayback() {
        StreamPlayer closing = video;
        video = null;
        playing = null;
        closing.release();
        getWindow().getDecorView().setSystemUiVisibility(0);
        setContentView(root);
        if (returnFocus != null) {
            returnFocus.requestFocus();
            returnFocus = null;
        }
    }
    void showHistory(){
        cancelRequest();
        historyOpen=true;episodes=false;posters.show(null);poster.setVisibility(View.INVISIBLE);rows.removeAllViews();JSONArray entries=new WatchHistory(this).entries();
        for(int i=0;i<entries.length();i++){JSONObject item=entries.optJSONObject(i);if(item==null)continue;
            String title=item.optString("slug").replaceFirst("-[0-9]+$", "").replace('-', ' ');
            Button entry=row(title+" // EP "+item.optString("label","?")+" // "+(item.optBoolean("completed")?"Completed":item.optLong("position")/60000+" min")+" // "+item.optString("mode"), () ->
                playSaved(item.optString("slug"), item.optString("query"), item.optString("preference", item.optString("mode", "sub")),
                    item.optString("episode"), item.optString("label"), item.optLong("position")));
            entry.setOnFocusChangeListener((v,focused)->{if(focused){String id=item.optString("slug");poster.setVisibility(id.isEmpty()?View.INVISIBLE:View.VISIBLE);poster.setContentDescription(title+" poster");posters.show(null,id.isEmpty()?null:Provider.BASE+"/"+id);}});
        }
        status.setText(entries.length()==0?"NO WATCH HISTORY YET":"LAST WATCHED // latest 20 episodes");
        if(rows.getChildCount()>0)rows.getChildAt(0).requestFocus();
    }
    void saveProgress() {
        if (video == null || playing == null) return;
        Selection selection = playing.selection();
        Provider.Episode episode = selection.episode();
        long position = video.player.getCurrentPosition();
        boolean completed = video.player.getPlaybackState() == Player.STATE_ENDED;
        if (position < 1_000 && !completed) return;
        new WatchHistory(this).save(episode.id(), selection.series(), selection.query(), playing.mode(),
            selection.preference(), episode.label(), position, completed);
        SharedPreferences.Editor resume = getPreferences(0).edit();
        if (completed) {
            resume.clear().apply();
            resumeButton.setVisibility(View.GONE);
        } else {
            resume.putString("label", episode.label()).putString("episode", episode.id())
                .putString("slug", selection.series()).putString("mode", playing.mode())
                .putString("preference", selection.preference()).putString("query", selection.query())
                .putLong("position", position).apply();
            resumeButton.setVisibility(View.VISIBLE);
        }
    }
    @Override public boolean dispatchKeyEvent(android.view.KeyEvent event){
        int key=event.getKeyCode();
        if((getApplicationInfo().flags&android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE)!=0&&event.getAction()==android.view.KeyEvent.ACTION_DOWN&&((key>=19&&key<=23)||key==24||key==25||key==164)){
            android.content.SharedPreferences diagnostic=getSharedPreferences("remote-diagnostics",0);String name="key"+key;diagnostic.edit().putInt(name,diagnostic.getInt(name,0)+1).apply();
        }
        return (video != null && video.key(event)) || super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() {
        if (video != null && (video.volume.dismissShortcut() || video.dismissControls())) return;
        cancelRequest();
        if (video != null) {
            saveProgress();
            closePlayback();
        } else if (historyOpen) home();
        else if (episodes) search(false);
        else super.onBackPressed();
    }

    @Override protected void onResume() {
        super.onResume();
        foreground = true;
    }

    @Override protected void onPause() {
        foreground = false;
        if (loadingEpisode) {
            cancelRequest();
            if (video == null) status.setText("Select an episode to play.");
            else video.setMessage(playbackLabel(playing));
        }
        if (video != null) {
            saveProgress();
            video.pause();
        }
        super.onPause();
    }

    @Override protected void onDestroy() {
        cancelRequest();
        progressHandler.removeCallbacks(progressTick);
        if (posters != null) posters.close();
        saveProgress();
        if (video != null) {
            StreamPlayer closing = video;
            video = null;
            closing.release();
        }
        worker.shutdownNow();
        super.onDestroy();
    }
}
