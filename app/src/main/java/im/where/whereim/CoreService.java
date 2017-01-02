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

import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.util.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.security.KeyStore;

import static com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback.AWSIotMqttClientStatus.*;

public class CoreService extends Service {
    private final static String TAG = "CoreService";
    public class CoreBinder extends Binder{

    };

    private final IBinder mBinder = new CoreBinder();

    public CoreService() {
    }

    private String CLIENT_ID = "xxxxx";
    private final static String ACCOUNT_ENDPOINT_PREFIX = "a3ftvwpcurxils";
    private final static Region REGION_ID = Region.getRegion(Regions.AP_NORTHEAST_1);

    private static final String CERT_ID = "xxxxx";
    private static final String CERT_FILE = "test1.crt";
    private static final String PRIVATE_KEY_FILE = "test1.key";

    private KeyStore keyStore;
    private static final String KEY_STORE_NAME = "xxxxx.jks";
    private static final String KEY_STORE_PASSWORD = "xxxxx";
    private String keyStorePath;

    private AWSIotMqttManager mqttManager;

    @Override
    public IBinder onBind(Intent intent) {
        startLocationService();

        keyStorePath = getFilesDir().getAbsolutePath();

        mqttManager = new AWSIotMqttManager(CLIENT_ID, REGION_ID, ACCOUNT_ENDPOINT_PREFIX);
        String cert = assetsFileToString(CERT_FILE);
        String priv = assetsFileToString(PRIVATE_KEY_FILE);
        File keyStoreFile = new File(keyStorePath, KEY_STORE_NAME);
        if(!keyStoreFile.exists() || !AWSIotKeystoreHelper.keystoreContainsAlias(CERT_ID, keyStorePath, KEY_STORE_NAME, KEY_STORE_PASSWORD)){
            AWSIotKeystoreHelper.saveCertificateAndPrivateKey(CERT_ID, cert, priv, keyStorePath, KEY_STORE_NAME, KEY_STORE_PASSWORD);
        }
        keyStore = AWSIotKeystoreHelper.getIotKeystore(CERT_ID, keyStorePath, KEY_STORE_NAME, KEY_STORE_PASSWORD);
        mqttManager.connect(keyStore, new AWSIotMqttClientStatusCallback() {
            @Override
            public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
                Log.d(TAG, "AWSIotMqttClientStatus changed: "+status);
                switch (status){
                    case Connected:
                        mqttManager.publishString("lala", "test", AWSIotMqttQos.QOS1);
                        mqttManager.subscribeToTopic("lala", AWSIotMqttQos.QOS1, new AWSIotMqttNewMessageCallback() {
                            @Override
                            public void onMessageArrived(String topic, byte[] data) {
                                Log.e(TAG, topic+" "+new String(data));
                            }
                        });
                        break;
                }
            }
        });

        return mBinder;
    }

    private String assetsFileToString(String path){
        try {
            InputStream json = getAssets().open(path);
            String ret = IOUtils.toString(json);
            json.close();
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
