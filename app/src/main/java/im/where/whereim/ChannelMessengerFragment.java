package im.where.whereim;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import im.where.whereim.models.Channel;
import im.where.whereim.models.Marker;
import im.where.whereim.models.Mate;
import im.where.whereim.models.Message;
import im.where.whereim.views.WimSpan;

public class ChannelMessengerFragment extends BaseFragment {
    public ChannelMessengerFragment() {
        // Required empty public constructor
    }

    @Override
    public void onShow() {
        getChannel(new ChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        binder.addMessageListener(channel, mMessageListener);
                        binder.setRead(mChannel);
                    }
                });
            }
        });

    }

    @Override
    public void onHide() {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                if(mChannel!=null){
                    binder.removeMessageListener(mChannel, mMessageListener);
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private Handler mHandler = new Handler();

    private Channel mChannel;
    private MessageCursorAdapter mAdapter;
    private class MessageCursorAdapter extends CursorAdapter {
        private Message.BundledCursor mBundledCursor;
        public MessageCursorAdapter(Context context, Message.BundledCursor bc) {
            super(context, bc.cursor, true);
            mBundledCursor = bc;
        }

        public void changeCursor(Message.BundledCursor bc) {
            mBundledCursor = bc;
            super.changeCursor(bc.cursor);
        }

        private int getItemViewType(Cursor cursor) {
            Message m = Message.parse(cursor);
            if(mChannel.mate_id.equals(m.mate_id)){
                return 1; // out
            }else{
                return 0; // in
            }
        }

        @Override
        public int getItemViewType(int position) {
            Cursor cursor = (Cursor) getItem(position);
            return getItemViewType(cursor);
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        class InViewHolder {
            TextView date;
            TextView sender;
            TextView time;
            TextView message;
        }

        class OutViewHolder {
            TextView date;
            TextView time;
            TextView message;
        }

        @Override
        public int getCount() {
            return mBundledCursor.count;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = null;
            switch(getItemViewType(cursor)){
                case 0:
                    view = LayoutInflater.from(context).inflate(R.layout.in_message_item, parent, false);
                    InViewHolder ivh = new InViewHolder();
                    ivh.date = (TextView) view.findViewById(R.id.date);
                    ivh.sender = (TextView) view.findViewById(R.id.sender);
                    ivh.time = (TextView) view.findViewById(R.id.time);
                    ivh.message = (TextView) view.findViewById(R.id.message);
                    view.setTag(ivh);
                    return view;
                case 1:
                    view = LayoutInflater.from(context).inflate(R.layout.out_message_item, parent, false);
                    OutViewHolder ovh = new OutViewHolder();
                    ovh.date = (TextView) view.findViewById(R.id.date);
                    ovh.time = (TextView) view.findViewById(R.id.time);
                    ovh.message = (TextView) view.findViewById(R.id.message);
                    view.setTag(ovh);

                    return view;
            }
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CoreService.CoreBinder binder = getBinder();
            final int position = cursor.getPosition();
            boolean showDate = false;
            Message m = Message.parse(cursor);
            Timestamp time = new Timestamp(m.time*1000);
            String lymd = DateFormat.getDateInstance().format(time);
            String eee = new SimpleDateFormat("EEE").format(time);
            String hm = new SimpleDateFormat("HH:mm").format(time);
            switch(getItemViewType(cursor)){
                case 0:
                    InViewHolder ivh = (InViewHolder) view.getTag();

                    if(binder==null){
                        ivh.date.setVisibility(View.GONE);
                        ivh.sender.setText(null);
                        ivh.message.setText(null);
                        ivh.time.setText(null);
                        return;
                    }
                    Mate mate = binder.getChannelMate(mChannel.id, m.mate_id);
                    ivh.sender.setText(mate==null?"":mate.getDisplayName());
                    ivh.message.setText(m.getText(getActivity(), binder, clickedListener));
                    ivh.time.setText(hm);
                    if(position==0){
                        showDate = true;
                    }else{
                        cursor.moveToPosition(position-1);
                        Message prev = Message.parse(cursor);
                        if(!new SimpleDateFormat("yyyy-MM-dd").format(time).equals(new SimpleDateFormat("yyyy-MM-dd").format(new Timestamp(prev.time*1000)))){
                            showDate = true;
                        }
                        cursor.moveToPosition(position);
                    }
                    ivh.date.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    if(showDate){
                        ivh.date.setText(getString(R.string.date_format, eee, lymd));
                    }
                    return;
                case 1:
                    OutViewHolder ovh = (OutViewHolder) view.getTag();
                    if(binder==null){
                        ovh.date.setVisibility(View.GONE);
                        ovh.message.setText(null);
                        ovh.time.setText(null);
                        return;
                    }
                    ovh.message.setText(m.getText(getActivity(), binder, clickedListener));
                    ovh.time.setText(hm);
                    if(position==0){
                        showDate = true;
                    }else{
                        cursor.moveToPosition(position-1);
                        Message prev = Message.parse(cursor);
                        if(!new SimpleDateFormat("yyyy-MM-dd").format(time).equals(new SimpleDateFormat("yyyy-MM-dd").format(new Timestamp(prev.time*1000)))){
                            showDate = true;
                        }
                        cursor.moveToPosition(position);
                    }
                    ovh.date.setVisibility(showDate ? View.VISIBLE : View.GONE);
                    if(showDate){
                        ovh.date.setText(getString(R.string.date_format, eee, lymd));
                    }
                    return;
            }
        }
    };

    private WimSpan.OnClickedListener clickedListener = new WimSpan.OnClickedListener(){

        @Override
        public void onClick(String url) {
            final ChannelActivity activity = (ChannelActivity) getActivity();
            final String[] args = url.split("/");
            switch(args[0]){
                case "marker":
                    postBinderTask(new CoreService.BinderTask() {
                        @Override
                        public void onBinderReady(CoreService.CoreBinder binder) {
                            Marker m = binder.getChannelMarker(mChannel.id, args[1]);
                            if(m != null){
                                activity.moveToMarker(m, true);
                            } else {

                            }
                        }
                    });
                    break;
            }
        }
    };

    private boolean messageViewEnd = true;
    private long maxMessageId = 0;
    private Message.BundledCursor mCurrentCursor;
    private ListView mListView;
    private View mUnread;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_messenger, container, false);

        mUnread = view.findViewById(R.id.unread);
        mUnread.setVisibility(View.GONE);
        mUnread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListView.setSelection(mAdapter.getCount() - 1);
                messageViewEnd = true;
            }
        });

        final EditText input = (EditText) view.findViewById(R.id.input);
        view.findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String message = input.getEditableText().toString();
                if(message.isEmpty()){
                    return;
                }
                input.setText(null);
                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(final CoreService.CoreBinder binder) {
                        getChannel(new ChannelActivity.GetChannelCallback() {
                            @Override
                            public void onGetChannel(Channel channel) {
                                binder.sendMessage(channel, message);
                            }
                        });
                    }
                });
            }
        });

        mListView = (ListView) view.findViewById(R.id.message);

        mListView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if(mCurrentCursor==null){
                    return;
                }
                messageViewEnd = view.getChildCount() == 0 || view.getLastVisiblePosition() == view.getAdapter().getCount() - 1 &&
                        view.getChildAt(view.getChildCount() - 1).getBottom() <= view.getHeight();
                if(messageViewEnd){
                    mUnread.setVisibility(View.GONE);
                }
                if(firstVisibleItem==0){
                    postBinderTask(new CoreService.BinderTask() {
                        @Override
                        public void onBinderReady(CoreService.CoreBinder binder) {
                            if(mCurrentCursor.loadMoreChannelData || mCurrentCursor.loadMoreUserData){
                                binder.requestMessage(mChannel, mCurrentCursor.loadMoreBefore, mCurrentCursor.loadMoreAfter);
                            }else{
                                binder.requestMessage(mChannel, mCurrentCursor.firstId, null);
                            }
                        }
                    });
                }
            }
        });

        getChannel(new ChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                mChannel = channel;
            }
        });
        return view;
    }

    private boolean inited = false;
    private Runnable mMessageListener = new Runnable() {
        @Override
        public void run() {
            if(!inited){
                inited = true;
                reloadData.run();
            }else{
                mHandler.removeCallbacks(reloadData);
                mHandler.postDelayed(reloadData, 1500);
            }
        }
    };

    private Runnable reloadData = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    final boolean origMessageViewEnd = messageViewEnd;
                    mCurrentCursor = binder.getMessageCursor(mChannel);
                    if(mAdapter==null){
                        mAdapter = new MessageCursorAdapter(getActivity(), mCurrentCursor);
                        mListView.setAdapter(mAdapter);
                        maxMessageId = mCurrentCursor.lastId;
                    }else{
                        int originPosition = mListView.getFirstVisiblePosition();
                        long originId = mAdapter.getItemId(originPosition);
                        Integer newPosition = mCurrentCursor.positionMap.get(originId);
                        if(newPosition == null){
                            newPosition = originPosition;
                        }
                        View v = mListView.getChildAt(0);
                        int originTop = (v == null) ? 0 : (v.getTop() - mListView.getPaddingTop());
                        mAdapter.changeCursor(mCurrentCursor);
                        mListView.setSelectionFromTop(newPosition, originTop);
                        if(origMessageViewEnd){
                            mListView.smoothScrollToPosition(mAdapter.getCount() - 1);
                            messageViewEnd = origMessageViewEnd;
                        }else{
                            if(mCurrentCursor.lastId > maxMessageId) {
                                mUnread.setVisibility(View.VISIBLE);
                            }
                            maxMessageId = mCurrentCursor.lastId;
                        }
                    }
                    if(isShowed()) {
                        binder.setRead(mChannel);
                    }
                }
            });
        }
    };

    @Override
    public void onDestroyView() {
        if(mCurrentCursor!=null){
            mCurrentCursor.cursor.close();
        }
        super.onDestroyView();
    }
}
