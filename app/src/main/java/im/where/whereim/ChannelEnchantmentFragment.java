package im.where.whereim;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;

import im.where.whereim.dialogs.DialogFixedEnchantment;
import im.where.whereim.dialogs.DialogMobileEnchantment;
import im.where.whereim.models.Channel;
import im.where.whereim.models.Enchantment;
import im.where.whereim.views.EmojiText;
import im.where.whereim.views.FilterBar;

public class ChannelEnchantmentFragment extends BaseFragment {
    public ChannelEnchantmentFragment() {
        // Required empty public constructor
    }

    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    private Channel mChannel;
    private Enchantment.List mEnchantmentList;
    private EnchantmentAdapter mAdapter;
    private class EnchantmentAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return 3;
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            switch (groupPosition){
                case 0:
                    return 1;
                case 1:
                    if(mEnchantmentList==null)
                        return 1;
                    return mEnchantmentList.public_list.size() > 0 ? mEnchantmentList.public_list.size() : 1;
                case 2:
                    if(mEnchantmentList==null)
                        return 1;
                    return mEnchantmentList.private_list.size() > 0 ? mEnchantmentList.private_list.size() : 1;
            }
            return 0;
        }

        @Override
        public int getChildType(int groupPosition, int childPosition) {
            if(groupPosition==0){
                return 0;
            }else{
                if(getChild(groupPosition, childPosition)!=null) {
                    return 0;
                } else {
                    if(mFilterKeyword==null) {
                        return 1;
                    } else {
                        return 2;
                    }
                }
            }
        }

        @Override
        public int getChildTypeCount() {
            return 3;
        }

        @Override
        public Object getGroup(int groupPosition) {
            switch (groupPosition){
                case 0:
                    return R.string.self;
                case 1:
                    return R.string.public_enchantment;
                case 2:
                    return R.string.private_enchantment;
            }
            return 0;
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            switch (groupPosition){
                case 0:
                    return null;
                case 1:
                    if(mEnchantmentList==null)
                        return null;
                    if(childPosition<mEnchantmentList.public_list.size())
                        return mEnchantmentList.public_list.get(childPosition);
                    return null;
                case 2:
                    if(mEnchantmentList==null)
                        return null;
                    if(childPosition<mEnchantmentList.private_list.size())
                        return mEnchantmentList.private_list.get(childPosition);
                    return null;
            }
            return null;
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            long r = 0;
            int i;
            for(i=0;i<groupPosition;i+=1){
                r += getChildrenCount(groupPosition);
            }
            r += childPosition;
            return r;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        private class GroupViewHolder {
            TextView name;

            public GroupViewHolder(View view) {
                this.name = (TextView) view.findViewById(R.id.name);
            }

            public void setItem(int title_id){
                this.name.setText(TextUtils.concat(getString(title_id), " ", new EmojiText(getActivity(), "ℹ️")));
            }
        }
        @Override
        public View getGroupView(int groupPosition, boolean isExpanded, View view, ViewGroup parent) {
            GroupViewHolder vh;
            if(view==null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.header_item, parent, false);
                vh = new GroupViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (GroupViewHolder) view.getTag();
            }
            vh.setItem((Integer) getGroup(groupPosition));
            return view;
        }

        private class ChildViewHolder {
            private Enchantment enchantment;
            private Channel channel;
            TextView name;
            Switch enable;
            View loading;

            public ChildViewHolder(View view) {
                this.name = (TextView) view.findViewById(R.id.name);
                this.enable = (Switch) view.findViewById(R.id.enable);
                this.loading = view.findViewById(R.id.loading);
                this.enable.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        postBinderTask(new CoreService.BinderTask() {
                            @Override
                            public void onBinderReady(CoreService.CoreBinder binder) {
                                if(enchantment!=null) {
                                    binder.toggleEnchantmentEnabled(enchantment);
                                }
                                if(channel!=null){
                                    binder.toggleRadiusEnabled(channel);
                                }
                            }
                        });
                    }
                });
            }

            public void setItem(Enchantment e){
                enchantment = e;
                channel = null;
                this.name.setText(e.name);
                if(e.enabled ==null){
                    this.loading.setVisibility(View.VISIBLE);
                    this.enable.setVisibility(View.GONE);
                }else{
                    this.loading.setVisibility(View.GONE);
                    this.enable.setVisibility(View.VISIBLE);
                    this.enable.setChecked(e.enabled);
                }
            }

            public void setItem(Channel c){
                enchantment = null;
                channel = c;
                this.name.setText(getString(R.string.radius_m, (int)c.radius));
                if(c.enable_radius ==null){
                    this.loading.setVisibility(View.VISIBLE);
                    this.enable.setVisibility(View.GONE);
                }else{
                    this.loading.setVisibility(View.GONE);
                    this.enable.setVisibility(View.VISIBLE);
                    this.enable.setChecked(c.enable_radius);
                }
            }
        }
        @Override
        public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
            if(groupPosition==0){
                ChildViewHolder vh;
                if(view==null){
                    view = LayoutInflater.from(getActivity()).inflate(R.layout.enchantment_item, parent, false);
                    vh = new ChildViewHolder(view);
                    view.setTag(vh);
                }else{
                    vh = (ChildViewHolder) view.getTag();
                }
                vh.setItem(mChannel);
            }else{
                Enchantment e = (Enchantment) getChild(groupPosition, childPosition);
                if(e!=null) {
                    ChildViewHolder vh;
                    if (view == null) {
                        view = LayoutInflater.from(getActivity()).inflate(R.layout.enchantment_item, parent, false);
                        vh = new ChildViewHolder(view);
                        view.setTag(vh);
                    } else {
                        vh = (ChildViewHolder) view.getTag();
                    }
                    vh.setItem(e);
                }else{
                    if (mFilterKeyword==null) {
                        if (view == null) {
                            view = LayoutInflater.from(getActivity()).inflate(R.layout.placeholder_item, parent, false);
                        }
                    } else {
                        if (view == null) {
                            view = LayoutInflater.from(getActivity()).inflate(R.layout.no_match_placeholder_item, parent, false);
                        }
                    }
                }
            }
            return view;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return true;
        }
    };

    private ActionMode.Callback mSelfAction = new ActionMode.Callback() {
        private final static int ACTION_EDIT = 0;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(0, ACTION_EDIT, 0, "✏️");

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            mode.finish();
            switch(item.getItemId()) {
                case ACTION_EDIT:
                    Activity activity = getActivity();
                    final View dialog_view = LayoutInflater.from(activity).inflate(R.layout.dialog_radius_edit,  null);
                    final Spinner spinner = (Spinner) dialog_view.findViewById(R.id.radius);
                    final ArrayList<Integer> radius_list = new ArrayList<>();
                    for (int r : Config.SELF_RADIUS) {
                        radius_list.add(r);
                    }
                    if(!radius_list.contains(mChannel.radius)){
                        radius_list.add(mChannel.radius);
                        Collections.sort(radius_list);
                    }
                    spinner.setAdapter(new BaseAdapter() {
                        class ViewHolder {
                            TextView label;

                            public ViewHolder(View view) {
                                view.setTag(this);
                                this.label = (TextView) view.findViewById(R.id.label);
                            }

                            void setItem(String text){
                                label.setText(text);
                            }
                        }

                        @Override
                        public int getCount() {
                            return radius_list.size();
                        }

                        @Override
                        public String getItem(int position) {
                            return getString(R.string.radius_m, radius_list.get(position));
                        }

                        @Override
                        public long getItemId(int position) {
                            return position;
                        }

                        @Override
                        public View getView(int position, View view, ViewGroup parent) {
                            ViewHolder vh;
                            if(view==null){
                                view = LayoutInflater.from(getActivity()).inflate(R.layout.radius_item, parent, false);
                                vh = new ViewHolder(view);
                            }else{
                                vh = (ViewHolder) view.getTag();
                            }
                            vh.setItem(getItem(position));
                            return view;
                        }
                    });
                    spinner.setSelection(radius_list.indexOf(mChannel.radius));
                    new AlertDialog.Builder(activity)
                            .setView(dialog_view)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final int r = radius_list.get(spinner.getSelectedItemPosition());
                                    postBinderTask(new CoreService.BinderTask() {
                                        @Override
                                        public void onBinderReady(CoreService.CoreBinder binder) {
                                            binder.setSelfRadius(mChannel, r);
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    };

    private ActionMode.Callback mEnchantmentAction = new ActionMode.Callback() {
        private final static int ACTION_EDIT = 0;
        private final static int ACTION_DELETE = 1;

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.add(0, ACTION_EDIT, 0, "✏️");
            menu.add(0, ACTION_DELETE, 0, "❌️");
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            mode.finish();
            Activity activity = getActivity();
            switch(item.getItemId()) {
                case ACTION_EDIT:
                    final View dialog_view = LayoutInflater.from(activity).inflate(R.layout.dialog_enchantment_edit,  null);
                    final EditText et_name = (EditText) dialog_view.findViewById(R.id.name);
                    et_name.setText(mEditingEnchantment.name);
                    new AlertDialog.Builder(activity)
                            .setView(dialog_view)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final String name = et_name.getText().toString();
                                    if(name.isEmpty())
                                        return;
                                    postBinderTask(new CoreService.BinderTask() {
                                        @Override
                                        public void onBinderReady(CoreService.CoreBinder binder) {
                                            try {
                                                JSONObject changes = new JSONObject();
                                                changes.put(Key.NAME, name);
                                                binder.updateEnchantment(mEditingEnchantment, changes);
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }

                                        }
                                    });
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                    return true;
                case ACTION_DELETE:
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.delete)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    postBinderTask(new CoreService.BinderTask() {
                                        @Override
                                        public void onBinderReady(CoreService.CoreBinder binder) {
                                            if(mEditingEnchantment!=null) {
                                                binder.deleteEnchantment(mEditingEnchantment);
                                            }
                                        }
                                    });
                                }
                            })
                            .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            }).show();
                    return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    };

    private Enchantment mEditingEnchantment;
    private String mFilterKeyword = null;
    private FilterBar mFilter;
    private ExpandableListView mListView;
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_enchantment, container, false);

        mFilter = (FilterBar) view.findViewById(R.id.filter);
        mListView = (ExpandableListView) view.findViewById(R.id.enchantment);

        getChannel(new ChannelActivity.GetChannelCallback() {
            @Override
            public void onGetChannel(final Channel channel) {
                mChannel = channel;

                postBinderTask(new CoreService.BinderTask() {
                    @Override
                    public void onBinderReady(CoreService.CoreBinder binder) {
                        mFilter.setCallback(new FilterBar.Callback() {
                            @Override
                            public void onFilter(String keyword) {
                                mFilterKeyword = keyword;
                                mHandler.post(mEnchantmentListener);
                            }
                        });

                        mAdapter = new EnchantmentAdapter();
                        mListView.setAdapter(mAdapter);
                        for(int i=0;i<mAdapter.getGroupCount();i+=1){
                            mListView.expandGroup(i);
                        }
                        mListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
                            @Override
                            public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
                                Enchantment enchantment = (Enchantment) mAdapter.getChild(groupPosition, childPosition);
                                if(enchantment!=null){
                                    ChannelActivity activity = (ChannelActivity) getActivity();
                                    activity.moveToEnchantment(enchantment);
                                    return true;
                                }
                                return false;
                            }
                        });
                        mListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                            @Override
                            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                                long packedPos = mListView.getExpandableListPosition(position);
                                int itemType = ExpandableListView.getPackedPositionType(id);

                                if (itemType == ExpandableListView.PACKED_POSITION_TYPE_CHILD) {
                                    int childPosition = ExpandableListView.getPackedPositionChild(packedPos);
                                    int groupPosition = ExpandableListView.getPackedPositionGroup(packedPos);

                                    if (groupPosition==0) {
                                        getActivity().startActionMode(mSelfAction);
                                    } else {
                                        mEditingEnchantment = (Enchantment) mAdapter.getChild(groupPosition, childPosition);
                                        if(mEditingEnchantment!=null) {
                                            getActivity().startActionMode(mEnchantmentAction);
                                        }
                                    }

                                    return true;
                                }
                                return false;
                            }
                        });

                        mListView.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
                            @Override
                            public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
                                switch (groupPosition){
                                    case 0:
                                        new DialogMobileEnchantment(getActivity());
                                        break;
                                    case 1:
                                    case 2:
                                        new DialogFixedEnchantment(getActivity());
                                        break;
                                }
                                return true; // disable click-to-collapse
                            }
                        });

                        binder.addChannelChangedListener(channel.id, mChannelListener);
                        binder.addEnchantmentListener(channel, mEnchantmentListener);
                    }
                });
            }
        });
        return view;
    }

    private Runnable mChannelListener = new Runnable() {
        @Override
        public void run() {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    private Runnable mEnchantmentListener = new Runnable() {
        @Override
        public void run() {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(final CoreService.CoreBinder binder) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            mEnchantmentList = binder.getChannelEnchantments(mChannel.id, mFilterKeyword);
                            mAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }
    };

    @Override
    public void onDestroyView() {
        postBinderTask(new CoreService.BinderTask() {
            @Override
            public void onBinderReady(CoreService.CoreBinder binder) {
                if(mChannel!=null){
                    binder.removeChannelChangedListener(mChannel.id, mChannelListener);
                    binder.removeEnchantmentListener(mChannel, mEnchantmentListener);
                }
            }
        });
        super.onDestroyView();
    }
}
