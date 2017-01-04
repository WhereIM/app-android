package im.where.whereim;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mBinder = (CoreService.CoreBinder) service;
            mBinder.addChannelListChangedListener(mChannelListChangedListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

        }
    };

    private CoreService.CoreBinder mBinder;
    private List<Models.Channel> mChannelList;
    private ListView mListView;
    private Runnable mChannelListChangedListener = new Runnable() {
        @Override
        public void run() {
            mChannelList = mBinder.getChannelList();
            mAdapter.notifyDataSetChanged();
        }
    };

    private BaseAdapter mAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            if(mChannelList == null)
                return 0;
            return mChannelList.size();
        }

        @Override
        public Object getItem(int position) {
            return mChannelList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        class ViewHolder{
            Models.Channel mChannel;
            TextView mTitle;
            TextView mSubtitle;
            Switch mEnable;
            View mLoading;

            public ViewHolder(View view) {
                mTitle = (TextView) view.findViewById(R.id.title);
                mSubtitle = (TextView) view.findViewById(R.id.subtitle);
                mEnable = (Switch) view.findViewById(R.id.enable);
                mLoading = view.findViewById(R.id.loading);
                mEnable.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBinder.toggleChannelEnabled(mChannel);
                    }
                });
            }

            public void setItem(Models.Channel channel){
                mChannel = channel;
                if(channel.name!=null){
                    mTitle.setText(channel.name);
                    mSubtitle.setText(channel.channel_name);
                }else{
                    mTitle.setText(channel.channel_name);
                    mSubtitle.setText("");
                }
                if(channel.enable==null){
                    mLoading.setVisibility(View.VISIBLE);
                    mEnable.setVisibility(View.GONE);
                }else{
                    mLoading.setVisibility(View.GONE);
                    mEnable.setVisibility(View.VISIBLE);
                    mEnable.setChecked(channel.enable);
                }
            }
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder vh;
            if(view==null){
                view = LayoutInflater.from(HomeActivity.this).inflate(R.layout.channel_list_item, null);
                vh = new ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ViewHolder) view.getTag();
            }

            Models.Channel channel = (Models.Channel) getItem(position);
            vh.setItem(channel);
            return view;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mListView = (ListView) findViewById(R.id.channel_list);
        mListView.setAdapter(mAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, CoreService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mBinder!=null) {
            mBinder.removeChannelListChangedListener(mChannelListChangedListener);
        }
        unbindService(mConnection);
    }

}
