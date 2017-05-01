package im.where.whereim.views;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.NativeExpressAdView;

/**
 * Created by buganini on 01/05/17.
 *         <com.google.android.gms.ads.NativeExpressAdView
 android:id="@+id/adView"
 android:layout_width="wrap_content"
 android:layout_height="wrap_content"
 android:layout_centerInParent="true">
 </com.google.android.gms.ads.NativeExpressAdView>
 */

public class AdFrame extends RelativeLayout {
    private AdRequest mAdRequest = new AdRequest.Builder().build();
    private Context mContext;
    public AdFrame(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
    }

    private NativeExpressAdView mLastAdView = null;
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        int width = (int)(w / getResources().getDisplayMetrics().density);
        int height = (int)(h / getResources().getDisplayMetrics().density);

        if(mLastAdView!=null){
            removeView(mLastAdView);
        }

        if(width > 1200) {
            width = 1200;
        }
        if(height > 1200) {
            height = 1200;
        }

        String adUnitId;
        if(width >= 280 && height >= 250){
            adUnitId = "ca-app-pub-5449795846702141/2364100516";
        } else if(width >= 280 && height >= 132){
            adUnitId = "ca-app-pub-5449795846702141/7988171714";
        } else if(width >= 280 && height >= 80){
            adUnitId = "ca-app-pub-5449795846702141/9464904911";
        } else {
            return;
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);

        mLastAdView = new NativeExpressAdView(mContext);
        mLastAdView.setLayoutParams(params);
        addView(mLastAdView);

        mLastAdView.setAdUnitId(adUnitId);
        mLastAdView.setAdSize(new AdSize(width, height));

        mLastAdView.loadAd(mAdRequest);
    }
}
