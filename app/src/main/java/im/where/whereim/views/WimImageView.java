package im.where.whereim.views;

import android.content.Context;
import android.util.AttributeSet;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import im.where.whereim.Config;
import im.where.whereim.models.Image;

/**
 * Created by buganini on 21/03/18.
 */

public class WimImageView extends android.support.v7.widget.AppCompatImageView {
    private Context mContext;

    public WimImageView(Context context) {
        super(context, null);
    }

    public WimImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    private float mW = -1;
    private float mH = -1;

    public void setImage(Image image){
        Glide.with(mContext).load(Config.getThumbnail(image.url)).apply(new RequestOptions().transform(new RoundedCorners(15))).into(this);
        mW = image.width;
        mH = image.height;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        if(mW!=-1 && mH!=-1){
            final int widthSpecMode = MeasureSpec.getMode(widthMeasureSpec);
            final int heightSpecMode = MeasureSpec.getMode(heightMeasureSpec);
            boolean fixedWidth = widthSpecMode == MeasureSpec.EXACTLY;
            boolean fixedHeight = heightSpecMode == MeasureSpec.EXACTLY;
            if(fixedWidth && !fixedHeight){
                int width =  MeasureSpec.getSize(widthMeasureSpec);
                int height = (int)(width*(mH/mW));
                setMeasuredDimension(width, height);
                return;
            }else if(fixedHeight && !fixedWidth){
                int height =  MeasureSpec.getSize(heightMeasureSpec);
                int width = (int)(height*(mW/mH));
                setMeasuredDimension(width, height);
                return;
            }
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
