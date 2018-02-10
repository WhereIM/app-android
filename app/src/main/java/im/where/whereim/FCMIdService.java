package im.where.whereim;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class FCMIdService extends Service {
    public FCMIdService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
