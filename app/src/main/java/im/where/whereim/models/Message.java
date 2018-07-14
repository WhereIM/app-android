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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

import im.where.whereim.Key;
import im.where.whereim.R;
import im.where.whereim.Util;
import im.where.whereim.views.WimSpan;

/**
 * Created by buganini on 17/01/17.
 */

public class Message extends BaseModel {
    public static final String TABLE_NAME = "message";

    final static String COL_ID = "_id";
    final static String COL_SN = "sn";
    final static String COL_CHANNEL = "channel";
    final static String COL_PUBLIC = "public";
    final static String COL_MATE = "mate";
    final static String COL_TYPE = "type";
    final static String COL_MESSAGE = "message";
    final static String COL_TIME = "time";
    final static String COL_DELETED = "deleted";
    final static String COL_HIDDEN = "hidden";

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
                COL_DELETED + " BOOLEAN, " +
                COL_HIDDEN + " BOOLEAN, " +
                COL_TIME + " INTEGER)";
        db.execSQL(sql);

        sql = "CREATE INDEX message_index ON "+TABLE_NAME+" ("+COL_CHANNEL+")";
        db.execSQL(sql);
    }

    public static void upgradeTable(SQLiteDatabase db, int currVersion){
        String sql;
        if (currVersion < 6) {
            sql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_DELETED + " BOOLEAN NOT NULL DEFAULT 0";
            db.execSQL(sql);
            sql = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + COL_HIDDEN + " BOOLEAN NOT NULL DEFAULT 0";
            db.execSQL(sql);

            currVersion = 6;
        }
    }

    public long id;
    public long sn;
    public String channel_id;
    public String mate_id;
    public String type;
    public String message;
    public long time;
    public boolean isPublic;
    public boolean deleted;
    public boolean hidden;

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
            if(json.has(Key.DELETED)){
                m.deleted = json.getBoolean(Key.DELETED);
            }
            if(json.has(Key.HIDDEN)){
                m.hidden = json.getBoolean(Key.HIDDEN);
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
        m.deleted = 0 != cursor.getInt(cursor.getColumnIndexOrThrow(COL_DELETED));
        m.hidden = 0 != cursor.getInt(cursor.getColumnIndexOrThrow(COL_HIDDEN));
        return m;
    }

    public Image getImage() {
        JSONObject json = null;
        try {
            json = new JSONObject(this.message);
        } catch (Exception e) {
            return null;
        }
        if(!"image".equals(this.type)){
            return null;
        }
        if(!json.has(Key.KEY)){
            return null;
        }
        try {
            Image img = new Image();
            img.key = json.getString(Key.KEY);
            if(json.has(Key.WIDTH) && json.has(Key.HEIGHT)){
                img.width = json.getInt(Key.WIDTH);
                img.height = json.getInt(Key.HEIGHT);
            }else{
                img.width = -1;
                img.height = -1;
            }
            if(json.has(Key.EXTENSION)){
                img.ext = json.getString(Key.EXTENSION);
            }else{
                img.ext = "jpg";
            }
            return img;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    public SpannableString getText(Context context, WimSpan.OnClickedListener clickedListener) {
        if(deleted){
            return new SpannableString(Html.fromHtml(String.format("<font color=\"gray\"><i>%s</i></font>", context.getResources().getString(R.string.message_deleted))));
        }
        if(hidden){
            return new SpannableString(Html.fromHtml(String.format("<font color=\"gray\"><i>%s</i></font>", context.getResources().getString(R.string.message_hidden))));
        }
        return getText(context, this.type, this.message, clickedListener);
    }

    public static SpannableString getText(Context context, String type, Object message, WimSpan.OnClickedListener clickedListener){
        if("text".equals(type)){
            return new SpannableString((String) message);
        }
        try {
            JSONObject json = null;
            if(message instanceof  JSONObject){
                json = (JSONObject) message;
            }else if(message instanceof String){
                try {
                    json = new JSONObject((String)message);
                } catch (Exception e) {
                    // noop
                }
            }
            JSONObject j;
            Drawable d;
            ImageSpan span;
            switch (type) {
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
                        d = ResourcesCompat.getDrawable(context.getResources(), R.drawable.crosshair, null);
                        d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
                        span = new ImageSpan(d, ImageSpan.ALIGN_BASELINE);
                        icon.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                        icon.setSpan(new WimSpan(String.format(Locale.ENGLISH, "pin/%f/%f", lat, lng), clickedListener), 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                        b.append(icon);
                        b.append("\n");
                    }
                    b.append(new SpannableString(json.optString(Key.TEXT)));
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

                case "geofence_create":
                    return new SpannableString(context.getResources().getString(R.string.message_geofence_create, json.optString("name", "")));

                case "geofence_emerge":
                    return new SpannableString(context.getResources().getString(R.string.message_geofence_emerge, json.optString("name", "")));

                case "geofence_in":
                    return new SpannableString(context.getResources().getString(R.string.message_geofence_in, json.optString("name", "")));

                case "geofence_out":
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

    public String getPlainText(){
        if("text".equals(type)){
            return this.message;
        }
        JSONObject json;
        try {
            json = new JSONObject(message);
        } catch (Exception e) {
            return null;
        }
        switch (type) {
            case "rich": {
                return json.optString(Key.TEXT);
            }
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
        cv.put(COL_DELETED, deleted?1:0);
        cv.put(COL_HIDDEN, hidden?1:0);
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
        public List<PendingMessage> pending;
    }

    public static void setDeleted(SQLiteDatabase db, String channel_id, long id){
        db.execSQL("UPDATE "+TABLE_NAME+" SET "+COL_DELETED+"=1, "+COL_MESSAGE+"='' WHERE "+COL_CHANNEL+"=? AND "+COL_ID+"="+id, new String[]{channel_id});
    }

    public static void setHidden(SQLiteDatabase db, String channel_id, long id){
        db.execSQL("UPDATE "+TABLE_NAME+" SET "+COL_HIDDEN+"=1, "+COL_MESSAGE+"='' WHERE "+COL_CHANNEL+"=? AND "+COL_ID+"="+id, new String[]{channel_id});
    }

    public static BundledCursor getCursor(SQLiteDatabase db, Channel channel){
        MessageBlock mb = MessageBlock.get(db, channel);

        BundledCursor bc = new BundledCursor();
        bc.cursor = db.rawQuery("SELECT * FROM "+TABLE_NAME+" WHERE "+COL_ID+">="+mb.firstId+" AND "+COL_ID+"<="+mb.lastId+" AND "+COL_TYPE+"!='ctrl' AND ("+COL_CHANNEL+"=? OR "+COL_CHANNEL+" IS NULL) ORDER BY "+COL_ID+" ASC", new String[]{channel.id});
        bc.count = bc.cursor.getCount();
        bc.loadMoreBefore = mb.loadMoreBefore;
        bc.loadMoreAfter = mb.loadMoreAfter;
        if(bc.cursor.moveToLast()){
            mb.lastId = bc.cursor.getLong(bc.cursor.getColumnIndexOrThrow(COL_ID));
        }else{
            mb.lastId = -1;
        }
        if(bc.cursor.moveToFirst()){
            mb.firstId = bc.cursor.getLong(bc.cursor.getColumnIndexOrThrow(COL_ID));
        }else{
            mb.firstId = -1;
        }
        bc.loadMoreChannelData = mb.loadMoreChannelData;
        bc.loadMoreUserData = mb.loadMoreUserData;
        bc.pending = PendingMessage.getMessage(db, channel.id);
        return bc;
    }
}

