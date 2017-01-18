package im.where.whereim.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by buganini on 19/01/17.
 */

public class Joystick extends View {
    public Joystick(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private float mR;
    private float mX;
    private float mY;

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom){
        mR = getWidth() / 2.0f;
        mX = getWidth() / 2.0f;
        mY = getHeight() / 2.0f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                float x = event.getX();
                float y = event.getY();
                float dx = x - mR;
                float dy = y - mR;
                if(dx*dx + dy*dy < mR*mR){
                    mX = x;
                    mY = y;
                    invalidate();
                }
                startFiring();
                break;
            case MotionEvent.ACTION_UP:
                mX = getWidth() / 2.0f;
                mY = getHeight() / 2.0f;
                invalidate();
                stopFiring();
                break;
        }
        return true;
    }


    private Paint mBgPaint;
    private Paint mPaint;
    @Override
    protected void onDraw(Canvas canvas){
        super.onDraw(canvas);
        if (mBgPaint == null) {
            mBgPaint = new Paint();
            mBgPaint.setColor(Color.BLACK);
            mBgPaint.setStrokeWidth(1);
            mBgPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mBgPaint.setShader(new RadialGradient(getWidth() / 2, getHeight() / 2,
                    getHeight() / 2, Color.LTGRAY, Color.GRAY, Shader.TileMode.MIRROR));
        }

        if (mPaint == null) {
            mPaint = new Paint();
            mPaint.setColor(Color.RED);
            mPaint.setStyle(Paint.Style.FILL);
        }
        float x = getWidth()/2.0f;
        float y = getHeight()/2.0f;
        float r = getWidth()/2.0f;
        canvas.drawCircle(x, y, r, mBgPaint);
        canvas.drawCircle(mX, mY, getWidth()*0.9f/10.0f, mPaint);
    }

    public interface Callback{
        void callback(float x, float y);
    }

    private final List<Callback> mListener = new ArrayList<>();
    public void addListener(Callback callback){
        synchronized (mListener) {
            mListener.add(callback);
        }
    }

    public void removeListener(Callback callback){
        synchronized (mListener) {
            mListener.remove(callback);
        }
    }

    private Runnable fire = new Runnable() {
        @Override
        public void run() {
            synchronized (mListener) {
                for (Callback callback : mListener) {
                    callback.callback((mX-mR)/mR, (mY-mR)/mR);
                }
            }
            if(mFiring)
                postDelayed(this, 100);
        }
    };

    private boolean mFiring = false;
    private void startFiring(){
        if(mFiring)
            return;
        mFiring = true;
        post(fire);
    }

    private void stopFiring(){
        mFiring = false;
    }
}
