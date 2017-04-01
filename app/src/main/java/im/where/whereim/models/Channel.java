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
    private final static String COL_ENABLE = "enable";
    private final static String COL_ENABLE_RADIUS = "enable_radius";
    private final static String COL_RADIUS = "radius";

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_CHANNEL_NAME + " TEXT, " +
                COL_USER_CHANNEL_NAME + " TEXT, " +
                COL_MATE + " TEXT, " +
                COL_ENABLE + " BOOLEAN, " +
                COL_ENABLE_RADIUS + " BOOLEAN, " +
                COL_RADIUS + " DOUBLE PRECISION" +
                ")";
        db.execSQL(sql);
    }

    public String id;
    public String channel_name;
    public String user_channel_name;
    public String mate_id;
    public Boolean enable;
    public Boolean enable_radius;
    public double radius;
    public boolean deleted = false;

    public static Channel parse(Cursor cursor){
        Channel channel = new Channel();
        channel.id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
        channel.channel_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL_NAME));
        channel.user_channel_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_CHANNEL_NAME));
        channel.mate_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_MATE));
        channel.enable = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLE))!=0;
        channel.enable_radius = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLE_RADIUS))!=0;
        channel.radius = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_RADIUS));
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
        cv.put(COL_ENABLE, enable?1:0);
        cv.put(COL_ENABLE_RADIUS, enable_radius ?1:0);
        cv.put(COL_RADIUS, radius);
        return cv;
    }

    public void delete(SQLiteDatabase db){
        db.rawQuery("DELETE FROM "+TABLE_NAME+" WHERE "+COL_ID+"=?", new String[]{id});
    }

    public static Cursor getCursor(SQLiteDatabase db){
        return db.rawQuery("SELECT * FROM "+TABLE_NAME+" ORDER BY COALESCE("+COL_USER_CHANNEL_NAME+","+COL_CHANNEL_NAME+")", new String[]{});
    }
}

