package im.where.whereim.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import im.where.whereim.CoreService;
import im.where.whereim.Models;
import im.where.whereim.R;

/**
 * Created by buganini on 17/01/17.
 */

public class Message extends ORM {
    public static final String TABLE_NAME = "message";

    private final static String COL_ID = "_id";
    private final static String COL_SN = "sn";
    private final static String COL_CHANNEL = "channel";
    private final static String COL_USER = "user";
    private final static String COL_MATE = "mate";
    private final static String COL_TYPE = "type";
    private final static String COL_MESSAGE = "message";
    private final static String COL_TIME = "time";

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY, " +
                COL_SN + " INTEGER, " +
                COL_CHANNEL + " TEXT, " +
                COL_USER + " INTEGER, " +
                COL_MATE + " TEXT, " +
                COL_TYPE + " TEXT, " +
                COL_MESSAGE + " TEXT, " +
                COL_TIME + " INTEGER)";
        db.execSQL(sql);
        //todo create index
    }

    public long id;
    public long sn;
    public String channel_id;
    public String mate_id;
    public String type;
    public String message;
    public long time;
    public boolean notify;

    public static Message parse(JSONObject json){
        try {
            Message m = new Message();
            m.id = json.getLong("id");
            m.sn = json.getLong("sn");
            m.channel_id = json.getString("channel");
            m.mate_id = json.getString("mate");
            m.type = json.getString("type");
            m.message = json.getString("message");
            m.time = json.getLong("time");
            if(json.has("notify") && json.getBoolean("notify")){
                m.notify = true;
            }else{
                m.notify = false;
            }
            return m;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Message parse(Cursor cursor){
        Message m = new Message();
        m.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        m.sn = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SN));
        m.channel_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL));
        m.mate_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_MATE));
        m.type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE));
        m.message = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE));
        m.time = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME));
        m.notify = false;
        return m;
    }

    public String getText(Context context, CoreService.CoreBinder binder){
        if("text".equals(this.type)){
            return this.message;
        }
        try {
            JSONObject json = new JSONObject(this.message);
            switch (this.type) {
                case "enchantment_create":
                    return context.getResources().getString(R.string.message_enchantment_create, json.optString("name", ""));

                case "enchantment_emerge":
                    return context.getResources().getString(R.string.message_enchantment_emerge, json.optString("name", ""));

                case "enchantment_in":
                    return context.getResources().getString(R.string.message_enchantment_in, json.optString("name", ""));

                case "enchantment_out":
                    return context.getResources().getString(R.string.message_enchantment_out, json.optString("name", ""));

                case "marker_create":
                    return context.getResources().getString(R.string.message_marker_create, json.optString("name", ""));
            }
        }catch(JSONException e){
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getTableName() {
        return TABLE_NAME;
    }

    @Override
    public ContentValues buildInsert() {
        ContentValues cv = new ContentValues();
        cv.put(COL_ID, id);
        cv.put(COL_SN, sn);
        cv.put(COL_CHANNEL, channel_id);
        cv.put(COL_MATE, mate_id);
        cv.put(COL_TYPE, type);
        cv.put(COL_MESSAGE, message);
        cv.put(COL_TIME, time);
        return cv;
    }

    public static class BundledCursor {
        public Cursor cursor;
        public int count;
        public long loadMoreBefore;
        public long loadMoreAfter;
        public long firstId;
        public long lastId;
        public boolean loadMoreChannelData;
        public boolean loadMoreUserData;
    }

    public static BundledCursor getCursor(SQLiteDatabase db, Models.Channel channel){
        BundledCursor bc = new BundledCursor();
        bc.loadMoreChannelData = false;
        bc.loadMoreUserData = false;
        boolean hasChannelData = false;
        boolean hasUserData = false;
        long channelDataSn = 0;
        long channelDataId = 0;
        long userDataSn = 0;
        long userDataId = 0;

        bc.cursor = db.rawQuery("SELECT "+COL_ID+","+COL_SN+","+COL_USER+","+COL_CHANNEL+","+COL_MATE+","+COL_TYPE+","+COL_MESSAGE+","+COL_TIME+" FROM "+TABLE_NAME+" WHERE "+COL_CHANNEL+"=? ORDER BY "+COL_ID+" ASC", new String[]{channel.id});
        bc.count = 0;
        if(bc.cursor.moveToLast()) {
            do {
                bc.count += 1;
                long id = bc.cursor.getLong(0);
                long sn = bc.cursor.getLong(1);
                int user = bc.cursor.getInt(2);
                if (user == 0) {
                    if (!hasChannelData) {
                        hasChannelData = true;
                        channelDataSn = sn;
                    } else {
                        if (sn == channelDataSn - 1) {
                            channelDataSn = sn;
                            channelDataId = id;
                        } else {
                            bc.loadMoreBefore = channelDataId;
                            bc.loadMoreAfter = id;
                            bc.loadMoreChannelData = true;
                            break;
                        }
                    }
                } else {
                    if (!hasUserData) {
                        hasUserData = true;
                        userDataSn = sn;
                    } else {
                        if (sn == userDataSn - 1) {
                            userDataSn = sn;
                            userDataId = id;
                        } else {
                            bc.loadMoreBefore = userDataId;
                            bc.loadMoreAfter = id;
                            bc.loadMoreUserData = true;
                            break;
                        }
                    }
                }
            } while (bc.cursor.moveToPrevious());
        }
        Log.e("lala", "loadMoreBefore="+bc.loadMoreBefore);
        Log.e("lala", "loadMoreAfter="+bc.loadMoreAfter);
        Log.e("lala", "loadMoreUserData="+bc.loadMoreUserData);
        Log.e("lala", "loadMoreChannelData="+bc.loadMoreChannelData);
        Log.e("lala", "firstId="+bc.firstId);
        Log.e("lala", "lastId="+bc.lastId);
        Log.e("lala", "count="+bc.count);
        if(bc.cursor.moveToLast()){
            bc.lastId = bc.cursor.getLong(0);
        }else{
            bc.lastId = -1;
        }
        if(bc.cursor.moveToFirst()){
            bc.firstId = bc.cursor.getLong(0);
        }else{
            bc.firstId = -1;
        }
        return bc;
    }
}

