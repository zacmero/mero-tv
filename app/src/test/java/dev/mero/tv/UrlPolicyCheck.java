package dev.mero.tv;

/** Run with javac/java; no test framework required. */
public final class UrlPolicyCheck {
    public static void main(String[] args) {
        require(UrlPolicy.normalize(" example.org/path ").equals("https://example.org/path"));
        require(UrlPolicy.normalize("http://localhost:8080/test?q=1").equals("http://localhost:8080/test?q=1"));
        require(UrlPolicy.normalize("localhost:8080/test").equals("https://localhost:8080/test"));
        for (String url : new String[]{"", "javascript:alert(1)", "file:///data/local/tmp/a", "intent://x", "data:text/html,x", "https://user:pass@example.org", "https://", "https://example.org:99999", "https://bad host"}) {
            try { UrlPolicy.normalize(url); throw new AssertionError("Accepted forbidden address"); }
            catch (IllegalArgumentException expected) { /* rejected */ }
        }
        require(!UrlPolicy.isWeb("example.org"));
        require(!UrlPolicy.isWeb("about:blank"));
        require(UrlPolicy.isWeb("https://example.org"));
        System.out.println("URL boundary checks passed");
    }
    private static void require(boolean value) { if (!value) throw new AssertionError(); }
}
