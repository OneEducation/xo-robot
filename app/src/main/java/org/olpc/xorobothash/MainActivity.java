package org.olpc.xorobothash;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.util.Log;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;



public class MainActivity extends ActionBarActivity {

    ImageView robotImageView;
    String robotImagePath;

    //For debugging
    String tag = "XORoboHash";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        robotImageView = (ImageView) findViewById(R.id.robot_hash_image);
        robotImagePath = this.getFilesDir() + File.pathSeparator + getString(R.string.robot_image_internal_filename);
        getRobot();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void getRobot() {
        Log.i (tag, "In getRobot()");
        //Check if the robot image file already exists
        File file = new File(robotImagePath);
        // Check if we have already downloaded the XO's robot image
        if(file.exists()) {
            Log.i (tag, "Robohash image file exists.");
            // Show the already downloaded robohash image
            showRobot(robotImagePath);
            return;
        }

        Log.i (tag, "File does not exist.");
        // We haven't already downloaded the image - check network connectivity
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            Log.i (tag, "Network is connected.");
            // Get the device's serial and concatenate into a RoboHash URL
            String serialNumber = android.os.Build.SERIAL;  // if SERIAL doesn't work, try Settings.Secure.ANDROID_ID; ?
            String roboHashUrl = getString(R.string.robohash_url) + serialNumber + getString(R.string.robohash_file_extension);
            // Execute the async download of the image
            new DownloadRobotImage().execute(roboHashUrl);
        } else {
            Log.e (tag, "Network is NOT connected.");
            // Hide the "Your robot is" message
            TextView robotText = (TextView) findViewById(R.id.robot_text_message);
            robotText.setVisibility(View.GONE);
            // Show the no network connection message
            TextView errorMessage = (TextView) findViewById(R.id.error_message);
            errorMessage.setText(getString(R.string.no_network_connection));
            return;
        }

    }

    private void showRobot(String imagePath) {
        Log.i (tag, "In showRobot()");
        if(imagePath!=null) {
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            robotImageView.setImageBitmap(bitmap);
        }
    }

    private class DownloadRobotImage extends AsyncTask<String, Void, String> {

        @Override
        protected void onPreExecute() {
            // TODO Display a spinner or loading message
        }

        @Override
        protected String doInBackground(String... params) {
            Log.i (tag, "In DownloadRobotImage() doInBackground()");

            HttpURLConnection connection = null;

            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();

                InputStream in = new BufferedInputStream(connection.getInputStream());

                OutputStream out = new FileOutputStream(robotImagePath);

                int data = in.read();
                while (data != -1) {
                    out.write(data);
                    data = in.read();
                }
            } catch (IOException e) {
                Log.e("TASK", "error retrieving robohash.org image", e);
                e.printStackTrace();
                return null;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }

            return robotImagePath;

        }

        protected void onPostExecute(String imagePath) {
            // TODO Display the image in the robot_hash_image ImageView
            Log.i (tag, "In onPostExecute()");
            showRobot(imagePath);

        }

    }

}
