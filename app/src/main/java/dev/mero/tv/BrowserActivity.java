package dev.mero.tv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Native chrome and local storage; visited pages receive no privileged bridge. */
public final class BrowserActivity extends Activity {
    private static final int BACKGROUND = 0xff101417, SURFACE = 0xff1b2326, ACCENT = 0xffc7ef79;
    private static final int INK = 0xffedf0e5, MUTED = 0xffa8b7b2, VIOLET = 0xffb4a4e9;
    private SiteStore store;
    private FrameLayout root, webArea;
    private LinearLayout browserPanel, browserTools;
    private WebView web;
    private WebView pendingPopup;
    private CursorView pointer;
    private TextView pageLabel;
    private View fullscreen;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private boolean library = true, cursorMode = true, scrollMode;
    private float cursorX = 160, cursorY = 140;
    private int speed = 1;
    private long selectedSite = -1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        store = new SiteStore(this);
        root = new FrameLayout(this);
        root.setBackgroundColor(BACKGROUND);
        setContentView(root);
        immersive();
        showLibrary(-1);
        if (state != null && UrlPolicy.isWeb(state.getString("page"))) open(state.getString("page"));
        if (UrlPolicy.isWeb(getIntent().getDataString())) open(getIntent().getDataString());
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent); setIntent(intent);
        if (UrlPolicy.isWeb(intent.getDataString())) open(intent.getDataString());
    }

    private int dp(float value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void immersive() {
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }
    private LinearLayout column() {
        LinearLayout view = new LinearLayout(this);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }
    private TextView label(String text, int size, int color) {
        TextView view = new TextView(this);
        view.setText(text); view.setTextSize(size); view.setTextColor(color);
        return view;
    }
    private GradientDrawable background(boolean focused) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(focused ? 0xff303d26 : SURFACE);
        shape.setCornerRadius(dp(4));
        shape.setStroke(dp(focused ? 3 : 1), focused ? ACCENT : 0xff35423f);
        return shape;
    }
    private Button button(String text, Runnable action) {
        Button b = new Button(this);
        b.setText(text); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(INK);
        b.setContentDescription(text.equals("←") ? "Go back" : text.equals("→") ? "Go forward" : text);
        b.setPadding(dp(14), dp(10), dp(14), dp(10));
        b.setMinHeight(dp(48)); b.setMinimumHeight(dp(48));
        b.setMinWidth(0); b.setMinimumWidth(0); b.setBackground(background(false));
        b.setOnFocusChangeListener((v, focused) -> v.setBackground(background(focused)));
        b.setOnClickListener(v -> action.run());
        return b;
    }
    private void attachButton(LinearLayout row, Button b) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(48));
        params.setMargins(0, 0, dp(8), dp(8)); row.addView(b, params);
    }
    private void message(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }

    private void showLibrary(long focusId) {
        if (fullscreen != null) exitFullscreen();
        library = true;
        if (web != null) {
            web.evaluateJavascript("document.querySelectorAll('video,audio').forEach(function(m){m.pause()})", null);
            web.onPause();
        }
        root.removeAllViews();
        root.addView(new LibraryBackdrop(), new FrameLayout.LayoutParams(-1, -1));
        ScrollView scroll = new ScrollView(this);
        LinearLayout content = column(); content.setPadding(dp(38), dp(30), dp(38), dp(30));
        scroll.addView(content); root.addView(scroll);
        ImageView wordmark = new ImageView(this); wordmark.setImageResource(dev.mero.tv.R.drawable.wordmark);
        wordmark.setContentDescription("Mero TV"); wordmark.setScaleType(ImageView.ScaleType.FIT_START);
        content.addView(wordmark, new LinearLayout.LayoutParams(dp(248), dp(52)));
        TextView title = label("The hidden stream gem", 32, INK);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL));
        title.setPadding(0, dp(25), 0, dp(8)); content.addView(title);
        TextView hint = label("Choose a site · Hold Select or press Menu to make it yours", 16, MUTED);
        hint.setPadding(0, 0, 0, dp(22)); content.addView(hint);
        LinearLayout actions = new LinearLayout(this);
        Button add = button("+ Add site", () -> edit(null, "", ""));
        attachButton(actions, add);
        attachButton(actions, button("Open address", () -> addressDialog("")));
        attachButton(actions, button("Browser mode", this::browserMode));
        if (web != null && UrlPolicy.isWeb(web.getUrl())) attachButton(actions, button("Resume page", this::showBrowser));
        attachButton(actions, button("About", () -> new AlertDialog.Builder(this).setTitle("Mero TV 0.2")
                .setMessage("Native TV library with a protected Firefox browser and uBlock Origin Lite 2026.1110.1816. The lightweight system browser remains available without extensions. GeckoView is MPL-2.0; uBlock Origin is GPL-3.0-only. Source and licenses are in the project notices. No root is required.")
                .setPositiveButton("Close", null).show()));
        content.addView(actions);
        GridLayout grid = new GridLayout(this); grid.setColumnCount(3);
        content.addView(grid);
        java.util.List<SiteStore.Site> sites = store.list();
        Button focus = add;
        for (SiteStore.Site site : sites) {
            Button card = button(site.title() + "\n\n" + addressSummary(site.url()), () -> open(site.url()));
            card.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            card.setMaxLines(4);
            card.setOnFocusChangeListener((v, focused) -> {
                v.setBackground(background(focused)); if (focused) selectedSite = site.id();
            });
            card.setOnLongClickListener(v -> { siteActions(site); return true; });
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; params.height = dp(138);
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.setMargins(0, 0, dp(12), dp(12)); grid.addView(card, params);
            if (site.id() == focusId) focus = card;
        }
        if (sites.isEmpty()) {
            TextView eyebrow = label("A CLEAN SLATE", 12, VIOLET);
            eyebrow.setLetterSpacing(0.18f); eyebrow.setPadding(0, dp(48), 0, dp(8)); content.addView(eyebrow);
            TextView empty = label("Start with somewhere you love.", 23, INK);
            content.addView(empty);
            TextView guidance = label("Add a title and address. Your sites will be right here.", 16, MUTED);
            guidance.setPadding(0, dp(8), 0, dp(30)); content.addView(guidance);
        }
        focus.requestFocus();
    }
    private String addressSummary(String url) {
        Uri uri = Uri.parse(url);
        return ("http".equals(uri.getScheme()) ? "HTTP · " : "") + uri.getHost();
    }
    private EditText field(String hint, String value, boolean url) {
        EditText field = new EditText(this); field.setHint(hint); field.setText(value);
        field.setTextSize(18); field.setSingleLine(true);
        field.setInputType(InputType.TYPE_CLASS_TEXT | (url ? InputType.TYPE_TEXT_VARIATION_URI : InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        return field;
    }
    private void edit(SiteStore.Site site, String initialTitle, String initialUrl) {
        LinearLayout form = column(); form.setPadding(dp(24), dp(8), dp(24), dp(8));
        EditText title = field("Title", site == null ? initialTitle : site.title(), false);
        EditText url = field("https://…", site == null ? initialUrl : site.url(), true);
        form.addView(title); form.addView(url);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(site == null ? "Add site" : "Edit site")
                .setView(form).setPositiveButton("Save", null).setNegativeButton("Cancel", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try {
                store.save(site == null ? -1 : site.id(), title.getText().toString(), url.getText().toString());
                dialog.dismiss();
                if (library) showLibrary(site == null ? -1 : site.id()); else message("Site saved to your library.");
            } catch (IllegalArgumentException e) { url.setError(e.getMessage()); url.requestFocus(); }
            catch (android.database.SQLException e) { message("Could not save the site. Free some storage and try again."); }
        }));
        dialog.show();
    }
    private void siteActions(SiteStore.Site site) {
        new AlertDialog.Builder(this).setTitle(site.title())
                .setItems(new String[]{"Open", "Edit", "Move earlier", "Move later", "Delete"}, (d, which) -> {
                    if (which == 0) open(site.url());
                    if (which == 1) edit(site, "", "");
                    if (which == 2 || which == 3) { store.move(site.id(), which == 2 ? -1 : 1); showLibrary(site.id()); }
                    if (which == 4) new AlertDialog.Builder(this).setTitle("Delete this site?")
                            .setMessage(site.title()).setNegativeButton("Cancel", null).setPositiveButton("Delete", (a, b) -> {
                                store.delete(site.id()); showLibrary(-1);
                            }).show();
                }).show();
    }
    private void addressDialog(String value) {
        EditText field = field("Website address", value, true);
        LinearLayout form = column(); form.setPadding(dp(24), dp(8), dp(24), dp(8)); form.addView(field);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Open a website")
                .setView(form).setPositiveButton("Open", null).setNegativeButton("Cancel", null).create();
        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            try { String url = UrlPolicy.normalize(field.getText().toString()); dialog.dismiss(); open(url); }
            catch (IllegalArgumentException e) { field.setError(e.getMessage()); }
        }));
        dialog.show();
    }

    private void createBrowser() {
        browserPanel = column();
        browserPanel.setBackgroundColor(BACKGROUND);
        LinearLayout tools = new LinearLayout(this); tools.setPadding(dp(12), dp(8), 0, 0);
        browserTools = tools;
        attachButton(tools, button("Library", () -> showLibrary(-1)));
        attachButton(tools, button("Address", () -> addressDialog(web.getUrl())));
        attachButton(tools, button("←", () -> { if (web.canGoBack()) web.goBack(); }));
        attachButton(tools, button("→", () -> { if (web.canGoForward()) web.goForward(); }));
        attachButton(tools, button("Reload", () -> web.reload()));
        attachButton(tools, button("Save", this::saveCurrent));
        attachButton(tools, button("Scroll", () -> { scrollMode = !scrollMode; cursorMode = true; pointer.setVisibility(View.VISIBLE); web.requestFocus(); message(scrollMode ? "D-pad now scrolls. Press Scroll to move the pointer again." : "D-pad moves the pointer; screen edges scroll."); }));
        attachButton(tools, button("Controls", this::browserActions));
        browserPanel.addView(tools);
        pageLabel = label("", 13, MUTED); pageLabel.setSingleLine(true);
        pageLabel.setPadding(dp(18), 0, dp(18), dp(6)); browserPanel.addView(pageLabel);
        webArea = new FrameLayout(this);
        browserPanel.addView(webArea, new LinearLayout.LayoutParams(-1, 0, 1));
        web = new WebView(this);
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false); settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportMultipleWindows(true); settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setBuiltInZoomControls(true); settings.setDisplayZoomControls(false);
        settings.setUseWideViewPort(true); settings.setLoadWithOverviewMode(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setBackgroundColor(Color.WHITE);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (UrlPolicy.isWeb(url)) return false;
                if (request.isForMainFrame() && request.hasGesture()) external(url);
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                pageLabel.setText(("http".equals(Uri.parse(url).getScheme()) ? "HTTP · Unencrypted · " : "")
                        + Uri.parse(url).getHost() + "  ·  Menu: controls");
                CookieManager.getInstance().flush();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) message("This page could not load. Use Menu to retry or return to the library.");
            }
            @Override public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                if (fullscreen != null) exitFullscreen();
                webArea.removeAllViews(); view.destroy(); web = null; browserPanel = null;
                showLibrary(-1); message("The page stopped responding. Your library is safe."); return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreen != null) { callback.onCustomViewHidden(); return; }
                fullscreen = view; fullscreenCallback = callback;
                // Keep the WebView at the fullscreen origin for Amazon's video compositor.
                browserTools.setVisibility(View.GONE); pageLabel.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(-1, -1)); view.requestFocus();
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); immersive();
            }
            @Override public void onHideCustomView() { exitFullscreen(); }
            @Override public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, android.os.Message result) {
                if (!userGesture || pendingPopup != null) return false;
                pendingPopup = new WebView(BrowserActivity.this);
                pendingPopup.getSettings().setAllowFileAccess(false);
                pendingPopup.getSettings().setAllowContentAccess(false);
                pendingPopup.setWebViewClient(new WebViewClient() {
                    @Override public boolean shouldOverrideUrlLoading(WebView popup, WebResourceRequest request) {
                        if (request.isForMainFrame()) {
                            String url = request.getUrl().toString(); discardPopup(popup);
                            if (UrlPolicy.isWeb(url)) new AlertDialog.Builder(BrowserActivity.this)
                                    .setTitle("Open requested window?").setMessage(addressSummary(url))
                                    .setNegativeButton("Cancel", null).setPositiveButton("Open", (a, b) -> open(url)).show();
                        }
                        return true;
                    }
                });
                ((WebView.WebViewTransport) result.obj).setWebView(pendingPopup); result.sendToTarget();
                WebView popup = pendingPopup; root.postDelayed(() -> discardPopup(popup), 15000);
                return true;
            }
        });
        webArea.addView(web, new FrameLayout.LayoutParams(-1, -1));
        pointer = new CursorView(); webArea.addView(pointer, new FrameLayout.LayoutParams(-1, -1));
        web.setFocusable(true); web.setFocusableInTouchMode(true);
    }
    private void discardPopup(WebView popup) {
        if (pendingPopup == popup) { pendingPopup = null; popup.destroy(); }
    }
    private void open(String value) {
        try {
            String url = UrlPolicy.normalize(value);
            if (getSharedPreferences("browser", MODE_PRIVATE).getBoolean("extensions", true)) {
                startActivity(new Intent(this, ProtectedBrowserActivity.class).setData(Uri.parse(url)));
                return;
            }
            if (web == null) createBrowser();
            showBrowser(); web.loadUrl(url);
        } catch (IllegalArgumentException e) { message(e.getMessage()); }
    }
    private void showBrowser() {
        if (fullscreen != null) exitFullscreen();
        library = false;
        root.removeAllViews(); root.addView(browserPanel, new FrameLayout.LayoutParams(-1, -1));
        web.onResume(); web.requestFocus(); pointer.setVisibility(cursorMode ? View.VISIBLE : View.GONE);
    }
    private void browserMode() {
        int selected = getSharedPreferences("browser", MODE_PRIVATE).getBoolean("extensions", true) ? 0 : 1;
        new AlertDialog.Builder(this).setTitle("Browser mode")
                .setSingleChoiceItems(new String[]{"Protected browser · uBlock Origin Lite", "Lightweight browser · no extensions"}, selected, (d, which) -> {
                    getSharedPreferences("browser", MODE_PRIVATE).edit().putBoolean("extensions", which == 0).apply();
                    d.dismiss(); message(which == 0 ? "Protected browser selected." : "Lightweight browser selected. uBlock Origin is unavailable in this mode.");
                }).setNegativeButton("Cancel", null).show();
    }
    private void saveCurrent() {
        if (web != null && UrlPolicy.isWeb(web.getUrl())) edit(null, web.getTitle() == null ? "" : web.getTitle(), web.getUrl());
        else message("Open a website before saving it.");
    }
    private void external(String url) {
        String scheme = Uri.parse(url).getScheme();
        if (!"mailto".equalsIgnoreCase(scheme) && !"tel".equalsIgnoreCase(scheme)) {
            message("Only web addresses can open here."); return;
        }
        new AlertDialog.Builder(this).setTitle("Open another app?").setMessage("This page requested a " + scheme + " link.")
                .setNegativeButton("Cancel", null).setPositiveButton("Open", (a, b) -> {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
                    catch (android.content.ActivityNotFoundException e) { message("No app can open this link."); }
                }).show();
    }
    private void exitFullscreen() {
        if (fullscreen == null) return;
        root.removeView(fullscreen); fullscreen = null;
        browserTools.setVisibility(View.VISIBLE); pageLabel.setVisibility(View.VISIBLE);
        WebChromeClient.CustomViewCallback callback = fullscreenCallback; fullscreenCallback = null;
        if (callback != null) callback.onCustomViewHidden();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        web.requestFocus(); immersive();
    }
    private void browserActions() {
        String[] actions = {"Return to library", "Open address", "Save this page", "Reload", "Stop loading",
                cursorMode ? "Use page focus instead of cursor" : "Enable remote cursor",
                scrollMode ? "Move cursor with D-pad" : "Scroll page with D-pad",
                "Cursor speed: " + new String[]{"slow", "normal", "fast"}[speed], "Exit fullscreen"};
        new AlertDialog.Builder(this).setTitle("Browser controls").setItems(actions, (d, which) -> {
            if (which == 0) { showLibrary(-1); return; }
            if (which == 1) { addressDialog(web.getUrl()); return; }
            if (which == 2) { saveCurrent(); return; }
            if (which == 3) web.reload();
            if (which == 4) web.stopLoading();
            if (which == 5) { cursorMode = !cursorMode; pointer.setVisibility(cursorMode ? View.VISIBLE : View.GONE); }
            if (which == 6) { scrollMode = !scrollMode; cursorMode = true; pointer.setVisibility(View.VISIBLE); }
            if (which == 7) speed = (speed + 1) % 3;
            if (which == 8) exitFullscreen();
            web.requestFocus();
        }).show();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_MENU) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                if (!library) browserActions();
                else {
                    for (SiteStore.Site site : store.list()) if (site.id() == selectedSite) { siteActions(site); break; }
                }
            }
            return true;
        }
        if (!library && fullscreen == null && (key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD || key == KeyEvent.KEYCODE_MEDIA_REWIND)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) web.scrollBy(0, (key == KeyEvent.KEYCODE_MEDIA_FAST_FORWARD ? 1 : -1) * web.getHeight() * 2 / 3);
            return true;
        }
        if (!library && fullscreen == null && cursorMode && web.hasFocus()
                && (key >= KeyEvent.KEYCODE_DPAD_UP && key <= KeyEvent.KEYCODE_DPAD_CENTER)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                int step = dp(new int[]{10, 22, 44}[speed]);
                int x = key == KeyEvent.KEYCODE_DPAD_LEFT ? -step : key == KeyEvent.KEYCODE_DPAD_RIGHT ? step : 0;
                int y = key == KeyEvent.KEYCODE_DPAD_UP ? -step : key == KeyEvent.KEYCODE_DPAD_DOWN ? step : 0;
                if (key == KeyEvent.KEYCODE_DPAD_CENTER && event.getRepeatCount() == 0) {
                    long now = SystemClock.uptimeMillis();
                    MotionEvent down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, cursorX, cursorY, 0);
                    MotionEvent up = MotionEvent.obtain(now, now + 30, MotionEvent.ACTION_UP, cursorX, cursorY, 0);
                    web.dispatchTouchEvent(down); web.dispatchTouchEvent(up); down.recycle(); up.recycle();
                } else if (scrollMode) web.scrollBy(x * 4, y * 4);
                else {
                    cursorX = Math.max(0, Math.min(web.getWidth() - 1, cursorX + x));
                    int edge = dp(32);
                    if (y > 0 && cursorY + y > web.getHeight() - edge || y < 0 && cursorY + y < edge) web.scrollBy(0, y * 3);
                    else cursorY = Math.max(edge, Math.min(web.getHeight() - edge, cursorY + y));
                    pointer.invalidate();
                }
            }
            return true;
        }
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed() {
        if (fullscreen != null) exitFullscreen();
        else if (!library && web.canGoBack()) web.goBack();
        else if (!library) showLibrary(-1);
        else super.onBackPressed();
    }
    @Override protected void onPause() {
        if (web != null) {
            web.evaluateJavascript("document.querySelectorAll('video,audio').forEach(function(m){m.pause()})", null);
            web.onPause();
            CookieManager.getInstance().flush();
        }
        super.onPause();
    }
    @Override protected void onResume() { super.onResume(); if (library) showLibrary(selectedSite); else if (web != null) web.onResume(); immersive(); }
    @Override protected void onSaveInstanceState(Bundle state) {
        if (!library && web != null && UrlPolicy.isWeb(web.getUrl())) state.putString("page", web.getUrl());
        super.onSaveInstanceState(state);
    }
    @Override protected void onDestroy() {
        if (pendingPopup != null) discardPopup(pendingPopup);
        if (web != null) { webArea.removeView(web); web.destroy(); }
        store.close(); super.onDestroy();
    }
    private final class CursorView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        CursorView() { super(BrowserActivity.this); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setStyle(Paint.Style.FILL); paint.setColor(0x99000000); canvas.drawCircle(cursorX, cursorY, dp(10), paint);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(2)); paint.setColor(ACCENT);
            canvas.drawCircle(cursorX, cursorY, dp(8), paint);
            canvas.drawLine(cursorX - dp(13), cursorY, cursorX + dp(13), cursorY, paint);
            canvas.drawLine(cursorX, cursorY - dp(13), cursorX, cursorY + dp(13), paint);
        }
    }

    /** Static vector-like ornament: no animation loop or page rendering overhead. */
    private final class LibraryBackdrop extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        LibraryBackdrop() { super(BrowserActivity.this); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float right = getWidth() - dp(38), top = dp(40);
            paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(dp(1)); paint.setColor(0xff3b4434);
            canvas.drawLine(right - dp(115), top, right, top, paint);
            canvas.drawLine(right, top, right, top + dp(26), paint);
            paint.setColor(VIOLET); canvas.drawLine(right - dp(22), top + dp(7), right, top + dp(7), paint);
            paint.setColor(0xff29332b);
            float bottom = getHeight() - dp(38);
            canvas.drawLine(dp(38), bottom, dp(86), bottom, paint);
            canvas.drawLine(dp(38), bottom - dp(12), dp(38), bottom, paint);
        }
    }
}
