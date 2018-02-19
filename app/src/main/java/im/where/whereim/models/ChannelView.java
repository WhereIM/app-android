package im.where.whereim.models;

import java.util.Locale;

import im.where.whereim.Config;

/**
 * Created by buganini on 20/02/18.
 */

public class ChannelView {
    public String id;
    public String name;
    public boolean enable_message;
    public boolean admin;
    public boolean deleted;

    public String getLink(){
        return String.format(Locale.ENGLISH, Config.WHERE_IM_URL, "view/"+id);
    }
}
