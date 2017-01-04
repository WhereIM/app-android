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
import android.os.Handler;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class CoreService extends Service {
    private final static String TAG = "CoreService";
    public class CoreBinder extends Binder{
        public void clearChannelList(){
            mChannelList.clear();
            mChannelMap.clear();
        }

        public void addChannelListChangedListener(Runnable r){
            synchronized (mChannelListChangedListener){
                mChannelListChangedListener.add(r);
            }
            mHandler.post(r);
        }

        public void removeChannelListChangedListener(Runnable r){
            synchronized (mChannelListChangedListener){
                mChannelListChangedListener.remove(r);
            }
        }

        public List<Models.Channel> getChannelList(){
            return Collections.unmodifiableList(mChannelList);
        }

        public void toggleChannelEnabled(Models.Channel channel){
            if(channel.enable==null){
                return;
            }
            try {
                JSONObject payload = new JSONObject();
                payload.put("channel", channel.id);
                payload.put("enable", !channel.enable);
                channel.enable = null;
                String topic = String.format("client/%s/setting/set", user_id);
                String message = payload.toString();
                mqttManager.publishString(message, topic, AWSIotMqttQos.QOS1);
                Log.e(TAG, "pub "+topic+" "+message);
                notifyChannelListChangedListeners();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void clientInfo(){
            if(!mConnected){
                return;
            }
        }

    };

    private Handler mHandler = new Handler();
    private List<Runnable> mChannelListChangedListener = new ArrayList<>();
    private final IBinder mBinder = new CoreBinder();

    public CoreService() {

    }

    private List<Models.Channel> mChannelList = new ArrayList<>();
    private HashMap<String, Models.Channel> mChannelMap = new HashMap<>();

    private final static String user_id= "a535d42efaff46278c37764f84abcc01";

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

    private boolean mConnected = false;

    private void notifyChannelListChangedListeners(){
        synchronized (mChannelListChangedListener){
            for (Runnable runnable : mChannelListChangedListener) {
                mHandler.post(runnable);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        startLocationService();

        keyStorePath = getFilesDir().getAbsolutePath();

        mqttManager = new AWSIotMqttManager(UUID.randomUUID().toString(), REGION_ID, ACCOUNT_ENDPOINT_PREFIX);
        String cert = assetsFileToString(CERT_FILE);
        String priv = assetsFileToString(PRIVATE_KEY_FILE);
        File keyStoreFile = new File(keyStorePath, KEY_STORE_NAME);
        if(!keyStoreFile.exists() || !AWSIotKeystoreHelper.keystoreContainsAlias(CERT_ID, keyStorePath, KEY_STORE_NAME, KEY_STORE_PASSWORD)){
            AWSIotKeystoreHelper.saveCertificateAndPrivateKey(CERT_ID, cert, priv, keyStorePath, KEY_STORE_NAME, KEY_STORE_PASSWORD);
        }
        Log.e(TAG, "Service Started");
        keyStore = AWSIotKeystoreHelper.getIotKeystore(CERT_ID, keyStorePath, KEY_STORE_NAME, KEY_STORE_PASSWORD);
        mqttManager.connect(keyStore, new AWSIotMqttClientStatusCallback() {
            @Override
            public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
                Log.d(TAG, "AWSIotMqttClientStatus changed: "+status);
                switch (status){
                    case Connected:
                        mqttOnConnected();
                        mConnected = true;
                        break;
                    default:
                        mConnected = false;
                        break;
                }
            }
        });

        return mBinder;
    }

    private void mqttOnConnected(){
        mqttManager.subscribeToTopic(String.format("client/%s/unicast", user_id), AWSIotMqttQos.QOS1, new AWSIotMqttNewMessageCallback() {
            @Override
            public void onMessageArrived(String topic, byte[] data) {
                try {
                    String message = new String(data, "UTF-8");
                    JSONObject payload = new JSONObject(message);
                    mqttClientUnicastHandler(payload.getString("topic"), new JSONObject(payload.getString("message")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        mqttManager.subscribeToTopic(String.format("client/%s/setting/get", user_id), AWSIotMqttQos.QOS1, new AWSIotMqttNewMessageCallback() {
            @Override
            public void onMessageArrived(String topic, byte[] data) {
                try {
                    String message = new String(data, "UTF-8");
                    mqttClientSettingGetHandler(topic, new JSONObject(message));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        Log.e(TAG, "Topics subscribed");
    }

    private void mqttClientUnicastHandler(String topic, JSONObject msg){
        Log.e(TAG, topic+" "+msg.toString());

    }

    private void mqttClientSettingGetHandler(String topic, JSONObject msg){
        try {
            Log.e(TAG, System.currentTimeMillis()+" "+topic+" "+msg.toString());
            Models.Channel channel;
            String channel_id = msg.getString("channel");
            if(mChannelMap.containsKey(channel_id)){
                channel = mChannelMap.get(channel_id);
            }else{
                channel = new Models.Channel();
                mChannelList.add(channel);
                mChannelMap.put(channel_id, channel);
            }
            channel.id = msg.getString("channel");
            channel.channel_name  = msg.optString("channel_name", channel.channel_name);
            channel.name  = msg.optString("name", channel.name);
            if(msg.has("enable")){
                channel.enable = msg.getBoolean("enable");
            }

            notifyChannelListChangedListeners();
        } catch (JSONException e) {
            e.printStackTrace();
        }
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
