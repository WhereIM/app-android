package im.where.whereim;

import android.content.Context;
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
import im.where.whereim.models.Mate;

public class ChannelGoogleMapFragment extends ChannelMapFragment implements GoogleMap.OnMapLongClickListener {
    public ChannelGoogleMapFragment() {
        // Required empty public constructor
    }

    private List<OnMapReadyCallback> mPendingTask = new ArrayList<>();

    protected void postMapTask(OnMapReadyCallback task){
        synchronized (mPendingTask) {
            mPendingTask.add(task);
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

    private MapView mMapView;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        Location locationByGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        Location locationByNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        long positionTime = 0;
        if(locationByNetwork != null){
            positionTime = locationByNetwork.getTime();
            defaultLat = locationByNetwork.getLatitude();
            defaultLng = locationByNetwork.getLongitude();
        }
        if(locationByGPS != null && locationByGPS.getTime() > positionTime){
            defaultLat = locationByGPS.getLatitude();
            defaultLng = locationByGPS.getLongitude();
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
                googleMap.setMyLocationEnabled(true);
                googleMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
                    @Override
                    public void onCameraIdle() {
                        cameraMoved(googleMap);
                    }
                });
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(defaultLat, defaultLng), 15));
                googleMap.setOnMapLongClickListener(ChannelGoogleMapFragment.this);
            }
        });

        return view;
    }

    final ArrayList<Polyline> lines = new ArrayList<>();
    private void cameraMoved(GoogleMap googleMap){
        float zoom = googleMap.getCameraPosition().zoom-1;
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
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(final CoreService.CoreBinder binder) {
                getChannel(new ChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(Channel channel) {
                        mChannel = channel;
                        binder.openMap(channel, ChannelGoogleMapFragment.this);
                        binder.addChannelChangedListener(mChannel.id, channedChangedListener);
                    }
                });
            }
        });
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
    }

    @Override
    public void onPause() {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.closeMap(mChannel, ChannelGoogleMapFragment.this);
                binder.removeChannelChangedListener(mChannel.id, channedChangedListener);
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
    public void onDestroyView() {
        if(mMapView!=null)
            mMapView.onDestroy();
        super.onDestroyView();
    }

    private Mate selfMate = null;
    private Marker mMockMarker;
    private Circle mRadiusCircle = null;
    private HashMap<String, Circle> mMateCircle = new HashMap<>();
    private HashMap<String, Marker> mMateMarker = new HashMap<>();

    @Override
    public void onMockData(final Mate mock) {
        postMapTask(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                if(mMockMarker!=null){
                    mMockMarker.remove();
                }
                if(mock==null){
                    return;
                }
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        if(mChannel==null){
                            mMarkerViewTitle.setText(null);
                        }else{
                            Mate m = binder.getChannelMate(mChannel.id, mChannel.mate_id);
                            mMarkerViewTitle.setText(m.getDisplayName());
                        }
                        mMarkerView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        mMarkerView.layout(0, 0, mMarkerView.getMeasuredWidth(), mMarkerView.getMeasuredHeight());
                        mMarkerView.setDrawingCacheEnabled(true);
                        mMarkerView.buildDrawingCache();

                        mMockMarker = googleMap.addMarker(
                                new MarkerOptions()
                                        .position(new LatLng(mock.latitude, mock.longitude))
                                        .alpha(0.3f)
                                        .anchor(0.5f, 1f)
                                        .icon(BitmapDescriptorFactory.fromBitmap(mMarkerView.getDrawingCache()))
                        );
                    }
                });
            }
        });
    }

    @Override
    public void onMateData(final Mate mate){
        if(mate.latitude==null){
            return;
        }
        postMapTask(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                synchronized (mMateCircle) {
                    Circle circle = mMateCircle.get(mate.id);
                    if(circle!=null){
                        circle.remove();
                    }
                }

                synchronized (mMateMarker) {
                    Marker marker = mMateMarker.get(mate.id);
                    if(marker!=null){
                        marker.remove();
                    }
                }

                if(mate.deleted){
                    mMateCircle.remove(mate.id);
                    mMateMarker.remove(mate.id);
                }else {
                    Circle circle = googleMap.addCircle(new CircleOptions()
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

                    Marker marker = googleMap.addMarker(
                            new MarkerOptions()
                                    .position(new LatLng(mate.latitude, mate.longitude))
                                    .anchor(0.5f, 1f)
                                    .icon(BitmapDescriptorFactory.fromBitmap(mMarkerView.getDrawingCache()))
                    );

                    synchronized (mMateMarker) {
                        mMateMarker.put(mate.id, marker);
                    }
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
        postMapTask(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                if(mRadiusCircle!=null){
                    mRadiusCircle.remove();
                    mRadiusCircle = null;
                }
                if(mChannel.enable_radius!=null && mChannel.enable_radius) {
                    int color;
                    if (mChannel.enable) {
                        color = Color.MAGENTA;
                    } else {
                        color = Color.GRAY;
                    }
                    mRadiusCircle = googleMap.addCircle(new CircleOptions()
                            .center(new LatLng(selfMate.latitude, selfMate.longitude))
                            .radius(mChannel.radius)
                            .strokeWidth(5)
                            .strokeColor(color));
                }
            }
        });
    }

    @Override
    public void moveToMate(final Mate mate) {
        if(mate.latitude==null){
            return;
        }
        if(mMapView!=null){
            mMapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(mate.latitude, mate.longitude)));
                }
            });
        }
    }

    final private HashMap<String, Circle> mEnchantmentCircle = new HashMap<>();

    @Override
    public void onEnchantmentData(final Enchantment enchantment){
        postMapTask(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                synchronized (mEnchantmentCircle) {
                    Circle circle = mEnchantmentCircle.get(enchantment.id);
                    if(circle!=null){
                        circle.remove();
                    }
                }
                if(enchantment.deleted){
                    mEnchantmentCircle.remove(enchantment.id);
                }else {
                    if (enchantment.enable == null || enchantment.enable) {
                        Circle circle = googleMap.addCircle(new CircleOptions()
                                .center(new LatLng(enchantment.latitude, enchantment.longitude))
                                .radius(enchantment.radius)
                                .strokeWidth(3)
                                .strokeColor(enchantment.isPublic ? Color.RED : 0xFFFFA500));
                        synchronized (mEnchantmentCircle) {
                            mEnchantmentCircle.put(enchantment.id, circle);
                        }
                    }
                }
            }
        });
    }

    final private HashMap<String, Marker> mMarkerMarker = new HashMap<>();

    @Override
    public void onMarkerData(final im.where.whereim.models.Marker marker) {
        postMapTask(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                Marker m;
                synchronized (mMarkerMarker) {
                    m = mMarkerMarker.get(marker.id);
                    if(m!=null){
                        m.remove();
                    }
                }
                if(marker.deleted){
                    mMarkerMarker.remove(marker.id);
                }else {
                    if (marker.enable == null || marker.enable) {
                        m = googleMap.addMarker(
                                new MarkerOptions()
                                        .title(marker.name)
                                        .position(new LatLng(marker.latitude, marker.longitude))
                                        .icon(marker.getIconBitmapDescriptor())
                                        .anchor(0.5f, 1)
                        );

                        synchronized (mMarkerMarker) {
                            mMarkerMarker.put(marker.id, m);
                        }

                    }
                }
            }
        });
    }

    @Override
    public void moveToMarker(final im.where.whereim.models.Marker marker) {
        if(mMapView!=null){
            mMapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    googleMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(marker.latitude, marker.longitude)));
                    Marker m = mMarkerMarker.get(marker.id);
                    if(m!=null) {
                        m.showInfoWindow();
                    }
                }
            });
        }
    }

    private final HashMap<String, Marker> mAdMarkers = new HashMap<>();
    @Override
    public void onMapAd(final HashMap<String, Ad> ads) {
        postMapTask(new OnMapReadyCallback() {
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
        mEditingLatitude = latLng.latitude;
        mEditingLongitude = latLng.longitude;

        startEditing();
    }

    @Override
    protected void refreshEditing(){
        if(mEditingType==R.string.create_enchantment){
            mEnchantmentController.setVisibility(View.VISIBLE);
            mMapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if(mEditingEnchantmentCircle !=null){
                        mEditingEnchantmentCircle.remove();
                    }
                    mEditingEnchantmentCircle = googleMap.addCircle(new CircleOptions()
                            .center(new LatLng(mEditingLatitude, mEditingLongitude))
                            .radius(Config.ENCHANTMENT_RADIUS[mEditingEnchantmentRadiusIndex])
                            .strokeWidth(5)
                            .strokeColor(mEditingEnchantment.isPublic ? Color.RED : 0xFFFFA500));
                }
            });
        }else{
            mEnchantmentController.setVisibility(View.GONE);
            if(mEditingEnchantmentCircle!=null){
                mEditingEnchantmentCircle.remove();
            }
        }
        if(mEditingType==R.string.create_marker){
            mMarkerController.setVisibility(View.VISIBLE);
            mMapView.getMapAsync(new OnMapReadyCallback() {
                @Override
                public void onMapReady(GoogleMap googleMap) {
                    if(mEditingMarkerMarker !=null){
                        mEditingMarkerMarker.remove();
                    }
                    mEditingMarkerMarker = googleMap.addMarker(
                            new MarkerOptions()
                                    .position(new LatLng(mEditingLatitude, mEditingLongitude))
                                    .icon(mEditingMarker.getIconBitmapDescriptor())
                                    .anchor(0.5f, 1f)
                    );
                }
            });

        }else{
            mMarkerController.setVisibility(View.GONE);
            if(mEditingMarkerMarker !=null){
                mEditingMarkerMarker.remove();
            }
        }
    }
}
