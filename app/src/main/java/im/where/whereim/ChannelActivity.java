package im.where.whereim;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
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

public class ChannelActivity extends BaseActivity implements CoreService.MateDataReceiver {

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

    private Models.Channel mChannel;

    interface GetChannelCallback{
        public void onGetChannel(Models.Channel channel);
    }

    public Models.Channel getChannel(){
        return mChannel;
    }

    private TextView mChannelTitle;
    private TextView mChannelSubtitle;
    private Switch mEnable;
    private View mEnableLoading;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_channel);

        mChannelTitle = (TextView) findViewById(R.id.channel_title);
        mChannelSubtitle = (TextView) findViewById(R.id.channel_subtitle);
        mEnable = (Switch) findViewById(R.id.enable);
        mEnableLoading = findViewById(R.id.enable_loading);

        mEnable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postBinderTask(new Models.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        binder.toggleChannelEnabled(mChannel);
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

        mTabLayout = (TabLayout) findViewById(R.id.tabs);
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
        i.putExtra(Intent.EXTRA_TEXT, "http://where.im/channel/"+mChannelId);
        startActivity(Intent.createChooser(i, getString(R.string.action_invite)));
    }

    @Override
    public void onMateData(Models.Mate mate) {
        mChannelMapFragment.onMateData(mate);
    }

    @Override
    public void onEnchantmentData(Models.Enchantment enchantment) {
        mChannelMapFragment.onEnchantmentData(enchantment);
    }

    private ChannelMapFragment mChannelMapFragment;
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    if(mChannelMapFragment ==null)
                        mChannelMapFragment = new ChannelMapFragment();
                    return mChannelMapFragment;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Map";
            }
            return null;
        }
    }

    private Runnable mChannelListChangedListener = new Runnable() {
        @Override
        public void run() {
            if(mChannel==null) {
                mEnable.setVisibility(View.GONE);
                mEnableLoading.setVisibility(View.GONE);
                return;
            }
            if(mChannel.user_channel_name==null){
                mChannelTitle.setText(mChannel.channel_name);
                mChannelSubtitle.setVisibility(View.GONE);
            }else{
                mChannelSubtitle.setVisibility(View.VISIBLE);
                mChannelTitle.setText(mChannel.user_channel_name);
                mChannelSubtitle.setText(mChannel.channel_name);
            }
            Boolean enable = mChannel.enable;
            if(enable==null){
                mEnable.setVisibility(View.GONE);
                mEnableLoading.setVisibility(View.VISIBLE);
            }else{
                mEnable.setVisibility(View.VISIBLE);
                mEnableLoading.setVisibility(View.GONE);
                mEnable.setChecked(enable);
            }
        }
    };

    String mChannelId;
    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        super.onServiceConnected(name, service);
        String channel_id = getIntent().getStringExtra("channel");
        if(channel_id!=null){
            mChannelId = channel_id;
        }
        mBinder.addChannelListChangedListener(mChannelListChangedListener);

        mChannel = mBinder.getChannelById(mChannelId);
        if(!mBinder.openChannel(mChannel, ChannelActivity.this)){
            finish();
            return;
        }
        mTabLayout.setupWithViewPager(mViewPager);
    }

    @Override
    protected void onPause() {
        if(mBinder!=null) {
            mBinder.removeChannelListChangedListener(mChannelListChangedListener);
            mBinder.closeChannel(mChannel, ChannelActivity.this);
        }
        super.onPause();
    }
}
