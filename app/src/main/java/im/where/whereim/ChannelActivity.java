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
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import im.where.whereim.dialogs.DialogLocationServicePermissionRationale;
import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Ad;
import im.where.whereim.models.Channel;
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
    private ImageView mStatus;
    private Switch mActive;
    private View mActiveLoading;
    private View mSendingPane;
    private TextView mGeofence;

    private View resizeHandler;
    private FrameLayout mainFrame;
    private FrameLayout paneFrame;

    private float MIN_VISION_AREA_HEIGHT;
    private float MAP_TOP_INSET;
    private float MIN_PANE_HEIGHT;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        MIN_VISION_AREA_HEIGHT = Util.dp2px(this, 80);
        MAP_TOP_INSET = getResources().getDimension(R.dimen.map_top_inset);
        MIN_PANE_HEIGHT = getResources().getDimension(R.dimen.tab_height);

        setContentView(R.layout.activity_channel);

        mGeofence = findViewById(R.id.geofence);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        resizeHandler = findViewById(R.id.resize_handler);
        mainFrame = findViewById(R.id.main_frame);
        paneFrame = findViewById(R.id.pane_frame);

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
                            float dy = motionEvent.getRawY() - y;
                            ViewGroup.LayoutParams params = paneFrame.getLayoutParams();
                            params.height -= dy;
                            boolean resize = true;
                            if(mContentRoot.getHeight() - params.height - MAP_TOP_INSET < MIN_VISION_AREA_HEIGHT){
                                resize = false;
                            }
                            if(params.height < MIN_PANE_HEIGHT){
                                resize = false;
                            }
                            if(resize){
                                paneFrame.setLayoutParams(params);
                            }
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
        mStatus = findViewById(R.id.status);
        mActive = findViewById(R.id.active);
        mActiveLoading = findViewById(R.id.active_loading);
        mSendingPane = findViewById(R.id.sending_pane);

        mStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleSendingPanel();
            }
        });

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
        mActiveLoading.setOnLongClickListener(deactivate);

        findViewById(R.id.forge_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showPane(PaneComp.FORGE_LOCATION);
            }
        });

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
                                    showPane(PaneComp.MESSAGE);
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
                showPane(PaneComp.TAB);
            }
        });
    }

    private void toggleSendingPanel(){
        boolean show = mSendingPane.getVisibility() != View.GONE;
        setSendingPanel(!show);
    }

    public void setSendingPanel(boolean show){
        mSendingPane.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    public void closeDrawer(){
        mDrawerLayout.closeDrawer(Gravity.LEFT);
    }

    public void setCrosshair(boolean display){
        mChannelMapFragment.setCrosshair(display);
    }

    enum PaneSizePolicy {
        TAB,
        FREE,
        WRAP,
        FULL,
    }
    private PaneSizePolicy currentPaneSizePolicy = PaneSizePolicy.TAB;
    void setPaneSizePolicy(PaneSizePolicy size){
        boolean doChange = true;
        ViewGroup.LayoutParams params;
        params = paneFrame.getLayoutParams();
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
                if(currentPaneSizePolicy == PaneSizePolicy.FREE){
                    doChange = false;
                }
                break;
        }
        if(doChange){
            paneFrame.setLayoutParams(params);
        }
        currentPaneSizePolicy = size;
    }

    public int getPaneSize(){
        ViewGroup.LayoutParams params = paneFrame.getLayoutParams();
        return params.height;
    }

    public void setPaneSize(int height){
        if(height > mContentRoot.getHeight()){ // this may happen after screen rotation
            return;
        }
        ViewGroup.LayoutParams params = paneFrame.getLayoutParams();
        params.height = height;
        paneFrame.setLayoutParams(params);
    }

    enum PaneComp {
        TAB,
        SEARCH,
        MESSAGE,
        MARKER,
        MATE,
        MARKER_CREATE,
        FORGE_LOCATION,
    }
    private PaneComp paneComp = PaneComp.TAB;
    void showPane(PaneComp comp){
        paneComp = comp;
        setSendingPanel(false);
        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        switch (comp) {
            case TAB:
                setSearchResult(new ArrayList<POI>());
                if(mPaneRoot == null){
                    mPaneRoot = new PaneRoot();
                }
                fm.beginTransaction()
                        .replace(R.id.pane_frame, mPaneRoot)
                        .commit();
                break;
            case SEARCH:
                if(mPaneSearch == null || mPaneSearch.getProvider() != Config.getMapProvider(this)){
                    mPaneSearch = PaneSearch.newFragment(this);
                }
                mPaneRoot.resetSizePolicy();
                fm.beginTransaction()
                        .replace(R.id.pane_frame, mPaneSearch)
                        .commit();
                break;
            case MESSAGE:
                if(mPaneMessenger == null){
                    mPaneMessenger = new PaneMessenger();
                }
                mPaneMessenger.resetSizePolicy();
                fm.beginTransaction()
                        .replace(R.id.pane_frame, mPaneMessenger)
                        .commit();
                break;
            case MARKER:
                if(mPaneMarker == null){
                    mPaneMarker = new PaneMarker();
                }
                mPaneMarker.resetSizePolicy();
                fm.beginTransaction()
                        .replace(R.id.pane_frame, mPaneMarker)
                        .commit();
                break;
            case MATE:
                if(mPaneMate == null){
                    mPaneMate = new PaneMate();
                }
                mPaneMate.resetSizePolicy();
                fm.beginTransaction()
                        .replace(R.id.pane_frame, mPaneMate)
                        .commit();
                break;
            case MARKER_CREATE:
                fm.beginTransaction()
                        .replace(R.id.pane_frame, PaneMarkerEdit.newInstance(null, null, null, Config.DEFAULT_GEOFENCE_RADIUS, false, null))
                        .commit();
                break;
            case FORGE_LOCATION:
                fm.beginTransaction()
                        .replace(R.id.pane_frame, new PaneForgeLocation())
                        .commit();
                break;
        }
    }

    public void setPaneResizable(boolean resizable){
        resizeHandler.setVisibility(resizable ? View.VISIBLE : View.GONE);
    }

    public void clearFocus(){
        setSendingPanel(false);
        closeKeyboard();
        if(mPaneMarkerView != null && mPaneMarkerView.isVisible()){
            getSupportFragmentManager().popBackStackImmediate();
        }
        mPaneMarkerView = null;
    }

    private PaneMarkerView mPaneMarkerView = null;
    public void viewMarker(Marker marker){
        if(mPaneMarkerView != null && mPaneMarkerView.isVisible()){
            mPaneMarkerView.setMarker(marker);
        }else{
            FragmentManager fm = getSupportFragmentManager();
            BasePane currentFragment = (BasePane) fm.findFragmentById(R.id.pane_frame);
            if(currentFragment != null){
                ViewGroup.LayoutParams params = paneFrame.getLayoutParams();
                currentFragment.setHeight(params.height);
            }
            mPaneMarkerView = PaneMarkerView.newInstance(marker.id);
            fm.beginTransaction()
                    .replace(R.id.pane_frame, mPaneMarkerView)
                    .addToBackStack(null)
                    .commit();
        }
    }

    public void editMarker(String id, QuadTree.LatLng latLng, String name, String color, int radius, boolean geofence, Boolean isPublic){
        if(latLng != null){
            moveTo(latLng);
        }
        FragmentManager fm = getSupportFragmentManager();
        BasePane currentFragment = (BasePane) fm.findFragmentById(R.id.pane_frame);
        if(currentFragment != null){
            ViewGroup.LayoutParams params = paneFrame.getLayoutParams();
            currentFragment.setHeight(params.height);
        }
        fm.beginTransaction()
                .replace(R.id.pane_frame, PaneMarkerEdit.newInstance(id, name, color, radius, geofence, isPublic))
                .addToBackStack(null)
                .commit();
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
        showPane(paneComp);
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
            mActiveLoading.setVisibility(View.VISIBLE);
        }else{
            mStatus.setImageResource(active ? R.drawable.icon_mate : R.drawable.baseline_face_black_36);
            mActive.setVisibility(View.VISIBLE);
            mActiveLoading.setVisibility(View.GONE);
            mActive.setChecked(active);
        }

        if(mChannel.enable_radius){
            mGeofence.setText(getString(R.string.radius_m, mChannel.radius));
        }else{
            mGeofence.setText(R.string.off);
        }

        if(mChannelMapFragment != null) {
            mChannelMapFragment.deinitChannel();
            mChannelMapFragment.initChannel();
        }

        if(mPaneMessenger != null) {
            mPaneMessenger.deinitChannel();
            mPaneMessenger.initChannel();
        }

        if(mPaneMarker != null) {
            mPaneMarker.deinitChannel();
            mPaneMarker.initChannel();
        }

        if(mPaneMate != null) {
            mPaneMate.deinitChannel();
            mPaneMate.initChannel();
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
            setPaneSizePolicy(PaneSizePolicy.FREE);
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

    private PaneRoot mPaneRoot;
    private ChannelMapFragment mChannelMapFragment;
    private PaneSearch mPaneSearch;
    private PaneMessenger mPaneMessenger = new PaneMessenger();
    private PaneMarker mPaneMarker = new PaneMarker();
    private PaneMate mPaneMate = new PaneMate();

    private Runnable mChannelListChangedListener = new Runnable() {
        @Override
        public void run() {
            if(mChannel==null) {
                mActive.setVisibility(View.GONE);
                mActiveLoading.setVisibility(View.GONE);
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
        if(paneComp != PaneComp.TAB){
            if(!getSupportFragmentManager().popBackStackImmediate()){
                showPane(PaneComp.TAB);
            }
        }
    }
}
