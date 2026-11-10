package dev.mero.tv;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

final class SiteStore extends SQLiteOpenHelper {
    record Site(long id, String title, String url, int position) {}

    SiteStore(Context context) { this(context, "library.db"); }
    SiteStore(Context context, String name) { super(context, name, null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE sites (_id INTEGER PRIMARY KEY, title TEXT NOT NULL, url TEXT NOT NULL, position INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("A library migration is required.");
    }
    List<Site> list() {
        List<Site> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery("SELECT _id,title,url,position FROM sites ORDER BY position,_id", null)) {
            while (c.moveToNext()) result.add(new Site(c.getLong(0), c.getString(1), c.getString(2), c.getInt(3)));
        }
        return result;
    }
    void save(long id, String title, String url) {
        String normalized = UrlPolicy.normalize(url);
        ContentValues values = new ContentValues();
        values.put("title", title.trim().isEmpty() ? android.net.Uri.parse(normalized).getHost() : title.trim());
        values.put("url", normalized);
        SQLiteDatabase db = getWritableDatabase();
        if (id < 0) {
            try (Cursor c = db.rawQuery("SELECT COALESCE(MAX(position),-1)+1 FROM sites", null)) {
                c.moveToFirst(); values.put("position", c.getInt(0));
            }
            db.insertOrThrow("sites", null, values);
        } else if (db.update("sites", values, "_id=?", new String[]{Long.toString(id)}) != 1) {
            throw new IllegalStateException("The saved site no longer exists.");
        }
    }
    void delete(long id) { getWritableDatabase().delete("sites", "_id=?", new String[]{Long.toString(id)}); }
    void move(long id, int direction) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            List<Site> sites = list();
            for (int i = 0; i < sites.size(); i++) {
                if (sites.get(i).id() != id) continue;
                int next = i + direction;
                if (next < 0 || next >= sites.size()) break;
                Site a = sites.get(i), b = sites.get(next);
                db.execSQL("UPDATE sites SET position=? WHERE _id=?", new Object[]{b.position(), a.id()});
                db.execSQL("UPDATE sites SET position=? WHERE _id=?", new Object[]{a.position(), b.id()});
                break;
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
}
