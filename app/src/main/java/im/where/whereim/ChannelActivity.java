package im.where.whereim;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

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
    private final static int TAB_MAP = 0;
    private final static int TAB_SEARCH = 1;
    private final static int TAB_MESSAGE = 2;
    private final static int TAB_MARKER = 3;
    private final static int TAB_ENCHANTMENT = 4;

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private TabLayout mTabLayout;

    private Handler mHandler = new Handler();

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        mConnectionStatus.setVisibility(connected?View.GONE:View.VISIBLE);
    }

    private View mContentRoot;
    private View mCover;
    private ImageView mActiveChannelPointer;
    private View mActiveChannelDesc;
    private ImageView mInvitePointer;
    private View mInviteDesc;

    private View mConnectionStatus;
    private TextView mChannelTitle;
    private TextView mChannelSubtitle;
    private Switch mActive;
    private View mEnableLoading;

    private int mReady = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_channel);

        mContentRoot = findViewById(R.id.content_root);
        mCover = findViewById(R.id.cover);
        mActiveChannelPointer = (ImageView) findViewById(R.id.toggle_channel_pointer);
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

        mInvitePointer = (ImageView) findViewById(R.id.invite_pointer);
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

        mConnectionStatus = findViewById(R.id.connection_status);

        mChannelTitle = (TextView) findViewById(R.id.channel_title);
        mChannelSubtitle = (TextView) findViewById(R.id.channel_subtitle);
        mActive = (Switch) findViewById(R.id.enable);
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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);

        toolbar.findViewById(R.id.list).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(ChannelActivity.this, ChannelListActivity.class);
                startActivity(intent);
            }
        });


        mTabLayout = (TabLayout) findViewById(R.id.tabs);

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


        switch(Config.getMapProvider(ChannelActivity.this)){
            case GOOGLE:
                mChannelMapFragment = new ChannelGoogleMapFragment();
                mChannelSearchFragment = new ChannelGoogleSearchFragment();
                break;
            case MAPBOX:
                mChannelMapFragment = new ChannelMapboxFragment();
                mChannelSearchFragment = new ChannelMapboxSearchFragment();
                break;
        }

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

                // Create the adapter that will return a fragment for each of the three
                // primary sections of the activity.
                mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

                // Set up the ViewPager with the sections adapter.
                mViewPager = (ViewPager) findViewById(R.id.container);
                mViewPager.setAdapter(mSectionsPagerAdapter);
                mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getCount());
                mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
                    @Override
                    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

                    }

                    @Override
                    public void onPageSelected(int position) {
                        View view = getCurrentFocus();
                        if (view != null) {
                            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                        }
                    }

                    @Override
                    public void onPageScrollStateChanged(int state) {

                    }
                });

                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        Intent intent = getIntent();

                        mTabLayout.setupWithViewPager(mViewPager);

                        String tab = intent.getStringExtra("tab");
                        if(tab != null){
                            switch (tab){
                                case "map":
                                    mTabLayout.getTabAt(TAB_MAP).select();
                                    break;
                                case "message":
                                    mTabLayout.getTabAt(TAB_MESSAGE).select();
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
    protected void onChannelChanged() {
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
    }

    private void checkTips() {
        if(mReady<2){
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
    public boolean onCreateOptionsMenu(final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_channel, menu);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final View invite = findViewById(R.id.action_invite);

                int[] rootOrig = new int[2];
                mContentRoot.getLocationInWindow(rootOrig);

                int[] ivOrig = new int[2];
                invite.getLocationInWindow(ivOrig);

                RelativeLayout.LayoutParams params;
                params = (RelativeLayout.LayoutParams) mInvitePointer.getLayoutParams();
                params.topMargin = ivOrig[1] - rootOrig[1] + invite.getHeight() - 35;
                params.leftMargin = ivOrig[0] - rootOrig[0] - mInvitePointer.getDrawable().getIntrinsicWidth() + 35;
                mInvitePointer.setLayoutParams(params);

                mReady += 1;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        checkTips();
                    }
                });
            }
        });
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
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
        mTabLayout.getTabAt(TAB_MESSAGE).select();
    }

    @Override
    public void moveToPin(QuadTree.LatLng latLng) {
        mChannelMapFragment.moveToPin(latLng);
        mTabLayout.getTabAt(TAB_MAP).select();
    }

    @Override
    public void onMateData(Mate mate, boolean focus) {
        mChannelMapFragment.onMateData(mate, focus);
    }

    @Override
    public void moveToMate(Mate mate, boolean focus) {
        mChannelMapFragment.moveToMate(mate, focus);
        if(mate.latitude!=null){
            mTabLayout.getTabAt(TAB_MAP).select();
        }
    }

    @Override
    public void onEnchantmentData(Enchantment enchantment) {
        mChannelMapFragment.onEnchantmentData(enchantment);
    }

    @Override
    public void moveToEnchantment(Enchantment enchantment) {
        mChannelMapFragment.moveToEnchantment(enchantment);
        mTabLayout.getTabAt(TAB_MAP).select();
    }

    @Override
    public void onMarkerData(Marker marker, boolean focus) {
        mChannelMapFragment.onMarkerData(marker, focus);
    }

    @Override
    public void moveToMarker(Marker marker, boolean focus) {
        mChannelMapFragment.moveToMarker(marker, focus);
        mTabLayout.getTabAt(TAB_MAP).select();

    }

    @Override
    public void editEnchantment(Enchantment enchantment) {
        mChannelMapFragment.editEnchantment(enchantment);
        mTabLayout.getTabAt(TAB_MAP).select();
    }

    @Override
    public void editMarker(Marker marker) {
        mChannelMapFragment.editMarker(marker);
        mTabLayout.getTabAt(TAB_MAP).select();
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
        mTabLayout.getTabAt(TAB_MAP).select();
    }

    @Override
    public void onMapAd(HashMap<String, Ad> ads) {
        mChannelMapFragment.onMapAd(ads);
    }

    private ChannelMapFragment mChannelMapFragment;
    private ChannelSearchFragment mChannelSearchFragment;
    private ChannelMessengerFragment mChannelMessengerFragment = new ChannelMessengerFragment();
    private ChannelMarkerFragment mChannelMarkerFragment = new ChannelMarkerFragment();
    private ChannelEnchantmentFragment mChannelEnchantmentFragment = new ChannelEnchantmentFragment();
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return mChannelMapFragment;
                case 1:
                    return mChannelSearchFragment;
                case 2:
                    return mChannelMessengerFragment;
                case 3:
                    return mChannelMarkerFragment;
                case 4:
                    return mChannelEnchantmentFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: // map
                    return "\uD83C\uDF0F️";
                case 1: // search
                    return "\uD83D\uDD0D";
                case 2: // messenger
                    return "\uD83D\uDCAC";
                case 3: // marker
                    return "\uD83D\uDEA9";
                case 4: // enchantment
                    return "⭕";
            }
            return null;
        }
    }

    private Runnable mChannelListChangedListener = new Runnable() {
        @Override
        public void run() {
            if(mChannel==null) {
                mActive.setVisibility(View.GONE);
                mEnableLoading.setVisibility(View.GONE);
                return;
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
