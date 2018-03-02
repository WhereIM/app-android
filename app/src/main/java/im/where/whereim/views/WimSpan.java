package im.where.whereim.views;

import android.text.style.ClickableSpan;
import android.view.View;

/**
 * Created by buganini on 02/03/18.
 */

public class WimSpan extends ClickableSpan {
    public interface OnClickedListener {
        void onClick(String url);
    }

    private String url;
    private OnClickedListener callback;
    public WimSpan(String url, OnClickedListener callback) {
        super();
        this.url = url;
        this.callback = callback;
    }

    @Override
    public void onClick(View widget) {
        if(callback!=null){
            callback.onClick(url);
        }
    }
}
