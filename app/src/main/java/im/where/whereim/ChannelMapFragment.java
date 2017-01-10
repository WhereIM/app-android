package im.where.whereim;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.HashMap;

public class ChannelMapFragment extends SupportMapFragment {
    private CoreService.CoreBinder mBinder;

    public ChannelMapFragment() {
        // Required empty public constructor
    }

    private double defaultLat = 0;
    private double defaultLng = 0;

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
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.setMyLocationEnabled(true);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(defaultLat, defaultLng), 15));
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

    private HashMap<String, Circle> mCircleList = new HashMap<>();
    private HashMap<String, Marker> mMarkerList = new HashMap<>();
    public void onMateData(final Models.Mate mate){
        if(mate.latitude==null){
            return;
        }
        getMapAsync(new OnMapReadyCallback() {
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
