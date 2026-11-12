package dev.mero.tv;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;

/** Real-device transactional library checks in an isolated database context. */
public final class BrowserChecks extends Instrumentation {
    private String importData;
    private boolean checkProtection;
    private Activity protectionActivity;
    @Override public void onCreate(Bundle args) { super.onCreate(args); importData = args == null ? null : args.getString("sitesBase64"); checkProtection = args != null && "true".equals(args.getString("checkProtection")); start(); }
    @Override public void onStart() {
        if (importData != null) { importSites(); return; }
        if (checkProtection) {
            try {
                // Extension startup needs a foreground Gecko window, as it does during browsing.
                protectionActivity = startActivitySync(new android.content.Intent(getTargetContext(), ProtectedBrowserActivity.class)
                        .setData(android.net.Uri.parse("http://127.0.0.1:8765/blocking.html"))
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
                verifyProtection();
            } catch (Throwable error) { protectionResult(false, "test activity: " + error.getClass().getSimpleName()); }
            return;
        }
        Bundle result = new Bundle();
        Context context = getTargetContext();
        String database = "library-checks.db"; // Separate test database; never the owner's library.
        context.deleteDatabase(database);
        try {
            SiteStore store = new SiteStore(context, database);
            require(store.list().isEmpty());
            store.save(-1, "First", "https://example.org/first");
            store.save(-1, "Second", "https://example.org/second");
            long first = store.list().get(0).id(), second = store.list().get(1).id();
            store.move(second, -1);
            require(store.list().get(0).id() == second);
            store.save(first, "Edited", "http://localhost:8080/test");
            store.close();
            store = new SiteStore(context, database);
            require(store.list().size() == 2);
            require(store.list().get(1).title().equals("Edited"));
            require(store.list().get(1).url().equals("http://localhost:8080/test"));
            store.delete(second);
            require(store.list().size() == 1 && store.list().get(0).id() == first);
            store.move(first, -1);
            require(store.list().size() == 1);
            store.close();
            result.putString("stream", "PASS: empty library, create, reorder, edit, reopen persistence, delete, boundary move\n");
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            result.putString("stream", "FAIL: " + error.getClass().getSimpleName() + ": " + error.getMessage() + "\n");
            finish(Activity.RESULT_CANCELED, result);
        } finally { context.deleteDatabase(database); }
    }
    private void protectionResult(boolean success, String text) {
        if (protectionActivity != null) protectionActivity.runOnUiThread(protectionActivity::finish);
        Bundle result = new Bundle(); result.putString("stream", (success ? "PASS: " : "FAIL: ") + text + "\n");
        finish(success ? Activity.RESULT_OK : Activity.RESULT_CANCELED, result);
    }
    private void verifyProtection() {
        runOnMainSync(() -> {
            try {
                ProtectedEngine engine = ProtectedEngine.get(getTargetContext());
                boolean original = engine.enabled();
                engine.prepare().then(extension -> {
                    require(ProtectedEngine.BLOCKER_ID.equals(extension.id));
                    require(ProtectedEngine.BLOCKER_VERSION.equals(extension.metaData.version));
                    return engine.apply(false);
                }).then(extension -> {
                    require(!extension.metaData.enabled && !engine.enabled());
                    return engine.apply(true);
                }).then(extension -> {
                    require(extension.metaData.enabled && engine.enabled());
                    require(extension.metaData.optionsPageUrl != null && !extension.metaData.optionsPageUrl.isEmpty());
                    return engine.apply(original);
                }).accept(extension -> protectionResult(true, "signed uBlock Origin Lite installed; startup and disable/enable verified; preference restored"),
                        error -> protectionResult(false, "extension check: " + error.getClass().getSimpleName() + " " + error.getMessage()));
            } catch (Throwable error) { protectionResult(false, "engine startup: " + error.getClass().getSimpleName()); }
        });
    }
    /** Explicit shell-invoked development import. No page/native bridge or bundled destinations. */
    private void importSites() {
        Bundle result = new Bundle();
        try (SiteStore store = new SiteStore(getTargetContext())) {
            org.json.JSONArray sites = new org.json.JSONArray(new String(android.util.Base64.decode(importData, android.util.Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8));
            android.database.sqlite.SQLiteDatabase db = store.getWritableDatabase();
            int added = 0;
            db.beginTransaction();
            try {
                for (int i = 0; i < sites.length(); i++) {
                    org.json.JSONObject entry = sites.getJSONObject(i);
                    String url = UrlPolicy.normalize(entry.getString("url"));
                    boolean exists = false;
                    for (SiteStore.Site site : store.list()) if (site.url().equals(url)) exists = true;
                    if (!exists) { store.save(-1, entry.getString("title"), url); added++; }
                }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            result.putString("stream", "PASS: requested sites imported; added=" + added + "; total=" + store.list().size() + "\n");
            finish(Activity.RESULT_OK, result);
        } catch (Exception error) {
            result.putString("stream", "FAIL: import rolled back: " + error.getClass().getSimpleName() + "\n");
            finish(Activity.RESULT_CANCELED, result);
        }
    }
    private static void require(boolean value) { if (!value) throw new AssertionError("Library invariant failed"); }
}
