package im.where.whereim;

public class AuxFragment extends BaseChannelFragment {
    public boolean isResizable(){
        return false;
    }

    private ChannelActivity.AuxSize mSizePolicy = null;
    protected ChannelActivity.AuxSize getInitialSizePolicy(){
        return ChannelActivity.AuxSize.FULL;
    }
    private ChannelActivity.AuxSize getSizePolicy(){
        if(mSizePolicy == null){
            return getInitialSizePolicy();
        }
        return mSizePolicy;
    }
    protected void setSizePolicy(ChannelActivity.AuxSize sizePolicy){
        mSizePolicy = sizePolicy;
        if(channelActivity != null){
            channelActivity.setAuxSizePolicy(sizePolicy);
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
        channelActivity.setAuxSizePolicy(getSizePolicy());
        if(getSizePolicy() == ChannelActivity.AuxSize.FREE){
            if(height != null){
                channelActivity.setAuxSize(height);
            }
        }
        channelActivity.setAuxResizable(isResizable());
    }
}