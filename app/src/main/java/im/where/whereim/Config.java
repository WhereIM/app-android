package im.where.whereim;

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

    public final static String CAPTCHA_URL = "https://where.im/captcha.html";
    public final static String CAPTCHA_PREFIX = "whereim://";

    public final static int FACEBOOK_REQUEST_CODE = 5566;

    public final static int[] SELF_RADIUS = new int[]{75, 100, 150, 200, 250, 300, 400, 500, 1000, 1500, 2000, 3000};
    public final static int[] ENCHANTMENT_RADIUS = new int[]{15, 30, 50, 75, 100, 150, 200, 250, 300, 400, 500, 1000, 1500, 2000, 3000};
    public final static int DEFAULT_ENCHANTMENT_RADIUS_INDEX = 2;

    public final static int MAP_AD_TTL = 5 * 60 * 1000; //ms
}
