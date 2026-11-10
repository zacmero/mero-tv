package dev.mero.tv;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;

/** Real-device transactional library checks in an isolated database context. */
public final class BrowserChecks extends Instrumentation {
    @Override public void onCreate(Bundle args) { super.onCreate(args); start(); }
    @Override public void onStart() {
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
    private static void require(boolean value) { if (!value) throw new AssertionError("Library invariant failed"); }
}
