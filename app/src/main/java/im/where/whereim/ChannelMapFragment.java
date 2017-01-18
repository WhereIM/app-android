package im.where.whereim;

import android.os.Handler;
import android.view.View;
import android.widget.TextView;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelMapFragment extends BaseFragment implements CoreService.MapDataReceiver {
    protected Handler mHandler = new Handler();

    protected View mEnchantmentController;
    protected View mMarkerController;
    protected View mMarkerView;
    protected TextView mMarkerViewTitle;

}
