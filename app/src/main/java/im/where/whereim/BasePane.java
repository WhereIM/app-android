package im.where.whereim;

public class BasePane extends BaseChannelFragment {
    public boolean isResizable(){
        return false;
    }

    private ChannelActivity.PaneSizePolicy mSizePolicy = null;
    protected ChannelActivity.PaneSizePolicy getInitialSizePolicy(){
        return ChannelActivity.PaneSizePolicy.FULL;
    }
    private ChannelActivity.PaneSizePolicy getSizePolicy(){
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

    @Override
    public void onStart() {
        super.onStart();
        channelActivity.setPaneSizePolicy(getSizePolicy());
        if(getSizePolicy() == ChannelActivity.PaneSizePolicy.FREE){
            if(height != null){
                channelActivity.setPaneSize(height);
            }
        }
        channelActivity.setPaneResizable(isResizable());
    }
}