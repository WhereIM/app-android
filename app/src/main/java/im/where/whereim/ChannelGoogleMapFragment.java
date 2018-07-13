package im.where.whereim;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Ad;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Enchantment;
import im.where.whereim.models.GooglePOI;
import im.where.whereim.models.Mate;
import im.where.whereim.models.POI;

public class ChannelGoogleMapFragment extends ChannelMapFragment implements GoogleMap.OnMapLongClickListener, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener {
    public ChannelGoogleMapFragment() {
        // Required empty public constructor
    }

    private List<OnMapReadyCallback> mPendingTask = new ArrayList<>();

    protected void getMapAsync(OnMapReadyCallback callback){
        synchronized (mPendingTask) {
            mPendingTask.add(callback);
        }
        processMapTask();
    }

    private Runnable mMapTaskRunnable = new Runnable() {
        @Override
        public void run() {
            while(true){
                OnMapReadyCallback task = null;
                synchronized (mPendingTask){
                    if(mPendingTask.size()>0){
                        task = mPendingTask.remove(0);
                    }
                }
                if(task==null){
                    break;
                }else{
                    mMapView.getMapAsync(task);
                }
            }
        }
    };
    private void processMapTask() {
        if(mMapView==null){
            return;
        }
        mHandler.post(mMapTaskRunnable);
    }

    private double defaultLat = 0;
    private double defaultLng = 0;
    private float defaultZoom = 0;
    private double currentLat = 0;
    private double currentLng = 0;

    private MapView mMapView;

    private Marker mPendingPOIMarker = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Activity activity = getActivity();
        Intent intent = activity.getIntent();
        if (intent.getBooleanExtra(Key.PENDING_POI, false)) {
            final POI poi = new POI();
            poi.latitude = intent.getDoubleExtra(Key.LATITUDE, 0);
            poi.longitude = intent.getDoubleExtra(Key.LONGITUDE, 0);
            poi.name = intent.getStringExtra(Key.NAME);

            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if(mPendingPOIMarker!=null){
                        mPendingPOIMarker.remove();
                    }
                    mPendingPOIMarker = googleMap.addMarker(
                            new MarkerOptions()
                                    .title(poi.name)
                                    .position(new LatLng(poi.latitude, poi.longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.search_marker))
                                    .anchor(0.5f, 1f)
                                    .zIndex(0.5f)
                    );
                    mPendingPOIMarker.showInfoWindow();
                    mMarkerMap.put(mPendingPOIMarker, poi);
                    clickMarker(poi);
                }
            });

            currentLat = defaultLat = poi.latitude;
            currentLng = defaultLng = poi.longitude;
            defaultZoom = 13;
        }else{
            postLocationServiceTask(new Runnable() {
                @Override
                public void run() {
                    LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                    Location locationByGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    Location locationByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    long positionTime = 0;

                    if(locationByNetwork != null){
                        positionTime = locationByNetwork.getTime();
                        defaultLat = locationByNetwork.getLatitude();
                        defaultLng = locationByNetwork.getLongitude();
                        defaultZoom = 15;
                    }
                    if(locationByGPS != null && locationByGPS.getTime() > positionTime){
                        defaultLat = locationByGPS.getLatitude();
                        defaultLng = locationByGPS.getLongitude();
                        defaultZoom = 15;
                    }
                    currentLat = defaultLat;
                    currentLng = defaultLng;
                    getMapAsync(new OnMapReadyCallback() {
                        @Override
                        public void onMapReady(GoogleMap googleMap) {
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(defaultLat, defaultLng), defaultZoom));
                        }
                    });
                }
            });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_googlemap, container, false);

        mMapView = (MapView) view.findViewById(R.id.map);

        MapsInitializer.initialize(getActivity());

        mMapView.onCreate(savedInstanceState);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                int p = (int)Util.dp2px(channelActivity, 80);
                googleMap.setPadding(0, p, 0, 0);
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                postLocationServiceTask(new Runnable() {
                    @Override
                    public void run() {
                        googleMap.setMyLocationEnabled(true);

                    }
                });
                googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                        cameraMoved(googleMap);
                    }
                });
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(defaultLat, defaultLng), defaultZoom));
                googleMap.setOnMapClickListener(ChannelGoogleMapFragment.this);
                googleMap.setOnMapLongClickListener(ChannelGoogleMapFragment.this);
                googleMap.setOnMarkerClickListener(ChannelGoogleMapFragment.this);
            }
        });

        processMapTask();

        return view;
    }

    final ArrayList<Polyline> lines = new ArrayList<>();
    private void cameraMoved(GoogleMap googleMap){
        float zoom = googleMap.getCameraPosition().zoom-1;
        currentLat = googleMap.getCameraPosition().target.latitude;
        currentLng = googleMap.getCameraPosition().target.longitude;
        LatLngBounds bounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
        String nw = QuadTree.fromLatLng(bounds.northeast.latitude, bounds.southwest.longitude, zoom);
        String se = QuadTree.fromLatLng(bounds.southwest.latitude, bounds.northeast.longitude, zoom);
        final String[] enums = QuadTree.interpolate(nw, se);
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.setVisibleTiles(enums);
            }
        });
        if(Config.DEBUG_QUADTREE) {
            synchronized (lines) {
                for (Polyline line : lines) {
                    line.remove();
                }
            }
            for (String t : enums) {
                QuadTree.Bound b = QuadTree.toBound(t);
                Polyline line = googleMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(b.north, b.west), new LatLng(b.north, b.east), new LatLng(b.south, b.east), new LatLng(b.south, b.west))
                        .width(5)
                        .color(Color.RED));
                synchronized (lines) {
                    lines.add(line);
                }
            }
        }
    }

    private Channel mChannel;

    private Runnable channedChangedListener = new Runnable() {
        @Override
        public void run() {
            updateSelfMate();
        }
    };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMarkerView = LayoutInflater.from(context).inflate(R.layout.map_mate, null);
        mMarkerViewTitle = (TextView) mMarkerView.findViewById(R.id.title);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mMarkerView = null;
    }

    @Override
    public void onResume() {
        super.onResume();

        if(mMapView!=null)
            mMapView.onResume();

        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(final CoreService.CoreBinder binder) {
                getChannel(new ChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(Channel channel) {
                        mChannel = channel;
                        binder.addChannelChangedListener(mChannel.id, channedChangedListener);
                    }
                });
            }
        });
    }

    @Override
    public void onPause() {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                if(mChannel != null){
                    binder.removeChannelChangedListener(mChannel.id, channedChangedListener);
                }
            }
        });

        if(mMapView!=null)
            mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        try {
            if (mMapView != null)
                mMapView.onDestroy();
        }catch(Exception e){
            // noop
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if(mMapView!=null)
            mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        if(mMapView!=null)
            mMapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    protected void resetMap() {
        synchronized (mMarkerMap) {
            mMarkerMap.clear();
        }
        selfMate = null;
        if(mRadiusCircle!=null){
            mRadiusCircle.remove();
            mRadiusCircle = null;
        }
        synchronized (mMateCircle) {
            for(Circle c: mMateCircle.values()){
                c.remove();
            }
            mMateCircle.clear();
        }
        synchronized (mMateMarker) {
            for(Marker m: mMateMarker.values()){
                m.remove();
            }
            mMateMarker.clear();
        }
    }

    @Override
    public void onDestroyView() {
        if(mMapView!=null)
            mMapView.onDestroy();
        super.onDestroyView();
    }

    private HashMap<Marker, Object> mMarkerMap = new HashMap<>();
    private Mate selfMate = null;
    private Circle mRadiusCircle = null;
    private HashMap<String, Circle> mMateCircle = new HashMap<>();
    private HashMap<String, Marker> mMateMarker = new HashMap<>();

    @Override
    public void moveTo(final QuadTree.LatLng location) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.latitude, location.longitude)));
            }
        });
    }

    @Override
    public void moveToPin(final QuadTree.LatLng location) {
        if(mPendingPOIMarker!=null){
            mPendingPOIMarker.remove();
        }
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mPendingPOIMarker = googleMap.addMarker(
                        new MarkerOptions()
                                .position(new LatLng(location.latitude, location.longitude))
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icon_pin))
                                .anchor(0.5f, 1f)
                                .zIndex(0.5f)
                );
                mPendingPOIMarker.showInfoWindow();
                mMarkerMap.put(mPendingPOIMarker, location);
                moveTo(location);
                clickMarker(location);
            }
        });
    }

    @Override
    public void onMateData(final Mate mate, final boolean focus){
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                Circle circle;
                Marker marker;
                synchronized (mMateCircle) {
                    circle = mMateCircle.get(mate.id);
                    if(circle!=null){
                        circle.remove();
                    }
                }

                synchronized (mMateMarker) {
                    marker = mMateMarker.get(mate.id);
                    if(marker!=null){
                        marker.remove();
                    }
                }

                marker = null;
                if(mate.deleted){
                    mMateCircle.remove(mate.id);
                    mMateMarker.remove(mate.id);
                }else {
                    if(mate.latitude!=null && mate.longitude!=null && (!mate.stale || mate==focusMate)){
                        circle = googleMap.addCircle(new CircleOptions()
                                .center(new LatLng(mate.latitude, mate.longitude))
                                .radius(mate.accuracy)
                                .fillColor(0x3f888888)
                                .strokeWidth(0)
                        );
                        synchronized (mMateCircle) {
                            mMateCircle.put(mate.id, circle);
                        }

                        mMarkerView.invalidate();
                        mMarkerViewTitle.setText(mate.getDisplayName());
                        mMarkerView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        mMarkerView.layout(0, 0, mMarkerView.getMeasuredWidth(), mMarkerView.getMeasuredHeight());
                        mMarkerView.setDrawingCacheEnabled(true);
                        mMarkerView.buildDrawingCache();

                        marker = googleMap.addMarker(
                                new MarkerOptions()
                                        .position(new LatLng(mate.latitude, mate.longitude))
                                        .anchor(0.5f, 1f)
                                        .zIndex(0.75f)
                                        .alpha(mate.stale ? 0.5f : 1f)
                                        .icon(BitmapDescriptorFactory.fromBitmap(mMarkerView.getDrawingCache()))
                        );

                        synchronized (mMateMarker) {
                            mMateMarker.put(mate.id, marker);
                        }
                    }
                }

                synchronized (mMarkerMap) {
                    if(marker!=null){
                        mMarkerMap.put(marker, mate);
                    }
                }
                if(focus && mate.latitude!=null && mate.longitude!=null){
                    clickMarker(mate);
                }

                if(mate.id.equals(mChannel.mate_id)){
                    selfMate = mate;
                    updateSelfMate();
                }
            }
        });
    }

    private void updateSelfMate(){
        if(selfMate==null)
            return;
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                getChannel(new ChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(Channel channel) {
                        if(mRadiusCircle!=null){
                            mRadiusCircle.remove();
                            mRadiusCircle = null;
                        }
                        if(selfMate.latitude!=null && selfMate.longitude!=null){
                            if(channel.enable_radius!=null && channel.enable_radius) {
                                int color;
                                if (channel.active) {
                                    color = Color.MAGENTA;
                                } else {
                                    color = Color.GRAY;
                                }
                                mRadiusCircle = googleMap.addCircle(new CircleOptions()
                                        .center(new LatLng(selfMate.latitude, selfMate.longitude))
                                        .radius(channel.radius)
                                        .strokeWidth(5)
                                        .strokeColor(color));
                            }
                        }
                    }
                });
            }
        });
    }

    private Mate focusMate = null;
    @Override
    public void moveToMate(final Mate mate, final boolean focus) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                Mate exFocusMate = focusMate;
                focusMate = mate;
                if(exFocusMate!=null) {
                    onMateData(exFocusMate, false);
                }
                if(mate!=null) {
                    onMateData(mate, focus);
                    if(mate.latitude!=null && mate.longitude!=null){
                        moveTo(new QuadTree.LatLng(mate.latitude, mate.longitude));
                    }
                }
            }
        });
    }

    final private HashMap<String, Circle> mEnchantmentCircle = new HashMap<>();

    @Override
    public void onEnchantmentData(final Enchantment enchantment){
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                synchronized (mEnchantmentCircle) {
                    Circle circle = mEnchantmentCircle.get(enchantment.id);
                    if(circle!=null){
                        circle.remove();
                    }
                }
                if(mEditingType == Key.MAP_OBJECT.ENCHANTMENT && enchantment.id.equals(mEditingEnchantment.id)){
                    return;
                }
                if(enchantment.deleted){
                    mEnchantmentCircle.remove(enchantment.id);
                }else {
                    if (enchantment.enabled == null || enchantment.enabled || enchantment==focusEnchantment) {
                        Circle circle = googleMap.addCircle(new CircleOptions()
                                .center(new LatLng(enchantment.latitude, enchantment.longitude))
                                .radius(enchantment.radius)
                                .strokeWidth(3)
                                .strokeColor((enchantment.enabled == null || enchantment.enabled) ? (enchantment.isPublic ? Color.RED : 0xFFFFA500) : Color.GRAY));
                        synchronized (mEnchantmentCircle) {
                            mEnchantmentCircle.put(enchantment.id, circle);
                        }
                    }
                }
            }
        });
    }

    private Enchantment focusEnchantment = null;
    @Override
    public void moveToEnchantment(final Enchantment enchantment) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                Enchantment exFocusEnchantment = focusEnchantment;
                focusEnchantment = enchantment;
                if(exFocusEnchantment!=null) {
                    onEnchantmentData(exFocusEnchantment);
                }
                if(enchantment!=null) {
                    onEnchantmentData(enchantment);
                    moveTo(new QuadTree.LatLng(enchantment.latitude, enchantment.longitude));
                }
            }
        });
    }

    final private HashMap<String, Marker> mMarkerMarker = new HashMap<>();

    @Override
    public void onMarkerData(final im.where.whereim.models.Marker marker, final boolean focus) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                Marker m;
                synchronized (mMarkerMarker) {
                    m = mMarkerMarker.get(marker.id);
                    if(m!=null){
                        m.remove();
                    }
                }
                if(mEditingType == Key.MAP_OBJECT.MARKER && marker.id.equals(mEditingMarker.id)){
                    return;
                }
                m = null;
                if(marker.deleted){
                    mMarkerMarker.remove(marker.id);
                }else {
                    if (marker.enabled == null || marker.enabled || marker==focusMarker) {
                        m = googleMap.addMarker(
                                new MarkerOptions()
                                        .title(marker.name)
                                        .alpha((marker.enabled == null || marker.enabled) ? 1f: 0.4f)
                                        .position(new LatLng(marker.latitude, marker.longitude))
                                        .icon(marker.getIconBitmapDescriptor())
                                        .anchor(0.5f, 1)
                                        .zIndex(0.25f)
                        );

                        synchronized (mMarkerMarker) {
                            mMarkerMarker.put(marker.id, m);
                        }

                        if(marker==focusMarker){
                            m.showInfoWindow();
                        }
                    }
                }
                synchronized (mMarkerMap) {
                    if(m!=null){
                        mMarkerMap.put(m, marker);
                    }
                }
                if(focus){
                    clickMarker(marker);
                }
            }
        });
    }

    private im.where.whereim.models.Marker focusMarker = null;
    @Override
    public void moveToMarker(final im.where.whereim.models.Marker marker, final boolean focus) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                im.where.whereim.models.Marker exFocusMaker = focusMarker;
                focusMarker = marker;
                if(exFocusMaker!=null) {
                    onMarkerData(exFocusMaker, false);
                }
                if(marker!=null) {
                    onMarkerData(marker, focus);
                    moveTo(new QuadTree.LatLng(marker.latitude, marker.longitude));
                }
            }
        });
    }

    @Override
    public QuadTree.LatLng getMapCenter() {
        return new QuadTree.LatLng(currentLat, currentLng);
    }

    private ArrayList<POI> mSearchResults;
    private ArrayList<Marker> mSearchResultMarkers = new ArrayList<>();
    @Override
    public void setSearchResult(final ArrayList<POI> results) {
        synchronized (mSearchResultMarkers) {
            for (Marker marker : mSearchResultMarkers) {
                marker.remove();
            }
            mSearchResultMarkers.clear();
        }
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mSearchResults = results;
                for (POI _result : results) {
                    GooglePOI result = (GooglePOI) _result;
                    Marker m = googleMap.addMarker(
                            new MarkerOptions()
                                    .title(result.name)
                                    .position(new LatLng(result.latitude, result.longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.search_marker))
                                    .anchor(0.5f, 1f)
                                    .zIndex(0.5f)
                    );
                    synchronized (mSearchResultMarkers) {
                        mSearchResultMarkers.add(m);
                    }
                    synchronized (mMarkerMap) {
                        mMarkerMap.put(m, _result);
                    }
                }
            }
        });
    }

    @Override
    public void moveToSearchResult(final int position, final boolean focus) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                if(mSearchResults==null || position >= mSearchResults.size()){
                    return;
                }
                POI result = mSearchResults.get(position);
                googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(result.latitude, result.longitude)));
                Marker m = mSearchResultMarkers.get(position);
                if(m != null){
                    m.showInfoWindow();
                }
                if(focus) {
                    clickMarker(result);
                }
            }
        });
    }

    private final HashMap<String, Marker> mAdMarkers = new HashMap<>();
    @Override
    public void onMapAd(final HashMap<String, Ad> ads) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                float zoom = googleMap.getCameraPosition().zoom;
                ArrayList<String> ids = new ArrayList<>();
                synchronized (ads) {
                    for (Ad ad : ads.values()) {
                        if(ad.level <= zoom){
                            ids.add(ad.id);
                            synchronized (mAdMarkers) {
                                if (!mAdMarkers.containsKey(ad.id)) {
                                    Marker marker = googleMap.addMarker(
                                            new MarkerOptions()
                                                    .title(ad.name)
                                                    .snippet(ad.desc)
                                                    .position(new LatLng(ad.latitude, ad.longitude))
                                    );
                                    mAdMarkers.put(ad.id, marker);
                                }
                            }
                        }
                    }
                }
                synchronized (mAdMarkers) {
                    ArrayList<String> out = new ArrayList<>();
                    for (String id : mAdMarkers.keySet()) {
                        if(!ids.contains(id)){
                            out.add(id);
                        }
                    }
                    for (String id : out) {
                        mAdMarkers.get(id).remove();
                        mAdMarkers.remove(id);
                    }
                }
            }
        });
    }

    private Circle mEditingEnchantmentCircle = null;
    private Marker mEditingMarkerMarker = null;
    @Override
    public void onMapLongClick(final LatLng latLng) {
        onMapLongClick(new QuadTree.LatLng(latLng.latitude, latLng.longitude));
    }

    @Override
    public void onMapClick(LatLng latLng) {
        if(mEditingType!=null){
            return;
        }
        if(mPendingPOIMarker!=null){
            mPendingPOIMarker.remove();
        }
        clearAction(false);
    }

    @Override
    protected void refreshEditing(){
        if(mEditingType== Key.MAP_OBJECT.ENCHANTMENT){
            mEnchantmentController.setVisibility(View.VISIBLE);
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if(mEditingEnchantmentCircle !=null){
                        mEditingEnchantmentCircle.remove();
                    }
                    mEditingEnchantmentCircle = googleMap.addCircle(new CircleOptions()
                            .center(new LatLng(mEditingEnchantment.latitude, mEditingEnchantment.longitude))
                            .radius(mEditingEnchantment.radius)
                            .strokeWidth(3)
                            .strokeColor(mEditingEnchantment.isPublic ? Color.RED : 0xFFFFA500));
                }
            });
        }else{
            mEnchantmentController.setVisibility(View.GONE);
            if(mEditingEnchantmentCircle!=null){
                mEditingEnchantmentCircle.remove();
            }
        }
        if(mEditingType== Key.MAP_OBJECT.MARKER){
            mMarkerController.setVisibility(View.VISIBLE);
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if(mEditingMarkerMarker !=null){
                        mEditingMarkerMarker.remove();
                    }
                    mEditingMarkerMarker = googleMap.addMarker(
                            new MarkerOptions()
                                    .title(mEditingMarker.name)
                                    .position(new LatLng(mEditingMarker.latitude, mEditingMarker.longitude))
                                    .icon(mEditingMarker.getIconBitmapDescriptor())
                                    .anchor(0.5f, 1f)
                                    .zIndex(1f)
                    );
                    mEditingMarkerMarker.showInfoWindow();
                }
            });

        }else{
            mMarkerController.setVisibility(View.GONE);
            if(mEditingMarkerMarker !=null){
                mEditingMarkerMarker.remove();
            }
        }
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Object obj = mMarkerMap.get(marker);
        if(obj!=null) { // non-editting marker
            if (mPendingPOIMarker != null) {
                mPendingPOIMarker.remove();
            }
            clearAction(true);
            clickMarker(obj);
        }
        return false;
    }
}
