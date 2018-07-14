package im.where.whereim;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.TextView;

import java.util.LinkedList;
import java.util.List;

import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.Enchantment;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;
import im.where.whereim.models.POI;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelMapFragment extends AuxFragment implements CoreService.MapDataDelegate {
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

    protected View mMarkerView;
    protected TextView mMarkerViewTitle;


    protected Marker mEditingMarkerOrig = null;
    protected Enchantment mEditingEnchantmentOrig = null;
    protected Marker mEditingMarker = new Marker();
    protected Enchantment mEditingEnchantment = new Enchantment();
    protected Key.MAP_OBJECT mEditingType = null;

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    protected void deinitChannel() {
        resetMap();
    }

    protected abstract void resetMap();

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    abstract protected void refreshEditing();
    protected void onMapLongClick(QuadTree.LatLng location){
        moveTo(location);
        ChannelActivity activity = (ChannelActivity) getActivity();
        activity.showAux(ChannelActivity.AuxComp.MARKER_CREATE);
    }

    public void clickMarker(Object obj) {
        if(obj==null){
            return;
        }
        String title;
        double latitude;
        double longitude;

        if (obj instanceof Mate) {
            Mate mate = (Mate) obj;
            title = mate.getDisplayName();
            latitude = mate.latitude;
            longitude = mate.longitude;


        } else if (obj instanceof im.where.whereim.models.Marker) {
            im.where.whereim.models.Marker marker = (im.where.whereim.models.Marker) obj;
            title = marker.name;
            latitude = marker.latitude;
            longitude = marker.longitude;


        } else if (obj instanceof POI) {
            POI poi = (POI) obj;
            title = poi.name;
            latitude = poi.latitude;
            longitude = poi.longitude;


        } else if (obj instanceof QuadTree.LatLng) {
            QuadTree.LatLng location = (QuadTree.LatLng) obj;
            moveTo(location);

        } else {
            return;
        }
    }
}
