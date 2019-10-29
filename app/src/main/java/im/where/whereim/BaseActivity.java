package im.where.whereim;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.where.whereim.dialogs.DialogChannelJoin;
import im.where.whereim.models.POI;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;

/**
 * Created by buganini on 07/01/17.
 */

public class BaseActivity extends AppCompatActivity implements ServiceConnection {

    final private List<CoreService.BinderTask> mPendingTask = new LinkedList<>();

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
                if(mBinder != null){
                    task.onBinderReady(mBinder);
                }
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

    protected void processDeepLink(){
        Branch branch = Branch.getInstance();
        branch.initSession(new Branch.BranchReferralInitListener(){
            @Override
            public void onInitFinished(JSONObject referringParams, BranchError error) {
                if (error == null) {
                    String uri = Util.JsonOptNullableString(referringParams, "$deeplink_path", null);
                    if(uri!=null){
                        processLink(uri);
                    }
                } else {
                    Log.i("WhereIM", error.getMessage());
                }
            }
        }, getIntent().getData(), this);
    }

    protected String pending_joined_channel = null;
    protected void processLink(String link){
        Pattern mPatternChannelJoin = Pattern.compile("^(?:channel|map)/([A-Fa-f0-9]{32})$");
        Matcher m;
        m = mPatternChannelJoin.matcher(link);
        if(m.matches()){
            pending_joined_channel = m.group(1);
            channelJoin(pending_joined_channel);
            return;
        }
        Pattern mPatternHere = Pattern.compile("^here/(-?[0-9.]+)/(-?[0-9.]+)(?:/(.*))?$");
        m = mPatternHere.matcher(link);
        if(m.matches()){
            if(this instanceof ChannelActivity){
                ChannelActivity channelActivity = (ChannelActivity) this;
                POI poi = new POI();
                poi.name = m.group(3);
                poi.latitude = Double.valueOf(m.group(1));
                poi.longitude = Double.valueOf(m.group(2));
                channelActivity.setPOI(poi);
            }
        }
    }

    protected void channelJoin(final String channel_id){
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(final CoreService.CoreBinder binder) {
                new DialogChannelJoin(BaseActivity.this, binder.getUserName(), new DialogChannelJoin.Callback() {
                    @Override
                    public void onDone(String mate_name) {
                        binder.joinChannel(channel_id, null /*channel_alias*/, mate_name);
                    }
                });
            }
        });
    }

    @Override
    public void finish() {
        synchronized (mPendingTask) {
            mPendingTask.clear();
        }
        super.finish();
    }
}
