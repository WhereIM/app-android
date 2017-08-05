package im.where.whereim;

import android.content.Intent;
import android.support.multidex.MultiDexApplication;

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

        Branch.getAutoInstance(this);
        LoginManager.getInstance().logOut();
        AppEventsLogger.activateApp(this);
        startService(new Intent(this, CoreService.class));
    }
}
