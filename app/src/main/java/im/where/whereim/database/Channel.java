package im.where.whereim.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import im.where.whereim.Models;

/**
 * Created by buganini on 31/01/17.
 */

public class Channel extends BaseModel {
    public static final String TABLE_NAME = "channel";

    private final static String COL_ID = "_id";
    private final static String COL_CHANNEL_NAME = "channel_name";
    private final static String COL_USER_CHANNEL_NAME = "user_channel_name";
    private final static String COL_MATE = "mate";
    private final static String COL_ENABLE = "enable";

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_CHANNEL_NAME + " TEXT, " +
                COL_USER_CHANNEL_NAME + " TEXT, " +
                COL_MATE + " TEXT, " +
                COL_ENABLE + " INTEGER)";
        db.execSQL(sql);
        //todo create index
    }

    public String id;
    public String channel_name;
    public String user_channel_name;
    public String mate_id;
    public Boolean enable;

    public static JSONObject parseToJson(Cursor cursor){
        try {
            JSONObject j = new JSONObject();
            j.put(Models.KEY_CHANNEL, cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)));
            j.put("channel_name", cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL_NAME)));
            j.put("user_channel_name", cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CHANNEL_NAME)));
            j.put(Models.KEY_MATE, cursor.getString(cursor.getColumnIndexOrThrow(COL_MATE)));
            j.put("enable", cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLE))!=0);
            return j;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        cv.put(COL_ID, id);
        cv.put(COL_CHANNEL_NAME, channel_name);
        cv.put(COL_USER_CHANNEL_NAME, user_channel_name);
        cv.put(COL_MATE, mate_id);
        cv.put(COL_ENABLE, enable?1:0);
        return cv;
    }

    public static Cursor getCursor(SQLiteDatabase db){
        return db.rawQuery("SELECT * FROM "+TABLE_NAME+" ORDER BY COALESCE("+COL_USER_CHANNEL_NAME+","+COL_CHANNEL_NAME+")", new String[]{});
    }
}

