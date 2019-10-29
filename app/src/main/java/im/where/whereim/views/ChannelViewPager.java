package im.where.whereim.views;

import android.content.Context;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class ChannelViewPager extends ViewPager {

    public ChannelViewPager(Context context) {
        super(context);
    }

    public ChannelViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Never allow swiping to switch between pages
        return false;
    }

    @Override
    public void setCurrentItem(int item) {
        super.setCurrentItem(item, false);
    }
}