package dev.mero.stream;
import android.app.Activity;
import android.graphics.*;
import android.util.LruCache;
import android.widget.ImageView;
import java.net.*;
import java.io.*;
import java.util.concurrent.*;
/** One focused poster, bounded download/decode/cache; failures never block text navigation. */
final class PosterPreview {
 final Activity activity;final ImageView image;
 final ThreadPoolExecutor worker=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(1),new ThreadPoolExecutor.DiscardOldestPolicy());
 final LruCache<String,Bitmap> cache=new LruCache<String,Bitmap>(4*1024*1024){protected int sizeOf(String key,Bitmap value){return value.getAllocationByteCount();}};
 int generation;boolean closed;
 PosterPreview(Activity a,ImageView image){activity=a;this.image=image;}
 void show(String url){show(url,null);}
 void show(String url,String detail){int request=++generation;image.setImageDrawable(null);if(url==null||!url.startsWith("https://"))return;
  String cacheKey=detail==null?url:detail;
  Bitmap existing=cache.get(cacheKey);if(existing!=null){image.setImageBitmap(existing);return;}
  worker.execute(()->{try{String source=url;
   if(detail!=null)try{String html=Provider.get(detail,3000);java.util.regex.Matcher metadata=java.util.regex.Pattern.compile("<meta[^>]*property=\"og:image\"[^>]*content=\"([^\"]+)\"",java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);if(metadata.find()&&metadata.group(1).startsWith("https://"))source=Provider.clean(metadata.group(1));}catch(Exception ignored){/* Search artwork remains a fallback. */}
   Bitmap bitmap=load(source);if(bitmap==null&&!source.equals(url))bitmap=load(url);final Bitmap decoded=bitmap;activity.runOnUiThread(()->{if(closed)return;if(decoded!=null)cache.put(cacheKey,decoded);if(request==generation)image.setImageBitmap(decoded);});}catch(Exception ignored){/* Keep the menu usable when poster delivery fails. */}});
 }
 static Bitmap load(String url)throws Exception{
  URL target=new URL(url);if(!target.getProtocol().equals("https"))throw new IOException("HTTPS required");
  HttpURLConnection c=(HttpURLConnection)target.openConnection();c.setConnectTimeout(3000);c.setReadTimeout(3000);
  try{if(c.getResponseCode()!=200)return null;ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(InputStream in=c.getInputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>2*1024*1024)throw new IOException("Poster too large");bytes.write(buffer,0,n);}}
   byte[] data=bytes.toByteArray();BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,options);
   if(options.outWidth<=0||options.outHeight<=0||options.outWidth>8192||options.outHeight>8192)return null;
   options.inSampleSize=1;while(options.outWidth/options.inSampleSize>768||options.outHeight/options.inSampleSize>1152)options.inSampleSize*=2;
   options.inJustDecodeBounds=false;options.inPreferredConfig=Bitmap.Config.ARGB_8888;return BitmapFactory.decodeByteArray(data,0,data.length,options);
  }finally{c.disconnect();}
 }
 void close(){closed=true;generation++;worker.shutdownNow();cache.evictAll();image.setImageDrawable(null);}
}
