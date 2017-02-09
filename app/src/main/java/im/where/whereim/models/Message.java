package im.where.whereim.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.where.whereim.CoreService;
import im.where.whereim.Models;
import im.where.whereim.R;

/**
 * Created by buganini on 17/01/17.
 */

public class Message extends BaseModel {
    public static final String TABLE_NAME = "message";

    private final static String COL_ID = "_id";
    private final static String COL_SN = "sn";
    private final static String COL_CHANNEL = "channel";
    private final static String COL_PUBLIC = "public";
    private final static String COL_MATE = "mate";
    private final static String COL_TYPE = "type";
    private final static String COL_MESSAGE = "message";
    private final static String COL_TIME = "time";

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " INTEGER PRIMARY KEY, " +
                COL_SN + " INTEGER, " +
                COL_CHANNEL + " TEXT NULL, " +
                COL_PUBLIC + " INTEGER, " +
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
    public int notify;
    public boolean isPublic;

    public static Message parse(JSONObject json){
        try {
            Message m = new Message();
            if(json.has(Models.KEY_ID)) {
                m.id = json.getLong(Models.KEY_ID);
            }
            if(json.has(Models.KEY_SN)) {
                m.sn = json.getLong(Models.KEY_SN);
            }
            if(json.has(Models.KEY_CHANNEL)){
                m.channel_id = json.getString("channel");
            }
            if(json.has(Models.KEY_MATE)) {
                m.mate_id = json.getString(Models.KEY_MATE);
            }
            m.type = json.getString("type");
            m.message = json.getString("message");
            m.time = json.getLong("time");
            if(json.has("notify")){
                m.notify = json.getInt("notify");
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
        m.notify = 0;
        m.isPublic = 0 != cursor.getInt(cursor.getColumnIndexOrThrow(COL_PUBLIC));
        return m;
    }

    Pattern iconPattern = Pattern.compile("\\{icon\\}");
    public SpannableString getText(Context context, CoreService.CoreBinder binder){
        if("text".equals(this.type)){
            return new SpannableString(this.message);
        }
        try {
            SpannableString ret;
            JSONObject json = new JSONObject(this.message);
            JSONObject j;
            Drawable d;
            ImageSpan span;
            switch (this.type) {
                case "enchantment_create":
                    return new SpannableString(context.getResources().getString(R.string.message_enchantment_create, json.optString("name", "")));

                case "enchantment_emerge":
                    return new SpannableString(context.getResources().getString(R.string.message_enchantment_emerge, json.optString("name", "")));

                case "enchantment_in":
                    return new SpannableString(context.getResources().getString(R.string.message_enchantment_in, json.optString("name", "")));

                case "enchantment_out":
                    return new SpannableString(context.getResources().getString(R.string.message_enchantment_out, json.optString("name", "")));

                case "marker_create":
                    ret = new SpannableString(context.getResources().getString(R.string.message_marker_create, json.optString("name", "")));
                    if(json.has(Models.KEY_ATTR)) {
                        j = json.getJSONObject(Models.KEY_ATTR);
                        if(j.has(Models.KEY_COLOR)){
                            d = ResourcesCompat.getDrawable(context.getResources(), Marker.getIconResource(j.getString(Models.KEY_COLOR)), null);
                            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                            Matcher m = iconPattern.matcher(ret);
                            if(m.find()) {
                                span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                                ret.setSpan(span, m.start(), m.end(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                            }
                        }
                    }
                    return ret;
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
    public ContentValues buildContentValues(SQLiteDatabase db) {
        ContentValues cv = new ContentValues();
        if(id==0){
            Cursor cursor = db.rawQuery("SELECT "+COL_ID+" FROM "+TABLE_NAME+" WHERE "+COL_CHANNEL+" IS NULL AND "+COL_ID+" < 0 ORDER BY "+COL_ID, new String[]{});
            if(cursor.moveToFirst()){
                id = cursor.getLong(0) - 1;
            }else{
                id = -1L;
            }
            cursor.close();
        }
        cv.put(COL_ID, id);
        cv.put(COL_SN, sn);
        cv.put(COL_CHANNEL, channel_id);
        cv.put(COL_MATE, mate_id);
        cv.put(COL_TYPE, type);
        cv.put(COL_MESSAGE, message);
        cv.put(COL_TIME, time);
        cv.put(COL_PUBLIC, isPublic?1:0);
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

    public static BundledCursor getCursor(SQLiteDatabase db, Channel channel){
        BundledCursor bc = new BundledCursor();
        bc.loadMoreChannelData = false;
        bc.loadMoreUserData = false;
        boolean hasChannelData = false;
        boolean hasUserData = false;
        long channelDataSn = 0;
        long channelDataId = 0;
        long userDataSn = 0;
        long userDataId = 0;

        bc.cursor = db.rawQuery("SELECT "+COL_ID+","+COL_SN+","+COL_PUBLIC+","+COL_CHANNEL+","+COL_MATE+","+COL_TYPE+","+COL_MESSAGE+","+COL_TIME+" FROM "+TABLE_NAME+" WHERE "+COL_CHANNEL+"=? OR "+COL_CHANNEL+" IS NULL ORDER BY "+COL_TIME+" ASC,"+COL_ID+" ASC", new String[]{channel.id});
        bc.count = 0;
        if(bc.cursor.moveToLast()) {
            do {
                bc.count += 1;
                long id = bc.cursor.getLong(0);
                long sn = bc.cursor.getLong(1);
                boolean isPublic = 0 != bc.cursor.getInt(2);
                String channel_id = bc.cursor.getString(3);
                if(channel_id==null){
                    continue;
                }
                if (isPublic) {
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
