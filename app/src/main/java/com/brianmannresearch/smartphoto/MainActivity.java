package com.brianmannresearch.smartphoto;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int CAMERA_INTENT = 1, COURSE_LOCATION_REQUEST = 2, FINE_LOCATION_REQUEST = 3,
            WRITE_STORAGE_REQUEST = 4, READ_STORAGE_REQUEST = 5, CAMERA_REQUEST = 6;

    Button startButton, continueButton, tripButton, exitButton, deleteButton;
    TextView tripText;
    File imagesFolder, directory;
    GPSTracker gps;
    File[] folders, files;
    String[] foldername;

    int tripid, permission = 0;
    StringBuilder trips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!isLocationEnabled(this)) {
            showSettingsAlert();
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, COURSE_LOCATION_REQUEST);
        }else{
            permission++;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, FINE_LOCATION_REQUEST);
        }else{
            permission++;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_STORAGE_REQUEST);
        }else{
            permission++;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_STORAGE_REQUEST);
            }
        }else{
            permission++;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_REQUEST);
        }else{
            permission++;
        }

        gps = new GPSTracker(MainActivity.this);

        tripText = (TextView) findViewById(R.id.tripText);

        if(permission == 5){
            resumestartup();
        }

        startButton = (Button) findViewById(R.id.startButton);
        continueButton = (Button) findViewById(R.id.continueButton);
        tripButton = (Button) findViewById(R.id.tripButton);
        exitButton = (Button) findViewById(R.id.exitButton);
        deleteButton = (Button) findViewById(R.id.deleteButton);

        startButton.setOnClickListener(this);
        continueButton.setOnClickListener(this);
        tripButton.setOnClickListener(this);
        exitButton.setOnClickListener(this);
        deleteButton.setOnClickListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case CAMERA_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission++;
                    if (permission == 5) {
                        resumestartup();
                    }
                }
                break;
            }
            case COURSE_LOCATION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission++;
                    if (permission == 5) {
                        resumestartup();
                    }
                }
                break;
            }
            case FINE_LOCATION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission++;
                    if (permission == 5) {
                        resumestartup();
                    }
                }
                break;
            }
            case READ_STORAGE_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission++;
                    if (permission == 5) {
                        resumestartup();
                    }
                }
                break;
            }
            case WRITE_STORAGE_REQUEST:{
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permission++;
                    if (permission == 5) {
                        resumestartup();
                    }
                }
                break;
            }
        }
    }

    private void resumestartup() {
        directory = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)));
        folders = directory.listFiles();
        trips = new StringBuilder();
        trips.append("Existing Trips:");
        for (int i = 0; i < folders.length; i++){
            foldername = folders[i].toString().split("/");
            if (foldername[foldername.length-1].matches("Trip_\\d*")) {
                trips.append("\n").append("- ").append(foldername[foldername.length - 1]);
            }
        }
        tripText.setText(trips);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.startButton:
                showNewTripAlert();
                break;
            case R.id.continueButton:
                showContinueTripAlert();
                break;
            case R.id.tripButton:
                showTripAlert();
                break;
            case R.id.exitButton:
                showFinishAlert();
                break;
            case R.id.deleteButton:
                showDeleteAlert();
                break;
        }
    }

    @Override
    public void onBackPressed(){

    }

    private void showFinishAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        alertDialog.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
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

    private void showTripAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        alertDialog.setMessage("What trip number do you want to view?")
                .setCancelable(false)
                .setView(inflater.inflate(R.layout.trip_dialog, null))
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.tripID);
                        String input = text.getText().toString();
                        if (input.matches("")){
                            Toast.makeText(MainActivity.this, "Please enter a value", Toast.LENGTH_LONG).show();
                        }else {
                            tripid = Integer.parseInt(input);
                            Intent galleryIntent = new Intent(MainActivity.this, GalleryActivity.class);
                            galleryIntent.putExtra("tripid", tripid);
                            startActivity(galleryIntent);
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void showNewTripAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        alertDialog.setMessage("What trip number is this?")
                .setCancelable(false)
                .setView(inflater.inflate(R.layout.trip_dialog, null))
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.tripID);
                        String input = text.getText().toString();
                        if (input.matches("")){
                            Toast.makeText(MainActivity.this, "Please enter a value", Toast.LENGTH_LONG).show();
                        }else {
                            tripid = Integer.parseInt(input);
                            imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Trip_" + tripid);
                            if (imagesFolder.exists() && imagesFolder.isDirectory() && (imagesFolder.listFiles().length != 0)) {
                                showExistsNewAlert();
                            } else {
                                Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
                                cameraIntent.putExtra("tripid", tripid);
                                cameraIntent.putExtra("mode", "new");
                                startActivityForResult(cameraIntent, CAMERA_INTENT);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void showContinueTripAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();

        alertDialog.setMessage("What trip do you want to continue?")
                .setCancelable(false)
                .setView(inflater.inflate(R.layout.trip_dialog, null))
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.tripID);
                        String input = text.getText().toString();
                        if (input.matches("")){
                            Toast.makeText(MainActivity.this, "Please enter a value", Toast.LENGTH_LONG).show();
                        }else {
                            tripid = Integer.parseInt(input);
                            imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Trip_" + tripid);
                            if (!imagesFolder.exists() && !imagesFolder.isDirectory()) {
                                showExistsContinueAlert();
                            } else {
                                Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
                                cameraIntent.putExtra("tripid", tripid);
                                cameraIntent.putExtra("mode", "continue");
                                startActivityForResult(cameraIntent, CAMERA_INTENT);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void showExistsNewAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("This trip already exists. Do you want to continue this trip, instead?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
                        cameraIntent.putExtra("tripid", tripid);
                        cameraIntent.putExtra("mode", "continue");
                        startActivityForResult(cameraIntent, CAMERA_INTENT);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void showExistsContinueAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("This trip does not exist. Do you want to create it, instead?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent cameraIntent = new Intent(MainActivity.this, CameraActivity.class);
                        cameraIntent.putExtra("tripid", tripid);
                        cameraIntent.putExtra("mode", "new");
                        startActivityForResult(cameraIntent, CAMERA_INTENT);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void showConfirmDeleteAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("Are you sure you want to delete this trip?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (!imagesFolder.exists() && !imagesFolder.isDirectory()) {
                            showExistsAlert();
                        } else {
                            deleteDirectory(imagesFolder);
                            directory = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)));
                            folders = directory.listFiles();
                            trips = new StringBuilder();
                            trips.append("Existing Trips:");
                            for (int j = 0; j < folders.length; j++){
                                foldername = folders[j].toString().split("/");
                                if (foldername[foldername.length-1].matches("Trip_\\d*")) {
                                    trips.append("\n").append("- ").append(foldername[foldername.length - 1]);
                                }
                            }
                            tripText.setText(trips);
                        }
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void showDeleteAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = this.getLayoutInflater();

        alertDialog.setMessage("What trip number do you want to delete?")
                .setCancelable(false)
                .setView(inflater.inflate(R.layout.trip_dialog, null))
                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Dialog f = (Dialog) dialogInterface;
                        EditText text = (EditText) f.findViewById(R.id.tripID);
                        String input = text.getText().toString();
                        if (input.matches("")){
                            Toast.makeText(MainActivity.this, "Please enter a value", Toast.LENGTH_LONG).show();
                        }else {
                            tripid = Integer.parseInt(input);
                            imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Trip_" + tripid);
                            showConfirmDeleteAlert();
                        }
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // do nothing
                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    private void deleteDirectory(final File Directory){
        files = Directory.listFiles();
        for (int j = 0; j < files.length; j++) {
            if (files[j].delete()){
                Log.e("-->", "file Deleted :" + files[j].getAbsolutePath());
                callBroadCast();
            }else{
                Log.e("-->", "file not Deleted :" + files[j].getAbsolutePath());
            }
        }
    }

    private void callBroadCast() {
        if (Build.VERSION.SDK_INT >= 14){
            MediaScannerConnection.scanFile(this, new String[]{Environment.getExternalStorageDirectory().toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
                @Override
                public void onScanCompleted(String path, Uri uri) {
                    Toast.makeText(MainActivity.this, "Scanned", Toast.LENGTH_LONG).show();
                    Log.e("ExternalStorage", "Scanned " + path + ":");
                    Log.e("ExternalStorage", "-> uri=" + uri);
                }
            });
        }else{
            Log.e("-->", " < 14");
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
                    Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        }
    }

    private void showExistsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setMessage("This trip does not exist!")
                .setCancelable(false)
                .setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });
        final AlertDialog alert = alertDialog.create();
        alert.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_INTENT && resultCode == RESULT_OK) {
            directory = new File(String.valueOf(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)));
            folders = directory.listFiles();
            trips = new StringBuilder();
            trips.append("Existing Trips:");
            for (int i = 0; i < folders.length; i++){
                foldername = folders[i].toString().split("/");
                if (foldername[foldername.length-1].matches("Trip_\\d*")) {
                    trips.append("\n").append("- ").append(foldername[foldername.length - 1]);
                }
            }
            tripText.setText(trips);
        }
    }

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
}

