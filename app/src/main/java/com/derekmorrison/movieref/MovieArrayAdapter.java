package com.derekmorrison.movieref;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

/**
 * Created by Derek on 10/17/2015.
 */
public class MovieArrayAdapter extends ArrayAdapter<MovieData> {
    public MovieArrayAdapter(Context context, ArrayList<MovieData> movies) {
        super(context, 0, movies);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        final String url_base = "http://image.tmdb.org/t/p/";
        final String image_thumbnail_size = "w185";

        // Get the data item for this position
        MovieData movieData = getItem(position);

        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_movie_poster, parent, false);
        }

        // find the ImageView for this movie
        ImageView imageView = (ImageView) convertView.findViewById(R.id.list_item_movie_poster_imageview);

        if (movieData.getmPosterPath().equals(getContext().getResources().getString(R.string.not_available_NA))) {
            // load the "Image Not Available" image because there is no poster on TMDB website
            Picasso
                    .with(getContext())
                    .load(R.drawable.image_not_avail)
                    .error(R.drawable.error_image_not_loaded)
                    .into(imageView);

        } else {
            // load the ImageView with the smallish poster image from TMDB
            String imageURL = url_base + image_thumbnail_size + movieData.getmPosterPath();
            Picasso
                    .with(getContext())
                    .load(imageURL)
                    .error(R.drawable.error_image_not_loaded)  // Picasso was unable to download the poster image
                    .into(imageView);
        }

        // Return the completed view to render on screen
        return convertView;
    }
}
