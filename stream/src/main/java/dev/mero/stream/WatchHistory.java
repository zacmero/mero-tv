package dev.mero.stream;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;

/** Bounded local history; no stream URLs or credentials are persisted. */
final class WatchHistory {
    final SharedPreferences prefs;
    WatchHistory(Context context){prefs=context.getSharedPreferences("watch-history",0);}
    JSONArray entries(){try{return new JSONArray(prefs.getString("entries","[]"));}catch(JSONException e){return new JSONArray();}}
    void save(String episode,String slug,String query,String mode,String label,long position,boolean completed){
        try{
            JSONObject entry=new JSONObject().put("episode",episode).put("slug",slug).put("query",query).put("mode",mode).put("label",label).put("position",completed?0:position).put("completed",completed);
            JSONArray next=new JSONArray().put(entry),old=entries();
            for(int i=0;i<old.length()&&next.length()<20;i++){
                JSONObject item=old.optJSONObject(i);if(item!=null&&!(episode.equals(item.optString("episode"))&&mode.equals(item.optString("mode"))))next.put(item);
            }
            prefs.edit().putString("entries",next.toString()).apply();
        }catch(JSONException e){throw new IllegalStateException("Invalid history entry",e);}
    }
}
