package cesi.com.tchatapp;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import cesi.com.tchatapp.adapter.MessagesAdapter;
import cesi.com.tchatapp.helper.JsonParser;
import cesi.com.tchatapp.helper.NetworkHelper;
import cesi.com.tchatapp.model.Message;
import cesi.com.tchatapp.utils.Constants;

/**
 * Created by sca on 02/06/15.
 */
public class TchatActivity extends ActionBarActivity {

    RecyclerView listView;
    FloatingActionButton fab;
    MessagesAdapter adapter;

    String token;

    List<Message> messages;

    private LinearLayoutManager mLayoutManager;
    private Toolbar mToolbar;
    private DrawerLayout mDrawerLayout;

    @Override
    public void onCreate(Bundle savedInstance) {
        super.onCreate(savedInstance);
        setContentView(R.layout.activity_tchat);
        token = this.getIntent().getExtras().getString(Constants.INTENT_TOKEN);
        if (token == null) {
            Toast.makeText(this, this.getString(R.string.error_no_token), Toast.LENGTH_SHORT).show();
            finish();
        }
        listView = (RecyclerView) findViewById(R.id.tchat_list);
        listView.setHasFixedSize(true);
        // use a linear layout manager
        mLayoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(mLayoutManager);

        mToolbar = (Toolbar) findViewById(R.id.tchat_toolbar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WriteMsgDialog d = WriteMsgDialog.getInstance(token);
                d.show(TchatActivity.this.getFragmentManager(), "write");

            }
        });

        adapter = new MessagesAdapter(this);
        listView.setAdapter(adapter);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (navigationView != null) {
            setupNavigationView(navigationView);
        }


        refresh();
    }

    /**
     * setup drawer.
     * @param navigationView
     */
    private void setupNavigationView(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(MenuItem menuItem) {
                        if(menuItem.getItemId() == R.id.tchat_disconnect){
                            //TODO  Your turn
                            TchatActivity.this.finish();

                        } if(menuItem.getItemId() == R.id.tchat_users){
                            Intent i = new Intent(TchatActivity.this, UsersActivity.class);
                            i.putExtra(Constants.INTENT_TOKEN, token);
                            startActivity(i);
                        } else if(menuItem.getItemId() == R.id.tchat_tchat) {
                            refresh();
                        } else {
                            menuItem.setChecked(true);
                            mDrawerLayout.closeDrawers();
                        }
                        return true;
                    }
                });
    }

    private void refresh() {
        new GetMessagesAsyncTask(this).execute();
    }
    /**
     * AsyncTask for sign-in
     */
    protected class GetMessagesAsyncTask extends AsyncTask<String, Void, List<Message>> {

        Context context;

        public GetMessagesAsyncTask(final Context context) {
            this.context = context;
        }

        @Override
        protected List<Message> doInBackground(String... params) {
            if(!NetworkHelper.isInternetAvailable(context)){
                return null;
            }

            InputStream inputStream = null;

            try {
                URL url = new URL(context.getString(R.string.url_msg));
                Log.d("Calling URL", url.toString());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                //set authorization header
                conn.setRequestProperty("Authorization", "Bearer " + token);


                int response = conn.getResponseCode();
                Log.d("TchatActivity", "The response code is: " + response);

                inputStream = conn.getInputStream();
                String contentAsString = null;
                if(response == 200) {
                    // Convert the InputStream into a string
                    contentAsString = NetworkHelper.readIt(inputStream);
                    return JsonParser.getMessages(contentAsString);
                }
                return null;

                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } catch (Exception e) {
                Log.e("NetworkHelper", e.getMessage());
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
        public void onPostExecute(final List<Message> msgs){
            int nb = 0;
            if(msgs != null){
                nb = msgs.size();
                adapter.addMessage(msgs);
            }
            Toast.makeText(TchatActivity.this, "loaded nb messages: "+nb, Toast.LENGTH_LONG).show();
        }
    }


}
