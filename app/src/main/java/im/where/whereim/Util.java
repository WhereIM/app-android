package im.where.whereim;

import org.json.JSONObject;

/**
 * Created by buganini on 06/01/17.
 */

public class Util {

    static public String JsonGetNullableString(JSONObject jsObj, String key){
        try {
            if (!jsObj.has(key) || jsObj.isNull(key)) {
                return null;
            } else {
                return jsObj.getString(key);
            }
        }catch (Exception e){
            return null;
        }
    }

    static public String JsonOptNullableString(JSONObject jsObj, String key, String fallback){
        try {
            if (!jsObj.has(key) || jsObj.isNull(key)) {
                return fallback;
            } else {
                return jsObj.getString(key);
            }
        }catch (Exception e){
            return fallback;
        }
    }
}
