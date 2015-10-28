package com.derekmorrison.movieref;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

/**
 * A placeholder fragment containing a simple view.
 */
public class MovieDetailActivityFragment extends Fragment {

    public MovieDetailActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        // an instance of MovieData is passed from the main activity, it's a 'Parcelable'
        Bundle bun = getActivity().getIntent().getExtras();
        MovieData thisMovie = bun.getParcelable("com.derekmorrison.movieref.MovieData");

        // make sure the MovieData object has arrived safely
        if (thisMovie != null) {

            // find and populate the various widgets with data from the MovieData object
            TextView title = (TextView)rootView.findViewById(R.id.movie_title_textview);
            title.setText(thisMovie.getmTitle());

            TextView rating_prefix = (TextView)rootView.findViewById(R.id.movie_rating_prefix_textview);
            rating_prefix.setText(getString(R.string.rating_prefix));

            TextView rating = (TextView)rootView.findViewById(R.id.movie_rating_textview);
            rating.setText(thisMovie.getmRating());

            TextView releasePrefix = (TextView)rootView.findViewById(R.id.movie_release_prefix_textview);
            releasePrefix.setText(getString(R.string.date_prefix));

            TextView releaseDate = (TextView)rootView.findViewById(R.id.movie_release_textview);
            releaseDate.setText(thisMovie.getmReleaseDate());

            TextView overView = (TextView)rootView.findViewById(R.id.movie_overview_textview);
            overView.setText(thisMovie.getmOverview());

            ImageView imageView = (ImageView) rootView.findViewById(R.id.movie_poster_imageview);

            // There is only one movie poster on this page so make it big
            final String TMDB_BASE = "http://image.tmdb.org/t/p/";
            final String TMDB_IMAGE_SIZE = "w780";

            if (thisMovie.getmPosterPath().equals(getResources().getString(R.string.not_available_NA))) {
                // load the "Image Not Available" image because there is no poster
                Picasso.with(rootView.getContext()).load(R.drawable.image_not_avail).into(imageView);
            } else {
                StringBuilder imageURL = new StringBuilder(TMDB_BASE);
                imageURL.append(TMDB_IMAGE_SIZE);
                imageURL.append(thisMovie.getmPosterPath());

                // use the Picasso library to do image handling
                Picasso.with(rootView.getContext()).load(imageURL.toString()).into(imageView);
            }
        }

        return rootView;
    }
}
