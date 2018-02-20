package im.where.whereim;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import org.json.JSONObject;

/**
 * Created by buganini on 06/01/17.
 */

public class Util {

    static public String JsonGetNullableString(JSONObject jsObj, String key) {
        try {
            if (!jsObj.has(key) || jsObj.isNull(key)) {
                return null;
            } else {
                return jsObj.getString(key);
            }
        } catch (Exception e) {
            return null;
        }
    }

    static public String JsonOptNullableString(JSONObject jsObj, String key, String fallback){
        try {
            if (!jsObj.has(key)) {
                return fallback;
            } else {
                if (jsObj.isNull(key)) {
                    return null;
                } else {
                    return jsObj.getString(key);
                }
            }
        }catch (Exception e){
            return fallback;
        }
    }

    static public Double JsonOptNullableDouble(JSONObject jsObj, String key, Double fallback){
        try {
            if (!jsObj.has(key)) {
                return fallback;
            } else {
                if (jsObj.isNull(key)) {
                    return null;
                } else {
                    return jsObj.getDouble(key);
                }
            }
        }catch (Exception e){
            return fallback;
        }
    }

    static public Boolean JsonOptBoolean(JSONObject jsObj, String key, Boolean fallback) {
        try {
            if (!jsObj.has(key)) {
                return fallback;
            } else {
                return jsObj.getBoolean(key);
            }
        } catch (Exception e) {
            return fallback;
        }
    }

    static public void closeKeyboard(Activity activity){
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
    }
}
