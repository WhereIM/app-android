package im.where.whereim;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import im.where.whereim.dialogs.DialogChannelJoin;
import im.where.whereim.dialogs.DialogChannelNew;
import im.where.whereim.dialogs.DialogCreateChannel;
import im.where.whereim.models.Channel;
import im.where.whereim.models.POI;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;

public class ChannelListActivity extends BaseActivity implements CoreService.ConnectionStatusCallback {
    public final static int REQUEST_PENDING_POI = 0;
    public final static int REQUEST_QR_CODE = 1;

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
            View mUnread;
            TextView mTitle;
            TextView mSubtitle;
            Switch mEnable;
            View mLoading;
            View mLoadingSwitch;

            public ViewHolder(View view) {
                mUnread = view.findViewById(R.id.unread);
                mTitle = (TextView) view.findViewById(R.id.title);
                mSubtitle = (TextView) view.findViewById(R.id.subtitle);
                mEnable = (Switch) view.findViewById(R.id.enable);
                mLoading = view.findViewById(R.id.loading);
                mEnable.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mBinder.toggleChannelActive(mChannel);
                    }
                });
            }

            public void setItem(Channel channel){
                mChannel = channel;
                mUnread.setVisibility(mChannel.enabled!=null && mChannel.enabled && mChannel.unread ? View.VISIBLE : View.INVISIBLE);
                if(channel.user_channel_name !=null && !channel.user_channel_name.isEmpty()){
                    mSubtitle.setVisibility(View.VISIBLE);
                    mTitle.setText(channel.user_channel_name);
                    mSubtitle.setText(channel.channel_name);
                } else {
                    mTitle.setText(channel.channel_name);
                    mSubtitle.setVisibility(View.GONE);
                }
                if(channel.enabled!=null && channel.enabled) {
                    if(channel.active == null) {
                        mLoading.setVisibility(View.VISIBLE);
                        mEnable.setVisibility(View.GONE);
                    } else {
                        mLoading.setVisibility(View.GONE);
                        mEnable.setVisibility(View.VISIBLE);
                        mEnable.setChecked(channel.active);
                    }
                } else {
                    mLoading.setVisibility(View.GONE);
                    mEnable.setVisibility(View.GONE);
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

            if(position==0){
                final View sw = vh.mEnable;
                final View cell = view;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        int[] rootOrig = new int[2];
                        mViewRoot.getLocationInWindow(rootOrig);

                        int[] swOrig = new int[2];
                        sw.getLocationInWindow(swOrig);
                        RelativeLayout.LayoutParams params;
                        params = (RelativeLayout.LayoutParams) mActiveChannelPointer.getLayoutParams();
                        params.topMargin = swOrig[1] - rootOrig[1] + sw.getHeight();
                        params.leftMargin = swOrig[0] - rootOrig[0] - mActiveChannelPointer.getDrawable().getIntrinsicWidth();
                        mActiveChannelPointer.setLayoutParams(params);

                        int[] cellOrig = new int[2];
                        cell.getLocationInWindow(cellOrig);
                        params = (RelativeLayout.LayoutParams) mEnterChannelPointer.getLayoutParams();
                        params.topMargin = cellOrig[1] - rootOrig[1] + cell.getHeight() - 45;
                        params.leftMargin = cellOrig[0] - rootOrig[0] + (cell.getWidth() - mEnterChannelPointer.getDrawable().getIntrinsicWidth())/2;
                        mEnterChannelPointer.setLayoutParams(params);

                        checkTips();
                    }
                });
            }

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
            public void onBinderReady(final CoreService.CoreBinder binder) {
            new DialogChannelJoin(ChannelListActivity.this, binder.getUserName(), new DialogChannelJoin.Callback() {
                @Override
                public void onDone(String mate_name) {
                    binder.joinChannel(channel_id, null /*channel_alias*/, mate_name);
                }
            });
            }
        });
    }

    private View mViewRoot;
    private View mCover;
    private ImageView mNewChannelPointer;
    private View mNewChannelPointerDesc;
    private ImageView mActiveChannelPointer;
    private View mActiveChannelPointerDesc;
    private ImageView mEnterChannelPointer;
    private View mEnterChannelPointerDesc;

    private POI pendingPOI;
    private View mPendingPanel;
    private TextView mPendingTitle;
    private TextView mPendingDesc;
    private View mConnectionStatus;
    private FloatingActionButton mNewChannel;
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

        mViewRoot = findViewById(R.id.root);
        mCover = findViewById(R.id.cover);

        mNewChannelPointer = (ImageView) findViewById(R.id.new_channel_pointer);
        mNewChannelPointerDesc = findViewById(R.id.new_channel_pointer_desc);
        findViewById(R.id.new_channel_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = ChannelListActivity.this.getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(Key.TIP_NEW_CHANNEL, true);
                editor.apply();
                checkTips();
            }
        });

        mActiveChannelPointer = (ImageView) findViewById(R.id.active_channel_pointer);
        mActiveChannelPointerDesc = findViewById(R.id.active_channel_pointer_desc);
        findViewById(R.id.active_channel_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = ChannelListActivity.this.getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(Key.TIP_ACTIVE_CHANNEL, true);
                editor.apply();
                checkTips();
            }
        });

        mEnterChannelPointer = (ImageView) findViewById(R.id.enter_channel_pointer);
        mEnterChannelPointerDesc = findViewById(R.id.enter_channel_pointer_desc);
        findViewById(R.id.enter_channel_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = ChannelListActivity.this.getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(Key.TIP_ENTER_CHANNEL, true);
                editor.apply();
                checkTips();
            }
        });

        mPendingPanel = findViewById(R.id.pending);
        mPendingTitle = (TextView) findViewById(R.id.title);
        mPendingDesc = (TextView) findViewById(R.id.desc);

        mConnectionStatus = findViewById(R.id.connection_status);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        mListView = (ListView) findViewById(R.id.channel_list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Channel channel = (Channel) mAdapter.getItem(position);
                if(channel.enabled!=null && channel.enabled){
                    Intent intent = new Intent(ChannelListActivity.this, ChannelActivity.class);
                    intent.putExtra("channel", channel.id);
                    if(pendingPOI != null){
                        intent.putExtra(Key.LATITUDE, pendingPOI.latitude);
                        intent.putExtra(Key.LONGITUDE, pendingPOI.longitude);
                        intent.putExtra(Key.NAME, pendingPOI.name);
                        intent.putExtra(Key.PENDING_POI, true);
                        pendingPOI = null;
                    }
                    startActivity(intent);
                }
            }
        });
        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                startActionMode(new ActionMode.Callback() {
                    private final static int ACTION_EDIT = 0;
                    private final static int ACTION_TOGGLE_ENABLED = 1;
                    private final static int ACTION_DELETE = 2;
                    private Channel channel;

                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        channel = (Channel) mAdapter.getItem(position);
                        menu.add(0, ACTION_EDIT, 0, "✏️");
                        if(channel.enabled !=null && !channel.enabled)
                            menu.add(0, ACTION_TOGGLE_ENABLED, 0, "\uD83D\uDD13");
                        if(channel.enabled !=null && channel.enabled)
                            menu.add(0, ACTION_TOGGLE_ENABLED, 0, "\uD83D\uDD12");
                        menu.add(0, ACTION_DELETE, 0, "❌️");
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        mode.finish();
                        switch(item.getItemId()){
                            case ACTION_EDIT:
                                final View dialog_view = LayoutInflater.from(ChannelListActivity.this).inflate(R.layout.dialog_channel_edit,  null);
                                final EditText et_channel_name = (EditText) dialog_view.findViewById(R.id.channel_name);
                                final EditText et_user_channel_name = (EditText) dialog_view.findViewById(R.id.user_channel_name);
                                et_channel_name.setText(channel.channel_name);
                                et_user_channel_name.setText(channel.user_channel_name);
                                new AlertDialog.Builder(ChannelListActivity.this)
                                        .setTitle(R.string.edit_channel)
                                        .setView(dialog_view)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                final String channel_name = et_channel_name.getText().toString();
                                                final String user_channel_name = et_user_channel_name.getText().toString();
                                                postBinderTask(new CoreService.BinderTask() {
                                                    @Override
                                                    public void onBinderReady(CoreService.CoreBinder binder) {
                                                        binder.editChannel(channel, channel_name, user_channel_name);
                                                    }
                                                });
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        }).show();
                                return true;
                            case ACTION_TOGGLE_ENABLED:
                                mBinder.toggleChannelEnabled(channel);
                                return true;
                            case ACTION_DELETE:
                                new AlertDialog.Builder(ChannelListActivity.this)
                                        .setTitle(R.string.quit_channel)
                                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                postBinderTask(new CoreService.BinderTask() {
                                                    @Override
                                                    public void onBinderReady(CoreService.CoreBinder binder) {
                                                        binder.deleteChannel(channel);
                                                    }
                                                });
                                            }
                                        })
                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {

                                            }
                                        }).show();
                                return true;
                            default:
                                return false;
                        }
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {
                    }
                });
                return true;
            }
        });

        mNewChannel = (FloatingActionButton) findViewById(R.id.fab);
        mNewChannel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new DialogChannelNew(ChannelListActivity.this, new DialogChannelNew.Callback() {
                    @Override
                    public void onSelectJoinByQrCode() {
                        Intent intent = new Intent(ChannelListActivity.this, ScannerActivity.class);
                        startActivityForResult(intent, REQUEST_QR_CODE);
                    }

                    @Override
                    public void onSelectCreateChannel() {
                        new DialogCreateChannel(ChannelListActivity.this, mBinder.getUserName(), new DialogCreateChannel.Callback() {

                            @Override
                            public void onDone(String channel_name, String mate_name) {
                                mBinder.createChannel(channel_name, mate_name);
                            }
                        });
                    }
                });
            }
        });
    }

    private void checkTips() {
        mCover.setVisibility(View.GONE);
        SharedPreferences sp = ChannelListActivity.this.getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        if(!sp.getBoolean(Key.TIP_NEW_CHANNEL, false)) {
            mNewChannelPointer.setVisibility(View.VISIBLE);
            mNewChannelPointerDesc.setVisibility(View.VISIBLE);
            mCover.setVisibility(View.VISIBLE);
            return;
        } else {
            mNewChannelPointer.setVisibility(View.GONE);
            mNewChannelPointerDesc.setVisibility(View.GONE);
        }

        if(mAdapter.getCount()>0){
            if(!sp.getBoolean(Key.TIP_ACTIVE_CHANNEL, false)) {
                mActiveChannelPointer.setVisibility(View.VISIBLE);
                mActiveChannelPointerDesc.setVisibility(View.VISIBLE);
                mCover.setVisibility(View.VISIBLE);
                return;
            } else {
                mActiveChannelPointer.setVisibility(View.GONE);
                mActiveChannelPointerDesc.setVisibility(View.GONE);
            }

            if(!sp.getBoolean(Key.TIP_ENTER_CHANNEL, false)) {
                mEnterChannelPointer.setVisibility(View.VISIBLE);
                mEnterChannelPointerDesc.setVisibility(View.VISIBLE);
                mCover.setVisibility(View.VISIBLE);
                return;
            } else {
                mEnterChannelPointer.setVisibility(View.GONE);
                mEnterChannelPointerDesc.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_channel_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
            return true;
        }
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
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
                            processLink(uri);
                        }
                    } else {
                        Log.i("WhereIM", error.getMessage());
                    }
                }
            }, this.getIntent().getData(), this);

            if(pendingPOI==null){
                mPendingPanel.setVisibility(View.GONE);
            }else{
                mPendingPanel.setVisibility(View.VISIBLE);
                if (pendingPOI.name!=null) {
                    mPendingTitle.setText(pendingPOI.name);
                    mPendingTitle.setVisibility(View.VISIBLE);
                } else {
                    mPendingTitle.setVisibility(View.GONE);
                }
                mPendingDesc.setText(String.format(Locale.ENGLISH, "%f,%f", pendingPOI.latitude, pendingPOI.longitude));
            }
        }
        mBinder.setActivity(this);
        mBinder.addChannelListChangedListener(mChannelListChangedListener);
        mBinder.addConnectionStatusChangedListener(this);

        mNewChannel.post(new Runnable() {
            @Override
            public void run() {
                int[] rootOrig = new int[2];
                mViewRoot.getLocationInWindow(rootOrig);

                int[] ncOrig = new int[2];
                mNewChannel.getLocationInWindow(ncOrig);

                RelativeLayout.LayoutParams params;
                params = (RelativeLayout.LayoutParams) mNewChannelPointer.getLayoutParams();
                params.bottomMargin = mViewRoot.getHeight() - (ncOrig[1] - rootOrig[1]);
                params.rightMargin = mViewRoot.getWidth() - (ncOrig[0] - rootOrig[0]);
                mNewChannelPointer.setLayoutParams(params);

                checkTips();
            }
        });
    }

    private void processLink(String link){
        Pattern mPatternChannelJoin = Pattern.compile("^channel/([A-Fa-f0-9]{32})$");
        Matcher m;
        m = mPatternChannelJoin.matcher(link);
        if(m.matches()){
            channelJoin(m.group(1));
            return;
        }
        Pattern mPatternHere = Pattern.compile("^here/(-?[0-9.]+)/(-?[0-9.]+)(?:/(.*))?$");
        m = mPatternHere.matcher(link);
        if(m.matches()){
            Intent intent = new Intent(ChannelListActivity.this, PoiViewerActivity.class);
            intent.putExtra(Key.LATITUDE, Double.valueOf(m.group(1)));
            intent.putExtra(Key.LONGITUDE, Double.valueOf(m.group(2)));
            intent.putExtra(Key.NAME, m.group(3));
            startActivityForResult(intent, 0);
        }
    }

    @Override
    protected void onPause() {
        if(mBinder!=null) {
            mBinder.setActivity(null);
            mBinder.removeChannelListChangedListener(mChannelListChangedListener);
            mBinder.removeConnectionStatusChangedListener(this);
        }
        super.onPause();
    }

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        mConnectionStatus.setVisibility(connected?View.GONE:View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_PENDING_POI:
                if (resultCode == 1) {
                    pendingPOI = new POI();
                    pendingPOI.latitude = data.getDoubleExtra(Key.LATITUDE, 0);
                    pendingPOI.longitude = data.getDoubleExtra(Key.LONGITUDE, 0);
                    pendingPOI.name = data.getStringExtra(Key.NAME);
                }
                break;
            case REQUEST_QR_CODE:
                if (resultCode == 1) {
                    String link = data.getStringExtra(Key.LINK);
                    if(link!=null){
                        processLink(link);
                    }
                }
                break;
        }
    }
}
