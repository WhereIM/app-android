package im.where.whereim;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

import im.where.whereim.geo.QuadTree;
import im.where.whereim.models.POI;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by buganini on 19/01/17.
 */

public class PaneMapboxSearch extends PaneSearch {


    private void getApiKey(final CoreService.ApiKeyCallback callback){
        callback.apiKey(Config.API_KEY_MAPBOX);
    }

    @Override
    public Config.MapProvider getProvider() {
        return Config.MapProvider.MAPBOX;
    }

    @Override
    public void search(final String keyword) {
        final ChannelActivity activity = (ChannelActivity) getActivity();

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
                                .host("api.mapbox.com")
                                .addPathSegment("geocoding")
                                .addPathSegment("v5")
                                .addPathSegment("mapbox.places")
                                .addPathSegment(keyword+".json")
                                .addQueryParameter("access_token", api_key)
                                .addQueryParameter("proximity", String.format(Locale.ENGLISH, "%f,%f", latlng.longitude, latlng.latitude))
                                .build();
                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(url)
                                .addHeader("Referer", "where.im")
                                .build();
                        try {
                            Response response = client.newCall(request).execute();
                            JSONObject ret = new JSONObject(response.body().string());
                            final String attribution = ret.getString("attribution");
                            JSONArray features = ret.getJSONArray("features");
                            final ArrayList<POI> res = new ArrayList<POI>();
                            for(int i=0;i<features.length();i+=1){
                                JSONObject feature = features.getJSONObject(i);
                                POI poi = new POI();
                                String place_name = feature.getString("place_name");
                                JSONArray center = feature.getJSONArray("center");
                                poi.name = place_name;
                                poi.longitude = center.getDouble(0);
                                poi.latitude = center.getDouble(1);
                                res.add(poi);
                            }

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setTextAttribution(attribution);
                                    setSearchResult(res);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
    }

    @Override
    protected void autoComplete(final String keyword) {
        final ChannelActivity activity = (ChannelActivity) getActivity();

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
                                .host("api.mapbox.com")
                                .addPathSegment("geocoding")
                                .addPathSegment("v5")
                                .addPathSegment("mapbox.places")
                                .addPathSegment(keyword+".json")
                                .addQueryParameter("access_token", api_key)
                                .addQueryParameter("autocomplete", "true")
                                .addQueryParameter("proximity", String.format(Locale.ENGLISH, "%f,%f", latlng.longitude, latlng.latitude))
                                .build();

                        OkHttpClient client = new OkHttpClient();
                        Request request = new Request.Builder()
                                .url(url)
                                .addHeader("Referer", "where.im")
                                .build();
                        try {
                            Response response = client.newCall(request).execute();
                            JSONObject ret = new JSONObject(response.body().string());
                            final String attribution = ret.getString("attribution");
                            JSONArray features = ret.getJSONArray("features");
                            final ArrayList<String> res = new ArrayList<String>();
                            for(int i=0;i<features.length();i+=1){
                                JSONObject feature = features.getJSONObject(i);
                                String place_name= feature.getString("place_name");
                                res.add(place_name);
                            }

                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    setTextAttribution(attribution);
                                    setAutoComplete(res);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });
    }

    private class SearchResultAdapter extends BaseAdapter {
        private class ViewHolder {
            TextView name;

            public ViewHolder(View view){
                name = (TextView) view.findViewById(R.id.name);
            }

            public void setItem(POI result){
                name.setText(result.name);
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
                view = LayoutInflater.from(getActivity()).inflate(R.layout.mapbox_search_result_item, parent, false);
                vh = new ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (ViewHolder) view.getTag();
            }

            vh.setItem((POI) getItem(position));

            return view;
        }
    }

    @Override
    protected BaseAdapter getSearchResultAdapter() {
        return new SearchResultAdapter();
    }

    class AutoCompleteAdapter extends BaseAdapter {
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
            return mPredictions.size();
        }

        @Override
        public Object getItem(int i) {
            return mPredictions.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            AutoCompleteAdapter.ViewHolder vh;
            if(view == null){
                view = LayoutInflater.from(getActivity()).inflate(R.layout.autocomplete_item, parent, false);
                vh = new AutoCompleteAdapter.ViewHolder(view);
                view.setTag(vh);
            }else{
                vh = (AutoCompleteAdapter.ViewHolder) view.getTag();
            }

            vh.setItem((String) getItem(position));

            return view;
        }
    }

    @Override
    protected BaseAdapter getAutoCompleteAdapter() {
        return new AutoCompleteAdapter();
    }
}
