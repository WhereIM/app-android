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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
            public void onMapReady(GoogleMap googleMap) {
                googleMap.setMyLocationEnabled(true);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(defaultLat, defaultLng), 15));

                googleMap.setOnMapLongClickListener(ChannelGoogleMapFragment.this);
            }
        });

        return view;
    }

    private Models.Channel mChannel;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMarkerView = LayoutInflater.from(context).inflate(R.layout.map_mate, null);
        mMarkerViewTitle = (TextView) mMarkerView.findViewById(R.id.title);
        postBinderTask(new Models.BinderTask() {
            @Override
            public void onBinderReady(final CoreService.CoreBinder binder) {
                getChannel(new ChannelActivity.GetChannelCallback() {
                    @Override
                    public void onGetChannel(Models.Channel channel) {
                        mChannel = channel;
                        binder.openMap(channel, ChannelGoogleMapFragment.this);
                    }
                });
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        postBinderTask(new Models.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.closeMap(mChannel, ChannelGoogleMapFragment.this);
            }
        });
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

    private Marker mMockMarker;
    private HashMap<String, Circle> mMateCircle = new HashMap<>();
    private HashMap<String, Marker> mMateMarker = new HashMap<>();

    @Override
    public void onMockData(final Models.Mate mock) {
        postMapTask(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                if(mMockMarker!=null){
                    mMockMarker.remove();
                }
                if(mock==null){
                    return;
                }
                postBinderTask(new Models.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        if(mChannel==null){
                            mMarkerViewTitle.setText(null);
                        }else{
                            Models.Mate m = binder.getChannelMate(mChannel.id, mChannel.mate_id);
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
    public void onMateData(final Models.Mate mate){
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
                Circle circle = googleMap.addCircle(new CircleOptions()
                        .center(new LatLng(mate.latitude, mate.longitude))
                        .radius(mate.accuracy)
                        .strokeColor(Color.BLUE));
                synchronized (mMateCircle) {
                    mMateCircle.put(mate.id, circle);
                }

                synchronized (mMateMarker) {
                    Marker marker = mMateMarker.get(mate.id);
                    if(marker!=null){
                        marker.remove();
                    }
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

                synchronized (mMateMarker){
                    mMateMarker.put(mate.id, marker);
                }
            }
        });
    }

    final private HashMap<String, Circle> mEnchantmentCircle = new HashMap<>();

    @Override
    public void onEnchantmentData(final Models.Enchantment enchantment){
        postMapTask(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                synchronized (mEnchantmentCircle) {
                    Circle circle = mEnchantmentCircle.get(enchantment.id);
                    if(circle!=null){
                        circle.remove();
                    }
                }
                Circle circle = googleMap.addCircle(new CircleOptions()
                        .center(new LatLng(enchantment.latitude, enchantment.longitude))
                        .radius(enchantment.radius)
                        .strokeColor(enchantment.enable?Color.RED:Color.YELLOW));
                synchronized (mEnchantmentCircle) {
                    mEnchantmentCircle.put(enchantment.id, circle);
                }
            }
        });
    }

    final private HashMap<String, Marker> mMarkerMarker = new HashMap<>();

    @Override
    public void onMarkerData(final Models.Marker marker) {
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
                m = googleMap.addMarker(
                        new MarkerOptions()
                                .position(new LatLng(marker.latitude, marker.longitude))
                );

                synchronized (mMarkerMarker){
                    mMarkerMarker.put(marker.id, m);
                }
            }
        });
    }

    private Circle mEditingEnchantmentCircle = null;
    private Marker mEditingMarker = null;
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
                            .strokeColor(Color.RED));
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
                    if(mEditingMarker !=null){
                        mEditingMarker.remove();
                    }
                    mEditingMarker = googleMap.addMarker(
                            new MarkerOptions()
                                    .position(new LatLng(mEditingLatitude, mEditingLongitude))
                    );
                }
            });

        }else{
            mMarkerController.setVisibility(View.GONE);
            if(mEditingMarker!=null){
                mEditingMarker.remove();
            }
        }
    }
}
