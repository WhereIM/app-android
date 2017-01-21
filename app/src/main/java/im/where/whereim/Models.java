package im.where.whereim;

/**
 * Created by buganini on 04/01/17.
 */

public class Models {
    public static class Channel{
        public String id;
        public String channel_name;
        public String user_channel_name;
        public String mate_id;
        public Boolean enable;
    }

    public static class Mate{
        public String id;
        public String mate_name;
        public String user_mate_name;

        public Double latitude;
        public Double longitude;
        public Double accuracy;
        public Double altitude; //m
        public Double bearing;
        public Double speed; //m
        public Long time;

        public String getDisplayName(){
            if(this.user_mate_name!=null){
                return this.user_mate_name;
            }
            return this.mate_name;
        }
    }

    public static class Marker{
        public String id;
        public String channel_id;
        public String name;
        public double latitude;
        public double longitude;
        public boolean isPublic;
    }

    public static class Enchantment{
        public String id;
        public String channel_id;
        public String name;
        public double latitude;
        public double longitude;
        public double radius;
        public boolean isPublic;
        public boolean enable;
    }

    interface BinderTask{
        void onBinderReady(CoreService.CoreBinder binder);
    };

    final static String KEY_LATITUDE = "lat";
    final static String KEY_LONGITUDE = "lng";
    final static String KEY_ACCURACY = "acc";
    final static String KEY_ALTITUDE = "alt";
    final static String KEY_BEARING = "bear";
    final static String KEY_SPEED = "spd";
    final static String KEY_TIME = "time";
    final static String KEY_PROVIDER = "pvdr";
    final static String KEY_RADIUS = "r";
    final static String KEY_PUBLIC = "public";
    final static String KEY_ENABLE = "enable";
    final static String KEY_ID = "id";
    final static String KEY_NAME = "name";
    final static String KEY_CHANNEL = "channel";
    final static String KEY_MATE_NAME = "mate_name";
    final static String KEY_USER_MATE_NAME = "user_mate_name";
    final static String KEY_MATE = "mate";
    final static String KEY_CLIENT_ID = "client_id";
    final static String KEY_MESSAGE = "msg";
}
