package im.where.whereim;

import android.content.Context;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by buganini on 11/01/17.
 */

public class BaseFragment extends Fragment {
    private List<CoreService.BinderTask> mPendingTask = new ArrayList<>();

    protected boolean _mResumed = false;
    protected boolean _mVisible = true;
    protected boolean _mShowed = false;

    protected void determineVisibility(){
        if(_mResumed && _mVisible){
            if(!_mShowed){
                _mShowed = true;

                onShow();
            }
        }else{
            if(_mShowed){
                _mShowed = false;
                onHide();
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        _mVisible = isVisibleToUser;
        determineVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        _mResumed = true;
        determineVisibility();
    }

    @Override
    public void onPause() {
        _mResumed = false;
        determineVisibility();
        super.onPause();
    }

    public void onShow(){

    }

    public void onHide(){

    }

    public boolean isShowed(){
        return _mShowed;
    }

    protected CoreService.CoreBinder getBinder(){
        BaseActivity activity = (BaseActivity) getActivity();
        if(activity==null){
            return null;
        }
        return activity.getBinder();
    }

    protected void postBinderTask(CoreService.BinderTask task){
        synchronized (mPendingTask) {
            mPendingTask.add(task);
        }
        processBinderTask();
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        processBinderTask();
    }


    private void processBinderTask() {
        BaseActivity activity = (BaseActivity) getActivity();
        if(activity==null){
            return;
        }
        while(true){
            CoreService.BinderTask task = null;
            synchronized (mPendingTask){
                if(mPendingTask.size()>0){
                    task = mPendingTask.remove(0);
                }
            }
            if(task==null){
                break;
            }else{
                activity.postBinderTask(task);
            }
        }
    }

    protected void getChannel(final ChannelActivity.GetChannelCallback callback){
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                ((ChannelActivity) getActivity()).getChannel(callback);
            }
        });
    }
}
