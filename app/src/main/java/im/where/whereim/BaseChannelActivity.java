package im.where.whereim;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
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
        setChannel(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setChannel(intent);
    }

    protected void setChannel(Intent intent){
        if(intent == null){
            return;
        }
        String channel_id = intent.getStringExtra(Key.CHANNEL);
        if(channel_id==null) {
            SharedPreferences sp = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
            channel_id = sp.getString(Key.CHANNEL, null);
            if(channel_id == null) {
                // XXX pick one with greatest TS
                Log.e("lala", "no mChannelId");
            }
        } else {
            SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
            editor.putString(Key.CHANNEL, channel_id);
            editor.apply();
        }

        final String _channel_id = channel_id;
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                mChannel = mBinder.getChannelById(_channel_id);
                onChannelChanged();
                processGetChannelCallback();
            }
        });
    }

    protected abstract void onChannelChanged();

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
}
