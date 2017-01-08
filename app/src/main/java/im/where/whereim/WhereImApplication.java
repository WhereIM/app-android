package im.where.whereim;

import android.content.Intent;
import android.support.multidex.MultiDexApplication;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;

/**
 * Created by buganini on 03/01/17.
 */

public class WhereImApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext(), Config.FACEBOOK_REQUEST_CODE);
        LoginManager.getInstance().logOut();
        AppEventsLogger.activateApp(this);
        startService(new Intent(this, CoreService.class));
    }
}
