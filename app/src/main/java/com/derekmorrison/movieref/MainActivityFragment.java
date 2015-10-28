package com.derekmorrison.movieref;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This Fragment contains a GridView of movie posters. The list is based on the most popular
 * or the highest vote score (user setting). Fetching the movie data from The Movie Database
 * (TMDB) is done on an Async thread and the data for each movie is stored in an instance
 * of MovieData. The movie data is loaded into a custom ArrayAdapter (MovieArrayAdapter) that
 * is connected to the GridView. The MovieArrayAdapter uses Picasso to handle getting and
 * caching the movie poster images from TMDB.
 * The GridView has it's onItemClicked listener set to invoke the MovieDetailActivity using
 * an Intent, therefore if the user touches a movie poster the Details display appears
 */
public class MainActivityFragment extends Fragment {

    private MovieData[] mResult;
    private MovieArrayAdapter mMovieAdapter;

    public MainActivityFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // savedInstanceState always seems to be null
        if (savedInstanceState != null) {

            // all the MovieData objects were saved in the Bundle
            mResult = (MovieData[]) savedInstanceState.getParcelableArray("LastMovieData");

            if (mResult != null) {
                ArrayList<MovieData> movieList = new ArrayList(Arrays.asList(mResult));
                mMovieAdapter = new MovieArrayAdapter(
                        getActivity(),
                        movieList
                );
            }
        }

        // Add this line in order for this fragment to handle menu events.
        setHasOptionsMenu(true);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArray("LastMovieData", mResult);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // if the mMovieAdapter was just restored by the onCreate method then we don't need to create a new one
        if (mMovieAdapter == null) {
            MovieData tempMovie = new MovieData("Loading...", "", "", "", "Please be patient");
            ArrayList<MovieData> mList = new ArrayList(Arrays.asList(tempMovie));

            mMovieAdapter = new MovieArrayAdapter(
                getActivity(),
                mList
            );
        }

        // get a reference to the Retry Connection button and setup a listener
        Button rButton = (Button) rootView.findViewById(R.id.retryButton);
        rButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    updateMovieList();
                }
            }
        );


        // Get a reference to the GridView, and attach the Movie Adapter to it.
        GridView gridView = (GridView) rootView.findViewById(R.id.poster_gridView);
        gridView.setAdapter(mMovieAdapter);

        // attach a Listener to launch the MovieDetailActivity when a poster is tapped
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {

                // get the MovieData associated with the selected movie poster
                MovieData thisMovie = mMovieAdapter.getItem(position);

                // create a new intent to launch the movie detail activity
                Intent detailIntent = new Intent(getActivity(), MovieDetailActivity.class);

                // The MovieData object is passed to the MovieDetailActivity using the intent Bundle
                detailIntent.putExtra("com.derekmorrison.movieref.MovieData", thisMovie);
                startActivity(detailIntent);
            }
        });

        return rootView;
    }

    @Override
    public void onStart(){
        super.onStart();

        // The SettingsActivity will set a Global boolean whenever a preference is changed
        if (Globals.getInstance().getPrefChanged()) {
            updateMovieList();
        }
    }

    private void updateMovieList(){

        final String LEGIT_MIN_VOTES = "50";
        final String NO_MIN_VOTES = "0";
        final String HIGHEST_RATED = "0";

        // read the current preferences stored on this device
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getActivity().getApplicationContext());
        String sort_by = sharedPref.getString(getString(R.string.pref_sort_order), "1");  // 1 = Most Popular, 0 = Highest Vote
        Boolean min_count = sharedPref.getBoolean("pref_vote_count", true);
        String api_key = sharedPref.getString("pref_api_key", "");

        // check the API Key stored in the preferences, at this point just use empty vs. not empty
        if (api_key.isEmpty()) {
            showDialog();
            return;
        }

        // get the parameters from the pref storage and pass them to the async thread that calls TMDB
        String[] pList = new String[3];
        pList[0] = sort_by;

        // according to The Movie Database, a movie needs at least 50 votes to be legitimately popular
        pList[1] = min_count ? LEGIT_MIN_VOTES : NO_MIN_VOTES;

        // private API Key issued by TMDB
        pList[2] = api_key;

        // inform the user about the list of posters that is being displayed, the sorting is either 'Most Popular' or 'Highest Rating'
        View thisView = getView();
        TextView selectedBy = null;

        // create a string to inform the user about the sort order of the displayed posters
        String sorted_by = getResources().getString(R.string.sorted_by_most_popular);
        if (sort_by.equals(HIGHEST_RATED)) sorted_by = getResources().getString(R.string.sorted_by_highest_rating);

        // make sure the view is not null before using it to find the TextView
        if (thisView != null)
            selectedBy = (TextView)thisView.findViewById(R.id.selectedBytextView);

        if (selectedBy != null) {
            String sort_selection = getResources().getString(R.string.sorted_by_title);
            sort_selection += sorted_by;
            selectedBy.setText(sort_selection);
        }

        // create a new async task and launch it
        FetchMoviesTask moviesTask = new FetchMoviesTask();
        moviesTask.execute(pList);
    }

    // this method is used to inform the user that there is no proper API Key for TMDB stored in the prefs
    // the choices are to exit this app or enter the API key now via the settings activity
    private void showDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setCancelable(false);
        builder.setTitle(R.string.no_key_title);
        builder.setInverseBackgroundForced(true);
        builder.setPositiveButton(R.string.no_key_positive_button,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,
                                    int which) {
                dialog.dismiss();

                // use an Intent to launch the SettingsActivity
                Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
                settingsIntent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
                settingsIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, SettingsActivity.GeneralPreferenceFragment.class.getName());
                startActivity(settingsIntent);
                }
            });
        builder.setNegativeButton(R.string.no_key_negative_button,
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    getActivity().finish();
                }
            });

        builder.show();
    }


    public class FetchMoviesTask extends AsyncTask<String, Void, MovieData[]> {

        private final String LOG_TAG = FetchMoviesTask.class.getSimpleName();

        private String units = "2";

        // sometimes a field from The Movie Database can come back containing the word 'null'
        // it is more user friendly to show 'N/A' for the string instead of 'null'
        private String ReplaceNull(String in){
            if (in.equals("null")){
                in = getResources().getString(R.string.not_available_NA);
            }
            return in;
        }

        /**
         * This was mostly copied from the UDACITY Sunshine project
         *
         * Take the String representing the movies in JSON Format and
         * pull out the data we need to construct the Strings needed for the wireframes.
         *
         * Fortunately parsing is easy:  constructor takes the JSON string and converts it
         * into an Object hierarchy for us.
         */
        private MovieData[] getMovieDataFromJson(String movieDatabaseJsonStr, int numDays)
            throws JSONException {

            // These are the names of the JSON objects that need to be extracted.
            final String TMDB_RESULTS = "results";
            final String TMDB_TITLE = "title"; //"original_title";  use title because it's more likely English
            final String TMDB_RELEASE_DATE = "release_date";
            final String TMDB_POSTER_PATH = "poster_path";
            final String TMDB_VOTE = "vote_average";
            final String TMDB_DESCRIPTION = "overview";

            JSONObject forecastJson = new JSONObject(movieDatabaseJsonStr);
            JSONArray movieArray = forecastJson.getJSONArray(TMDB_RESULTS);

            int numMovies = movieArray.length();

            MovieData[] resultStrs = new MovieData[numMovies];

            for(int i = 0; i < movieArray.length(); i++) {

                // Get the JSON object representing one movie
                JSONObject movieInfo = movieArray.getJSONObject(i);

                // replace the word 'null' with 'N/A', it's a little more user friendly
                // while copying the fields we want into a new MovieData instance
                resultStrs[i] = new MovieData(
                    ReplaceNull(movieInfo.getString(TMDB_TITLE)),
                    ReplaceNull(movieInfo.getString(TMDB_RELEASE_DATE)),
                    ReplaceNull(movieInfo.getString(TMDB_POSTER_PATH)),
                    ReplaceNull(movieInfo.getString(TMDB_VOTE)),
                    ReplaceNull(movieInfo.getString(TMDB_DESCRIPTION)));
            }
            return resultStrs;
        }

        @Override
        protected MovieData[] doInBackground(String... params) {

            // all of these strings are currently defined by the movie database
            final String MOVIE_DB_BASE_URL = "http://api.themoviedb.org/3/discover/movie";
            final String SORT_PARAM = "sort_by";
            final String VOTE_COUNT = "vote_count.gte";
            final String API_KEY = "api_key";

            final String SORT_BY_POPULAR = "popularity.desc";
            final String SORT_BY_VOTE = "vote_average.desc";

            // Check for working internet connection and remember the status
            boolean connectionOK = hasActiveInternetConnection(getActivity().getApplicationContext());
            Globals.getInstance().setDataConnection(connectionOK);

            // no data connection means there is no point in trying to call TMDB
            if (!connectionOK) {
                return null;
            }

            // default sort order is "Most Popular"
            String sort = SORT_BY_POPULAR;

            // sort order can be by vote score
            if (params[0].equals("0"))
                sort = SORT_BY_VOTE;

            String count_minimum = params[1];
            if (count_minimum.isEmpty())
                count_minimum = "0";

            // if there is no API Key for TMDB then there is no point in calling TMDB
            // todo add more advanced tests for the API Key?
            String apiKey = params[2];
            if (apiKey.isEmpty())
                return null;

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String movieDataJsonStr = null;

            try {
                // Construct the URL for the The Movie Database query
                Uri builtUri = Uri.parse(MOVIE_DB_BASE_URL).buildUpon()
                        .appendQueryParameter(SORT_PARAM, sort)
                        .appendQueryParameter(VOTE_COUNT, count_minimum)
                        .appendQueryParameter(API_KEY, apiKey)
                        .build();

                URL url = new URL(builtUri.toString());

                // open the connection and request the data
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String

                // ToDo try catch block here - error probably bad api key
                InputStream inputStream = urlConnection.getInputStream();

                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }

                movieDataJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                // json data has been collected, now convert it into MovieData objects
                return getMovieDataFromJson(movieDataJsonStr, 0);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the movie data.
            return null;
        }


        /*
            This code was lifted from a couple of examples found on StackOverflow

            requires     <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

            this method tests the flags available in the ConnectivityManager and returns 'true' if any data path is 'CONNECTED'
        */
        public boolean isNetworkAvailable(Context context) {
            ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            if (connectivity == null) {
                return false;
            } else {
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null) {
                    for (int i = 0; i < info.length; i++) {
                        if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /*
            This code was lifted from a couple of examples found on StackOverflow

          this method checks for a data connection and then attempts to 'ping' a google server
          if the '204' request is successful then 'true' is returned
        */
        public boolean hasActiveInternetConnection(Context context) {
            if (isNetworkAvailable(context)) {
                try {
                    HttpURLConnection urlc = (HttpURLConnection) (new URL("http://clients3.google.com/generate_204").openConnection());
                    urlc.setRequestProperty("User-Agent", "Android");
                    urlc.setRequestProperty("Connection", "close");
                    urlc.setConnectTimeout(1500);
                    urlc.connect();
                    return (urlc.getResponseCode() == 204 &&
                            urlc.getContentLength() == 0);
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Error checking internet connection", e);
                }
            } else {
                Log.d(LOG_TAG, "No network available!");
            }
            return false;
        }

        @TargetApi(Build.VERSION_CODES.HONEYCOMB)
        @Override
        protected void onPostExecute(MovieData[] result) {

            // reset the flag to avoid unnecessary updates
            Globals.getInstance().setPrefChanged(false);

            int errorMsgVisibility = View.GONE;
            String errMessageString = getResources().getString(R.string.no_internet_connection);

            // clear out all old movie data
            mMovieAdapter.clear();

            if (result != null) {
                // save this array for saveInstanceState call
                mResult = result;

                // pass the movie data in as an ArrayList to the Movie Adapter
                ArrayList<MovieData> m = new ArrayList(Arrays.asList(result));
                mMovieAdapter.addAll(m);
            } else {
                // show error message to the user
                errorMsgVisibility = View.VISIBLE;

                if (Globals.getInstance().getDataConnection()){
                    errMessageString = getResources().getString(R.string.error_check_API_Key);
                }
            }

            Button retry = null;
            TextView errorMsgTextView = null;
            View thisView = getView();

            // make sure the view is not null before using it to find the TextView
            if (thisView != null) {
                retry = (Button) thisView.findViewById(R.id.retryButton);
                errorMsgTextView = (TextView) thisView.findViewById(R.id.errorTMDB_TextView);
            }

            // make sure the TextView was found before setting the visibility
            if (errorMsgTextView != null) {
                errorMsgTextView.setText(errMessageString);
                errorMsgTextView.setVisibility(errorMsgVisibility);
            }

            // the retry button is only visability when the problem is the internet connection
            if (retry != null) {
                int buttonVisibility = View.GONE;
                if (!Globals.getInstance().getDataConnection())
                    buttonVisibility = View.VISIBLE;
                retry.setVisibility(buttonVisibility);
            }
        }
    }

}
