package im.where.whereim;

import android.os.Bundle;
import androidx.annotation.Nullable;

public class BasePane extends BaseChannelFragment {
    public final static String FIELD_ACTION = "action";


    public boolean isResizable(){
        return false;
    }

    private ChannelActivity.PaneSizePolicy mSizePolicy = null;
    protected ChannelActivity.PaneSizePolicy getInitialSizePolicy(){
        return ChannelActivity.PaneSizePolicy.FULL;
    }
    public ChannelActivity.PaneSizePolicy getSizePolicy(){
        if(mSizePolicy == null){
            return getInitialSizePolicy();
        }
        return mSizePolicy;
    }
    protected void setSizePolicy(ChannelActivity.PaneSizePolicy sizePolicy){
        mSizePolicy = sizePolicy;
        if(channelActivity != null){
            channelActivity.setPaneSizePolicy(sizePolicy);
        }
    }
    public void resetSizePolicy(){
        mSizePolicy = null;
    }

    private Integer height = null;
    public void setHeight(Integer height){
        this.height = height;
    }
    public Integer getHeight(){
        return height;
    }

    public boolean showCrosshair(){
        return false;
    }

    public boolean requireFocus(){
        return false;
    }

    public boolean lockFocus(){
        return false;
    }

    public boolean clearOnChannelChanged(){
        return false;
    }

    @Override
    public void setArguments(@Nullable Bundle args) {
        super.setArguments(args);
        onSetArguments(args);
    }

    protected void onSetArguments(Bundle args) {

    }

    private boolean mStarted = false;
    @Override
    public void onStart() {
        super.onStart();
        channelActivity.setCurrentPane(this);
        mStarted = true;
    }

    @Override
    public void onStop() {
        mStarted = false;
        super.onStop();
    }

    protected boolean isStarted(){
        return mStarted;
    }

    protected void setResult(Bundle data){
        if(channelActivity != null){
            channelActivity.setPaneResult(data);
        }
    }

    boolean onResult(Bundle data){
        return false;
    }

    protected void startPane(Class<? extends BasePane> pane, Bundle data){
        if(channelActivity != null){
            channelActivity.startPane(pane, data);
        }
    }

    protected void finish(){
        if(channelActivity != null) {
            channelActivity.onBackPressed();
        }
    }
}