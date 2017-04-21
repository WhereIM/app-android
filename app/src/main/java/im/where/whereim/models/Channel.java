package im.where.whereim.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by buganini on 31/01/17.
 */

public class Channel extends BaseModel {
    public static final String TABLE_NAME = "channel";

    private final static String COL_ID = "_id";
    private final static String COL_CHANNEL_NAME = "channel_name";
    private final static String COL_USER_CHANNEL_NAME = "user_channel_name";
    private final static String COL_MATE = "mate";
    private final static String COL_ACTIVE = "active";
    private final static String COL_ENABLED = "enabled";
    private final static String COL_ENABLE_RADIUS = "enable_radius";
    private final static String COL_RADIUS = "radius";

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_CHANNEL_NAME + " TEXT, " +
                COL_USER_CHANNEL_NAME + " TEXT, " +
                COL_MATE + " TEXT, " +
                COL_ACTIVE + " BOOLEAN, " +
                COL_ENABLED + " BOOLEAN, " +
                COL_ENABLE_RADIUS + " BOOLEAN, " +
                COL_RADIUS + " INTEGER" +
                ")";
        db.execSQL(sql);
    }

    public String id;
    public String channel_name;
    public String user_channel_name;
    public String mate_id;
    public Boolean active;
    public Boolean enabled;
    public Boolean enable_radius;
    public int radius;
    public boolean deleted = false;

    public String getSortValue(){
        if(this.user_channel_name!=null && !this.user_channel_name.isEmpty()){
            return this.user_channel_name;
        }
        if(this.channel_name!=null && !this.channel_name.isEmpty()){
            return this.channel_name;
        }
        return "";
    }

    public static Channel parse(Cursor cursor){
        Channel channel = new Channel();
        channel.id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
        channel.channel_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL_NAME));
        channel.user_channel_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CHANNEL_NAME));
        channel.mate_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_MATE));
        channel.active = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ACTIVE))!=0;
        channel.enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLED))!=0;
        channel.enable_radius = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLE_RADIUS))!=0;
        channel.radius = cursor.getInt(cursor.getColumnIndexOrThrow(COL_RADIUS));
        return channel;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public ContentValues buildContentValues(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        cv.put(COL_ID, id);
        cv.put(COL_CHANNEL_NAME, channel_name);
        cv.put(COL_USER_CHANNEL_NAME, user_channel_name);
        cv.put(COL_MATE, mate_id);
        cv.put(COL_ACTIVE, active ?1:0);
        cv.put(COL_ENABLED, enabled ?1:0);
        cv.put(COL_ENABLE_RADIUS, enable_radius ?1:0);
        cv.put(COL_RADIUS, radius);
        return cv;
    }

    public void delete(SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COL_ID + "=?", new String[]{id});
    }

    public static Cursor getCursor(SQLiteDatabase db){
        return db.rawQuery("SELECT * FROM "+TABLE_NAME, new String[]{});
    }
}

