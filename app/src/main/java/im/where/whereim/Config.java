package im.where.whereim;

import android.content.Context;
import android.content.SharedPreferences;

import com.amazonaws.regions.Regions;
import com.google.common.io.Files;

import java.util.Locale;

import im.where.whereim.models.Image;

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

    public final static String THUMBNAIL_URL = "https://s3-ap-northeast-1.amazonaws.com/whereim-thumbnail/%s";
    public final static String PREVIEW_URL = "https://s3-ap-northeast-1.amazonaws.com/whereim-preview/%s";

    public final static int[] SELF_RADIUS = new int[]{75, 100, 150, 200, 250, 300, 400, 500, 1000, 1500, 2000, 3000};
    public final static int ENCHANTMENT_RADIUS_MAX = 5000;
    public final static int ENCHANTMENT_RADIUS_MIN = 15;
    public final static int DEFAULT_ENCHANTMENT_RADIUS = 50;

    public final static int MAP_AD_TTL = 5 * 60 * 1000; //ms

    public final static String SERVER_KEY_GOOGLE = "737200913386-n4c1mvkcik92n3slo8p2s7smkgs1ue7f.apps.googleusercontent.com";
    public final static String API_KEY_MAPBOX = "pk.eyJ1Ijoid2hlcmVpbSIsImEiOiJjaXltbmtvbHUwMDM4MzNwZnNsZHVtbHE4In0.n36bMG_LdA9yOu8-fQS2vw";

    public static int getRadiusStep(int radius){
        if(radius >= 1000) {
            return 500;
        }
        if(radius >= 500) {
            return 250;
        }
        if(radius >= 300) {
            return 100;
        }
        if(radius >= 50) {
            return 25;
        }
        return 5;
    }

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

    public static String getThumbnail(Image image){
        return String.format(Locale.ENGLISH, Config.THUMBNAIL_URL, Files.getNameWithoutExtension(image.key)+"."+image.ext);
    }

    public static String getPreview(Image image){
        return String.format(Locale.ENGLISH, Config.PREVIEW_URL, Files.getNameWithoutExtension(image.key)+"."+image.ext);
    }
}
