package im.where.whereim;

import android.content.Context;
import android.content.SharedPreferences;

import com.amazonaws.regions.Regions;

/**
 * Created by buganini on 07/01/17.
 */

public class Config {
    public final static boolean DEBUG_QUADTREE = false;

    public final static String APP_SHARED_PREFERENCES_NAME = "where.im";

    public final static String CERT_ID = "whereim";
    public final static String KEY_STORE_NAME = "whereim.jks";
    public final static String KEY_STORE_PASSWORD = "xxxxx";

    public final static Regions AWS_REGION_ID = Regions.AP_NORTHEAST_1;
    public final static String AWS_IOT_MQTT_ENDPOINT = "a3ftvwpcurxils";
    public final static String AWS_API_GATEWAY_REGISTER_CLIENT = "https://gznzura26h.execute-api.ap-northeast-1.amazonaws.com/production";

    public final static String CAPTCHA_URL = "http://dev.where.im/captcha.html";
    public final static String CAPTCHA_PREFIX = "whereim://";

    public final static String WHERE_IM_URL = "https://dev.where.im/%s";

    public final static int[] SELF_RADIUS = new int[]{75, 100, 150, 200, 250, 300, 400, 500, 1000, 1500, 2000, 3000};
    public final static int[] ENCHANTMENT_RADIUS = new int[]{15, 30, 50, 75, 100, 150, 200, 250, 300, 400, 500, 1000, 1500, 2000, 3000};
    public final static int DEFAULT_ENCHANTMENT_RADIUS_INDEX = 2;

    public final static int MAP_AD_TTL = 5 * 60 * 1000; //ms

    public final static String API_KEY_MAPBOX = "pk.eyJ1Ijoid2hlcmVpbSIsImEiOiJjaXltbmtvbHUwMDM4MzNwZnNsZHVtbHE4In0.n36bMG_LdA9yOu8-fQS2vw";

    public enum MapProvider {
        GOOGLE,
        MAPBOX
    }
    public static MapProvider getMapProvider(Context context){
        SharedPreferences sp = context.getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        return MapProvider.valueOf(sp.getString(Key.SERVICE_PROVIDER, MapProvider.GOOGLE.toString()));
    }

    public static void setMapProvider(Context context, MapProvider provider){
        SharedPreferences.Editor editor = context.getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(Key.SERVICE_PROVIDER, provider.toString());
        editor.apply();
    }
}
