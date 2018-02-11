package im.where.whereim;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.CursorAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;

import im.where.whereim.models.Channel;
import im.where.whereim.models.Mate;
import im.where.whereim.models.Message;

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
            TextView sender;
            TextView time;
            TextView message;
        }

        class OutViewHolder {
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
                    ivh.sender = (TextView) view.findViewById(R.id.sender);
                    ivh.time = (TextView) view.findViewById(R.id.time);
                    ivh.message = (TextView) view.findViewById(R.id.message);
                    view.setTag(ivh);
                    return view;
                case 1:
                    view = LayoutInflater.from(context).inflate(R.layout.out_message_item, parent, false);
                    OutViewHolder ovh = new OutViewHolder();
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
            Message m = Message.parse(cursor);
            switch(getItemViewType(cursor)){
                case 0:
                    InViewHolder ivh = (InViewHolder) view.getTag();
                    if(binder==null){
                        ivh.sender.setText(null);
                        ivh.message.setText(null);
                        ivh.time.setText(null);
                        return;
                    }
                    Mate mate = binder.getChannelMate(mChannel.id, m.mate_id);
                    ivh.sender.setText(mate==null?"":mate.getDisplayName());
                    ivh.message.setText(m.getText(getActivity(), binder));
                    ivh.time.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(m.time*1000)));
                    return;
                case 1:
                    OutViewHolder ovh = (OutViewHolder) view.getTag();
                    if(binder==null){
                        ovh.message.setText(null);
                        ovh.time.setText(null);
                        return;
                    }
                    ovh.message.setText(m.getText(getActivity(), binder));
                    ovh.time.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(m.time*1000)));
                    return;
            }
        }
    };

    private Message.BundledCursor mCurrentCursor;
    private ListView mListView;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_messenger, container, false);

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

                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        mCurrentCursor = binder.getMessageCursor(mChannel);
                        mAdapter = new MessageCursorAdapter(getActivity(), mCurrentCursor);
                        mListView.setAdapter(mAdapter);
                    }
                });
            }
        });
        return view;
    }

    private Runnable mMessageListener = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    mCurrentCursor = binder.getMessageCursor(mChannel);
                    mAdapter.changeCursor(mCurrentCursor);
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
