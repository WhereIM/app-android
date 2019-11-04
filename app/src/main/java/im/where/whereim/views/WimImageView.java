package im.where.whereim.views;

import android.content.Context;
import android.util.AttributeSet;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;

import im.where.whereim.Config;
import im.where.whereim.Util;
import im.where.whereim.models.Image;

/**
 * Created by buganini on 21/03/18.
 */

public class WimImageView extends androidx.appcompat.widget.AppCompatImageView {
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
        Glide.with(mContext).load(Config.getThumbnail(image)).apply(new RequestOptions().fitCenter().transform(new RoundedCorners((int)Util.dp2px(mContext, 10)))).into(this);
        if(image != null){
            mW = image.width;
            mH = image.height;
        }
        requestLayout();
    }

    public void setFile(File file){
        Glide.with(mContext).load(file).apply(new RequestOptions().fitCenter().transform(new RoundedCorners((int) Util.dp2px(mContext, 10)))).into(this);
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
