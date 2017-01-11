package im.where.whereim;

import android.content.Context;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ChannelMapFragment extends Fragment implements GoogleMap.OnMapLongClickListener {
    private CoreService.CoreBinder mBinder;

    public ChannelMapFragment() {
        // Required empty public constructor
    }

    private List<OnMapReadyCallback> mPendingTask = new ArrayList<>();

    protected void postMapTask(OnMapReadyCallback task){
        synchronized (mPendingTask) {
            mPendingTask.add(task);
        }
        processMapTask();
    }


    private void processMapTask() {
        if(mMapView==null){
            return;
        }
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

    private double defaultLat = 0;
    private double defaultLng = 0;

    private MapView mMapView;
    private View mMarkerView;
    private TextView mMarkerViewTitle;
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_map, container, false);

//        MapsInitializer.initialize(getActivity());

        mMapView = (MapView) view.findViewById(R.id.map);
        mMapView.onCreate(savedInstanceState);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.setMyLocationEnabled(true);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(defaultLat, defaultLng), 15));

                googleMap.setOnMapLongClickListener(ChannelMapFragment.this);
            }
        });

        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mMarkerView = LayoutInflater.from(context).inflate(R.layout.map_mate, null);
        mMarkerViewTitle = (TextView) mMarkerView.findViewById(R.id.title);

        mBinder = ((ChannelActivity) context).getBinder();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBinder = null;
        mMarkerView = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        mMapView.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mMapView.onDestroy();
        super.onDestroy();
    }


    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        mMapView.onLowMemory();
        super.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        mMapView.onDestroy();
        super.onDestroyView();
    }

    private HashMap<String, Circle> mCircleList = new HashMap<>();
    private HashMap<String, Marker> mMarkerList = new HashMap<>();
    public void onMateData(final Models.Mate mate){
        if(mate.latitude==null){
            return;
        }

        postMapTask(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                synchronized (mCircleList) {
                    Circle circle = mCircleList.get(mate.id);
                    if(circle!=null){
                        circle.remove();
                    }
                }
                Circle circle = googleMap.addCircle(new CircleOptions()
                        .center(new LatLng(mate.latitude, mate.longitude))
                        .radius(mate.accuracy)
                        .strokeColor(Color.RED));
                synchronized (mCircleList) {
                    mCircleList.put(mate.id, circle);
                }

                synchronized (mMarkerList) {
                    Marker marker = mMarkerList.get(mate.id);
                    if(marker!=null){
                        marker.remove();
                    }
                }

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

                synchronized (mMarkerList){
                    mMarkerList.put(mate.id, marker);
                }
            }
        });
    }
}
