package im.where.whereim;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import im.where.whereim.models.Ad;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Enchantment;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;
import im.where.whereim.models.Message;
import im.where.whereim.models.WimDBHelper;

public class CoreService extends Service {
    private final static String TAG = "CoreService";

    public interface BinderTask{
        void onBinderReady(CoreService.CoreBinder binder);
    };

    public interface MapDataReceiver {
        void onMockData(Mate mock);
        void onMateData(Mate mate);
        void moveToMate(Mate mate);
        void onEnchantmentData(Enchantment enchantment);
        void onMarkerData(Marker marker);
        void moveToMarker(Marker marker);
        void onMapAd(HashMap<String, Ad> ads);
    };

    public interface RegisterClientCallback {
        void onCaptchaRequired();
        void onExhausted();
        void onDone();
    }

    public interface ConnectionStatusCallback {
        void onConnectionStatusChanged(boolean connected);
    }

    public class CoreBinder extends Binder{
        public String getClientId(){
            return mClientId;
        }

        public String getUserName(){
            return mUserName;
        }

        public void setOTP(String otp){
            mOTP = otp;
        }

        public void register_client(final String provider, final String auth_id, final String name, final RegisterClientCallback callback){
            if(mOTP==null || mOTP.isEmpty()){
                callback.onCaptchaRequired();
                return;
            }
            new Thread(){
                @Override
                public void run() {
                    try {
                        mUserName = name;

                        JSONObject payload = new JSONObject();
                        payload.put("auth_provider", provider);
                        payload.put("auth_id", auth_id);
                        payload.put("otp", mOTP);

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

                        String status = res.getString("status");
                        if("exhausted".equals(status)){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    callback.onExhausted();
                                }
                            });
                            return;
                        }
                        if("otp".equals(status)){
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                   callback.onCaptchaRequired();
                                }
                            });
                            return;
                        }

                        if("ok".equals(status)) {
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
                            if (keyStoreFile.exists()) {
                                keyStoreFile.delete();
                            }
                            AWSIotKeystoreHelper.saveCertificateAndPrivateKey(Config.CERT_ID, crt_str, key_str, keyStorePath, Config.KEY_STORE_NAME, Config.KEY_STORE_PASSWORD);

                            mClientId = res.getString(Key.ID);

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
                                    editor.putString(Key.CLIENT_ID, mClientId);
                                    editor.commit();
                                    onAuthed();
                                    callback.onDone();
                                }
                            });
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }

        public void addConnectionStatusChangedListener(ConnectionStatusCallback callback){
            synchronized (mConnectionStatusChangedListener) {
                mConnectionStatusChangedListener.add(callback);
            }
            callback.onConnectionStatusChanged(mMqttConnected);
        }

        public void removeConnectionStatusChangedListener(ConnectionStatusCallback callback){
            synchronized (mConnectionStatusChangedListener) {
                mConnectionStatusChangedListener.remove(callback);
            }
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

        public List<Channel> getChannelList(){
            synchronized (mChannelList) {
                return Collections.unmodifiableList(mChannelList);
            }
        }

        public Channel getChannelById(String id){
            synchronized (mChannelList){
                for (Channel channel : mChannelList) {
                    if(channel.id.equals(id))
                        return channel;
                }
            }
            return null;
        }

        public void toggleChannelEnabled(Channel channel){
            if(channel==null){
                return;
            }
            if(channel.enable==null){
                return;
            }
            try {
                JSONObject payload = new JSONObject();
                payload.put(Key.CHANNEL, channel.id);
                payload.put(Key.ENABLE, !channel.enable);
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
                payload.put(Key.CHANNEL_NAME, channel_name);
                payload.put(Key.MATE_NAME, mate_name);
                String topic = String.format("channel/create");
                publish(topic, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void joinChannel(String channel_id, String channel_alias, String mate_name){
            try {
                JSONObject payload = new JSONObject();
                payload.put(Key.CHANNEL_NAME, channel_alias);
                payload.put(Key.MATE_NAME, mate_name);
                String topic = String.format("channel/%s/join", channel_id);
                publish(topic, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public boolean openMap(Channel channel, MapDataReceiver receiver){
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
            synchronized (mChannelEnchantment) {
                HashMap<String, Enchantment> list = mChannelEnchantment.get(channel.id);
                if(list!=null){
                    for (Enchantment enchantment : list.values()) {
                        receiver.onEnchantmentData(enchantment);
                    }
                }
            }
            synchronized (mChannelMarker) {
                HashMap<String, Marker> list = mChannelMarker.get(channel.id);
                if(list!=null){
                    for (Marker marker : list.values()) {
                        receiver.onMarkerData(marker);
                    }
                }
            }
            receiver.onMapAd(mapAds);
            if(mMocking) {
                receiver.onMockData(mMockMate);
            }
            subscribeChannelLocation(channel.id);
            return true;
        }

        public void closeMap(Channel channel, MapDataReceiver receiver){
            setVisibleTiles(new String[]{});
            if(channel==null)
                return;
            synchronized (mMapDataReceiver){
                if(mMapDataReceiver.containsKey(channel.id)){
                    mMapDataReceiver.get(channel.id).remove(receiver);
                }
            }
            unsubscribeChannelLocation(channel.id);
        }

        public void setVisibleTiles(String[] tiles){
            CoreService.this.setVisibleTiles(tiles);
        }

        public void checkLocationService(){
            _checkLocationService();
        }

        public void addMateListener(Channel channel, Runnable r){
            List<Runnable> list;
            synchronized (mMateListener){
                list = mMateListener.get(channel.id);
                if(list==null){
                    list = new ArrayList<>();
                    mMateListener.put(channel.id, list);
                }
            }
            list.add(r);
            mHandler.post(r);
        }

        public void removeMateListener(Channel channel, Runnable r){
            List<Runnable> list;
            synchronized (mMateListener){
                list = mMateListener.get(channel.id);
                if(list!=null){
                    list.remove(r);
                }
            }
        }

        public ArrayList<Mate> getChannelMate(String channel_id){
            return CoreService.this.getChannelMate(channel_id);
        }

        public Mate getChannelMate(String channel_id, String mate_id){
            return CoreService.this.getChannelMate(channel_id, mate_id);
        }

        public void createEnchantment(String name, String channel_id, boolean ispublic, double latitude, double longitude, int radius, boolean enable) {
            try {
                JSONObject payload = new JSONObject();
                payload.put(Key.NAME, name);
                payload.put(Key.CHANNEL, channel_id);
                payload.put(Key.LATITUDE, latitude);
                payload.put(Key.LONGITUDE, longitude);
                payload.put(Key.RADIUS, radius);
                payload.put(Key.ENABLE, enable);
                String topic;
                if(ispublic){
                    topic = String.format("channel/%s/data/enchantment/put", channel_id);
                }else{
                    topic = String.format("client/%s/enchantment/put", mClientId);
                }
                publish(topic, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void toggleEnchantmentEnabled(Enchantment enchantment){
            if(enchantment==null){
                return;
            }
            if(enchantment.enable==null){
                return;
            }
            try {
                JSONObject payload = new JSONObject();
                payload.put(Key.ID, enchantment.id);
                payload.put(Key.ENABLE, !enchantment.enable);
                enchantment.enable = null;
                String topic;
                if(enchantment.isPublic){
                    topic = String.format("channel/%s/data/enchantment/put", enchantment.channel_id);
                }else{
                    topic = String.format("client/%s/enchantment/put", mClientId);
                }
                publish(topic, payload);
                notifyChannelEnchantmentListChangedListeners(enchantment.channel_id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void createMarker(String name, String channel_id, boolean ispublic, double latitude, double longitude, JSONObject attr, boolean enable) {
            try {
                JSONObject payload = new JSONObject();
                payload.put(Key.NAME, name);
                payload.put(Key.CHANNEL, channel_id);
                payload.put(Key.LATITUDE, latitude);
                payload.put(Key.LONGITUDE, longitude);
                payload.put(Key.ATTR, attr);
                payload.put(Key.ENABLE, enable);
                String topic;
                if(ispublic){
                    topic = String.format("channel/%s/data/marker/put", channel_id);
                }else{
                    topic = String.format("client/%s/marker/put", mClientId);
                }
                publish(topic, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void toggleMarkerEnabled(Marker marker){
            if(marker==null){
                return;
            }
            if(marker.enable==null){
                return;
            }
            try {
                JSONObject payload = new JSONObject();
                payload.put(Key.ID, marker.id);
                payload.put(Key.ENABLE, !marker.enable);
                marker.enable = null;
                String topic;
                if(marker.isPublic){
                    topic = String.format("channel/%s/data/marker/put", marker.channel_id);
                }else{
                    topic = String.format("client/%s/marker/put", mClientId);
                }
                publish(topic, payload);
                notifyChannelMarkerListChangedListeners(marker.channel_id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public Enchantment getChannelEnchantment(String channel_id, String enchantment_id){
            synchronized (mChannelEnchantment) {
                HashMap<String, Enchantment> list;
                list = mChannelEnchantment.get(channel_id);
                if(list==null){
                    return null;
                }
                return list.get(enchantment_id);
            }
        }

        public Enchantment.List getChannelEnchantment(String channel_id){
            Enchantment.List ret = new Enchantment.List();
            synchronized (mChannelEnchantment) {
                HashMap<String, Enchantment> list;
                list = mChannelEnchantment.get(channel_id);
                if(list==null){
                    return ret;
                }
                for (Enchantment enchantment : list.values()) {
                    if(enchantment.isPublic){
                        ret.public_list.add(enchantment);
                    }else{
                        ret.private_list.add(enchantment);
                    }
                }
            }
            Collections.sort(ret.public_list, new Comparator<Enchantment>() {
                @Override
                public int compare(Enchantment lhs, Enchantment rhs) {
                    return lhs.name.compareToIgnoreCase(rhs.name);
                }
            });
            Collections.sort(ret.private_list, new Comparator<Enchantment>() {
                @Override
                public int compare(Enchantment lhs, Enchantment rhs) {
                    return lhs.name.compareToIgnoreCase(rhs.name);
                }
            });
            return ret;
        }

        public void addEnchantmentListener(Channel channel, Runnable r){
            List<Runnable> list;
            synchronized (mEnchantmentListener){
                list = mEnchantmentListener.get(channel.id);
                if(list==null){
                    list = new ArrayList<>();
                    mEnchantmentListener.put(channel.id, list);
                }
            }
            list.add(r);
            mHandler.post(r);
        }

        public void removeEnchantmentListener(Channel channel, Runnable r){
            List<Runnable> list;
            synchronized (mEnchantmentListener){
                list = mEnchantmentListener.get(channel.id);
                if(list!=null){
                    list.remove(r);
                }
            }
        }

        public Marker getChannelMarker(String channel_id, String marker_id){
            synchronized (mChannelMarker) {
                HashMap<String, Marker> list;
                list = mChannelMarker.get(channel_id);
                if(list==null){
                    return null;
                }
                return list.get(marker_id);
            }
        }

        public Marker.List getChannelMarker(String channel_id){
            Marker.List ret = new Marker.List();
            synchronized (mChannelMarker) {
                HashMap<String, Marker> list;
                list = mChannelMarker.get(channel_id);
                if(list==null){
                    return ret;
                }
                for (Marker marker : list.values()) {
                    if(marker.isPublic){
                        ret.public_list.add(marker);
                    }else{
                        ret.private_list.add(marker);
                    }
                }
            }
            Collections.sort(ret.public_list, new Comparator<Marker>() {
                @Override
                public int compare(Marker lhs, Marker rhs) {
                    return lhs.name.compareToIgnoreCase(rhs.name);
                }
            });
            Collections.sort(ret.private_list, new Comparator<Marker>() {
                @Override
                public int compare(Marker lhs, Marker rhs) {
                    return lhs.name.compareToIgnoreCase(rhs.name);
                }
            });
            return ret;
        }

        public void addMarkerListener(Channel channel, Runnable r){
            List<Runnable> list;
            synchronized (mMarkerListener){
                list = mMarkerListener.get(channel.id);
                if(list==null){
                    list = new ArrayList<>();
                    mMarkerListener.put(channel.id, list);
                }
            }
            list.add(r);
            mHandler.post(r);
        }

        public void removeMarkerListener(Channel channel, Runnable r){
            List<Runnable> list;
            synchronized (mMarkerListener){
                list = mMarkerListener.get(channel.id);
                if(list!=null){
                    list.remove(r);
                }
            }
        }

        public void requestMessage(Channel channel, Long before, Long after){
            try {
                JSONObject req = new JSONObject();
                req.put(Key.CHANNEL, channel.id);
                if(before!=null){
                    req.put("before", (long)before);
                }
                if(after!=null){
                    req.put("after", (long)after);
                }
                publish(String.format("client/%s/message/sync", mClientId), req);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void addMessageListener(Channel channel, Runnable r){
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

        public void removeMessageListener(Channel channel, Runnable r){
            List<Runnable> list;
            synchronized (mMessageListener){
                list = mMessageListener.get(channel.id);
                if(list!=null){
                    list.remove(r);
                }
            }
        }

        public void sendMessage(Channel channel, String s) {
            try {
                JSONObject payload = new JSONObject();
                payload.put(Key.MESSAGE, s);
                String topic = String.format("channel/%s/data/message/put", channel.id);
                publish(topic, payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public Message.BundledCursor getMessageCursor(Channel channel){
            return Message.getCursor(mWimDBHelper.getDatabase(), channel);
        }

        public boolean isMocking(){
            return mMocking;
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

    private Mate mMockMate = new Mate();
    private void notifyMocking(Location loc){
        Mate mate = null;
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
    private final List<ConnectionStatusCallback> mConnectionStatusChangedListener = new ArrayList<>();
    private final List<Runnable> mChannelListChangedListener = new ArrayList<>();
    private final HashMap<String, List<Runnable>> mMateListener = new HashMap<>();
    private final HashMap<String, List<Runnable>> mEnchantmentListener = new HashMap<>();
    private final HashMap<String, List<Runnable>> mMarkerListener = new HashMap<>();
    private final HashMap<String, List<Runnable>> mMessageListener = new HashMap<>();
    private final CoreBinder mBinder = new CoreBinder();

    final private HashMap<String, List<MapDataReceiver>> mMapDataReceiver = new HashMap<>();

    public CoreService() {

    }

    private WimDBHelper mWimDBHelper;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopLocationService();
        if(mIsForeground){
            mIsForeground = false;
            stopForeground(true);
        }

        mInited = false;

        super.onDestroy();
    }


    private final Object mutex = new Object();
    private boolean mInited = false;
    private void init(){
        synchronized (mutex) {
            if(!mInited) {
                mInited = true;
                mWimDBHelper = new WimDBHelper(this);

                keyStorePath = getFilesDir().getAbsolutePath();

                SharedPreferences settings = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
                mClientId = settings.getString(Key.CLIENT_ID, null);
                mUserName = settings.getString(Key.NAME, null);
                if (mClientId != null) {
                    onAuthed();
                }
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        init();

        return mBinder;
    }

    private void onAuthed(){
        if(mqttManager!=null)
            return;
        Log.e(TAG, "ClientId: "+mClientId);

        subscribe(String.format("client/%s/+/get", mClientId));

        try {
            Cursor cursor;

            cursor = Channel.getCursor(mWimDBHelper.getDatabase());
            while (cursor.moveToNext()) {
                mqttClientChannelHandler(Channel.parseToJson(cursor));
            }

            cursor = Mate.getCursor(mWimDBHelper.getDatabase());
            while (cursor.moveToNext()) {
                JSONObject j = Mate.parseToJson(cursor);
                mqttChannelMateHandler(j.getString(Key.CHANNEL), j);
            }

            cursor = Marker.getCursor(mWimDBHelper.getDatabase());
            while (cursor.moveToNext()) {
                JSONObject j = Marker.parseToJson(cursor);
                mqttMarkerHandler(j);
            }

            cursor = Enchantment.getCursor(mWimDBHelper.getDatabase());
            while (cursor.moveToNext()) {
                JSONObject j = Enchantment.parseToJson(cursor);
                mqttEnchantmentHandler(j);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (mConnectionStatusChangedListener) {
                                    for (ConnectionStatusCallback callback : mConnectionStatusChangedListener) {
                                        callback.onConnectionStatusChanged(true);
                                    }
                                }
                            }
                        });
                        break;
                    default:
                        Log.e(TAG, "MQTT Disconnected");
                        synchronized (mSubscribedTopics) {
                            mSubscribedTopics.clear();
                        }
                        synchronized (mChannelMessageSync) {
                            mChannelMessageSync.clear();
                        }
                        synchronized (mChannelDataSync) {
                            mChannelDataSync.clear();
                        }
                        mMqttConnected = false;
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                synchronized (mConnectionStatusChangedListener) {
                                    for (ConnectionStatusCallback callback : mConnectionStatusChangedListener) {
                                        callback.onConnectionStatusChanged(false);
                                    }
                                }
                            }
                        });
                        break;
                }
            }
        });
    }

    private List<Channel> mChannelList = new ArrayList<>();
    private HashMap<String, Channel> mChannelMap = new HashMap<>();


    private KeyStore keyStore;
    private String keyStorePath;

    private String mOTP;
    private String mClientId;
    private String mUserName;

    private AWSIotMqttManager mqttManager;

    private boolean mMqttConnected = false;
    private boolean mIsForeground = false;

    private void setTS(long ts){
        long ots = getTS();
        if(ts>ots){
            SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
            editor.putLong(Key.TS, ts);
            editor.apply();
        }
    }

    private long getTS(){
        return getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getLong(Key.TS, 0);
    }

    private void setTS(String channel_id, long ts){
        long ots = getTS(channel_id);
        if(ts>ots){
            String key = String.format("%s/%s", Key.TS, channel_id);
            SharedPreferences.Editor editor = getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
            editor.putLong(key, ts);
            editor.apply();
        }
    }

    private long getTS(String channel_id){
        String key = String.format("%s/%s", Key.TS, channel_id);
        return getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).getLong(key, 0);
    }

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

    private void notifyChannelEnchantmentListChangedListeners(String channel_id){
        List<Runnable> list;
        synchronized (mEnchantmentListener){
            list = mEnchantmentListener.get(channel_id);
            if(list!=null){
                for (Runnable runnable : list) {
                    mHandler.post(runnable);
                }
            }
        }

    }

    private void notifyChannelMarkerListChangedListeners(String channel_id){
        List<Runnable> list;
        synchronized (mMarkerListener){
            list = mMarkerListener.get(channel_id);
            if(list!=null){
                for (Runnable runnable : list) {
                    mHandler.post(runnable);
                }
            }
        }

    }

    private void _checkLocationService(){
        boolean pending = false;
        int enableCount = 0;
        synchronized (mChannelList){
            for (Channel channel : mChannelList) {
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
                            .setSmallIcon(R.drawable.ic_stat_logo)
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

    private void mqttOnConnected(){
        try{
            synchronized (mSubscribedTopics) {
                for (String topic : mSubscribedTopics) {
                    Log.e(TAG, "Re-Subscribe "+topic);
                    mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS1, mAwsIotMqttNewMessageCallback);
                }
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        try {
            JSONObject sync = new JSONObject();
            sync.put(Key.TS, getTS());
            publish(String.format("client/%s/channel/sync", mClientId), sync);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        synchronized (mChannelList) {
            for (Channel channel : mChannelList) {
                syncChannelData(channel);
                syncChannelMessage(channel);
            }
        }
    }

    final AWSIotMqttNewMessageCallback mAwsIotMqttNewMessageCallback = new AWSIotMqttNewMessageCallback() {
        @Override
        public void onMessageArrived(String topic, byte[] data) {
            try {
                String message = new String(data, "UTF-8");
                Log.e(TAG, "Receive "+topic+" "+message);
                JSONObject payload = new JSONObject(message);
                mqttMessageHandler(topic, payload);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    private Pattern mClientGetPattern = Pattern.compile("^client/[A-Fa-f0-9]{32}/([^/]+)/get$");
    private Pattern mChannelDataPattern = Pattern.compile("^channel/([a-f0-9]{32})/data/([^/]+)/get$");
    private Pattern mChannelLocationPattern = Pattern.compile("^channel/([a-f0-9]{32})/location/([^/]+)/get$");
    private Pattern mMapAdPattern = Pattern.compile("^system/map_ad/get/([0-3]*)$");
    private Pattern mSystemMessagePattern = Pattern.compile("^system/message/get$");
    private void mqttMessageHandler(String topic, JSONObject msg){
        try {
            Matcher m;
            m = mClientGetPattern.matcher(topic);
            if (m.matches()) {
                switch (m.group(1)) {
                    case "unicast":
                        mqttMessageHandler(msg.getString("topic"), msg.getJSONObject("message"));
                        break;
                    case "channel":
                        mqttClientChannelHandler(msg);
                        break;
                    case "enchantment":
                        mqttEnchantmentHandler(msg);
                        break;
                    case "marker":
                        mqttMarkerHandler(msg);
                        break;
                }
                return;
            }
            m = mChannelLocationPattern.matcher(topic);
            if(m.matches()){
                mqttChannelLocationHandler(m.group(1), msg);
                return;
            }

            m = mChannelDataPattern.matcher(topic);
            if(m.matches()){
                String channel_id = m.group(1);
                switch (m.group(2)){
                    case "mate":
                        mqttChannelMateHandler(channel_id, msg);
                        break;
                    case "enchantment":
                        mqttEnchantmentHandler(msg);
                        break;
                    case "marker":
                        mqttMarkerHandler(msg);
                        break;
                    case "message":
                        mqttChannelMessageHandler(channel_id, msg);
                        break;
                }
                return;
            }
            m = mMapAdPattern.matcher(topic);
            if(m.matches()){
                mqttSystemMapAdHandler(msg);
                return;
            }
            m = mSystemMessagePattern.matcher(topic);
            if(m.matches()){
                mqttSystemMessageHandler(msg, false);
                return;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private final HashMap<String, HashMap<String, Enchantment>> mChannelEnchantment = new HashMap<>();
    private void mqttEnchantmentHandler(JSONObject data){
        final String enchantment_id;
        final String channel_id;
        Enchantment enchantment;
        try {
            enchantment_id = data.getString(Key.ID);
            channel_id = data.getString(Key.CHANNEL);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        synchronized (mChannelEnchantment) {
            HashMap<String, Enchantment> list = mChannelEnchantment.get(channel_id);
            if(list==null){
                list = new HashMap<>();
                mChannelEnchantment.put(channel_id, list);
            }

            enchantment = list.get(enchantment_id);
            if(enchantment==null){
                enchantment = new Enchantment();
                list.put(enchantment_id, enchantment);
            }
            enchantment.id = enchantment_id;
            enchantment.channel_id = channel_id;
            enchantment.name = Util.JsonOptNullableString(data, Key.NAME, enchantment.name);
            enchantment.latitude = data.optDouble(Key.LATITUDE, enchantment.latitude);
            enchantment.longitude = data.optDouble(Key.LONGITUDE, enchantment.longitude);
            enchantment.radius = data.optDouble(Key.RADIUS, enchantment.radius);
            enchantment.isPublic = data.optBoolean(Key.PUBLIC, enchantment.isPublic);
            enchantment.enable = Util.JsonOptBoolean(data, Key.ENABLE, enchantment.enable);
        }

        try {
            if(data.has(Key.TS)) {
                mWimDBHelper.replace(enchantment);
                setTS(channel_id, data.getLong(Key.TS));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final Enchantment _e = enchantment;
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mMapDataReceiver){
                    if(mMapDataReceiver.containsKey(channel_id)){
                        for (MapDataReceiver mapDataReceiver : mMapDataReceiver.get(channel_id)) {
                            mapDataReceiver.onEnchantmentData(_e);
                        }
                    }
                }

            }
        });
        notifyChannelEnchantmentListChangedListeners(channel_id);
    }

    private final HashMap<String, HashMap<String, Marker>> mChannelMarker = new HashMap<>();
    private void mqttMarkerHandler(JSONObject data){
        final String marker_id;
        final String channel_id;
        try {
            marker_id = data.getString(Key.ID);
            channel_id = data.getString(Key.CHANNEL);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        Marker marker;
        synchronized (mChannelMarker) {
            HashMap<String, Marker> list = mChannelMarker.get(channel_id);
            if(list==null){
                list = new HashMap<>();
                mChannelMarker.put(channel_id, list);
            }

            marker = list.get(marker_id);
            if(marker==null){
                marker = new Marker();
                list.put(marker_id, marker);
            }

            marker.id = marker_id;
            marker.channel_id = channel_id;
            try {
                marker.name = Util.JsonOptNullableString(data, Key.NAME, marker.name);
                marker.latitude = data.optDouble(Key.LATITUDE, marker.latitude);
                marker.longitude = data.optDouble(Key.LONGITUDE, marker.longitude);
                if(data.has(Key.ATTR)) {
                    marker.attr = data.getJSONObject(Key.ATTR);
                }
                marker.isPublic = data.optBoolean(Key.PUBLIC, marker.isPublic);
                marker.enable = Util.JsonOptBoolean(data, Key.ENABLE, marker.enable);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        try {
            if(data.has(Key.TS)) {
                mWimDBHelper.replace(marker);
                setTS(channel_id, data.getLong(Key.TS));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final Marker _m = marker;
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
        notifyChannelMarkerListChangedListeners(channel_id);
    }

    private final HashMap<String, Boolean> mChannelDataSync = new HashMap<>();
    private final HashMap<String, Boolean> mChannelMessageSync = new HashMap<>();
    private void mqttClientChannelHandler(JSONObject msg){
        try {
            final Channel channel;
            final String channel_id = msg.getString(Key.CHANNEL);
            if(mChannelMap.containsKey(channel_id)){
                channel = mChannelMap.get(channel_id);
            }else{
                channel = new Channel();
                mChannelMap.put(channel_id, channel);
            }
            synchronized (mChannelList) {
                if(!mChannelList.contains(channel)){
                    mChannelList.add(channel);
                }
            }
            channel.id = channel_id;
            channel.channel_name  = msg.optString(Key.CHANNEL_NAME, channel.channel_name);
            channel.user_channel_name = Util.JsonOptNullableString(msg, Key.USER_CHANNEL_NAME, channel.user_channel_name);
            channel.mate_id = Util.JsonOptNullableString(msg, Key.MATE, channel.mate_id);
            if(msg.has(Key.ENABLE)){
                channel.enable = msg.getBoolean(Key.ENABLE);
            }

            if(msg.has(Key.TS)) {
                mWimDBHelper.replace(channel);
                setTS(msg.getLong(Key.TS));
            }

            subscribe(String.format("channel/%s/data/+/get", channel_id));

            syncChannelData(channel);

            notifyChannelListChangedListeners();

            syncChannelMessage(channel);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // ================ Channel Data ================
    private void syncChannelData(final Channel channel){
        if(!mChannelDataSync.containsKey(channel.id)){
            mChannelDataSync.put(channel.id, true);
            try {
                JSONObject sync = new JSONObject();
                sync.put(Key.TS, getTS(channel.id));
                sync.put(Key.CHANNEL, channel.id);
                publish(String.format("client/%s/channel_data/sync", mClientId), sync);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void syncChannelMessage(final Channel channel){
        boolean doSync = true;
        synchronized (mChannelMessageSync) {
            if(mChannelMessageSync.containsKey(channel.id)) {
                doSync = false;
            }
            mChannelMessageSync.put(channel.id, true);
        }
        if(doSync){
            new Thread(){
                @Override
                public void run() {
                    Message.BundledCursor bc = Message.getCursor(mWimDBHelper.getDatabase(), channel);
                    bc.cursor.close();
                    JSONObject data = new JSONObject();
                    try {
                        data.put(Key.CHANNEL, channel.id);
                        data.put("after", bc.lastId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return;
                    }
                    publish(String.format("client/%s/message/sync", mClientId), data);
                }
            }.start();
        }
    }

    // ================ Channel Data - Mate ================
    private void mqttChannelMateHandler(String channel_id, JSONObject data){
        String mate_id;
        try {
            mate_id = data.getString(Key.ID);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        Mate mate = getChannelMate(channel_id, mate_id);
        mate.mate_name = Util.JsonGetNullableString(data, Key.MATE_NAME);
        mate.user_mate_name = Util.JsonGetNullableString(data, Key.USER_MATE_NAME);

        try {
            if(data.has(Key.TS)) {
                mWimDBHelper.replace(mate);
                setTS(channel_id, data.getLong(Key.TS));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private HashMap<String, HashMap<String, Mate>> mChannelMate = new HashMap<>();
    private ArrayList<Mate> getChannelMate(String channel_id){
        ArrayList<Mate> list = new ArrayList<>();

        synchronized (mChannelMate) {
            HashMap<String, Mate> mateMap = mChannelMate.get(channel_id);
            if(mateMap!=null){
                for (Mate mate : mateMap.values()) {
                    list.add(mate);
                }
            }
        }

        Collections.sort(list, new Comparator<Mate>() {
            @Override
            public int compare(Mate lhs, Mate rhs) {
                return lhs.getDisplayName().compareToIgnoreCase(rhs.getDisplayName());
            }
        });
        return list;
    }

    private Mate getChannelMate(String channel_id, String mate_id){
        HashMap<String, Mate> mateMap;
        synchronized (mChannelMate) {
            mateMap = mChannelMate.get(channel_id);
            if(mateMap==null){
                mateMap = new HashMap<>();
                mChannelMate.put(channel_id, mateMap);
            }
        }
        Mate mate;
        synchronized (mateMap) {
            mate = mateMap.get(mate_id);
            if(mate==null){
                mate = new Mate();
                mate.id = mate_id;
                mate.channel_id = channel_id;
                mateMap.put(mate_id, mate);
            }
        }
        return mate;
    }

    // ================ Channel Data - Message ================

    private void mqttChannelMessageHandler(String channel_id, JSONObject payload){
        Message message = Message.parse(payload);
        mWimDBHelper.insert(message);
        notifyMessageListener(channel_id, message);
    }

    private void notifyMessageListener(String channel_id, Message message){
        int count = 0;
        if(channel_id==null){
            synchronized (mMessageListener) {
                for (List<Runnable> runnables : mMessageListener.values()) {
                    for (Runnable runnable : runnables) {
                        mHandler.post(runnable);
                        count += 1;
                    }
                }
            }
        }else{
            synchronized (mMessageListener) {
                List<Runnable> list = mMessageListener.get(channel_id);
                if(list!=null){
                    for (Runnable runnable : list) {
                        mHandler.post(runnable);
                        count += 1;
                    }
                }
            }
        }
//        if(count==0){
        Log.e("lala","count="+count);
            if(message.notify>0){
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_stat_logo)
                                .setContentTitle(getString(R.string.app_name))
                                .setContentText(message.getText(this, mBinder))
                                .setDefaults(Notification.DEFAULT_SOUND)
                                .setAutoCancel(true);
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify((int)message.id, mBuilder.build());
            }
//        }
    }

    // ================ Channel Location ================

    private void mqttChannelLocationHandler(final String channel_id, final JSONObject data){
        String mate_id;

        try {
            mate_id = data.getString(Key.MATE);
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }
        Mate mate = getChannelMate(channel_id, mate_id);
        try {
            mate.latitude = data.getDouble(Key.LATITUDE);
            mate.longitude = data.getDouble(Key.LONGITUDE);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            mate.accuracy = data.getDouble(Key.ACCURACY);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final Mate _m = mate;
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

    private final HashMap<String, Long> mapAdTileHistory = new HashMap();
    private final ArrayList<String> subscribedTiles = new ArrayList<>();
    public void setVisibleTiles(String[] tiles){
        List<String> tilesArray = Arrays.asList(tiles);
        ArrayList<String> out = new ArrayList<>();
        synchronized (subscribedTiles) {
            for (String tile : subscribedTiles) {
                if(!tilesArray.contains(tile)){
                    out.add(tile);
                }
            }
            for (String tile : out) {
                subscribedTiles.remove(tile);
            }
            long now = System.currentTimeMillis();
            for (String tile : tiles) {
                Long ttl = mapAdTileHistory.get(tile);
                if(ttl!=null && now<ttl){
                    continue;
                }
                mapAdTileHistory.put(tile, now+Config.MAP_AD_TTL);
                JSONObject payload = new JSONObject();
                try {
                    payload.put(Key.LANG, getString(R.string.lang));
                    payload.put(Key.TILE, tile);
                    publish("system/map_ad/get", payload);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                subscribedTiles.add(tile);
            }
        }
    }

    private final HashMap<String, Ad> mapAds = new HashMap<>();
    private void mqttSystemMapAdHandler(JSONObject data){
        Ad ad = Ad.parse(data);
        synchronized (mapAds) {
            mapAds.put(ad.id, ad);
        }
        notifyMapAd();
    }

    private void notifyMapAd(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mMapDataReceiver){
                    for (List<MapDataReceiver> mapDataReceiverList : mMapDataReceiver.values()) {
                        for (MapDataReceiver mapDataReceiver : mapDataReceiverList) {
                            mapDataReceiver.onMapAd(mapAds);
                        }
                    }
                }

            }
        });
    }

    private void mqttSystemMessageHandler(JSONObject msg, boolean isPublic){
//        Message message = Message.parse(msg);
//        message.isPublic = isPublic;
//        mWimDBHelper.insert(message);
//        notifyMessageListener(null, message);
    }

    // ================ Util Functions ================

    private void subscribeChannelLocation(String channel_id){
        String topic = String.format("channel/%s/location/private/get", channel_id);
        subscribe(topic);
    }

    private void unsubscribeChannelLocation(String channel_id){
        String topic = String.format("channel/%s/location/private/get", channel_id);
        unsubscribe(topic);
    }

    private final List<String> mSubscribedTopics = new ArrayList<>();

    private void subscribe(final String topic){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    synchronized (mSubscribedTopics) {
                        if(mSubscribedTopics.contains(topic))
                            return;
                        mSubscribedTopics.add(topic);
                    }
                    Log.e(TAG, "Subscribe "+topic);
                    mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS1, mAwsIotMqttNewMessageCallback);
                } catch(Exception e) {
                    // noop
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
                    synchronized (mSubscribedTopics) {
                        mSubscribedTopics.remove(topic);
                    }
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        });
    }


    private void publish(final String topic, final JSONObject payload){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String message = payload.toString();
                Log.e(TAG, "Publish "+topic+" "+message);
                try {
                    mqttManager.publishString(message, topic, AWSIotMqttQos.QOS1);
                } catch (com.amazonaws.AmazonClientException e){
                    // noop
                }
            }
        });
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
            provider = "MOCKING";
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
            msg.put(Key.LATITUDE, loc.getLatitude());
            msg.put(Key.LONGITUDE, loc.getLongitude());
            if(loc.hasAccuracy()){
                msg.put(Key.ACCURACY, loc.getAccuracy());
            }
            if(loc.hasAltitude()){
                msg.put(Key.ALTITUDE, loc.getAltitude());
            }
            if(loc.hasBearing()){
                msg.put(Key.BEARING, loc.getBearing());
            }
            if(loc.hasSpeed()){
                msg.put(Key.SPEED, loc.getSpeed());
            }
            msg.put(Key.TIME, System.currentTimeMillis());
            msg.put(Key.PROVIDER, provider);
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
