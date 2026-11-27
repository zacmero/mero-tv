package dev.mero.stream;
import android.text.Html;
import org.json.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.*;
/** HiAnime extraction adapted from GPL-3.0 ani-cli. Never executes page JavaScript. */
final class Provider {
    static final String BASE = "https://hianime.at";
    private static final int TIMEOUT = 10000;
    private static final Pattern EPISODE = Pattern.compile("data-number=\"([^\"]*)\"[^>]*data-id=\"([0-9]+)\"");
    private static final Pattern PLAYER_CONFIG = Pattern.compile("window\\.__P=\"([^\"]+)\"");

    record Episode(String id, String label) {}
    record Opening(long startMs, long endMs) {}
    record Stream(String url, String referer, JSONArray subtitles, String mode, Opening opening) {}

    static final class Unavailable extends IOException {
        Unavailable(String message) { super(message); }
    }

    static String get(String url, int timeout) throws IOException {
        URL address = httpsUrl(url);
        HttpURLConnection connection = (HttpURLConnection) address.openConnection();
        connection.setConnectTimeout(timeout);
        connection.setReadTimeout(timeout);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw new Unavailable("Provider returned HTTP " + status + ".");
            try (InputStream in = connection.getInputStream(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                for (int count; (count = in.read(buffer)) != -1; ) {
                    if (out.size() + count > 4_000_000) throw new Unavailable("Provider response was too large.");
                    out.write(buffer, 0, count);
                }
                return out.toString("UTF-8");
            }
        } finally {
            connection.disconnect();
        }
    }

    private static URL httpsUrl(String url) throws IOException {
        URL address = new URL(url);
        if (!"https".equals(address.getProtocol()) || address.getHost().isEmpty() || address.getUserInfo() != null) {
            throw new Unavailable("The provider returned an invalid video address.");
        }
        return address;
    }

    static List<SearchResult> search(String query) throws IOException {
        return searchResults(get(BASE + "/search?keyword=" + URLEncoder.encode(query, "UTF-8"), TIMEOUT));
    }

    static List<Episode> episodes(String series) throws IOException, JSONException {
        String id = series.substring(series.lastIndexOf('-') + 1);
        if (!id.matches("[0-9]+")) throw new Unavailable("Series no longer listed.");
        String json = get(BASE + "/api/theme/episode/list/" + id, TIMEOUT);
        Matcher matcher = EPISODE.matcher(new JSONObject(json).getString("html"));
        List<Episode> episodes = new ArrayList<>();
        while (matcher.find()) episodes.add(new Episode(matcher.group(2), matcher.group(1)));
        return episodes;
    }
    static String clean(String s){return Html.fromHtml(s,Html.FROM_HTML_MODE_LEGACY).toString();}
 static final class SearchResult {final String id,title,poster;SearchResult(String id,String title,String poster){this.id=id;this.title=title;this.poster=poster;}}
 static java.util.List<SearchResult> searchResults(String html){
  java.util.List<SearchResult> results=new java.util.ArrayList<>();
  html=html.split("id=\"main-sidebar\"")[0];
  Pattern title=Pattern.compile("<h3 class=\"film-name\">\\s*<a href=\"[^\"]*/([^\"/]+)\"\\s*title=\"([^\"]*)\"",Pattern.DOTALL);
  Pattern image=Pattern.compile("<img[^>]*?\\bsrc=\"([^\"]+)\"",Pattern.DOTALL);
  for(String card:html.split("(?=<div class=\"flw-item)")){Matcher name=title.matcher(card);if(!name.find())continue;Matcher poster=image.matcher(card);String url=poster.find()?clean(poster.group(1)):"";results.add(new SearchResult(name.group(1),clean(name.group(2)),url));if(results.size()>=50)break;}
  return results;
 }
    static Stream resolve(String id, String preferredMode) throws IOException, JSONException {
        if (!id.matches("[0-9]+")) throw new Unavailable("Episode no longer listed.");
        String raw = get(BASE + "/api/theme/episode/servers?episodeId=" + id, TIMEOUT);
        String html = new JSONObject(raw).getString("html");
        String preferred = "dub".equals(preferredMode) ? "dub" : "sub";
        String alternative = "sub".equals(preferred) ? "dub" : "sub";
        for (String mode : new String[]{preferred, alternative}) {
            Matcher server = Pattern.compile("data-type=\"" + mode
                + "\"[^>]*data-server-name=\"ZokoAnime\"[^>]*data-hash=\"([^\"]+)\"").matcher(html);
            if (!server.find()) continue;
            String embed = new String(android.util.Base64.decode(server.group(1), 0), StandardCharsets.UTF_8);
            URL address = httpsUrl(embed);
            Matcher blob = PLAYER_CONFIG.matcher(get(embed, TIMEOUT));
            if (!blob.find()) throw new Unavailable("The server did not return a video.");
            byte[] bytes = android.util.Base64.decode(blob.group(1), 0);
            byte[] key = "otaku-embed-v1".getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < bytes.length; i++) bytes[i] ^= key[i % key.length];
            JSONObject config = new JSONObject(new String(bytes, StandardCharsets.UTF_8));
            String stream = config.getString("src");
            httpsUrl(stream);
            String referer = address.getProtocol() + "://" + address.getAuthority() + "/";
            return new Stream(stream, referer, config.optJSONArray("subtitles"), mode, opening(config));
        }
        throw new Unavailable("No supported video source for this episode.");
    }

    private static Opening opening(JSONObject config) {
        JSONObject skip = config.optJSONObject("skip");
        JSONObject intro = skip == null ? null : skip.optJSONObject("intro");
        if (intro == null) return null;
        double start = intro.optDouble("start");
        double end = intro.optDouble("end");
        if (!Double.isFinite(start) || !Double.isFinite(end) || start < 0 || end <= start
            || end >= Long.MAX_VALUE / 1000.0) return null;
        long startMs = Math.round(start * 1000);
        long endMs = Math.round(end * 1000);
        return endMs > startMs ? new Opening(startMs, endMs) : null;
    }
}
