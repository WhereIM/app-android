package im.where.whereim;

import android.Manifest;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class ChannelListActivity extends BaseActivity {
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
                if(channel.user_channel_name !=null){
                    mTitle.setText(channel.user_channel_name);
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

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View dialog_view = LayoutInflater.from(ChannelListActivity.this).inflate(R.layout.dialog_channel_create,  null);
                final EditText et_channel_name = (EditText) dialog_view.findViewById(R.id.channel_name);
                final EditText et_mate_name = (EditText) dialog_view.findViewById(R.id.mate_name);
                new AlertDialog.Builder(ChannelListActivity.this)
                        .setTitle(R.string.create_channel)
                        .setView(dialog_view)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String channel_name = et_channel_name.getText().toString();
                                String mate_name = et_mate_name.getText().toString();
                                mBinder.createChannel(channel_name, mate_name);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
            }
        });

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
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        if(getBinder().getClientId()==null){
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
        mBinder.addChannelListChangedListener(mChannelListChangedListener);
    }

    @Override
    protected void onPause() {
        if(mBinder!=null) {
            mBinder.removeChannelListChangedListener(mChannelListChangedListener);
        }
        super.onPause();
    }
}
