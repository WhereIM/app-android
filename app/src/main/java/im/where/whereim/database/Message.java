package im.where.whereim.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import im.where.whereim.Models;

/**
 * Created by buganini on 17/01/17.
 */

public class Message extends ORM {
    public static final String TABLE_NAME = "message";

    private final static String COL_ID = "_id";
    private final static String COL_CHANNEL = "channel";
    private final static String COL_MATE = "mate";
    private final static String COL_MESSAGE = "message";
    private final static String COL_TIME = "time";

    public static String CREATE_TABLE = "CREATE TABLE " + TABLE_NAME + " (" +
            COL_ID + " INTEGER PRIMARY KEY, " +
            COL_CHANNEL + " TEXT, " +
            COL_MATE + " TEXT, " +
            COL_MESSAGE + " TEXT, " +
            COL_TIME + " INTEGER)";

    public long id;
    public String channel_id;
    public String mate_id;
    public String message;
    public long time;

    public static Message parse(JSONObject json){
        try {
            Message m = new Message();
            m.id = json.getLong("id");
            m.channel_id = json.getString("channel_id");
            m.mate_id = json.getString("mate_id");
            m.message = json.getString("message");
            m.time = json.getLong("time");
            return m;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Message parse(Cursor cursor){
        Message m = new Message();
        m.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        m.channel_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL));
        m.mate_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_MATE));
        m.message = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE));
        m.time = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME));
        return m;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public ContentValues buildInsert() {
        ContentValues cv = new ContentValues();
        cv.put(COL_ID, id);
        cv.put(COL_CHANNEL, channel_id);
        cv.put(COL_MATE, mate_id);
        cv.put(COL_MESSAGE, message);
        cv.put(COL_TIME, time);
        return cv;
    }

    public static Cursor getCursor(SQLiteDatabase db, Models.Channel channel){
        return db.rawQuery("SELECT * FROM "+TABLE_NAME+" WHERE "+COL_CHANNEL+"=? ORDER BY time ASC", new String[]{channel.id});
    }
}

