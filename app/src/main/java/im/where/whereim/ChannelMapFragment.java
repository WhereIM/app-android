package im.where.whereim;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class ChannelMapFragment extends SupportMapFragment {
    private CoreService.CoreBinder mBinder;

    public ChannelMapFragment() {
        // Required empty public constructor
    }

    private double defaultLat = 0;
    private double defaultLng = 0;

    Bitmap mMarkerBitmap;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMarkerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.marker_mate);

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
        mMarkerBitmap.recycle();
        mMarkerBitmap = null;
        super.onDestroy();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mBinder = ((ChannelActivity) context).getBinder();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mBinder = null;
    }

    private HashMap<String, Circle> mCircleList = new HashMap<>();
    private HashMap<String, Marker> mMarkerList = new HashMap<>();
    public void onMapData(final JSONObject data){
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                try {
                    String mate_id = data.getString("mate");

                    synchronized (mCircleList) {
                        Circle circle = mCircleList.get(mate_id);
                        if(circle!=null){
                            circle.remove();
                        }
                    }
                    double lat = data.getDouble("lat");
                    double lng = data.getDouble("lng");
                    double acc = data.getDouble("acc");
                    Circle circle = googleMap.addCircle(new CircleOptions()
                            .center(new LatLng(lat, lng))
                            .radius(acc)
                            .strokeColor(Color.RED));
                    synchronized (mCircleList) {
                        mCircleList.put(mate_id, circle);
                    }

                    synchronized (mMarkerList) {
                        Marker marker = mMarkerList.get(mate_id);
                        if(marker!=null){
                            marker.remove();
                        }
                    }
                    Marker marker = googleMap.addMarker(
                        new MarkerOptions()
                            .position(new LatLng(lat, lng))
                            .title(mate_id)
                            .anchor(0.5f, 1f)
                            .icon(BitmapDescriptorFactory.fromBitmap(mMarkerBitmap))
                    );
                    synchronized (mMarkerList){
                        mMarkerList.put(mate_id, marker);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
