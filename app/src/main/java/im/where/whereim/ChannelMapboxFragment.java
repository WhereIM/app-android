package im.where.whereim;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.annotations.Polyline;
import com.mapbox.mapboxsdk.annotations.PolylineOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerMode;
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin;
import com.mapbox.services.android.telemetry.location.LocationEngine;
import com.mapbox.services.android.telemetry.location.LocationEngineListener;
import com.mapbox.services.android.telemetry.location.LocationEnginePriority;
import com.mapbox.services.android.telemetry.location.LostLocationEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Ad;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Mate;
import im.where.whereim.models.POI;

public class ChannelMapboxFragment extends ChannelMapFragment implements LocationEngineListener, MapboxMap.OnMapLongClickListener, MapboxMap.OnMapClickListener, MapboxMap.OnMarkerViewClickListener {
    private final static int MAP_MOVE_ANIMATION_DURATION = 750; //ms

    // https://github.com/mapbox/mapbox-gl-native/issues/2167#issuecomment-200302992
    private ArrayList<LatLng> polygonCircleForCoordinate(LatLng location, double radius) {
        int degreesBetweenPoints = 8; //45 sides
        int numberOfPoints = (int) Math.floor(360 / degreesBetweenPoints);
        double distRadians = radius / 6371000.0; // earth radius in meters
        double centerLatRadians = location.getLatitude() * Math.PI / 180;
        double centerLonRadians = location.getLongitude() * Math.PI / 180;
        ArrayList<LatLng> polygons = new ArrayList<>(); //array to hold all the points
        for (int index = 0; index < numberOfPoints; index++) {
            double degrees = index * degreesBetweenPoints;
            double degreeRadians = degrees * Math.PI / 180;
            double pointLatRadians = Math.asin(Math.sin(centerLatRadians) * Math.cos(distRadians) + Math.cos(centerLatRadians) * Math.sin(distRadians) * Math.cos(degreeRadians));
            double pointLonRadians = centerLonRadians + Math.atan2(Math.sin(degreeRadians) * Math.sin(distRadians) * Math.cos(centerLatRadians),
                    Math.cos(distRadians) - Math.sin(centerLatRadians) * Math.sin(pointLatRadians));
            double pointLat = pointLatRadians * 180 / Math.PI;
            double pointLon = pointLonRadians * 180 / Math.PI;
            LatLng point = new LatLng(pointLat, pointLon);
            polygons.add(point);
        }
        polygons.add(polygons.get(0));
        return polygons;
    }

    public ChannelMapboxFragment() {
        // Required empty public constructor
    }

    private List<OnMapReadyCallback> mPendingTask = new ArrayList<>();

    protected void getMapAsync(OnMapReadyCallback callback) {
        synchronized (mPendingTask) {
            mPendingTask.add(callback);
        }
        processMapTask();
    }

    private Runnable mMapTaskRunnable = new Runnable() {
        @Override
        public void run() {
            while (true) {
                OnMapReadyCallback task = null;
                synchronized (mPendingTask) {
                    if (mPendingTask.size() > 0) {
                        task = mPendingTask.remove(0);
                    }
                }
                if (task == null) {
                    break;
                } else {
                    mMapView.getMapAsync(task);
                }
            }
        }
    };

    private void processMapTask() {
        if (mMapView == null) {
            return;
        }
        mHandler.post(mMapTaskRunnable);
    }

    private MapView mMapView;
    private IconFactory iconFactory;

    private MarkerView mPOIMarker = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getActivity(), Config.API_KEY_MAPBOX);

        iconFactory = IconFactory.getInstance(getActivity());

        postLocationServiceTask(new Runnable() {
            @Override
            public void run() {
                locationEngine = new LostLocationEngine(getContext());
                locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);
                locationEngine.addLocationEngineListener(ChannelMapboxFragment.this);
                locationEngine.activate();
            }
        });

        postLocationServiceTask(new Runnable() {
            @Override
            public void run() {
                Location l = locationEngine.getLastLocation();
                if(l != null){
                    currentLat = l.getLatitude();
                    currentLng = l.getLongitude();
                }

                getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(MapboxMap mapboxMap) {
                        CameraPosition position = new CameraPosition.Builder()
                                .target(new LatLng(defaultLat, defaultLng))
                                .build();

                        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), MAP_MOVE_ANIMATION_DURATION);
                    }
                });
            }
        });
    }

    private LocationLayerPlugin locationLayerPlugin;
    private LocationEngine locationEngine;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_mapbox, container, false);

        view.findViewById(R.id.my_location).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(MapboxMap mapboxMap) {
                        CameraPosition position = new CameraPosition.Builder()
                                .target(new LatLng(defaultLat, defaultLng))
                                .build();

                        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), MAP_MOVE_ANIMATION_DURATION);
                    }
                });
            }
        });

        mMapView = view.findViewById(R.id.map);

        mMapView.onCreate(savedInstanceState);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                mapboxMap.setPadding(0, getResources().getDimensionPixelOffset(R.dimen.map_top_inset), 0, 0);
                postLocationServiceTask(new Runnable() {
                    @Override
                    public void run() {
                        locationLayerPlugin = new LocationLayerPlugin(mMapView, mapboxMap, locationEngine);
                        locationLayerPlugin.setLocationLayerEnabled(LocationLayerMode.COMPASS);
                        getLifecycle().addObserver(locationLayerPlugin);
                    }
                });

                mapboxMap.setAllowConcurrentMultipleOpenInfoWindows(false);
                mapboxMap.getUiSettings().setCompassGravity(Gravity.LEFT|Gravity.TOP);

                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(defaultLat, defaultLng))
                        .zoom(defaultZoom)
                        .build();

                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), MAP_MOVE_ANIMATION_DURATION);
                mapboxMap.setOnCameraChangeListener(new MapboxMap.OnCameraChangeListener() {
                    @Override
                    public void onCameraChange(CameraPosition position) {
                        cameraMoved(mapboxMap);
                    }
                });
                mapboxMap.setOnMapClickListener(ChannelMapboxFragment.this);
                mapboxMap.setOnMapLongClickListener(ChannelMapboxFragment.this);
                mapboxMap.getMarkerViewManager().setOnMarkerViewClickListener(ChannelMapboxFragment.this);
            }
        });

        processMapTask();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        mMapView.onStart();
        postLocationServiceTask(new Runnable() {
            @Override
            public void run() {
                if (locationEngine != null) {
                    locationEngine.requestLocationUpdates();
                    locationEngine.addLocationEngineListener(ChannelMapboxFragment.this);
                }
            }
        });
    }

    @Override
    public void onStop() {
        mMapView.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationEngineListener(this);
            locationEngine.removeLocationUpdates();
        }
        super.onStop();
    }

    @Override
    public void setPOI(final POI poi) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mPOIMarker != null) {
                            mPOIMarker.remove();
                        }
                        if(poi != null){
                            MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                                    .title(poi.name)
                                    .position(new LatLng(poi.latitude, poi.longitude))
                                    .icon(iconFactory.fromResource(R.drawable.search_marker)
//                          .zIndex(0.5f)
                                    );
                            mPOIMarker = mapboxMap.addMarker(markerViewOptions);
                            mapboxMap.selectMarker(mPOIMarker);
                            mMarkerMap.put(mPOIMarker, poi);
                            clickMarker(poi);
                        }
                    }
                });
            }
        });
    }

    final LinkedList<Polyline> lines = new LinkedList<>();

    private CameraPosition mLastCameraPosition = null;
    private void cameraMoved(MapboxMap mapboxMap) {
        CameraPosition cp = mapboxMap.getCameraPosition();
        if(mLastCameraPosition!=null && mLastCameraPosition.equals(cp)){
            return;
        }
        mLastCameraPosition = cp;
        double zoom = cp.zoom - 1;
        currentLat = cp.target.getLatitude();
        currentLng = cp.target.getLongitude();
        LatLngBounds bounds = mapboxMap.getProjection().getVisibleRegion().latLngBounds;
        String nw = QuadTree.fromLatLng(bounds.getLatNorth(), bounds.getLonWest(), zoom);
        String se = QuadTree.fromLatLng(bounds.getLatSouth(), bounds.getLonEast(), zoom);
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
                Polyline line = mapboxMap.addPolyline(new PolylineOptions()
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

        if (mMapView != null)
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
                if (mChannel != null) {
                    binder.removeChannelChangedListener(mChannel.id, channedChangedListener);
                }
            }
        });

        if (mMapView != null)
            mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        try {
            if (mMapView != null)
                mMapView.onDestroy();
        } catch (Exception e) {
            // noop
        }
        if(locationEngine != null){
            locationEngine.deactivate();
        }
        super.onDestroy();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mMapView != null)
            mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        if (mMapView != null)
            mMapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    Config.MapProvider getProvider() {
        return Config.MapProvider.MAPBOX;
    }

    @Override
    protected void resetMap() {
        synchronized (mMarkerMap) {
            mMarkerMap.clear();
        }
        selfMate = null;
        if (mRadiusCircle != null) {
            mRadiusCircle.remove();
            mRadiusCircle = null;
        }
        synchronized (mMateCircle) {
            for(Polygon p: mMateCircle.values()){
                p.remove();
            }
            mMateCircle.clear();
        }
        synchronized (mMateMarker) {
            for(MarkerView m: mMateMarker.values()){
                m.remove();
            }
            mMateMarker.clear();
        }
        synchronized (mMarkerCircle) {
            for(Polyline p: mMarkerCircle.values()){
                p.remove();
            }
            mMarkerCircle.clear();
        }
        synchronized (mMarkerMarker) {
            for(MarkerView m: mMarkerMarker.values()){
                m.remove();
            }
            mMarkerMarker.clear();
        }
    }

    @Override
    public void onDestroyView() {
        if (mMapView != null)
            mMapView.onDestroy();
        super.onDestroyView();
    }

    private final HashMap<MarkerView, Object> mMarkerMap = new HashMap<>();
    private Mate selfMate = null;
    private Polygon mRadiusCircle = null;
    private final HashMap<String, Polygon> mMateCircle = new HashMap<>();
    private final HashMap<String, MarkerView> mMateMarker = new HashMap<>();
    private final HashMap<String, MarkerView> mMarkerMarker = new HashMap<>();
    private final HashMap<String, Polyline> mMarkerCircle = new HashMap<>();


    @Override
    public void moveTo(final QuadTree.LatLng location) {
        super.moveTo(location);
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                CameraPosition position = new CameraPosition.Builder()
                        .target(new LatLng(location.latitude, location.longitude))
                        .build();
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), MAP_MOVE_ANIMATION_DURATION);
            }
        });
    }

    @Override
    public void onMateData(final Mate mate, final boolean focus) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                Polygon circle;
                MarkerView marker;
                synchronized (mMateCircle) {
                    circle = mMateCircle.get(mate.id);
                    if (circle != null) {
                        circle.remove();
                    }
                }

                synchronized (mMateMarker) {
                    marker = mMateMarker.get(mate.id);
                    if (marker != null) {
                        marker.remove();
                    }
                }

                marker = null;
                if (mate.deleted) {
                    mMateCircle.remove(mate.id);
                    mMateMarker.remove(mate.id);
                } else {
                    if (mate.latitude != null && mate.longitude != null && (!mate.stale || mate == focusMate)) {
                        circle = mapboxMap.addPolygon(new PolygonOptions()
                                .addAll(polygonCircleForCoordinate(new LatLng(mate.latitude, mate.longitude), mate.accuracy))
                                .fillColor(Color.parseColor("#888888"))
                                .alpha(0.25f)
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
                        Bitmap bm = mMarkerView.getDrawingCache();

                        MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                                .position(new LatLng(mate.latitude, mate.longitude))
                                .icon(iconFactory.fromBitmap(bm.copy(bm.getConfig(), true)))
                                .alpha(mate.stale ? 0.5f : 1f);
//                              .zIndex(0.75f)

                        marker = mapboxMap.addMarker(markerViewOptions);

                        synchronized (mMateMarker) {
                            mMateMarker.put(mate.id, marker);
                        }
                    }
                }

                synchronized (mMarkerMap) {
                    if (marker != null) {
                        mMarkerMap.put(marker, mate);
                    }
                }
                if (focus && mate.latitude != null && mate.longitude != null) {
                    clickMarker(mate);
                }

                if (mate.id.equals(mChannel.mate_id)) {
                    selfMate = mate;
                    updateSelfMate();
                }
            }
        });
    }

    private void updateSelfMate() {
        if (selfMate == null)
            return;
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                getChannel(new ChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(Channel channel) {
                        if (mRadiusCircle != null) {
                            mRadiusCircle.remove();
                            mRadiusCircle = null;
                        }
                        if (selfMate.latitude != null && selfMate.longitude != null) {
                            if (channel.enable_radius != null && channel.enable_radius) {
                                int color;
                                if (channel.active) {
                                    color = Color.MAGENTA;
                                } else {
                                    color = Color.GRAY;
                                }
                                mRadiusCircle = mapboxMap.addPolygon(new PolygonOptions()
                                        .addAll(polygonCircleForCoordinate(new LatLng(selfMate.latitude, selfMate.longitude), channel.radius))
                                        .strokeColor(color)
                                );
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
            public void onMapReady(MapboxMap mapboxMap) {
                Mate exFocusMate = focusMate;
                focusMate = mate;
                if (exFocusMate != null) {
                    onMateData(exFocusMate, false);
                }
                if (mate != null) {
                    onMateData(mate, focus);
                    if (mate.latitude != null && mate.longitude != null) {
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
            public void onMapReady(MapboxMap mapboxMap) {
                MarkerView m;
                synchronized (mMarkerMarker) {
                    m = mMarkerMarker.get(marker.id);
                    if (m != null) {
                        m.remove();
                    }
                }
                synchronized (mMarkerCircle) {
                    Polyline circle = mMarkerCircle.get(marker.id);
                    if (circle != null) {
                        circle.remove();
                    }
                }
                if(mEditingType == Key.MAP_OBJECT.MARKER && marker.id.equals(mEditingMarker.id)){
                    return;
                }
                m = null;
                if (marker.deleted) {
                    mMarkerMarker.remove(marker.id);
                    mMarkerCircle.remove(marker.id);
                } else {
                    if (marker.enabled == null || marker.enabled || marker == focusMarker) {
                        MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                                .title(marker.name)
                                .alpha((marker.enabled == null || marker.enabled) ? 1f: 0.4f)
                                .position(new LatLng(marker.latitude, marker.longitude))
                                .icon(iconFactory.fromResource(marker.getIconResId())
//                                .zIndex(0.25f)
                        );

                        m = mapboxMap.addMarker(markerViewOptions);

                        synchronized (mMarkerMarker) {
                            mMarkerMarker.put(marker.id, m);
                        }

                        if (marker == focusMarker) {
                            mapboxMap.selectMarker(m);
                        }
                    }
                    if(marker.geofence){
                        Polyline circle = mapboxMap.addPolyline(new PolylineOptions()
                                .addAll(polygonCircleForCoordinate(new LatLng(marker.latitude, marker.longitude), marker.radius))
                                .width(1)
                                .color(marker.isPublic ? Color.RED : 0xFFFFA500)
                        );
                        synchronized (mMarkerCircle) {
                            mMarkerCircle.put(marker.id, circle);
                        }
                    }
                }
                synchronized (mMarkerMap) {
                    if (m != null) {
                        mMarkerMap.put(m, marker);
                    }
                }
                if (focus) {
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
            public void onMapReady(MapboxMap mapboxMap) {
                im.where.whereim.models.Marker exFocusMaker = focusMarker;
                focusMarker = marker;
                if (exFocusMaker != null) {
                    onMarkerData(exFocusMaker, false);
                }
                if (marker != null) {
                    onMarkerData(marker, focus);
                    moveTo(new QuadTree.LatLng(marker.latitude, marker.longitude));
                }
            }
        });
    }

    private ArrayList<POI> mSearchResults;

    private ArrayList<MarkerView> mSearchResultMarkers = new ArrayList<>();
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
            public void onMapReady(MapboxMap mapboxMap) {
                mSearchResults = results;
                for (POI result : results) {
                    MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                            .title(result.name)
                            .position(new LatLng(result.latitude, result.longitude))
                            .icon(iconFactory.fromResource(R.drawable.search_marker));
//                          .zIndex(0.5f)
                    MarkerView m = mapboxMap.addMarker(markerViewOptions);
                    synchronized (mSearchResultMarkers) {
                        mSearchResultMarkers.add(m);
                    }
                    synchronized (mMarkerMap) {
                        mMarkerMap.put(m, result);
                    }
                }
            }
        });
    }

    @Override
    public void moveToSearchResult(final int position, final boolean focus) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                if (mSearchResults == null || position >= mSearchResults.size()) {
                    return;
                }
                POI result = mSearchResults.get(position);
                moveTo(new QuadTree.LatLng(result.latitude, result.longitude));
                MarkerView m = mSearchResultMarkers.get(position);
                if(m != null){
                    mapboxMap.selectMarker(m);
                }
                if (focus) {
                    clickMarker(result);
                }
            }
        });
    }

    private final HashMap<String, MarkerView> mAdMarkers = new HashMap<>();
    @Override
    public void onMapAd(final HashMap<String, Ad> ads) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
//                float zoom = googleMap.getCameraPosition().zoom;
//                ArrayList<String> ids = new ArrayList<>();
//                synchronized (ads) {
//                    for (Ad ad : ads.values()) {
//                        if(ad.level <= zoom){
//                            ids.add(ad.id);
//                            synchronized (mAdMarkers) {
//                                if (!mAdMarkers.containsKey(ad.id)) {
//                                    Marker marker = googleMap.addMarker(
//                                            new MarkerOptions()
//                                                    .title(ad.name)
//                                                    .snippet(ad.desc)
//                                                    .position(new LatLng(ad.latitude, ad.longitude))
//                                    );
//                                    mAdMarkers.put(ad.id, marker);
//                                }
//                            }
//                        }
//                    }
//                }
//                synchronized (mAdMarkers) {
//                    ArrayList<String> out = new ArrayList<>();
//                    for (String id : mAdMarkers.keySet()) {
//                        if(!ids.contains(id)){
//                            out.add(id);
//                        }
//                    }
//                    for (String id : out) {
//                        mAdMarkers.get(id).remove();
//                        mAdMarkers.remove(id);
//                    }
//                }
            }
        });
    }

    private Polyline mEditingEnchantmentCircle = null;
    private MarkerView mEditingMarkerMarker = null;

    @Override
    public void onMapClick(@NonNull LatLng point) {
        onMapClick(new QuadTree.LatLng(point.getLatitude(), point.getLongitude()));
    }

    @Override
    public void onMapLongClick(@NonNull LatLng point) {
        onMapLongClick(new QuadTree.LatLng(point.getLatitude(), point.getLongitude()));
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker, @NonNull View view, @NonNull MapboxMap.MarkerViewAdapter adapter) {
        Object obj = mMarkerMap.get(marker);
        if(obj!=null) { // non-editting marker
            clickMarker(obj);
        }
        return false;
    }

    @Override
    public void onConnected() {
        locationEngine.requestLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {

    }
}