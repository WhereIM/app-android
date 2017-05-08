package im.where.whereim;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Created by buganini on 19/01/17.
 */

public class GooglePoiViewerFragment extends BaseFragment {
    protected Handler mHandler = new Handler();

    private MapView mMapView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_poi_viewer_googlemap, container, false);
        mMapView = (MapView) view.findViewById(R.id.map);

        MapsInitializer.initialize(getActivity());

        mMapView.onCreate(savedInstanceState);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final GoogleMap googleMap) {
                PoiViewerActivity activity = (PoiViewerActivity) getActivity();
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                googleMap.setMyLocationEnabled(true);

                LatLng latlng = new LatLng(activity.poi.latitude , activity.poi.longitude);
                Marker m = googleMap.addMarker(
                        new MarkerOptions()
                                .title(activity.poi.name)
                                .position(latlng)
                                .icon(im.where.whereim.models.Marker.getIconBitmapDescriptor(im.where.whereim.models.Marker.getIconResource("res")))
                                .anchor(0.5f, 1)
                );

                m.showInfoWindow();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latlng, 13));
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

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

}