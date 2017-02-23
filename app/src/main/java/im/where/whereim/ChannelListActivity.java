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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.where.whereim.models.Channel;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;

public class ChannelListActivity extends BaseActivity {
    private List<Channel> mChannelList;
    private ListView mListView;
    private Runnable mChannelListChangedListener = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(final CoreService.CoreBinder binder) {
                    postUITask(new Runnable(){

                        @Override
                        public void run() {
                            mChannelList = binder.getChannelList();
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
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
            Channel mChannel;
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

            public void setItem(Channel channel){
                mChannel = channel;
                if(channel.user_channel_name !=null && !channel.user_channel_name.isEmpty()){
                    mSubtitle.setVisibility(View.VISIBLE);
                    mTitle.setText(channel.user_channel_name);
                    mSubtitle.setText(channel.channel_name);
                }else{
                    mTitle.setText(channel.channel_name);
                    mSubtitle.setVisibility(View.GONE);
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
                view = LayoutInflater.from(ChannelListActivity.this).inflate(R.layout.channel_item, null);
                vh = new ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ViewHolder) view.getTag();
            }

            Channel channel = (Channel) getItem(position);
            vh.setItem(channel);
            return view;
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        this.setIntent(intent);
    }

    private void channelJoin(final String channel_id){
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                final View dialog_view = LayoutInflater.from(ChannelListActivity.this).inflate(R.layout.dialog_channel_join,  null);
                final TextView tv_channel_name = (TextView) dialog_view.findViewById(R.id.channel_name);
//        final EditText et_channel_alias = (EditText) dialog_view.findViewById(R.id.channel_alias);
                final EditText et_mate_name = (EditText) dialog_view.findViewById(R.id.mate_name);
                et_mate_name.setText(binder.getUserName());
                new AlertDialog.Builder(ChannelListActivity.this)
                        .setTitle(R.string.join_channel)
                        .setView(dialog_view)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
//                        final String channel_alias = et_channel_alias.getText().toString();
                                final String mate_name = et_mate_name.getText().toString();
                                postBinderTask(new CoreService.BinderTask() {
                                    @Override
                                    public void onBinderReady(CoreService.CoreBinder binder) {
                                        binder.joinChannel(channel_id, null /*channel_alias*/, mate_name);
                                    }
                                });
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        }).show();
            }
        });
    }

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
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Channel channel = (Channel) mAdapter.getItem(position);
                Intent intent = new Intent(ChannelListActivity.this, ChannelActivity.class);
                intent.putExtra("channel", channel.id);
                startActivity(intent);
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final View dialog_view = LayoutInflater.from(ChannelListActivity.this).inflate(R.layout.dialog_channel_create,  null);
                final EditText et_channel_name = (EditText) dialog_view.findViewById(R.id.channel_name);
                final EditText et_mate_name = (EditText) dialog_view.findViewById(R.id.mate_name);
                et_mate_name.setText(mBinder.getUserName());
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
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_channel_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_quit) {
            stopService(new Intent(this, CoreService.class));
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    postBinderTask(new CoreService.BinderTask() {
                        @Override
                        public void onBinderReady(CoreService.CoreBinder binder) {
                            binder.checkLocationService();
                        }
                    });
                }
                return;
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        if(getBinder().getClientId()==null){
            Log.e("ChannelListActivity", "start LoginActivity");
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }else{
            Branch branch = Branch.getInstance();
            branch.initSession(new Branch.BranchReferralInitListener(){
                @Override
                public void onInitFinished(JSONObject referringParams, BranchError error) {
                    if (error == null) {
                        String uri = Util.JsonOptNullableString(referringParams, "$deeplink_path", null);
                        if(uri!=null){
                            Pattern mPatternChannelJoin = Pattern.compile("^channel/([A-Fa-f0-9]{32})$");
                            Matcher m = mPatternChannelJoin.matcher(uri);
                            if(m.matches()){
                                channelJoin(m.group(1));
                                return;
                            }
                        }
                    } else {
                        Log.i("WhereIM", error.getMessage());
                    }
                }
            }, this.getIntent().getData(), this);
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
