package im.where.whereim;

/**
 * Created by buganini on 04/01/17.
 */

class Models {
    static class Channel{
        String id;
        String channel_name;
        String user_channel_name;
        Boolean enable;
    }

    static class Mate{
        String id;
        String mate_name;
        String user_mate_name;

        Double latitude;
        Double longitude;
        Double accuracy;
        Double altitude; //m
        Double bearing;
        Double speed; //m
        Long time;
        String provider;

        String getDisplayName(){
            if(this.user_mate_name!=null){
                return this.user_mate_name;
            }
            return this.mate_name;
        }
    }

    interface BinderTask{
        void onBinderReady(CoreService.CoreBinder binder);
    };

    final static String KEY_LATITUDE = "lat";
    final static String KEY_LONGITURE = "lng";
    final static String KEY_ACCURACY = "acc";
    final static String KEY_ALTITUDE = "alt";
    final static String KEY_BEARING = "bear";
    final static String KEY_SPEED = "spd";
    final static String KEY_TIME = "time";
    final static String KEY_PROVIDER = "pvdr";

    final static String KEY_MATE_NAME = "mate_name";
    final static String KEY_USER_MATE_NAME = "user_mate_name";

    final static String TARGET_0 = "0";
    final static String TARGET_1 = "1";
    final static String TARGET_2 = "2";
    final static String TARGET_3 = "3";
    final static String TARGET_MATE = "mate";
}
