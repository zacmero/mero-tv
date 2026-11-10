package dev.mero.tv;

import java.net.URI;
import java.util.Locale;

/** Addresses remain data. Only explicit HTTP(S) navigation is allowed. */
public final class UrlPolicy {
    private UrlPolicy() {}

    public static String normalize(String input) {
        String value = input.trim();
        if (value.isEmpty()) throw new IllegalArgumentException("Enter a website address.");
        // A bare hostname may have a port. Explicit non-web schemes remain forbidden.
        if (!value.matches("(?i)^[a-z][a-z0-9+.-]*:.*")) value = "https://" + value;
        else if (value.matches("^[^/\\s:]+:[0-9]+(?:/.*)?$")) value = "https://" + value;
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            if ((!scheme.equals("https") && !scheme.equals("http")) || uri.getHost() == null
                    || uri.getRawUserInfo() != null || uri.getPort() > 65535 || uri.getPort() == 0) {
                throw new IllegalArgumentException("Use an HTTP or HTTPS address without embedded credentials.");
            }
            return uri.toASCIIString();
        } catch (java.net.URISyntaxException e) {
            throw new IllegalArgumentException("Check the address; spaces and malformed URLs are not supported.");
        }
    }

    public static boolean isWeb(String url) {
        try { normalizeExplicit(url); return true; }
        catch (IllegalArgumentException e) { return false; }
    }

    private static void normalizeExplicit(String url) {
        if (url == null || !url.matches("(?i)^https?://.*")) throw new IllegalArgumentException();
        normalize(url);
    }
}
