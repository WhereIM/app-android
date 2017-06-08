package im.where.whereim;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by buganini on 07/01/17.
 */

public class BaseActivity extends AppCompatActivity implements ServiceConnection {

    private List<CoreService.BinderTask> mPendingTask = new ArrayList<>();

    protected Handler mHandler = new Handler();
    protected void postUITask(Runnable r){
        mHandler.post(r);
    }

    protected void postBinderTask(CoreService.BinderTask task){
        synchronized (mPendingTask) {
            mPendingTask.add(task);
        }
        processBinderTask();
    }


    private void processBinderTask() {
        if(mBinder==null){
            return;
        }
        while(true){
            CoreService.BinderTask task = null;
            synchronized (mPendingTask){
                if(mPendingTask.size()>0){
                    task = mPendingTask.remove(0);
                }
            }
            if(task==null){
                break;
            }else{
                task.onBinderReady(mBinder);
            }
        }
    }

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
        super.onPause();

        unbindService(this);
        mBinder = null;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        mBinder = (CoreService.CoreBinder) service;
        processBinderTask();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }
}
