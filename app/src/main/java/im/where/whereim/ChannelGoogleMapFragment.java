package im.where.whereim;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
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

    private MapView mMapView;

    private Marker mPOIMarker = null;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_googlemap, container, false);

        mMapView = view.findViewById(R.id.map);

        MapsInitializer.initialize(getActivity());

        mMapView.onCreate(savedInstanceState);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                googleMap.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.map_top_inset), 0, 0);
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

    @Override
    public void setPOI(final POI poi) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                if(mPOIMarker !=null){
                    mPOIMarker.remove();
                }
                if(poi != null){
                    mPOIMarker = googleMap.addMarker(
                            new MarkerOptions()
                                    .title(poi.name)
                                    .position(new LatLng(poi.latitude, poi.longitude))
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.search_marker))
                                    .anchor(0.5f, 1f)
                                    .zIndex(0.5f)
                    );
                    mPOIMarker.showInfoWindow();
                    mMarkerMap.put(mPOIMarker, poi);
                    clickMarker(poi);
                }
            }
        });
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
    Config.MapProvider getProvider() {
        return Config.MapProvider.GOOGLE;
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
        synchronized (mMarkerMarker) {
            for(Marker m: mMarkerMarker.values()){
                m.remove();
            }
            mMarkerMarker.clear();
        }
        synchronized (mMarkerCircle) {
            for(Circle c: mMarkerCircle.values()){
                c.remove();
            }
            mMarkerCircle.clear();
        }
    }

    @Override
    public void onDestroyView() {
        if(mMapView!=null)
            mMapView.onDestroy();
        super.onDestroyView();
    }

    private final HashMap<Marker, Object> mMarkerMap = new HashMap<>();
    private Mate selfMate = null;
    private Circle mRadiusCircle = null;
    private final HashMap<String, Circle> mMateCircle = new HashMap<>();
    private final HashMap<String, Marker> mMateMarker = new HashMap<>();
    private final HashMap<String, Marker> mMarkerMarker = new HashMap<>();
    private final HashMap<String, Circle> mMarkerCircle = new HashMap<>();


    @Override
    public void moveTo(final QuadTree.LatLng location) {
        super.moveTo(location);
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(location.latitude, location.longitude)));
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
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                getChannel(new ChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(Channel channel) {
                        if(selfMate==null)
                            return;
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
                synchronized (mMarkerCircle) {
                    Circle circle = mMarkerCircle.get(marker.id);
                    if(circle!=null){
                        circle.remove();
                    }
                }
                if(mEditingType == Key.MAP_OBJECT.MARKER && marker.id.equals(mEditingMarker.id)){
                    return;
                }
                m = null;
                if(marker.deleted){
                    mMarkerMarker.remove(marker.id);
                    mMarkerCircle.remove(marker.id);
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
                    if (marker.geofence) {
                        Circle circle = googleMap.addCircle(new CircleOptions()
                                .center(new LatLng(marker.latitude, marker.longitude))
                                .radius(marker.radius)
                                .strokeWidth(3)
                                .strokeColor(marker.isPublic ? Color.RED : 0xFFFFA500)
                        );
                        synchronized (mMarkerCircle) {
                            mMarkerCircle.put(marker.id, circle);
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

    private Circle mEditingMarkerCircle = null;
    private Marker mEditingMarkerMarker = null;
    @Override
    public void onMapLongClick(final LatLng latLng) {
        onMapLongClick(new QuadTree.LatLng(latLng.latitude, latLng.longitude));
    }

    @Override
    public void onMapClick(LatLng latLng) {
        onMapClick(new QuadTree.LatLng(latLng.latitude, latLng.longitude));
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Object obj = mMarkerMap.get(marker);
        if(obj!=null) { // non-editting marker
            clickMarker(obj);
        }
        return false;
    }
}
