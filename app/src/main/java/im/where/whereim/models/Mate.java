package im.where.whereim.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by buganini on 31/01/17.
 */

public class Mate extends BaseModel {
    public static final String TABLE_NAME = "mate";

    private final static String COL_ID = "_id";
    private final static String COL_CHANNEL_ID = "channel_id";
    private final static String COL_MATE_NAME = "mate_name";
    private final static String COL_USER_MATE_NAME = "user_mate_name";
    private final static String COL_DELETED = "deleted";

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_CHANNEL_ID + " TEXT, " +
                COL_MATE_NAME + " TEXT, " +
                COL_USER_MATE_NAME + " TEXT, " +
                COL_DELETED + " BOOLEAN" +
                ")";
        db.execSQL(sql);

        sql = "CREATE INDEX mate_index ON "+TABLE_NAME+" ("+COL_CHANNEL_ID+")";
        db.execSQL(sql);
    }

    public static void upgradeTable(SQLiteDatabase db, int currVersion){
        String sql;
        if (currVersion < 2) {
            sql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_DELETED + " BOOLEAN NOT NULL DEFAULT 0";
            db.execSQL(sql);

            currVersion = 2;
        }
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
    public boolean deleted = false;

    public String getDisplayName(){
        if(this.user_mate_name!=null){
            return this.user_mate_name;
        }
        if(this.mate_name!=null){
            return this.mate_name;
        }
        return "";
    }

    public static Mate parse(Cursor cursor){
        Mate mate = new Mate();
        mate.id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
        mate.channel_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL_ID));
        mate.mate_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_MATE_NAME));
        mate.user_mate_name = cursor.getString(cursor.getColumnIndexOrThrow(COL_USER_MATE_NAME));
        mate.deleted = cursor.getInt(cursor.getColumnIndexOrThrow(COL_DELETED))!=0;
        return mate;
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
        cv.put(COL_DELETED, deleted);
        return cv;
    }

    public void delete(SQLiteDatabase db){
        db.rawQuery("DELETE FROM "+TABLE_NAME+" WHERE "+COL_ID+"=?", new String[]{id});
    }

    public static Cursor getCursor(SQLiteDatabase db){
        return db.rawQuery("SELECT * FROM "+TABLE_NAME, new String[]{});
    }
}

