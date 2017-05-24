package im.where.whereim;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

public class ChannelMapboxFragment extends ChannelMapFragment implements MapboxMap.OnMapLongClickListener, MapboxMap.OnMapClickListener, MapboxMap.OnMarkerViewClickListener {
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

    private double defaultLat = 0;
    private double defaultLng = 0;
    private float defaultZoom = 0;
    private double currentLat = 0;
    private double currentLng = 0;

    private MapView mMapView;
    private IconFactory iconFactory;

    private MarkerView mPendingPOIMarker = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(getActivity(), "pk.eyJ1Ijoid2hlcmVpbSIsImEiOiJjaXltbmtvbHUwMDM4MzNwZnNsZHVtbHE4In0.n36bMG_LdA9yOu8-fQS2vw");

        iconFactory = IconFactory.getInstance(getActivity());

        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Location locationByGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        long positionTime = 0;

        Activity activity = getActivity();
        Intent intent = activity.getIntent();
        if (intent.getBooleanExtra(Key.PENDING_POI, false)) {
            final POI poi = new POI();
            poi.latitude = intent.getDoubleExtra(Key.LATITUDE, 0);
            poi.longitude = intent.getDoubleExtra(Key.LONGITUDE, 0);
            poi.name = intent.getStringExtra(Key.NAME);

            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(MapboxMap mapboxMap) {
                    MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                            .title(poi.name)
                            .position(new LatLng(poi.latitude, poi.longitude))
                            .icon(iconFactory.fromResource(R.drawable.search_marker)
//                          .zIndex(0.5f)
                    );
                    mPendingPOIMarker = mapboxMap.addMarker(markerViewOptions);
                    mapboxMap.selectMarker(mPendingPOIMarker);
                    mMarkerMap.put(mPendingPOIMarker, poi);
                    clickMarker(poi);
                }
            });

            currentLat = defaultLat = poi.latitude;
            currentLng = defaultLng = poi.longitude;
            defaultZoom = 13;
        } else {
            if (locationByNetwork != null) {
                positionTime = locationByNetwork.getTime();
                defaultLat = locationByNetwork.getLatitude();
                defaultLng = locationByNetwork.getLongitude();
                defaultZoom = 15;
            }
            if (locationByGPS != null && locationByGPS.getTime() > positionTime) {
                defaultLat = locationByGPS.getLatitude();
                defaultLng = locationByGPS.getLongitude();
                defaultZoom = 15;
            }
            currentLat = defaultLat;
            currentLng = defaultLng;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_mapbox, container, false);

        mMapView = (MapView) view.findViewById(R.id.map);

        mMapView.onCreate(savedInstanceState);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                mapboxMap.setMyLocationEnabled(true);
                mapboxMap.setAllowConcurrentMultipleOpenInfoWindows(false);

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
    }

    @Override
    public void onStop() {
        mMapView.onStop();
        super.onStop();
    }

    final ArrayList<Polyline> lines = new ArrayList<>();

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
    public void onDestroyView() {
        if (mMapView != null)
            mMapView.onDestroy();
        super.onDestroyView();
    }

    private HashMap<MarkerView, Object> mMarkerMap = new HashMap<>();
    private Mate selfMate = null;
    private Polygon mRadiusCircle = null;
    private HashMap<String, Polygon> mMateCircle = new HashMap<>();
    private HashMap<String, MarkerView> mMateMarker = new HashMap<>();

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
            public void onMapReady(MapboxMap mapboxMap) {
                if (mRadiusCircle != null) {
                    mRadiusCircle.remove();
                    mRadiusCircle = null;
                }
                if (selfMate.latitude != null && selfMate.longitude != null) {
                    if (mChannel.enable_radius != null && mChannel.enable_radius) {
                        int color;
                        if (mChannel.active) {
                            color = Color.MAGENTA;
                        } else {
                            color = Color.GRAY;
                        }
                        mRadiusCircle = mapboxMap.addPolygon(new PolygonOptions()
                                .addAll(polygonCircleForCoordinate(new LatLng(selfMate.latitude, selfMate.longitude), mChannel.radius))
                                .strokeColor(color)
                        );
                    }
                }
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
                        CameraPosition position = new CameraPosition.Builder()
                                .target(new LatLng(mate.latitude, mate.longitude))
                                .build();
                        mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), MAP_MOVE_ANIMATION_DURATION);
                    }
                }
            }
        });
    }

    final private HashMap<String, Polyline> mEnchantmentCircle = new HashMap<>();

    @Override
    public void onEnchantmentData(final Enchantment enchantment) {
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                synchronized (mEnchantmentCircle) {
                    Polyline circle = mEnchantmentCircle.get(enchantment.id);
                    if (circle != null) {
                        circle.remove();
                    }
                }
                if (enchantment.deleted) {
                    mEnchantmentCircle.remove(enchantment.id);
                } else {
                    if (enchantment.enabled == null || enchantment.enabled || enchantment == focusEnchantment) {
                        Polyline circle = mapboxMap.addPolyline(new PolylineOptions()
                                .addAll(polygonCircleForCoordinate(new LatLng(enchantment.latitude, enchantment.longitude), enchantment.radius))
                                .width(3)
                                .color((enchantment.enabled == null || enchantment.enabled) ? (enchantment.isPublic ? Color.RED : 0xFFFFA500) : Color.GRAY)
                        );
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
            public void onMapReady(MapboxMap mapboxMap) {
                Enchantment exFocusEnchantment = focusEnchantment;
                focusEnchantment = enchantment;
                if (exFocusEnchantment != null) {
                    onEnchantmentData(exFocusEnchantment);
                }
                if (enchantment != null) {
                    onEnchantmentData(enchantment);
                    CameraPosition position = new CameraPosition.Builder()
                            .target(new LatLng(enchantment.latitude, enchantment.longitude))
                            .build();
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), MAP_MOVE_ANIMATION_DURATION);
                }
            }
        });
    }

    final private HashMap<String, MarkerView> mMarkerMarker = new HashMap<>();

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
                m = null;
                if (marker.deleted) {
                    mMarkerMarker.remove(marker.id);
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
                    CameraPosition position = new CameraPosition.Builder()
                            .target(new LatLng(marker.latitude, marker.longitude))
                            .build();
                    mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), MAP_MOVE_ANIMATION_DURATION);
                }
            }
        });
    }

    @Override
    public QuadTree.LatLng getMapCenter() {
        return new QuadTree.LatLng(currentLat, currentLng);
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
                for (POI _result : results) {
                    GooglePOI result = (GooglePOI) _result;
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
            public void onMapReady(MapboxMap mapboxMap) {
                if (mSearchResults == null || position >= mSearchResults.size()) {
                    return;
                }
                POI result = mSearchResults.get(position);
                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(new LatLng(result.latitude, result.longitude))
                        .build();
                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), MAP_MOVE_ANIMATION_DURATION);
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

    private Polygon mEditingEnchantmentCircle = null;
    private MarkerView mEditingMarkerMarker = null;

    @Override
    public void onMapClick(@NonNull LatLng point) {
        if (mPendingPOIMarker != null) {
            mPendingPOIMarker.remove();
        }
        clearAction();
    }

    @Override
    public void onMapLongClick(@NonNull LatLng point) {
        clearAction();
        mEditingLatitude = point.getLatitude();
        mEditingLongitude = point.getLongitude();

        startEditing();
    }

    @Override
    protected void refreshEditing() {
        if (mEditingType == R.string.create_enchantment) {
            mEnchantmentController.setVisibility(View.VISIBLE);
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(MapboxMap mapboxMap) {
                    if (mEditingEnchantmentCircle != null) {
                        mEditingEnchantmentCircle.remove();
                    }
                    mEditingEnchantmentCircle = mapboxMap.addPolygon(new PolygonOptions()
                            .addAll(polygonCircleForCoordinate(new LatLng(mEditingLatitude, mEditingLongitude), Config.ENCHANTMENT_RADIUS[mEditingEnchantmentRadiusIndex]))
                            .strokeColor(mEditingEnchantment.isPublic ? Color.RED : 0xFFA500)
                    );
                }
            });
        } else {
            mEnchantmentController.setVisibility(View.GONE);
            if (mEditingEnchantmentCircle != null) {
                mEditingEnchantmentCircle.remove();
            }
        }
        if (mEditingType == R.string.create_marker) {
            mMarkerController.setVisibility(View.VISIBLE);
            getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(MapboxMap mapboxMap) {
                    if (mEditingMarkerMarker != null) {
                        mEditingMarkerMarker.remove();
                    }
                    MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                            .title(mEditingMarker.name)
                            .position(new LatLng(mEditingLatitude, mEditingLongitude))
                            .icon(iconFactory.fromResource(mEditingMarker.getIconResId()));
//                          .zIndex(1f)

                    mEditingMarkerMarker = mapboxMap.addMarker(markerViewOptions);
                    mapboxMap.selectMarker(mEditingMarkerMarker);
                }
            });

        } else {
            mMarkerController.setVisibility(View.GONE);
            if (mEditingMarkerMarker != null) {
                mEditingMarkerMarker.remove();
            }
        }
    }

    @Override
    public boolean onMarkerClick(@NonNull Marker marker, @NonNull View view, @NonNull MapboxMap.MarkerViewAdapter adapter) {
        Object obj = mMarkerMap.get(marker);
        clickMarker(obj);
        return false;
    }
}