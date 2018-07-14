package im.where.whereim;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

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

    private View mConnectingStatus;
    private TextView mChannelTitle;
    private TextView mChannelSubtitle;
    private Switch mActive;
    private View mEnableLoading;

    private View resizeHandler;
    private FrameLayout mainFrame;
    private FrameLayout auxFrame;

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
                                    showAux(AuxComp.MESSAGE);
                                    break;
                            }
                        }
                    }
                });
            }
        });

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setupMapView();
                showAux(AuxComp.TAB);
            }
        });
    }

    public void closeDrawer(){
        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    public void setCrosshair(boolean display){
        mChannelMapFragment.setCrosshair(display);
    }

    enum AuxSize {
        TAB,
        FREE,
        WRAP,
        FULL,
    }
    private AuxSize currentAuxSize = AuxSize.TAB;
    void setAuxSizePolicy(AuxSize size){
        boolean doChange = true;
        ViewGroup.LayoutParams params;
        params = auxFrame.getLayoutParams();
        switch(size){
            case TAB:
                params.height = getResources().getDimensionPixelSize(R.dimen.tab_height);
                break;
            case WRAP:
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                break;
            case FULL:
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                break;
            case FREE:
                params.height = (int) Util.dp2px(ChannelActivity.this, 240);
                if(currentAuxSize == AuxSize.FREE){
                    doChange = false;
                }
                break;
        }
        if(doChange){
            auxFrame.setLayoutParams(params);
        }
        currentAuxSize = size;
    }

    public int getAuxSize(){
        ViewGroup.LayoutParams params = auxFrame.getLayoutParams();
        return params.height;
    }

    public void setAuxSize(int height){
        if(height > mContentRoot.getHeight()){ // this may happen after screen rotation
            return;
        }
        ViewGroup.LayoutParams params = auxFrame.getLayoutParams();
        params.height = height;
        auxFrame.setLayoutParams(params);
    }

    enum AuxComp {
        TAB,
        SEARCH,
        MESSAGE,
        MARKER,
        MATE,
        MARKER_CREATE,
    }
    private AuxComp auxComp = AuxComp.TAB;
    void showAux(AuxComp comp){
        auxComp = comp;
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        switch (comp) {
            case TAB:
                setSearchResult(new ArrayList<POI>());
                if(mChannelActionFragment == null){
                    mChannelActionFragment = new ChannelActionFragment();
                }
                fm.beginTransaction()
                        .replace(R.id.aux_frame, mChannelActionFragment)
                        .commit();
                break;
            case SEARCH:
                if(mChannelSearchFragment == null || mChannelSearchFragment.getProvider() != Config.getMapProvider(this)){
                    mChannelSearchFragment = ChannelSearchFragment.newFragment(this);
                }
                mChannelActionFragment.resetSizePolicy();
                fm.beginTransaction()
                        .replace(R.id.aux_frame, mChannelSearchFragment)
                        .commit();
                break;
            case MESSAGE:
                if(mChannelMessengerFragment == null){
                    mChannelMessengerFragment = new ChannelMessengerFragment();
                }
                mChannelMessengerFragment.resetSizePolicy();
                fm.beginTransaction()
                        .replace(R.id.aux_frame, mChannelMessengerFragment)
                        .commit();
                break;
            case MARKER:
                if(mChannelMarkerFragment== null){
                    mChannelMarkerFragment = new ChannelMarkerFragment();
                }
                mChannelMarkerFragment.resetSizePolicy();
                fm.beginTransaction()
                        .replace(R.id.aux_frame, mChannelMarkerFragment)
                        .commit();
                break;
            case MATE:
                if(mChannelMateFragment== null){
                    mChannelMateFragment = new ChannelMateFragment();
                }
                mChannelMateFragment.resetSizePolicy();
                fm.beginTransaction()
                        .replace(R.id.aux_frame, mChannelMateFragment)
                        .commit();
                break;
            case MARKER_CREATE:
                fm.beginTransaction()
                        .replace(R.id.aux_frame, ChannelMarkerEditFragment.newInstance(null, null, null, null))
                        .commit();
                break;
        }
    }

    public void setAuxResizable(boolean resizable){
        resizeHandler.setVisibility(resizable ? View.VISIBLE : View.GONE);
    }

    public void editMarker(String id, QuadTree.LatLng latLng, String name, String color, Boolean isPublic){
        if(latLng != null){
            moveTo(latLng);
        }
        FragmentManager fm = getSupportFragmentManager();
        AuxFragment currentFragment = (AuxFragment) fm.findFragmentById(R.id.aux_frame);
        ViewGroup.LayoutParams params = auxFrame.getLayoutParams();
        currentFragment.setHeight(params.height);
        fm.beginTransaction()
                .replace(R.id.aux_frame, ChannelMarkerEditFragment.newInstance(id, name, color, isPublic))
                .addToBackStack(null)
                .commit();
        resizeHandler.setVisibility(View.GONE);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setAuxSizePolicy(AuxSize.WRAP);
            }
        });
    }

    public void closeKeyboard(){
        InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        View focusView = getCurrentFocus();
        if(focusView != null){
            focusView.clearFocus();
        }
        if(focusView != null && inputManager != null){
            inputManager.hideSoftInputFromWindow(focusView.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }
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
        showAux(auxComp);
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

        if(mChannelMateFragment != null) {
            mChannelMateFragment.deinitChannel();
            mChannelMateFragment.initChannel();
        }
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.openMap(mChannel, ChannelActivity.this);
            }
        });
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
    public void moveTo(QuadTree.LatLng location) {
        mChannelMapFragment.moveTo(location);
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

    public QuadTree.LatLng getMapCenter() {
        return mChannelMapFragment.getMapCenter();
    }

    public void setSearchResult(ArrayList<POI> results){
        mChannelMapFragment.setSearchResult(results);
        if(results.size() > 0){
            setAuxSizePolicy(ChannelActivity.AuxSize.FREE);
            moveToSearchResult(0, false);
        }
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
    private ChannelMateFragment mChannelMateFragment = new ChannelMateFragment();

    private Runnable mChannelListChangedListener = new Runnable() {
        @Override
        public void run() {
            if(mChannel==null) {
                mActive.setVisibility(View.GONE);
                mEnableLoading.setVisibility(View.GONE);
                return;
            }
        }
    };

    private void setupMapView(){
        if(mChannelMapFragment==null || mChannelMapFragment.getProvider() != Config.getMapProvider(this)){
            mChannelMapFragment = ChannelMapFragment.newFragment(this);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_frame, mChannelMapFragment)
                    .commit();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                setupMapView();

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

    @Override
    public void onBackPressed() {
        if(auxComp==AuxComp.TAB){
            super.onBackPressed();
        }else{
            if(!getSupportFragmentManager().popBackStackImmediate()){
                showAux(AuxComp.TAB);
            }
        }
    }
}
