package im.where.whereim.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.SQLInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import im.where.whereim.Key;
import im.where.whereim.R;
import im.where.whereim.Util;
import im.where.whereim.views.WimSpan;

/**
 * Created by buganini on 17/01/17.
 */

public class PendingMessage extends BaseModel {
    public static final String TABLE_NAME = "pending_message";

    private final static String COL_ID = "_id";
    private final static String COL_HASH = "hash";
    private final static String COL_CHANNEL = "channel";
    private final static String COL_TYPE = "type";
    private final static String COL_PAYLOAD = "payload";

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY, " +
                COL_HASH + " TEXT, " +
                COL_CHANNEL + " TEXT, " +
                COL_TYPE + " TEXT, " +
                COL_PAYLOAD + " TEXT)";
        db.execSQL(sql);

        sql = "CREATE INDEX pending_message_index ON "+TABLE_NAME+" ("+COL_HASH+")";
        db.execSQL(sql);
    }


    public static void upgradeTable(SQLiteDatabase db, int currVersion){
        if (currVersion < 5) {
            PendingMessage.createTable(db);

            currVersion = 5;
        }
    }

    public long id = -1;
    public String hash = null;
    public String channel_id;
    public String type;
    public JSONObject payload = new JSONObject();

    public static PendingMessage parse(Cursor cursor){
        PendingMessage m = new PendingMessage();
        m.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        m.hash = cursor.getString(cursor.getColumnIndexOrThrow(COL_HASH));
        m.channel_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL));
        m.type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE));
        try {
            m.payload = new JSONObject(cursor.getString(cursor.getColumnIndexOrThrow(COL_PAYLOAD)));
        } catch (JSONException e) {
            m.payload = new JSONObject();
            e.printStackTrace();
        }
        return m;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public ContentValues buildContentValues(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        if(id != -1){
            cv.put(COL_ID, id);
        }
        if(hash == null){
            hash = UUID.randomUUID().toString().replace("-", "")+System.currentTimeMillis();
        }
        cv.put(COL_HASH, hash);
        cv.put(COL_CHANNEL, channel_id);
        cv.put(COL_TYPE, type);
        cv.put(COL_PAYLOAD, payload.toString());
        return cv;
    }

    public static PendingMessage pop(SQLiteDatabase db){
        Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_NAME+" ORDER BY "+COL_ID+" ASC LIMIT 1", new String[]{});
        if(cursor.moveToFirst()){
            return PendingMessage.parse(cursor);
        }
        return null;
    }

    public static void delete(SQLiteDatabase db, String hash){
        db.execSQL("DELETE FROM "+TABLE_NAME+" WHERE "+COL_HASH+"=?", new String[]{hash});
    }

    public static List<PendingMessage> getAll(SQLiteDatabase db, String channel_id){
        ArrayList<PendingMessage> ret = new ArrayList<>();
        Cursor cursor = db.rawQuery("SELECT * FROM "+TABLE_NAME+" WHERE "+COL_CHANNEL+"=? ORDER BY "+COL_ID+" ASC", new String[]{channel_id});
        if(cursor.moveToFirst()){
            do{
                PendingMessage m = PendingMessage.parse(cursor);
                if(m != null){
                    ret.add(m);
                }
            }while(cursor.moveToNext());
        }
        return ret;
    }
}

