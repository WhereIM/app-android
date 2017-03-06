package im.where.whereim.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import im.where.whereim.Key;

/**
 * Created by buganini on 31/01/17.
 */

public class Mate extends BaseModel {
    public static final String TABLE_NAME = "mate";

    private final static String COL_ID = "_id";
    private final static String COL_CHANNEL_ID = "channel_id";
    private final static String COL_MATE_NAME = "mate_name";
    private final static String COL_USER_MATE_NAME = "user_mate_name";

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_CHANNEL_ID + " TEXT, " +
                COL_MATE_NAME + " TEXT, " +
                COL_USER_MATE_NAME + " TEXT)";
        db.execSQL(sql);

        sql = "CREATE INDEX mate_index ON "+TABLE_NAME+" ("+COL_CHANNEL_ID+")";
        db.execSQL(sql);
    }

    public String id;
    public String channel_id;
    public String mate_name;
    public String user_mate_name;

    public Double latitude;
    public Double longitude;
    public Double accuracy;
    public Double altitude; //m
    public Double bearing;
    public Double speed; //m
    public Long time;

    public String getDisplayName(){
        if(this.user_mate_name!=null){
            return this.user_mate_name;
        }
        return this.mate_name;
    }
    public static JSONObject parseToJson(Cursor cursor){
        try {
            JSONObject j = new JSONObject();
            j.put(Key.ID, cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)));
            j.put(Key.CHANNEL, cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL_ID)));
            j.put(Key.MATE_NAME, cursor.getString(cursor.getColumnIndexOrThrow(COL_MATE_NAME)));
            j.put(Key.USER_MATE_NAME, cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_MATE_NAME)));
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
    public ContentValues buildContentValues(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put(COL_ID, id);
        cv.put(COL_CHANNEL_ID, channel_id);
        cv.put(COL_MATE_NAME, mate_name);
        cv.put(COL_USER_MATE_NAME, user_mate_name);
        return cv;
    }

    public static Cursor getCursor(SQLiteDatabase db){
        return db.rawQuery("SELECT * FROM "+TABLE_NAME, new String[]{});
    }
}

