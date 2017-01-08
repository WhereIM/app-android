package im.where.whereim;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.iot.AWSIotKeystoreHelper;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;
import com.amazonaws.regions.Region;
import com.amazonaws.util.IOUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

public class CoreService extends Service {
    private final static String TAG = "CoreService";
    public interface MapDataReceiver{
        public void onMapData(JSONObject data);
    };

    public class CoreBinder extends Binder{
        public String getClientId(){
            return mClientId;
        }

        public void register_client(final String provider, final String auth_id, final String name, final Runnable callback){
            new Thread(){
                @Override
                public void run() {
                    try {
                        JSONObject payload = new JSONObject();
                        payload.put("auth_provider", provider);
                        payload.put("auth_id", auth_id);
                        payload.put("name", name);

                        HttpsURLConnection conn;
                        InputStream is;

                        URL url = new URL(Config.AWS_API_GATEWAY_REGISTER_CLIENT);
                        conn = (HttpsURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setUseCaches(false);
                        conn.setDoInput(true);
                        conn.setDoOutput(true);

                        OutputStream os = conn.getOutputStream();
                        os.write(payload.toString().getBytes());
                        os.close();

                        is = conn.getInputStream();
                        String json = IOUtils.toString(is);
                        is.close();
                        conn.disconnect();

                        JSONObject res = new JSONObject(json);

                        URL key_url = new URL(res.getString("key"));
                        conn = (HttpsURLConnection) key_url.openConnection();
                        is = conn.getInputStream();
                        String key_str = IOUtils.toString(is);
                        is.close();
                        conn.disconnect();

                        URL crt_url = new URL(res.getString("crt"));
                        conn = (HttpsURLConnection) crt_url.openConnection();
                        is = conn.getInputStream();
                        String crt_str = IOUtils.toString(is);
                        is.close();
                        conn.disconnect();

                        File keyStoreFile = new File(keyStorePath, Config.KEY_STORE_NAME);
                        if(keyStoreFile.exists()){
                            keyStoreFile.delete();
                        }
                        AWSIotKeystoreHelper.saveCertificateAndPrivateKey(Config.CERT_ID, crt_str, key_str, keyStorePath, Config.KEY_STORE_NAME, Config.KEY_STORE_PASSWORD);

                        mClientId = res.getString("id");

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                                editor.putString("client_id", mClientId);
                                editor.commit();
                                onAuthed();
                                callback.run();
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }

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
                String topic = String.format("client/%s/setting/set", mClientId);
                publish(topic, payload);
                notifyChannelListChangedListeners();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void createChannel(String channel_name, String mate_name){
            try {
                JSONObject payload = new JSONObject();
                payload.put("channel_name", channel_name);
                payload.put("mate_name", mate_name);
                String topic = String.format("channel/create");
                publish(topic, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        public boolean openChannel(Models.Channel channel, MapDataReceiver receiver){
            if(channel==null)
                return false;
            if(!mMqttConnected)
                return false;
            synchronized (mMapDataReceiver){
                if(!mMapDataReceiver.containsKey(channel.id)){
                    mMapDataReceiver.put(channel.id, new ArrayList<MapDataReceiver>());
                }
                mMapDataReceiver.get(channel.id).add(receiver);
            }
            synchronized (mOpenedChannel) {
                mOpenedChannel.add(channel.id);
            }
            subscribeChannel(channel.id);
            return true;
        }

        public void closeChannel(Models.Channel channel, MapDataReceiver receiver){
            if(channel==null)
                return;
            synchronized (mMapDataReceiver){
                if(mMapDataReceiver.containsKey(channel.id)){
                    mMapDataReceiver.get(channel.id).remove(receiver);
                }
            }
            unsubscribe(String.format("channel/%s/get/+", channel.id));
        }

        public void checkLocationService(){
            _checkLocationService();
        }
    };

    private Handler mHandler = new Handler();
    private List<Runnable> mChannelListChangedListener = new ArrayList<>();
    private final IBinder mBinder = new CoreBinder();

    private List<String> mOpenedChannel = new ArrayList<>();
    private HashMap<String, List<MapDataReceiver>> mMapDataReceiver = new HashMap<>();

    public CoreService() {

    }

    CognitoCachingCredentialsProvider credentialsProvider;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        keyStorePath = getFilesDir().getAbsolutePath();

        SharedPreferences settings = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mClientId = settings.getString("client_id", null);
        if(mClientId!=null){
            onAuthed();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void onAuthed(){
        if(mqttManager!=null)
            return;
        mqttManager = new AWSIotMqttManager(mClientId, Region.getRegion(Config.AWS_REGION_ID), Config.AWS_IOT_MQTT_ENDPOINT);
        mqttManager.setAutoReconnect(true);
        Log.e(TAG, "Service Started");
        keyStore = AWSIotKeystoreHelper.getIotKeystore(Config.CERT_ID, keyStorePath, Config.KEY_STORE_NAME, Config.KEY_STORE_PASSWORD);
        mqttManager.connect(keyStore, new AWSIotMqttClientStatusCallback() {
            @Override
            public void onStatusChanged(AWSIotMqttClientStatus status, Throwable throwable) {
                Log.d(TAG, "AWSIotMqttClientStatus changed: "+status);
                switch (status){
                    case Connected:
                        Log.e(TAG, "MQTT Connected");
                        mqttOnConnected();
                        mMqttConnected = true;
                        break;
                    default:
                        Log.e(TAG, "MQTT Disconnected");
                        mMqttConnected = false;
                        break;
                }
            }
        });
    }

    private List<Models.Channel> mChannelList = new ArrayList<>();
    private HashMap<String, Models.Channel> mChannelMap = new HashMap<>();


    private KeyStore keyStore;
    private String keyStorePath;

    private String mClientId;

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
        String topic;

        topic = String.format("client/%s/unicast", mClientId);
        subscribe(topic, new AWSIotMqttNewMessageCallback() {
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
        topic = String.format("client/%s/setting/get", mClientId);
        subscribe(topic, new AWSIotMqttNewMessageCallback() {
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
        _subscribeOpenedChannel();
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
            channel.user_channel_name = Util.JsonOptNullableString(msg, "user_channel_name", channel.user_channel_name);
            if(msg.has("enable")){
                channel.enable = msg.getBoolean("enable");
            }

            notifyChannelListChangedListeners();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void mqttChannelGetHandler(String topic, final JSONObject msg){
        Log.e(TAG, "Receive "+topic+" "+msg.toString());
        Matcher m = mChannelGetPattern.matcher(topic);
        m.find();
        final String channel_id = m.group(1);

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mMapDataReceiver){
                    if(mMapDataReceiver.containsKey(channel_id)){
                        for (MapDataReceiver mapDataReceiver : mMapDataReceiver.get(channel_id)) {
                            mapDataReceiver.onMapData(msg);
                        }
                    }
                }

            }
        });
    }

    private void _subscribeOpenedChannel(){
        synchronized (mOpenedChannel) {
            for (String c : mOpenedChannel) {
                subscribeChannel(c);
            }
        }
    }

    private void subscribeChannel(String channel_id){
        String topic = String.format("channel/%s/get/+", channel_id);
        subscribe(topic, new AWSIotMqttNewMessageCallback() {
            @Override
            public void onMessageArrived(String topic, byte[] data) {
                try {
                    mqttChannelGetHandler(topic, new JSONObject(new String(data, "UTF-8")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private boolean subscribe(String topic, AWSIotMqttNewMessageCallback callback){
        try{
            Log.e(TAG, "Subscribe "+topic);
            mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS1, callback);
            return true;
        }catch(Exception e){
            return false;
        }
    }

    private boolean unsubscribe(String topic){
        try{
            Log.e(TAG, "Unsubscribe "+topic);
            mqttManager.unsubscribeTopic(topic);
            return true;
        }catch(Exception e){
            return false;
        }
    }


    private boolean publish(String topic, JSONObject payload){
        try {
            String message = payload.toString();
            Log.e(TAG, "Publish "+topic+" "+message);
            mqttManager.publishString(message, topic, AWSIotMqttQos.QOS1);
            return true;
        }catch(Exception e){
            return false;
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
            String topic = String.format("client/%s/info", mClientId);
            publish(topic, msg);
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
