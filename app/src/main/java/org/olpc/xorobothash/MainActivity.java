package org.olpc.xorobothash;

import android.app.ProgressDialog;
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

    private ImageView robotImageView;
    private ProgressDialog pd;

    //For debugging
    private static final String TAG = "XORobotHash";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        robotImageView = (ImageView) findViewById(R.id.robot_hash_image);
        fetchRobot();
    }

    /**
     * Updates the view to show the already-downloaded robot, otherwise attempts to download it.
     * @return whether a robot image was successfully displayed
     */
    private void fetchRobot() {
        boolean isConnected = checkNetworkConnectivity();
        boolean done = updateView(isConnected);
        if (!done) {
           if(isConnected) {
               // Will call updateView again when done
               new DownloadRobotImage().execute(uniqueRoboHashURL(), robotImagePath());
           }
           // Nothing else to do, updateView already showed the right error message
        }
    }

    private boolean updateView(boolean isConnected) {
        Log.i(TAG, "In updateView()");
        boolean robotVisible = false;
        File robotFile = new File(robotImagePath());
        // Check if we have ever successfully downloaded the XO's robot image
        if (robotFile.exists()) {
            Log.i(TAG, "Robohash image file exists.");
            robotVisible = showRobot(robotFile);
            //If the robot couldn't be displayed, delete the file
            if (!robotVisible) {
                deleteFile(robotFile.getAbsolutePath());
            }
        }
        if (!isConnected && !robotVisible ) {
            Log.i(TAG, "Robohash image file does not exist, network is not connected.");
            showDisconnected();
        }
        return robotVisible;
    }

    private boolean showRobot(File robotFile) {
        Log.i(TAG, "In showRobot()");
        boolean robotDrawn = false;
        Bitmap bitmap = BitmapFactory.decodeFile(robotFile.getAbsolutePath());
        // Check the bitmap is valid
        if (bitmap != null) {
            // Draw the valid bitmap
            robotImageView.setImageBitmap(bitmap);
            robotDrawn = true;
        }
        return robotDrawn;
    }

    private void showDisconnected() {
        //Hide the robot text
        TextView robotText = (TextView) findViewById(R.id.robot_text_message);
        robotText.setVisibility(View.GONE);
        //Hide the robot image
        ImageView robotImage = (ImageView) findViewById(R.id.robot_hash_image);
        robotImage.setVisibility(View.GONE);
        //Show the disconnected error message
        TextView errorMessage = (TextView) findViewById(R.id.error_message);
        errorMessage.setText(getString(R.string.no_network_connection));
    }

    private String robotImagePath() {
        return this.getFilesDir() + File.pathSeparator + "this-xos-robot.png";
    }

    private String uniqueRoboHashURL() {
        String serialNumber = android.os.Build.SERIAL;  // if SERIAL doesn't work, try Settings.Secure.ANDROID_ID; ?
        return "http://robohash.org/" + serialNumber + ".png?ignoreext=false&size=600x600";
    }

    private boolean checkNetworkConnectivity() {
        boolean connected;
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            Log.i(TAG, "Network is connected.");
            connected = true;
        } else {
            Log.e(TAG, "Network is NOT connected.");
            connected = false;
        }
        return connected;
    }

    private class DownloadRobotImage extends AsyncTask<String, Void, Boolean> {

        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(MainActivity.this, "", getString(R.string.loading_message), false);
        }

        @Override
        protected Boolean doInBackground(String... params) {
            Log.i(TAG, "In DownloadRobotImage() doInBackground()");
            HttpURLConnection connection = null;
            boolean downloadSuccess = false;
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();

                InputStream in = new BufferedInputStream(connection.getInputStream());
                OutputStream out = new FileOutputStream(params[1]);

                int data = in.read();
                while (data != -1) {
                    out.write(data);
                    data = in.read();
                }
                downloadSuccess = true;
            } catch (IOException e) {
                Log.e(TAG + " TASK", "error retrieving robohash.org image", e);
                // Delete the file
                deleteFile(params[1]);
                downloadSuccess = false;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
            return downloadSuccess;
        }

        protected void onPostExecute(Boolean downloadSuccess) {
            Log.i(TAG, "In onPostExecute()");
            pd.dismiss();
            if (downloadSuccess) {
                updateView(downloadSuccess);
            } else {
                showDisconnected();
            }
        }
    }

}
