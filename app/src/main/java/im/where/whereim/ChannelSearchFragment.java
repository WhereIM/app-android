package im.where.whereim;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

import im.where.whereim.models.POI;

/**
 * Created by buganini on 19/01/17.
 */

abstract public class ChannelSearchFragment extends AuxFragment {
    public static ChannelSearchFragment newFragment(Context context){
        switch(Config.getMapProvider(context)){
            case GOOGLE:
                return new ChannelGoogleSearchFragment();
            case MAPBOX:
                return new ChannelMapboxSearchFragment();
        }
        return null;
    }

    private class SearchHistoryAdapter extends BaseAdapter {
        private class ViewHolder {
            TextView text;

            public ViewHolder(View view){
                text = (TextView) view.findViewById(R.id.text);
            }

            public void setItem(String prediction){
                text.setText(prediction);
            }
        }

        @Override
        public int getCount() {
            return mSearchHistory.size();
        }

        @Override
        public Object getItem(int i) {
            return mSearchHistory.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder vh;
            if(view == null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.search_history_item, parent, false);
                vh = new ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ViewHolder) view.getTag();
            }

            vh.setItem((String) getItem(position));

            return view;
        }
    }

    protected Handler mHandler = new Handler();

    abstract protected void search(String keyword);
    abstract protected void autoComplete(String keyword);
    abstract protected BaseAdapter getSearchResultAdapter();
    abstract protected BaseAdapter getAutoCompleteAdapter();

    private void do_search(final String keyword){
        channelActivity.closeKeyboard();

        if(keyword.isEmpty()){
            setSearchResult(new ArrayList<POI>());
            showHistory();
            return;
        }

        mHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (mSearchHistory) {
                    mSearchHistory.remove(keyword);
                    mSearchHistory.add(0, keyword);
                    while(mSearchHistory.size() > 15){
                        mSearchHistory.remove(mSearchHistory.size()-1);
                    }
                    mSearchHistoryAdapter.notifyDataSetChanged();
                    commit();
                }
            }
        });

        mLoading.setVisibility(View.VISIBLE);
        mListView.setAdapter(null);

        search(keyword);
    }

    protected ArrayList<POI> mSearchResult = new ArrayList<>();
    protected void setSearchResult(final ArrayList<POI> result){
        if (result.size()>0) {
            mSearch.setVisibility(View.GONE);
            mClear.setVisibility(View.VISIBLE);
        } else {
            mSearch.setVisibility(View.VISIBLE);
            mClear.setVisibility(View.GONE);

        }

        final ChannelActivity activity = (ChannelActivity) getActivity();
        activity.setSearchResult(result);
        mLoading.setVisibility(View.GONE);
        mSearchResult = result;
        mSearchResultAdapter.notifyDataSetChanged();
        mListView.setAdapter(mSearchResultAdapter);
    }

    protected ArrayList<String> mPredictions = new ArrayList<>();
    protected void setAutoComplete(final ArrayList<String> predictions){
        mLoading.setVisibility(View.GONE);
        mPredictions = predictions;
        mAutoCompleteAdapter.notifyDataSetChanged();
        mListView.setAdapter(mAutoCompleteAdapter);
    }

    protected void setGoogleAttribution(){
        mGoogleAttribution.setVisibility(View.VISIBLE);
        mTextAttribution.setVisibility(View.GONE);
    }

    protected void setTextAttribution(String text){
        mGoogleAttribution.setVisibility(View.GONE);
        mTextAttribution.setVisibility(View.VISIBLE);
        mTextAttribution.setText(text);
    }

    protected void clearAttribution(){
        mGoogleAttribution.setVisibility(View.GONE);
        mTextAttribution.setVisibility(View.GONE);
    }

    protected void showHistory(){
        clearAttribution();
        mLoading.setVisibility(View.GONE);
        mListView.setAdapter(mSearchHistoryAdapter);
    }

    private EditText mKeyword;
    private View mSearch;
    private View mClear;
    private View mLoading;
    private ListView mListView;
    private View mGoogleAttribution;
    private TextView mTextAttribution;
    private BaseAdapter mSearchResultAdapter = getSearchResultAdapter();
    private BaseAdapter mAutoCompleteAdapter = getAutoCompleteAdapter();
    private BaseAdapter mSearchHistoryAdapter = new SearchHistoryAdapter();

    @Override
    public boolean isResizable() {
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_search, container, false);

        mClear = view.findViewById(R.id.clear);
        mClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSizePolicy(ChannelActivity.AuxSize.FULL);
                clearAttribution();
                mKeyword.setText("");
                mKeyword.clearFocus();
                do_search("");
                mSearch.setVisibility(View.VISIBLE);
                mClear.setVisibility(View.GONE);
            }
        });

        mSearch = view.findViewById(R.id.search);
        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String q = mKeyword.getText().toString();
                do_search(q);
            }
        });

        mListView = view.findViewById(R.id.results);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(mListView.getAdapter()==mSearchResultAdapter){
                    final ChannelActivity activity = (ChannelActivity) getActivity();
                    if(activity!=null){
                        activity.moveToSearchResult(position, true);
                    }
                    return;
                }
                if(mListView.getAdapter()==mAutoCompleteAdapter){
                    String k = (String)mAutoCompleteAdapter.getItem(position);
                    do_search(k);
                    mKeyword.setText(k);
                    return;
                }
                if(mListView.getAdapter()==mSearchHistoryAdapter){
                    String k = (String)mSearchHistoryAdapter.getItem(position);
                    do_search(k);
                    mKeyword.setText(k);
                    return;
                }
            }
        });
        mListView.setAdapter(mSearchResultAdapter);

        mGoogleAttribution = view.findViewById(R.id.google_attribution);
        mTextAttribution = view.findViewById(R.id.text_attribution);

        mLoading = view.findViewById(R.id.loading);

        mKeyword = view.findViewById(R.id.keyword);
        mKeyword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String q = mKeyword.getText().toString();
                    do_search(q);
                    handled = true;
                }
                return handled;
            }
        });
        mKeyword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mKeyword.isFocused() && mKeyword.getText().toString().isEmpty()) {
                    showHistory();
                }
            }
        });
        mKeyword.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (b) {
                    showHistory();
                } else {
                    if(mKeyword.getText().toString().isEmpty()){
                        do_search("");
                    }
                }
            }
        });
        mKeyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mClear.setVisibility(View.GONE);
                mSearch.setVisibility(View.VISIBLE);
                if (s.length()==0) {
                    showHistory();
                } else {
                    autoComplete(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        showHistory();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        checkout();
    }

    private ArrayList<String> mSearchHistory = new ArrayList<>();
    private void checkout(){
        SharedPreferences sp = getActivity().getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
        String history = sp.getString(Key.SEARCH_HISTORY, "[]");
        try {
            JSONArray a = new JSONArray(history);
            synchronized (mSearchHistory) {
                mSearchHistory.clear();
                for(int i=0;i<a.length();i+=1){
                    String k = a.getString(i);
                    mSearchHistory.add(k);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void commit(){
        JSONArray a = new JSONArray();
        synchronized (mSearchHistory) {
            for (String k : mSearchHistory) {
                a.put(k);
            }
        }
        SharedPreferences.Editor editor = getActivity().getSharedPreferences(Config.APP_SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(Key.SEARCH_HISTORY, a.toString());
        editor.apply();
    }
}
