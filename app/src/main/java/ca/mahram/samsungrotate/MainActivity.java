package ca.mahram.samsungrotate;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.Locale;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity
  extends AppCompatActivity {

    private static final int    REQUEST_IMAGE_CAPTURE = 100;
    private static final String LOGTAG                = "SAMSUNGROTATE";

    @Bind (R.id.toolbar)      Toolbar   toolbar;
    @Bind (R.id.content_main) View      mainContentView;
    @Bind (R.id.image)        ImageView imageView;

    private Uri imageUri = null;

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_main);
        ButterKnife.bind (this);

        setSupportActionBar (toolbar);
    }

    @Override protected void onActivityResult (final int requestCode, final int resultCode, final Intent data) {
        super.onActivityResult (requestCode, resultCode, data);

        if (requestCode != REQUEST_IMAGE_CAPTURE)
            return;

        if (RESULT_OK != resultCode) {
            Snackbar.make (mainContentView,
                           resultCode == RESULT_CANCELED
                           ? R.string.image_capture_cancelled
                           : R.string.image_capture_failed,
                           Snackbar.LENGTH_LONG)
                    .setAction (android.R.string.ok, doNothingClickListener).show ();
            clearImage ();
            return;
        }

        setImage (data.getData ());

        final Bundle extras = data.getExtras ();

        if (extras != null)
            logExtras (extras);

        Log.d (LOGTAG, data.toString ());
    }

    private void logExtras (final Bundle extras) {
        if (null == extras) return;

        Log.d(LOGTAG, "Extras:");
        for (final String key : extras.keySet ()) {
            final Object value = extras.get (key);
            Log.d(LOGTAG, String.format (Locale.ENGLISH, "%s : %s", key, value == null ? "<null>" : value.toString ()));
        }
    }

    private void clearImage () {
        imageUri = null;
        imageView.setImageResource (R.drawable.ic_image_none);
    }

    private void setImage (final Uri uri) {
        imageUri = uri;

        Picasso.with (this).load (uri).into (imageView, new Callback () {
            @Override public void onSuccess () {
                new RotationFinder ().execute (uri);
            }

            @Override public void onError () {

            }
        });
    }

    @OnClick (R.id.fab)
    public void onFabClicked (final View view) {
        Intent takePictureIntent = new Intent (Intent.ACTION_PICK);
        takePictureIntent.setType ("image/*");

        if (takePictureIntent.resolveActivity (getPackageManager ()) != null) {
            startActivityForResult (takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private class RotationFinder extends AsyncTask <Uri, Integer, Integer> {
        private Uri uri;

        @Override protected Integer doInBackground (final Uri... uris) {
            if (null == uris || uris.length == 0)
                return null;

            uri = uris[0];

            if (uris.length > 1) {
                Log.w (LOGTAG, "Too many uris. Ignoring all but " + uri.toString ());
            }

            if (null == uri) {
                Log.e(LOGTAG, "Invalid Uri: " + uri);
                return null;
            }

            final Cursor image = getContentResolver ().query (uri,
                                                              new String[] {MediaStore.Images.ImageColumns.ORIENTATION},
                                                              null, null, null);

            try {
                if (null == image) {
                    Log.wtf (LOGTAG, "Error. Null cursor.");
                    return null;
                }

                if (!image.moveToFirst ()) {
                    Log.e(LOGTAG, "Image not found");
                    return null;
                }

                return image.getInt (0);
            } finally {
                if (null != image) image.close ();
            }
        }

        @Override protected void onPostExecute (final Integer rotation) {
            if (null == rotation)  {
                Log.e (LOGTAG, "Error. Unable to find rotation");
                return;
            }

            Log.d (LOGTAG, uri.toString () + " has rotation " + rotation);

            if (rotation == 0)
                return;

            Snackbar.make (mainContentView, getString (R.string.needs_rotation, rotation), Snackbar.LENGTH_LONG)
              .setAction (R.string.rotate, new View.OnClickListener () {
                  @Override public void onClick (final View view) {
                      Picasso.with (MainActivity.this).load (uri).rotate (rotation).into (imageView);
                  }
              }).show ();
        }
    }

    private final View.OnClickListener doNothingClickListener = new View.OnClickListener () {
        @Override public void onClick (final View view) {
            // meh
        }
    };
}
