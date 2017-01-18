package im.where.whereim;

import android.Manifest;
import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
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
import android.widget.Toast;

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

import im.where.whereim.database.Message;
import im.where.whereim.database.WimDBHelper;

public class CoreService extends Service {
    private final static String TAG = "CoreService";
    public interface MapDataReceiver {
        void onMockData(Models.Mate mock);
        void onMateData(Models.Mate mate);
        void onEnchantmentData(Models.Enchantment enchantment);
        void onMarkerData(Models.Marker marker);
    };

    public class CoreBinder extends Binder{
        public String getClientId(){
            return mClientId;
        }

        public String getUserName(){
            return mUserName;
        }

        public void register_client(final String provider, final String auth_id, final String name, final Runnable callback){
            new Thread(){
                @Override
                public void run() {
                    try {
                        SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                        editor.putString(Models.KEY_NAME, name);
                        editor.apply();

                        JSONObject payload = new JSONObject();
                        payload.put("auth_provider", provider);
                        payload.put("auth_id", auth_id);

                        HttpsURLConnection conn;
                        InputStream is;

                        URL url = new URL(Config.AWS_API_GATEWAY_REGISTER_CLIENT);
                        conn = (HttpsURLConnection) url.openConnection();
                        conn.setRequestMethod("POST");
                        conn.setUseCaches(false);
                        conn.setDoInput(true);
                        conn.setDoOutput(true);

                        String content = payload.toString();
                        Log.e(TAG, "register_client -> "+content);
                        OutputStream os = conn.getOutputStream();
                        os.write(content.getBytes());
                        os.close();

                        is = conn.getInputStream();
                        String json = IOUtils.toString(is);
                        is.close();
                        conn.disconnect();

                        Log.e(TAG, "register_client <- "+json);
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

                        mClientId = res.getString(Models.KEY_ID);

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                                editor.putString(Models.KEY_CLIENT_ID, mClientId);
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
            if(channel==null){
                return;
            }
            if(channel.enable==null){
                return;
            }
            try {
                JSONObject payload = new JSONObject();
                payload.put(Models.KEY_CHANNEL, channel.id);
                payload.put(Models.KEY_ENABLE, !channel.enable);
                channel.enable = null;
                String topic = String.format("client/%s/channel/put", mClientId);
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

        public void joinChannel(String channel_id, String channel_alias, String mate_name){
            try {
                JSONObject payload = new JSONObject();
                payload.put("channel_name", channel_alias);
                payload.put("mate_name", mate_name);
                String topic = String.format("channel/%s/join", channel_id);
                publish(topic, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public boolean openMap(Models.Channel channel, MapDataReceiver receiver){
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
            synchronized (mEnchantment) {
                for (Models.Enchantment enchantment : mEnchantment.values()) {
                    if(enchantment.channel_id.equals(channel.id)){
                        receiver.onEnchantmentData(enchantment);
                    }
                }
            }
            synchronized (mChannelMarker) {
                HashMap<String, Models.Marker> list = mChannelMarker.get(channel.id);
                if(list!=null){
                    for (Models.Marker marker : list.values()) {
                        receiver.onMarkerData(marker);
                    }
                }
            }
            subscribeChannelLocation(channel.id);
            return true;
        }

        public void closeMap(Models.Channel channel, MapDataReceiver receiver){
            if(channel==null)
                return;
            synchronized (mMapDataReceiver){
                if(mMapDataReceiver.containsKey(channel.id)){
                    mMapDataReceiver.get(channel.id).remove(receiver);
                }
            }
            unsubscribeChannelLocation(channel.id);
        }

        public void checkLocationService(){
            _checkLocationService();
        }

        public Models.Mate getChannelMate(String channel_id, String mate_id){
            return CoreService.this.getChannelMate(channel_id, mate_id);
        }

        public void createEnchantment(String name, String channel_id, double latitude, double longitude, int radius, boolean enable) {
            try {
                JSONObject payload = new JSONObject();
                payload.put(Models.KEY_NAME, name);
                payload.put(Models.KEY_CHANNEL, channel_id);
                payload.put(Models.KEY_LATITUDE, latitude);
                payload.put(Models.KEY_LONGITUDE, longitude);
                payload.put(Models.KEY_RADIUS, radius);
                payload.put(Models.KEY_ENABLE, enable);
                String topic = String.format("client/%s/enchantment/put", mClientId);
                publish(topic, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void createMarker(String name, String channel_id, double latitude, double longitude) {
            try {
                JSONObject payload = new JSONObject();
                payload.put(Models.KEY_NAME, name);
                payload.put(Models.KEY_CHANNEL, channel_id);
                payload.put(Models.KEY_LATITUDE, latitude);
                payload.put(Models.KEY_LONGITUDE, longitude);
                String topic = String.format("channel/%s/data/marker/put", channel_id);
                publish(topic, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public Models.Marker getChannelMarker(String channel_id, String marker_id){
            synchronized (mChannelMarker) {
                HashMap<String, Models.Marker> list;
                list = mChannelMarker.get(channel_id);
                if(list==null){
                    return null;
                }
                return list.get(marker_id);
            }
        }

        public void addMessageListener(Models.Channel channel, Runnable r){
            List<Runnable> list;
            synchronized (mMessageListener){
                list = mMessageListener.get(channel.id);
                if(list==null){
                    list = new ArrayList<>();
                    mMessageListener.put(channel.id, list);
                }
            }
            list.add(r);
        }

        public void removeMessageListener(Models.Channel channel, Runnable r){
            List<Runnable> list;
            synchronized (mMessageListener){
                list = mMessageListener.get(channel.id);
                if(list!=null){
                    list.remove(r);
                }
            }
        }


        public void sendMessage(Models.Channel channel, String s) {
            try {
                JSONObject payload = new JSONObject();
                payload.put(Models.KEY_MESSAGE, s);
                String topic = String.format("channel/%s/data/message/put", channel.id);
                publish(topic, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public Cursor getMessageCursor(Models.Channel channel){
            return Message.getCursor(mWimDBHelper.getDatabase(), channel);
        }

        public void startMocking(){
            if(mMocking){
                return;
            }
            mMocking = true;
            if(!mLocationServiceRunning){
                Toast.makeText(CoreService.this, R.string.mock_location_service_not_running, Toast.LENGTH_SHORT).show();
            }
            if(mLastBestLocation!=null){
                mMockingLocation = new Location(mLastBestLocation);
                notifyMocking(mMockingLocation);
            }
        }

        public void moveMocking(float x, float y){
            if(mMockingLocation==null)
                return;
            mMockingLocation.setLongitude(mMockingLocation.getLongitude() + 0.0005*x);
            mMockingLocation.setLatitude(mMockingLocation.getLatitude() - 0.0005*y);

            notifyMocking(mMockingLocation);
        }

        public void stopMocking(){
            mMocking = false;
            mMockingLocation = null;
            notifyMocking(null);
        }
    };

    private Models.Mate mMockMate = new Models.Mate();
    private void notifyMocking(Location loc){
        Models.Mate mate = null;
        if(loc!=null){
            mMockMate.latitude = loc.getLatitude();
            mMockMate.longitude = loc.getLongitude();
            mate = mMockMate;
        }
        synchronized (mMapDataReceiver) {
            for (List<MapDataReceiver> mapDataReceivers : mMapDataReceiver.values()) {
                for (MapDataReceiver mapDataReceiver : mapDataReceivers) {
                    mapDataReceiver.onMockData(mate);
                }
            }
        }
    }

    private boolean mMocking = false;
    private Handler mHandler = new Handler();
    private final List<Runnable> mChannelListChangedListener = new ArrayList<>();
    private final HashMap<String, List<Runnable>> mMessageListener = new HashMap<>();
    private final IBinder mBinder = new CoreBinder();

    private List<String> mOpenedChannel = new ArrayList<>();
    private HashMap<String, List<MapDataReceiver>> mMapDataReceiver = new HashMap<>();

    public CoreService() {

    }

    private WimDBHelper mWimDBHelper;
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mWimDBHelper = new WimDBHelper(this);

        keyStorePath = getFilesDir().getAbsolutePath();

        SharedPreferences settings = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        mClientId = settings.getString(Models.KEY_CLIENT_ID, null);
        mUserName = settings.getString(Models.KEY_NAME, null);
        if(mClientId!=null){
            onAuthed();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void onAuthed(){
        if(mqttManager!=null)
            return;
        Log.e(TAG, "ClientId: "+mClientId);
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
                        synchronized (mChannelMate) {
                            mChannelMate.clear();
                        }
                        synchronized (mChannelList) {
                            mChannelList.clear();
                        }
                        synchronized (mChannelMap) {
                            mChannelMap.clear();
                        }

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
    private String mUserName;

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

    private Pattern mClientGetPattern = Pattern.compile("^client/[A-Fa-f0-9]{32}/([^/]+)/get$");

    private void mqttOnConnected(){
        subscribe(String.format("client/%s/+/get", mClientId), new AWSIotMqttNewMessageCallback() {
            @Override
            public void onMessageArrived(String topic, byte[] data) {
                Matcher m;
                m = mClientGetPattern.matcher(topic);
                if(!m.matches()){
                    return;
                }
                try {
                    String message = new String(data, "UTF-8");
                    Log.e(TAG, "Receive "+topic+" "+message);
                    JSONObject payload = new JSONObject(message);
                    switch(m.group(1)){
                        case "unicast":
                            mqttClientUnicastHandler(payload.getString("topic"), payload.getJSONObject("message"));
                            break;
                        case "channel":
                            mqttClientChannelHandler(payload);
                            break;
                        case "enchantment":
                            mqttClientEnchantmentHandler(payload);
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        _subscribeOpenedChannel();
    }

    private Pattern mChannelDataPattern = Pattern.compile("^channel/([a-f0-9]{32})/data/([^/]+)/get$");
    private Pattern mChannelLocationPattern = Pattern.compile("^channel/([a-f0-9]{32})/location/([^/]+)/get$");
    private void mqttClientUnicastHandler(String topic, JSONObject msg){
        Matcher m;
        m = mChannelLocationPattern.matcher(topic);
        if(m.matches()){
            mqttChannelLocationHandler(topic, msg);
            return;
        }

        m = mChannelDataPattern.matcher(topic);
        if(m.matches()){
            String channel_id = m.group(1);
            switch (m.group(2)){
                case "mate":
                    mqttChannelMateHandler(channel_id, msg);
                    break;
                case "marker":
                    mqttChannelMarkerHandler(channel_id, msg);
                    break;
                case "message":
                    mqttChannelMessageHandler(channel_id, msg);
                    break;
            }
            return;
        }
    }

    final private HashMap<String, Models.Enchantment> mEnchantment = new HashMap<>();
    final private HashMap<String, Models.Enchantment> mEnabledEnchantment = new HashMap<>();
    private void mqttClientEnchantmentHandler(JSONObject msg){
        try {
            String channel_id = msg.getString(Models.KEY_CHANNEL);
            String enchantment_id = msg.getString(Models.KEY_ID);
            Models.Enchantment enchantment;

            synchronized (mEnchantment) {
                if(!mEnchantment.containsKey(enchantment_id)){
                    enchantment = new Models.Enchantment();
                    enchantment.id = enchantment_id;
                    mEnchantment.put(enchantment.id, enchantment);
                }else{
                    enchantment = mEnchantment.get(enchantment_id);
                }
            }

            enchantment.channel_id = channel_id;
            enchantment.name = msg.getString(Models.KEY_NAME);
            enchantment.latitude = msg.getDouble(Models.KEY_LATITUDE);
            enchantment.longitude = msg.getDouble(Models.KEY_LONGITUDE);
            enchantment.radius = msg.getDouble(Models.KEY_RADIUS);
            enchantment.enable = msg.getBoolean(Models.KEY_ENABLE);

            if(enchantment.enable){
                synchronized (mEnabledEnchantment) {
                    mEnabledEnchantment.put(enchantment.id, enchantment);
                }
            }else{
                synchronized (mEnabledEnchantment) {
                    mEnabledEnchantment.remove(enchantment.id);
                }
            }
            synchronized (mMapDataReceiver){
                if(mMapDataReceiver.containsKey(channel_id)){
                    for (MapDataReceiver mapDataReceiver : mMapDataReceiver.get(channel_id)) {
                        mapDataReceiver.onEnchantmentData(enchantment);
                    }
                }
            }

//            processEnabledEnchantment();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void mqttClientChannelHandler(JSONObject msg){
        try {
            final Models.Channel channel;
            final String channel_id = msg.getString(Models.KEY_CHANNEL);
            if(mChannelMap.containsKey(channel_id)){
                channel = mChannelMap.get(channel_id);
            }else{
                channel = new Models.Channel();
                mChannelMap.put(channel_id, channel);
            }
            synchronized (mChannelList) {
                if(!mChannelList.contains(channel)){
                    mChannelList.add(channel);
                }
            }
            channel.id = channel_id;
            channel.channel_name  = msg.optString("channel_name", channel.channel_name);
            channel.user_channel_name = Util.JsonOptNullableString(msg, "user_channel_name", channel.user_channel_name);
            channel.mate_id = Util.JsonOptNullableString(msg, Models.KEY_MATE, channel.mate_id);
            if(msg.has("enable")){
                channel.enable = msg.getBoolean("enable");
            }

            subscribe(String.format("channel/%s/data/+/get", channel_id), new AWSIotMqttNewMessageCallback() {
                @Override
                public void onMessageArrived(String topic, byte[] data) {
                    Matcher m;
                    m = mChannelDataPattern.matcher(topic);
                    if(!m.matches()){
                        return;
                    }
                    try {
                        String channel_id = m.group(1);
                        String message = new String(data, "UTF-8");
                        Log.e(TAG, "Receive "+topic+" "+message);
                        JSONObject payload = new JSONObject(message);
                        switch(m.group(2)){
                            case "mate":
                                mqttChannelMateHandler(channel_id, payload);
                                break;
                            case "message":
                                mqttChannelMessageHandler(channel_id, payload);
                                break;
                            case "marker":
                                mqttChannelMarkerHandler(channel_id, payload);
                                break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            notifyChannelListChangedListeners();

            new Thread(){
                @Override
                public void run() {
                    JSONObject data = Message.getSyncData(mWimDBHelper.getDatabase(), channel);
                    if(data==null){
                        return;
                    }
                    try {
                        data.put(Models.KEY_CHANNEL, channel.id);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }
                    publish(String.format("client/%s/message/sync", mClientId), data);
                }
            }.start();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ================ Channel Data ================

    // ================ Channel Data - Marker ================
    private final HashMap<String, HashMap<String, Models.Marker>> mChannelMarker = new HashMap<>();
    private void mqttChannelMarkerHandler(final String channel_id, JSONObject data){
        String marker_id;
        String name;
        double latitude;
        double longitude;
        try {
            marker_id = data.getString(Models.KEY_ID);
            name = data.getString(Models.KEY_NAME);
            latitude = data.getDouble(Models.KEY_LATITUDE);
            longitude = data.getDouble(Models.KEY_LONGITUDE);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        Models.Marker marker;
        synchronized (mChannelMarker) {
            HashMap<String, Models.Marker> list = mChannelMarker.get(channel_id);
            if(list==null){
                list = new HashMap<>();
                mChannelMarker.put(channel_id, list);
            }

            marker = list.get(marker_id);
            if(marker==null){
                marker = new Models.Marker();
                list.put(marker_id, marker);
            }
            marker.id = marker_id;
            marker.channel_id = channel_id;
            marker.name = name;
            marker.latitude = latitude;
            marker.longitude = longitude;
        }

        final Models.Marker _m = marker;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mMapDataReceiver){
                    if(mMapDataReceiver.containsKey(channel_id)){
                        for (MapDataReceiver mapDataReceiver : mMapDataReceiver.get(channel_id)) {
                            mapDataReceiver.onMarkerData(_m);
                        }
                    }
                }

            }
        });
    }

    // ================ Channel Data - Mate ================
    private void mqttChannelMateHandler(String channel_id, JSONObject data){
        String mate_id;
        try {
            mate_id = data.getString(Models.KEY_ID);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        Models.Mate mate = getChannelMate(channel_id, mate_id);
        mate.mate_name = Util.JsonGetNullableString(data, Models.KEY_MATE_NAME);
        mate.user_mate_name = Util.JsonGetNullableString(data, Models.KEY_USER_MATE_NAME);
    }

    private HashMap<String, HashMap<String, Models.Mate>> mChannelMate = new HashMap<>();
    private Models.Mate getChannelMate(String channel_id, String mate_id){
        HashMap<String, Models.Mate> mateMap;
        synchronized (mChannelMate) {
            mateMap = mChannelMate.get(channel_id);
            if(mateMap==null){
                mateMap = new HashMap<>();
                mChannelMate.put(channel_id, mateMap);
            }
        }
        Models.Mate mate;
        synchronized (mateMap) {
            mate = mateMap.get(mate_id);
            if(mate==null){
                mate = new Models.Mate();
                mate.id = mate_id;
                mateMap.put(mate_id, mate);
            }
        }
        return mate;
    }

    // ================ Channel Data - Message ================

    private void mqttChannelMessageHandler(String channel_id, JSONObject payload){
        mWimDBHelper.insert(Message.parse(payload));
        notifyMessageListener(channel_id);
    }

    private void notifyMessageListener(String channel_id){
        synchronized (mMessageListener) {
            List<Runnable> list = mMessageListener.get(channel_id);
            if(list==null){
                return;
            }
            for (Runnable runnable : list) {
                mHandler.post(runnable);
            }
        }
    }

    // ================ Channel Location ================

    private void mqttChannelLocationHandler(String topic, final JSONObject data){
        Log.e(TAG, "Receive "+topic+" "+data.toString());
        Matcher m = mChannelLocationPattern.matcher(topic);
        m.find();
        final String channel_id = m.group(1);

        String mate_id;

        try {
            mate_id = data.getString("mate");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        Models.Mate mate = getChannelMate(channel_id, mate_id);
        try {
            mate.latitude = data.getDouble(Models.KEY_LATITUDE);
            mate.longitude = data.getDouble(Models.KEY_LONGITUDE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            mate.accuracy = data.getDouble(Models.KEY_ACCURACY);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final Models.Mate _m = mate;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mMapDataReceiver){
                    if(mMapDataReceiver.containsKey(channel_id)){
                        for (MapDataReceiver mapDataReceiver : mMapDataReceiver.get(channel_id)) {
                            mapDataReceiver.onMateData(_m);
                        }
                    }
                }

            }
        });
    }

    // ================ Util Functions ================

    private void _subscribeOpenedChannel(){
        synchronized (mOpenedChannel) {
            for (String c : mOpenedChannel) {
                subscribeChannelLocation(c);
            }
        }
    }

    private void subscribeChannelLocation(String channel_id){
        String topic = String.format("channel/%s/location/private/get", channel_id);
        subscribe(topic, new AWSIotMqttNewMessageCallback() {
            @Override
            public void onMessageArrived(String topic, byte[] data) {
                try {
                    mqttChannelLocationHandler(topic, new JSONObject(new String(data, "UTF-8")));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void unsubscribeChannelLocation(String channel_id){
        String topic = String.format("channel/%s/location/private/get", channel_id);
        unsubscribe(topic);
    }

    private void subscribe(final String topic, final AWSIotMqttNewMessageCallback callback){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try{
                    Log.e(TAG, "Subscribe "+topic);
                    mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS1, callback);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }

    private void unsubscribe(final String topic){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try{
                    Log.e(TAG, "Unsubscribe "+topic);
                    mqttManager.unsubscribeTopic(topic);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
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

    // ================ Location Service ================

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

    private final static int UPDATE_MIN_TIME = 10000; //10s
    private final static int UPDATE_MIN_DISTANCE = 5; //5m

    private void startGPSListener() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, UPDATE_MIN_TIME, UPDATE_MIN_DISTANCE, mGpsLocationListener);
    }

    private void startNetworkListener(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, UPDATE_MIN_TIME, UPDATE_MIN_DISTANCE, mNetworkLocationListener);
    }

    Location mMockingLocation;
    private void processLocation(String provider, Location newLocation){
        Location loc;
        if(mMocking){
            if(mMockingLocation==null){
                if(isBetterLocation(newLocation, mLastBestLocation)){
                    mMockingLocation = new Location(newLocation);
                }else{
                    mMockingLocation = new Location(mLastBestLocation);
                }
                notifyMocking(mMockingLocation);
            }
            loc = mMockingLocation;
        }else{
            if(!isBetterLocation(newLocation, mLastBestLocation)){
                return;
            }
            mLastBestLocation = newLocation;
            loc = mLastBestLocation;
        }
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
            String topic = String.format("client/%s/location/put", mClientId);
            publish(topic, msg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private Location mLastBestLocation = null;

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
