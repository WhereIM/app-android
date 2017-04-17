package im.where.whereim;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import im.where.whereim.models.Channel;
import im.where.whereim.models.Enchantment;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;

public class ChannelActivity extends BaseActivity implements CoreService.ConnectionStatusCallback {
    private final static int TAB_MAP = 0;
    private final static int TAB_MESSAGE = 1;

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

    private Channel mChannel;

    private Handler mHandler = new Handler();

    @Override
    public void onConnectionStatusChanged(boolean connected) {
        mConnectionStatus.setVisibility(connected?View.GONE:View.VISIBLE);
    }

    interface GetChannelCallback{
        public void onGetChannel(Channel channel);
    }

    private final List<GetChannelCallback> mGetChannelCallback = new ArrayList<>();
    public void getChannel(GetChannelCallback callback){
        synchronized (mGetChannelCallback) {
            mGetChannelCallback.add(callback);
        }
        processGetChannelCallback();
    }

    private void processGetChannelCallback(){
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

    private View mConnectionStatus;
    private TextView mChannelTitle;
    private TextView mChannelSubtitle;
    private Switch mActive;
    private View mEnableLoading;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_channel);

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
                        binder.toggleChannelActive(mChannel);
                    }
                });
            }
        });

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(null);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(mSectionsPagerAdapter.getCount());

        mTabLayout = (TabLayout) findViewById(R.id.tabs);

        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                Intent intent = getIntent();

                String channel_id = intent.getStringExtra("channel");
                if(channel_id!=null){
                    mChannelId = channel_id;
                }

                mChannel = mBinder.getChannelById(mChannelId);
                if(mChannel.enabled!=null && !mChannel.enabled){
                    finish();
                    return;
                }

                processGetChannelCallback();
                mTabLayout.setupWithViewPager(mViewPager);

                String tab = intent.getStringExtra("tab");
                if(tab != null){
                    switch (tab){
                        case "message":
                            mTabLayout.getTabAt(TAB_MESSAGE).select();
                            break;
                    }
                }
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_channel, menu);
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
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.action_invite));
        i.putExtra(Intent.EXTRA_TEXT, "https://where.im/channel/"+mChannelId);
        startActivity(Intent.createChooser(i, getString(R.string.action_invite)));
    }

    public void moveToMate(Mate mate){
        if(mate.latitude==null){
            return;
        }
        mChannelMapFragment.moveToMate(mate);
        mTabLayout.getTabAt(TAB_MAP).select();
    }

    public void moveToEnchantment(Enchantment enchantment){
        mChannelMapFragment.moveToEnchantment(enchantment);
        mTabLayout.getTabAt(TAB_MAP).select();
    }

    public void moveToMaker(Marker marker){
        mChannelMapFragment.moveToMarker(marker);
        mTabLayout.getTabAt(TAB_MAP).select();
    }

    private ChannelMapFragment mChannelMapFragment;
    private ChannelMessengerFragment mChannelMessengerFragment;
    private ChannelMarkerFragment mChannelMarkerFragment;
    private ChannelEnchantmentFragment mChannelEnchantmentFragment;
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    if(mChannelMapFragment ==null) {
                        mChannelMapFragment = new ChannelGoogleMapFragment();
                        processMapRunnable();
                    }
                    return mChannelMapFragment;
                case 1:
                    if(mChannelMessengerFragment ==null)
                        mChannelMessengerFragment = new ChannelMessengerFragment();
                    return mChannelMessengerFragment;
                case 2:
                    if(mChannelMarkerFragment ==null)
                        mChannelMarkerFragment = new ChannelMarkerFragment();
                    return mChannelMarkerFragment;
                case 3:
                    if(mChannelEnchantmentFragment ==null)
                        mChannelEnchantmentFragment = new ChannelEnchantmentFragment();
                    return mChannelEnchantmentFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            return 4;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: // map
                    return "\uD83C\uDF0F️";
                case 1: // messenger
                    return "\uD83D\uDCAC";
                case 2: // marker
                    return "\uD83D\uDEA9";
                case 3: // enchantment
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
        }
    };

    String mChannelId;

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        mBinder.setActivity(this);
        mBinder.addChannelListChangedListener(mChannelListChangedListener);
        mBinder.addConnectionStatusChangedListener(this);
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

    private interface MapFragmentCallback{
        public void onMapFragmentReady(CoreService.MapDataReceiver fragment);
    }
    private List<MapFragmentCallback> mPendingMapRunnable = new ArrayList<>();
    private void postMap(MapFragmentCallback r){
        synchronized (mPendingMapRunnable) {
            mPendingMapRunnable.add(r);
        }
        processMapRunnable();
    }

    private void processMapRunnable(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if(mChannelMapFragment ==null){
                    return;
                }
                synchronized (mPendingMapRunnable) {
                    while(mPendingMapRunnable.size()>0){
                        MapFragmentCallback r = mPendingMapRunnable.remove(0);
                        r.onMapFragmentReady(mChannelMapFragment);
                    }
                }
            }
        });
    }
}
