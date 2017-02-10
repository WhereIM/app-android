package im.where.whereim.models;

import org.json.JSONException;
import org.json.JSONObject;

import im.where.whereim.Config;
import im.where.whereim.Key;

/**
 * Created by buganini on 05/02/17.
 */

public class Ad {
    public String id;
    public double latitude;
    public double longitude;
    public String name;
    public String desc;
    public String icon;
    public int level;
    public long ttl;

    public static Ad parse(JSONObject data){
        try {
            Ad ad = new Ad();
            ad.id = data.getString(Key.ID);
            ad.latitude = data.getDouble(Key.LATITUDE);
            ad.longitude = data.getDouble(Key.LONGITUDE);
            ad.name = data.getString(Key.NAME);
            ad.desc = data.getString(Key.DESC);
            ad.level = data.getInt(Key.LEVEL);
            ad.ttl = System.currentTimeMillis() + Config.MAP_AD_TTL;
            return ad;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
