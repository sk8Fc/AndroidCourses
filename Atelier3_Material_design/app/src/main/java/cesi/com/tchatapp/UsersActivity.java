package cesi.com.tchatapp;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.io.InputStream;

import cesi.com.tchatapp.helper.JsonParser;
import cesi.com.tchatapp.helper.NetworkHelper;
import cesi.com.tchatapp.utils.Constants;

/**
 * Created by sca on 08/07/15.
 */
public class UsersActivity extends Activity {

    private ListView list;
    private ArrayAdapter<String> adapter;
    String token = null;
    private SwipeRefreshLayout swipeLayout;

    @Override
    public void onCreate(Bundle savedInstace){
        super.onCreate(savedInstace);
        setContentView(R.layout.activity_users);
        token = getIntent().getExtras().getString(Constants.INTENT_TOKEN);
        list = (ListView) findViewById(R.id.list);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, new LinkedList<String>());
        list.setAdapter(adapter);
        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.users_swiperefresh);
        setupRefreshLayout();
    }


    @Override
    public void onResume(){
        super.onResume();
        loading();
    }
    private void loading() {
        swipeLayout.setRefreshing(true);
        new GetUsersAsyncTask().execute();
    }

    /**
     * Setup refresh layout
     */
    private void setupRefreshLayout() {
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loading();
            }
        });
        swipeLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimaryDark, R.color.colorPrimary);
        /**
         * this is to avoid error on double scroll on listview/swipeRefreshLayout
         */
        list.setOnScrollListener(new AbsListView.OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                boolean enable = false;
                if (list != null && list.getChildCount() > 0) {
                    // check if the first item of the list is visible
                    boolean firstItemVisible = list.getFirstVisiblePosition() == 0;
                    // check if the top of the first item is visible
                    boolean topOfFirstItemVisible = list.getChildAt(0).getTop() == 0;
                    // enabling or disabling the refresh layout
                    enable = firstItemVisible && topOfFirstItemVisible;
                }
                swipeLayout.setEnabled(enable);
            }
        });
    }

    /**
     * AsyncTask for sign-in
     */
    protected class GetUsersAsyncTask extends AsyncTask<String, Void, List<String>> {

        @Override
        protected List<String> doInBackground(String... params) {
            if(!NetworkHelper.isInternetAvailable(UsersActivity.this)){
                return null;
            }

            InputStream inputStream = null;

            try {
                //then create an httpClient.
                URL url = new URL(UsersActivity.this.getString(R.string.url_users));
                Log.d("Calling URL", url.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                //set authorization header
                conn.setRequestProperty("Authorization", "Bearer " + token);

                int responseCode = conn.getResponseCode();
                Log.d("UsersActivity", "The response code is: " + responseCode);
                Log.d("UsersActivity", "The response message is: " + conn.getResponseMessage());

                String contentAsString = null;
                if(responseCode == 200) {
                    inputStream = conn.getInputStream();
                    // Convert the InputStream into a string
                    contentAsString = NetworkHelper.readIt(inputStream);
                    return JsonParser.getUsers(contentAsString);
                }
                return null;
                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } catch (Exception e){
                Log.d(Constants.TAG, "Error occured in your AsyncTask : ", e);
                return null;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        Log.e("NetworkHelper", e.getMessage());
                    }
                }
            }
        }

        @Override
        public void onPostExecute(final List<String> users){
            if (users != null){
                adapter.clear();
                adapter.addAll(users);
            }

            adapter.notifyDataSetChanged();
            swipeLayout.setRefreshing(false);
        }
    }
}
