package im.where.whereim;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
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
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Enchantment;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;

public class ChannelActivity extends BaseActivity implements CoreService.ConnectionStatusCallback {
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

        updateLayout();
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
        getChannel(new GetChannelCallback() {
            @Override
            public void onGetChannel(Channel channel) {
                Intent i = new Intent(Intent.ACTION_SEND);
                i.setType("text/plain");
                i.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.action_invite));
                i.putExtra(Intent.EXTRA_TEXT, getString(R.string.invitation, channel.channel_name)+"\n"+String.format(Config.WHERE_IM_URL, "channel/"+mChannelId));
                startActivity(Intent.createChooser(i, getString(R.string.action_invite)));
            }
        });
    }

    public void moveToMate(Mate mate){
        mChannelMapFragment.moveToMate(mate, true);
        if(mate.latitude!=null){
            mTabLayout.getTabAt(TAB_MAP).select();
        }
    }

    public void moveToEnchantment(Enchantment enchantment){
        mChannelMapFragment.moveToEnchantment(enchantment);
        mTabLayout.getTabAt(TAB_MAP).select();
    }

    public void moveToMaker(Marker marker){
        mChannelMapFragment.moveToMarker(marker, true);
        mTabLayout.getTabAt(TAB_MAP).select();
    }

    public QuadTree.LatLng getMapCenter() {
        return mChannelMapFragment.getMapCenter();
    }

    public void setSearchResult(ArrayList<ChannelSearchFragment.SearchResult> results){
        mChannelMapFragment.setSearchResult(results);
    }

    public void moveToSearchResult(int position){
        mChannelMapFragment.moveToSearchResult(position, true);
        mTabLayout.getTabAt(TAB_MAP).select();
    }

    private ChannelMapFragment mChannelMapFragment = new ChannelGoogleMapFragment();
    private ChannelSearchFragment mChannelSearchFragment = new ChannelGoogleSearchFragment();
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
}
