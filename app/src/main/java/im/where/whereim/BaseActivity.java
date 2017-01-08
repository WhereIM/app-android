package im.where.whereim;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * Created by buganini on 07/01/17.
 */

public class BaseActivity extends AppCompatActivity implements ServiceConnection {

    protected CoreService.CoreBinder mBinder;

    public CoreService.CoreBinder getBinder(){
        return mBinder;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = new Intent(this, CoreService.class);
        bindService(intent, this, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        Log.e("lala", "destroy");
        unbindService(this);
        mBinder = null;

        super.onDestroy();
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mBinder = (CoreService.CoreBinder) service;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
