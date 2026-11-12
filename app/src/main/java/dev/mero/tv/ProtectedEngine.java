package dev.mero.tv;

import android.content.Context;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.mozilla.geckoview.GeckoResult;
import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoRuntimeSettings;
import org.mozilla.geckoview.WebExtension;
import org.mozilla.geckoview.WebExtensionController;

/** One Firefox runtime and the pinned, signed uBlock Lite package. No native messaging. */
final class ProtectedEngine {
    static final String BLOCKER_ID = "uBOLiteRedux@raymondhill.net";
    static final String BLOCKER_VERSION = "2026.1110.1816";
    private static ProtectedEngine instance;
    final GeckoRuntime runtime;
    WebExtension blocker;
    boolean filteringReady;
    private final Context context;
    private GeckoResult<WebExtension> ready;

    private ProtectedEngine(Context context) {
        this.context = context.getApplicationContext();
        // Bound process and heap budgets for the confirmed 1 GB ARMv7 unit.
        File limits = new File(this.context.getFilesDir(), "engine-limits.yaml");
        try (OutputStream output = this.context.openFileOutput(limits.getName(), Context.MODE_PRIVATE)) {
            output.write(("prefs:\n"
                    + "  dom.ipc.processCount: 1\n"
                    + "  dom.ipc.processPrelaunch.enabled: false\n"
                    // Avoid the GPU-process surface allocator's 100 ms IPC deadline
                    // on this unit. Hardware rendering remains in the parent process.
                    + "  layers.gpu-process.enabled: false\n"
                    + "  javascript.options.mem.max: 128\n"
                    + "  javascript.options.mem.nursery.max_kb: 4096\n").getBytes(StandardCharsets.UTF_8));
        } catch (IOException error) {
            throw new IllegalStateException("Could not configure the browser memory budget", error);
        }
        boolean debug = (context.getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;
        boolean diagnostics = debug && context.getSharedPreferences("development", Context.MODE_PRIVATE)
                .getBoolean("engineDiagnostics", false);
        runtime = GeckoRuntime.create(this.context, new GeckoRuntimeSettings.Builder()
                .configFilePath(limits.getAbsolutePath()).fissionEnabled(false)
                .extensionsProcessEnabled(true)
                .remoteDebuggingEnabled(diagnostics).consoleOutput(diagnostics).build());
        android.util.Log.i("MeroProtection", "Runtime created");
        runtime.getWebExtensionController().setAddonManagerDelegate(new WebExtensionController.AddonManagerDelegate() {
            @Override public void onReady(WebExtension extension) {
                if (!BLOCKER_ID.equals(extension.id) || !BLOCKER_VERSION.equals(extension.metaData.version)) return;
                blocker = extension;
                filteringReady = false;
            }
            @Override public void onDisabled(WebExtension extension) {
                if (BLOCKER_ID.equals(extension.id)) {
                    filteringReady = false;
                }
            }
        });
        runtime.getWebExtensionController().setPromptDelegate(new WebExtensionController.PromptDelegate() {
            @Override public GeckoResult<WebExtension.PermissionPromptResponse> onInstallPromptRequest(
                    WebExtension extension, String[] permissions, String[] origins, String[] dataPermissions) {
                boolean approved = BLOCKER_ID.equals(extension.id) && BLOCKER_VERSION.equals(extension.metaData.version);
                return GeckoResult.fromValue(new WebExtension.PermissionPromptResponse(approved, false, false));
            }
        });
    }
    static ProtectedEngine get(Context context) {
        if (instance == null) instance = new ProtectedEngine(context);
        return instance;
    }
    boolean enabled() { return context.getSharedPreferences("browser", Context.MODE_PRIVATE).getBoolean("blocker", true); }
    GeckoResult<WebExtension> prepare() {
        if (ready != null) return ready;
        android.util.Log.i("MeroProtection", "Listing extensions");
        ready = runtime.getWebExtensionController().list().then(extensions -> {
            android.util.Log.i("MeroProtection", "Extensions listed: " + extensions.size());
            for (WebExtension extension : extensions) {
                if ("uBlock0@raymondhill.net".equals(extension.id) && extension.metaData.enabled) {
                    return runtime.getWebExtensionController().disable(extension, WebExtensionController.EnableSource.USER)
                            .then(ignored -> installOrFind(extensions));
                }
            }
            return installOrFind(extensions);
        });
        GeckoResult<WebExtension> pending = ready;
        pending.accept(extension -> {}, error -> ready = null);
        return pending;
    }
    private GeckoResult<WebExtension> installOrFind(java.util.List<WebExtension> extensions) {
        for (WebExtension extension : extensions) {
            if (BLOCKER_ID.equals(extension.id) && BLOCKER_VERSION.equals(extension.metaData.version)) {
                blocker = extension;
                return apply(enabled());
            }
        }
        return runtime.getWebExtensionController().install("resource://android/assets/extensions/ublock.xpi")
                .then(extension -> { blocker = extension; return apply(enabled()); });
    }
    GeckoResult<WebExtension> apply(boolean enabled) {
        android.util.Log.i("MeroProtection", "Apply blocker enabled=" + enabled);
        android.content.SharedPreferences preferences = context.getSharedPreferences("browser", Context.MODE_PRIVATE);
        WebExtensionController controller = runtime.getWebExtensionController();
        GeckoResult<WebExtension> result = enabled
                ? controller.enable(blocker, WebExtensionController.EnableSource.USER)
                : controller.disable(blocker, WebExtensionController.EnableSource.USER);
        // Enable completion makes the extension page available. The page's
        // initialized response, rather than a separate add-on event, gates sites.
        return result.then(extension -> {
            android.util.Log.i("MeroProtection", "Applied blocker active=" + extension.metaData.enabled);
            blocker = extension;
            preferences.edit().putBoolean("blocker", extension.metaData.enabled).commit();
            return GeckoResult.fromValue(extension);
        });
    }
}
