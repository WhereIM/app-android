package im.where.whereim.models;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.drawable.Drawable;
import android.support.v4.content.res.ResourcesCompat;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Locale;

import im.where.whereim.CoreService;
import im.where.whereim.Key;
import im.where.whereim.R;
import im.where.whereim.Util;
import im.where.whereim.views.WimSpan;

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
                COL_PUBLIC + " BOOLEAN, " +
                COL_MATE + " TEXT, " +
                COL_TYPE + " TEXT, " +
                COL_MESSAGE + " TEXT, " +
                COL_TIME + " INTEGER)";
        db.execSQL(sql);

        sql = "CREATE INDEX message_index ON "+TABLE_NAME+" ("+COL_CHANNEL+")";
        db.execSQL(sql);
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
            if(json.has(Key.ID)) {
                m.id = json.getLong(Key.ID);
            }
            if(json.has(Key.SN)) {
                m.sn = json.getLong(Key.SN);
            }
            if(json.has(Key.CHANNEL)){
                m.channel_id = json.getString(Key.CHANNEL);
            }
            if(json.has(Key.MATE)) {
                m.mate_id = Util.JsonGetNullableString(json, Key.MATE);
            }
            m.type = json.getString("type");
            m.message = json.getString("message");
            m.isPublic = json.getBoolean(Key.PUBLIC);
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
        int COL_MATE_IDX = cursor.getColumnIndexOrThrow(COL_MATE);
        Message m = new Message();
        m.id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID));
        m.sn = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SN));
        m.channel_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL));
        m.mate_id = cursor.isNull(COL_MATE_IDX) ? null : cursor.getString(COL_MATE_IDX);
        m.type = cursor.getString(cursor.getColumnIndexOrThrow(COL_TYPE));
        m.message = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE));
        m.time = cursor.getLong(cursor.getColumnIndexOrThrow(COL_TIME));
        m.isPublic = 0 != cursor.getInt(cursor.getColumnIndexOrThrow(COL_PUBLIC));
        return m;
    }

    public String getPayload() {
        JSONObject json = null;
        try {
            json = new JSONObject(this.message);
        } catch (Exception e) {
            return null;
        }
        try {
            switch (this.type) {
                case "image": {
                        if(!json.has(Key.IMAGE)){
                            return null;
                        }
                        return json.getString(Key.IMAGE);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
    public SpannableString getText(Context context, CoreService.CoreBinder binder, WimSpan.OnClickedListener clickedListener){
        if("text".equals(this.type)){
            return new SpannableString(this.message);
        }
        try {
            SpannableString ret;
            JSONObject json = null;
            try {
                json = new JSONObject(this.message);
            } catch (Exception e) {
                // noop
            }
            JSONObject j;
            Drawable d;
            ImageSpan span;
            switch (this.type) {
                case "rich": {
                    SpannableStringBuilder b = new SpannableStringBuilder();
                    Double lat = null;
                    Double lng = null;
                    try {
                        lat = json.optDouble(Key.LATITUDE);
                        lng = json.getDouble(Key.LONGITUDE);
                    } catch (Exception e) {
                        //noop
                    }
                    if (lat != null && lng != null) {
                        SpannableString icon = new SpannableString("@");
                        d = ResourcesCompat.getDrawable(context.getResources(), R.drawable.icon_pin, null);
                        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                        span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                        icon.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                        icon.setSpan(new WimSpan(String.format(Locale.ENGLISH, "pin/%f/%f", lat, lng), clickedListener), 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                        b.append(icon);
                        b.append("\n");
                    }
                    b.append(new SpannableString(json.optString("text")));
                    return SpannableString.valueOf(b);
                }
                case "enchantment_create":
                    return new SpannableString(context.getResources().getString(R.string.message_enchantment_create, json.optString("name", "")));

                case "enchantment_emerge":
                    return new SpannableString(context.getResources().getString(R.string.message_enchantment_emerge, json.optString("name", "")));

                case "enchantment_in":
                    return new SpannableString(context.getResources().getString(R.string.message_enchantment_in, json.optString("name", "")));

                case "enchantment_out":
                    return new SpannableString(context.getResources().getString(R.string.message_enchantment_out, json.optString("name", "")));

                case "marker_create": {
                    SpannableStringBuilder b = new SpannableStringBuilder();
                    if (json.has(Key.ATTR)) {
                        j = json.getJSONObject(Key.ATTR);
                        if (j.has(Key.COLOR)) {
                            SpannableString icon = new SpannableString("@");
                            d = ResourcesCompat.getDrawable(context.getResources(), Marker.getIconResource(j.getString(Key.COLOR)), null);
                            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                            span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                            icon.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                            if (json.has(Key.ID) && json.has(Key.LATITUDE) && json.has(Key.LONGITUDE)) {
                                icon.setSpan(new WimSpan(String.format(Locale.ENGLISH, "marker/%s/%f/%f", json.getString(Key.ID), json.getDouble(Key.LATITUDE), json.getDouble(Key.LONGITUDE)), clickedListener), 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                            }
                            b.append(icon);
                            b.append("\n");
                        }
                    }
                    b.append(new SpannableString(context.getResources().getString(R.string.message_marker_create, json.optString("name", ""))));
                    return SpannableString.valueOf(b);
                }
                case "radius_report":
                    return new SpannableString(context.getResources().getString(R.string.message_radius_report, json.optString("in", ""), json.optString("out", ""), json.optString(Key.RADIUS, "")));

                default:
                    return new SpannableString(Html.fromHtml(String.format("<font color=\"gray\"><i>%s</i></font>", context.getResources().getString(R.string.message_not_implemented))));
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
        public HashMap<Long, Integer> positionMap = new HashMap<>();
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

        bc.cursor = db.rawQuery("SELECT "+COL_ID+","+COL_SN+","+COL_PUBLIC+","+COL_CHANNEL+","+COL_MATE+","+COL_TYPE+","+COL_MESSAGE+","+COL_TIME+" FROM "+TABLE_NAME+" WHERE "+COL_CHANNEL+"=? OR "+COL_CHANNEL+" IS NULL ORDER BY "+COL_ID+" ASC", new String[]{channel.id});
        bc.count = 0;
        if(bc.cursor.moveToLast()) {
            do {
                bc.count += 1;
                long id = bc.cursor.getLong(0);
                long sn = bc.cursor.getLong(1);
                boolean isPublic = 0 != bc.cursor.getInt(2);
                String channel_id = bc.cursor.getString(3);

                bc.positionMap.put(id, bc.cursor.getPosition());

                if(channel_id==null){
                    continue;
                }
                if (isPublic) {
                    if (!hasChannelData) {
                        hasChannelData = true;
                        channelDataSn = sn;
                    } else {
                        if (sn == channelDataSn - 1) {
                            Log.e("lala", "sn="+sn+", channelDataSn="+channelDataSn);
                            channelDataSn = sn;
                            channelDataId = id;
                        } else {
                            Log.e("lala", "!! sn="+sn+", channelDataSn="+channelDataSn);
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
                            Log.e("lala", "sn="+sn+", userDataSn="+userDataSn);
                            userDataSn = sn;
                            userDataId = id;
                        } else {
                            Log.e("lala", "!! sn="+sn+", userDataSn="+userDataSn);
                            bc.loadMoreBefore = userDataId;
                            bc.loadMoreAfter = id;
                            bc.loadMoreUserData = true;
                            break;
                        }
                    }
                }
            } while (bc.cursor.moveToPrevious());
        }
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
        Log.e("lala", "===========================");
        Log.e("lala", "loadMoreBefore="+bc.loadMoreBefore);
        Log.e("lala", "loadMoreAfter="+bc.loadMoreAfter);
        Log.e("lala", "loadMoreUserData="+bc.loadMoreUserData);
        Log.e("lala", "loadMoreChannelData="+bc.loadMoreChannelData);
        Log.e("lala", "firstId="+bc.firstId);
        Log.e("lala", "lastId="+bc.lastId);
        Log.e("lala", "count="+bc.count);
        Log.e("lala", "----------------------------");
        return bc;
    }
}

