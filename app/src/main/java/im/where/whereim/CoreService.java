package im.where.whereim;

import android.Manifest;
import android.app.Notification;
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
import android.support.v4.app.NotificationCompat;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CoreService extends Service {
    private final static String TAG = "CoreService";
    public class CoreBinder extends Binder{
        public void clearChannelList(){
            mChannelList.clear(); // minor leak in mChannelMap
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
            synchronized (mChannelList) {
                return Collections.unmodifiableList(mChannelList);
            }
        }

        public Models.Channel getChannelById(String id){
            synchronized (mChannelList){
                for (Models.Channel channel : mChannelList) {
                    if(channel.id.equals(id))
                        return channel;
                }
            }
            return null;
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
                String topic = String.format("client/%s/setting/set", client_id);
                String message = payload.toString();
                mqttManager.publishString(message, topic, AWSIotMqttQos.QOS1);
                Log.e(TAG, "Publish "+topic+" "+message);
                notifyChannelListChangedListeners();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public boolean openChannel(Models.Channel channel){
            if(channel==null)
                return false;
            String topic = String.format("channel/%s/get/+", channel.id);
            Log.e(TAG, "Subscribe "+topic);
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS1, new AWSIotMqttNewMessageCallback() {
                @Override
                public void onMessageArrived(String topic, byte[] data) {
                    try {
                        mqttChannelGetHandler(topic, new JSONObject(new String(data, "UTF-8")));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            return true;
        }

        public void closeChannel(Models.Channel channel){
            if(channel==null)
                return;
            mqttManager.unsubscribeTopic(String.format("channel/%s/get/+", channel.id));
        }

        public void checkLocationService(){
            _checkLocationService();
        }
    };

    private Handler mHandler = new Handler();
    private List<Runnable> mChannelListChangedListener = new ArrayList<>();
    private final IBinder mBinder = new CoreBinder();

    public CoreService() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        keyStorePath = getFilesDir().getAbsolutePath();

        mqttManager = new AWSIotMqttManager(client_id, REGION_ID, ACCOUNT_ENDPOINT_PREFIX);
        mqttManager.setAutoReconnect(true);
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
                        mMqttConnected = true;
                        break;
                    default:
                        mMqttConnected = false;
                        break;
                }
            }
        });
        return super.onStartCommand(intent, flags, startId);
    }

    private List<Models.Channel> mChannelList = new ArrayList<>();
    private HashMap<String, Models.Channel> mChannelMap = new HashMap<>();

    private final static String client_id = "a535d42efaff46278c37764f84abcc01";

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

    private boolean mMqttConnected = false;
    private boolean mIsForeground = false;

    private void notifyChannelListChangedListeners(){
        synchronized (mChannelListChangedListener){
            for (Runnable runnable : mChannelListChangedListener) {
                mHandler.post(runnable);
            }
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                _checkLocationService();
            }
        });
    }

    private void _checkLocationService(){
        boolean pending = false;
        int enableCount = 0;
        synchronized (mChannelList){
            for (Models.Channel channel : mChannelList) {
                if(channel.enable==null){
                    pending = true;
                    break;
                }
                if(channel.enable)
                    enableCount += 1;
            }
        }
        if(!pending) {
            if(enableCount>0){
                if(!mIsForeground){
                    mIsForeground = true;
                    Notification notification = new NotificationCompat.Builder(this)
                            .setContentTitle(getResources().getString(R.string.app_name))
                            .setSmallIcon(R.mipmap.ic_launcher)
                            .build();

                    startForeground(1, notification);
                }
                startLocationService();
            }else if(enableCount==0){
                stopLocationService();
                if(mIsForeground){
                    mIsForeground = false;
                    stopForeground(true);
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private void mqttOnConnected(){
        Log.e(TAG, "mqttOnConnected");
        String topic;

        topic = String.format("client/%s/unicast", client_id);
        Log.e(TAG, "Subscribe "+topic);
        mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS1, new AWSIotMqttNewMessageCallback() {
            @Override
            public void onMessageArrived(String topic, byte[] data) {
                try {
                    String message = new String(data, "UTF-8");
                    Log.e(TAG, "Receive "+topic+" "+message);
                    JSONObject payload = new JSONObject(message);
                    mqttClientUnicastHandler(payload.getString("topic"), new JSONObject(payload.getString("message")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        topic = String.format("client/%s/setting/get", client_id);
        Log.e(TAG, "Subscribe "+topic);
        mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS1, new AWSIotMqttNewMessageCallback() {
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
    }

    private Pattern mChannelGetPattern = Pattern.compile("^channel/([A-Fa-f0-9]{32})/get/([0-9]+)$");
    private void mqttClientUnicastHandler(String topic, JSONObject msg){
        Matcher m;
        m = mChannelGetPattern.matcher(topic);
        if(m.matches()){
            mqttChannelGetHandler(topic, msg);
            return;
        }
    }

    private void mqttClientSettingGetHandler(String topic, JSONObject msg){
        try {
            Log.e(TAG, "Receive "+topic+" "+msg.toString());
            Models.Channel channel;
            String channel_id = msg.getString("channel");
            if(mChannelMap.containsKey(channel_id)){
                channel = mChannelMap.get(channel_id);
            }else{
                channel = new Models.Channel();
                mChannelMap.put(channel_id, channel);
            }
            if(!mChannelList.contains(channel)){
                mChannelList.add(channel);
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

    private void mqttChannelGetHandler(String topic, JSONObject msg){
        Log.e(TAG, "Receive "+topic+" "+msg.toString());
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

    private boolean mLocationServiceRunning = false;
    private void startLocationService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if(mLocationServiceRunning)
            return;
        mLocationServiceRunning = true;
        Log.e(TAG, "startLocationService");
        mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        startGPSListener();
        startNetworkListener();
    }

    private void stopLocationService(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        if(!mLocationServiceRunning)
            return;
        mLocationServiceRunning = false;
        Log.e(TAG, "stopLocationService");
        mLocationManager.removeUpdates(mGpsLocationListener);
        mLocationManager.removeUpdates(mNetworkLocationListener);
    }

    private LocationListener mGpsLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            processLocation("GPS", location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (LocationProvider.OUT_OF_SERVICE == status) {
                Log.e(TAG, "GPS provider out of service");
            }

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private LocationListener mNetworkLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            processLocation("NETWORK", location);
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
    };

    private void startGPSListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 5, mGpsLocationListener);
    }

    private void startNetworkListener(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 5, mNetworkLocationListener);
    }

    private void processLocation(String provider, Location loc){
        if(!isBetterLocation(loc, lastBestLocation)){
            return;
        }
        lastBestLocation = loc;
        try {
            JSONObject msg = new JSONObject();
            msg.put("lat", loc.getLatitude());
            msg.put("lng", loc.getLongitude());
            if(loc.hasAccuracy()){
                msg.put("acc", loc.getAccuracy());
            }
            if(loc.hasAltitude()){
                msg.put("alt", loc.getAltitude());
            }
            if(loc.hasBearing()){
                msg.put("bear", loc.getBearing());
            }
            if(loc.hasSpeed()){
                msg.put("spd", loc.getSpeed());
            }
            msg.put("time", System.currentTimeMillis());
            msg.put("pvdr", provider);
            String topic = String.format("client/%s/info", client_id);
            String message = msg.toString();
            mqttManager.publishString(message, topic, AWSIotMqttQos.QOS1);
            Log.e(TAG, "Publish "+topic+" "+message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Location lastBestLocation = null;

    private static final int EXPIRATION = 1000 * 60 * 1; // 1 min

    /** Determines whether one Location reading is better than the current Location fix
     * @param location  The new Location that you want to evaluate
     * @param currentBestLocation  The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > EXPIRATION;
        boolean isSignificantlyOlder = timeDelta < -EXPIRATION;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

}
