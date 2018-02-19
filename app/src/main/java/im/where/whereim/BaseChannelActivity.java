package im.where.whereim;

import android.content.Intent;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

import im.where.whereim.models.Channel;

/**
 * Created by buganini on 20/02/18.
 */

public class BaseChannelActivity extends BaseActivity {
    protected String mChannelId;
    protected Channel mChannel;

    interface GetChannelCallback{
        void onGetChannel(Channel channel);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();

        String channel_id = intent.getStringExtra("channel");
        if(channel_id!=null){
            mChannelId = channel_id;
        }
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                mChannel = mBinder.getChannelById(mChannelId);
                if(mChannel.enabled!=null && !mChannel.enabled){
                    finish();
                    return;
                }

                processGetChannelCallback();
            }
        });
    }

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
