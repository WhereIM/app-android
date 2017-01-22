package im.where.whereim;

import android.content.Context;
import android.support.v4.app.Fragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by buganini on 11/01/17.
 */

public class BaseFragment extends Fragment {
    private List<Models.BinderTask> mPendingTask = new ArrayList<>();

    protected CoreService.CoreBinder getBinder(){
        BaseActivity activity = (BaseActivity) getActivity();
        if(activity==null){
            return null;
        }
        return activity.getBinder();
    }

    protected void postBinderTask(Models.BinderTask task){
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
            Models.BinderTask task = null;
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
        postBinderTask(new Models.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                ((ChannelActivity) getActivity()).getChannel(callback);
            }
        });
    }
}
