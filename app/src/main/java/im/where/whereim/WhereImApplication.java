package im.where.whereim;

import android.content.Intent;
import android.support.multidex.MultiDexApplication;

/**
 * Created by buganini on 03/01/17.
 */

public class WhereImApplication extends MultiDexApplication {
    @Override
    public void onCreate() {
        super.onCreate();

        startService(new Intent(this, CoreService.class));
    }
}
