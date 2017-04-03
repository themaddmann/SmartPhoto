package com.brianmannresearch.smartphoto;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;

import android.location.Location;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class CameraActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, View.OnClickListener {
    private static final int TAKE_PICTURE = 0;
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    private ImageView selectedImage;
    private Button bCamera, endButton;
    private TextView exifData;
    private String filename;
    private Bitmap chosenImage;
    private String foldername, mode;
    private String[] filepath;
    private File imagesFolder;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private Location mCurrentLocation;

    StringBuilder builder;

    private float[] latlong = new float[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        // check if location is enabled
        // if not, create an alert dialog
        if (!isLocationEnabled(this)) {
            showSettingsAlert();
        }

        // collect data passed from previous activity
        Bundle extras = getIntent().getExtras();
        if (extras != null){
            foldername = extras.getString("folder");
            mode = extras.getString("mode");
        }

        // check if this is a new trip or a previous trip
        assert foldername != null;
        imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), foldername);
        if (mode.matches("new")) {
            imagesFolder.mkdir();
        }

        // create necessary elements for the layout
        builder = new StringBuilder();
        selectedImage = (ImageView) findViewById(R.id.selectedImage);
        bCamera = (Button) findViewById(R.id.bCamera);
        endButton = (Button) findViewById(R.id.endButton);
        exifData = (TextView) findViewById(R.id.ExifData);

        bCamera.setOnClickListener(this);
        endButton.setOnClickListener(this);

        // googleapi request
        if (mGoogleApiClient == null){
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        createLocationRequest();
    }

    private void createLocationRequest(){
        mLocationRequest = new LocationRequest();
        // desired interval for updates
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bCamera:
                // check if location services are enabled
                // if not, require the user to enable them before opening the camera
                if (!isLocationEnabled(this)) {
                    showSettingsAlert();
                }
                Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(photoIntent, TAKE_PICTURE);
                break;
            case R.id.endButton:
                showFinishAlert();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK) {
            // set imageview to latest picture
            chosenImage = (Bitmap) data.getExtras().get("data");
            selectedImage.setImageBitmap(chosenImage);

            filename = getOriginalImagePath();
            filepath = filename.split("/");

            // call function that handles embedding metadata into the image
            ReadExif(filename);

            // move the image to the trip folder
            File sourceFile = new File(filename);
            final File destFile = new File(imagesFolder, filepath[filepath.length - 1]);
            try {
                moveFile(sourceFile, destFile);
                scanFile(destFile.getAbsolutePath());
            } catch (IOException ex) {
                Toast.makeText(CameraActivity.this, ex.toString(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private String getOriginalImagePath(){
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = CameraActivity.this.managedQuery(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, null);
        int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToLast();

        return cursor.getString(column_index_data);
    }

    private void ReadExif(String file){
        try{
            ExifInterface exifInterface = new ExifInterface(file);

            // check if the image already has GPS metadata
            // if not, get current location and embed it into the image
            if (!exifInterface.getLatLong(latlong)) {
                double latitude = mCurrentLocation.getLatitude();
                double longitude = mCurrentLocation.getLongitude();
                String lat = convertLocation(latitude);
                String lon = convertLocation(longitude);
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitude < 0 ? "S" : "N");
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitude < 0 ? "W" : "E");
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, lat);
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lon);

                exifInterface.saveAttributes();
            }

            // generate the string to add the to the layout
            builder = new StringBuilder();
            builder.append("Filename: ").append(filepath[filepath.length-1]).append("\n");
            builder.append("Date & Time: ").append(exifInterface.getAttribute(ExifInterface.TAG_DATETIME)).append("\n");
            builder.append("GPS Latitude: ").append(getGeoCoordinates(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE))).append(" ").append(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)).append("\n");
            builder.append("GPS Longitude: ").append(getGeoCoordinates(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE))).append(" ").append(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
            exifData.setText(builder.toString());

        }catch (IOException e){
            e.printStackTrace();
            Toast.makeText(CameraActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    // function to convert from deg/min/sec into degrees
    private String getGeoCoordinates(String loc){
        String[] degMinSec = loc.split(",");
        String[] deg = degMinSec[0].split("/");
        String[] min = degMinSec[1].split("/");
        String[] sec = degMinSec[2].split("/");
        double deg1 = Double.parseDouble(deg[0]);
        double deg2 = Double.parseDouble(deg[1]);
        double degs = deg1/deg2;
        double min1 = Double.parseDouble(min[0]);
        double min2 = Double.parseDouble(min[1]);
        double mins = min1/min2;
        double sec1 = Double.parseDouble(sec[0]);
        double sec2 = Double.parseDouble(sec[1]);
        double secs = sec1/sec2;
        double degrees = degs + mins/60 + secs/3600;
        return String.valueOf(degrees);
    }

    // convert string location of deg/min/sec into a double
    private String convertLocation(double location){
        location=Math.abs(location);
        int degree = (int) location;
        location *= 60;
        location -= (degree * 60.0d);
        int minute = (int) location;
        location *= 60;
        location -= (minute * 60.0d);
        int second = (int) (location*1000.0d);

        return String.valueOf(degree) +
                "/1," +
                minute +
                "/1," +
                second +
                "/1000,";
    }

    // ask user if they want to finish current trip
    private void showFinishAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setMessage("Are you sure you want to end this trip?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent data = new Intent();
                        String text = "Finished";
                        data.setData(Uri.parse(text));
                        setResult(RESULT_OK, data);
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void startLocationUpdates(){
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }

    // when the user hits the back button, ask if they want to exit the app
    @Override
    public void onBackPressed(){
        showFinishAlert();
    }

    // function that moves the newly created photo file from the standard photo directory to the specific trip directory
    private void moveFile(File sourceFile, File destFile) throws IOException{
        FileChannel source;
        FileChannel destination;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (source != null){
            destination.transferFrom(source, 0, source.size());
            sourceFile.delete();
            scanFile(sourceFile.getAbsolutePath());
        }
        if (source != null){
            source.close();
        }
        destination.close();
    }

    // run the media scanner
    private void scanFile(String path){
        MediaScannerConnection.scanFile(CameraActivity.this, new String[] { path }, null,
                new MediaScannerConnection.OnScanCompletedListener(){
                    public void onScanCompleted(String path, Uri uri){
                    }
                });
    }

    // check if location services are enabled
    private static boolean isLocationEnabled(Context context){
        int locationMode;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try{
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            }catch (Settings.SettingNotFoundException e){
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }

    // prompt user to turn on location services
    private void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setTitle("Location Services");
        alertDialog.setMessage("Your GPS seems to be disabled. This application requires GPS to be turned on. Do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener(){
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id){
                        dialog.cancel();
                        finish();
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    // remaining functions are all needed for location services
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (mCurrentLocation == null){
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
    }

    @Override
    protected void onStart(){
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onResume(){
        super.onResume();
        if (mGoogleApiClient.isConnected()){
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    protected void onStop(){
        mGoogleApiClient.disconnect();
        super.onStop();
    }
}
