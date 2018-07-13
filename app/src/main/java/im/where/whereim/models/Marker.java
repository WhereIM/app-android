package im.where.whereim.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import im.where.whereim.Key;
import im.where.whereim.R;

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
    private final static String COL_ATTR = "attr";
    private final static String COL_PUBLIC = "public";
    private final static String COL_ENABLED = "enabled";

    public static String[] getIconList(){
        return new String[]{
            "red",
            "orange",
            "yellow",
            "green",
            "cyan",
            "azure",
            "blue",
            "violet",
            "magenta",
            "rose",
            "grey"
        };
    }

    public static void createTable(SQLiteDatabase db){
        String sql;
        sql = "CREATE TABLE " + TABLE_NAME + " (" +
                COL_ID + " TEXT PRIMARY KEY, " +
                COL_CHANNEL_ID + " TEXT, " +
                COL_NAME + " TEXT, " +
                COL_LATITUDE + " DOUBLE PRECISION, " +
                COL_LONGITUDE + " DOUBLE PRECISION, " +
                COL_ATTR + " TEXT, " +
                COL_PUBLIC + " BOOLEAN, " +
                COL_ENABLED + " BOOLEAN)";
        db.execSQL(sql);

        sql = "CREATE INDEX marker_index ON "+TABLE_NAME+" ("+COL_CHANNEL_ID+")";
        db.execSQL(sql);
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
    public JSONObject attr;
    public boolean isPublic;
    public Boolean enabled;
    public boolean deleted = false;

    public final static String DEFAULT_COLOR = "red";

    public static int getIconResource(String color){
        switch(color){
            case "azure": return R.drawable.icon_marker_azure;
            case "blue": return R.drawable.icon_marker_blue;
            case "cyan": return R.drawable.icon_marker_cyan;
            case "green": return R.drawable.icon_marker_green;
            case "grey": return R.drawable.icon_marker_grey;
            case "magenta": return R.drawable.icon_marker_magenta;
            case "orange": return R.drawable.icon_marker_orange;
            case "red": return R.drawable.icon_marker_red;
            case "rose": return R.drawable.icon_marker_rose;
            case "violet": return R.drawable.icon_marker_violet;
            case "yellow": return R.drawable.icon_marker_yellow;
        }
        return R.drawable.icon_marker_red;
    }

    public String getIconColor() {
        String color = "red";
        if(attr!=null){
            color = attr.optString(Key.COLOR, "red");
        }
        return color;
    }

    public int getIconResId(){
        return getIconResource(getIconColor());
    }

    private final static HashMap<Integer, BitmapDescriptor> mIconBitmapDescriptor = new HashMap<>();
    public BitmapDescriptor getIconBitmapDescriptor(){
        return getIconBitmapDescriptor(getIconResId());
    }

    public static BitmapDescriptor getIconBitmapDescriptor(int resourceId){
        BitmapDescriptor f;
        if(mIconBitmapDescriptor.containsKey(resourceId)) {
            f = mIconBitmapDescriptor.get(resourceId);
        }else{
            f = BitmapDescriptorFactory.fromResource(resourceId);
            mIconBitmapDescriptor.put(resourceId, f);
        }
        return f;
    }

    public static Marker parse(Cursor cursor){
        Marker marker = new Marker();
        marker.id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID));
        marker.channel_id = cursor.getString(cursor.getColumnIndexOrThrow(COL_CHANNEL_ID));
        marker.name = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME));
        marker.latitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LATITUDE));
        marker.longitude = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_LONGITUDE));
        try {
            marker.attr = new JSONObject(cursor.getString(cursor.getColumnIndexOrThrow(COL_ATTR)));
        } catch (JSONException e) {
            marker.attr = new JSONObject();
        }
        marker.isPublic = cursor.getInt(cursor.getColumnIndexOrThrow(COL_PUBLIC))!=0;
        marker.enabled = cursor.getInt(cursor.getColumnIndexOrThrow(COL_ENABLED))!=0;
        return marker;
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
        cv.put(COL_ATTR, attr.toString());
        cv.put(COL_PUBLIC, isPublic?1:0);
        if (enabled != null) {
            cv.put(COL_ENABLED, enabled ?1:0);
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

