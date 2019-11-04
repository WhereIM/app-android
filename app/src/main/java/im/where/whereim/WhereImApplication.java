package im.where.whereim;

import androidx.multidex.MultiDexApplication;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;

import io.branch.referral.Branch;

/**
 * Created by buganini on 03/01/17.
 */

public class WhereImApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
        Branch.getAutoInstance(this);
        LoginManager.getInstance().logOut();
        AppEventsLogger.activateApp(this);
    }
}
