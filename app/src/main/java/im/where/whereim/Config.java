package im.where.whereim;

import com.amazonaws.regions.Regions;

/**
 * Created by buganini on 07/01/17.
 */

public class Config {
    public final static String APP_SHARED_PREFERENCES_NAME = "where.im";

    public final static String CERT_ID = "whereim";
    public final static String KEY_STORE_NAME = "whereim.jks";
    public final static String KEY_STORE_PASSWORD = "xxxxx";

    public final static Regions AWS_REGION_ID = Regions.AP_NORTHEAST_1;
    public final static String AWS_IOT_MQTT_ENDPOINT = "a3ftvwpcurxils";
    public final static String AWS_API_GATEWAY_REGISTER_CLIENT = "https://gznzura26h.execute-api.ap-northeast-1.amazonaws.com/production";

    public final static int FACEBOOK_REQUEST_CODE = 5566;
}
