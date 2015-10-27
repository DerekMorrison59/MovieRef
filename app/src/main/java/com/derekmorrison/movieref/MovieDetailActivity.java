package com.derekmorrison.movieref;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

public class MovieDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
/*
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
*/
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onPause() {
        //onBackPressed();
        super.onPause();
        //if (!getSupportFragmentManager().popBackStackImmediate()) {
        //    supportFinishAfterTransition();
        //}
    }

    /**
     * Take care of popping the fragment back stack or finishing the activity
     * as appropriate.
     */
//    public void onBackPressed() {
//        if (!getSupportFragmentManager().popBackStackImmediate()) {
//            supportFinishAfterTransition();
//        }
//    }

}
