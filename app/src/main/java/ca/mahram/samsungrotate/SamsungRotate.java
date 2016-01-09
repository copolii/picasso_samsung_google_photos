package ca.mahram.samsungrotate;

import android.app.Application;

import com.squareup.picasso.Picasso;

/**
 Created by mahram on 2016-01-08.
 */
public class SamsungRotate
  extends Application {
    @Override public void onCreate () {
        super.onCreate ();
        Picasso.setSingletonInstance (new Picasso.Builder (this)
                                        .indicatorsEnabled (true)
                                        .loggingEnabled (true)
                                        .build ());
    }
}
