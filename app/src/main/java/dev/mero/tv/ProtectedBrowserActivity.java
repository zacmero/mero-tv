package dev.mero.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import org.mozilla.geckoview.AllowOrDeny;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;
import org.mozilla.geckoview.PanZoomController;
import org.mozilla.geckoview.MediaSession;
import org.mozilla.geckoview.ScreenLength;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebRequestError;

/** Extension-capable page surface within Mero TV; the native library remains shared. */
public final class ProtectedBrowserActivity extends Activity {
    private ProtectedEngine engine;
    private GeckoSession session;
    private GeckoView page;
    private FrameLayout area;
    private LinearLayout tools;
    private TextView status, loading;
    private Button scrollButton, blockButton;
    private Pointer pointer;
    private String url, title = "";
    private boolean back, forward, fullscreen, scrollMode, blockerReady, currentWebPage;
    private float x = 160, y = 140;
    private MediaSession activeMedia;
    private String blockedCount = "";
    private boolean warmingProtection, inProtectionSettings, playing, protectionFailed;
    private final Runnable protectionTimeout = () -> {
        if (blockerReady || protectionFailed || isFinishing() || isDestroyed()) return;
        protectionFailed = true;
        warmingProtection = false;
        android.util.Log.w("MeroProtection", "Protection startup timed out");
        blockButton.setText("uBOL: error");
        status.setText("Protection did not become ready. Return to the library and reopen the site.");
        loading.setText("Protection could not start.\nReturn to the library to retry.");
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        url = state == null ? null : state.getString("url");
        if (!UrlPolicy.isWeb(url)) url = getIntent().getDataString();
        if (!UrlPolicy.isWeb(url)) { finish(); return; }
        LinearLayout layout = new LinearLayout(this); layout.setOrientation(LinearLayout.VERTICAL); layout.setBackgroundColor(0xff101417);
        tools = new LinearLayout(this); tools.setPadding(dp(12), dp(8), 0, 0); layout.addView(tools);
        add("Library", this::finish);
        add("Address", this::address);
        add("←", () -> { if (back) session.goBack(); });
        add("→", () -> { if (forward) session.goForward(); });
        add("Reload", () -> { if (blockerReady) session.reload(); else message("Preparing browser protection…"); });
        add("Save", this::save);
        scrollButton = add("Scroll: OFF", this::toggleScroll);
        blockButton = add("uBOL: loading", this::toggleBlocker);
        add("Controls", this::controls);
        status = new TextView(this); status.setTextColor(0xffa8b7b2); status.setTextSize(13); status.setPadding(dp(16), 0, dp(16), dp(5));
        status.setSingleLine(true); layout.addView(status);
        area = new FrameLayout(this); layout.addView(area, new LinearLayout.LayoutParams(-1, 0, 1));
        page = new GeckoView(this); area.addView(page, new FrameLayout.LayoutParams(-1, -1));
        pointer = new Pointer(); area.addView(pointer, new FrameLayout.LayoutParams(-1, -1));
        pointer.setVisibility(View.GONE);
        loading = new TextView(this); loading.setGravity(android.view.Gravity.CENTER);
        loading.setText("MERO TV\n\nPreparing browser protection…"); loading.setTextSize(22);
        loading.setTextColor(0xffc7ef79); loading.setBackgroundColor(0xff101417);
        area.addView(loading, new FrameLayout.LayoutParams(-1, -1));
        setContentView(layout); immersive();
        page.postDelayed(protectionTimeout, 45000);
        try {
            engine = ProtectedEngine.get(this);
            session = new GeckoSession();
            session.setMediaSessionDelegate(new MediaSession.Delegate() {
                @Override public void onActivated(GeckoSession s, MediaSession media) { activeMedia = media; }
                @Override public void onDeactivated(GeckoSession s, MediaSession media) {
                    if (activeMedia == media) { activeMedia = null; playing = false; page.setKeepScreenOn(false); }
                }
                @Override public void onPlay(GeckoSession s, MediaSession media) { activeMedia = media; playing = true; page.setKeepScreenOn(true); }
                @Override public void onPause(GeckoSession s, MediaSession media) { playing = false; page.setKeepScreenOn(false); }
                @Override public void onStop(GeckoSession s, MediaSession media) { playing = false; page.setKeepScreenOn(false); }
            });
            session.setNavigationDelegate(new GeckoSession.NavigationDelegate() {
                @Override public void onLocationChange(GeckoSession s, String location,
                        List<GeckoSession.PermissionDelegate.ContentPermission> permissions, Boolean gesture) {
                    currentWebPage = UrlPolicy.isWeb(location);
                    inProtectionSettings = warmingProtection && engine.blocker != null
                            && location.equals(engine.blocker.metaData.optionsPageUrl);
                    if (currentWebPage) url = location;
                    updateStatus();
                }
                @Override public void onCanGoBack(GeckoSession s, boolean value) { back = value; }
                @Override public void onCanGoForward(GeckoSession s, boolean value) { forward = value; }
                @Override public GeckoResult<AllowOrDeny> onLoadRequest(GeckoSession s, LoadRequest request) {
                    String base = engine.blocker == null ? null : engine.blocker.metaData.baseUrl;
                    boolean ownExtension = base != null && base.startsWith("moz-extension://") && request.uri.startsWith(base);
                    boolean allowed = (UrlPolicy.isWeb(request.uri) && blockerReady && !protectionFailed)
                            || ownExtension || request.uri.equals("about:license");
                    if (request.target == TARGET_WINDOW_NEW && !request.hasUserGesture) allowed = false;
                    android.util.Log.i("MeroProtection", "Navigation allowed=" + allowed + "; web=" + UrlPolicy.isWeb(request.uri));
                    return GeckoResult.fromValue(allowed ? AllowOrDeny.ALLOW : AllowOrDeny.DENY);
                }
                @Override public GeckoResult<String> onLoadError(GeckoSession s, String location, WebRequestError error) {
                    android.util.Log.w("MeroProtection", "Load failed category=" + error.category + "; code=" + error.code);
                    if (!blockerReady) { protectionTimeout.run(); return null; }
                    status.setText("Page could not load. Use Reload or return to the library.");
                    loading.setVisibility(View.GONE);
                    return null;
                }
                @Override public GeckoResult<GeckoSession> onNewSession(GeckoSession s, String destination) {
                    if (UrlPolicy.isWeb(destination)) new AlertDialog.Builder(ProtectedBrowserActivity.this)
                            .setTitle("Open requested window?").setMessage(android.net.Uri.parse(destination).getHost())
                            .setNegativeButton("Cancel", null).setPositiveButton("Open", (a, b) -> session.loadUri(destination)).show();
                    return null;
                }
            });
            session.setContentDelegate(new GeckoSession.ContentDelegate() {
                @Override public void onFirstContentfulPaint(GeckoSession s) {
                    if (protectionFailed || isFinishing() || isDestroyed()) return;
                    if (warmingProtection && inProtectionSettings) {
                        warmingProtection = false; page.removeCallbacks(protectionTimeout);
                        engine.filteringReady = true;
                        blockerReady = true; blockButton.setEnabled(true); updateStatus();
                        loadWebsite();
                    } else if (blockerReady && currentWebPage) {
                        loading.setVisibility(View.GONE); pointer.setVisibility(View.VISIBLE);
                    }
                }
                @Override public void onTitleChange(GeckoSession s, String value) { title = value == null ? "" : value; }
                @Override public void onFullScreen(GeckoSession s, boolean active) {
                    fullscreen = active; tools.setVisibility(active ? View.GONE : View.VISIBLE);
                    status.setVisibility(active ? View.GONE : View.VISIBLE); pointer.setVisibility(active ? View.GONE : View.VISIBLE);
                    if (active) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    immersive();
                }
                @Override public void onCrash(GeckoSession s) { message("The page stopped. Reopen it from the library."); finish(); }
            });
            session.setProgressDelegate(new GeckoSession.ProgressDelegate() {
                @Override public void onPageStart(GeckoSession s, String location) { blockedCount = ""; status.setText("Loading…"); }
                @Override public void onPageStop(GeckoSession s, boolean success) {
                    android.util.Log.i("MeroProtection", "Page stopped success=" + success);
                    if (success) updateStatus(); else status.setText("Page could not load. Try Reload.");
                }
            });
            updateStatus();
            session.open(engine.runtime); page.setSession(session); session.setActive(true);
            engine.runtime.getWebExtensionController().setTabActive(session, true);
            // The open foreground window is enough for extension startup. Do not
            // serialize it behind rendering a throwaway page in another process.
            prepareProtection();
        } catch (RuntimeException error) {
            android.util.Log.e("MeroProtection", "Engine startup failed", error);
            protectionFailed = true; page.removeCallbacks(protectionTimeout);
            status.setText("Browser could not start. Return to the library to retry.");
            loading.setText("Browser could not start.\nReturn to the library to retry.");
        }
    }
    private void prepareProtection() {
        engine.prepare().accept(extension -> {
                if (protectionFailed || isFinishing() || isDestroyed()) return;
                session.getWebExtensionController().setActionDelegate(extension, new WebExtension.ActionDelegate() {
                    @Override public void onBrowserAction(WebExtension e, GeckoSession s, WebExtension.Action action) {
                        if (action.badgeText != null && action.badgeText.matches("[0-9]+")) {
                            blockedCount = action.badgeText;
                            updateStatus();
                        }
                    }
                });
                if (engine.enabled() && !engine.filteringReady) prepareFiltering(extension);
                else { page.removeCallbacks(protectionTimeout); blockerReady = true; updateStatus(); loadWebsite(); }
            }, error -> {
                if (isFinishing() || isDestroyed()) return;
                protectionFailed = true; page.removeCallbacks(protectionTimeout);
                android.util.Log.e("MeroProtection", "Bundled extension startup failed", error);
                blockButton.setText("uBOL: error"); status.setText("Protection could not start. Retry or return to the library.");
                loading.setText("Protection could not start.\nReturn to the library to retry.");
            });
    }
    private void prepareFiltering(WebExtension extension) {
        // In the pinned Lite release, dashboard body.loading is hidden until
        // getOptionsPageData has awaited isFullyInitialized. Its first paint is
        // therefore later than Gecko's add-on onReady notification.
        boolean restarting = blockerReady;
        warmingProtection = true; inProtectionSettings = false; blockerReady = false;
        blockButton.setEnabled(false); blockButton.setText("uBOL: starting");
        loading.setVisibility(View.VISIBLE); pointer.setVisibility(View.GONE);
        if (restarting) { page.removeCallbacks(protectionTimeout); page.postDelayed(protectionTimeout, 30000); }
        session.load(new GeckoSession.Loader().uri(extension.metaData.optionsPageUrl)
                .flags(GeckoSession.LOAD_FLAGS_REPLACE_HISTORY));
    }
    private void loadWebsite() {
        session.load(new GeckoSession.Loader().uri(url).flags(
                GeckoSession.LOAD_FLAGS_REPLACE_HISTORY | GeckoSession.LOAD_FLAGS_BYPASS_CACHE));
        page.requestFocus();
    }
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }
    @Override protected void onNewIntent(android.content.Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (UrlPolicy.isWeb(intent.getDataString())) {
            url = intent.getDataString();
            if (blockerReady) session.loadUri(url);
        }
    }
    private void immersive() { getWindow().getDecorView().setSystemUiVisibility(5894); }
    private void message(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    private Button add(String text, Runnable action) {
        Button b = new Button(this); b.setText(text); b.setTextSize(15); b.setAllCaps(false); b.setTextColor(0xffedf0e5);
        b.setMinimumWidth(0); b.setMinWidth(0); b.setPadding(dp(12), 0, dp(12), 0);
        b.setBackground(background(false)); b.setOnFocusChangeListener((v, focused) -> v.setBackground(background(focused)));
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, dp(46)); lp.setMargins(0, 0, dp(6), dp(6)); tools.addView(b, lp); return b;
    }
    private GradientDrawable background(boolean focused) {
        GradientDrawable d = new GradientDrawable(); d.setColor(focused ? 0xff303d26 : 0xff1b2326);
        d.setCornerRadius(dp(4)); d.setStroke(dp(focused ? 3 : 1), focused ? 0xffc7ef79 : 0xff35423f); return d;
    }
    private void updateStatus() {
        if (blockerReady) blockButton.setText(engine.enabled() ? "uBOL: ON" + (blockedCount.isEmpty() ? "" : " · " + blockedCount) : "uBOL: OFF");
        scrollButton.setText(scrollMode ? "Scroll: ON" : "Scroll: OFF");
        if (!currentWebPage) {
            status.setText(blockerReady ? "Browser settings · Menu: controls" : "Preparing browser protection…");
            return;
        }
        String host = android.net.Uri.parse(url).getHost();
        status.setText((url.startsWith("http:") ? "HTTP · Unencrypted · " : "") + host
                + " · D-pad: " + (scrollMode ? "scroll" : "pointer + edge scroll") + " · ◀◀ / ▶▶: page scroll");
    }
    private void toggleScroll() { scrollMode = !scrollMode; updateStatus(); page.requestFocus(); }
    private void toggleBlocker() {
        if (!blockerReady) return;
        blockButton.setEnabled(false);
        engine.apply(!engine.enabled()).accept(extension -> {
            if (isFinishing() || isDestroyed()) return;
            if (engine.enabled()) { prepareFiltering(extension); return; }
            blockButton.setEnabled(true); updateStatus();
            // Gecko may otherwise reuse scripts allowed before the rules changed.
            if (currentWebPage) session.reload(GeckoSession.LOAD_FLAGS_BYPASS_CACHE);
            else session.load(new GeckoSession.Loader().uri(url).flags(GeckoSession.LOAD_FLAGS_BYPASS_CACHE));
            page.requestFocus();
        }, error -> {
            if (isFinishing() || isDestroyed()) return;
            blockButton.setEnabled(true); updateStatus();
            message("Could not finish changing protection. Reopen the page to retry.");
        });
    }
    private void address() {
        EditText field = new EditText(this); field.setSingleLine(true); field.setInputType(17); field.setText(url);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("Open address").setView(field)
                .setNegativeButton("Cancel", null).setPositiveButton("Open", null).create();
        d.setOnShowListener(v -> d.getButton(-1).setOnClickListener(w -> {
            try { String target = UrlPolicy.normalize(field.getText().toString()); d.dismiss(); if (blockerReady) session.loadUri(target); else url = target; page.requestFocus(); }
            catch (IllegalArgumentException error) { field.setError(error.getMessage()); }
        })); d.show();
    }
    private void save() {
        if (!currentWebPage) { message("Open a website before saving."); return; }
        EditText field = new EditText(this); field.setSingleLine(true); field.setText(title);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("Save site title").setView(field)
                .setNegativeButton("Cancel", null).setPositiveButton("Save", null).create();
        d.setOnShowListener(v -> d.getButton(-1).setOnClickListener(w -> {
            try (SiteStore store = new SiteStore(this)) {
                store.save(-1, field.getText().toString(), url); d.dismiss(); message("Site saved.");
            } catch (IllegalArgumentException error) { field.setError(error.getMessage()); }
        })); d.show();
    }
    private void controls() {
        if (engine == null) { message("Preparing browser…"); return; }
        new AlertDialog.Builder(this).setTitle("Browser controls").setItems(new String[]{"Return to library", "Open address", "Save site",
                scrollMode ? "Move pointer with D-pad" : "Scroll with D-pad", "Turn uBlock Origin Lite " + (engine.enabled() ? "off" : "on"),
                "uBlock Origin Lite settings & filter lists", "Reload page", "Stop loading", "Exit fullscreen", "Engine licenses"}, (d, index) -> {
            if (index == 0) finish();
            if (index == 1) address();
            if (index == 2) save();
            if (index == 3) toggleScroll();
            if (index == 4) toggleBlocker();
            if (index == 5 && blockerReady) {
                if (engine.enabled()) session.loadUri(engine.blocker.metaData.optionsPageUrl);
                else message("Turn protection on to open its settings.");
            }
            if (index == 6 && blockerReady) session.reload();
            if (index == 7 && blockerReady) session.stop();
            if (index == 8 && blockerReady) session.exitFullScreen();
            if (index == 9 && blockerReady) session.loadUri("about:license");
        }).show();
    }
    private void scroll(int dx, int dy) {
        session.getPanZoomController().scrollBy(ScreenLength.fromPixels(dx), ScreenLength.fromPixels(dy), PanZoomController.SCROLL_BEHAVIOR_AUTO);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (activeMedia != null && (key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE
                || key == KeyEvent.KEYCODE_MEDIA_PLAY || key == KeyEvent.KEYCODE_MEDIA_PAUSE)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (key == KeyEvent.KEYCODE_MEDIA_PAUSE || key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && playing) activeMedia.pause();
                else activeMedia.play();
            }
            return true;
        }
        if (key == KeyEvent.KEYCODE_MENU) { if (event.getAction() == 0 && event.getRepeatCount() == 0) controls(); return true; }
        if (!fullscreen && blockerReady && (key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || key == KeyEvent.KEYCODE_MEDIA_REWIND)) {
            if (event.getAction() == 0) scroll(0, (key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ? 1 : -1) * area.getHeight() * 2 / 3);
            return true;
        }
        if (!fullscreen && blockerReady && page.hasFocus() && key >= 19 && key <= 23) {
            if (event.getAction() == 0) {
                int step = dp(24); int dx = key == 21 ? -step : key == 22 ? step : 0; int dy = key == 19 ? -step : key == 20 ? step : 0;
                if (key == 23 && event.getRepeatCount() == 0) {
                    long now = SystemClock.uptimeMillis();
                    MotionEvent down = MotionEvent.obtain(now, now, 0, x, y, 0), up = MotionEvent.obtain(now, now + 30, 1, x, y, 0);
                    page.dispatchTouchEvent(down); page.dispatchTouchEvent(up); down.recycle(); up.recycle();
                } else if (scrollMode) scroll(dx * 4, dy * 4);
                else {
                    int edge = dp(32);
                    if (dy > 0 && y + dy > area.getHeight() - edge || dy < 0 && y + dy < edge) scroll(0, dy * 3);
                    else y = Math.max(edge, Math.min(area.getHeight() - edge, y + dy));
                    x = Math.max(0, Math.min(area.getWidth() - 1, x + dx)); pointer.invalidate();
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() { if (!blockerReady) finish(); else if (fullscreen) session.exitFullScreen(); else if (back) session.goBack(); else finish(); }
    @Override protected void onPause() { if (activeMedia != null) activeMedia.pause(); if (session != null && session.isOpen()) { session.setActive(false); engine.runtime.getWebExtensionController().setTabActive(session, false); } super.onPause(); }
    @Override protected void onResume() { super.onResume(); if (session != null && session.isOpen()) { session.setActive(true); engine.runtime.getWebExtensionController().setTabActive(session, true); } }
    @Override protected void onSaveInstanceState(Bundle state) { state.putString("url", url); super.onSaveInstanceState(state); }
    @Override protected void onDestroy() { if (page != null) page.removeCallbacks(protectionTimeout); if (session != null && session.isOpen()) { page.releaseSession(); session.close(); } super.onDestroy(); }
    private final class Pointer extends View {
        private final Paint paint = new Paint(3);
        Pointer() { super(ProtectedBrowserActivity.this); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) {
            paint.setColor(0x99000000); paint.setStyle(Paint.Style.FILL); canvas.drawCircle(x, y, dp(10), paint);
            paint.setColor(0xffc7ef79); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); canvas.drawCircle(x, y, dp(8), paint);
            canvas.drawLine(x - dp(13), y, x + dp(13), y, paint); canvas.drawLine(x, y - dp(13), x, y + dp(13), paint);
        }
    }
}
