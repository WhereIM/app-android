package im.where.whereim;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import android.view.View;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;
import im.where.whereim.models.POI;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelMapFragment extends BaseChannelFragment implements CoreService.MapDataDelegate {
    public static ChannelMapFragment newFragment(Context context){
        switch(Config.getMapProvider(context)){
            case GOOGLE:
                return new ChannelGoogleMapFragment();
            case MAPBOX:
                return new ChannelMapboxFragment();
        }
        return null;
    }

    protected Handler mHandler = new Handler();

    final private List<Runnable> mPendingLocationServiceTask = new LinkedList<>();

    abstract Config.MapProvider getProvider();

    public void onLocationServiceReady(){
        processLocationServiceTask();
    }

    protected void postLocationServiceTask(Runnable task){
        synchronized (mPendingLocationServiceTask) {
            mPendingLocationServiceTask.add(task);
        }
        processLocationServiceTask();
    }


    private void processLocationServiceTask() {
        if (channelActivity ==null || ActivityCompat.checkSelfPermission(channelActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        while(true){
            Runnable task = null;
            synchronized (mPendingLocationServiceTask){
                if(mPendingLocationServiceTask.size()>0){
                    task = mPendingLocationServiceTask.remove(0);
                }
            }
            if(task==null){
                break;
            }else{
                task.run();
            }
        }
    }
    protected double defaultLat = 0;
    protected double defaultLng = 0;
    protected float defaultZoom = 0;

    protected double currentLat = 0;
    protected double currentLng = 0;

    protected View mCrosshair;
    protected View mMarkerView;
    protected TextView mMarkerViewTitle;

    protected Marker mEditingMarker = new Marker();
    protected Key.MAP_OBJECT mEditingType = null;

    public void setCrosshair(boolean display){
        mCrosshair.setVisibility(display ? View.VISIBLE : View.GONE);
    }

    @Override
    protected void deinitChannel() {
        resetMap();
    }

    protected void moveTo(QuadTree.LatLng location){
        defaultLat = location.latitude;
        defaultLng = location.longitude;
        defaultZoom = 13;
    }
    protected abstract void resetMap();

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mCrosshair = view.findViewById(R.id.crosshair);
        mCrosshair.setVisibility(View.GONE);
        getChannel(new BaseChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(Channel channel) {
                setCrosshair(channelActivity.showCrosshair());
                setPOI(channelActivity.getPoi());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public QuadTree.LatLng getMapCenter() {
        return new QuadTree.LatLng(currentLat, currentLng);
    }

    abstract public void setPOI(POI poi);
    protected void onMapClick(QuadTree.LatLng location){
        channelActivity.clearFocus();
    }

    protected void onMapLongClick(QuadTree.LatLng location){
        if(!channelActivity.isLocked()){
            moveTo(location);
            channelActivity.showPane(ChannelActivity.PaneComp.MARKER_CREATE);
        }
    }

    public void clickMarker(Object obj) {
        if(obj==null){
            return;
        }
        if(channelActivity.isLocked()){
            return;
        }
        if (obj instanceof Mate) {
            Mate mate = (Mate) obj;
        } else if (obj instanceof im.where.whereim.models.Marker) {
            im.where.whereim.models.Marker marker = (im.where.whereim.models.Marker) obj;
            Bundle data = new Bundle();
            data.putString(PaneMarkerView.FIELD_ID, marker.id);
            channelActivity.startPane(PaneMarkerView.class, data);
        } else if (obj instanceof POI) {
            POI poi = (POI) obj;
            moveTo(new QuadTree.LatLng(poi.latitude, poi.longitude));
            Bundle data = new Bundle();
            data.putString(PaneMarkerEdit.FIELD_ID, null);
            data.putDouble(PaneMarkerEdit.FIELD_LAT, poi.latitude);
            data.putDouble(PaneMarkerEdit.FIELD_LNG, poi.longitude);
            data.putString(PaneMarkerEdit.FIELD_NAME, poi.name);
            data.putString(PaneMarkerEdit.FIELD_COLOR, null);
            data.putInt(PaneMarkerEdit.FIELD_RADIUS, Config.DEFAULT_GEOFENCE_RADIUS);
            data.putBoolean(PaneMarkerEdit.FIELD_GEOFENCE, false);
            channelActivity.startPane(PaneMarkerEdit.class, data);
        } else {
            return;
        }
    }
}
