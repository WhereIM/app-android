package im.where.whereim;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

/**
 * Created by buganini on 19/01/17.
 */

public class MapboxPoiViewerFragment extends BaseFragment {
    protected Handler mHandler = new Handler();

    private MapView mMapView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Mapbox.getInstance(getActivity(), Config.API_KEY_MAPBOX);

        View view = inflater.inflate(R.layout.fragment_poi_viewer_mapbox, container, false);
        mMapView = (MapView) view.findViewById(R.id.map);

        mMapView.onCreate(savedInstanceState);

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(final MapboxMap mapboxMap) {
                PoiViewerActivity activity = (PoiViewerActivity) getActivity();
                mapboxMap.setMyLocationEnabled(true);
                mapboxMap.setAllowConcurrentMultipleOpenInfoWindows(false);
                mapboxMap.getUiSettings().setCompassGravity(Gravity.LEFT|Gravity.TOP);

                LatLng latlng = new LatLng(activity.poi.latitude, activity.poi.longitude);

                CameraPosition position = new CameraPosition.Builder()
                        .target(latlng)
                        .zoom(13)
                        .build();

                mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position), 0);


                IconFactory iconFactory = IconFactory.getInstance(activity);

                MarkerViewOptions markerViewOptions = new MarkerViewOptions()
                        .title(activity.poi.name)
                        .position(latlng)
                        .icon(iconFactory.fromResource(im.where.whereim.models.Marker.getIconResource("red")));

                MarkerView m = mapboxMap.addMarker(markerViewOptions);
                mapboxMap.selectMarker(m);
            }
        });
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();

        if(mMapView!=null)
            mMapView.onStart();
    }

    @Override
    public void onStop() {
        if(mMapView!=null)
            mMapView.onStop();

        super.onStop();
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