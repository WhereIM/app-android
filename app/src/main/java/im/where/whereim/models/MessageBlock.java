package im.where.whereim.models;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by buganini on 23/03/18.
 */

public class MessageBlock {
    public int count;
    public long loadMoreBefore;
    public long loadMoreAfter;
    public long firstId;
    public long lastId;
    public boolean loadMoreChannelData;
    public boolean loadMoreUserData;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        sb.append("loadMoreBefore="+loadMoreBefore);
        sb.append("loadMoreAfter="+loadMoreAfter);
        sb.append("loadMoreUserData="+loadMoreUserData);
        sb.append("loadMoreChannelData="+loadMoreChannelData);
        sb.append("firstId="+firstId);
        sb.append("lastId="+lastId);
        sb.append("count="+count);
        sb.append("----------------------------");
        return sb.toString();
    }

    public static MessageBlock get(SQLiteDatabase db, Channel channel){
        MessageBlock mb = new MessageBlock();
        mb.loadMoreChannelData = false;
        mb.loadMoreUserData = false;
        boolean hasChannelData = false;
        boolean hasUserData = false;
        long channelDataSn = 0;
        long channelDataId = 0;
        long userDataSn = 0;
        long userDataId = 0;

        Cursor cursor = db.rawQuery("SELECT "+Message.COL_ID+","+Message.COL_SN+","+Message.COL_PUBLIC+","+Message.COL_CHANNEL+","+Message.COL_MATE+","+Message.COL_TYPE+","+Message.COL_MESSAGE+","+Message.COL_TIME+","+Message.COL_DELETED+","+Message.COL_HIDDEN+" FROM "+Message.TABLE_NAME+" WHERE "+Message.COL_CHANNEL+"=? OR "+Message.COL_CHANNEL+" IS NULL ORDER BY "+Message.COL_ID+" ASC", new String[]{channel.id});
        mb.count = 0;
        if(cursor.moveToLast()) {
            do {
                mb.count += 1;
                long id = cursor.getLong(0);
                long sn = cursor.getLong(1);
                boolean isPublic = 0 != cursor.getInt(2);
                String channel_id = cursor.getString(3);

                if(channel_id==null){
                    continue;
                }
                if (isPublic) {
                    if (!hasChannelData) {
                        hasChannelData = true;
                        channelDataSn = sn;
                    } else {
                        if (sn == channelDataSn - 1) {
//                            sb.append("sn="+sn+", channelDataSn="+channelDataSn);
                            channelDataSn = sn;
                            channelDataId = id;
                        } else {
//                            sb.append("!! sn="+sn+", channelDataSn="+channelDataSn);
                            mb.loadMoreBefore = channelDataId;
                            mb.loadMoreAfter = id;
                            mb.loadMoreChannelData = true;
                            break;
                        }
                    }
                } else {
                    if (!hasUserData) {
                        hasUserData = true;
                        userDataSn = sn;
                    } else {
                        if (sn == userDataSn - 1) {
//                            sb.append("sn="+sn+", userDataSn="+userDataSn);
                            userDataSn = sn;
                            userDataId = id;
                        } else {
//                            sb.append("!! sn="+sn+", userDataSn="+userDataSn);
                            mb.loadMoreBefore = userDataId;
                            mb.loadMoreAfter = id;
                            mb.loadMoreUserData = true;
                            break;
                        }
                    }
                }
            } while (cursor.moveToPrevious());
        }
        if(cursor.moveToLast()){
            mb.lastId = cursor.getLong(0);
        }else{
            mb.lastId = -1;
        }
        if(cursor.moveToFirst()){
            mb.firstId = cursor.getLong(0);
        }else{
            mb.firstId = -1;
        }
        cursor.close();
        return mb;
    }
}
