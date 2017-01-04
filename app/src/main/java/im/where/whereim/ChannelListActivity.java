package im.where.whereim;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class ChannelListActivity extends AppCompatActivity {

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
            if(mBinder==null)
                return;
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
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(ChannelListActivity.this, ChannelActivity.class);
                        intent.putExtra("channel", mChannel.id);
                        startActivity(intent);
                    }
                });
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
                view = LayoutInflater.from(ChannelListActivity.this).inflate(R.layout.channel_list_item, null);
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

        if (ContextCompat.checkSelfPermission(ChannelListActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(ChannelListActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                Toast.makeText(ChannelListActivity.this, R.string.permission_rationale, Toast.LENGTH_SHORT).show();

            } else {
                ActivityCompat.requestPermissions(ChannelListActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        0);
            }
        }

        setContentView(R.layout.activity_channel_list);

        mListView = (ListView) findViewById(R.id.channel_list);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if(mBinder!=null){
                        mBinder.checkLocationService();
                    }
                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, CoreService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        if(mBinder!=null) {
            mBinder.removeChannelListChangedListener(mChannelListChangedListener);
        }
        unbindService(mConnection);
        mBinder = null;
        super.onPause();
    }

}
