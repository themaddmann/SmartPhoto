package com.brianmannresearch.smartphoto;

import android.app.ProgressDialog;
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
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.FileChannel;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int TAKE_PICTURE = 0;

    ImageView selectedImage;
    Button bCamera, endButton, bUpload, reviewButton;
    TextView exifData;
    ProgressDialog dialog = null;
    String filename, upLoadServerUrl = null, mode;
    Bitmap chosenImage;
    String[] filepath;
    File[] files;
    File imagesFolder;
    int serverResponseCode = 0, tripid;

    GPSTracker gps;
    StringBuilder builder;


    float[] latlong = new float[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (!isLocationEnabled(this)) {
            showSettingsAlert();
        }

        Bundle extras = getIntent().getExtras();
        if (extras != null){
            mode = extras.getString("mode");
            tripid = extras.getInt("tripid");
        }

        gps = new GPSTracker(CameraActivity.this);

        imagesFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Trip_" + tripid);
        if ("new".matches(mode)) {
            imagesFolder.mkdirs();
        }

        builder = new StringBuilder();
        selectedImage = (ImageView) findViewById(R.id.selectedImage);
        bCamera = (Button) findViewById(R.id.bCamera);
        bUpload = (Button) findViewById(R.id.bUpload);
        endButton = (Button) findViewById(R.id.endButton);
        exifData = (TextView) findViewById(R.id.ExifData);
        reviewButton = (Button) findViewById(R.id.reviewTrip);


        upLoadServerUrl = "http://ndssl.000webhostapp.com/photos/upload.php";

        bCamera.setOnClickListener(this);
        endButton.setOnClickListener(this);
        bUpload.setOnClickListener(this);
        reviewButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bCamera:
                if (!isLocationEnabled(this)) {
                    showSettingsAlert();
                }
                gps = new GPSTracker(CameraActivity.this);
                Intent photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(photoIntent, TAKE_PICTURE);
                break;
            case R.id.bUpload:
                if(imagesFolder.listFiles().length == 0) {
                    Toast.makeText(CameraActivity.this, "Please select a photo or take a new one", Toast.LENGTH_LONG).show();
                }else {
                    dialog = ProgressDialog.show(CameraActivity.this, "", "Uploading...", true);
                    files = imagesFolder.listFiles();
                    for (int i = 0; i < files.length; i++) {
                        final String uploadname = files[i].toString();
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                uploadPhoto(uploadname);
                            }
                        });
                        thread.start();
                        try {
                            thread.join();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                break;
            case R.id.endButton:
                showFinishAlert();
                break;
            case R.id.reviewTrip:
                Intent gIntent = new Intent(CameraActivity.this, GalleryActivity.class);
                gIntent.putExtra("tripid", tripid);
                startActivity(gIntent);
                break;
        }
    }

    private int uploadPhoto(String sourceFileUri) {
        HttpURLConnection conn;
        DataOutputStream dos;
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1024 * 1024;
        File sourceFile = new File(sourceFileUri);

        if (!sourceFile.isFile()) {
            dialog.dismiss();

            Log.e("uploadFile", "Source File does not exist: " + filename);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(CameraActivity.this, "File does not exist", Toast.LENGTH_LONG).show();
                }
            });
            return 0;
        } else {
            try {
                // open a URL connection to the server
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUrl);

                // open a HTTP connection to the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // allow inputs
                conn.setDoOutput(true); // allow outputs
                conn.setUseCaches(false); // prevent a cached copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", sourceFileUri);

                dos = new DataOutputStream(conn.getOutputStream());

                String trip_id = String.valueOf(tripid);

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=trip_id;id=" + trip_id + "\r\n");
                dos.writeBytes("\r\n");

                dos.writeBytes(trip_id);
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "\r\n");

                dos.writeBytes("--" + boundary + "\r\n");
                dos.writeBytes("Content-Disposition: form-data; name=uploaded_file;filename=" + sourceFileUri + "\r\n");
                dos.writeBytes("\r\n");

                // create a buffer of max size
                bytesAvailable = fileInputStream.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                // send multipart form data
                dos.writeBytes("\r\n");
                dos.writeBytes("--" + boundary + "--\r\n");

                // responses from the server
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();
                Log.i("uploadFile", "HTTP Response is: " + serverResponseMessage + ": " + serverResponseCode);

                if (serverResponseCode == 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {Toast.makeText(CameraActivity.this, "Upload complete...", Toast.LENGTH_LONG).show();}
                    });
                }

                // close the streams
                fileInputStream.close();
                dos.flush();
                dos.close();
            } catch (MalformedURLException ex) {
                dialog.dismiss();
                ex.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CameraActivity.this, "Malformed URL", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {
                dialog.dismiss();
                e.printStackTrace();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(CameraActivity.this, "Exception detected", Toast.LENGTH_LONG).show();
                    }
                });
                Log.e("Upload file exception", "Exception: " + e.getMessage(), e);
            }
            dialog.dismiss();
            return serverResponseCode;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_PICTURE && resultCode == RESULT_OK) {
            gps = new GPSTracker(CameraActivity.this);
            chosenImage = (Bitmap) data.getExtras().get("data");
            selectedImage.setImageBitmap(chosenImage);

            filename = getOriginalImagePath();
            filepath = filename.split("/");

            ReadExif(filename);

            File sourceFile = new File(filename);
            final File destFile = new File(imagesFolder, filepath[filepath.length - 1]);
            try {
                copyFile(sourceFile, destFile);
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

            if (!exifInterface.getLatLong(latlong)) {
                double latitude = gps.getLatitude();
                double longitude = gps.getLongitude();
                String lat = Location.convert(Math.abs(latitude), Location.FORMAT_SECONDS);
                String lon = Location.convert(Math.abs(longitude), Location.FORMAT_SECONDS);
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latitude < 0 ? "S" : "N");
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, longitude < 0 ? "W" : "E");
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, lat);
                exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lon);
                exifInterface.saveAttributes();
            }

            builder = new StringBuilder();
            builder.append("Filename: ").append(filepath[filepath.length-1]).append("\n");
            builder.append("Date & Time: ").append(exifInterface.getAttribute(ExifInterface.TAG_DATETIME)).append("\n");
            builder.append("Trip ID: ").append(tripid).append("\n");
            String lat = getGeoCoordinates(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE));
            String lon = getGeoCoordinates(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE));
            builder.append("GPS Latitude: ").append(lat).append(" ").append(exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)).append("\n");
            builder.append("GPS Longitude: ").append(lon).append(" ").append(exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF));
            exifData.setText(builder.toString());

        }catch (IOException e){
            e.printStackTrace();
            Toast.makeText(CameraActivity.this, e.toString(), Toast.LENGTH_LONG).show();
        }
        latlong = new float[2];
    }

    private String getGeoCoordinates(String loc){
        String[] degMinSec = loc.split(":");
        double degrees = Double.parseDouble(degMinSec[0]) + Double.parseDouble(degMinSec[1])/60 + Double.parseDouble(degMinSec[2])/3600;
        return String.valueOf(degrees);
    }

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

    @Override
    public void onBackPressed(){

    }

    private void copyFile(File sourceFile, File destFile) throws IOException{
        FileChannel source;
        FileChannel destination;
        source = new FileInputStream(sourceFile).getChannel();
        destination = new FileOutputStream(destFile).getChannel();
        if (source != null){
            destination.transferFrom(source, 0, source.size());
        }
        if (source != null){
            source.close();
        }
        destination.close();
    }

    private void scanFile(String path){
        MediaScannerConnection.scanFile(CameraActivity.this, new String[] { path }, null,
                new MediaScannerConnection.OnScanCompletedListener(){
                    public void onScanCompleted(String path, Uri uri){
                    }
                });
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
