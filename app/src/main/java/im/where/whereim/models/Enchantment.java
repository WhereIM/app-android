package im.where.whereim.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

/**
 * Created by buganini on 31/01/17.
 */

public class Enchantment extends BaseModel {
    public static final String TABLE_NAME = "enchantment";

    private final static String COL_ID = "_id";
    private final static String COL_CHANNEL_ID = "channel_id";
    private final static String COL_NAME = "name";
    private final static String COL_LATITUDE = "latitude";
    private final static String COL_LONGITUDE = "longitude";
    private final static String COL_RADIUS = "radius";
    private final static String COL_PUBLIC = "public";
    private final static String COL_ENABLED = "enabled";

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_CHANNEL_ID + " TEXT, " +
                COL_NAME + " TEXT, " +
                COL_LATITUDE + " DOUBLE PRECISION, " +
                COL_LONGITUDE + " DOUBLE PRECISION, " +
                COL_RADIUS + " INTEGER, " +
                COL_PUBLIC + " BOOLEAN, " +
                COL_ENABLED + " BOOLEAN)";
        db.execSQL(sql);

        sql = "CREATE INDEX enchantment_index ON "+TABLE_NAME+" ("+COL_CHANNEL_ID+")";
        db.execSQL(sql);
    }


    public static class List {
        public ArrayList<Enchantment> public_list;
        public ArrayList<Enchantment> private_list;

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
    public int radius;
    public boolean isPublic;
    public Boolean enabled;
    public boolean deleted = false;

    public static Enchantment parse(Cursor cursor){
        Enchantment enchantment = new Enchantment();
        enchantment.id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
        enchantment.channel_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL_ID));
        enchantment.name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
        enchantment.latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE));
        enchantment.longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE));
        enchantment.radius = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RADIUS));
        enchantment.isPublic = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PUBLIC))!=0;
        enchantment.enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLED))!=0;
        return enchantment;
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
        cv.put(COL_NAME, name);
        cv.put(COL_LATITUDE, latitude);
        cv.put(COL_LONGITUDE, longitude);
        cv.put(COL_RADIUS, radius);
        cv.put(COL_PUBLIC, isPublic?1:0);
        if (enabled != null) {
            cv.put(COL_ENABLED, enabled?1:0);
        }
        return cv;
    }

    public void delete(SQLiteDatabase db){
        db.execSQL("DELETE FROM "+TABLE_NAME+" WHERE "+COL_ID+"=?", new String[]{id});
    }

    public static Cursor getCursor(SQLiteDatabase db){
        return db.rawQuery("SELECT * FROM "+TABLE_NAME, new String[]{});
    }
}

