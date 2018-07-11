package im.where.whereim;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import im.where.whereim.dialogs.DialogChannelInvite;
import im.where.whereim.dialogs.DialogLocationServicePermissionRationale;
import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Ad;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Enchantment;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;
import im.where.whereim.models.POI;

public class ChannelActivity extends BaseChannelActivity implements CoreService.ConnectionStatusCallback, CoreService.MapDataDelegate {
    private Handler mHandler = new Handler();

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        mConnectingStatus.setVisibility(connected?View.GONE:View.VISIBLE);
    }

    private DrawerLayout mDrawerLayout;
    private View mContentRoot;
    private View mCover;
    private ImageView mActiveChannelPointer;
    private View mActiveChannelDesc;
    private ImageView mInvitePointer;
    private View mInviteDesc;

    private View mConnectingStatus;
    private TextView mChannelTitle;
    private TextView mChannelSubtitle;
    private Switch mActive;
    private View mEnableLoading;

    private View resizeHandler;
    private FrameLayout mainFrame;
    private FrameLayout auxFrame;

    private int mReady = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_channel);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        resizeHandler = findViewById(R.id.resize_handler);
        mainFrame = findViewById(R.id.main_frame);
        auxFrame = findViewById(R.id.aux_frame);

        resizeHandler.setOnTouchListener(new View.OnTouchListener() {
            private Float y = null;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch(motionEvent.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        y = motionEvent.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if(y != null){
                            ViewGroup.LayoutParams params = auxFrame.getLayoutParams();
                            params.height -= motionEvent.getRawY() - y;
                            auxFrame.setLayoutParams(params);
                            y = motionEvent.getRawY();
                            return true;
                        }else{
                            return false;
                        }
                    case MotionEvent.ACTION_UP:
                        y = null;
                        return true;
                }
                return false;
            }
        });

        mContentRoot = findViewById(R.id.content_frame);
        mCover = findViewById(R.id.cover);
        mActiveChannelPointer = findViewById(R.id.toggle_channel_pointer);
        mActiveChannelDesc = findViewById(R.id.toggle_channel_pointer_desc);
        findViewById(R.id.toggle_channel_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = ChannelActivity.this.getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(Key.TIP_ACTIVE_CHANNEL_2, true);
                editor.apply();
                checkTips();
            }
        });

        mInvitePointer = findViewById(R.id.invite_pointer);
        mInviteDesc = findViewById(R.id.invite_pointer_desc);
        findViewById(R.id.invite_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor editor = ChannelActivity.this.getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                editor.putBoolean(Key.TIP_INVITE_CHANNEL, true);
                editor.apply();
                checkTips();
            }
        });

        mConnectingStatus = findViewById(R.id.connecting_status);

        mChannelTitle = findViewById(R.id.channel_title);
        mChannelSubtitle = findViewById(R.id.channel_subtitle);
        mActive = findViewById(R.id.enable);
        mEnableLoading = findViewById(R.id.enable_loading);

        mActive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        binder.toggleChannelActive(ChannelActivity.this, mChannel);
                    }
                });
            }
        });
        View.OnLongClickListener deactivate = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        binder.deactivateChannel(mChannel);
                    }
                });
                return true;
            }
        };
        mActive.setOnLongClickListener(deactivate);
        mEnableLoading.setOnLongClickListener(deactivate);

        findViewById(R.id.list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mDrawerLayout.openDrawer(Gravity.LEFT);
            }
        });


        updateLayout();

        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                if(mBinder.getClientId()==null){
                    Intent intent = new Intent(ChannelActivity.this, LoginActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

        postChannelReadyTask(new Runnable(){
            @Override
            public void run() {
                if (ContextCompat.checkSelfPermission(ChannelActivity.this,
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    onLocationServiceReady();
                }else{

                    if (ActivityCompat.shouldShowRequestPermissionRationale(ChannelActivity.this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                        new DialogLocationServicePermissionRationale(ChannelActivity.this);

                    } else {
                        ActivityCompat.requestPermissions(ChannelActivity.this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                0);
                    }
                }


                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        Intent intent = getIntent();

                        String tab = intent.getStringExtra("tab");
                        if(tab != null){
                            switch (tab){
                                case "message":
                                    showAux(R.id.message);
                                    break;
                            }
                        }
                    }
                });

                mReady += 1;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        checkTips();
                    }
                });
            }
        });

        showMain(R.id.map);
        showAux(0);
    }

    public void closeDrawer(){
        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    void showMain(int comp){
        switch (comp) {
            case R.id.map:
                if(mChannelMapFragment == null){
                    mChannelMapFragment = ChannelMapFragment.newFragment(this);
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.main_frame, mChannelMapFragment).commit();
                break;
        }
    }

    void showAux(int comp){
        ViewGroup.LayoutParams params;
        boolean resizable = false;
        int height = 0;
        switch (comp) {
            case 0:
                resizable = false;
                if(mChannelActionFragment == null){
                    mChannelActionFragment = new ChannelActionFragment();
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.aux_frame, mChannelActionFragment).commit();
                height = 50;
                break;
            case R.id.search:
                resizable = true;
                if(mChannelSearchFragment == null){
                    mChannelSearchFragment = ChannelSearchFragment.newFragment(this);
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.aux_frame, mChannelSearchFragment).commit();
                height = 240;
                break;
            case R.id.message:
                resizable = true;
                if(mChannelMessengerFragment == null){
                    mChannelMessengerFragment = new ChannelMessengerFragment();
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.aux_frame, mChannelMessengerFragment).commit();
                height = 240;
                break;
            case R.id.marker:
                resizable = true;
                if(mChannelMarkerFragment== null){
                    mChannelMarkerFragment = new ChannelMarkerFragment();
                }
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.aux_frame, mChannelMarkerFragment).commit();
                height = 240;
                break;
        }
        params = auxFrame.getLayoutParams();
        params.height = (int) Util.dp2px(ChannelActivity.this, height);
        auxFrame.setLayoutParams(params);
        resizeHandler.setVisibility(resizable ? View.VISIBLE : View.GONE);
        auxFrame.setVisibility(View.VISIBLE);
    }

    private void onLocationServiceReady(){
        mChannelMapFragment.onLocationServiceReady();
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.checkLocationService();
            }
        });
    }

    @Override
    protected void onChannelChanged(final Channel prevChannel) {
        if(prevChannel != null){
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    binder.closeMap(prevChannel, ChannelActivity.this);
                }
            });
        }
        if(mChannel.user_channel_name==null || mChannel.user_channel_name.isEmpty()){
            mChannelTitle.setText(mChannel.channel_name);
            mChannelSubtitle.setVisibility(View.GONE);
        }else{
            mChannelSubtitle.setVisibility(View.VISIBLE);
            mChannelTitle.setText(mChannel.user_channel_name);
            mChannelSubtitle.setText(mChannel.channel_name);
        }
        Boolean active = mChannel.active;
        if(active==null){
            mActive.setVisibility(View.GONE);
            mEnableLoading.setVisibility(View.VISIBLE);
        }else{
            mActive.setVisibility(View.VISIBLE);
            mEnableLoading.setVisibility(View.GONE);
            mActive.setChecked(active);
        }

        if(mChannelMapFragment != null) {
            mChannelMapFragment.deinitChannel();
            mChannelMapFragment.initChannel();
        }

        if(mChannelMessengerFragment != null) {
            mChannelMessengerFragment.deinitChannel();
            mChannelMessengerFragment.initChannel();
        }

        if(mChannelMarkerFragment != null) {
            mChannelMarkerFragment.deinitChannel();
            mChannelMarkerFragment.initChannel();
        }

        if(mChannelMarkerFragment != null) {
            mChannelEnchantmentFragment.deinitChannel();
            mChannelEnchantmentFragment.initChannel();
        }
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.openMap(mChannel, ChannelActivity.this);
            }
        });
    }

    private void checkTips() {
        if(mReady<1){
            return;
        }
        mCover.setVisibility(View.GONE);
        SharedPreferences sp = ChannelActivity.this.getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);

        if(!sp.getBoolean(Key.TIP_ACTIVE_CHANNEL_2, false)) {
            mActiveChannelPointer.setVisibility(View.VISIBLE);
            mActiveChannelDesc.setVisibility(View.VISIBLE);
            mCover.setVisibility(View.VISIBLE);
            return;
        } else {
            mActiveChannelPointer.setVisibility(View.GONE);
            mActiveChannelDesc.setVisibility(View.GONE);
        }

        if(!sp.getBoolean(Key.TIP_INVITE_CHANNEL, false)) {
            mInvitePointer.setVisibility(View.VISIBLE);
            mInviteDesc.setVisibility(View.VISIBLE);
            mCover.setVisibility(View.VISIBLE);
            return;
        } else {
            mInvitePointer.setVisibility(View.GONE);
            mInviteDesc.setVisibility(View.GONE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateLayout();
    }

    private void updateLayout(){
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        } else {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent channelActivity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_invite) {
            invite_join();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void invite_join(){
        getChannel(new GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                new DialogChannelInvite(ChannelActivity.this, channel);
            }
        });
    }

    public void sendPin(QuadTree.LatLng location){
        mChannelMessengerFragment.pinLocation = location;
    }

    @Override
    public void moveToPin(QuadTree.LatLng latLng) {
        mChannelMapFragment.moveToPin(latLng);
    }

    @Override
    public void onMateData(Mate mate, boolean focus) {
        mChannelMapFragment.onMateData(mate, focus);
    }

    @Override
    public void moveToMate(Mate mate, boolean focus) {
        mChannelMapFragment.moveToMate(mate, focus);
    }

    @Override
    public void onEnchantmentData(Enchantment enchantment) {
        mChannelMapFragment.onEnchantmentData(enchantment);
    }

    @Override
    public void moveToEnchantment(Enchantment enchantment) {
        mChannelMapFragment.moveToEnchantment(enchantment);
    }

    @Override
    public void onMarkerData(Marker marker, boolean focus) {
        mChannelMapFragment.onMarkerData(marker, focus);
    }

    @Override
    public void moveToMarker(Marker marker, boolean focus) {
        mChannelMapFragment.moveToMarker(marker, focus);
    }

    @Override
    public void editEnchantment(Enchantment enchantment) {
        mChannelMapFragment.editEnchantment(enchantment);
    }

    @Override
    public void editMarker(Marker marker) {
        mChannelMapFragment.editMarker(marker);
    }

    public QuadTree.LatLng getMapCenter() {
        return mChannelMapFragment.getMapCenter();
    }

    public void setSearchResult(ArrayList<POI> results){
        mChannelMapFragment.setSearchResult(results);
    }

    @Override
    public void moveToSearchResult(int position, boolean focus) {
        mChannelMapFragment.moveToSearchResult(position, focus);
    }

    @Override
    public void onMapAd(HashMap<String, Ad> ads) {
        mChannelMapFragment.onMapAd(ads);
    }

    private ChannelActionFragment mChannelActionFragment;
    private ChannelMapFragment mChannelMapFragment;
    private ChannelSearchFragment mChannelSearchFragment;
    private ChannelMessengerFragment mChannelMessengerFragment = new ChannelMessengerFragment();
    private ChannelMarkerFragment mChannelMarkerFragment = new ChannelMarkerFragment();
    private ChannelEnchantmentFragment mChannelEnchantmentFragment = new ChannelEnchantmentFragment();

    private Runnable mChannelListChangedListener = new Runnable() {
        @Override
        public void run() {
            if(mChannel==null) {
                mActive.setVisibility(View.GONE);
                mEnableLoading.setVisibility(View.GONE);
                return;
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int[] rootOrig = new int[2];
                    mContentRoot.getLocationInWindow(rootOrig);

                    int[] acOrig = new int[2];
                    mActive.getLocationInWindow(acOrig);

                    RelativeLayout.LayoutParams params;
                    params = (RelativeLayout.LayoutParams) mActiveChannelPointer.getLayoutParams();
                    params.topMargin = acOrig[1] - rootOrig[1] + mActive.getHeight();
                    params.leftMargin = acOrig[0] - rootOrig[0] + (mActive.getWidth() - mActiveChannelPointer.getDrawable().getIntrinsicWidth())/2;
                    mActiveChannelPointer.setLayoutParams(params);
                }
            });
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        postChannelReadyTask(new Runnable() {
            @Override
            public void run() {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        binder.setActivity(ChannelActivity.this);
                        binder.openMap(mChannel, ChannelActivity.this);
                        binder.addChannelListChangedListener(mChannelListChangedListener);
                        binder.addConnectionStatusChangedListener(ChannelActivity.this);
                    }
                });
            }
        });
    }

    @Override
    protected void onPause() {
        if(mBinder!=null) {
            mBinder.setActivity(null);
            mBinder.closeMap(mChannel, ChannelActivity.this);
            mBinder.removeChannelListChangedListener(mChannelListChangedListener);
            mBinder.removeConnectionStatusChangedListener(this);
        }
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onLocationServiceReady();
                }
                return;
            }
        }
    }
}
