package dev.mero.stream;
import android.net.Uri;
import android.text.Html;
import org.json.*;
import java.net.*;
import java.io.*;
import java.util.regex.*;
/** HiAnime extraction adapted from GPL-3.0 ani-cli. Never executes page JavaScript. */
final class Provider {
 static final String BASE="https://hianime.at";
 static final class Stream { final String url,referer;final JSONArray subtitles;Stream(String u,String r,JSONArray s){url=u;referer=r;subtitles=s;} }
    static String get(String url) throws Exception {
        URL u=new URL(url);if(!u.getProtocol().equals("https"))throw new IOException("HTTPS required");
        HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestProperty("User-Agent","Mozilla/5.0");
        try {if(c.getResponseCode()!=200)throw new IOException("Provider unavailable");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>4000000)throw new IOException("Response too large");out.write(b,0,n);}return out.toString("UTF-8");}}finally{c.disconnect();}
    }
    static String clean(String s){return Html.fromHtml(s,Html.FROM_HTML_MODE_LEGACY).toString();}
 static Stream resolve(String id,String mode)throws Exception {
            String raw=get(BASE+"/api/theme/episode/servers?episodeId="+id);String html=new JSONObject(raw).optString("html");
            Matcher m=Pattern.compile("data-type=\""+mode+"\"[^>]*data-server-name=\"ZokoAnime\"[^>]*data-hash=\"([^\"]+)\"").matcher(html);if(!m.find())throw new IOException("No supported server");
            String embed=new String(android.util.Base64.decode(m.group(1),0),"UTF-8");
            Matcher blob=Pattern.compile("window\\.__P=\"([^\"]+)\"").matcher(get(embed));if(!blob.find())throw new IOException("No stream config");
            byte[] bytes=android.util.Base64.decode(blob.group(1),0),key="otaku-embed-v1".getBytes("UTF-8");for(int i=0;i<bytes.length;i++)bytes[i]^=key[i%key.length];
            JSONObject config=new JSONObject(new String(bytes,"UTF-8"));String stream=config.getString("src");Uri uri=Uri.parse(stream);if(!"https".equals(uri.getScheme()))throw new IOException("Invalid stream");
            URI e=new URI(embed);String referer=e.getScheme()+"://"+e.getAuthority()+"/";
 return new Stream(stream,referer,config.optJSONArray("subtitles"));
 }
}
