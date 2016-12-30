package im.where.whereim;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class CoreService extends Service {
    private final static String TAG = "CoreService";
    public class CoreBinder extends Binder{

    };

    private final IBinder mBinder = new CoreBinder();

    public CoreService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        startLocationService();
        return mBinder;
    }

    private LocationManager mLocationManager;

    private void startLocationService() {
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        startGPSListener();
        startNetworkListener();
    }

    private void startGPSListener() {
        Log.e("lala", "startGPSListener");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.e("lala", "startGPSListener checked");
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.e("lala", "Accuracy: "+location.getAccuracy());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                if (LocationProvider.OUT_OF_SERVICE == status) {
                    Log.e(TAG, "GPS provider out of service");
                    startNetworkListener();
                }

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        });
    }

    private void startNetworkListener(){
        Log.e("lala", "startNetworkListener");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Log.e("lala", "startNetworkListener checked");
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 5, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.e("lala", "Accuracy: "+location.getAccuracy());
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                if (LocationProvider.OUT_OF_SERVICE == status) {
                    Log.e(TAG, "Network provider out of service");
                }

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        });
    }
}
