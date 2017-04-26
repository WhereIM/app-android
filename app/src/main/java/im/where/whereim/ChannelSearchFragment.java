package im.where.whereim;

import android.os.Handler;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelSearchFragment extends BaseFragment {
    protected Handler mHandler = new Handler();

    public static class SearchResult {
        String name;
        Double latitude;
        Double longitude;
    }

    abstract public void search(String keyword);
}
