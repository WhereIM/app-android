package im.where.whereim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import im.where.whereim.models.Channel;

/**
 * Created by buganini on 20/02/18.
 */

public abstract class BaseChannelActivity extends BaseActivity {
    protected Channel mChannel;

    interface GetChannelCallback{
        void onGetChannel(Channel channel);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(intent != null && intent.hasExtra(Key.CHANNEL)){
            setChannel(intent.getStringExtra(Key.CHANNEL));
        } else {
            setChannel(null);
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    if(binder.getChannelList().size() > 0){
                        channelReady = true;
                        processChannelReadyTask();
                    }
                }
            });
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if(intent != null && intent.hasExtra(Key.CHANNEL)){
            setChannel(intent.getStringExtra(Key.CHANNEL));
        }
    }

    final private List<Runnable> mPendingChannelReadyTask = new LinkedList<>();

    protected void postChannelReadyTask(Runnable task){
        synchronized (mPendingChannelReadyTask) {
            mPendingChannelReadyTask.add(task);
        }
        processChannelReadyTask();
    }


    private void processChannelReadyTask() {
        if(!channelReady){
            return;
        }
        while(true){
            Runnable task = null;
            synchronized (mPendingChannelReadyTask){
                if(mPendingChannelReadyTask.size()>0){
                    task = mPendingChannelReadyTask.remove(0);
                }
            }
            if(task==null){
                break;
            }else{
                task.run();
            }
        }
    }

    private boolean channelReady = false;
    private Runnable mChannelSyncedCallback = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    if(binder.getChannelList().size()==0){
                        Intent intent = new Intent(BaseChannelActivity.this, NewChannelActivity.class);
                        startActivity(intent);
                        finish();
                    }else{
                        if(!channelReady){
                            channelReady = true;
                            processChannelReadyTask();
                        }
                    }
                }
            });
        }
    };

    private Runnable mChannelChangedCallback = new Runnable() {
        @Override
        public void run() {
            onChannelChanged(null);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.addChannelChangedListener(mChannel.id, mChannelChangedCallback);
                binder.addChannelSyncedListeners(mChannelSyncedCallback);
            }
        });
    }

    @Override
    protected void onPause() {
        if(mBinder != null){
            mBinder.removeChannelSyncedListeners(mChannelSyncedCallback);
            mBinder.removeChannelChangedListener(mChannel.id, mChannelChangedCallback);
        }
        super.onPause();
    }

    protected void setChannel(String channel_id){
        if(channel_id==null) {
            SharedPreferences sp = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            channel_id = sp.getString(Key.CHANNEL, null);
        }
        final String _channel_id = channel_id;
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                boolean failed = false;
                if(_channel_id == null){
                    failed = true;
                }
                final Channel prevChannel = mChannel;
                mChannel = mBinder.getChannelById(_channel_id);
                if(mChannel == null){
                    failed = true;
                }
                if(!failed){
                    SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                    editor.putString(Key.CHANNEL, _channel_id);
                    editor.apply();

                    onChannelChanged(prevChannel);
                    processGetChannelCallback();
                }
            }
        });
    }

    protected abstract void onChannelChanged(Channel prevChannel);

    private final List<GetChannelCallback> mGetChannelCallback = new ArrayList<>();
    protected void getChannel(GetChannelCallback callback){
        synchronized (mGetChannelCallback) {
            mGetChannelCallback.add(callback);
        }
        processGetChannelCallback();
    }

    protected void processGetChannelCallback(){
        if(mChannel==null){
            return;
        }
        synchronized (mGetChannelCallback) {
            while(mGetChannelCallback.size()>0){
                GetChannelCallback callback = mGetChannelCallback.remove(0);
                callback.onGetChannel(mChannel);
            }
        }
    }

    @Override
    public void finish() {
        synchronized (mPendingChannelReadyTask) {
            mPendingChannelReadyTask.clear();
        }
        super.finish();
    }
}
