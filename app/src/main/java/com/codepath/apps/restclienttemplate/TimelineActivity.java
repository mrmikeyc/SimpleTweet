package com.codepath.apps.restclienttemplate;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.method.TimeKeyListener;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.codepath.apps.restclienttemplate.models.Tweet;
import com.codepath.asynchttpclient.callback.JsonHttpResponseHandler;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.parceler.Parcels;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Headers;

// This acts as the main activity for our Tweets Application
public class TimelineActivity extends AppCompatActivity {

    private static final String TAG = TimelineActivity.class.getSimpleName();
    private static final int COMPOSE_ACTIVITY_REQUEST_CODE = 20;

    TwitterClient client;
    RecyclerView rvTweets;
    List<Tweet> tweets;
    TweetsAdapter adapter;
    SwipeRefreshLayout swipeContainer;
    EndlessRecyclerViewScrollListener scrollListener;
    FloatingActionButton fab;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if the menu is present
        getMenuInflater().inflate(R.menu.menu_main, menu);
        // return true;
        //return super.onCreateOptionsMenu(menu);

        // To hide the menu bar
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.compose) {
            // Compose icon has been tapped
            // Navigate to compose activity
            Intent intent = new Intent(this, ComposeActivity.class);

            // This will launch the compose activity as a CHILD of this Timeline View
            startActivityForResult(intent, COMPOSE_ACTIVITY_REQUEST_CODE);
            return true;
        }
        return false;
        // return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        Log.i(TAG, "Received compose activity's result");
        if (requestCode == COMPOSE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK) {
            // Get data from the intent (tweet object)
            Tweet tweet = Parcels.unwrap(data.getParcelableExtra("TWEET"));

            // Update the recyclerview manually with this new tweet so we don't have to refresh via API call
            // Modify data source of the tweets
            tweets.add(0, tweet);

            // Update the adapter
            adapter.notifyItemInserted(0);

            // Finally, scroll to the top of the recycler view so we don't have to once we click the tweet button
            rvTweets.smoothScrollToPosition(0);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void goToComposeActivity() {
        Intent intent = new Intent(this, ComposeActivity.class);
        startActivityForResult(intent, COMPOSE_ACTIVITY_REQUEST_CODE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timeline);

        client = TwitterApp.getRestClient(this);

        fab = findViewById(R.id.fabCompose);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.i(TAG, "FAB Clicked");
                goToComposeActivity();
            }
        });

        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.i(TAG, "Pull to refresh initiated - fetching new data");
                populateHomeTimeline();
            }
        });

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        // Use the adapter TweetsAdapter
        // 1. Find recyclerview
        rvTweets = findViewById(R.id.rvTweets);
        // 2. Init the list of tweets into the adapter
        tweets = new ArrayList<>();
        adapter = new TweetsAdapter(this, tweets);
        // Recylerview setup
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvTweets.setLayoutManager(linearLayoutManager);
        rvTweets.setAdapter(adapter);

        scrollListener = new EndlessRecyclerViewScrollListener(linearLayoutManager) {
            @Override
            public void onLoadMore(int page, int totalItemsCount, RecyclerView view) {
                // This will be automatically called when we hit the bottom of the list
                Log.i(TAG, "onLoadMore called: " + page);
                loadMoreData();
            }
        };

        // Add the scroll listener to the rv
        rvTweets.addOnScrollListener(scrollListener);

        populateHomeTimeline();
    }

    // this is where we will make another API call to get the next page of tweets and add the objects to our current list of tweets
    public void loadMoreData() {
        // 1. Send an API request to retrieve appropriate paginated data
        client.getNextPageOfTweets(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "onSuccess for LoadMoreData" + json.toString());

                // 2. Deserialize and construct new model objects from the API response
                JSONArray jsonArray = json.jsonArray;
                try {
                    List<Tweet> tweets = Tweet.fromJsonArray(jsonArray);
                    // 3. Append the new data objects to the existing set of items inside the array of items
                    // 4. Notify the adapter of the new items made with `notifyItemRangeInserted()`
                    adapter.addAll(tweets);
                } catch (JSONException | ParseException e) {
                    Log.e(TAG, "JSON Deserializing error in loadMoreData: ", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "onFailure for LoadMoreData " + response, throwable);
            }
        }, tweets.get(tweets.size()-1).id);
    }

    private void populateHomeTimeline() {
        client.getHomeTimeline(new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Headers headers, JSON json) {
                Log.i(TAG, "Json Response onSuccess: " + json.toString());

                JSONArray jsonArray = json.jsonArray;
                try {
                    // Here, utilize the swipe to refresh methods in the adapter to get new data when refreshing
                    adapter.clear();
                    adapter.addAll(Tweet.fromJsonArray(jsonArray));
                    swipeContainer.setRefreshing(false);

                    // tweets.addAll(Tweet.fromJsonArray(jsonArray));
                    // adapter.notifyDataSetChanged();
                } catch (JSONException | ParseException e) {
                    Log.e(TAG, "Error with JSONArray: ", e);
                }
            }

            @Override
            public void onFailure(int statusCode, Headers headers, String response, Throwable throwable) {
                Log.e(TAG, "Json Response onFailure:" + response, throwable);
            }
        });
    }
}