package com.irahavoi.activityrecognitiondemo;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;
import com.irahavoi.activityrecognitiondemo.service.DetectedActivitiesIntentService;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity  implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks, ResultCallback<Status>{

    private TextView detectedActivitiesTxt;
    private Button requestActivitiesUpdatesButton;
    private Button removeActivitiesUpdatesButton;

    private ActivityDetectionBroadcastReceiver broadcastReceiver;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        detectedActivitiesTxt = (TextView) findViewById(R.id.detectedActivities);
        requestActivitiesUpdatesButton = (Button) findViewById(R.id.request_activity_updates_button);
        removeActivitiesUpdatesButton = (Button) findViewById(R.id.remove_activity_updates_button);

        broadcastReceiver = new ActivityDetectionBroadcastReceiver();
        buildGoogleAPiClient();
    }

    private void buildGoogleAPiClient(){
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(ActivityRecognition.API)
                .build();
    }

    @Override
    public void onStart(){
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    public void onStop(){
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public void onPause(){
        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter(Constants.BROADCAST_ACTION));
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

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public void requestActivityUpdatesButtonHandler(View view){
        if(!googleApiClient.isConnected()){
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                googleApiClient,
                Constants.DETECTION_INTERVAL_MILLIS,
                getActivityDetectionPendingIntent())
        .setResultCallback(this);

        requestActivitiesUpdatesButton.setEnabled(false);
        removeActivitiesUpdatesButton.setEnabled(true);
    }

    public void removeActivityUpdatesButtonHandler(View view){
        if(!googleApiClient.isConnected()){
            Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        ActivityRecognition.ActivityRecognitionApi.removeActivityUpdates(
                googleApiClient,
                getActivityDetectionPendingIntent())
        .setResultCallback(this);

        requestActivitiesUpdatesButton.setEnabled(true);
        removeActivitiesUpdatesButton.setEnabled(false);
    }

    protected PendingIntent getActivityDetectionPendingIntent(){
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    @Override
    public void onResult(Status status) {
        if(status.isSuccess()){
            Log.e(this.getLocalClassName(), "Successfully added activity detection!");
        } else{
            Log.e(this.getLocalClassName(), "Error adding or removing activity detection!");
        }
    }

    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver{
        protected final static String LOG_TAG = "ActivityDetectionBroadcastReceiver";
        @Override
        public void onReceive(Context context, Intent intent) {
            List<DetectedActivity> activities = (ArrayList)intent.getSerializableExtra(Constants.ACTIVITY_EXTRA);

            StringBuilder sb = new StringBuilder();

            for(DetectedActivity activity : activities){
                sb.append(getActivityString(activity.getType()) + " " + activity.getConfidence() + "% \n");
            }

            detectedActivitiesTxt.setText(sb.toString());
        }

        private String getActivityString(int activityType){
            switch (activityType){
                case DetectedActivity.IN_VEHICLE:
                    return getString(R.string.in_vehicle);
                case DetectedActivity.ON_BICYCLE:
                    return getString(R.string.on_bicycle);
                case DetectedActivity.ON_FOOT:
                    return getString(R.string.on_foot);
                case DetectedActivity.RUNNING:
                    return getString(R.string.running);
                case DetectedActivity.STILL:
                    return getString(R.string.still);
                case DetectedActivity.TILTING:
                    return getString(R.string.tilting);
                case DetectedActivity.UNKNOWN:
                    return getString(R.string.unknown);
                case DetectedActivity.WALKING:
                    return getString(R.string.walking);
                default:
                    return getString(R.string.unidentifiable_activity);

            }
        }
    }
}
