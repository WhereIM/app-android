package im.where.whereim;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by buganini on 07/01/17.
 */

public class BaseActivity extends AppCompatActivity implements ServiceConnection {

    protected CoreService.CoreBinder mBinder;

    public CoreService.CoreBinder getBinder(){
        return mBinder;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, CoreService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        unbindService(this);
        mBinder = null;

        super.onPause();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mBinder = (CoreService.CoreBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
