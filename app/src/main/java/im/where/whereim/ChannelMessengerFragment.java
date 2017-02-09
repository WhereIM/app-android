package im.where.whereim;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.SpannableString;
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
import im.where.whereim.models.Message;

public class ChannelMessengerFragment extends BaseFragment {
    public ChannelMessengerFragment() {
        // Required empty public constructor
    }

    private Handler mHandler = new Handler();

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

        class ViewHolder {
            TextView sender;
            TextView time;
            TextView message;
        }

        @Override
        public int getCount() {
            return mBundledCursor.count;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.message_item, parent, false);
            ViewHolder vh = new ViewHolder();
            vh.sender = (TextView) view.findViewById(R.id.sender);
            vh.time = (TextView) view.findViewById(R.id.time);
            vh.message = (TextView) view.findViewById(R.id.message);
            view.setTag(vh);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            CoreService.CoreBinder binder = getBinder();
            Message m = Message.parse(cursor);
            ViewHolder vh = (ViewHolder) view.getTag();
            if(binder==null){
                vh.sender.setText(null);
                vh.message.setText(null);
                vh.time.setText(null);
                return;
            }
            vh.sender.setText(binder.getChannelMate(mChannel.id, m.mate_id).getDisplayName());
            SpannableString text = m.getText(getActivity(), binder);
            vh.message.setText(text);
            vh.time.setText(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Timestamp(m.time*1000)));
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

                        binder.addMessageListener(channel, mMessageListener);
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
                }
            });
        }
    };

    @Override
    public void onDestroyView() {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                binder.removeMessageListener(mChannel, mMessageListener);
            }
        });
        super.onDestroyView();
    }
}
