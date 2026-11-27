package dev.mero.stream;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/** Recent-first list of distinct submitted search terms, stored only on the device. */
final class SearchHistory {
    final SharedPreferences prefs;

    SearchHistory(Context context) {
        prefs = context.getSharedPreferences("search-history", Context.MODE_PRIVATE);
    }

    void seed(WatchHistory watched, String resumeQuery) {
        if (prefs.contains("terms")) return;
        LinkedHashMap<String, String> terms = new LinkedHashMap<>();
        addTerm(terms, resumeQuery);
        JSONArray previous = watched.entries();
        for (int i = 0; i < previous.length(); i++) {
            if (previous.optJSONObject(i) != null) {
                addTerm(terms, previous.optJSONObject(i).optString("query"));
            }
        }
        save(new ArrayList<>(terms.values()));
    }

    List<String> terms() {
        List<String> terms = new ArrayList<>();
        try {
            JSONArray saved = new JSONArray(prefs.getString("terms", "[]"));
            for (int i = 0; i < saved.length(); i++) terms.add(saved.getString(i));
        } catch (JSONException ignored) {
            // A malformed entry must not prevent new searches.
        }
        return terms;
    }

    void record(String query) {
        List<String> terms = terms();
        terms.removeIf(term -> term.equalsIgnoreCase(query));
        terms.add(0, query);
        save(terms);
    }

    private static void addTerm(LinkedHashMap<String, String> terms, String query) {
        if (!query.trim().isEmpty()) terms.putIfAbsent(query.toLowerCase(Locale.ROOT), query);
    }

    private void save(List<String> terms) {
        prefs.edit().putString("terms", new JSONArray(terms).toString()).apply();
    }
}
