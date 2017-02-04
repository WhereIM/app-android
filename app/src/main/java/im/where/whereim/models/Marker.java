package im.where.whereim.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import im.where.whereim.Models;

/**
 * Created by buganini on 31/01/17.
 */

public class Marker extends BaseModel {
    public static final String TABLE_NAME = "marker";

    private final static String COL_ID = "_id";
    private final static String COL_CHANNEL_ID = "channel_id";
    private final static String COL_NAME = "name";
    private final static String COL_LATITUDE = "latitude";
    private final static String COL_LONGITUDE = "longitude";
    private final static String COL_PUBLIC = "public";
    private final static String COL_ENABLE = "enable";

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_CHANNEL_ID + " TEXT, " +
                COL_NAME + " TEXT, " +
                COL_LATITUDE + " DOUBLE PRECISION, " +
                COL_LONGITUDE + " DOUBLE PRECISION, " +
                COL_PUBLIC + " INTEGER, " +
                COL_ENABLE + " INTEGER)";
        db.execSQL(sql);
        //todo create index
    }


    public static class List {
        public ArrayList<Marker> public_list;
        public ArrayList<Marker> private_list;

        public List() {
            public_list = new ArrayList<>();
            private_list = new ArrayList<>();
        }
    }

    public String id;
    public String channel_id;
    public String name;
    public double latitude;
    public double longitude;
    public boolean isPublic;
    public Boolean enable;

    public static JSONObject parseToJson(Cursor cursor){
        try {
            JSONObject j = new JSONObject();
            j.put(Models.KEY_ID, cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)));
            j.put(Models.KEY_CHANNEL, cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL_ID)));
            j.put(Models.KEY_NAME, cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)));
            j.put(Models.KEY_LATITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE)));
            j.put(Models.KEY_LONGITUDE, cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE)));
            j.put(Models.KEY_PUBLIC, cursor.getInt(cursor.getColumnIndexOrThrow(COL_PUBLIC))!=0);
            j.put(Models.KEY_ENABLE, cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLE))!=0);
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
        cv.put(COL_CHANNEL_ID, channel_id);
        cv.put(COL_NAME, name);
        cv.put(COL_LATITUDE, latitude);
        cv.put(COL_LONGITUDE, longitude);
        cv.put(COL_PUBLIC, isPublic?1:0);
        cv.put(COL_ENABLE, enable?1:0);
        return cv;
    }

    public static Cursor getCursor(SQLiteDatabase db){
        return db.rawQuery("SELECT * FROM "+TABLE_NAME, new String[]{});
    }
}

