package com.example.android.popularmovies;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.android.popularmovies.adapter.MoviesAdapter;
import com.example.android.popularmovies.api.Client;
import com.example.android.popularmovies.api.Service;
import com.example.android.popularmovies.data.FavoriteContract;
import com.example.android.popularmovies.data.FavoriteDbHelper;
import com.example.android.popularmovies.model.Movie;
import com.example.android.popularmovies.model.MoviesResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.example.android.popularmovies.data.FavoriteContract.FavoriteEntry.CONTENT_URI;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView recyclerView;
    private MoviesAdapter adapter;
    private List<Movie> movieList = new ArrayList<>();
    ProgressDialog pd;
    private SwipeRefreshLayout swipeContainer;
    private FavoriteDbHelper favoriteDbHelper;
    private AppCompatActivity activity = MainActivity.this;
    public static final String LOG_TAG = MoviesAdapter.class.getName();
    private static final int FAVORITE_LOADER = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
    }

    public Activity getActivity(){
        Context context = this;
        while (context instanceof ContextWrapper){
            if (context instanceof  Activity){
                return (Activity) context;
            }
            context=((ContextWrapper) context).getBaseContext();
        }
        return null;
    }


    private void initViews(){

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        movieList = new ArrayList<>();
        adapter = new MoviesAdapter(this, movieList);
        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        }

        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        favoriteDbHelper = new FavoriteDbHelper(activity);


        swipeContainer = (SwipeRefreshLayout) findViewById(R.id.main_content);
        swipeContainer.setColorSchemeResources(android.R.color.holo_orange_dark);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh(){
                checkSortOrder();
                Toast.makeText(MainActivity.this, "Movies Refreshed", Toast.LENGTH_SHORT).show();
            }
        });
        checkSortOrder();
    }

    private void initViews2(){
        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        movieList = new ArrayList<>();
        getSupportLoaderManager().initLoader(FAVORITE_LOADER, null, this);
        //getAllFavorite();
    }

    private void loadJSON(){

        try{
            if (BuildConfig.THE_MOVIE_DB_API_TOKEN.isEmpty()){
                Toast.makeText(getApplicationContext(), "Please obtain API Key firstly from themoviedb.org", Toast.LENGTH_SHORT).show();
                pd.dismiss();
                return;
            }

            Client Client = new Client();
            Service apiService =
                    Client.getClient().create(Service.class);
            Call<MoviesResponse> call = apiService.getPopularMovies(BuildConfig.THE_MOVIE_DB_API_TOKEN);
            call.enqueue(new Callback<MoviesResponse>() {
                @Override
                public void onResponse(Call<MoviesResponse> call, Response<MoviesResponse> response) {
                    if (response.isSuccessful()){
                        if (response.body() != null){
                            List<Movie> movies = response.body().getResults();
                            recyclerView.setAdapter(new MoviesAdapter(getApplicationContext(), movies));
                            recyclerView.smoothScrollToPosition(0);
                            if (swipeContainer.isRefreshing()){
                                swipeContainer.setRefreshing(false);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<MoviesResponse> call, Throwable t) {
                    Log.d("Error", t.getMessage());
                    Toast.makeText(MainActivity.this, "Error Fetching Data!", Toast.LENGTH_SHORT).show();

                }
            });
        }catch (Exception e){
            Log.d("Error", e.getMessage());
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadJSON1(){

        try{
            if (BuildConfig.THE_MOVIE_DB_API_TOKEN.isEmpty()){
                Toast.makeText(getApplicationContext(), "Please obtain API Key firstly from themoviedb.org", Toast.LENGTH_SHORT).show();
                pd.dismiss();
                return;
            }

            Client Client = new Client();
            Service apiService =
                    Client.getClient().create(Service.class);
            Call<MoviesResponse> call = apiService.getTopRatedMovies(BuildConfig.THE_MOVIE_DB_API_TOKEN);
            call.enqueue(new Callback<MoviesResponse>() {
                @Override
                public void onResponse(Call<MoviesResponse> call, Response<MoviesResponse> response) {
                    List<Movie> movies = response.body().getResults();
                    recyclerView.setAdapter(new MoviesAdapter(getApplicationContext(), movies));
                    recyclerView.smoothScrollToPosition(0);
                    if (swipeContainer.isRefreshing()){
                        swipeContainer.setRefreshing(false);
                    }

                }

                @Override
                public void onFailure(Call<MoviesResponse> call, Throwable t) {
                    Log.d("Error", t.getMessage());
                    Toast.makeText(MainActivity.this, "Error Fetching Data!", Toast.LENGTH_SHORT).show();

                }
            });
        }catch (Exception e){
            Log.d("Error", e.getMessage());
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()){
            case R.id.menu_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void checkSortOrder(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String sortOrder = preferences.getString(
                this.getString(R.string.pref_sort_order_key),
                this.getString(R.string.pref_most_popular)
        );
        if (sortOrder.equals(this.getString(R.string.pref_most_popular))) {
            Log.d(LOG_TAG, "Sorting by most popular");
            loadJSON();
        } else if (sortOrder.equals(this.getString(R.string.favorite))){
            Log.d(LOG_TAG, "Sorting by favorite");
            initViews2();
        } else{
            Log.d(LOG_TAG, "Sorting by vote average");
            loadJSON1();
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        checkSortOrder();
    }

    @Override
    public void onResume(){
        super.onResume();
        if (movieList.isEmpty()){
            checkSortOrder();
        }else{

            checkSortOrder();
        }
    }

   /* private void getAllFavorite(){
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void... params){
                movieList.clear();
                movieList.addAll(favoriteDbHelper.getAllFavorite());
                return null;
            }
            @Override
            protected void onPostExecute(Void aVoid){
                super.onPostExecute(aVoid);
                adapter.notifyDataSetChanged();
            }
        }.execute();
    }
*/
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = {
                FavoriteContract.FavoriteEntry._ID,
                FavoriteContract.FavoriteEntry.COLUMN_MOVIEID,
                FavoriteContract.FavoriteEntry.COLUMN_TITLE,
                FavoriteContract.FavoriteEntry.COLUMN_USERRATING,
                FavoriteContract.FavoriteEntry.COLUMN_POSTER_PATH,
                FavoriteContract.FavoriteEntry.COLUMN_PLOT_SYNOPSIS
        };

        return new CursorLoader(this,   // Parent activity context
                CONTENT_URI,   // Provider content URI to query
                projection,             // Columns to include in the resulting Cursor
                null,                   // No selection clause
                null,                   // No selection arguments
                null);                  // Default sort order
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null || cursor.isClosed()) {
            return;
        }else{
            if (cursor.moveToFirst()) {
                do {
                    Movie movie = new Movie();
                    movie.setId(Integer.parseInt(cursor.getString(cursor.getColumnIndex(FavoriteContract.FavoriteEntry.COLUMN_MOVIEID))));
                    movie.setOriginalTitle(cursor.getString(cursor.getColumnIndex(FavoriteContract.FavoriteEntry.COLUMN_TITLE)));
                    movie.setVoteAverage(Double.parseDouble(cursor.getString(cursor.getColumnIndex(FavoriteContract.FavoriteEntry.COLUMN_USERRATING))));
                    movie.setPosterPath(cursor.getString(cursor.getColumnIndex(FavoriteContract.FavoriteEntry.COLUMN_POSTER_PATH)));
                    movie.setOverview(cursor.getString(cursor.getColumnIndex(FavoriteContract.FavoriteEntry.COLUMN_PLOT_SYNOPSIS)));
                    movieList.add(movie);
                } while (cursor.moveToNext());
            }
            cursor.close();
        }

        adapter = new MoviesAdapter(this, movieList);

        if (getActivity().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){
            recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        } else {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        }

        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        favoriteDbHelper = new FavoriteDbHelper(activity);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}
