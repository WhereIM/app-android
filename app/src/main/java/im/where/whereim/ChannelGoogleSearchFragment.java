package im.where.whereim;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.Html;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.NativeExpressAdView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import im.where.whereim.geo.QuadTree;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by buganini on 19/01/17.
 */

public class ChannelGoogleSearchFragment extends ChannelSearchFragment {

    private String cachedApiKey = null;

    private void getApiKey(final CoreService.ApiKeyCallback callback){
        if (cachedApiKey!=null) {
            callback.apiKey(cachedApiKey);
        } else {
            postBinderTask(new CoreService.BinderTask() {
                @Override
                public void onBinderReady(CoreService.CoreBinder binder) {
                    binder.getApiKey(Key.GOOGLE_SEARCH, new CoreService.ApiKeyCallback() {
                        @Override
                        public void apiKey(String api_key) {
                            cachedApiKey = api_key;
                            callback.apiKey(api_key);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void search(final String keyword) {
        final ChannelActivity activity = (ChannelActivity) getActivity();
        if(activity!=null) {
            View view = activity.getCurrentFocus();
            if (view != null) {
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }

        if(keyword.isEmpty()){
            mAdView.setVisibility(View.VISIBLE);
            setSearchResult(new ArrayList<SearchResult>());
            return;
        }

        mAdView.setVisibility(View.GONE);

        mLoading.setVisibility(View.VISIBLE);
        mListView.setAdapter(null);
        getApiKey(new CoreService.ApiKeyCallback() {
            @Override
            public void apiKey(final String api_key) {
                if(activity==null)
                    return;
                new Thread(){
                    @Override
                    public void run() {
                        QuadTree.LatLng latlng = activity.getMapCenter();
                        HttpUrl url = new HttpUrl.Builder()
                                .scheme("https")
                                .host("maps.googleapis.com")
                                .addPathSegment("maps")
                                .addPathSegment("api")
                                .addPathSegment("place")
                                .addPathSegment("textsearch")
                                .addPathSegment("json")
                                .addQueryParameter("key", api_key)
                                .addQueryParameter("query", keyword)
                                .addQueryParameter("location", String.format(Locale.ENGLISH, "%f,%f", latlng.latitude, latlng.longitude))
                                .addQueryParameter("rankby", "distance")
                                .build();
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(url)
                                .addHeader("Referer", "where.im")
                                .build();
                        try {
                            Response response = client.newCall(request).execute();
                            JSONObject ret = new JSONObject(response.body().string());
                            String status = ret.getString("status");
                            if("REQUEST_DENIED".equals(status)){
                                postBinderTask(new CoreService.BinderTask() {
                                    @Override
                                    public void onBinderReady(CoreService.CoreBinder binder) {
                                        cachedApiKey = null;
                                        binder.invalidateApiKey(Key.GOOGLE_SEARCH);
                                        mHandler.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                search(keyword);
                                            }
                                        });
                                    }
                                });
                                return;
                            }
                            if("OVER_QUERY_LIMIT".equals(status)){
                                Toast.makeText(activity, R.string.error, Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if("ZERO_RESULTS".equals(status)){
                                setSearchResult(new ArrayList<SearchResult>());
                                return;
                            }
                            if("OK".equals(status)){
                                JSONArray results = ret.getJSONArray("results");
                                ArrayList<SearchResult> res = new ArrayList<SearchResult>();
                                for(int i=0;i<results.length();i+=1){
                                    JSONObject result = results.getJSONObject(i);
                                    Double lat = null;
                                    Double lng = null;
                                    String name = null;
                                    String address = null;
                                    Spanned attribution = null;
                                    try{
                                        JSONObject geometry = result.getJSONObject("geometry");
                                        JSONObject location = geometry.getJSONObject("location");
                                        lat = location.getDouble("lat");
                                        lng = location.getDouble("lng");

                                        name = result.getString("name");
                                        address = result.getString("formatted_address");

                                        String attr = result.optString("attribution");
                                        if(attr!=null){
                                            attribution = Html.fromHtml(attr);
                                        }
                                    } catch(Exception e) {
                                        e.printStackTrace();
                                        continue;
                                    }
                                    GoogleSearchResult r = new GoogleSearchResult();
                                    r.name = name;
                                    r.address = address;
                                    r.attribution = attribution;
                                    r.latitude = lat;
                                    r.longitude = lng;
                                    res.add(r);
                                }
                                setSearchResult(res);
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        mClear.setVisibility(View.VISIBLE);
                                        mSearch.setVisibility(View.GONE);
                                    }
                                });
                                return;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
    }

    public static class GoogleSearchResult extends ChannelSearchFragment.SearchResult {
        String address;
        Spanned attribution;
    }

    private ArrayList<SearchResult> mSearchResult = new ArrayList<>();
    private void setSearchResult(final ArrayList<SearchResult> result){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final ChannelActivity activity = (ChannelActivity) getActivity();
                activity.setSearchResult(result);
                mLoading.setVisibility(View.GONE);
                mSearchResult = result;
                mAdapter.notifyDataSetChanged();
                mListView.setAdapter(mAdapter);
            }
        });
    }

    class SearchResultAdapter extends BaseAdapter {
        private class ViewHolder {
            TextView name;
            TextView address;
            TextView attribution;

            public ViewHolder(View view){
                name = (TextView) view.findViewById(R.id.name);
                address = (TextView) view.findViewById(R.id.address);
                attribution = (TextView) view.findViewById(R.id.attribution);
            }

            public void setItem(GoogleSearchResult result){
                name.setText(result.name);
                if(result.address==null){
                    address.setVisibility(View.GONE);
                }else{
                    address.setText(result.address);
                    address.setVisibility(View.VISIBLE);
                }
                if(result.attribution==null){
                    attribution.setVisibility(View.GONE);
                }else{
                    attribution.setText(result.attribution);
                    attribution.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public int getCount() {
            return mSearchResult.size();
        }

        @Override
        public Object getItem(int position) {
            return mSearchResult.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            ViewHolder vh;
            if(view == null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.search_result_item, parent, false);
                vh = new ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ViewHolder) view.getTag();
            }

            vh.setItem((GoogleSearchResult) getItem(position));

            return view;
        }
    }

    private NativeExpressAdView mAdView;
    private Button mSearch;
    private Button mClear;
    private View mLoading;
    private ListView mListView;
    private SearchResultAdapter mAdapter = new SearchResultAdapter();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_channel_search, container, false);

        mAdView = (NativeExpressAdView) view.findViewById(R.id.adView);

        AdRequest request = new AdRequest.Builder()
                .build();
        mAdView.loadAd(request);

        mLoading = view.findViewById(R.id.loading);

        final EditText keyword = (EditText) view.findViewById(R.id.keyword);
        keyword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String q = keyword.getText().toString();
                    search(q);
                    handled = true;
                }
                return handled;
            }
        });
        keyword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mClear.setVisibility(View.GONE);
                mSearch.setVisibility(View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mClear = (Button) view.findViewById(R.id.clear);
        mClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mClear.setVisibility(View.GONE);
                mSearch.setVisibility(View.VISIBLE);
                keyword.setText("");
                search("");
            }
        });

        mSearch = (Button) view.findViewById(R.id.search);
        mSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String q = keyword.getText().toString();
                search(q);
            }
        });

        mListView = (ListView) view.findViewById(R.id.results);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final ChannelActivity activity = (ChannelActivity) getActivity();
                if(activity!=null){
                    activity.moveToSearchResult(position);
                }
            }
        });

        return view;
    }
}
