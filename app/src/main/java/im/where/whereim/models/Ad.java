package im.where.whereim.models;

import org.json.JSONException;
import org.json.JSONObject;

import im.where.whereim.Config;
import im.where.whereim.Models;

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
            ad.id = data.getString(Models.KEY_ID);
            ad.latitude = data.getDouble(Models.KEY_LATITUDE);
            ad.longitude = data.getDouble(Models.KEY_LONGITUDE);
            ad.name = data.getString(Models.KEY_NAME);
            ad.desc = data.getString(Models.KEY_DESC);
            ad.level = data.getInt(Models.KEY_LEVEL);
            ad.ttl = System.currentTimeMillis() + Config.MAP_AD_TTL;
            return ad;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
